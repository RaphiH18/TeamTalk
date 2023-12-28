package teamtalk.client.ui

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceDialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.client.handler.ChatClient
import java.io.IOException
import kotlin.system.exitProcess

class ClientGUI : Application() {
    private val chatClient = ChatClient()
    override fun start(stage: Stage) {
        CoroutineScope(Dispatchers.JavaFx).launch {
            startConnectionGUI(stage)
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

    private fun startConnectionGUI(stage :Stage) {
        val serverLbl = Label("Server")
        val serverTf = TextField("127.0.0.1")

        val serverHb = HBox().apply{
            prefWidth = 200.0
            prefHeight = 50.0
            padding = Insets(50.0,0.0,10.0,50.0)
            spacing = 10.0
            with(children) {
                add(serverLbl)
                add(serverTf)
            }
        }

        val messageLbl = Label("Nachricht")
        val messageOutputLbl = Label("Bereit")

        val messageHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 100.0
            padding = Insets(10.0,0.0,0.0,50.0)
            spacing = 10.0
            with(children){
                add(messageLbl)
                add(messageOutputLbl)
            }
        }

        val portLbl = Label("Port")
        val portTf = TextField("4444").apply {
            maxWidth =68.0
        }
        val portTfVb = VBox().apply {
            padding = Insets(0.0,0.0,0.0,11.0)
            children.add(portTf)
        }
        val connectBtn = Button("Verbinden").apply {
            setOnAction {
                messageOutputLbl.text = "Verbinde..."
                try {
                    chatClient.start(serverTf.text, portTf.text.toInt())
                } catch(e: IOException){
                    messageOutputLbl.text = "Verbindung Fehlgeschlagen"
                }
            }
        }

        val portHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 50.0
            padding = Insets(0.0,0.0,10.0,50.0)
            spacing = 10.0
            with(children) {
                add(portLbl)
                add(portTfVb)
                add(connectBtn)
            }
        }

        val connectionGUIVb = VBox().apply{
            prefWidth = 350.0
            prefHeight = 200.0
            with(children){
                add(serverHb)
                add(portHb)
                add(messageHb)
            }
        }

        with(stage) {
            scene = Scene(connectionGUIVb)
            title = "TeamTalk Serverauswahl"
            setOnCloseRequest { exitProcess(0) }
            show()
        }
    }
}