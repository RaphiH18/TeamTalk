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
import teamtalk.client.ClientMessage
import teamtalk.client.handler.ChatClient
import kotlin.system.exitProcess

class ClientGUI : Application() {

    private val defaultServerIP = "127.0.0.1"
    private val defaultPort = "4444"

    private val connectBtn = Button("Verbinden")
    private val messageOutputLbl = Label("Bereit")

    private val chatClient = ChatClient()

    override fun start(stage: Stage) {
        CoroutineScope(Dispatchers.JavaFx).launch {
            startConnectionGUI(stage)
            while (true) {
                if (!(chatClient.isConnected())) {
                    messageOutputLbl.text = chatClient.getStatusMessage()
                    delay(100)
                    if (chatClient.getStatusMessage() == "Timeout") {
                        connectBtn.isDisable = false
                    }
                } else {
                    messageOutputLbl.text = chatClient.getStatusMessage()
                    break
                }
            }
            delay(100)
            startBenutzerauswahlGUI(stage)
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

    private fun startConnectionGUI(stage: Stage) {
        val serverLbl = Label("Server")
        val serverTf = TextField(defaultServerIP)

        val serverHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 50.0
            padding = Insets(50.0, 0.0, 10.0, 50.0)
            spacing = 10.0
            with(children) {
                add(serverLbl)
                add(serverTf)
            }
        }

        val messageLbl = Label("Status:")

        val messageHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 100.0
            padding = Insets(10.0, 0.0, 0.0, 50.0)
            spacing = 10.0
            with(children) {
                add(messageLbl)
                add(messageOutputLbl)
            }
        }

        val portLbl = Label("Port")
        val portTf = TextField(defaultPort).apply {
            maxWidth = 68.0
        }

        val portTfVb = VBox().apply {
            padding = Insets(0.0, 0.0, 0.0, 11.0)
            children.add(portTf)
        }

        connectBtn.apply {
            setOnAction {
                chatClient.start(serverTf.text, portTf.text.toInt())
                connectBtn.isDisable = true
            }
        }

        val portHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 50.0
            padding = Insets(0.0, 0.0, 10.0, 50.0)
            spacing = 10.0
            with(children) {
                add(portLbl)
                add(portTfVb)
                add(connectBtn)
            }
        }

        val connectionGUIVb = VBox().apply {
            prefWidth = 350.0
            prefHeight = 200.0
            with(children) {
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

    fun startBenutzerauswahlGUI(stage: Stage) {
        Platform.runLater {
            val userChoice: ChoiceDialog<String> = ChoiceDialog("", chatClient.getServerUsers())
            with(userChoice) {
                setTitle("Benutzerauswahl")
                setHeaderText(null)
                setContentText("Benutzername:")
            }

            val result = userChoice.showAndWait()
            result.ifPresent {
                selectedUsername -> chatClient.setUsername(selectedUsername)
                chatClient.getHandler().send(ClientMessage.LOGIN.getJSONString(chatClient))
            }
            startMainGUI(stage, chatClient)
        }
    }
}