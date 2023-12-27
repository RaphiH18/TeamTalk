package teamtalk.client.ui

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.ChoiceDialog
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.client.handler.ChatClient
import teamtalk.server.logger.debug
import java.lang.Thread.sleep
import kotlin.system.exitProcess

class ClientGUI : Application() {

    override fun start(stage: Stage) {
        val chatClient = ChatClient()
        chatClient.start()

        CoroutineScope(Dispatchers.JavaFx).launch {
            while (!(chatClient.isConnected())) {
                delay(100)
            }

            /*
            TODO: Entfernen von runLater { }
            Das wird aktuell noch benötigt, weil es sonst folgenden Error gibt:
                Exception in thread "JavaFX Application Thread" java.lang.IllegalStateException: showAndWait is not allowed during animation or layout processing
            Mit Listeners könnte man das aber lösen (reaktiver Ansatz)
             */
            Platform.runLater {
                val userChoice: ChoiceDialog<String> = ChoiceDialog("", chatClient.getServerUsers())
                with(userChoice) {
                    setTitle("Benutzerauswahl")
                    setHeaderText(null)
                    setContentText("Benutzername:")
                }

                val result = userChoice.showAndWait()
                result.ifPresent { selected -> println("Selected: $selected") }
                startMainGUI(stage, chatClient)
            }
        }
    }

    private fun startMainGUI(stage: Stage, chatClient: ChatClient) {
        with(stage) {
            scene = Scene(chatClient.createBaseView(), 800.0, 600.0)
            title = "TeamTalk Client"
            setOnCloseRequest { exitProcess(0) }
            show()
        }
    }
}