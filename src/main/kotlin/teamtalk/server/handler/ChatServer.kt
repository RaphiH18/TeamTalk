package teamtalk.server.handler

import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.VBox
import teamtalk.server.logger
import java.net.InetAddress

class ChatServer(port: Int) {

    private val IP = InetAddress.getLoopbackAddress()
    private val PORT = port

    private val clients = mutableListOf<ServerClient>()
    private val handler: ServerHandler = ServerHandler(this)

    fun start() {
        handler.start()
    }

    fun stop() {
        handler.stop()
    }

    fun getIP() = IP

    fun getPort() = PORT

    fun getClients() = clients

    fun getUsers() =
        mutableListOf("Raphael Hegi", "Lukas Ledergerber", "Yannick Meier", "Budei Babdei", "Sone Anderi Person")

    fun getOnlineUsers(): MutableList<String> {
        val onlineUsers = mutableListOf<String>()

        for (client in clients) {
            onlineUsers.add(client.getUsername())
        }
        return onlineUsers
    }

    fun createBaseView(): VBox {
        val vBoxBase = VBox()
        val vBoxContent = createContentView()
        val menuBar = createMenuBar()

        with(vBoxBase) {
            children.add(menuBar)
            children.add(vBoxContent)
        }

        return vBoxBase
    }

    fun createContentView(): VBox {
        val vBoxContent = VBox().apply {
            padding = Insets(10.0)
        }

        with(vBoxContent) {
            children.add(handler.createControlView())
            children.add(logger.createServerView())
        }

        return vBoxContent
    }

    fun createMenuBar() = bar(
        menu(
            "Datei",
            item("Schliessen", { System.exit(0) })
        )
    )

    private fun bar(vararg elements: Menu) = MenuBar().apply { getMenus().addAll(elements) }
    private fun menu(text: String, vararg elements: MenuItem) = Menu(text).apply { getItems().addAll(elements) }
    private fun item(text: String, method: () -> Unit) = MenuItem(text).apply { setOnAction { method() } }
    private fun separator() = SeparatorMenuItem()
}