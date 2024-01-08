package teamtalk.client.handler

import org.json.JSONObject

enum class ClientMessage {
    HELLO, LOGIN, MESSAGE, FILE, BYE;

    fun toJSON(chatClient: ChatClient, receiverName: String = "", payloadSize: Int = 0): JSONObject {
        val type = this@ClientMessage.toString()

        return JSONObject().apply {
            put("type", type)
            put("payloadSize", payloadSize)

            when (type) {
                "MESSAGE", "FILE" -> {
                    put("senderName", chatClient.getUsername())
                    put("receiverName", receiverName)
                }
                "LOGIN" -> {
                    put("username", chatClient.getUsername())
                }
            }
        }
    }
}