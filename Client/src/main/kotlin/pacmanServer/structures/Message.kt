package pacmanServer.structures

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val type: String,
    val command: String? = null,
    val status: Int? = null,
    val body: Body? = null
)