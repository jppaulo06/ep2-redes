package pacmanServer.models.structures

import pacmanServer.models.Score

data class User(
    val username: String,
    var score: Score,
    var session: Session? = null,
)
