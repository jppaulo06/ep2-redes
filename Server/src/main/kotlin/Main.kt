import pacmanServer.models.UserManager
import pacmanServer.views.Logger
import pacmanServer.views.TCPServer
import pacmanServer.views.UDPServer
import kotlin.system.exitProcess

fun main(args: Array<String>) {


    val port = try {
        when (args.size) {
            1 -> args[0].toInt()
            0 -> Global.serverDefaultPort
            else -> {
                logHelp()
                exitProcess(1)
            }
        }
    } catch (e: Exception) {
        logHelp()
        return
    }

    Global.load()
    UserManager.load()

    Thread(TCPServer(port)).start()
    Thread(UDPServer(port)).start()

    Runtime.getRuntime().addShutdownHook(Thread {
        Logger.logInfo("Ending server", 0)
    })

}

fun logHelp() {
    println(
        "\nBad usage error. You should execute: \n\n" +
                "    server [serverPort]\n\n" +
                "When port is not provided, the default is ${Global.serverDefaultPort}"
    )
}
