package pacmanServer.structures.errors

class InvalidCommand(message: String) : CustomException(message, 400)