package teamtalk.server.handler

import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.AnchorPane
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.jsonUtil
import teamtalk.server.logger.log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class ServerHandler(private val server: ChatServer) {

    private lateinit var serverSocket: ServerSocket

    suspend fun start() {
        coroutineScope {
            launch {
                serverSocket = ServerSocket(server.getPort(), 20, server.getIP())
                log("Der Server wurde gestartet (${server.getIP()}, port ${server.getPort()})")

                while(true) {
                    val socket = serverSocket.accept()
                    log("New Client connected: ${socket.inetAddress.hostAddress}")

                    launch {
                        val output = PrintWriter(socket.getOutputStream())
                        val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                        process(input.readLine(), socket)
                    }
                }
            }
        }
    }

    fun createControlView() : Node {
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

    private fun createHandlerArea() : Node {
        val handlerTabPane = TabPane().apply {
            prefWidth = 400.0

            with(tabs) {
                add(Tab("Dashboard").apply {
                    isClosable = false
                    content = AnchorPane()
                })

                add(Tab("Einstellungen").apply {
                    isClosable = false
                    content = AnchorPane()
                })
            }
        }

        val handlerPane = AnchorPane().apply {
            children.add(handlerTabPane)
        }

        return handlerPane
    }

    private fun createStatsArea() : Node {
        val statsPane = AnchorPane()

        return statsPane
    }

    fun process(receivedString: String, socket: Socket) {
        if (jsonUtil.isJSON(receivedString)) {
            val jsonObj = JSONObject(receivedString)

            when (jsonObj.get("type")) {
                "HELLO" -> {
                    val client = ServerClient(socket, jsonObj.get("username").toString())
                    server.getClients().add(client)
                    log("Connection between client (${client.getUsername()}) and server successfully established!")
                }
                "MESSAGE" -> {

                }
                "FILE" -> {

                }
                "BYE" -> {

                }
            }
        }
    }
}