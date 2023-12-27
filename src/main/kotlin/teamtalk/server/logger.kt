package teamtalk.server

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

object logger {

    private val DEBUG = true
    private val THREAD_DEBUG = true
    private val PREFIX = "[TeamTalk]"

    private var logPane: TextArea = TextArea().apply {
        prefHeight = 150.0
        prefWidth = 200.0
        isEditable = false
        isWrapText = true
    }

    fun log(message: String) {
        println("$PREFIX $message")
        logPane.appendText("$PREFIX $message\n")
    }

    fun debug(message: String) {
        val dateAndTime = SimpleDateFormat("dd.M.yyyy HH:mm:ss").format(Date())

        if (DEBUG) {
            if (THREAD_DEBUG) {
                println("DEBUG $dateAndTime $PREFIX [${Thread.currentThread().name}] $message")
            } else {
                println("DEBUG $dateAndTime $PREFIX $message")
            }
        }
    }

    fun createServerView(): VBox {
        val logArea = VBox()

        val logLabel = Label("Ereignisse").apply {
            padding = Insets(5.0, 0.0, 5.0, 0.0)
        }

        logArea.children.add(logLabel)
        logArea.children.add(logPane)
        return logArea
    }
}