package pacmanServer.views

import pacmanServer.controllers.TCPConnectionController
import java.net.ServerSocket
import kotlin.system.exitProcess

class TCPServer(private val port: Int): Runnable {

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
        listen(serverSocket)
    }

    private tailrec fun listen(serverSocket: ServerSocket) {
        val clientSocket = serverSocket.accept()
        Logger.log("Client connected: ${clientSocket.inetAddress.hostAddress}", 1)
        Thread(TCPConnectionController(clientSocket)).start()
        listen(serverSocket)
    }
}