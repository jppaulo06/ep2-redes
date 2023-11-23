package pacmanServer.structures

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val state: String,
    val username: String,
    val score: Int
)
