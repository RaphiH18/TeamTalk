package teamtalk.client

import javafx.scene.control.TextArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object clientLogger {

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    var DEBUG = false
    val DEFAULT_LOG_PATH = "log/logs.txt"
    val PREFIX = "[TeamTalk]"

    var logPane: TextArea = TextArea()

    fun log(message: String) {
        val dateAndTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
        val logMessage = "$dateAndTime $PREFIX $message"

        //Ausgabe auf der Konsole
        println(logMessage)

        //Ausgabe im Logfile
        handlerScope.launch {
            logToFile(logMessage)
        }
    }

    fun debug(message: String) {
        if (DEBUG) {
            val dateAndTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
            val debugMessage = "DEBUG $dateAndTime $PREFIX [${Thread.currentThread().name}] $message"

            //Ausgabe auf der Konsole
            println(debugMessage)
        }
    }

    private fun logToFile(message: String) {
        val logFile = File(DEFAULT_LOG_PATH)

        if (logFile.exists()) {
            logFile.appendText("${message}\n")
        }
    }
}