package teamtalk.client

import javafx.application.Application
import teamtalk.client.ui.ClientGUI
import java.lang.Thread.sleep

fun main() {
    sleep(2000)
    Application.launch((ClientGUI::class.java))
}