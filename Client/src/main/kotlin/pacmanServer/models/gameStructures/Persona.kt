package pacmanServer.models.gameStructures

import pacmanServer.structures.errors.InvalidPosition

abstract class Persona(row: Int, col: Int, private val gameMap: GameMap): GameElement(row, col), Movable {
    override fun move(direction: Direction){
        val nextPosition = when(direction) {
            Direction.UP -> Position(((position.row - 1) + gameMap.rows) % gameMap.rows, position.col)
            Direction.DOWN -> Position((position.row + 1) % gameMap.rows, position.col)
            Direction.LEFT -> Position(position.row, ((position.col - 1) + gameMap.cols) % gameMap.cols)
            Direction.RIGHT -> Position(position.row, (position.col + 1) % gameMap.cols)
        }

        if(gameMap.isWall(nextPosition)) throw InvalidPosition()

        position.row = nextPosition.row
        position.col = nextPosition.col
    }
}

