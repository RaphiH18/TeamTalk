package teamtalk.server

import org.json.JSONArray
import org.json.JSONObject
import teamtalk.server.handler.ServerHandler

enum class ServerMessage {
    HELLO_RESPONSE, LOGIN_RESPONSE, MESSAGE, MESSAGE_RESPONSE, FILE_RESPONSE, STATUS_UPDATE, BYE_RESPONSE;

    fun getJSONString(serverHandler: ServerHandler, status: String = "", data: String = "", receiverName: String = ""): String {
        val type = this@ServerMessage.toString()

        return JSONObject().apply {
            put("type", type)

            if (type.contains("RESPONSE")) {
                put("status", status)
            }

            when(type) {
                "HELLO_RESPONSE"-> {
                    put("userList", JSONArray(serverHandler.getServer().getUsers()))
                }
                "STATUS_UPDATE" -> {
                    val clientNames = JSONArray()
                    for (client in serverHandler.getServer().getClients()) {
                        clientNames.put(client.getUsername())
                    }

                    put("userList", clientNames)

                }
                "LOGIN_RESPONSE" -> {
                    put("onlineUserList", JSONArray(serverHandler.getServer().getOnlineUsers()))
                }
                "MESSAGE_RESPONSE" -> {
                    put("receiverName" ,receiverName)
                    put("message", data)
                }
            }
        }.toString()
    }
}