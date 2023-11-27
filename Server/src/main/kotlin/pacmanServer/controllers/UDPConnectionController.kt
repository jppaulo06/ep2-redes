package pacmanServer.controllers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import pacmanServer.views.Logger
import java.net.InetAddress
import java.util.concurrent.BlockingQueue

class UDPConnectionController(private val udpClientQueue: BlockingQueue<ByteArray>, private val writeQueue: BlockingQueue<ByteArray>, private val address: InetAddress) : Runnable {

    private val controller = Controller(address)

    override fun run() {
        listen()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun listen() {
        val clientMessage: Message

        try {
            clientMessage = readNextMessage()
        }
        catch (e: Exception) {
            return
        }

        val serverMessage: Message = controller.handle(clientMessage)

        try {
            writeMessage(serverMessage)
        }
        catch (e: Exception) {
            return
        }

        if(serverMessage.type == "disconnectAck"){
            return
        }
        listen()
    }

    private fun readNextMessage(): Message {
        val byteArray = udpClientQueue.take()

        val lengthBytes = byteArray.sliceArray(0..< 8)
        val messageLength = lengthBytes.toString(Charsets.US_ASCII).toInt()

        val messageBytes = byteArray.sliceArray(8..< 8 + messageLength)

        Logger.log("[INFO]: receiving datagram of size ${messageLength}, until now ${messageBytes.size}", 2)

        val message: Message = Json.decodeFromString(messageBytes.toString(Charsets.UTF_8))

        Logger.log("[INFO]: message received: ${message.toString()}", 2)
        return message
    }

    private fun writeMessage(message: Message){
        Logger.logInfo("Sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray(Charsets.UTF_8)
        val bytesLength = bytesMessage.size.toString().padStart(8, '0').toByteArray(Charsets.US_ASCII)
        writeQueue.put(bytesLength + bytesMessage)
    }
}