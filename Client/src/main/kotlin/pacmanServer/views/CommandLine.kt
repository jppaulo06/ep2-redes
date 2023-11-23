package pacmanServer.views

import pacmanServer.models.Game

object CommandLine {

    private const val ERROR_RED = "\u001B[31m"
    private const val WARNING_YELLOW = "\u001B[33m"
    private const val SUCCESS_GREEN = "\u001B[32m"
    private const val COLOR_RESET = "\u001B[0m"

    fun log(message: String) {
        println(message)
    }

    fun logSuccess(message: String){
        println(SUCCESS_GREEN + message + COLOR_RESET)
    }

    fun logWarning(message: String){
        println(WARNING_YELLOW + message + COLOR_RESET)
    }

    fun logError(message: String){
        println(ERROR_RED + message + COLOR_RESET)
    }

    fun logError(e: Exception){
        logError("Something went wrong: ${e.message}")
    }

    fun logGame(game: Game){
        for(line in game.grid){
            for (element in line) {
                print(element)
            }
            println("")
        }
    }

    fun readCommand():Pair<String, List<String>>{
        val commands = readln().split(' ')
        return commands[0] to commands.subList(1, commands.size)
    }

    fun logCLI(){
        print("Pac-Man> ")
    }

    fun logCommandLine(){
        print("Pac-Man> ")
    }
}
