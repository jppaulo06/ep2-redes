package pacmanServer.controllers

import pacmanServer.models.structures.Session
import pacmanServer.errors.InvalidAuthentication
import pacmanServer.errors.InvalidCredentials
import pacmanServer.errors.InvalidMessage
import pacmanServer.errors.InvalidUser
import pacmanServer.models.structures.Body
import pacmanServer.models.structures.Message
import pacmanServer.models.UserManager
import pacmanServer.views.Logger
import java.net.InetAddress

class Controller(private val address: InetAddress) {

    private val userManager = UserManager.getInstance()
    private var session: Session = Session(address)

    fun flushSession() {
        if(session.isLoggedIn())
            session.logout()
        session = Session(address)
    }

    fun handle(clientMessage: Message): Message {
        Logger.log("[INFO] Received clientMessage of type ${clientMessage.type}", 2)

        val serverMessage = when (clientMessage.type) {
            "method" -> handleMethod(clientMessage)
            "disconnect" -> handleDisconnect()
            else -> throw InvalidMessage("Received unsupported message type : ${clientMessage.type}")
        }

        Logger.log("[INFO] Finished processing of clientMessage of type ${clientMessage.type}", 2)
        return serverMessage
    }

    private fun handleMethod(clientMessage: Message): Message {
        return when (clientMessage.method) {
            "registerUser" -> handleRegisterUser(clientMessage)
            "loginUser" -> handleLoginUser(clientMessage)
            "logoutUser" -> handleLogoutUser(clientMessage)
            "startGame" -> handleStartGame(clientMessage)
            "stopGame" -> handleStopGame(clientMessage)
            "challengeUser" -> handleChallengeUser(clientMessage)
            "listUsers" -> handleListUsers(clientMessage)
            else -> throw InvalidMessage("Unsupported method: ${clientMessage.method}")
        }
    }

    private fun handleDisconnect(): Message {
        session.logout()
        flushSession()
        return Message(
            type = "disconnectAck:",
            status = 200,
        )
    }

    private fun handleRegisterUser(clientMessage: Message): Message {
        if(clientMessage.body?.username == null || clientMessage.body.password == null){
           throw InvalidMessage("Username or password not passed for registering")
        }

        val username: String = clientMessage.body.username
        val password: String = clientMessage.body.password

        userManager.register(username, password)

        return Message(
            type = "methodAck",
            method = clientMessage.method,
            status = 201,
            body = Body(info = "User created!")
        )
    }

    private fun handleLoginUser(clientMessage: Message): Message {
        if(clientMessage.body?.username == null || clientMessage.body.password == null){
            throw InvalidMessage("Username or password not passed for logging in")
        }

        val username: String = clientMessage.body.username
        val password: String = clientMessage.body.password

        if(userManager.matches(username, password)){
            if(session.isLoggedIn()){
                throw InvalidAuthentication("[ERROR] User is already logged in")
            }

            session.login(username)
            userManager.saveSession(username, session)

            return Message(
                type = "methodAck",
                method = clientMessage.method,
                status = 200,
                body = Body(info = "User logged in!")
            )
        }
        else throw InvalidCredentials("Wrong username or password")
    }

   private fun handleLogoutUser(clientMessage: Message): Message {

       if(session.isOffline()){
            throw InvalidAuthentication("User is not logged in")
       }

       session.logout()
       flushSession()

       return Message(
           type = "methodAck",
           method = clientMessage.method,
           status = 200,
           body = Body(info = "User logged out!")
       )
    }

    private fun handleStartGame(clientMessage: Message): Message {
        if(session.isOffline()){
            throw InvalidAuthentication("User is not logged in")
        }

        session.startPlaying()

        return Message(
            type = "methodAck",
            method = clientMessage.method,
            status = 200,
            body = Body(info = "User is now in playing mode")
        )
    }

    private fun handleStopGame(clientMessage: Message): Message {
        if(session.isOffline()){
            throw InvalidAuthentication("User is not logged in")
        }

        session.stopPlaying()

        return Message(
            type = "methodAck",
            method = clientMessage.method,
            status = 200,
            body = Body(info = "User stopped playing")
        )
    }

    private fun handleChallengeUser(clientMessage: Message): Message {
        if(clientMessage.body?.username == null){
            throw InvalidMessage("Username not passed for challenging")
        }

        val username: String = clientMessage.body.username

        if(!userManager.existsUser(username)){
            throw InvalidUser("User $username does not exist")
        }

        val user = userManager.getUser(username)

        TODO()

        return Message(
            type = "methodAck",
            method = clientMessage.method,
            status = 200,
            body = Body(user = user)
        )
    }

    private fun handleListUsers(clientMessage: Message): Message {
        return Message(
            type = "methodAck",
            method = clientMessage.method,
            status = 200,
            body = Body(users = userManager.getUsers())
        )
    }
}