package teamtalk.server.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import teamtalk.server.handler.ChatServer
import teamtalk.server.handler.ServerHandler
import kotlin.system.exitProcess

class ServerGUI : Application() {

    private lateinit var handler: ServerHandler

    override fun start(stage: Stage) {
        val chatServer = ChatServer(4444)

        with(stage) {
            scene = Scene(chatServer.createBaseView(), 800.0, 600.0)
            title = "TeamTalk Server"
            setOnCloseRequest { exitProcess(0) }
            show()
        }
    }
}