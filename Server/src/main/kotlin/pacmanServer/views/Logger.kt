package pacmanServer.views
import java.io.FileWriter

class Logger private constructor() {

    private val logLevel = Config.logLevel
    private val filePath = Config.logPath
    private val writer: FileWriter = FileWriter(filePath, true)

    companion object {
        private val instance: Logger by lazy { Logger() }
        fun log(message: String, level: Int) {
            if (level <= instance.logLevel)
                println(message)
            instance.writer.write(message)
        }
    }
}