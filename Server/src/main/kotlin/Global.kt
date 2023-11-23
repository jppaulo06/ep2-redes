import pacmanServer.views.Logger
import java.io.File
import kotlin.system.exitProcess

object Global {
    const val serverDefaultPort = 3000
    const val maxDatagramSize = 5000
    const val logLevel = 2
    const val logPath = "src/main/resources/logs_pacman"

    private const val gridPath = "src/main/resources/grid.txt"
    val grid by lazy { readGrid(gridPath) }

    private fun readGrid(filePath: String): List<List<Char>> {
        val lines = File(filePath).readLines()

        val numRows = lines.size
        val numCols = lines.firstOrNull()?.length ?: 0
        val grid = List(numRows) { MutableList(numCols) { ' ' } }

        for (i in 0..<numRows) {
            val line = lines[i]
            if(numCols != line.length) {
                Logger.logError("Grid provided is not valid. Exiting...", 0)
                exitProcess(1)
            }
            for (j in 0..<numCols)
                grid[i][j] = line[j]
        }

        return grid
    }
}