package teamtalk.server.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.jsonUtil
import teamtalk.logger.debug
import teamtalk.logger.log
import java.net.ServerSocket
import java.net.SocketException

class ServerHandler(private val server: ChatServer) {

    private lateinit var serverSocket: ServerSocket

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        handlerScope.launch {
            serverSocket = ServerSocket(server.getPort(), 20, server.getIP())
            log("Der Server wurde gestartet (IP: ${server.getIP()}, Port: ${server.getPort()})")

            while (true) {
                try {
                    val socket = serverSocket.accept()
                    debug("Neue eingehende Verbindung: ${socket.inetAddress.hostAddress}")

                    launch {
                        val serverClient = ServerClient(socket)
                        server.getClients().add(serverClient)

                        while (socket.isConnected) {
                            process(serverClient.getInput().readLine(), serverClient)
                        }
                    }
                } catch (e: SocketException) {
                    log("Der Server wurde beendet (Socket closed).")
                    break
                }
            }
        }
    }

    private fun process(receivedString: String, serverClient: ServerClient) {
        debug("<- Von Client erhalten: $receivedString")

        if (jsonUtil.isJSON(receivedString)) {
            val jsonObj = JSONObject(receivedString)

            when (jsonObj.get("type")) {
                "HELLO" -> {
                    send(ServerMessage.HELLO_RESPONSE.getJSONString(this, "SUCCESS"), serverClient)
                }

                "LOGIN" -> {
                    serverClient.setUsername(jsonObj.get("username").toString())
                    log("Verbindung zwischen (${serverClient.getUsername()}) und dem Server erfolgreich aufgebaut.")

                    broadcast(ServerMessage.STATUS_UPDATE.getJSONString(this))
                }

                "MESSAGE" -> {
                    val receiverName = jsonObj.getString("receiverName").toString()
                    val senderName = jsonObj.getString("senderName")
                    val message = jsonObj.get("message").toString()
                    val receiverClient = server.getClients().find { it.getUsername() == receiverName }

                    if (receiverClient != null) {
                        send(ServerMessage.MESSAGE_RESPONSE.getJSONString(this, "FORWARDED", message, receiverName, senderName), serverClient)
                        send(receivedString, receiverClient)
                    } else {
                        send(ServerMessage.MESSAGE_RESPONSE.getJSONString(this, "USER_OFFLINE"), serverClient)
                    }
                }

                "FILE" -> {

                }

                "BYE" -> {

                }
            }
        } else {
            log("Warnung beim Verarbeiten der Nachricht: Kein JSON-Objekt.")
        }
    }

    private fun broadcast(string: String) {
        for (client in server.getClients()) {
            send(string, client)
        }
    }

    private fun send(string: String, serverClient: ServerClient) {
        serverClient.getOutput().println(string)
        serverClient.getOutput().flush()
        debug("-> An Client gesendet: $string")
    }

    fun stop() {
        for (client in server.getClients()) {
            try {
                client.getInput().close()
                client.getOutput().close()
                client.getSocket().close()
            } catch (e: Exception) {
                log("Fehler beim Schliessen der Client-Verbindung: ${e.message}")
            }
        }

        try {
            serverSocket.close()
        } catch (e: Exception) {
            log("Fehler beim Schliessen des ServerSockets: ${e.message}")
        }
    }

    fun getServer() = server
}