package teamtalk.server.handler

import teamtalk.server.handler.network.ServerClient
import teamtalk.server.stats.StatisticHandler
import teamtalk.server.ui.ServerGUI

class ChatServer(port: Int) {

    private val users = mutableListOf<ServerUser>()

    private val handler = ServerHandler(this)
    private val stats = StatisticHandler(this)
    private val gui = ServerGUI(this)

    private var IP = "127.0.0.1"
    private var PORT = port

    init {
        addUser("Raphael Hegi")
        addUser("Lukas Ledergerber")
        addUser("Yannick Meier")
    }

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

    fun getUsers() = users

    fun getUser(username: String) = users.firstOrNull { it.getName() == username }

    fun addUser(username: String) {
        users.add(ServerUser(username))
        gui.updateUserList()
    }

    fun getClients(): List<ServerClient> {
        val clients = mutableListOf<ServerClient>()

        for (user in users) {
            if (user.isOnline()) {
                clients.add(user.getClient())
            }
        }

        return clients.toList()
    }

    fun getClientNames(): List<String> {
        val names = mutableListOf<String>()

        for (user in users) {
            if (user.isOnline()) {
                names.add(user.getName())
            }
        }

        return names.toList()
    }

    fun getUserNames(): List<String> {
        val names = mutableListOf<String>()

        for (user in users) {
            names.add(user.getName())
        }

        return names
    }
}