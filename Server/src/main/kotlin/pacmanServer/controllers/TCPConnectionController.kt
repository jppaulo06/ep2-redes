package pacmanServer.controllers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import pacmanServer.views.Logger
import java.net.Socket

class TCPConnectionController(private val socket: Socket) : Runnable {

    private val controller = Controller(socket.inetAddress)

    public override fun run() {
        Logger.log("[INFO] Connected: ${socket.inetAddress}:${socket.port}", 1)
        listen()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = readNextMessage()
        }
        catch (e: Exception) {
            closeConnection(e)
            return
        }

        val serverMessage: Message = controller.handle(clientMessage)

        try {
            writeMessage(serverMessage)
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

    private fun readNextMessage(): Message {
        val lengthBytes = ByteArray(4)
        socket.getInputStream().read(lengthBytes)
        val messageLength = lengthBytes.toString(Charsets.UTF_8).toInt()

        val messageBytes = ByteArray(messageLength)
        socket.getInputStream().read(messageBytes)

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.log("[INFO]: message received: ${message.toString()}", 2)

        return message
    }

    private fun writeMessage(message: Message){
        Logger.log("[INFO] sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
        val bytesLength = bytesMessage.size.toString().padStart(8, '0').toByteArray(Charsets.US_ASCII)
        socket.getOutputStream().write(bytesLength + bytesMessage)
    }

    private fun closeConnection(e: Exception? = null){
        e?.let { Logger.log("[ERROR] $e" +
                " \n${e.stackTrace}", 0) }
        Logger.log("[ERROR] Closing connection...", 0)
        controller.flushSession()
        socket.close()
    }

}