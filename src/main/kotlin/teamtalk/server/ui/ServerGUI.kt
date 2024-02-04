package teamtalk.server.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color.*
import javafx.scene.shape.Circle
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import teamtalk.logger
import teamtalk.server.handler.ChatServer
import teamtalk.server.handler.ServerUser
import teamtalk.server.stats.charts.SummarizedFillWordsChart
import java.time.Duration
import java.time.Instant
import javax.print.attribute.standard.RequestingUserName
import kotlin.math.exp
import kotlin.system.exitProcess

class ServerGUI(private val chatServer: ChatServer) {

    //  CoroutineScope für JavaFX Thread
    val guiScope = CoroutineScope(Dispatchers.JavaFx)

    //  -------- Globale GUI-Variablen/Values --------
    val MIN_WIDTH = 1200.0
    val MIN_HEIGHT = 800.0

    //  Globale Variablen/Values für den "Dashboard"-Tab
    private val statusCIR = statusCIR()
    private val currentStateLBL = Label("Der TeamTalk Server ist gestoppt.")
    private var startBTN = startBTN()
    private var stopBTN = stopBTN()
    private val currentPortLBL = Label("4444")
    private val currentRuntimeLBL = Label("00:00:00")
    private val totalUsersLBL = Label("0")
    private val onlineUsersLBL = Label("0")
    private lateinit var runtimeClock: Job

    //  Globale Variablen/Values für den "Einstellungen"-Tab
    private val portTF = TextField("4444")
    private val ipTF = TextField("127.0.0.1")
    private val applyBTN = applyBTN()

    //  Globale Variablen/Values für den "Detaillierte Statistik"-Tab
    private var selectedStatsVB = selectedStatsVB()
    private var detailedStatsMB = detailedStatsMB()

    var controlArea = createControlSP()

    fun create(): VBox {
        return VBox().apply {
            children.add(createMenuBar())
            children.add(createContentVB())
        }
    }

    private fun createMenuBar() = bar(
        menu(
            "Datei",
            item("Schliessen") { exitProcess(0) }
        )
    )

    private fun createContentVB(): VBox {
        return VBox().apply {
            padding = Insets(10.0)

            children.add(controlArea)
            children.add(logger.createServerView())

            VBox.setVgrow(this, Priority.ALWAYS)
        }
    }

    private fun createControlSP(): SplitPane {
        return SplitPane().apply {
            minHeight = 550.0
            items.add(createControlTbPn())
            items.add(createStatisticTbPn())
            VBox.setVgrow(this, Priority.ALWAYS)

            controlArea = this
        }
    }

    private fun createControlTbPn(): TabPane {
        val controlTP = TabPane().apply {
            stopBTN.isDisable = true

            with(tabs) {
                add(createDashboardTab())
                add(createSettingsTab())
            }
        }

        return controlTP
    }

    private fun createDashboardTab(): Tab {
        val dashboardVB = VBox()
        dashboardVB.padding = Insets(10.0)
        dashboardVB.spacing = 5.0

        dashboardVB.children.add(Label("Start / Stop"))
        dashboardVB.children.add(Separator())

        dashboardVB.children.add(HBox().apply {
            children.add(statusCIR)
            children.add(currentStateLBL)
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
        })

        dashboardVB.children.add(HBox().apply {
            children.add(startBTN)
            children.add(stopBTN)
            alignment = Pos.CENTER_LEFT
            padding = Insets(5.0, 0.0, 15.0, 20.0)
            spacing = 15.0
        })

        dashboardVB.children.add(Label("Übersicht"))
        dashboardVB.children.add(Separator())

        dashboardVB.children.add(HBox().apply {
            padding = Insets(0.0, 0.0, 15.0, 20.0)
            spacing = 15.0

            children.add(VBox().apply {
                children.add(Label("Port:"))
                children.add(Label("Laufzeit:"))
                children.add(Label("Benutzer total:"))
                children.add(Label("Benutzer online:"))
            })

            children.add(VBox().apply {
                children.add(currentPortLBL)
                children.add(currentRuntimeLBL)
                children.add(totalUsersLBL)
                children.add(onlineUsersLBL)
            })
        })

        return Tab("Dashboard").apply {
            content = dashboardVB
            isClosable = false
        }
    }

    private fun createSettingsTab(): Tab {
        val settingsVB = VBox()
        settingsVB.padding = Insets(10.0)
        settingsVB.spacing = 5.0

        val networkSettingsGP = GridPane().apply {
            vgap = 5.0
            hgap = 10.0
            add(Label("IP-Adresse:"), 0, 0)
            add(ipTF, 1, 0)
            add(Label("Port:"), 0, 1)
            add(portTF, 1, 1)
        }

        settingsVB.children.add(Label("Netzwerkkonfiguration"))
        settingsVB.children.add(Separator())
        settingsVB.children.add(networkSettingsGP)
        settingsVB.children.add(applyBTN)

        return Tab("Einstellungen").apply {
            content = settingsVB
            isClosable = false
        }
    }

    private fun createStatisticTbPn(): Node {
        val chartsGP = GridPane().apply {
            val globalCharts = chatServer.getStats().globalCharts

            for (i in 0..(globalCharts.size - 1)) {
                val columnIndex = i % 2
                val rowIndex = i / 2

                add(globalCharts[i].getChart(), columnIndex, rowIndex)
            }
        }

        val statsTiPn = TitledPane("Statistiken", chartsGP)
        val overviewTiPn = TitledPane("Übersicht", (Label("Inhalt Übersicht")))

        val globalStatsARC = Accordion().apply {
            expandedPane = statsTiPn

            with(panes) {
                add(statsTiPn)
                add(overviewTiPn)
            }
        }

        val detailedStatisticVB = VBox().apply {
            padding = Insets(10.0)
            spacing = 10.0
            children.add(HBox().apply {
                spacing = 10.0
                alignment = Pos.CENTER_LEFT

                children.add(Label("Statistiken anzeigen für:"))
                children.add(detailedStatsMB)
            })

            children.add(selectedStatsVB)
        }

        val statsTabPane = TabPane().apply {
            with(tabs) {
                add(Tab("Globale Statistik").apply {
                    isClosable = false
                    content = globalStatsARC
                })
                add(Tab("Detailierte Statistik").apply {
                    isClosable = false
                    content = detailedStatisticVB
                })
            }
        }

        return statsTabPane
    }

    fun updateStatus(status: Boolean) {
        guiScope.launch {
            if (status) {
                statusCIR.fill = GREEN
                currentStateLBL.text = "Der TeamTalk Server läuft"
            } else {
                statusCIR.fill = DARKRED
                currentStateLBL.text = "Der TeamTalk Server ist gestoppt."
            }
        }
    }


    private fun applySettings() {
        currentPortLBL.text = portTF.text
        chatServer.setPort(portTF.text.toInt())
        chatServer.setIP(ipTF.text)
        logger.log("Einstellungen übernommen - IP: ${ipTF.text}, Port: ${portTF.text}")
    }

    /*
    --------------------------------------------------------------------

    Hilfsmethoden für GUI-Kreierung

    --------------------------------------------------------------------
     */
    private fun bar(vararg elements: Menu) = MenuBar().apply { getMenus().addAll(elements) }
    private fun menu(text: String, vararg elements: MenuItem) = Menu(text).apply { getItems().addAll(elements) }
    private fun item(text: String, method: () -> Unit) = MenuItem(text).apply { setOnAction { method() } }
    private fun separator() = SeparatorMenuItem()

    private fun statusCIR(): Circle {
        return Circle(5.0).apply {
            fill = DARKRED
        }
    }

    private fun startBTN() : Button {
        return Button("Start").apply {
            setOnAction {
                chatServer.start()
                startBTN.isDisable = true
                stopBTN.isDisable = false
                applyBTN.isDisable = true
                updateStatus(true)
            }
        }
    }

    private fun stopBTN() : Button {
        return Button("Stop").apply {
            isDisable = true
            setOnAction {
                chatServer.stop()
                stopBTN.isDisable = true
                startBTN.isDisable = false
                applyBTN.isDisable = false
                updateStatus(false)
            }
        }
    }

    private fun applyBTN() : Button {
        return Button("Übernehmen").apply {
            setOnAction {
                applySettings()
            }
        }
    }

    private fun selectedStatsVB() = VBox().apply {
        children.add(Label("Bitte wähle eine Ansicht aus."))
    }

    private fun statsGP(serverUser: ServerUser) = GridPane().apply {
        val userCharts = serverUser.getStats().charts

        for (i in 0..(userCharts.size - 1)) {
            val columnIndex = i % 2
            val rowIndex = i / 2

            add(userCharts[i].getChart(), columnIndex, rowIndex)
        }
    }

    private fun detailedStatsMB(): MenuButton {
        return MenuButton("Benutzer auswählen").apply {
            val menuButton = this

            items.add(MenuItem("Totalisiert").apply {
                setOnAction {
                    selectedStatsVB.children.clear()
                    selectedStatsVB.children.add(summarizedChartsVB())
                    menuButton.text = "Totalisiert"
                }
            })


            for ((index, user) in chatServer.getUserNames().withIndex()) {
                val buttonText = "Benutzer ${index + 1}"

                items.add(MenuItem(buttonText).apply {
                    setOnAction {
                        val selectedUser = chatServer.getUser(user)

                        if (selectedUser != null) {
                            if (selectedUser.isOnline()) {
                                println("online")
                                selectedStatsVB.children.clear()
                                selectedStatsVB.children.add(statsGP(selectedUser))
                                menuButton.text = buttonText
                            }
                        } else {
                            println("offline")
                            selectedStatsVB.children.clear()
                        }
                    }
                })
            }
        }
    }

    private fun summarizedChartsVB(): VBox {
        return VBox().apply {
            spacing = 10.0

            for(chart in chatServer.getStats().detailedCharts) {
                children.add(chart.getChart())
            }
        }
    }

    fun updateUserList(user: ServerUser) {
        val buttonText = "Benutzer ${user.getIndex() + 1}"

        detailedStatsMB.items.add(MenuItem(buttonText).apply {
            setOnAction {
                if (user.isOnline()) {
                    println("online")
                    selectedStatsVB.children.clear()
                    selectedStatsVB.children.add(statsGP(user))
                    detailedStatsMB.text = buttonText
                }
            }
        })

        for (chart in chatServer.getStats().detailedCharts) {
            chart.update()
        }

        for (chart in user.getStats().charts) {
            chart.update()
        }
    }

    fun startRuntimeClock() {
        val startTime: Instant = Instant.now()

        runtimeClock = guiScope.launch {
            while (isActive) {
                val duration = Duration.between(startTime, Instant.now())

                currentRuntimeLBL.text = String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart())

                delay(1000)
            }
        }
    }

    fun stopRuntimeClock() {
        guiScope.launch {
            runtimeClock.cancel()
            currentRuntimeLBL.text = "00:00:00"
        }
    }
}