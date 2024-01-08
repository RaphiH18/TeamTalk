package teamtalk.client.messaging

class Contact(private val username: String, private var online: Boolean = false) {

    private val messages = mutableListOf<Message>()

    fun addMessage(textMessage: TextMessage) {
        messages.add(textMessage)
    }

    fun addMessage(fileMessage: FileMessage) {
        messages.add(fileMessage)
    }

    fun getMessages() = messages

    fun getUsername() = username

    fun setOnline(online: Boolean) {
        this.online = online
    }

    fun isOnline() = online
}