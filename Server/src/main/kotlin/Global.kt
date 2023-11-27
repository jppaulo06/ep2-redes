
import pacmanServer.structures.errors.InvalidGrid
import java.io.File

object Global {
    const val serverDefaultPort = 3000
    const val clientDefaultPort = 3001
    const val maxDatagramSize = 5000
    const val logLevel = 2
    const val logPath = "src/main/resources/logs_pacman"
    const val usersFilePath = "src/main/resources/users_pacman"

    private const val gridPath = "src/main/resources/test_grid_pacman"
    val grid: List<List<Char>>

    init {
        grid = readGrid(gridPath)
        if(!gridIsValid()) throw InvalidGrid()
    }

    fun load() = Unit

    private fun readGrid(filePath: String): List<List<Char>> {
        val lines = File(filePath).readLines()

        val numRows = lines.size
        val numCols = lines.firstOrNull()?.length ?: 0
        val grid = List(numRows) { MutableList(numCols) { ' ' } }

        for (i in 0..<numRows) {
            val line = lines[i]
            for (j in line.indices)
                grid[i][j] = line[j]
        }

        return grid
    }

    private fun gridIsValid(): Boolean{
        var localGhost = false
        val numRows = grid.size
        val numCols = grid[0].size

        for(row in grid){
            if(row.size != numCols) return false
            for(char in row){
                when(char){
                    'F' -> {
                        if(localGhost) return false
                        localGhost = true
                    }
                    '*', '.', ' ' -> continue
                    else -> return false
                }
            }
        }

        return localGhost && grid[numRows/2][numCols/2] == ' '
    }
}