package teamtalk.client.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.ChoiceDialog
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import teamtalk.client.handler.ChatClient
import java.lang.Thread.sleep

class ClientGUI : Application() {
    private val uiScope = CoroutineScope(Dispatchers.JavaFx)

    override fun start(stage: Stage) {
        val chatClient = ChatClient()
        chatClient.start()
        while (!chatClient.getConnectStatus()) {
            sleep(100)
        }

        println("Benutzerabfrage")
        chatClient.isUserListStatus()

        val dialog: ChoiceDialog<String> = ChoiceDialog("Auswahl...", chatClient.getUserList())
        with(dialog) {
            setTitle("Benutzerauswahl")
            setHeaderText(null)
            setContentText("Benutzername:")
        }
        val result = dialog.showAndWait()
        result.ifPresent { selected -> println("Selected: $selected") }
        startMainGUI(stage, chatClient)

        println("Willkommen! The TeamTalk Client is now running!!!")
    }

    fun startMainGUI(stage: Stage, chatClient: ChatClient) {


        with(stage) {
            scene = Scene(chatClient.createBaseView(), 800.0, 600.0)
            title = "TeamTalk Client"
            setOnCloseRequest { java.lang.System.exit(0) }
            show()
        }
    }
}