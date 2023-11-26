package pacmanServer.views

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

    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = readNextMessage()
        }
        catch (e: ExceptionConnectionClosed){
            Logger.logInfo("Server has closed the connection", 0)
            return
        }
        catch (e: Exception) {
            Logger.logError("${e.message}", 0)
            return
        }

        Logger.logInfo("Received message of type ${clientMessage.type}", 2)

        when(clientMessage.type) {
            "hearbeat" -> clientWriterQueue.put(Message(type = "heartbeatAck"))
            else -> clientControllerQueue.put(clientMessage)
        }

        listen()
    }

    private fun readNextMessage(): Message {
        val lengthBytes = ByteArray(8)

        var ret = inputStream.read(lengthBytes)
        if(ret == -1) throw ExceptionConnectionClosed()

        val messageLength = lengthBytes.toString(Charsets.US_ASCII).toInt()
        val messageBytes = ByteArray(messageLength)

        ret = inputStream.read(messageBytes)
        if(ret == -1) throw ExceptionConnectionClosed()

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.logInfo("Message received: ${message.toString()}", 2)

        return message
    }
}