
import pacmanServer.controllers.Client
import pacmanServer.views.CommandLine
import pacmanServer.views.Logger
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    for(arg in args){
        if(arg == "--help" || arg == "-h") {
            logHelp()
            exitProcess(0)
        }
    }

    val (address, port) = when (args.size)
    {
        2 -> args[0] to (args[1].toIntOrNull() ?: Config.serverDefaultPort)
        1 -> args[0] to Config.serverDefaultPort
        0 -> "127.0.0.1" to Config.serverDefaultPort
        else -> {
            logHelp()
            exitProcess(1)
        }
    }

    val client = try {
        Client(address, port)
    }
    catch (e: Exception) {
        Logger.logError("Could not connect to server. Exiting process...", 0)
        CommandLine.logError("Could not connect to server. Client wont start.")
        exitProcess(1)
    }

    Client(address, port).start()
}

fun logHelp(){
    println("Usage: \n" +
            "  client [-h|--help]\n" +
            "  client [serverAddress] [port]\n" +
            "When serverAddress is not provided, the default is localhost - 127.0.0.1\n" +
            "When port is not provided, the default is 3000\n"
            )
}