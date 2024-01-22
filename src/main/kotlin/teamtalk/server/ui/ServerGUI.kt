package teamtalk.server.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.GREEN
import javafx.scene.shape.Circle
import teamtalk.logger
import teamtalk.server.handler.ChatServer

class ServerGUI(private val chatServer: ChatServer) {

    val MIN_WIDTH = 1200.0
    val MIN_HEIGHT = 800.0

    private val currentPortLbl = Label("4444")
    private val startBTN = Button("Start")
    private val stopBTN = Button("Stop")
    private val statusCIR = Circle(4.0)

    val portLbl = Label("Port").apply {
        padding = Insets(4.0, 45.0, 0.0, 0.0)
    }
    private val portTF = TextField("4444")
    private val ipTF = TextField("127.0.0.1")

    val applyBtn = Button("Übernehmen").apply {
        setOnAction {
            applySettings()
        }
    }

    var controlArea = createControlView()

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

    private fun applySettings(){
        currentPortLbl.text = portTF.text
        chatServer.setPort(portTF.text.toInt())
        chatServer.setIP(ipTF.text)
        logger.log("Einstellungen übernommen - IP: ${ipTF.text}, Port: ${portTF.text}")
    }

    private fun updateCircle(status: Boolean) {
        if (status) {
            statusCIR.fill = GREEN
        } else {
            statusCIR.fill = BLACK
        }
    }

    private fun createContentView(): VBox {
        val vBoxContent = VBox().apply {
            padding = Insets(10.0)
        }

        with(vBoxContent) {
            children.add(controlArea)
            children.add(logger.createServerView())
        }

        VBox.setVgrow(vBoxContent, Priority.ALWAYS)
        return vBoxContent
    }

    private fun createControlView(): SplitPane {
        controlArea = SplitPane()

        with(controlArea) {
            minHeight = 550.0

            items.add(createHandlerArea())
            items.add(createStatsArea())
        }

        VBox.setVgrow(controlArea, Priority.ALWAYS)
        return controlArea
    }

    private fun createHandlerArea(): Node {
        val handlerTabPane = TabPane().apply {
            stopBTN.isDisable = true

            val dashboard = VBox()
            dashboard.children.add(HBox().apply {
                children.add(statusCIR)
                children.add(Label("Server"))
                children.add(currentPortLbl)
                children.add(startBTN.also {
                    it.setOnAction {
                        chatServer.start()
                        startBTN.isDisable = true
                        stopBTN.isDisable = false
                        applyBtn.isDisable = true
                        updateCircle(true)
                    }
                })
                children.add(stopBTN.also {
                    it.setOnAction {
                        chatServer.stop()
                        stopBTN.isDisable = true
                        startBTN.isDisable = false
                        applyBtn.isDisable = false
                        updateCircle(false)
                    }
                })
                padding = Insets(10.0)
                spacing = 10.0
                alignment = Pos.CENTER_LEFT
            })

            with(tabs) {
                add(Tab("Dashboard").apply {
                    isClosable = false
                    content = dashboard
                })

                val ipLbl = Label("IP-Adresse").apply{
                    padding = Insets(4.0, 10.0, 0.0, 0.0)
                }

                val settingPortHb = HBox().apply{
                    with(children){
                        add(portLbl)
                        add(portTF)
                    }
                }

                val settingIPHb = HBox().apply{
                    with(children){
                        add(ipLbl)
                        add(ipTF)
                    }
                }

                val settingVb = VBox().apply{
                    with(children){
                        add(settingPortHb)
                        add(settingIPHb)
                        add(applyBtn)
                        padding = Insets(10.0)
                        spacing = 10.0
                    }
                }

                add(Tab("Einstellungen").apply {
                    isClosable = false
                    content = settingVb



                    /*
                    TODO: Settings im GUI verfügbar machen
                     */
                })
            }
        }

        return handlerTabPane
    }

    private fun createStatsArea(): Node {
        val statsTabPane = TabPane().apply {

        }

        return statsTabPane
    }

    private fun createMenuBar() = bar(
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