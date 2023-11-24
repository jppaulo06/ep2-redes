package pacmanServer.models

import Global
import pacmanServer.structures.Session
import pacmanServer.structures.User
import pacmanServer.structures.UserInfo
import pacmanServer.structures.errors.InvalidCredentials
import pacmanServer.views.Logger
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

object UserManager {

    private val users = ConcurrentHashMap<Username, User>()
    private val fileReader: FileReader
    private val fileWriter: FileWriter

    init {
        try {
            val usersFile = File(Global.usersFilePath)
            fileReader = FileReader(usersFile)
            loadUsersFromFile()
            fileReader.close()
            fileWriter = FileWriter(usersFile)
        }
        catch (e: Exception){
            Logger.logError("There was an error loading users file. Exiting...", 0)
            exitProcess(1)
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            saveUsersToFile()
            fileWriter.close()
        })
    }

    fun load() = Unit

    private fun loadUsersFromFile() {
        val lines = fileReader.readLines()

        for (line in lines) {
            if (line[0] == '#') continue
            val values = line.split(' ')
            val username = values[0]
            val password = values[1]
            val score = values[2].toInt()
            Logger.logInfo("Loading user $username", 2)
            users[username] = User(username = username, password = password, score = score)
        }
        Logger.logInfo("Users loaded successfully", 1)
    }

    private fun saveUsersToFile(){
        fileWriter.append(
            "# This file is generated automatically for the pacman server\n" +
                    "# Please DON'T CHANGE IT!\n" +
                    "# user password score\n"
        )
        for(user in users.values){
            fileWriter.append(user.username + " " + user.password + " " + user.score + "\n")
        }
    }

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
        Logger.logInfo("Incrementing score of $username by $newScore", 2)
        users[username]!!.score += newScore
    }

    fun getScore(username: Username): Score {
        val score = users[username]!!.score
        Logger.logInfo("User $username score found", 2)
        return score
    }

    fun getSession(username: String): Session? {
        val session = users[username]!!.session
        Logger.logInfo("User $username session found", 2)
        return session
    }

    fun existsUser(username: Username): Boolean {
        return users[username] != null
    }

    fun getUsers(): List<UserInfo> {
        Logger.logInfo("Getting all users", 2)
        val usersInfo = ArrayList<UserInfo>()
        for(user in users.values.toList()) {
            usersInfo.add(UserInfo(user))
        }
        return usersInfo
    }

    fun saveSession(username: Username, session: Session){
        users[username]!!.session = session
        Logger.logInfo("User $username save session", 2)
    }
}

typealias Username = String
typealias Password = String
typealias Score = Int
