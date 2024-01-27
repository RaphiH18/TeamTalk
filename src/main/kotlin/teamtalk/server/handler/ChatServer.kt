package teamtalk.server.handler

import teamtalk.server.ui.ServerGUI
import java.net.InetAddress

class ChatServer(port: Int) {

    private val handler = ServerHandler(this)
    private val gui = ServerGUI(this)
    private val stats = ServerStatistic(this)

    private var IP = "127.0.0.1"
    private var PORT = port

    private val clients = mutableListOf<ServerClient>()

    fun start() {
        handler.start()
        stats.start()
    }

    fun stop() {
        handler.stop()
    }

    fun getIP() = IP

    fun setIP(newIP: String) {
        IP = newIP
    }

    fun getPort() = PORT

    fun setPort(newPort: Int) {
        PORT = newPort
    }

    fun getGUI() = gui

    fun getStats() = stats

    fun getClients() = clients

    fun getUsers() = mutableListOf("Raphael Hegi", "Lukas Ledergerber", "Yannick Meier", "Budei Babdei", "Sone Anderi Person")

    fun getOnlineUsers(): MutableList<String> {
        val onlineUsers = mutableListOf<String>()

        for (client in clients) {
            onlineUsers.add(client.getUsername())
        }
        return onlineUsers
    }
}