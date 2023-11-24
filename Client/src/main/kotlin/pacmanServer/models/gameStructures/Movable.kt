package pacmanServer.models.gameStructures

import pacmanServer.models.gameStructures.Direction

interface Movable {
    fun move(direction: Direction)
}