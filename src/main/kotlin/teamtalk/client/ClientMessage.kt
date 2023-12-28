package teamtalk.client

import org.json.JSONObject
import teamtalk.client.handler.ChatClient

enum class ClientMessage {
    HELLO, LOGIN, MESSAGE, FILE, BYE;

    fun getJSONString(client: ChatClient, message: String = "", receiverName: String = ""): String {
        val type = this@ClientMessage.toString()

        return JSONObject().apply {
            put("type", type)

            if (type != "HELLO") {
                put("uuid", client.getUUID())
                put("username", client.getUsername())

                if (type == "MESSAGE") {
                    put("receiverName", receiverName)
                    put("message", message)
                }

                if (type == "FILE") {
                    /*
                    TODO: Implementation File-Übertragung
                     */
                }
            }
        }.toString()
    }
}