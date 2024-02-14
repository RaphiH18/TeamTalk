package teamtalk.server.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.message.FileMessage
import teamtalk.message.TextMessage
import teamtalk.server.handler.network.ServerClient
import teamtalk.server.handler.network.ServerHeader
import teamtalk.server.serverLogger.debug
import teamtalk.server.serverLogger.log
import teamtalk.utilities
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.time.Instant

class ServerHandler(private val chatServer: ChatServer) {

    //  CoroutineScope für Netzwerk und IO-Operationen
    private val handlerScope = CoroutineScope(Dispatchers.IO)

    private lateinit var serverSocket: ServerSocket
    private var isRunning = false
        set(value) {
            field = value
            chatServer.getGUI().updateStatus(value)
        }

    fun start() {
        handlerScope.launch {
            serverSocket = ServerSocket(chatServer.getPort(), 20, InetAddress.getByName(chatServer.getIP()))
            log("Der Server wurde gestartet (IP: ${chatServer.getIP()}, Port: ${chatServer.getPort()})")
            isRunning = true
            chatServer.getGUI().startRuntimeClock()
            chatServer.getGUI().deleteUserBTN.isDisable = true
            while (true) {
                try {
                    val socket = serverSocket.accept()
                    debug("Neue eingehende Verbindung: ${socket.inetAddress.hostAddress}")

                    launch {
                        val serverClient = ServerClient(socket)

                        while (!(socket.isClosed)) {
                            try {
                                process(serverClient)
                            } catch (e: Exception) {
                                val loggedOutUser = chatServer.getUser(serverClient)

                                if (loggedOutUser != null) {
                                    chatServer.getGUI().decreaseOnlineUsers()
                                    loggedOutUser.getStats().updateUsageTime()
                                    loggedOutUser.saveData()
                                    chatServer.getUser(serverClient)?.logout()
                                    chatServer.getStats().updateTotalAverageUsageTime()
                                    chatServer.getGUI().updateQuickStats()
                                    println(ServerHeader.STATUS_UPDATE.toJSON(this@ServerHandler))
                                    broadcast(ServerHeader.STATUS_UPDATE.toJSON(this@ServerHandler))
                                    log("Verbindung von ${loggedOutUser.getName()} (${serverClient.getSocket().inetAddress.hostAddress}) getrennt.")
                                } else {
                                    log("Verbindung von ${serverClient.getSocket().inetAddress.hostAddress} getrennt.")
                                }
                                break
                            }
                        }
                    }
                } catch (e: SocketException) {
                    log("Der Server wurde beendet (${e.message}).")
                    isRunning = false
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

        debug("<- Von Client erhalten (Header): $headerString")

        if (utilities.isJSON(headerString)) {
            val headerJSON = JSONObject(headerString)
            val payloadSize = headerJSON.getInt("payloadSize")

            when (headerJSON.get("type")) {
                "HELLO" -> {
                    serverClient.send(ServerHeader.HELLO_RESPONSE.toJSON(this, "SUCCESS"))
                }

                "LOGIN" -> {
                    val username = headerJSON.get("username").toString()
                    val user = chatServer.getUser(username)

                    if (user != null) {
                        user.login(serverClient)
                        log("Login erfolgreich: ${user.getName()} (${serverClient.getSocket().inetAddress.hostAddress}) hat sich eingeloggt.")
                        chatServer.getGUI().increaseOnlineUsers()
                        broadcast(ServerHeader.STATUS_UPDATE.toJSON(this))
                    } else {
                        serverClient.send(ServerHeader.LOGIN_RESPONSE.toJSON(this, "USER_NOT_EXISTS"))
                        debug("Login fehlgeschlagen: $username existiert nicht.")
                    }
                }

                "MESSAGE" -> {
                    val messageBytes = ByteArray(payloadSize)
                    serverClient.getInput().readFully(messageBytes)
                    val messageText = String(messageBytes, Charsets.UTF_8)

                    val senderName = headerJSON.getString("senderName")
                    val receiverName = headerJSON.getString("receiverName")

                    val receiverUser = chatServer.getUser(receiverName)

                    if (receiverUser != null) {
                        if (receiverUser.isOnline()) {
                            receiverUser.getClient()!!.send(headerJSON, messageBytes)

                            serverClient.send(
                                ServerHeader.MESSAGE_RESPONSE.toJSON(this, "FORWARDED", receiverName, senderName, messageBytes.size),
                                messageBytes
                            )

                            val message = TextMessage(senderName, receiverName, Instant.now(), messageText)
                            chatServer.getStats().newMessages.add(message)
                        }
                    } else {
                        serverClient.send(ServerHeader.MESSAGE_RESPONSE.toJSON(this, "USER_NOT_EXISTS", receiverName, senderName, messageBytes.size))
                    }
                }

                "FILE" -> {
                    val senderName = headerJSON.getString("senderName")
                    val receiverName = headerJSON.getString("receiverName")

                    val receiverUser = chatServer.getUser(receiverName)

                    if (receiverUser != null) {
                        if (receiverUser.isOnline()) {
                            receiverUser.getClient()!!.sendHeader(headerJSON)

                            val fileChunkBytes = ByteArray(4 * 1024)

                            var bytesReadTotal = 0
                            while (bytesReadTotal < payloadSize) {
                                val bytesRead = serverClient.getInput().read(fileChunkBytes)
                                bytesReadTotal += bytesRead
                                debug("<- Von Client erhalten: Nur Daten, eingelesen: $bytesRead")
                                receiverUser.getClient()!!.sendPayload(fileChunkBytes.copyOf(bytesRead))
                            }

                            val message = FileMessage(senderName, receiverName, Instant.now(), File(headerJSON.getString("filename")))
                            chatServer.getStats().newMessages.add(message)
                        }
                    } else {
                        serverClient.send(ServerHeader.MESSAGE_RESPONSE.toJSON(this, "USER_NOT_EXISTS", receiverName, senderName, headerJSON.getInt("payloadSize")))
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
        for (serverClient in chatServer.getClients()) {
            serverClient.send(header, payloadBytes)
        }
    }

    fun stop() {
        for (user in chatServer.getUsers()) {
            if (user.isOnline()) {
                try {
                    user.logout()
                    user.saveData()
                } catch (e: Exception) {
                    log("Fehler beim Schliessen der Client-Verbindung: ${e.message}")
                }
            }
        }

        try {
            serverSocket.close()
            chatServer.getGUI().stopRuntimeClock()
            chatServer.getGUI().deleteUserBTN.isDisable = false
        } catch (e: Exception) {
            log("Fehler beim Schliessen des ServerSockets: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getServer() = chatServer

    fun isRunning() = isRunning
}