package teamtalk.client.message

import java.time.Instant

class TextMessage(senderName: String, timestamp: Instant, private val messageText: String) : Message(senderName, timestamp) {

//    fun getMessage(): List<String> {
//        return listOf("${timeStamp.toString()}", "${senderName.toString()}", "${messageText}")
//    }

    override fun getMessage() = messageText

}