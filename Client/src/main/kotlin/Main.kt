
import pacmanServer.views.TCPClient

// TODO:
// --help -h
// --server -s
fun main(args: Array<String>) {
    // get ip and port from argument

    if(args.size != 2){
        throw IllegalArgumentException("Usage: ${args[0]} <server-address>")
    }

    val address = args[1]

    Thread(TCPClient(address)).start()
}