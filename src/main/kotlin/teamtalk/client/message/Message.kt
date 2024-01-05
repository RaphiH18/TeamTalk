package teamtalk.client.message

import java.time.Instant

abstract class Message(private val senderName: String, private val timestamp: Instant) {

    abstract fun getMessage(): Any

    fun getTimestamp() = timestamp

    fun getSenderName() = senderName
}