object Config {
    const val serverDefaultPort = 3000
    const val clientDefaultPort = 3001
    const val logLevel = 2
    const val logPath = "src/main/resources/logs_pacman"
    const val serverDefaultAddress = "127.0.0.1"
    const val maxDatagramSize = 5000

    var protocol = "TCP"
    var clientPort = clientDefaultPort
    var serverPort = serverDefaultPort
    var serverAddress = serverDefaultAddress
}