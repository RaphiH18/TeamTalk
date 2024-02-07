package teamtalk.server.handler

import teamtalk.logger.log
import teamtalk.server.handler.network.ServerClient
import teamtalk.server.stats.StatisticHandler
import teamtalk.server.ui.ServerGUI
import java.io.File

class ChatServer(port: Int) {

    private val users = mutableListOf<ServerUser>()

    private val handler = ServerHandler(this)
    private val stats = StatisticHandler(this)
    private val gui = ServerGUI(this)

    private var IP = "127.0.0.1"
    private var PORT = port

    init {
        loadData()
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

    fun getHandler() = handler

    fun getUsers() = users

    fun getUser(username: String) = users.firstOrNull { it.getName() == username }

    fun addUser(username: String) {
        val newUser = ServerUser(this, username)
        users.add(newUser)
        gui.updateUserList(newUser)
        log("Der Benutzer $username wurde erfolgreich erstellt.")
    }

    fun deleteUser(username: String) {
        val user = getUser(username)
        if (user != null) {
            user.deleteData()
            users.remove(user)
            gui.updateUserList()
            log("Der Benutzer $username wurde erfolgreicht gelöscht.")
        }
    }

    fun getUser(serverClient: ServerClient): ServerUser? {
        for (user in users) {
            if (user.getClient() == serverClient) {
                return user
            }
        }

        return null
    }

    fun getClients(): List<ServerClient> {
        val clients = mutableListOf<ServerClient>()

        for (user in users) {
            if (user.isOnline()) {
                clients.add(user.getClient()!!)
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

    fun saveData() {
        for (user in users) {
            user.saveData()
        }
    }

    private fun loadData() {
        val userDataDir = File("data")
        println("data-Ordner existiert: ${userDataDir.exists()}")
        println("data-Ordner ist Ordner: ${userDataDir.isDirectory}")
        println("Files im data-Ordner: ${userDataDir.listFiles()}")
        if (userDataDir.exists() and userDataDir.isDirectory) {
            val userDataFiles = userDataDir.listFiles()

            if (userDataFiles != null) {
                for (dataFile in userDataFiles) {
                    val user = ServerUser(this, dataFile.nameWithoutExtension)
                    users.add(user)
                    user.loadData()
                    stats.loadData(user)
                    gui.updateUserList(user)
                }

                stats.updateTotalAverageAnswerTime()
                gui.updateQuickStats()
            }
        }
    }
}