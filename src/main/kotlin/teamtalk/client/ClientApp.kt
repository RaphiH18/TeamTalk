package teamtalk.client

import javafx.application.Application
import javafx.stage.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import teamtalk.client.handler.ChatClient

class ClientApp : Application() {

    private val chatClient = ChatClient()

    override fun start(stage: Stage) {
        chatClient.getGUI().guiScope.launch {
            chatClient.getGUI().primaryStage = stage
            chatClient.getGUI().startConnectionGUI(stage)
            chatClient.getGUI().waitForConnected()
            delay(100)
            chatClient.getGUI().startUserselectionGUI(stage)
        }
    }
}