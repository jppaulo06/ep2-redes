package pacmanServer.controllers


import Config
import kotlinx.coroutines.*
import pacmanServer.models.Game
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.structures.errors.CustomException
import pacmanServer.structures.errors.InvalidCommand
import pacmanServer.structures.errors.InvalidMessage
import pacmanServer.views.CommandLine
import pacmanServer.views.Logger
import pacmanServer.views.TCPClientReader
import pacmanServer.views.TCPClientWriter
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import kotlin.system.exitProcess

class Client (
    serverAddress: String,
    serverPort: Int,
) {

    private val clientControllerQueue: BlockingQueue<Message> = SynchronousQueue()
    private val clientWriterQueue: BlockingQueue<Message> = SynchronousQueue()

    private val socket: Socket = Socket(serverAddress, serverPort)

    private val tcpReaderThread = Thread(TCPClientReader(socket.getInputStream(), clientControllerQueue, clientWriterQueue))
    private val tcpWriterThread = Thread(TCPClientWriter(socket.getOutputStream(), clientWriterQueue))

    private val port = Config.clientDefaultPort

    private var game: Game? = null
    private var user: String? = null

    fun start() = runBlocking {
        Logger.logInfo("Starting processing and threads", 2)
        tcpReaderThread.start()
        tcpWriterThread.start()

        launch() {
            while (isActive) {
                delay(1000)
                if (!tcpReaderThread.isAlive || !tcpWriterThread.isAlive) {
                    Logger.logError("Connection closing", 0)
                    killServerThreads()
                    socket.close()
                    this.cancel()
                }
           }
        }
        process()
    }

    private fun killServerThreads(){
        if (tcpReaderThread.isAlive) {
            tcpWriterThread.interrupt()
        }
        if (tcpWriterThread.isAlive) {
            tcpWriterThread.interrupt()
        }
    }

    private tailrec fun process(){
        game?.let { CommandLine.logGame(it) }
        CommandLine.logCommandLine()
        val (command, args) = CommandLine.readCommand()
        Logger.logInfo("Command read $command with args ${args}}", 2)
        executeCommand(command, args)
        process()
    }

    private fun executeCommand(command: String, args: List<String>){
        if(isClientCommand(command)) executeClientCommand(command, args)
        else executeServerCommand(command, args)
    }

    private fun executeClientCommand(command: String, args: List<String>) {
        Logger.logInfo("Executing client command $command", 2)
        TODO()
    }

    private fun executeServerCommand(command: String, args: List<String>) {
        val clientMessage: Message
        Logger.log("Executing server command $command", 2)
        try {
            clientMessage = createCommandMessage(command, args)
        }
        catch (e: InvalidCommand) {
            Logger.logError("Invalid command: $command with args $args", 1)
            CommandLine.logError("Command $command does not exit")
            return
        }
        catch (e: Exception) {
            Logger.logError("Invalid arguments $args", 1)
            CommandLine.logError("Please provide correct arguments for command $command")
            return
        }
        sendRequest(clientMessage)
        val serverMessage = getResponse()
        try {
            checkResponse(clientMessage, serverMessage)
            execute(command, serverMessage)
        }
        catch (e: Exception) {
            Logger.logError(e, 1)
            CommandLine.logError("There was an error when communicating with the server :(")
            return
        }
    }

    private fun createCommandMessage(command: String, args: List<String>): Message {
        return when(command) {
            "novo" -> Message(
                type = "command",
                command = "registerUser",
                body = Body(username = args[0], password = args[1])
            )
            "senha" -> Message(
                type = "command",
                command = "changePassword",
                body = Body(password = args[0], newPassword = args[1])
            )
            "entra" -> Message(
                type = "command",
                command = "loginUser",
                body = Body(username = args[0], password = args[1], port = port)
            )
            "sai" -> Message(
                type = "command",
                command = "logoutUser",
            )
            "lideres" -> Message(
                type = "command",
                command = "listUsers",
            )
            "l" -> Message(
                type = "command",
                command = "listUsers",
            )
            "inicia" -> Message(
                type = "command",
                command = "startGame",
            )
            "desafio" -> Message(
                type = "command",
                command = "challengeUser",
                body = Body(username = args[0])
            )
            "tchau" -> Message(
                type = "command",
                command = "endSession",
            )
            else -> throw InvalidCommand("Command not found")
        }
    }

    private fun execute(command: String, serverMessage: Message){
        when(command) {
            "novo" -> executeRegisterUser(serverMessage)
            "senha" -> executeChangePassword(serverMessage)
            "entra" -> executeLoginUser(serverMessage)
            "sai" -> executeLogoutUser(serverMessage)
            "lideres" -> executeRanking(serverMessage)
            "l" -> executeListUsers(serverMessage)
            "inicia" -> executeStartGame(serverMessage)
            "desafio" -> executeChallenge(serverMessage)
            "tchau" -> executeEndSession(serverMessage)
            else -> throw InvalidCommand("Command not found")
        }
    }

    private fun executeRegisterUser(serverMessage: Message) {
        Logger.logInfo("User ${serverMessage.body!!.username} created successfully", 0)
        CommandLine.logSuccess("User ${serverMessage.body.username} created successfully!")
    }

    private fun executeLoginUser(serverMessage: Message){
        user = serverMessage.body!!.username
        Logger.logInfo("User ${serverMessage.body.username} logged in", 0)
        CommandLine.logSuccess("User $user logged in!")
    }

    private fun executeLogoutUser(serverMessage: Message){
        Logger.logInfo("User ${serverMessage.body!!.username} logged out", 0)
        CommandLine.logSuccess("User $user logged out")
        user = null
    }
    private fun executeChangePassword(serverMessage: Message){
        Logger.logInfo("User ${serverMessage.body!!.username} changed the password", 0)
        CommandLine.logSuccess("Password for $user has been changed successfully!")
    }

    private fun executeRanking(serverMessage: Message){
        if(serverMessage.body!!.users!!.isEmpty()){
            CommandLine.logWarning("No users registered")
            return
        }
        val rankedUsers = serverMessage.body.users!!.sortedBy { it.score }
        for (user in rankedUsers) {
            CommandLine.log(user.username)
        }
    }

    private fun executeListUsers(serverMessage: Message){
        if(serverMessage.body!!.users!!.isEmpty()){
            CommandLine.logWarning("No users registered")
            return
        }
        for (user in serverMessage.body.users!!) {
            CommandLine.log(user.username + " " + user.state)
        }
    }

    private fun executeStartGame(serverMessage: Message){
        game = Game(serverMessage.body!!.grid!!)
    }

    private fun executeChallenge(serverMessage: Message){
        // start new thread to communicate with the other client
        TODO()
    }


    private fun executeEndSession(serverMessage: Message){
        CommandLine.logSuccess("Exiting the program...")
        exitProcess(0)
    }

    private fun sendRequest(clientMessage: Message) {
        clientWriterQueue.put(clientMessage)
    }

    private fun getResponse(): Message {
        return clientControllerQueue.take()
    }

    private fun checkResponse(clientMessage: Message, serverMessage: Message) {
        if(
            serverMessage.type != "commandAck" ||
            serverMessage.command != clientMessage.command
        )
            throw InvalidMessage("Received a not expected message from server")

        if(serverMessage.status!! >= 400)
            throw InvalidMessage(serverMessage.body!!.info!!)
    }

    fun isClientCommand(command: String): Boolean {
        return false
    }

    private fun errorMessage(e: Exception): Message {
        Logger.log("[ERROR]: ${e.message}", 0)
        return when(e) {
            is CustomException -> Message(
                type = "error",
                status = e.status,
                body = Body(info = e.message)
            )
            else -> Message(
                type = "error",
                status = 500,
                body = Body(info = "There was an unexpected error :(")
            )
        }
    }
}