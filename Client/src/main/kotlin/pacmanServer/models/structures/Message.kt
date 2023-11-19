package pacmanServer.models.structures

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val type: String,
    val method: String? = null,
    val status: Int? = null,
    val body: Body? = null
)