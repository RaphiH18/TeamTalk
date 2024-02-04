package teamtalk.message

import java.util.*

class Contact(private val username: String, private var online: Boolean = false) {

    private val messages = mutableListOf<Message>()
    private val newMessagesQueue = ArrayDeque<Message>()

    fun addMessage(textMessage: TextMessage) {
        messages.add(textMessage)
        newMessagesQueue.add(textMessage)
    }

    fun addMessage(fileMessage: FileMessage) {
        messages.add(fileMessage)
        newMessagesQueue.add(fileMessage)
    }

    fun getMessages(): MutableList<Message> {
        return messages
    }

    fun getNewMessages(): List<Message> {
        val newMessages = mutableListOf<Message>()
        while (newMessagesQueue.isNotEmpty()) {
            newMessages.add(newMessagesQueue.removeFirst())
        }
        return newMessages
    }

    fun clearNewMessagesQueue() {
        newMessagesQueue.clear()
    }

    fun getUsername() = username

    fun setOnline(online: Boolean) {
        this.online = online
    }

    fun isOnline() = online
}