package pacmanServer.models

import pacmanServer.models.gameStructures.*
import pacmanServer.structures.errors.InvalidPosition
import pacmanServer.views.CommandLine
import pacmanServer.views.Logger

class Game(grid: List<List<Char>>) {

    private var pacman: Pacman
    private var localGhost: Ghost
    private val points: MutableList<Point>
    private val gameMap: GameMap
    private val score = 0
    private var state: GameState = GameState.Playing

    init {
        gameMap = GameMap(grid)

        val (pacman, localGhost, points) = gameMap.parseGrid()

        this.pacman = pacman
        this.localGhost = localGhost
        this.points = points
    }

    fun ended(): Boolean {
        return state != GameState.Playing
    }

    fun localScore(): Int{
        return if(state == GameState.PacmanWon) 1 else 0
    }

    fun processRound(direction: Direction) {
        if(state != GameState.Playing) return

        moveLocalGhost()
        updateGameToClient()

        if(pacmanDied()) return gameOver(GameState.GhostsWon)

        movePacman(direction)
        eatPoints()

        updateGameToClient()

        if(pacmanWon()) return gameOver(GameState.PacmanWon)
        if(pacmanDied()) return gameOver(GameState.GhostsWon)
    }

    private fun pacmanDied(): Boolean {
        return pacman.conflicts(localGhost)
    }

    private fun eatPoints() {
        for(point in points) {
            if(pacman.conflicts(point)) point.eaten = true
        }
    }

    private fun movePacman(direction: Direction) {
        pacman.move(direction)
    }

    private fun moveLocalGhost() {
        try {
            localGhost.move(Direction.entries.random())
        }
        catch (e: InvalidPosition) {
            moveLocalGhost()
        }
    }

    private fun updateGameToClient() {
        val grid = gameMap.generateGrid(pacman, localGhost, points)
        CommandLine.logGame(grid)
    }

    private fun pacmanWon(): Boolean{
        return score == points.size
    }

    private fun gameOver(newState: GameState) {
        Logger.log("Game over!", 0)
        CommandLine.log("Game over!")
        when(newState) {
            GameState.PacmanWon -> {
                CommandLine.log("Pacman won the game with $points points")
                Logger.log("Pacman won the game with $points points", 0)
            }
            GameState.GhostsWon -> {
                CommandLine.log("Ghosts won the game and Pacman got $points points")
                Logger.log("Ghosts won the game and Pacman got $points points", 0)
            }
            else -> throw Exception("This should not happen (SUS)")
        }
        state = newState
    }
}