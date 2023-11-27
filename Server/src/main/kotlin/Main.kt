
import pacmanServer.models.UserManager
import pacmanServer.views.TCPServer
import pacmanServer.views.UDPServer
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    for(arg in args){
        if(arg == "--help" || arg == "-h") {
            logHelp()
            exitProcess(0)
        }
    }

    val port = when (args.size)
    {
        1 -> args[0].toIntOrNull() ?: Global.serverDefaultPort
        0 -> Global.serverDefaultPort
        else -> {
            logHelp()
            exitProcess(1)
        }
    }

    Global.load()
    UserManager.load()

    Thread(TCPServer(port)).start()
    Thread(UDPServer(port)).start()
}

fun logHelp(){
    println("Usage: \n" +
            "  server [-h|--help]\n" +
            "  server [serverAddress] [port]\n" +
            "When port is not provided, the default is 3000\n"
    )
}
