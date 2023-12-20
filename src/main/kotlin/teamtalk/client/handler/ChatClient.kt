package teamtalk.client.handler

import java.util.UUID

class ChatClient {

    private val uuid: UUID = UUID.randomUUID()
    private val username: String = "Max"

    private val handler = ClientHandler(this)

    suspend fun start() {
        handler.connect()
    }

    fun getHandler(): ClientHandler = handler

    fun getUsername(): String = username

    fun getUUID(): UUID = uuid
}