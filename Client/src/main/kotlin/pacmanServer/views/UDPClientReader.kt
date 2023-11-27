package pacmanServer.views

import Config
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import pacmanServer.structures.errors.ExceptionConnectionClosed
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.BlockingQueue

class UDPClientReader(
    private val serverSocket: DatagramSocket,
    private val clientControllerQueue: BlockingQueue<Message>,
    private val clientWriterQueue: BlockingQueue<Message>
): Runnable {

    private val buffer = ByteArray(Config.maxDatagramSize)

    override fun run() {
        Logger.logInfo("Starting UDP reader thread", 0)
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

    private fun getDatagram(): ByteArray {
        val datagram = DatagramPacket(buffer, buffer.size)
        serverSocket.receive(datagram)
        Logger.log("[INFO]: received datagram", 2)
        return buffer
    }

    private fun readNextMessage(): Message {
        val byteArray = getDatagram()

        val lengthBytes = byteArray.sliceArray(0..< 8)
        val messageLength = lengthBytes.toString(Charsets.US_ASCII).toInt()

        val messageBytes = byteArray.sliceArray(8..< 8 + messageLength)

        Logger.log("[INFO]: receiving datagram of size ${messageLength}, until now ${messageBytes.size}", 2)

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.log("[INFO]: message received: ${message.toString()}", 2)
        return message
    }
}