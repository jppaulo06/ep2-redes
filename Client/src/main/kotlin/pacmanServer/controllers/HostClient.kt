package pacmanServer.controllers

import Config
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.models.Game
import pacmanServer.models.gameStructures.Direction
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.views.Logger
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess

class HostClient(
    private val remoteMovementQueue: BlockingQueue<Direction>,
    private val remoteSavedGridsQueue: LinkedBlockingQueue<List<List<Char>>>,
    private val game: Game
) : Runnable {

    companion object {

        val port = Config.clientPort

        var serverSocket: ServerSocket = try {
            ServerSocket(port)
        } catch (e: Exception) {
            Logger.log("[ERROR] Could not start server", 0)
            exitProcess(1)
        }
    }

    lateinit var socket: Socket

    override fun run() {
        try {

            Logger.logInfo("Accepting connection", 0)

            socket = serverSocket.accept()
            game.createRemoteGhost()
            Logger.log("Client connected: ${socket.inetAddress.hostAddress}", 1)

            val grid = remoteSavedGridsQueue.take()
            val hostMessage = Message(
                type = "commandAck", command = "game", body = Body(grid = grid)
            )

            try {
                writeMessage(hostMessage)
            } catch (e: Exception) {
                closeConnection(e)
                return
            }

            listen()
        } catch (e: Exception) {
            Logger.logInfo("Sending result", 0)
            sendResult()
        } finally {
            Logger.logInfo("Closing stuff", 0)
        }
    }

    private tailrec fun listen() {

        val clientMessage: Message
        var grid: List<List<Char>>
        var hostMessage: Message

        try {
            clientMessage = readNextMessage()
        } catch (e: Exception) {
            closeConnection(e)
            return
        }

        if (clientMessage.type == "delay") {
            hostMessage = Message(
                type = "delayAck"
            )
            try {
                writeMessage(hostMessage)
            } catch (e: Exception) {
                closeConnection(e)
                return
            }
            return listen()
        }

        // processing for message

        if (clientMessage.body?.direction == null) {
            Logger.logError("Gimme the direction!!!", 0)
            closeConnection()
            return
        }

        grid = remoteSavedGridsQueue.take()
        hostMessage = Message(
            type = "commandAck", command = "game", body = Body(grid = grid)
        )

        try {
            writeMessage(hostMessage)
        } catch (e: Exception) {
            closeConnection(e)
            return
        }

        val direction = clientMessage.body.direction
        remoteMovementQueue.put(direction)

        grid = remoteSavedGridsQueue.take()
        hostMessage = Message(
            type = "commandAck", command = "game", body = Body(grid = grid)
        )

        try {
            writeMessage(hostMessage)
        } catch (e: Exception) {
            closeConnection(e)
            return
        }

        if (direction == Direction.DISCONNECT) {
            closeConnection()
        }

        grid = remoteSavedGridsQueue.take()
        hostMessage = Message(
            type = "commandAck", command = "game", body = Body(grid = grid)
        )

        try {
            writeMessage(hostMessage)
        } catch (e: Exception) {
            closeConnection(e)
            return
        }

        if(!game.ended()) {
            hostMessage = Message(
                type = "ok",
            )
            try {
                writeMessage(hostMessage)
            } catch (e: Exception) {
                closeConnection(e)
                return
            }
        }
        else {
            sendResult()
        }

        listen()
    }

    private fun readNextMessage(): Message {
        val lengthBytes = ByteArray(8)

        var ret = socket.getInputStream().read(lengthBytes)
        if (ret == -1) {
            Logger.logInfo("Connection has ended with client ${socket.inetAddress}:${socket.port}", 0)
            throw Exception()
        }

        val messageLength = lengthBytes.toString(Charsets.UTF_8).toInt()
        val messageBytes = ByteArray(messageLength)

        ret = socket.getInputStream().read(messageBytes)
        if (ret == -1) {
            Logger.logInfo("Connection has ended with client ${socket.inetAddress}:${socket.port}", 0)
            throw Exception()
        }

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.logInfo("Message received: ${message.toString()}", 2)

        return message
    }

    private fun writeMessage(message: Message) {
        Logger.logInfo("Sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
        val bytesLength = bytesMessage.size.toString().padStart(8, '0').toByteArray(Charsets.US_ASCII)
        socket.getOutputStream().write(bytesLength + bytesMessage)
    }

    fun closeConnection(e: Exception? = null) {
        e?.message?.let { Logger.logError(it, 1) }
        Logger.logInfo("Closing socket with ${socket.inetAddress}:${socket.port}", 0)
        socket.close()
    }

    fun sendResult() {
        if (!socket.isClosed) {
            Logger.logInfo("Sending feedback message", 0)
            val msg = if (game.remoteWon()) Message(type = "youWon") else Message(type = "youLost")
            writeMessage(msg)
            socket.close()
        }
    }
}