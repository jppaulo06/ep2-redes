package pacmanServer.structures

import pacmanServer.models.Score

data class User(
    val username: String,
    var password: String,
    var score: Score,
    var session: Session? = null,
)
