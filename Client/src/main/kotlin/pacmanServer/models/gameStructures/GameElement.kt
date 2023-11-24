package pacmanServer.models.gameStructures

abstract class GameElement(row: Int, col: Int) {
    var position = Position(row, col)
        private set

    fun conflicts(other: GameElement): Boolean {
        return other.position.row == this.position.row && other.position.col == this.position.col
    }
}