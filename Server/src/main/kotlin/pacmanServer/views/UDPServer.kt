package pacmanServer.views

import Config
import kotlinx.coroutines.channels.Channel
import pacmanServer.controllers.UDPConnectionController
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.system.exitProcess

class UDPServer(): Runnable {

    private val port = Config.port;
    private val channelMap = HashMap<String, Channel<String>>()

    override fun run() {
        TODO()
        start()
    }

    fun start() {
        TODO()
        val serverSocket: DatagramSocket
        val buffer = ByteArray(1024)
        try {
            serverSocket = DatagramSocket(port)
            Logger.log("[INFO] Server is listening on port $port", 0)
        }
        catch (e: Exception) {
            Logger.log("[ERROR] Could not start server", 0)
            exitProcess(1)
        }
        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            serverSocket.receive(packet)
            val clientAddress = packet.address
            val clientPort = packet.port
            val data = String(packet.data, 0, packet.length)
            val key = "$clientAddress:$clientPort"

            if (channelMap[key] == null) {
                Channel<String>().let { channel ->
                    channelMap[key] = channel
                    Thread(UDPConnectionController(channel)).start()
                }
            }

            //channelMap[key]!!.send("oi")
        }
    }
}