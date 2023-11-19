package pacmanServer.controllers

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.ExperimentalSerializationApi
import pacmanServer.models.structures.Message

class UDPConnectionController(private val channel: Channel<String>) : Runnable {

    public override fun run() {
        TODO()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun listen() {
        TODO()
    }

    private fun closeConnection(e: Exception? = null){
        TODO()
    }

    private fun errorMessage(e: Exception): Message {
        TODO()
    }
}