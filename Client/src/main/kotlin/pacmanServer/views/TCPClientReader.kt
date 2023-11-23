package pacmanServer.views

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import pacmanServer.structures.errors.ExceptionConnectionClosed
import java.io.InputStream
import java.util.concurrent.BlockingQueue

class TCPClientReader(
    private val inputStream: InputStream,
    private val clientControllerQueue: BlockingQueue<Message>,
    private val clientWriterQueue: BlockingQueue<Message>
): Runnable {
    override fun run() {
        Logger.logInfo("Starting reader thread", 0)
        listen()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = readNextMessage()
        }
        catch (e: ExceptionConnectionClosed){
            Logger.logError("Connection with server has been lost", 0)
            return
        }
        catch (e: Exception) {
            Logger.logError("${e.message}", 0)
            return
        }

        Logger.log("Received message of type ${clientMessage.type}", 0)

        when(clientMessage.type) {
            "hearbeat" -> clientWriterQueue.put(Message(type = "heartbeatAck"))
            else -> clientControllerQueue.put(clientMessage)
        }

        listen()
    }

    private fun readNextMessage(): Message {
        val lengthBytes = ByteArray(8)
        inputStream.read(lengthBytes)
        if(lengthBytes.size == 0) throw ExceptionConnectionClosed("Connection has been lost")
        val messageLength = lengthBytes.toString(Charsets.US_ASCII).toInt()

        val messageBytes = ByteArray(messageLength)
        inputStream.read(messageBytes)

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.log("[INFO]: message received: ${message.toString()}", 2)

        return message
    }
}