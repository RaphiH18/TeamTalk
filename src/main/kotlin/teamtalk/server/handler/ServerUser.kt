package teamtalk.server.handler

import teamtalk.server.handler.network.ServerClient
import teamtalk.server.stats.UserStatistic
import java.time.Instant

class ServerUser(private val chatServer: ChatServer, private var username: String) {

    private val userStats = UserStatistic(this)

    private var serverClient: ServerClient? = null
    private lateinit var loginTime: Instant

    fun getName() = username

    fun setName(newName: String) {
        username = newName
    }

    fun login(newClient: ServerClient) {
        this.serverClient = newClient
        loginTime = Instant.now()
    }

    fun getClient() = serverClient

    fun logout() {
        this.serverClient?.getSocket()?.close()
        this.serverClient?.getInput()?.close()
        this.serverClient?.getOutput()?.close()
        this.serverClient = null
    }

    fun getLoginTime() = loginTime

    fun isOnline() = serverClient != null

    fun getStats() = userStats

    fun getServer() = chatServer

    fun getIndex() = chatServer.getUsers().indexOf(this)

}