package pacmanServer.models.gameStructures

class GameMap(private val initialGrid: List<List<Char>>)
{
    private val grid  = copyGrid(initialGrid)
    private val baseGrid = createBaseGrid()

    val rows = grid.size
    val cols = grid[0].size

    private fun copyGrid(grid: List<List<Char>>): ArrayList<ArrayList<Char>> {
        val copy = ArrayList<ArrayList<Char>>()

        for (row in grid) {
            val newRow = ArrayList<Char>(row)
            copy.add(newRow)
        }

        return copy
    }

    fun parseGrid(): Triple<Pacman, Ghost, MutableList<Point>> {
        var ghost: Ghost? = null
        val pacman = Pacman(grid.size/2, grid[0].size/2, this)
        val points: MutableList<Point> = ArrayList()

        for (row in grid.indices) {
            for (col in grid[0].indices) {
                when (grid[row][col]) {
                    'F' -> {
                        ghost = Ghost(row, col, this)
                    }
                    '.' -> {
                        points.add(Point(row, col))
                    }
                }
            }
        }
        return Triple(pacman, ghost!!, points)
    }

    fun generateGrid(pacman: Pacman, ghost: Ghost, points: List<Point>): List<List<Char>>{
        resetToBaseGrid()
        for(point in points) {
            if(!point.eaten) grid[point.position.row][point.position.col] = '.'
        }
        grid[pacman.position.row][pacman.position.col] = 'C'
        grid[ghost.position.row][ghost.position.col] = 'F'
        return grid
    }

    fun isWall(position: Position): Boolean {
        return baseGrid[position.row][position.col] == '*'
    }

    private fun resetToBaseGrid() {
        for(i in 0..<rows) {
            for(j in 0..<cols) {
                grid[i][j] = baseGrid[i][j]
            }
        }
    }

    private fun createBaseGrid(): ArrayList<ArrayList<Char>> {
        val baseGrid = ArrayList<ArrayList<Char>>()
        for (row in grid) {
            val baseGridRow = ArrayList<Char>()
            for (char in row) {
                when (char) {
                    '*' -> {
                        baseGridRow.add(char)
                    }
                    else -> {
                        baseGridRow.add(' ')
                    }
                }
            }
            baseGrid.add(baseGridRow)
        }
        return baseGrid
    }
}
