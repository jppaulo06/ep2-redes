import pacmanServer.controllers.Client
import pacmanServer.views.CommandLine
import pacmanServer.views.Logger
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    val protocol: String
    val serverIp: String
    val serverPort: Int
    val clientPort: Int

    if (args.isEmpty()) {
        protocol = "TCP"
        serverIp = Config.serverDefaultAddress
        serverPort = Config.serverDefaultPort
        clientPort = Config.clientDefaultPort
    }
    else if (args.size == 1) {
        protocol = args[0]
        if(protocol != "UDP" && protocol != "TCP") {
            logHelp()
            return
        }
        serverIp = Config.serverDefaultAddress
        serverPort = Config.serverDefaultPort
        clientPort = Config.clientDefaultPort
    } else if (args.size == 4) {
        protocol = args[0]
        if(protocol != "UDP" && protocol != "TCP") {
            logHelp()
            return
        }
        serverIp = args[1]
        try {
            serverPort = args[2].toInt()
            clientPort = args[3].toInt()
        } catch (e: Exception) {
            logHelp()
            return
        }
    } else {
        logHelp()
        return
    }

    Config.protocol = protocol
    Config.serverAddress = serverIp
    Config.clientPort = clientPort
    Config.serverPort = serverPort

    val client = try {
        Client(serverAddress = Config.serverAddress, serverPort = Config.serverPort)
    } catch (e: Exception) {
        Logger.logError("Could not connect to server. Exiting process...", 0)
        CommandLine.logError("Could not connect to server. Client wont start.")
        exitProcess(1)
    }

    client.start()
}

fun logHelp() {
    CommandLine.logError(
        "\nBad usage error. You should execute: \n\n" +
                "    client UDP|TCP\n" +
                "    client UDP|TCP serverIP serverPort clientPort\n\n" +
                "In the first usage, the serverIP, serverPort and clientPort are defined to be the defaults:\n" +
                "127.0.0.1, 3000 and 3001"
    )
}