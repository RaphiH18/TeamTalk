package teamtalk.server.handler

import teamtalk.server.ui.ServerGUI
import java.net.InetAddress

class ChatServer(port: Int) {

    private val handler = ServerHandler(this)
    private val gui = ServerGUI(this)

    private val IP = InetAddress.getLoopbackAddress()
    private val PORT = port

    private val clients = mutableListOf<ServerClient>()

    fun start() {
        handler.start()
    }

    fun stop() {
        handler.stop()
    }

    fun getIP() = IP

    fun getPort() = PORT

    fun getGUI() = gui

    fun getClients() = clients

    fun getUsers() = mutableListOf("Raphael Hegi", "Lukas Ledergerber", "Yannick Meier", "Budei Babdei", "Sone Anderi Person")

    fun getOnlineUsers(): MutableList<String> {
        val onlineUsers = mutableListOf<String>()

        for (client in clients) {
            onlineUsers.add(client.getUsername())
        }
        return onlineUsers
    }

    fun getOfflineUsers(): MutableList<String> {
        val offlineUsers = mutableListOf<String>()

        for(user in getUsers()){
            if (getOnlineUsers().contains(user).not()) {
                offlineUsers.add(user)
            }
        }
        println("Aktuelle Offlineusers: $offlineUsers")
        return offlineUsers
    }
}