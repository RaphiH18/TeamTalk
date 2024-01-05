package teamtalk.client.message

class Contact(private val username: String, private var status: Boolean = false) {

    private val messages = mutableListOf<Message>()

    fun addMessage(textMessage: TextMessage) {
        messages.add(textMessage)
    }

    fun addMessage(fileMessage: FileMessage) {
        messages.add(fileMessage)
    }

//    fun addMessage(senderName: String, timeStamp: String, newTextMessage: String){
//        val message = TextMessage(senderName,timeStamp, newTextMessage)
//        println("Füge Message hinzu: $message")
//        messages.add(message)
//    }

    fun getUsername() = username

    fun setStatus(status: Boolean) {
        this.status = status
    }

    fun getStatus() = status

    fun getMessages() = messages
}