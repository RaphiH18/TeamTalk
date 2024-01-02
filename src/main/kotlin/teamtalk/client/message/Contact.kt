package teamtalk.client.message

class Contact(private val name: String) {
    private val messages = mutableListOf<TextMessage>()

    fun addMessage(senderName: String, timeStamp: String, newTextMessage: String){
        val message = TextMessage(senderName,timeStamp, newTextMessage)
        println("Füge Message hinzu: $message")
        messages.add(message)
    }

    fun getName() = name

    fun getMessages(): MutableList<TextMessage> {
        return messages
    }
}