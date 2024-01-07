package teamtalk.client.handler

import teamtalk.client.ui.ClientGUI
import java.util.UUID

class ChatClient {

    private val handler = ClientHandler(this)
    private val gui = ClientGUI(this)

    private val uuid: UUID = UUID.randomUUID()
    private var username: String = ""

    fun start(server: String, port: Int) {
        handler.connect(server, port)
    }

    fun getHandler() = handler

    fun getGUI() = gui

    fun getStatusMessage() = handler.getStatusMessage()

    fun isConnected() = handler.isConnected()

    fun getServerUsers() = handler.getServerUsers()

    fun getUUID() = uuid

    fun getUsername() = username

    fun setUsername(newUsername: String) {
        username = newUsername
        gui.setCurrentUser(username)
    }
}
