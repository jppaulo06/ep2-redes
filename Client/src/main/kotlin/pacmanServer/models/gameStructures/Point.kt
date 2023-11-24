package pacmanServer.models.gameStructures

class Point(row: Int, col: Int): GameElement(row, col) {
    var eaten = false
}