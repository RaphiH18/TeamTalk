package teamtalk.server.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import teamtalk.server.handler.ChatServer
import teamtalk.logger

class ServerGUI(private val chatServer: ChatServer) {

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
            children.add(createControlView())
            children.add(logger.createServerView())
        }

        return vBoxContent
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
                        chatServer.start()
                    }
                })
                children.add(Button("Stop").also {
                    it.setOnAction {
                        chatServer.stop()
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