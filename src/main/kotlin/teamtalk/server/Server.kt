package teamtalk.server

import javafx.application.Application
import teamtalk.server.ui.ServerGUI

suspend fun main() {
    /*
    main-Methode des Serverteils der TeamTalk-Chat-App
    */
    Application.launch(ServerGUI::class.java)
}
