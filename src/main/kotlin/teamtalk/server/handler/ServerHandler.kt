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
                        val output = PrintWriter(socket.getOutputStream())
                        val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                        while (socket.isConnected) {
                            process(input.readLine(), socket, output)
                        }
                    }
                } catch (e: SocketException) {
                    log("Der Server wurde beendet (Socket closed).")
                    break
                }
            }
        }
    }

    private fun process(receivedString: String, socket: Socket, output: PrintWriter) {
        debug("<- Von Client erhalten: $receivedString")

        if (jsonUtil.isJSON(receivedString)) {
            val jsonObj = JSONObject(receivedString)

            when (jsonObj.get("type")) {
                "HELLO" -> {
                    send(ServerMessage.HELLO_RESPONSE.getJSONString(this, "SUCCESS"), output)
                }

                "LOGIN" -> {
                    val client = ServerClient(socket, jsonObj.get("username").toString())
                    server.getClients().add(client)
                    log("Verbindung zwischen (${client.getUsername()}) und dem Server erfolgreich aufgebaut.")

                    broadcast(ServerMessage.STATUS_UPDATE.getJSONString(this))
                }

                "MESSAGE" -> {
                    val receiverName = jsonObj.get("receiverName").toString()
                    val message =  jsonObj.get("message").toString()
                    var receiverFound = false
                    for (client in server.getClients()) {
                        if (client.getUsername() == receiverName.toString()) {
                            println("client.getUsername(): ${client.getUsername()} == receiverName: ${receiverName}")
                            receiverFound = true

                            client.getOutput().println(receivedString)
                            client.getOutput().flush()

                            send(ServerMessage.MESSAGE_RESPONSE.getJSONString(this, "FORWARDED", message, receiverName), output)
                            break
                        }
                    }

                    if (!(receiverFound)) {
                        send(ServerMessage.MESSAGE_RESPONSE.getJSONString(this, "USER_OFFLINE"), output)
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
            val output = PrintWriter(client.getOutput())
            send(string, output)
        }
    }

    private fun send(string: String, output: PrintWriter) {
        output.println(string)
        output.flush()
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