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

    val handlerScope = CoroutineScope(Dispatchers.IO)

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
                            process(serverClient)
                        }
                    }
                } catch (e: SocketException) {
                    log("Der Server wurde beendet (Socket closed).")
                    break
                }
            }
        }
    }

    private fun process(serverClient: ServerClient) {
        val headerSize = serverClient.getInput().readInt()
        val headerBytes = ByteArray(headerSize)
        serverClient.getInput().readFully(headerBytes)
        val headerString = String(headerBytes, Charsets.UTF_8)

        debug("<- Von Client erhalten: $headerString")

        if (jsonUtil.isJSON(headerString)) {
            val headerJSON = JSONObject(String(headerBytes, Charsets.UTF_8))
            val payloadSize = headerJSON.getInt("payloadSize")

            when (headerJSON.get("type")) {
                "HELLO" -> {
                    send(serverClient, ServerMessage.HELLO_RESPONSE.toJSON(this, "SUCCESS"))
                }

                "LOGIN" -> {
                    serverClient.setUsername(headerJSON.get("username").toString())
                    log("Verbindung zwischen (${serverClient.getUsername()}) und dem Server erfolgreich aufgebaut.")

                    broadcast(ServerMessage.STATUS_UPDATE.toJSON(this))
                }

                "MESSAGE" -> {
                    val messageBytes = ByteArray(payloadSize)
                    serverClient.getInput().readFully(messageBytes)

                    val receiverClient = server.getClients().find { it.getUsername() == headerJSON.getString("receiverName") }

                    if (receiverClient != null) {
                        send(receiverClient, headerJSON, messageBytes)
                        send(
                            serverClient,
                            ServerMessage.MESSAGE_RESPONSE.toJSON(
                                this,
                                "FORWARDED",
                                receiverClient.getUsername(),
                                serverClient.getUsername(),
                                messageBytes.size
                            ),
                            messageBytes
                        )
                    } else {
                        send(
                            serverClient,
                            ServerMessage.MESSAGE_RESPONSE.toJSON(
                                this,
                                "USER_OFFLINE",
                                headerJSON.getString("receiverName"),
                                serverClient.getUsername(),
                                messageBytes.size
                            )
                        )
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

    private fun send(serverClient: ServerClient, header: JSONObject, payloadBytes: ByteArray = byteArrayOf()) {
        handlerScope.launch {
            val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
            serverClient.getOutput().writeInt(headerBytes.size)
            serverClient.getOutput().write(headerBytes)

            if (payloadBytes.isNotEmpty()) {
                serverClient.getOutput().write(payloadBytes)
            }

            debug("-> An Client gesendet (Header): $header")
        }
    }

    private fun broadcast(header: JSONObject, payloadBytes: ByteArray = byteArrayOf()) {
        for (serverClient in server.getClients()) {
            send(serverClient, header, payloadBytes)
        }
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