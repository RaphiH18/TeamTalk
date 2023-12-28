package teamtalk.client.handler

import javafx.scene.control.*
import javafx.scene.layout.VBox
import java.util.UUID

class ChatClient {

    private val uuid: UUID = UUID.randomUUID()
    private var username: String = ""

    private val handler = ClientHandler(this)

    fun start(server: String, port: Int) {
        handler.connect(server, port)
    }

    fun getStatusMessage() = handler.getStatusMessage()

    fun getHandler(): ClientHandler = handler

    fun isConnected() = handler.isConnected()

    fun getServerUsers() = handler.getServerUsers()

    fun getUsername(): String = username

    fun setUsername(newUsername: String) {
        username = newUsername
    }

    fun getUUID(): UUID = uuid

    fun createBaseView(): VBox {
        val vBoxBase = VBox()
        val vBoxContent = createContentView()
        val menuBar = createMenuBar()

        with(vBoxBase.children) {
            add(menuBar)
            add(vBoxContent)
        }

        return vBoxBase
    }

    fun createContentView(): SplitPane {
        val splitPane = SplitPane()

        with(splitPane) {
            items.addAll(handler.createContactView(getUsername()))
            items.addAll(handler.createChattingView(getUsername()))
        }

        splitPane.setDividerPositions(0.3)
        return splitPane
    }

    fun createMenuBar() = bar(
        menu("Datei",
            item( "Schliessen", { System.exit(0) })
            )
        )

    private fun bar(vararg elements: Menu) = MenuBar().apply { getMenus().addAll(elements) }
    private fun menu(text: String, vararg elements: MenuItem) = Menu(text).apply { getItems().addAll(elements) }
    private fun item(text: String, method: () -> Unit) = MenuItem(text).apply { setOnAction { method() } }
    private fun separator() = SeparatorMenuItem()
}
