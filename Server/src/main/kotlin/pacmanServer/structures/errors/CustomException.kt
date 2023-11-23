package pacmanServer.structures.errors

abstract class CustomException(
    message: String = "Something went wrong :(",
    val status: Int = 500
): Exception(message)