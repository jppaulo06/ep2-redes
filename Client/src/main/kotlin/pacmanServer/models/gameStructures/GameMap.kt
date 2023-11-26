package pacmanServer.models.gameStructures

class GameMap(initialGrid: List<List<Char>>) {
    val rows = initialGrid.size
    val cols = initialGrid[0].size

    private val wallGrid = createBaseGrid(initialGrid)
    private val canvasGrid = copyGrid(initialGrid)

    fun parseGrid(): Triple<Pacman, Ghost, MutableList<Point>> {
        var ghost: Ghost? = null
        val pacman = Pacman(rows / 2, cols / 2, this)
        val points: MutableList<Point> = ArrayList()

        for (row in canvasGrid.indices) {
            for (col in canvasGrid[0].indices) {
                when (canvasGrid[row][col]) {
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

    fun generateGrid(pacman: Pacman, ghost: Ghost, remoteGhost: RemoteGhost?, points: List<Point>): List<List<Char>> {
        clearCanvas()
        for (point in points) {
            if (!point.eaten) canvasGrid[point.position.row][point.position.col] = '.'
        }
        canvasGrid[pacman.position.row][pacman.position.col] = 'C'
        canvasGrid[ghost.position.row][ghost.position.col] = 'F'
        if (remoteGhost != null) {
            canvasGrid[remoteGhost.position.row][remoteGhost.position.col] =
                if (canvasGrid[remoteGhost.position.row][remoteGhost.position.col] == 'F') 'H' else 'f'
        }
        return canvasGrid
    }

    fun isWall(position: Position): Boolean {
        return wallGrid[position.row][position.col] == '*'
    }

    fun getPositionForRemoteGhost(pacman: Pacman, localGhost: Ghost, points: List<Point>): Pair<Int, Int> {
        // Só pra deixar registrado, esse foi o momento em que eu parei de ligar pra boas práticas.
        val grid = generateGrid(pacman, localGhost, null, points)
        for (i in grid.indices) {
            for (j in grid[0].indices) {
                if (grid[i][j] == ' ') {
                    return i to j
                }
            }
        }
        return 0 to 0 // never never never never never never (never) happens
    }

    private fun createBaseGrid(initialGrid: List<List<Char>>): ArrayList<ArrayList<Char>> {
        val baseGrid = ArrayList<ArrayList<Char>>()
        for (row in initialGrid) {
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

    private fun copyGrid(grid: List<List<Char>>): ArrayList<ArrayList<Char>> {
        val copy = ArrayList<ArrayList<Char>>()

        for (row in grid) {
            val newRow = ArrayList<Char>(row)
            copy.add(newRow)
        }

        return copy
    }

    private fun clearCanvas() {
        for (i in 0..<rows) {
            for (j in 0..<cols) {
                canvasGrid[i][j] = wallGrid[i][j]
            }
        }
    }
}
