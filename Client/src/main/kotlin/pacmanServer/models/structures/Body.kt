package pacmanServer.models.structures

import kotlinx.serialization.Serializable

@Serializable
data class Body(
    val info: String? = null,
    val username: String? = null,
    val password: String? = null,
    val user: UserInfo? = null,
    val users: List<UserInfo>? = null
)
