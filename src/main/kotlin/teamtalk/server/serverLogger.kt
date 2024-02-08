package teamtalk.server

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object serverLogger {

    var DEBUG = false
    private val PREFIX = "[TeamTalk]"
    val DEFAULT_LOG_PATH = "log/logs.txt"

    var logPane: TextArea = TextArea().apply {
        minHeight = 150.0
        isEditable = false
        isWrapText = true
    }

    fun log(message: String) {
        val dateAndTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
        val logMessage = "$dateAndTime $PREFIX $message"

        println(logMessage)
        logPane.appendText("$logMessage\n")

        logToFile(logMessage)
    }

    fun debug(message: String) {
        if (DEBUG) {
            val dateAndTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
            val debugMessage = "DEBUG $dateAndTime $PREFIX [${Thread.currentThread().name}] $message"

            println(debugMessage)
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

    private fun logToFile(message: String) {
        val logFile = File(DEFAULT_LOG_PATH)
        if (logFile.parentFile.exists().not()) {
            logFile.parentFile.mkdirs()
        }

        if (logFile.exists().not()) {
            logFile.createNewFile()
            return
        }

        logFile.appendText("${message}\n")
    }
}