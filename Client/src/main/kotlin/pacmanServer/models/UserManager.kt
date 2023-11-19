package pacmanServer.models

import pacmanServer.models.structures.Session
import pacmanServer.errors.InvalidCredentials
import pacmanServer.models.structures.User
import pacmanServer.models.structures.UserInfo
import pacmanServer.views.Logger
import java.util.concurrent.ConcurrentHashMap

class UserManager private constructor() {

    companion object {
        private val instance: UserManager by lazy { UserManager() }

        fun getInstance(): UserManager{
            return instance
        }
    }

    private val users = ConcurrentHashMap<Username, User>()
    private val passwords = ConcurrentHashMap<Username, Password>()

    fun register(username: Username, password: Password){
        if(users[username] != null) {
            throw InvalidCredentials("Username already exists")
        }
        users[username] = User(username = username, score = 0)
        passwords[username] = password
        Logger.log("[INFO] User $username created", 1)
    }

    fun matches(username: Username, password: Password): Boolean {
        return passwords[username] == password
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

    fun getUser(username: Username): UserInfo {
        return UserInfo(users[username]!!)
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
