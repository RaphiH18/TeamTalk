package teamtalk.server.handler

import org.json.JSONArray
import org.json.JSONObject

enum class ServerHeader {

    HELLO_RESPONSE, LOGIN_RESPONSE, MESSAGE_RESPONSE, FILE_RESPONSE, STATUS_UPDATE;

    fun toJSON(serverHandler: ServerHandler, status: String = "", receiverName: String = "", senderName: String = "", payloadSize: Int = 0): JSONObject {
        val type = this@ServerHeader.toString()

        return JSONObject().apply {
            put("type", type)
            put("payloadSize", payloadSize)

            if (type.contains("RESPONSE")) {
                put("status", status)
            }

            when(type) {
                "HELLO_RESPONSE"-> {
                    put("userList", JSONArray(serverHandler.getServer().getUsers()))
                }

                "LOGIN_RESPONSE" -> {
                    put("onlineUserList", JSONArray(serverHandler.getServer().getOnlineUsers()))
                }

                "MESSAGE_RESPONSE" -> {
                    put("senderName", senderName)
                    put("receiverName", receiverName)
                }

                "STATUS_UPDATE" -> {
                    val onlineNames = JSONArray()
                    for (client in serverHandler.getServer().getClients()) {
                        onlineNames.put(client.getUsername())
                    }

                    put("onlineUserList", onlineNames)
                }
            }
        }
    }
}