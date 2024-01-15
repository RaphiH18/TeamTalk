package teamtalk.client.messaging

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class Message(private val senderName: String, private val receiverName: String, private val timestamp: Instant) {

    abstract fun getMessage(): Any

    fun getTimestamp() = timestamp

    fun getSenderName() = senderName

    fun getReceiverName() = receiverName

}

fun Instant.toFormattedString(): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss").withZone(ZoneId.systemDefault())
    return formatter.format(this)
}