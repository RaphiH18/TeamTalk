package teamtalk.client

import java.text.SimpleDateFormat
import java.util.*

object clientLogger {

    var DEBUG = true
    private val PREFIX = "[TeamTalk]"

    fun log(message: String) {
        val dateAndTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
        val logMessage = "$dateAndTime $PREFIX $message"

        println(logMessage)
    }

    fun debug(message: String) {
        if (DEBUG) {
            val dateAndTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
            val debugMessage = "DEBUG $dateAndTime $PREFIX [${Thread.currentThread().name}] $message"

            println(debugMessage)
        }
    }
}