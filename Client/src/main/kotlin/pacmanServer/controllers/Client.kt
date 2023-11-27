package pacmanServer.controllers


import Config
import kotlinx.coroutines.*
import pacmanServer.models.Game
import pacmanServer.models.gameStructures.Direction
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.structures.Session
import pacmanServer.structures.errors.*
import pacmanServer.views.*
import java.net.DatagramSocket
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import kotlin.system.exitProcess

class Client(
    serverAddress: String,
    serverPort: Int,
) {

    private val clientControllerQueue: BlockingQueue<Message> = SynchronousQueue()
    private val clientWriterQueue: BlockingQueue<Message> = SynchronousQueue()

    private val readingThread: Thread
    private val writingThread: Thread

    private var session = Session()

    private var tcpSocket: Socket? = null

    private val localMovementQueue: BlockingQueue<Direction> = SynchronousQueue()
    private val remoteMovementQueue: BlockingQueue<Direction> = SynchronousQueue()
    private val localSavedGridsQueue = LinkedBlockingQueue<List<List<Char>>>()
    private val remoteSavedGridsQueue = LinkedBlockingQueue<List<List<Char>>>()

    init {
        if (Config.protocol == "TCP") {
            tcpSocket = Socket(serverAddress, serverPort)
            readingThread =
                Thread(TCPClientReader(tcpSocket!!.getInputStream(), clientControllerQueue, clientWriterQueue))
            writingThread = Thread(TCPClientWriter(tcpSocket!!.getOutputStream(), clientWriterQueue))
        } else {
            val udpSocket = DatagramSocket()
            readingThread =
                Thread(UDPClientReader(udpSocket, clientControllerQueue, clientWriterQueue))
            writingThread = Thread(UDPClientWriter(udpSocket, clientWriterQueue))
        }
    }

    fun start() = runBlocking {
        Logger.logInfo("Starting processing and threads", 2)
        readingThread.start()
        writingThread.start()

        launch() {
            if(Config.protocol != "TCP") this.cancel()
            while (isActive) {
                delay(1000)
                if (!readingThread.isAlive || !writingThread.isAlive) {
                    Logger.logError("Connection closing", 0)
                    killServerThreads()
                    tcpSocket!!.close()
                    this.cancel()
                }
            }
        }
        process()
    }

    private fun killServerThreads() {
        if (readingThread.isAlive) {
            readingThread.interrupt()
        }
        if (writingThread.isAlive) {
            writingThread.interrupt()
        }
    }

    private tailrec fun process() {
        CommandLine.logCommandLine()
        val (command, args) = CommandLine.readCommand()
        Logger.logInfo("Command read $command with args ${args}}", 2)
        processCommand(command, args)
        process()
    }

    private fun processCommand(command: String, args: List<String>) {
        if (isGameCommand(command)) processGameCommand(command, args)
        else processServerCommand(command, args)
    }

    private fun processGameCommand(command: String, args: List<String>) {
        try {
            executeGameCommand(command, args)
        } catch (e: InvalidSessionState) {
            Logger.logError("User tried to execute command $command of playing mode when not playing", 0)
            CommandLine.logError("You can only execute $command when playing!")
            return
        } catch (e: InvalidCommand) {
            Logger.logWarning("Invalid command: $command with args $args", 1)
            CommandLine.logWarning("Command $command does not exit")
            return
        } catch (e: InvalidDirection) {
            Logger.logWarning("Invalid direction ${args[0]}", 1)
            CommandLine.logWarning("Please, provide one of these directions: w, a, s, d")
            return
        } catch (e: Exception) {
            Logger.logWarning("Invalid arguments $args for command $command", 1)
            CommandLine.logError("Please provide correct arguments for command $command")
            return
        }
    }

    private fun executeGameCommand(command: String, args: List<String>) {
        Logger.logInfo("Executing game command $command", 2)
        executeCommandMiddlewares(command)
        when (command) {
            "move" -> executeMove(args[0])
            "encerra" -> executeStopGame()
            "atraso" -> executeDelay()
            else -> InvalidCommand()
        }
    }

    private fun executeMove(directionString: String) {
        val direction: Direction = mapDirection(directionString)
        if (session.isRemotePlaying()) {
            try {
                val game = session.remoteGame!!
                game.processRound(direction)
                if (game.ended) {
                    handleRemoteEndedGame()
                }
            } catch (e: Exception) {
                Logger.logError("Unexpected error processing game round remote", 0)
                Logger.logError(e, 0)
            }
        } else {
            try {
                val x = if (session.game!!.remoteGhost == null || !session.game!!.remotedStarted) 1 else 2
                getGameUpdates(x)
                if (gameHasEnded()) {
                    handleLocalEndedGame()
                    return
                }
                localMovementQueue.put(direction)
                getGameUpdates(1)
                if (gameHasEnded()) {
                    Logger.logInfo("Stopping local game", 0)
                    handleLocalEndedGame()
                    return
                }
            } catch (e: Exception) {
                Logger.logError("Unexpected error processing game round local", 0)
                Logger.logError(e, 0)
            }
        }
    }

    private fun executeStopGame(score: Int = 0) {
        if (session.isRemotePlaying()) {
            session.remoteGame!!.endConnection()
            session.stopPlaying()
            val stopGameMessage = Message(
                type = "command", command = "stopGame", body = Body(score = score)
            )
            sendRequest(stopGameMessage)
            val serverMessage = getResponse()
            try {
                checkResponse(stopGameMessage, serverMessage)
            } catch (e: ExceptionServer) {
                CommandLine.logError(e.message ?: "Server returned an error when closing the game :(")
                Logger.logError(e, 0)
                return
            } catch (e: Exception) {
                Logger.logError("Server returned bad message when stopping the game", 1)
                Logger.logError(e, 1)
                CommandLine.logError("Server returned a bad message (this definitely shouldn't have happened .-.)")
                return
            }
            Logger.logInfo("Game stopped successfully", 2)
        } else {
            //session.hostClient.sendResult()
            session.stopPlaying()
            val stopGameMessage = Message(
                type = "command", command = "stopGame", body = Body(score = score)
            )
            sendRequest(stopGameMessage)
            val serverMessage = getResponse()
            try {
                checkResponse(stopGameMessage, serverMessage)
            } catch (e: ExceptionServer) {
                CommandLine.logError(e.message ?: "Server returned an error when closing the game :(")
                Logger.logError(e, 0)
                return
            } catch (e: Exception) {
                Logger.logError("Server returned bad message when stopping the game", 1)
                Logger.logError(e, 1)
                CommandLine.logError("Server returned a bad message (this definitely shouldn't have happened .-.)")
                return
            }
            Logger.logInfo("Game stopped successfully", 2)
        }
    }

    private fun executeDelay() {
        CommandLine.logError("Nothing happens. Did not implement that, sir")
    }

    private fun processServerCommand(command: String, args: List<String>) {
        val clientMessage: Message
        Logger.logInfo("Executing server command $command", 2)

        if (command == "tchau" && session.isOffline()) exitProcess(0)

        try {
            executeCommandMiddlewares(command)
            clientMessage = createCommandMessage(command, args)
        } catch (e: InvalidSessionState) {
            CommandLine.logWarning("You can't execute $command now!")
            Logger.logWarning("User tried to execute not authorized command", 0)
            return
        } catch (e: InvalidCommand) {
            Logger.logWarning("Invalid command: $command with args $args", 1)
            CommandLine.logWarning("Command $command does not exit")
            return
        } catch (e: Exception) {
            Logger.logWarning("Invalid arguments $args for command $command", 1)
            CommandLine.logWarning("Please provide correct arguments for command $command")
            return
        }

        sendRequest(clientMessage)
        val serverMessage = getResponse()

        try {
            checkResponse(clientMessage, serverMessage)
            executeServerCommand(command, serverMessage)
        } catch (e: ExceptionServer) {
            CommandLine.logError(e.message ?: "Server returned an error :(")
            Logger.logError(e, 0)
            return
        } catch (e: Exception) {
            Logger.logError("There was an error with some server message", 1)
            Logger.logError(e, 1)
            CommandLine.logError("There was an error when communicating with the server :(")
            return
        }
    }

    private fun createCommandMessage(command: String, args: List<String>): Message {
        return when (command) {
            "novo" -> {
                checkUserIsOffline()
                return Message(
                    type = "command", command = "registerUser", body = Body(username = args[0], password = args[1])
                )
            }

            "senha" -> {
                checkUserIsLoggedIn()
                return Message(
                    type = "command", command = "changePassword", body = Body(password = args[0], newPassword = args[1])
                )
            }

            "entra" -> {
                checkUserIsOffline()
                return Message(
                    type = "command",
                    command = "loginUser",
                    body = Body(username = args[0], password = args[1], port = session.port)
                )
            }

            "tchau", "sai" -> {
                checkUserIsLoggedIn()
                return Message(
                    type = "command",
                    command = "logoutUser",
                )
            }

            "l", "lideres" -> Message(
                type = "command",
                command = "listUsers",
            )

            "inicia" -> {
                checkUserIsLoggedIn()
                checkUserIsNotPlaying()
                return Message(
                    type = "command",
                    command = "startGame",
                )
            }

            "desafio" -> {
                checkUserIsLoggedIn()
                checkUserIsNotPlaying()
                return Message(
                    type = "command", command = "challengeUser", body = Body(username = args[0])
                )
            }

            else -> throw InvalidCommand("Command not found")
        }
    }

    private fun executeServerCommand(command: String, serverMessage: Message) {
        when (command) {
            "novo" -> executeRegisterUser(serverMessage)
            "senha" -> executeChangePassword(serverMessage)
            "entra" -> executeLoginUser(serverMessage)
            "sai" -> executeLogoutUser(serverMessage)
            "lideres" -> executeRanking(serverMessage)
            "l" -> executeListUsers(serverMessage)
            "inicia" -> executeStartGame(serverMessage)
            "desafio" -> executeChallenge(serverMessage)
            "tchau" -> executeEndSession()
            else -> throw InvalidCommand("Command not found")
        }
    }

    private fun executeRegisterUser(serverMessage: Message) {
        Logger.logInfo("User ${serverMessage.body!!.username} created successfully", 0)
        CommandLine.logSuccess("User ${serverMessage.body.username} created successfully!")
    }

    private fun executeLoginUser(serverMessage: Message) {
        session.login(username = serverMessage.body!!.username!!)
        Logger.logInfo("User ${serverMessage.body.username} logged in", 0)
        CommandLine.logSuccess("User ${session.username} logged in!")
    }

    private fun executeLogoutUser(serverMessage: Message) {
        Logger.logInfo("User ${serverMessage.body!!.username} logged out", 0)
        CommandLine.logSuccess("User ${session.username} logged out")
        session.logout()
    }

    private fun executeChangePassword(serverMessage: Message) {
        Logger.logInfo("User ${serverMessage.body!!.username} changed the password", 0)
        CommandLine.logSuccess("Password for ${session.username} has been changed successfully!")
    }

    private fun executeRanking(serverMessage: Message) {
        if (serverMessage.body!!.users!!.isEmpty()) {
            CommandLine.logWarning("No users registered")
            return
        }
        val rankedUsers = serverMessage.body.users!!.sortedBy { it.score }.reversed()
        for ((rank, user) in rankedUsers.withIndex()) {
            CommandLine.log((rank + 1).toString() + ": " + user.username + " " + user.state + " " + user.score + " points")
        }
    }

    private fun executeListUsers(serverMessage: Message) {
        if (serverMessage.body!!.users!!.isEmpty()) {
            CommandLine.logWarning("No users registered")
            return
        }
        for (user in serverMessage.body.users!!) {
            CommandLine.log(user.username + " " + user.state)
        }
    }

    private fun executeStartGame(serverMessage: Message) {
        val grid = serverMessage.body!!.grid!!

        Logger.logInfo("Received grid", 1)

        val game = Game(
            grid, localMovementQueue, remoteMovementQueue, localSavedGridsQueue, remoteSavedGridsQueue
        )

        session.startPlaying(game)

        val hostClient = HostClient(remoteMovementQueue, remoteSavedGridsQueue, session.game!!)
        val hostThread = Thread(hostClient)
        hostThread.start()
        session.hostThread = hostThread
        session.hostClient = hostClient

        getGameUpdates(1)

        Logger.logInfo("Game has been created", 0)
    }

    private fun executeChallenge(serverMessage: Message) {
        val hostSocket: Socket = Socket(serverMessage.body!!.address!!.substring(1), serverMessage.body.port!!)
        val remoteGame = RemoteGame(hostSocket)

        session.startRemotePlaying(remoteGame)

        val clientMessage = Message(
            type = "command",
            command = "startGame",
        )

        sendRequest(clientMessage)
        val res = getResponse()

        try {
            checkResponse(clientMessage, res)
        } catch (e: ExceptionServer) {
            CommandLine.logError(e.message ?: "Server returned an error :(")
            Logger.logError(e, 0)
            return
        } catch (e: Exception) {
            Logger.logError("There was an error with some server message", 1)
            Logger.logError(e, 1)
            CommandLine.logError("There was an error when communicating with the server :(")
            return
        }
    }

    private fun executeEndSession() {
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
        if (serverMessage.status!! >= 400) throw ExceptionServer(serverMessage.body!!.info!!)
        if (serverMessage.type != "commandAck" || serverMessage.command != clientMessage.command) throw InvalidMessage("Received a not expected message from server")
    }

    private fun isGameCommand(command: String): Boolean {
        return (command == "atraso") || (command == "move") || (command == "encerra")
    }

    private fun executeCommandMiddlewares(command: String) {
        return when (command) {
            "entra", "novo" -> checkUserIsOffline()
            "senha", "tchau", "sai" -> checkUserIsLoggedIn()
            "inicia", "desafio" -> {
                checkUserIsLoggedIn()
                checkUserIsNotPlaying()
            }

            "move", "encerra", "atraso" -> checkUserIsPlaying()
            else -> Unit
        }
    }

    private fun checkUserIsLoggedIn() {
        if (!session.isLoggedIn()) {
            throw InvalidSessionState()
        }
    }

    private fun checkUserIsPlaying() {
        if (!session.isPlaying() && !session.isRemotePlaying()) throw InvalidSessionState()
    }

    private fun checkUserIsNotPlaying() {
        if (session.isPlaying()) throw InvalidSessionState()
    }

    private fun checkUserIsOffline() {
        if (!session.isOffline()) throw InvalidSessionState()
    }

    private fun mapDirection(directionString: String) = when (directionString) {
        "w" -> Direction.UP
        "s" -> Direction.DOWN
        "a" -> Direction.LEFT
        "d" -> Direction.RIGHT
        else -> throw InvalidDirection()
    }

    private fun handleRemoteEndedGame() {
        var score = 0
        val victory = session.remoteGame!!.victory
        if (victory) {
            CommandLine.logRemoteWonGame()
            score = 1
        } else CommandLine.logRemoteLostGame()
        executeStopGame(score)
    }

    private fun handleLocalEndedGame() {
        var score = 0
        val victory = session.game!!.localWon()
        if (victory) {
            CommandLine.logLocalWonGame()
            score = 1
        } else CommandLine.logLocalLostGame()
        executeStopGame(score)
    }

    private fun gameHasEnded() = session.game!!.ended()

    private fun getGameUpdates(updates: Int) {
        for (i in 0..<updates) {
            val grid = localSavedGridsQueue.take()
            CommandLine.logGame(grid)
        }
    }
}