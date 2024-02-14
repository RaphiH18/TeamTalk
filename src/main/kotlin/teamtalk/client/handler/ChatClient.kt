package teamtalk.client.handler

import teamtalk.client.ui.ClientGUI

class ChatClient {

    private val handler = ClientHandler(this)
    private val gui = ClientGUI(this)

    private var username: String = ""

    fun start(server: String, port: Int) {
        handler.connect(server, port)
    }

    fun getHandler() = handler

    fun getGUI() = gui

    fun isConnected() = handler.isConnected()

    fun getServerUsers() = handler.getContacts()

    fun getUsername() = username

    fun setUsername(username: String) {
        this.username = username
    }
}
