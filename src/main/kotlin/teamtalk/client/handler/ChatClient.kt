package teamtalk.client.handler

import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.VBox
import java.io.IOException
import java.util.ArrayList
import java.util.UUID

class ChatClient {

    private val uuid: UUID = UUID.randomUUID()
    private val username: String = "Max"

    private val handler = ClientHandler(this)

    fun start() {
        handler.connect()
    }

    fun getHandler(): ClientHandler = handler

    fun getConnectStatus(): Boolean {
        return handler.getConnectStatus()
    }

    fun isUserListStatus(): Boolean {
        return handler.isUserListStatus()
    }

    fun getUserList(): ArrayList<String> {
        return handler.getUserList()
    }
    fun getUsername(): String = username

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
            items.addAll(handler.createContactView())
            items.addAll(handler.createChattingView())
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
