package pacmanServer.views
import Config
import java.io.FileWriter

object Logger {
    private const val logLevel = Config.logLevel
    private const val filePath = Config.logPath
    private val writer: FileWriter = FileWriter(filePath, true)

    private const val ERROR_RED = "\u001B[31m"
    private const val WARNING_YELLOW = "\u001B[33m"
    private const val COLOR_RESET = "\u001B[0m"

    fun log(message: String, level: Int) {
        if (level <= logLevel)
            println(message)
        writer.write(message)
    }

    fun logInfo(message: String, level: Int){
        val formattedMessage = "[INFO] $message"
        if (level <= logLevel)
            println(formattedMessage)
        writer.write(message)
    }

    fun logWarning(message: String, level: Int){
        val formattedMessage = "[WARNING] $message"
        if (level <= logLevel)
            println(WARNING_YELLOW + formattedMessage + COLOR_RESET)
        writer.write(formattedMessage)
    }

    fun logError(message: String, level: Int){
        val formattedMessage = "[ERROR] $message"
        if (level <= logLevel)
            println(ERROR_RED + formattedMessage + COLOR_RESET)
        writer.write(formattedMessage)
    }

    fun logError(e: Exception, level: Int){
        val formattedMessage = "[ERROR] ${e.message}"
        if (level <= logLevel)
            println(ERROR_RED + formattedMessage + COLOR_RESET)
        writer.write(formattedMessage)
    }
}