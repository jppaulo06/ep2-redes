package pacmanServer.views

import java.net.ServerSocket

import pacmanServer.controllers.TCPConnectionController
import kotlin.system.exitProcess

class TCPServer(): Runnable {

    private val port = Config.port;

    override fun run() {
        val serverSocket: ServerSocket
        try {
            serverSocket = ServerSocket(port)
            Logger.log("[INFO] Server is listening on port $port", 0)
        }
        catch (e: Exception) {
            Logger.log("[ERROR] Could not start server", 0)
            exitProcess(1)
        }
        while (true) {
            val clientSocket = serverSocket.accept()
            Logger.log("Client connected: ${clientSocket.inetAddress.hostAddress}", 1)
            Thread(TCPConnectionController(clientSocket)).start()
        }
    }
}