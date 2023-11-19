package pacmanServer.controllers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import pacmanServer.errors.CustomException
import pacmanServer.models.structures.Body
import pacmanServer.models.structures.Message
import pacmanServer.views.Logger
import java.net.Socket
import java.util.concurrent.BlockingDeque
import java.util.concurrent.BlockingQueue

class TCPReaderController(writer: BlockingQueue<Message>, controller: BlockingQueue<Message>) : Runnable {

    private val controller = Controller(socket.inetAddress)

    public override fun run() {
        listen()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = Json.decodeFromStream(socket.getInputStream())
        }
        catch (e: Exception) {
            closeConnection(e)
            return
        }

        val serverMessage: Message = try {
            controller.handle(clientMessage)
        }
        catch (e: CustomException){
            errorMessage(e)
        }
        catch (e: Exception){
            errorMessage(e)
        }

        try {
            Json.encodeToStream(serverMessage, socket.getOutputStream())
        }
        catch (e: Exception) {
            closeConnection(e)
            return
        }

        if(serverMessage.type == "disconnectAck"){
            closeConnection()
            return
        }
        listen()
    }

    private fun closeConnection(e: Exception? = null){
        e?.let { Logger.log("[ERROR] ${e.message}", 0) }
        Logger.log("Closing connection...", 0)
        controller.flushSession()
        socket.close()
    }

    private fun errorMessage(e: Exception): Message {
        Logger.log("[ERROR]: ${e.message}", 0)
        return when(e) {
            is CustomException -> Message(
                    type = "error",
                    status = e.status,
                    body = Body(info = e.message)
                )
            else -> Message(
                type = "error",
                status = 500,
                body = Body(info = "There was an unexpected error :(")
            )
        }
    }
}