package teamtalk.server.handler

import org.apache.commons.configuration2.YAMLConfiguration
import teamtalk.server.serverLogger
import teamtalk.utilities
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ServerConfig(private val chatServer: ChatServer) {

    private val DEFAULT_CONFIG_PATH = "config/server.cfg"
    private val DEFAULT_USERDATA_PATH = "data"

    lateinit var configFile: YAMLConfiguration

    val fillWordsList = mutableListOf<String>()
    val triggerWordsList = mutableListOf<String>()

    fun saveData() {
        for (user in chatServer.getUsers()) {
            user.saveData()
        }

        saveSettings()
    }

    fun saveSettings() {
        configFile.setProperty("server.ip", chatServer.getIP())
        configFile.setProperty("server.port", chatServer.getPort())
        configFile.write(FileWriter(DEFAULT_CONFIG_PATH))
    }

    fun load() {
        loadConfig(File(DEFAULT_CONFIG_PATH))
        loadLogs(File(serverLogger.DEFAULT_LOG_PATH))

        val userDataDir = File(DEFAULT_USERDATA_PATH)
        if (userDataDir.exists() and userDataDir.isDirectory) {
            val userDataFiles = userDataDir.listFiles()

            if (userDataFiles != null) {
                for (dataFile in userDataFiles) {
                    val user = ServerUser(chatServer, dataFile.nameWithoutExtension)
                    user.loadData()
                    user.getStats().updateGUI()
                    chatServer.getUsers().add(user)
                    chatServer.getStats().loadData(user)
                    chatServer.getGUI().updateUserList(user)
                }

                chatServer.getStats().updateTotalAverageAnswerTime()
                chatServer.getStats().updateTotalAverageUsageTime()
                chatServer.getGUI().updateQuickStats()
            }
        }
    }

    fun loadLogs(file: File) {
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }

        if (file.exists().not()) {
            file.createNewFile()
            return
        }

        file.useLines {
            for (line in it) {
                serverLogger.logPane.appendText("${line}\n")
            }
        }
    }

    fun loadConfig(file: File) {
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }

        if (file.exists().not()) {
            file.createNewFile()

            configFile = defaultConfigFile()
            configFile.write(FileWriter(file))
        }

        configFile = YAMLConfiguration()
        configFile.read(FileReader(file))

        fillWordsList.addAll(configFile.getList("stats.fillwords") as List<String>)
        triggerWordsList.addAll(configFile.getList("stats.triggerwords.positive") as List<String>)
        triggerWordsList.addAll(configFile.getList("stats.triggerwords.neutral") as List<String>)
        triggerWordsList.addAll(configFile.getList("stats.triggerwords.negative") as List<String>)
        chatServer.setIP(configFile.getString("server.ip"))
        chatServer.setPort(configFile.getInt("server.port"))
        serverLogger.DEBUG = configFile.getBoolean("server.debug")
    }

    fun defaultFillWords(): List<String> {
        return listOf(
            "der",
            "die",
            "das",
            "und",
            "oder",
            "also",
            "quasi",
            "sozusagen",
            "eigentlich"
        )
    }

    fun defaultPositiveTriggerWords(): List<String> {
        return listOf(
            "gut",
            "ja",
            "super",
            "perfekt",
            "optimal",
            "prima"
        )
    }

    fun defaultNeutralTriggerWords(): List<String> {
        return listOf(
            "ok",
            "einverstanden",
            "passt"
        )
    }

    fun defaultNegativeTriggerWords(): List<String> {
        return listOf(
            "schlecht",
            "nein",
            "schade"
        )
    }

    fun defaultConfigFile(): YAMLConfiguration {
        val yamlConfig = YAMLConfiguration()
        yamlConfig.addProperty("server.ip", "127.0.0.1")
        yamlConfig.addProperty("server.port", 4444)
        yamlConfig.addProperty("server.debug", false)
        yamlConfig.addProperty("stats.fillwords", defaultFillWords())
        yamlConfig.addProperty("stats.triggerwords.positive", defaultPositiveTriggerWords())
        yamlConfig.addProperty("stats.triggerwords.neutral", defaultNeutralTriggerWords())
        yamlConfig.addProperty("stats.triggerwords.negative", defaultNegativeTriggerWords())
        return yamlConfig
    }
}