package teamtalk.client

import javafx.application.Application
import java.lang.Thread.sleep

fun main() {
    sleep(2000)
    Application.launch((ClientApp::class.java))
}