package teamtalk.client.message

import java.time.Instant

class TextMessage(senderName: String,
                  timeStamp: String,
                  private val messageText: String): Message(senderName, timeStamp){

    fun getMessage(): List<String> {
        return listOf("${timeStamp.toString()}", "${senderName.toString()}", "${messageText}")
    }
}