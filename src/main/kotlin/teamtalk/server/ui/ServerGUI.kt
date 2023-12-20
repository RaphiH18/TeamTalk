package teamtalk.server.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import teamtalk.server.handler.ChatServer

class ServerGUI : Application() {
    override fun start(stage: Stage) {
        val chatServer = ChatServer(4444)

        with(stage) {
            scene = Scene(chatServer.createBaseView(), 800.0, 600.0)
            title = "TeamTalk Server"
            setOnCloseRequest { System.exit(0) }
            show()
        }

        println("Welcome! The TeamTalk Server is now running")
    }

}