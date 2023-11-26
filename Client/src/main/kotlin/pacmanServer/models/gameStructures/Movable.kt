package pacmanServer.models.gameStructures

interface Movable {
    fun move(direction: Direction): Boolean
}