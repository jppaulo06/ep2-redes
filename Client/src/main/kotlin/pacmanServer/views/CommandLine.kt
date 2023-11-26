package pacmanServer.views

object CommandLine {

    private const val ERROR_RED = "\u001B[31m"
    private const val WARNING_YELLOW = "\u001B[33m"
    private const val SUCCESS_GREEN = "\u001B[32m"
    private const val COLOR_RESET = "\u001B[0m"

    fun log(message: String) {
        println(message)
    }

    fun logSuccess(message: String) {
        println(SUCCESS_GREEN + message + COLOR_RESET)
    }

    fun logWarning(message: String) {
        println(WARNING_YELLOW + message + COLOR_RESET)
    }

    fun logError(message: String) {
        println(ERROR_RED + message + COLOR_RESET)
    }

    fun readCommand(): Pair<String, List<String>> {
        val commands = readln().split(' ')
        return commands[0] to commands.subList(1, commands.size)
    }

    fun logCommandLine() {
        print("Pac-Man> ")
    }

    fun clear() {
        print("\u001b[H\u001b[2J")
    }

    fun logGame(grid: List<List<Char>>) {
        println("")
        for (line in grid) {
            for (element in line) {
                print(element)
            }
            println("")
        }
    }

    fun logLocalWonGame() {
        logSuccess("Congrats! You won the game!")
        Logger.logInfo("Pacman (local) won the game", 0)
    }

    fun logLocalLostGame() {
        logError("You lost ;-; Skill issue, not my problem")
        Logger.logInfo("Pacman (local) lost the game", 0)
    }

    fun logRemoteWonGame() {
        logSuccess("Congrats! You invaded the game and shattered Pacman happiness successfully! :D")
        Logger.logInfo("Remote Ghost won the game", 0)
    }

    fun logRemoteLostGame() {
        logError("You lost ;-; Skill issue, not my problem")
        Logger.logInfo("Remote Ghost lost the game", 0)
    }
}
