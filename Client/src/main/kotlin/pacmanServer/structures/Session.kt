package pacmanServer.structures

import Config
import pacmanServer.controllers.HostClient
import pacmanServer.controllers.RemoteGame
import pacmanServer.models.Game
import pacmanServer.structures.errors.InvalidMessage
import pacmanServer.structures.errors.InvalidSessionState
import pacmanServer.views.Logger

data class Session(
    var username: String? = null,
    private var state: SessionState = SessionState.Offline,
    var port: Int = Config.clientDefaultPort,
    var game: Game? = null,
    var gameThread: Thread? = null,
    var hostThread: Thread? = null
) {

    var remoteGame: RemoteGame? = null
    var hostClient: HostClient? = null

    enum class SessionState {
        Offline,
        LoggedIn,
        Playing,
        RemotePlaying;

        override fun toString(): String {
            return when (this) {
                Offline -> "offline"
                LoggedIn -> "online"
                Playing -> "playing"
                RemotePlaying -> "remote"
            }
        }
    }

    fun defineChallengePort(port: Int) {
        if (port <= 1000) throw InvalidMessage("Port must not be privileged")
        this.port = port
    }

    fun login(username: String) {
        if (state != SessionState.Offline)
            throw InvalidSessionState()
        state = SessionState.LoggedIn
        this.username = username
    }

    fun logout() {
        username = null
        state = SessionState.Offline
    }

    fun startRemotePlaying(remoteGame: RemoteGame) {
        if (state != SessionState.LoggedIn)
            throw InvalidSessionState()

        state = SessionState.RemotePlaying
        this.remoteGame = remoteGame
    }

    fun startPlaying(game: Game) {
        if (state != SessionState.LoggedIn)
            throw InvalidSessionState()

        state = SessionState.Playing
        this.game = game
        this.gameThread = Thread(game)
        gameThread!!.start()
    }

    fun stopPlaying() {
        if (state != SessionState.Playing && state != SessionState.RemotePlaying)
            throw InvalidSessionState()
        state = SessionState.LoggedIn


        gameThread?.interrupt()
        hostThread?.interrupt()

        //hostClient?.closeConnection()
        //hostClient?.serverSocket!!.close()

        // hostClient?.closeSockets()

        Logger.logInfo("Stopping host thread", 0)


        game = null
    }

    fun isOffline(): Boolean {
        return state == SessionState.Offline || username == null
    }

    fun isLoggedIn(): Boolean {
        return state == SessionState.LoggedIn || state == SessionState.Playing
    }

    fun isPlaying(): Boolean {
        return state == SessionState.Playing
    }

    fun isRemotePlaying(): Boolean {
        return state == SessionState.RemotePlaying
    }
}