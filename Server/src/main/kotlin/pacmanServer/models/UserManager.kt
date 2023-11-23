package pacmanServer.models

import pacmanServer.structures.errors.InvalidCredentials
import pacmanServer.structures.Session
import pacmanServer.structures.User
import pacmanServer.structures.UserInfo
import pacmanServer.views.Logger
import java.util.concurrent.ConcurrentHashMap

class UserManager private constructor() {

    companion object {
        @Volatile
        private var instance: UserManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: UserManager().also { instance = it }
            }
    }

    private val users = ConcurrentHashMap<Username, User>()

    fun register(username: Username, password: Password){
        if(users[username] != null) {
            throw InvalidCredentials("Username already exists")
        }
        users[username] = User(username = username, password = password, score = 0)
        Logger.log("[INFO] User $username created", 1)

    }

    fun matches(username: Username, password: Password): Boolean {
        return users[username]?.password == password
    }

    fun changePassword(username: Username, password: Password) {
        users[username]!!.password = password
    }

    fun getUserChallengePort(username: Username) {
        val port = users[username]!!.session!!.port
    }

    fun updateScore(username: Username, newScore: Score) {
        users[username]!!.score = newScore
    }

    fun getScore(username: Username): Score {
        val score = users[username]!!.score
        Logger.log("[INFO] User $username score found", 2)
        return score
    }

    fun getSession(username: String): Session? {
        val session = users[username]!!.session
        Logger.log("[INFO] User $username session found", 2)
        return session
    }

    fun existsUser(username: Username): Boolean {
        return users[username] != null
    }

    fun getUsers(): List<UserInfo> {
        val usersInfo = mutableListOf<UserInfo>()
        for(user in users.values.toList()) {
            usersInfo.add(UserInfo(user))
        }
        return usersInfo
    }

    fun saveSession(username: Username, session: Session){
        users[username]!!.session = session
        Logger.log("[INFO] User $username save session", 2)
    }
}

typealias Username = String
typealias Password = String
typealias Score = Int
