package teamtalk.message

import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage

class Contact(private val username: String, private var online: Boolean = false) {

    private val messages = mutableListOf<Message>()
    private var lastNewMessageIndex = -1

    fun addMessage(textMessage: TextMessage) {
        messages.add(textMessage)
    }

    fun addMessage(fileMessage: FileMessage) {
        messages.add(fileMessage)
    }

    fun getMessages() = messages

    fun getNewMessages(): List<Message> {
        val newMessages: List<Message>
        if ((lastNewMessageIndex + 1) < messages.size) {
            newMessages = messages.subList(lastNewMessageIndex + 1, messages.size)
            lastNewMessageIndex = messages.size - 1
            return newMessages
        } else {
            return emptyList()
        }
    }

    fun getUsername() = username

    fun setOnline(online: Boolean) {
        this.online = online
    }

    fun isOnline() = online
}