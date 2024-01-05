package teamtalk.server.handler

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.jsonUtil
import teamtalk.server.ServerMessage
import teamtalk.server.logger.debug
import teamtalk.server.logger.log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServerHandler(private val server: ChatServer) {

    private lateinit var serverSocket: ServerSocket

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)
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
                        send(receivedString, receiverClient)
                        send(ServerMessage.MESSAGE_RESPONSE.getJSONString(this, "FORWARDED", message, receiverName, senderName), serverClient)
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

    fun createControlView(): Node {
        val controlArea = SplitPane()

        with(controlArea) {
            setDividerPositions(0.5)
            prefHeight = 375.0

            items.addAll(createHandlerArea(), createStatsArea())

            dividers[0].positionProperty().addListener { observable, oldValue, newValue ->
                setDividerPositions(0.5)
            }
        }
        return controlArea
    }

    private fun createHandlerArea(): Node {
        val handlerTabPane = TabPane().apply {
            prefWidth = 400.0

            val dashboard = VBox()

            dashboard.children.add(HBox().apply {
                children.add(Circle(4.0))
                children.add(Label("Server"))
                children.add(Label("Port: 4444"))
                children.add(Button("Start").also {
                    it.setOnAction {
                        server.start()
                    }
                })
                children.add(Button("Stop").also {
                    it.setOnAction {
                        server.stop()
                    }
                })
                padding = Insets(10.0)
                spacing = 10.0
                alignment = Pos.CENTER_LEFT
            })

            val settAnchPane = AnchorPane().apply {
                children.add(Button())
            }

            with(tabs) {
                add(Tab("Dashboard").apply {
                    isClosable = false
                    content = dashboard
                })

                add(Tab("Einstellungen").apply {
                    isClosable = false
                    content = settAnchPane
                })
            }
        }

        val handlerPane = AnchorPane().apply {
            children.add(handlerTabPane)
        }

        return handlerPane
    }

    private fun createStatsArea(): Node {
        val statsPane = AnchorPane()

        return statsPane
    }

    fun getServer() = server
}