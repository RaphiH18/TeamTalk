package teamtalk

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import java.text.SimpleDateFormat
import java.util.*

object logger {

    private val DEBUG = true
    private val THREAD_DEBUG = true
    private val PREFIX = "[TeamTalk]"

    private var logPane: TextArea = TextArea().apply {
        minHeight = 150.0
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

        with(logArea) {
            minHeight = 200.0
            maxHeight = 250.0
            children.add(logLabel)
            children.add(logPane)
        }

        VBox.setVgrow(logArea, Priority.ALWAYS)
        VBox.setVgrow(logPane, Priority.ALWAYS)
        return logArea
    }
}