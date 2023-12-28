package teamtalk.client

class Contact(username: String) {
    private var message: String = ""
    //private var messageHistory = MutableList<String>

    fun updateMessage(newMessage: String){
        this.message = this.message + "\n" + newMessage
        println("Updated Message: ${this.message}")
    }

    fun getMessage() = message
}