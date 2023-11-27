package pacmanServer.views

import Global
import pacmanServer.controllers.UDPConnectionController
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue

class UDPServer(private val port: Int): Runnable {

    private val channelMap = HashMap<String, Pair<BlockingQueue<ByteArray>, BlockingQueue<ByteArray>>>()
    private val buffer = ByteArray(Global.maxDatagramSize)

    override fun run() {
        val serverSocket: DatagramSocket
        try {
            serverSocket = DatagramSocket(port)
            Logger.logInfo("UDP server is listening on port $port", 0)
        }
        catch (e: Exception) {
            Logger.logError("Could not start UDP server", 0)
            return
        }
        listen(serverSocket)
    }

    private tailrec fun listen(serverSocket: DatagramSocket) {

        val datagram = DatagramPacket(buffer, buffer.size)
        serverSocket.receive(datagram)

        val clientAddress = datagram.address
        val clientPort = datagram.port

        val key = "$clientAddress:$clientPort"

        if (channelMap[key] == null) {
            Logger.log("[INFO] New key: $key", 1)
            val udpClientQueue: BlockingQueue<ByteArray> = SynchronousQueue()
            val writeQueue: BlockingQueue<ByteArray> = SynchronousQueue()
            channelMap[key] = Pair(udpClientQueue, writeQueue)
            Thread(UDPConnectionController(udpClientQueue, writeQueue, clientAddress)).start()
        }

        val value = channelMap[key]!!

        value.first.put(buffer)

        val writeQueue = value.second
        val dataToSend = writeQueue.take()

        Logger.logInfo("Sending data to $clientAddress", 0)
        val packet = DatagramPacket(dataToSend, dataToSend.size, clientAddress, clientPort)
        serverSocket.send(packet)

        listen(serverSocket)
    }
}
