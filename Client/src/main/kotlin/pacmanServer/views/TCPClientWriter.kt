package pacmanServer.views

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pacmanServer.structures.Message
import java.io.OutputStream
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class TCPClientWriter(
    private val outputStream: OutputStream,
    private val clientWriterQueue: BlockingQueue<Message>
): Runnable {

    private fun <T> BlockingQueue<T>.blockingPeek(timeout: Long): T {
        while(true){
            val item = poll(timeout, TimeUnit.MILLISECONDS)
            if(item != null)
                return item
        }
    }

    override fun run() {
        Logger.logInfo("Starting writer thread", 0)
        write()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun write(){
        val clientMessage = clientWriterQueue.take()

        try {
            writeMessage(clientMessage)
        }
        catch (e: Exception) {
            Logger.logError("Failed to send message", 0)
            return
        }
        write()
    }

    private fun writeMessage(message: Message){
        Logger.logInfo("Sending message ${Json.encodeToString(message)}", 2)
        val bytesMessage = Json.encodeToString(message).toByteArray()
        val bytesLength = bytesMessage.size.toString().padStart(4, '0').toByteArray()
        outputStream.write(bytesLength + bytesMessage)
    }
}
