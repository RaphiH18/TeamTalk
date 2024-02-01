package teamtalk.server.handler

import teamtalk.server.handler.network.ServerClient
import teamtalk.server.stats.UserStatistic
import java.time.Instant

class ServerUser(private var username: String) {

    private val userStats = UserStatistic(this)

    private lateinit var serverClient: ServerClient
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

    fun isOnline() = this::serverClient.isInitialized

    fun getStats() = userStats

}