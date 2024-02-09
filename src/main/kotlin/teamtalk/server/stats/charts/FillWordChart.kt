package teamtalk.server.stats.charts

import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.chart.PieChart
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

class FillWordChart(private val chatServer: ChatServer, private val user: ServerUser? = null) : StatisticChart() {

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)

    private var fillWordsCount: MutableMap<String, Int> = mutableMapOf()

    private val fillWordsChartData = FXCollections.observableArrayList<PieChart.Data>()
    private val fillWordsChart = create()

    private var firstUpdate = true

    /*
    Chart-Logik
     */
    override fun create(): PieChart {
        fillWordsChartData.add(PieChart.Data("Keine Daten", 1.0))
        createContextMenu()

        return PieChart(fillWordsChartData).apply {
            labelsVisible = true

            if (user != null) {
                title = "Fullwörter Benutzer ${user.getIndex() + 1}"
            } else {
                title = "Füllwörter"
            }
        }
    }

    override fun update() {
        guiScope.launch {
            val currentDataMap = fillWordsChartData.associateBy { it.name }

            for ((word, count) in fillWordsCount) {
                if (count > 0) {
                    if (firstUpdate) {
                        fillWordsChartData.clear()
                        firstUpdate = false
                    }

                    val existingData = currentDataMap[word]

                    if (existingData != null) {
                        existingData.pieValue = count.toDouble()
                    } else {
                        fillWordsChartData.add(PieChart.Data(word, count.toDouble()))
                    }
                }
            }

            if (user != null) {
                fillWordsChart.title = "Fullwörter Benutzer ${user.getIndex() + 1}"
            }
        }
    }

    private fun copy(): PieChart {
        val copiedChartData = FXCollections.observableArrayList<PieChart.Data>()
        for (data in fillWordsChartData) {
            copiedChartData.add(PieChart.Data(data.name, data.pieValue))
        }

        val copiedChart = PieChart(copiedChartData).apply {
            labelsVisible = fillWordsChart.labelsVisible
            title = fillWordsChart.title
        }

        return copiedChart
    }


    private fun save() {
        val fileChooser = FileChooser().apply {
            initialDirectory = File(System.getProperty("user.home"))
            title = "Speichern als"
            extensionFilters.add(FileChooser.ExtensionFilter("PNG-Dateien", "*.png"))
        }

        val file = fileChooser.showSaveDialog(fillWordsChart.scene.window)

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

    override fun getChart() = fillWordsChart

    /*
    Interne Logik
     */
    fun getData() = fillWordsCount

    fun setData(data: Map<String, Int>) {
        val newData = data.toMutableMap()
        val keysToRemove = mutableListOf<String>()

        for ((word, count) in newData) {
            if (chatServer.getConfig().fillWordsList.contains(word)) {
                fillWordsCount[word] = count
            } else {
                keysToRemove.add(word)
            }
        }
        this.fillWordsCount = newData

        for (word in chatServer.getConfig().fillWordsList) {
            if (fillWordsCount.containsKey(word).not()) {
                fillWordsCount[word] = 0
            }
        }

        keysToRemove.forEach { newData.remove(it) }

        update()
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

            fillWordsChart.setOnMouseClicked {
                if (it.button == MouseButton.SECONDARY) {
                    contextMenu.show(fillWordsChart, it.screenX, it.screenY)
                } else if (contextMenu.isShowing) {
                    contextMenu.hide()
                }
            }
        }
    }

    fun countIfFillWord(fillWord: String): Boolean {
        if (isFillWord(fillWord)) {
            val currentCount = fillWordsCount[fillWord] ?: 0
            fillWordsCount[fillWord] = currentCount + 1

            return true
        }
        return false
    }

    fun isFillWord(word: String) = fillWordsCount.containsKey(word)
}