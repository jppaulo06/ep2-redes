package pacmanServer.views
import Global
import java.io.FileWriter

object Logger {
    private const val logLevel = Global.logLevel
    private const val filePath = Global.logPath

    private val fileWriter: FileWriter

    private const val ERROR_RED = "\u001B[31m"
    private const val WARNING_YELLOW = "\u001B[33m"
    private const val SUCCESS_GREEN = "\u001B[32m"
    private const val COLOR_RESET = "\u001B[0m"

    init {
        try {
            fileWriter = FileWriter(filePath, true)
        }
        catch (e: Exception){
            throw Exception("Could not open Logger file")
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            fileWriter.close()
        })
    }

    fun log(message: String, level: Int) {
        if (level <= logLevel)
            println(message)
        fileWriter.write(message + "\n")
        fileWriter.flush()
    }

    fun logInfo(message: String, level: Int){
        val formattedMessage = "[INFO] $message"
        if (level <= logLevel)
            println(formattedMessage)
        fileWriter.write(formattedMessage + "\n")
        fileWriter.flush()
    }

    fun logSuccess (message: String, level: Int){
        val formattedMessage = "[SUCCESS] $message"
        if (level <= logLevel)
            println(SUCCESS_GREEN + formattedMessage + COLOR_RESET)
        fileWriter.write(formattedMessage + "\n")
        fileWriter.flush()
    }

    fun logWarning(message: String, level: Int){
        val formattedMessage = "[WARNING] $message"
        if (level <= logLevel)
            println(WARNING_YELLOW + formattedMessage + COLOR_RESET)
        fileWriter.write(formattedMessage + "\n")
        fileWriter.flush()
    }

    fun logError(message: String, level: Int){
        val formattedMessage = "[ERROR] $message"
        if (level <= logLevel)
            println(ERROR_RED + formattedMessage + COLOR_RESET)
        fileWriter.write(formattedMessage + "\n")
        fileWriter.flush()
    }

    fun logError(e: Exception, level: Int){
        val formattedMessage = "[ERROR] ${e.message}"
        if (level <= logLevel)
            println(ERROR_RED + formattedMessage + COLOR_RESET)
        fileWriter.write(formattedMessage + "\n")
        fileWriter.flush()
    }
}