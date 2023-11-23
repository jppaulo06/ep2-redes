package pacmanServer.structures

import kotlinx.serialization.Serializable

@Serializable
data class Body(
    val info: String? = null,
    val username: String? = null,
    val password: String? = null,
    val newPassword: String? = null,
    val address: String? = null,
    val port: Int? = null,
    val score: Int? = null,
    val user: UserInfo? = null,
    val users: List<UserInfo>? = null,
    val grid: List<List<Char>>? = null
)
