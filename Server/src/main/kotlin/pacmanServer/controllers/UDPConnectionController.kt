package pacmanServer.controllers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.structures.errors.CustomException
import pacmanServer.views.Logger
import java.util.concurrent.BlockingQueue

class UDPConnectionController(val udpClientQueue: BlockingQueue<ByteArray>) : Runnable {

    // TODO: Create a some async stuff to verify if this thread has been stopped for too much time

    //private val controller = Controller()

    public override fun run() {
        TODO()
        listen()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = readNextMessage()
        }
        catch (e: Exception) {
            return
        }

        /*
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
            writeMessage(serverMessage)
        }
        catch (e: Exception) {
            return
        }

        if(serverMessage.type == "disconnectAck"){
            return
        }
        listen()

         */
    }

    private fun readNextMessage(): Message {
        val byteArray = udpClientQueue.take()

        val lengthBytes = byteArray.sliceArray(0..< 4)
        val messageLength = lengthBytes.toString(Charsets.UTF_8).toInt()

        val messageBytes = byteArray.sliceArray(4..< byteArray.size)
        while(messageBytes.size < messageLength) {
            messageBytes.plus(udpClientQueue.take())
        }

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.log("[INFO]: message received: ${message.toString()}", 2)
        return message
    }

    private fun writeMessage(message: Message){
        Logger.log("[INFO] sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
        val bytesLength = bytesMessage.size.toString().padStart(8, '0').toByteArray(Charsets.US_ASCII)
        TODO()
        //.write(bytesLength + bytesMessage)
    }

    private fun errorMessage(e: Exception): Message {
        Logger.log("[ERROR]: ${e.message}", 0)
        Logger.log("[ERROR]: Responding client with error message...", 0)
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