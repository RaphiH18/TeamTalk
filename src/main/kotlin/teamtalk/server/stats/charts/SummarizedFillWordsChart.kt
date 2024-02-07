package teamtalk.server.stats.charts

import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.stage.FileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.server.handler.ChatServer
import teamtalk.server.handler.ServerUser
import java.io.File
import javax.imageio.ImageIO

class SummarizedFillWordsChart(private val chatServer: ChatServer) : StatisticChart() {

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)

    private val xAxis = createXAxis()
    private val yAxis = createYAxis()

    private val fillWordsCount = mutableMapOf<ServerUser, MutableMap<String, Int>>()
    private var chart = create()

    override fun create(): StackedBarChart<String, Number> {
        createContextMenu()

        for (user in chatServer.getUsers()) {
            xAxis.categories.add(user.getName())
        }

        return StackedBarChart(xAxis, yAxis).apply {
            title = "Füllwörter totalisiert"
        }
    }

    override fun update() {
        guiScope.launch {
            for ((index, user) in chatServer.getUsers().withIndex()) {
                fillWordsCount[user] = user.getStats().fillWordChart.getFillWordsCount().toMutableMap()

                val name = "Benutzer ${index + 1}"
                if (!(xAxis.categories.contains(name))) {
                    xAxis.categories.add(name)
                }
            }

            val allWords = getAllFillWords()

            for (word in allWords) {
                var series = chart.data.find { it.name == word }

                if (series == null) {
                    series = XYChart.Series<String, Number>().apply { name = word }
                    chart.data.add(series)
                }

                // ANONYMES CHART
                for ((index, user) in chatServer.getUsers().withIndex()) {
                    val count = fillWordsCount[user]?.get(word) ?: 0
                    val userName = "Benutzer ${index + 1}"
                    var data = series.data.find { it.xValue == userName }

                    if (data != null) {
                        data.yValue = count
                    } else if (count > 0) { // Füge nur Datenpunkte hinzu, wenn count > 0 ist
                        series.data.add(XYChart.Data(userName, count))
                    }
                }

                /* PERSÖNLICH, NICHT ANONYM
                for ((user, wordUsageMap) in fillWordsCount) {
                    val count = wordUsageMap[word] ?: 0
                    var data = series.data.find { it.xValue == user.getName() }

                    if (data != null) {
                        data.yValue = count
                    } else {
                        data = XYChart.Data(user.getName(), count)
                        series.data.add(data)
                    }
                }
                */
            }
            println(xAxis.categories)
        }
    }

    fun copy(): StackedBarChart<String, Number> {
        val copiedXAxis = CategoryAxis()
        copiedXAxis.categories.addAll(xAxis.categories)
        copiedXAxis.label = xAxis.label

        val copiedYAxis = NumberAxis()
        copiedYAxis.label = yAxis.label

        val copiedChart = StackedBarChart(copiedXAxis, copiedYAxis)
        copiedChart.title = chart.title

        for (series in chart.data) {
            val copiedSeries = XYChart.Series<String, Number>()
            copiedSeries.name = series.name
            for (data in series.data) {
                val copiedData = XYChart.Data(data.xValue, data.yValue)
                copiedSeries.data.add(copiedData)
            }
            copiedChart.data.add(copiedSeries)
        }

        return copiedChart
    }

    fun save() {
        val fileChooser = FileChooser().apply {
            initialDirectory = File(System.getProperty("user.home"))
            title = "Speichern als"
            extensionFilters.add(FileChooser.ExtensionFilter("PNG-Dateien", "*.png"))
        }

        val file = fileChooser.showSaveDialog(chart.scene.window)

        if (file != null) {
            val copiedChart = copy()
            copiedChart.prefWidth = 800.0
            copiedChart.prefHeight = 600.0

            val dummyScene = Scene(Group(), 800.0, 600.0)
            (dummyScene.root as Group).getChildren().add(copiedChart)

            val image = dummyScene.snapshot(null)
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file)
        }
    }

    private fun createContextMenu() {
        guiScope.launch {
            val contextMenu = ContextMenu()
            val saveItem = MenuItem("Statistik als Bild speichern")
            val cancelItem = MenuItem("Abbrechen")

            contextMenu.items.addAll(saveItem, cancelItem)

            saveItem.setOnAction {
                save()
            }

            cancelItem.setOnAction {
                contextMenu.hide()
            }

            chart.setOnMouseClicked {
                if (it.button == MouseButton.SECONDARY) {
                    contextMenu.show(chart, it.screenX, it.screenY)
                } else if (contextMenu.isShowing) {
                    contextMenu.hide()
                }
            }
        }
    }

    private fun getAllFillWords(): List<String> {
        val allWords = mutableListOf<String>()
        for (wordUsageMap in fillWordsCount.values) {
            for (word in wordUsageMap.keys) {
                allWords.add(word)
            }
        }

        return allWords
    }

    override fun getChart() = chart

    private fun createXAxis(): CategoryAxis {
        return CategoryAxis().apply {
            categories = FXCollections.observableArrayList()
            label = "Benutzer (anonymisiert)"
        }
    }

    private fun createYAxis(): NumberAxis {
        return NumberAxis().apply {
            label = "Verwendete Füllwörter"
        }
    }
}