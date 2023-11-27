package pacmanServer.controllers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import pacmanServer.views.Logger
import java.net.Socket

class TCPConnectionController(private val socket: Socket) : Runnable {

    private val controller = Controller(socket.inetAddress)

    public override fun run() {
        Logger.logInfo("Connected: ${socket.inetAddress}:${socket.port}", 0)
        listen()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = readNextMessage()
        }
        catch (e: Exception) {
            closeConnection(e)
            return
        }

        val serverMessage: Message = controller.handle(clientMessage)

        try {
            writeMessage(serverMessage)
        }
        catch (e: Exception) {
            closeConnection(e)
            return
        }

        if(serverMessage.type == "disconnectAck"){
            closeConnection()
            return
        }
        listen()
    }

    private fun readNextMessage(): Message {
        val lengthBytes = ByteArray(8)

        var ret = socket.getInputStream().read(lengthBytes)
        if(ret == -1) {
            Logger.logInfo("Connection has ended with client ${socket.inetAddress}:${socket.port}", 0)
            throw Exception()
        }

        val messageLength = lengthBytes.toString(Charsets.UTF_8).toInt()
        val messageBytes = ByteArray(messageLength)

        ret = socket.getInputStream().read(messageBytes)
        if(ret == -1) {
            Logger.logInfo("Connection has ended with client ${socket.inetAddress}:${socket.port}", 0)
            throw Exception()
        }

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.logInfo("Message received: ${message.toString()}", 2)

        return message
    }

    private fun writeMessage(message: Message){
        Logger.logInfo("Sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
        val bytesLength = bytesMessage.size.toString().padStart(8, '0').toByteArray(Charsets.US_ASCII)
        socket.getOutputStream().write(bytesLength + bytesMessage)
    }

    private fun closeConnection(e: Exception? = null){
        e?.message?.let { Logger.logError(it, 1) }
        Logger.logInfo("Closing socket with ${socket.inetAddress}:${socket.port}", 0)
        controller.flushSession()
        socket.close()
    }
}