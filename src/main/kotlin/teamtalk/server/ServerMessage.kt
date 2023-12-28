package teamtalk.server

import org.json.JSONArray
import org.json.JSONObject
import teamtalk.server.handler.ServerHandler

enum class ServerMessage {
    HELLO_RESPONSE, LOGIN_RESPONSE, MESSAGE, MESSAGE_RESPONSE, FILE_RESPONSE, BYE_RESPONSE;

    fun getJSONString(status: String, serverHandler: ServerHandler): String {
        val type = this@ServerMessage.toString()

        return JSONObject().apply {
            put("type", type)

            if (type.contains("RESPONSE")) {
                put("status", status)
            }

            when(type) {
                "HELLO_RESPONSE" -> {
                    put("userList", JSONArray(serverHandler.getServer().getUsers()))
                }
                "LOGIN_RESPONSE" -> {
                    put("onlineUserList", JSONArray(serverHandler.getServer().getOnlineUsers()))
                }
                "MESSAGE" -> {

                }
            }
        }.toString()
    }
}