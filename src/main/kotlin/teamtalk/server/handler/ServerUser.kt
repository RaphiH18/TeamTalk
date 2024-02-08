package teamtalk.server.handler

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import teamtalk.server.handler.network.ServerClient
import teamtalk.server.stats.UserStatistic
import java.io.File
import java.time.Duration
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

    fun deleteData() {
        val file = File("data/${this.username}.json")

        if (file.exists()) {
            file.delete()
        }
    }

    fun saveData() {
        val userData = UserData(
            this.username,
            userStats.sentTextMessages,
            userStats.receivedTextMessages,
            userStats.sentFileMessages,
            userStats.receivedFileMessages,
            userStats.usageTime.toMillis(),
            userStats.getSimpleAnswerTime(),
            userStats.fillWordChart.getData(),
            userStats.triggerWordChart.getData())

        val jsonFormat = Json { prettyPrint = true }
        val serializedData = jsonFormat.encodeToString(userData)

        val file = File("data/${this.username}.json")

        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }

        if (file.exists().not()) {
            file.createNewFile()
        }

        file.writeText(serializedData)
    }

    fun loadData() {
        val file = File("data/${this.username}.json")

        if (file.exists()) {
            val jsonFormat = Json { prettyPrint = true }
            val userData: UserData = jsonFormat.decodeFromString(file.readText())

            this.username = userData.username
            this.userStats.sentTextMessages = userData.sentTextMessages
            this.userStats.receivedTextMessages = userData.receivedTextMessages
            this.userStats.sentFileMessages = userData.sentFileMessages
            this.userStats.receivedFileMessages = userData.receivedFileMessages
            this.userStats.usageTime = Duration.ofMillis(userData.usageTime)
            this.userStats.setFromSimpleAnswerTime(userData.answerTime)
            userStats.fillWordChart.setData(userData.fillWordStats)
            userStats.triggerWordChart.setData(userData.triggerWordStats)
        }
    }
}