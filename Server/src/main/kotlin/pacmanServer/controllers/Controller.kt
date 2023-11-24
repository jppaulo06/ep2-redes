package pacmanServer.controllers

import Global
import pacmanServer.models.UserManager
import pacmanServer.structures.Body
import pacmanServer.structures.Message
import pacmanServer.structures.Session
import pacmanServer.structures.errors.*
import pacmanServer.views.Logger
import java.net.InetAddress

class Controller(private val address: InetAddress) {

    private var session: Session = Session(address)

    fun flushSession() {
        if(session.isLoggedIn())
            session.logout()
        session = Session(address)
    }

    fun handle(clientMessage: Message): Message {
        Logger.log("[INFO] Received clientMessage of type ${clientMessage.type}", 2)

        return try {
            when (clientMessage.type) {
                "command" -> handleMethod(clientMessage)
                "disconnect" -> handleDisconnect()
                else -> throw InvalidMessage("Received unsupported message type : ${clientMessage.type}")
            }
        }
        catch (e: CustomException){
            errorMessage(e)
        }
        catch (e: Exception){
            errorMessage(e)
        }
    }

    private fun handleMethod(clientMessage: Message) = when (clientMessage.command) {
        "registerUser" -> handleRegisterUser(clientMessage)
        "loginUser" -> handleLoginUser(clientMessage)
        "logoutUser" -> handleLogoutUser(clientMessage)
        "changePassword" -> handleChangePassword(clientMessage)
        "startGame" -> handleStartGame(clientMessage)
        "stopGame" -> handleStopGame(clientMessage)
        "challengeUser" -> handleChallengeUser(clientMessage)
        "listUsers" -> handleListUsers(clientMessage)
        "updateScore" -> handleUpdateScore(clientMessage)
        else -> throw InvalidMessage("Unsupported command: ${clientMessage.command}")
    }

    private fun handleRegisterUser(clientMessage: Message): Message {
        if(clientMessage.body?.username == null || clientMessage.body.password == null){
           throw InvalidMessage("Username or password not passed for registering")
        }

        val username: String = clientMessage.body.username
        val password: String = clientMessage.body.password

        UserManager.register(username, password)

        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 201,
            body = Body(info = "User created!", username = username)
        )
    }

    private fun handleLoginUser(clientMessage: Message): Message {
        if(
            clientMessage.body?.username == null ||
            clientMessage.body.password == null ||
            clientMessage.body.port == null
            ){
            throw InvalidMessage("Username, password or port not passed for logging in")
        }

        val username: String = clientMessage.body.username
        val password: String = clientMessage.body.password
        val port: Int = clientMessage.body.port

        if(UserManager.matches(username, password)){
            if(session.isLoggedIn()){
                throw InvalidAuthentication("[ERROR] User is already logged in")
            }

            session.defineChallengePort(port)
            session.login(username)
            UserManager.saveSession(username, session)

            return Message(
                type = "commandAck",
                command = clientMessage.command,
                status = 200,
                body = Body(info = "User logged in!", username=username)
            )
        }
        else throw InvalidCredentials("Wrong username or password")
    }

   private fun handleLogoutUser(clientMessage: Message): Message {
       if(session.isOffline()){
            throw InvalidAuthentication("User is not logged in")
       }

       val username = session.username

       session.logout()
       flushSession()

       return Message(
           type = "commandAck",
           command = clientMessage.command,
           status = 200,
           body = Body(info = "User logged out!", username = username)
       )
    }

    private fun handleChangePassword(clientMessage: Message): Message {
        if(clientMessage.body?.password == null || clientMessage.body.newPassword == null){
            throw InvalidMessage("Username or password not passed for registering")
        }

        if(session.isOffline()){
            throw InvalidAuthentication("User is not logged in")
        }

        if(!UserManager.matches(session.username!!, clientMessage.body.password)){
            throw InvalidAuthentication("Password doesn't match current password")
        }

        UserManager.changePassword(session.username!!, clientMessage.body.newPassword)

        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 200,
            body = Body(info = "Password has been changed!", username = session.username)
        )
    }

    private fun handleStartGame(clientMessage: Message): Message {
        if(session.isOffline()){
            throw InvalidAuthentication("User is not logged in")
        }

        session.startPlaying()

        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 200,
            body = Body(info = "User is now in playing mode", grid = Global.grid)
        )
    }

    private fun handleStopGame(clientMessage: Message): Message {
        if(clientMessage.body?.score == null){
            throw InvalidMessage("Score not provided when finishing game")
        }

        val score = clientMessage.body.score

        if(!session.isPlaying()){
            throw InvalidAuthentication("User is not playing")
        }

        session.stopPlaying()
        UserManager.updateScore(session.username!!, score)

        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 200,
            body = Body(info = "User stopped playing")
        )
    }

    private fun handleChallengeUser(clientMessage: Message): Message {
        if(clientMessage.body?.username == null){
            throw InvalidMessage("Username not passed for challenging")
        }

        val username: String = clientMessage.body.username

        // TODO: Verify if the user is not already playing with someone. There is a lot of logic to implement
        // of remote playing yet...

        if(!UserManager.existsUser(username)){
            throw InvalidUser("User $username does not exist")
        }

        val session = UserManager.getSession(username)!!

        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 200,
            body = Body(username = session.username, address = session.address.toString(), port = session.port)
        )
    }

    private fun handleListUsers(clientMessage: Message): Message {
        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 200,
            body = Body(users = UserManager.getUsers())
        )
    }

    private fun handleDisconnect(): Message {
        session.logout()
        flushSession()
        return Message(
            type = "disconnectAck",
            status = 200,
        )
    }

    private fun handleUpdateScore(clientMessage: Message): Message {
        if(clientMessage.body?.score == null){
            throw InvalidMessage("New score was not provided")
        }
        if(session.isOffline()){
           throw InvalidAuthentication("User is not logged in")
        }
        val score = clientMessage.body.score
        UserManager.updateScore(session.username!!, score)
        return Message(
            type = "commandAck",
            command = clientMessage.command,
            status = 200,
            body = Body(info = "Score has been updated!")
        )
    }

    private fun errorMessage(e: Exception): Message {
        Logger.log("[ERROR]: ${e.message}", 0)
        Logger.log("[ERROR]: Responding client with error message...", 0)
        return when(e) {
            is CustomException -> Message(
                type = "error",
                status = e.status,
                body = Body(info = e.message)
            )
            else -> Message(
                type = "error",
                status = 500,
                body = Body(info = "There was an unexpected error :(")
            )
        }
    }
}