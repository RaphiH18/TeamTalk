package teamtalk.server.ui

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableArray
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.chart.*
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Color.*
import javafx.scene.shape.Circle
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import teamtalk.logger
import teamtalk.server.handler.ChatServer

class ServerGUI(private val chatServer: ChatServer) {

    val guiScope = CoroutineScope(Dispatchers.JavaFx)

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

    val applyBTN = Button("Übernehmen").apply {
        setOnAction {
            applySettings()
        }
    }

    var controlArea = createControlView()
    private var pieChartData = FXCollections.observableArrayList<PieChart.Data>()
    private var fillKeywordPIC = PieChart().apply {
        title = "Verwendete Füllwörter insgesamt"
    }

    private val xAxis = CategoryAxis().apply {
        categories = FXCollections.observableArrayList("")
        //categories = FXCollections.observableArrayList("Positiv", "Neutral", "Negativ")
        label = "Benutzer"
    }
    private val yAxis = NumberAxis().apply {
        label = "Anzahl"
    }
    /*private val positiveKeywords = XYChart.Series<String, Number>()
    private val neutralKeywords = XYChart.Series<String, Number>()
    private val negativeKeywords = XYChart.Series<String, Number>()
    */

    private val positiveKeywords = XYChart.Series<String, Number>().apply {
        name = "Positiv"
    }
    private val neutralKeywords = XYChart.Series<String, Number>().apply {
        name = "Neutral"
    }
    private val negativeKeywords = XYChart.Series<String, Number>().apply {
        name = "Negativ"
    }

    private val ratingKeywords = mutableListOf(positiveKeywords, neutralKeywords, negativeKeywords)
    //private lateinit var ratingKeywords: MutableList<List<XYChart<String, Number>>>


    //private val ratingKeywordData = XYChart.Series<String, Number>()
    private lateinit var ratingKeywordData: MutableList<XYChart.Series<String, Double>>
    private val ratingKeywordBAC = BarChart(xAxis, yAxis).apply {
        title = "Verwendete Wertungswörter pro Benutzer"
    }

    private var ratingKeywordUsers = mutableListOf<String>()
//    private var ratingKeywordUsers = listOf("Benutzer1", "Benutzer2", "Benutzer3")


    var initiate = true

    fun updateCharts(
        fillKeywordChartData: ObservableList<PieChart.Data>,
        ratingKeywordChartData: MutableList<Pair<String, List<XYChart.Series<String, Number>>>>
    ) {

        if (initiate) {
            initiate = false
            guiScope.launch {
                fillKeywordPIC.data = pieChartData
                ratingKeywordBAC.data.addAll(ratingKeywords)
            }

        }

        updateFillKeywordChart(fillKeywordChartData)
        sortRatingKeywordChart(ratingKeywordChartData)
    }

    /*fun updateRatingKeywordChart(
        filteredRatingKeywordChartData: ObservableList<XYChart.Data<String, Number>>,
        filterNumber: Int
    ) {
        var newRatingKeyword = true
        for (inputRating in filteredRatingKeywordChartData) {
            for (currentRating in ratingKeywords[filterNumber].data) {
                println("InputRating: X" + inputRating.xValue)
                println("InputRating: Y" + inputRating.yValue)
                println("CurrentRating X: " + currentRating.xValue)
                println("CurrentRating Y: " + currentRating.yValue)
                if (currentRating.xValue == inputRating.xValue) {
                    newRatingKeyword = false
                    if (currentRating.yValue.toLong() < inputRating.yValue.toLong()) {
                        println("New Input Value is hiher")
                        currentRating.yValue = inputRating.yValue
                        guiScope.launch {
                            ratingKeywordBAC.layout()
                        }

                    }
                    break
                }
            }
            if (newRatingKeyword) {
                println("Neuer Keyword: " + inputRating)
                guiScope.launch {
                    ratingKeywords[filterNumber].data.add(inputRating)
                }
            }
        }
    }*/

    //fun sortRatingKeywordChart(ratingKeywordChartData: List<XYChart.Series<String, Number>>) {
    /*fun sortRatingKeywordChart(ratingKeywordChartData: MutableList<Pair<String, List<XYChart.Series<String, Number>>>>) {
        for (i in 0..2) {
            when (i) {
                0 -> {
                    updateRatingKeywordChart(ratingKeywordChartData[i].data, i)
                }

                1 -> {
                    updateRatingKeywordChart(ratingKeywordChartData[i].data, i)
                }

                2 -> {
                    updateRatingKeywordChart(ratingKeywordChartData[i].data, i)
                }
            }
        }
    }*/

    fun createXYCData(username: String) {
        guiScope.launch {
            positiveKeywords.data.add(XYChart.Data<String, Number>(username, 0))
            neutralKeywords.data.add(XYChart.Data<String, Number>(username, 0))
            negativeKeywords.data.add(XYChart.Data<String, Number>(username, 0))
        }
    }
    fun sortRatingKeywordChart(ratingKeywordChartData: MutableList<Pair<String, List<XYChart.Series<String, Number>>>>) {
        for (inputData in ratingKeywordChartData) {
            val currentExistingUser = inputData.first
            println("Suchender Benutzer" + currentExistingUser)
            if (ratingKeywordUsers.contains(currentExistingUser).not()) {
                ratingKeywordUsers.addLast(currentExistingUser)
                val tempList: MutableList<String> = mutableListOf()
                for (user in ratingKeywordUsers) {
                    tempList.add(user)
                }
                createXYCData(currentExistingUser)

                guiScope.launch {
                    xAxis.categories.setAll(FXCollections.observableArrayList(tempList))
                }
            }
            if (ratingKeywordUsers.contains(inputData.first)) {
                println("Der Benuzter existiert: ${currentExistingUser}")
                val existingUserIndex = ratingKeywordUsers.indexOf(currentExistingUser)
                println("Mit Index: " + existingUserIndex)
                val newUserIndex = ratingKeywordChartData[0].first.indexOf(inputData.first)
                println("Der neue Benuzter: " + ratingKeywordChartData[0].first)
                println("Mit Index : " + newUserIndex)
                for (i in 0..2) {
                    when (i) {
                        0 -> {
                            var tempDataCounter = 0
                            for (value in inputData.second[i].data) {
                                println("VALUE0: " + value)
                                tempDataCounter += value.yValue.toInt()
                            }
                            for (user in positiveKeywords.data){
                                if(user.xValue == currentExistingUser) {
                                    guiScope.launch {
                                        user.yValue = tempDataCounter
                                        }
                                    }
                                }
                        }
                        1 -> {
                            var tempDataCounter = 0
                            for (value in inputData.second[i].data) {
                                println("VALUE1: " + value)
                                tempDataCounter += value.yValue.toInt()
                            }
                            for (user in neutralKeywords.data){
                                if(user.xValue == currentExistingUser) {
                                    guiScope.launch {
                                        user.yValue = tempDataCounter
                                    }
                                }
                            }
                        }
                        2 -> {
                            var tempDataCounter = 0
                            for (value in inputData.second[i].data) {
                                println("VALUE2: " + value)
                                tempDataCounter += value.yValue.toInt()
                            }
                            for (user in negativeKeywords.data){
                                if(user.xValue == currentExistingUser) {
                                    guiScope.launch {
                                        user.yValue = tempDataCounter
                                    }
                                }
                            }
                        }
                    }
                }
                /*
                for (i in 0..2) {
                    when (i) {
                        0 -> {
                                println("test")
                            }
                        }

                        1 -> {
                            var tempCounter = 0
                            for (value in ratingKeywords[ratingKeywordChartData.indexOfFirst { it.first == inputData.first }].data) {
                                println("1 " + value)
                            }
                        }

                        2 -> {
                            var tempCounter = 0
                            for (value in ratingKeywords[ratingKeywordChartData.indexOfFirst { it.first == inputData.first }].data) {
                                println("2 " + value)
                            }
                        }
                    }
                }*/
            }
        }
    }
    /*fun updateRatingKeywordChart(ratingKeywordChartData: List<XYChart.Series<String, Number>>) {
        for (i in 0..2) {
            when (i) {
                0 -> {
                    var newRating = true
                    for (currentValue in positiveKeywords.data) {
                        //println("CURRENTVALUE" + currentValue)
                        //println("NEWVALUE" + ratingKeywordChartData[i].data)
                        for (newValue in ratingKeywordChartData[i].data) {
                            println("NEWVALUE" + newValue)
                            println("CURRENTVALUE" + currentValue)
                            if (currentValue.xValue == newValue.toString()) {
                                println("currentDataValue: " + currentValue.yValue.toLong())
                                println("newDataValue: " + newValue.yValue.toLong())
                                if (currentValue.yValue.toLong() < newValue.yValue.toLong()) {
                                    currentValue.yValue == newValue.yValue
                                }
                                newRating = false
                                break
                            }
                        }

                    }
                    /*if (newRating) {
                        guiScope.launch {
                            positiveKeywords.data.add(XYChart.Data(element.xValue, element.yValue))
                        }
                    }*/
                    for (element in ratingKeywordChartData[i].data) {
                        //println("ELEMENT: " + element)
                        //println(element.xValue)
                        //println(element.yValue)
                        guiScope.launch {
                            positiveKeywords.data.add(XYChart.Data(element.xValue, element.yValue))
                        }
                    }
                }

                1 -> {
                    for (element in ratingKeywordChartData[i].data) {
                        guiScope.launch {
                            neutralKeywords.data.add(XYChart.Data(element.xValue, element.yValue))
                        }
                    }
                }

                2 -> {
                    for (element in ratingKeywordChartData[i].data) {
                        guiScope.launch {
                            negativeKeywords.data.add(XYChart.Data(element.xValue, element.yValue))
                        }
                    }
                }
            }
        }

    }*/

    fun updateFillKeywordChart(FillKeywordChartData: ObservableList<PieChart.Data>) {
        for (tempData in FillKeywordChartData) {
            var newData = true
            for (data in pieChartData) {
                if (data.name == tempData.name) {
                    guiScope.launch {
                        data.pieValue = tempData.pieValue
                    }
                    newData = false
                    break
                }
            }
            if (newData) {
                guiScope.launch {
                    pieChartData.add(tempData)
                }
            }
        }
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

    private fun applySettings() {
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
                        applyBTN.isDisable = true
                        updateCircle(true)
                    }
                })
                children.add(stopBTN.also {
                    it.setOnAction {
                        chatServer.stop()
                        stopBTN.isDisable = true
                        startBTN.isDisable = false
                        applyBTN.isDisable = false
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

                val ipLbl = Label("IP-Adresse").apply {
                    padding = Insets(4.0, 10.0, 0.0, 0.0)
                }

                val settingPortHb = HBox().apply {
                    with(children) {
                        add(portLbl)
                        add(portTF)
                    }
                }

                val settingIPHb = HBox().apply {
                    with(children) {
                        add(ipLbl)
                        add(ipTF)
                    }
                }

                val settingVb = VBox().apply {
                    with(children) {
                        add(settingPortHb)
                        add(settingIPHb)
                        add(applyBTN)
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
        val globalLeftVB = VBox().apply {
            guiScope.launch {
                children.add(fillKeywordPIC)
            }
        }
        val globalRightVB = VBox().apply {
            guiScope.launch {
                children.add(ratingKeywordBAC)
            }
        }
        val globalHB = HBox().apply {
            with(children) {
                add(globalLeftVB)
                add(globalRightVB)
            }
        }

        val chartsPAN = TitledPane("Statistiken", globalHB)
        val overviewPAN = TitledPane("Übersicht", (Label("Inhalt Übersicht")))

        val globaleStatsARC = Accordion().apply {
            with(panes) {
                add(chartsPAN)
                add(overviewPAN)
            }
        }

        val statsTabPane = TabPane().apply {
            with(tabs) {
                add(Tab("Globale Statistik").apply {
                    isClosable = false
                    content = globaleStatsARC
                })
                add(Tab("Detailierte Statistik").apply {
                    isClosable = false
                    content = Label("Detailierte Statistik")
                })
            }
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