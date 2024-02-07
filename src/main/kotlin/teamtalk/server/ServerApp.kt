package teamtalk.server

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.TabPane
import javafx.stage.Stage
import teamtalk.server.handler.ChatServer
import kotlin.system.exitProcess


class ServerApp : Application() {

    override fun start(stage: Stage) {
        val chatServer = ChatServer(4444)

        with(stage) {
            scene = Scene(chatServer.getGUI().create()).apply {
                minWidth = chatServer.getGUI().MIN_WIDTH
                minHeight = chatServer.getGUI().MIN_HEIGHT
            }

            scene.widthProperty().addListener { _, _, newValue ->
                val leftTabPane = chatServer.getGUI().controlArea.items[0] as TabPane
                val rightTabPane = chatServer.getGUI().controlArea.items[1] as TabPane

                leftTabPane.minWidth = (newValue.toDouble() * 0.3) - 14
                rightTabPane.minWidth = (newValue.toDouble() * 0.7) - 14
            }

            title = "TeamTalk Server"

            setOnCloseRequest {
                chatServer.saveData()
                exitProcess(0)
            }

            show()
        }
    }
}