package pacmanServer.views

import Config
import pacmanServer.controllers.TCPConnectionController
import java.net.Socket
import kotlin.system.exitProcess

class TCPClient(private val address: String): Runnable {

    private val port = Config.port;

    override fun run() {
        val clientSocket: Socket
        try {
            clientSocket = Socket(address, port)
            Logger.log("[INFO] Socket created to $address:$port", 0)
        }
        catch (e: Exception) {
            Logger.log("[ERROR] Could not create socket", 0)
            exitProcess(1)
        }
        Thread(TCPConnectionController(clientSocket)).start()
    }
}