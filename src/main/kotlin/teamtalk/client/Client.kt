package teamtalk.client

import javafx.application.Application
import teamtalk.client.handler.ChatClient
import teamtalk.client.ui.ClientGUI

suspend fun main() {
    Thread.sleep(2000)

    Application.launch((ClientGUI::class.java))

    /*val client = ChatClient()
    client.start()
     */
}