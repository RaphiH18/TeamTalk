package teamtalk.client.handler

import org.json.JSONObject

enum class ClientHeader {
    HELLO, LOGIN, MESSAGE, FILE, BYE;

    fun toJSON(chatClient: ChatClient, receiverName: String = "", payloadSize: Long = 0, filename: String = ""): JSONObject {
        val type = this@ClientHeader.toString()

        return JSONObject().apply {
            put("type", type)
            put("payloadSize", payloadSize)

            when (type) {
                "MESSAGE" -> {
                    put("senderName", chatClient.getUsername())
                    put("receiverName", receiverName)
                }
                "FILE" -> {
                    put("senderName", chatClient.getUsername())
                    put("receiverName", receiverName)
                    put("filename", filename)
                }
                "LOGIN" -> {
                    put("username", chatClient.getUsername())
                }
            }
        }
    }
}