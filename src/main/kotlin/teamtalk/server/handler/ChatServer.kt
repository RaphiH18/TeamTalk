package teamtalk.server.handler

import teamtalk.Communicable
import teamtalk.client.handler.ChatClient
import java.net.InetAddress
import java.net.ServerSocket


class ChatServer(port: Int) : Communicable {

    var IP = InetAddress.getByName("")
    val PORT = port
    var clients = ArrayList<ChatClient>()

    override fun start() {
        var serverSocket = ServerSocket(PORT, 20, IP)
        println("[TeamTalk] The Server has started and is listening for connections on ${IP}, port ${PORT}")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }
}