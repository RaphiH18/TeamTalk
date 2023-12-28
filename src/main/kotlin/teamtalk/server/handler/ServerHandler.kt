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
import org.json.JSONArray
import org.json.JSONObject
import teamtalk.jsonUtil
import teamtalk.server.ServerMessage
import teamtalk.server.logger.debug
import teamtalk.server.logger.log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServerHandler(private val server: ChatServer) {

    private lateinit var serverSocket: ServerSocket
    private lateinit var output: PrintWriter
    private lateinit var input: BufferedReader

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
                        debug("new coroutine for client launched")
                        output = PrintWriter(socket.getOutputStream())
                        input = BufferedReader(InputStreamReader(socket.getInputStream()))

                        while(socket.isConnected) {
                            debug("waiting for incoming msgs")
                            process(input.readLine(), socket)
                        }
                    }
                } catch (e: SocketException) {
                    log("Der Server wurde beendet (Socket closed).")
                    break
                }
            }
        }
    }

    private fun process(receivedString: String, socket: Socket) {
        debug("<- Von Client erhalten: $receivedString")

        if (jsonUtil.isJSON(receivedString)) {
            val jsonObj = JSONObject(receivedString)

            when (jsonObj.get("type")) {
                "HELLO" -> {
                    send(ServerMessage.HELLO_RESPONSE.getJSONString("SUCCESS", this))
                }

                "LOGIN" -> {
                    val client = ServerClient(socket, jsonObj.get("username").toString())
                    server.getClients().add(client)
                    log("Verbindung zwischen (${client.getUsername()}) und dem Server erfolgreich aufgebaut.")
                }

                "MESSAGE" -> {

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

    fun send(string: String) {
        if (this::output.isInitialized) {
            output.println(string)
            output.flush()
            debug("-> An Client gesendet: $string")
        } else {
            throw IllegalStateException("Server nicht gestartet - bitte Server erst starten.")
        }
    }

    fun stop() {
        try {
            for (client in server.getClients()) {
                client.getInput().close()
                client.getOutput().close()
                client.getSocket().close()
            }
        } catch (e: Exception) {
            log("Fehler beim Schliessen der Client-Verbindungen: ${e.message}")
        } finally {
            serverSocket.close()
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
                        start()
                    }
                })
                children.add(Button("Stop").also {
                    it.setOnAction {
                        stop()
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