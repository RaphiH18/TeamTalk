package teamtalk.client.message

class Contact(
    private val name: String,
    private var status: Boolean = false) {
    private val messages = mutableListOf<TextMessage>()

    fun addMessage(senderName: String, timeStamp: String, newTextMessage: String){
        val message = TextMessage(senderName,timeStamp, newTextMessage)
        println("Füge Message hinzu: $message")
        messages.add(message)
    }

    fun getName() = name

    fun setStatus(status: Boolean) {
        this.status = status
    }

    fun getStatus() = status

    fun getMessages(): MutableList<TextMessage> {
        return messages
    }
}