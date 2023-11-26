package pacmanServer.controllers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.models.gameStructures.Direction
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.views.CommandLine
import pacmanServer.views.Logger
import java.net.Socket

class RemoteGame(val socket: Socket) {
    // envia movimento
    // recebe mapa
    // printa
    // recebe mapa
    // printa

    var ended = false
    var victory = false

    init {
        val hostMessage: Message
        try {
            hostMessage = readNextMessage()
            val grid = hostMessage.body!!.grid!!
            CommandLine.logGame(grid)
        } catch (e: Exception) {
            closeConnection(e)
        }
    }

    fun endConnection(){
        if(socket.isClosed) return
        val remoteMessage = Message(
            type = "command",
            command = "game",
            body = Body(direction = Direction.DISCONNECT)
        )
        try {
            writeMessage(remoteMessage)
        } catch (e: Exception) {
            closeConnection(e)
            return
        }
        closeConnection()
    }

    fun processRound(direction: Direction) {
        var grids = 3

        val remoteMessage = Message(
            type = "command",
            command = "game",
            body = Body(direction = direction)
        )

        try {
            writeMessage(remoteMessage)
        } catch (e: Exception) {
            closeConnection(e)
            return
        }

        while(grids > 0){
            val hostMessage: Message

            try {
                hostMessage = readNextMessage()
            } catch (e: Exception) {
                closeConnection(e)
                return
            }

            when (hostMessage.type) {
                "youLost" -> {
                    ended = true
                    victory = false
                    return
                }
                "youWon" -> {
                    ended = true
                    victory = true
                    return
                }
                "delay" -> {
                    val remoteMessage = Message(
                        type = "delayAck"
                    )
                    try {
                        writeMessage(remoteMessage)
                    } catch (e: Exception) {
                        closeConnection(e)
                        return
                    }
                }
                else -> {
                    val grid = hostMessage.body!!.grid!!
                    CommandLine.logGame(grid)
                    grids--
                }
            }
        }
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

    private fun closeConnection(e: Exception? = null) {
        e?.message?.let { Logger.logError(it, 1) }
        Logger.logInfo("Closing socket with ${socket.inetAddress}:${socket.port}", 0)
        socket.close()
    }
}