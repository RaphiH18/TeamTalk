package teamtalk.server.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.message.TextMessage
import teamtalk.jsonUtil
import teamtalk.logger.debug
import teamtalk.logger.log
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.time.Instant

class ServerHandler(private val server: ChatServer) {

    private lateinit var serverSocket: ServerSocket
    private val handlerScope = CoroutineScope(Dispatchers.IO)
    private var serverState = false

    fun start() {
        handlerScope.launch {
            println("handler: ${Dispatchers.IO.toString()}")
            serverSocket = ServerSocket(server.getPort(), 20, InetAddress.getByName(server.getIP()))
            log("Der Server wurde gestartet (IP: ${server.getIP()}, Port: ${server.getPort()})")
            serverState = true
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
                    serverState = false
                    break
                }
            }
        }
    }

    private fun process(serverClient: ServerClient) {
        println("Ready for next message")
        val headerSize = serverClient.getInput().readInt()
        println("Angekündigte Headersize: $headerSize")
        val headerBytes = ByteArray(headerSize)
        serverClient.getInput().readFully(headerBytes)
        val headerString = String(headerBytes, Charsets.UTF_8)

        debug("<- Von Client erhalten (Header): $headerString")

        if (jsonUtil.isJSON(headerString)) {
            val headerJSON = JSONObject(headerString)
            val payloadSize = headerJSON.getInt("payloadSize")

            when (headerJSON.get("type")) {
                "HELLO" -> {
                    serverClient.send(ServerHeader.HELLO_RESPONSE.toJSON(this, "SUCCESS"))
                }

                "LOGIN" -> {
                    serverClient.setUsername(headerJSON.get("username").toString())
                    serverClient.setLoginTime(Instant.now())
                    log("Verbindung zwischen (${serverClient.getUsername()}) und dem Server erfolgreich aufgebaut.")

                    broadcast(ServerHeader.STATUS_UPDATE.toJSON(this))
                }

                "MESSAGE" -> {
                    val messageBytes = ByteArray(payloadSize)
                    serverClient.getInput().readFully(messageBytes)
                    val messageText = String(messageBytes, Charsets.UTF_8)

                    val receiverClient = server.getClients().find { it.getUsername() == headerJSON.getString("receiverName") }

                    if (receiverClient != null) {
                        receiverClient.send(headerJSON, messageBytes)

                        serverClient.send(
                            ServerHeader.MESSAGE_RESPONSE.toJSON(
                                this, "FORWARDED", receiverClient.getUsername(), serverClient.getUsername(),
                                messageBytes.size
                            ),
                            messageBytes
                        )

                        val message = TextMessage(serverClient.getUsername(), receiverClient.getUsername(), Instant.now(), messageText)
                        server.getStats().newMessages.add(message)
                    } else {
                        serverClient.send(
                            ServerHeader.MESSAGE_RESPONSE.toJSON(
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
                    val receiverClient = server.getClients().find { it.getUsername() == headerJSON.getString("receiverName") }

                    if (receiverClient != null) {
                        println("File wird weitergeleitet!")
                        receiverClient.sendHeader(headerJSON)

                        val fileChunkBytes = ByteArray(4 * 1024)
//                        var amountBytesRead = 0
//                        while (true) {
//                            println("Bytes forwarded: $amountBytesRead")
//                            amountBytesRead = serverClient.getInput().read(fileChunkBytes.copyOf(amountBytesRead))
//
//                            if (amountBytesRead == -1) {
//                                break
//                            }
//                            receiverClient.sendPayload(fileChunkBytes)
//                        }

                        var bytesReadTotal = 0
                        while(bytesReadTotal < payloadSize) {
                            val bytesRead = serverClient.getInput().read(fileChunkBytes)
                            bytesReadTotal += bytesRead
                            debug("<- Von Server erhalten: Nur Daten, eingelesen: $bytesRead")
                            receiverClient.sendPayload(fileChunkBytes.copyOf(bytesRead))
                        }
                        println("Alles gesendet!")
                    } else {
                        serverClient.send(
                            ServerHeader.MESSAGE_RESPONSE.toJSON(
                                this,
                                "USER_OFFLINE",
                                headerJSON.getString("receiverName"),
                                serverClient.getUsername(),
                                headerJSON.getInt("payloadSize")
                            )
                        )
                    }
                }

                "BYE" -> {

                }
            }
        } else {
            log("Warnung beim Verarbeiten der Nachricht: Kein JSON-Objekt.")
        }
    }

    private fun broadcast(header: JSONObject, payloadBytes: ByteArray = byteArrayOf()) {
        for (serverClient in server.getClients()) {
            serverClient.send(header, payloadBytes)
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

    fun isRunning() = serverState
}