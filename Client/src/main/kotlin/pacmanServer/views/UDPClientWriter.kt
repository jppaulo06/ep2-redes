package pacmanServer.views

import Config
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.BlockingQueue

class UDPClientWriter(
    private val serverSocket: DatagramSocket,
    private val clientWriterQueue: BlockingQueue<Message>
): Runnable {

    override fun run() {
        Logger.logInfo("Starting UDP writer thread", 0)
        write()
    }

    private tailrec fun write(){
        val clientMessage = clientWriterQueue.take()

        try {
            writeMessage(clientMessage)
        }
        catch (e: Exception) {
            Logger.logError("Failed to send message", 0)
            return
        }
        write()
    }

    private fun writeMessage(message: Message){
        Logger.logInfo("Sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray()
        val bytesLength = bytesMessage.size.toString().padStart(8, '0').toByteArray()

        val dataToSend = bytesLength + bytesMessage
        val packet = DatagramPacket(dataToSend, dataToSend.size, InetAddress.getByName(Config.serverDefaultAddress), Config.serverDefaultPort)
        serverSocket.send(packet)
    }
}
