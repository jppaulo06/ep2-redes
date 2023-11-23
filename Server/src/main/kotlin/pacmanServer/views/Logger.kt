package pacmanServer.views
import Global
import java.io.FileWriter

object Logger {
    private const val logLevel = Global.logLevel
    private const val filePath = Global.logPath
    private val writer: FileWriter = FileWriter(filePath, true)

    private const val ERROR_RED = "\u001B[31m"
    private const val WARNING_YELLOW = "\u001B[33m"
    private const val SUCCESS_GREEN = "\u001B[32m"

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

    fun logSuccess (message: String, level: Int){
        val formattedMessage = "[SUCCESS] $message"
        if (level <= logLevel)
            println(SUCCESS_GREEN + formattedMessage)
        writer.write(formattedMessage)
    }

    fun logWarning(message: String, level: Int){
        val formattedMessage = "[WARNING] $message"
        if (level <= logLevel)
            println(WARNING_YELLOW + formattedMessage)
        writer.write(formattedMessage)
    }

    fun logError(message: String, level: Int){
        val formattedMessage = "[ERROR] $message"
        if (level <= logLevel)
            println(ERROR_RED + formattedMessage)
        writer.write(formattedMessage)
    }

    fun logError(e: Exception, level: Int){
        val formattedMessage = "[ERROR] ${e.message}"
        if (level <= logLevel)
            println(ERROR_RED + formattedMessage)
        writer.write(formattedMessage)
    }
}