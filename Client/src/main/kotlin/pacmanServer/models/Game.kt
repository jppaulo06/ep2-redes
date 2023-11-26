package pacmanServer.models

import pacmanServer.models.gameStructures.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class Game(
    grid: List<List<Char>>,
    private val localMovementQueue: BlockingQueue<Direction>,
    private val remoteMovementQueue: BlockingQueue<Direction>,
    private val localSavedGridsQueue: LinkedBlockingQueue<List<List<Char>>>,
    private val remoteSavedGridsQueue: LinkedBlockingQueue<List<List<Char>>>
) : Runnable {

    private var pacman: Pacman
    private var localGhost: Ghost
    private val points: MutableList<Point>
    private val gameMap: GameMap
    private var eaten = 0
    private var state: GameState = GameState.Playing

    private var remoteIsConnected = AtomicBoolean(false)

    var remotedStarted = false
    var remoteGhost: RemoteGhost? = null

    init {
        gameMap = GameMap(grid)

        val (pacman, localGhost, points) = gameMap.parseGrid()

        this.pacman = pacman
        this.localGhost = localGhost
        this.points = points

        updateGridToClients()
    }

    override fun run() {
        processRound()
    }

    @Synchronized
    fun ended(): Boolean {
        return state != GameState.Playing
    }

    @Synchronized
    fun localWon(): Boolean {
        if (state == GameState.Playing) throw Exception("Game is still running")
        return state == GameState.PacmanWon
    }

    @Synchronized
    fun remoteWon(): Boolean {
        if (state == GameState.Playing) throw Exception("Game is still running")
        return state == GameState.GhostsWon
    }

    fun createRemoteGhost(){
        val (row, col) = gameMap.getPositionForRemoteGhost(pacman, localGhost, points)
        remoteGhost = RemoteGhost(row, col, gameMap)
    }

    private tailrec fun processRound() {
        updateGame()
        moveLocalGhost()

        if (ended()) return

        if (remoteGhost != null) {
            moveRemoteGhost()
            if(remoteGhost != null) updateGame()
            if (ended()) return
        }

        movePacman()
        eatPoints()

        updateGame()
        if (ended()) return

        if(remoteGhost != null) remotedStarted = true
        processRound()
    }

    private fun moveLocalGhost() {
        localGhost.move()
    }

    private fun moveRemoteGhost() {
        val direction = remoteMovementQueue.take()
        if(direction == Direction.DISCONNECT) {
            remotedStarted = false
            remoteGhost = null
        }
        else remoteGhost?.move(direction)
    }

    private fun movePacman() {
        val direction = localMovementQueue.take()
        pacman.move(direction)
    }

    private fun eatPoints() {
        for (point in points) {
            if (pacman.conflicts(point)) {
                point.eaten = true
                eaten += 1
            }
        }
    }

    private fun updateGame() {
        if (pacmanDied()) state = GameState.GhostsWon
        if (pacmanWon()) state = GameState.PacmanWon
        updateGridToClients()
    }

    private fun pacmanDied(): Boolean {
        return pacman.conflicts(localGhost) || (remoteGhost != null && pacman.conflicts(remoteGhost!!))
    }

    private fun pacmanWon(): Boolean {
        return eaten == points.size
    }

    private fun updateGridToClients() {
        val grid = gameMap.generateGrid(pacman, localGhost, remoteGhost, points)
        localSavedGridsQueue.put(grid)
        if(remoteGhost != null) remoteSavedGridsQueue.put(grid)
    }
}