package teamtalk.client.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.client.handler.ChatClient

class ClientGUI : Application() {
private val uiScope = CoroutineScope(Dispatchers.JavaFx)
private val handlerScope = CoroutineScope(Dispatchers.IO)
    //LOGIN-Brancheler
    // Lukalulu

    override fun start(stage: Stage) {
        val chatClient = ChatClient()

        handlerScope.launch {
            chatClient.start()
        }

        //Userlogin

        starMainGUI(stage, chatClient)

        println("Willkommen! The TeamTalk Client is now running!!!")
    }

    fun starMainGUI(stage: Stage, chatClient: ChatClient) {


        with(stage) {
            scene = Scene(chatClient.createBaseView(), 800.0, 600.0)
            title = "TeamTalk Client"
            setOnCloseRequest { java.lang.System.exit(0) }
            show()
        }
    }
}