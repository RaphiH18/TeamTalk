package teamtalk.client.messaging

import java.time.Instant

class TextMessage(senderName: String, receiverName: String, timestamp: Instant, private val messageText: String) : Message(senderName, receiverName, timestamp) {

    override fun getMessage() = messageText

}