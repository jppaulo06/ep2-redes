package pacmanServer.models.structures

import pacmanServer.errors.InvalidAuthentication
import java.net.InetAddress

data class Session(
    val address: InetAddress,
    var username: String? = null,
    var state: SessionState = SessionState.Offline,
    var lastHeartbeat: Int = 0
){

    enum class SessionState {
        Offline,
        LoggedIn,
        Playing,
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
        return when(state) {
            SessionState.Offline -> "offline"
            SessionState.LoggedIn -> "loggedIn"
            SessionState.Playing -> "playing"
        }
    }
}