package pacmanServer.models.gameStructures

class Ghost(row: Int, col: Int, gameMap: GameMap) : Persona(row, col, gameMap) {
    fun move() {
        var success: Boolean
        do {
            val direction = Direction.entries.random()
            success = if(direction == Direction.DISCONNECT) false
            else this.move(direction)
        } while (!success)
    }
}