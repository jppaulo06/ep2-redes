package pacmanServer.structures.errors

class InvalidAuthentication(message: String) : CustomException(message, 400)