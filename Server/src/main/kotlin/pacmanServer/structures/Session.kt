package pacmanServer.structures

import pacmanServer.structures.errors.InvalidAuthentication
import pacmanServer.structures.errors.InvalidMessage
import java.net.InetAddress

data class Session(
    val address: InetAddress,
    var username: String? = null,
    private var state: SessionState = SessionState.Offline,
    var lastHeartbeat: Int = 0,
    var port: Int = -1
){

    var isGhost = false

    enum class SessionState {
        Offline,
        LoggedIn,
        Playing;

        override fun toString(): String {
            return when(this) {
                Offline -> "offline"
                LoggedIn -> "online"
                Playing -> "playing"
            }
        }
    }

    fun defineChallengePort(port: Int) {
        if(port <= 1000) throw InvalidMessage("Port must not be privileged")
        this.port = port
    }

    fun login(username: String){
        if(state != SessionState.Offline)
            throw InvalidAuthentication("User is logged in")
        state = SessionState.LoggedIn
        this.username = username
    }

    fun logout(){
        state = SessionState.Offline
    }

    fun startPlaying(){
        if(state != SessionState.LoggedIn)
            throw InvalidAuthentication("User is not logged in")
        state = SessionState.Playing
    }

    fun stopPlaying(){
        if(state != SessionState.Playing)
            throw InvalidAuthentication("User is not playing")
        state = SessionState.LoggedIn
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

    fun stateString(): String {
        return state.toString()
    }
}