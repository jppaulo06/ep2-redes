import pacmanServer.views.TCPServer

fun main(args: Array<String>) {
    val tcpServer = TCPServer()
    Thread(tcpServer).start()
}