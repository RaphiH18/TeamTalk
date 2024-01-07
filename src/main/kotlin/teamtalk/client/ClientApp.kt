package teamtalk.client

import javafx.application.Application
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.client.handler.ChatClient

class ClientApp : Application() {

    private val chatClient = ChatClient()

    override fun start(stage: Stage) {
        CoroutineScope(Dispatchers.JavaFx).launch {
            chatClient.getGUI().startConnectionGUI(stage)
            chatClient.getGUI().waitForConnected()
            delay(100)
            chatClient.getGUI().startBenutzerauswahlGUI(stage)
        }
    }
}