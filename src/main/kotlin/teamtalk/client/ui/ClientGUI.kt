package teamtalk.client.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import teamtalk.client.handler.ChatClient

class ClientGUI : Application() {

    override fun start(stage: Stage) {
        val chatClient = ChatClient()

        with(stage) {
            scene = Scene(chatClient.createBaseView(), 800.0, 600.0)
            title = "TeamTalk Client"
            setOnCloseRequest { System.exit(0) }
            show()
        }

        println("Welcome! The TeamTalk Client is now running")
    }
}