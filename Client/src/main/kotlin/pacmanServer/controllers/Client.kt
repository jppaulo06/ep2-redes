package pacmanServer.controllers


import Config
import kotlinx.coroutines.*
import pacmanServer.models.Game
import pacmanServer.models.gameStructures.Direction
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.structures.Session
import pacmanServer.structures.errors.*
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

    private var session =  Session()

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
        CommandLine.logCommandLine()
        val (command, args) = CommandLine.readCommand()
        Logger.logInfo("Command read $command with args ${args}}", 2)
        processCommand(command, args)
        process()
    }

    private fun processCommand(command: String, args: List<String>){
        if(isGameCommand(command)) processGameCommand(command, args)
        else processServerCommand(command, args)
    }

    private fun processGameCommand(command: String, args: List<String>) {
        if(!session.isPlaying()) {
            Logger.logError("User is not playing and tried to execute $command", 0)
            CommandLine.logError("You have to be playing to execute $command")
            return
        }
        try {
            executeGameCommand(command, args)
        }
        catch (e: InvalidCommand) {
            Logger.logError("Invalid command: $command with args $args", 1)
            CommandLine.logError("Command $command does not exit")
        }
        catch (e: InvalidDirection) {
            Logger.logError("Invalid direction ${args[0]}", 1)
            CommandLine.logWarning("Please, provide one of these directions: w, a, s, d")
            CommandLine.log("")
            return
        }
        catch (e: Exception) {
            Logger.logError("Invalid arguments $args for command $command", 1)
            Logger.logError(e, 1)
            CommandLine.logError("Please provide correct arguments for command $command")
            return
        }
    }

    private fun executeGameCommand(command: String, args: List<String>) {
        Logger.logInfo("Executing game command $command", 2)
        when (command) {
            "move" -> executeMove(args[0])
            "encerra" -> executeStopGame()
            "atraso" -> executeDelay()
            else -> InvalidCommand()
        }
    }

    private fun executeMove(directionString: String) {
        val direction: Direction = when(directionString) {
            "w" -> Direction.UP
            "s" -> Direction.DOWN
            "a" -> Direction.LEFT
            "d" -> Direction.RIGHT
            else -> throw InvalidDirection()
        }
        try {
            session.game!!.processRound(direction)
            if(session.game!!.ended()) executeStopGame(session.game!!.localScore())
        }
        catch (e: InvalidPosition) {
            Logger.logWarning("Invalid position for pacman. Movement not executed", 1)
            CommandLine.logWarning("Take care! There is a wall there. Movement not executed")
        }
        catch (e: Exception) {
            Logger.logError("Unexpected error processing game round", 0)
            Logger.logError(e, 0)
        }
    }

    private fun executeStopGame(score: Int = 0) {
        session.stopPlaying()
        val stopGameMessage = Message(
            type = "command",
            command = "stopGame",
            body = Body(score = score)
        )
        sendRequest(stopGameMessage)
        val serverMessage = getResponse()
        try {
            checkResponse(stopGameMessage, serverMessage)
        }
        catch (e: Exception) {
            Logger.logError("Something went wrong ending the game", 1)
            Logger.logError(e, 1)
            CommandLine.logError(e.message ?: "Something went wrong ending the game")
            return
        }
        Logger.logInfo("Game stopped successfully", 2)
    }

    private fun executeDelay() {
        TODO()
    }

    private fun processServerCommand(command: String, args: List<String>) {
        val clientMessage: Message
        Logger.logInfo("Executing server command $command", 2)
        try {
            clientMessage = createCommandMessage(command, args)
        }
        catch (e: InvalidCommand) {
            Logger.logError("Invalid command: $command with args $args", 1)
            CommandLine.logError("Command $command does not exit")
            return
        }
        catch (e: Exception) {
            Logger.logError("Invalid arguments $args for command $command", 1)
            CommandLine.logError("Please provide correct arguments for command $command")
            return
        }
        sendRequest(clientMessage)
        val serverMessage = getResponse()
        try {
            checkResponse(clientMessage, serverMessage)
            executeServerCommand(command, serverMessage)
        }
        catch (e: ExceptionServer) {
            CommandLine.logError(e.message ?: "Server returned an error message")
            return
        }
        catch (e: Exception) {
            Logger.logError("There was an error with some server message", 1)
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
                command = "getUser",
                body = Body(username = args[0])
            )
            "tchau" -> Message(
                type = "command",
                command = "endSession",
            )
            else -> throw InvalidCommand("Command not found")
        }
    }

    private fun executeServerCommand(command: String, serverMessage: Message){
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
        session.login(username = serverMessage.body!!.username!!)
        Logger.logInfo("User ${serverMessage.body.username} logged in", 0)
        CommandLine.logSuccess("User ${session.username} logged in!")
    }

    private fun executeLogoutUser(serverMessage: Message){
        session.logout()
        Logger.logInfo("User ${serverMessage.body!!.username} logged out", 0)
        CommandLine.logSuccess("User ${session.username} logged out")
    }
    private fun executeChangePassword(serverMessage: Message){
        Logger.logInfo("User ${serverMessage.body!!.username} changed the password", 0)
        CommandLine.logSuccess("Password for ${session.username} has been changed successfully!")
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
        val grid = serverMessage.body!!.grid!!

        Logger.logInfo("Received valid grid", 1)

        session.startPlaying(Game(grid))

        Logger.logInfo("Game has been created", 2)
    }

    private fun executeChallenge(serverMessage: Message){
        // start new thread to communicate with the other client
        // serverMessage shall have the port and address of the user
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
        if(serverMessage.status!! >= 400)
            throw ExceptionServer(serverMessage.body!!.info!!)
        if(
            serverMessage.type != "commandAck" ||
            serverMessage.command != clientMessage.command
        )
            throw InvalidMessage("Received a not expected message from server")
    }

    private fun isGameCommand(command: String): Boolean {
        return (command == "atraso") || (command == "move") || (command == "encerra")
    }

    private fun errorMessage(e: Exception): Message {
        Logger.logError("${e.message}", 0)
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