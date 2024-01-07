package teamtalk.client.handler

import teamtalk.client.ui.ClientGUI
import java.util.UUID

class ChatClient {

    private val handler = ClientHandler(this)
    private val gui = ClientGUI(this)

    private val uuid: UUID = UUID.randomUUID()
    var username: String = ""
        set(value) {
            field = value
            gui.currentUser = value
        }

    fun start(server: String, port: Int) {
        handler.connect(server, port)
    }

    fun getHandler() = handler

    fun getGUI() = gui

    fun isConnected() = handler.isConnected()

    fun getServerUsers() = handler.getContacts()

    fun getUUID() = uuid
}
