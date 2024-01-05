package teamtalk.client

import org.json.JSONObject
import teamtalk.client.handler.ChatClient

enum class ClientMessage {
    HELLO, LOGIN, MESSAGE, FILE, BYE;

    fun getJSONString(client: ChatClient, message: String = "", receiverName: String = ""): String {
        val type = this@ClientMessage.toString()

        return JSONObject().apply {
            put("type", type)

            when(type) {
                "LOGIN" -> {
                    put("uuid", client.getUUID())
                    put("username", client.getUsername())
                }
                "MESSAGE" -> {
                    put("uuid", client.getUUID())
                    put("senderName", client.getUsername())
                    put("receiverName", receiverName)
                    put("message", message)
                }
                "FILE" -> {
                    /*
                    TODO: Implementation File-Übertragung
                     */
                }
            }
        }.toString()
    }
}