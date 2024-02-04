package teamtalk.server.stats.charts

import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.chart.PieChart
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseButton
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.server.handler.ServerUser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class TriggerWordChart(private val user: ServerUser? = null) : StatisticChart() {

    enum class WordCategory {
        POSITIVE, NEUTRAL, NEGATIVE;

        override fun toString(): String {
            return when(this) {
                POSITIVE    -> "Positiv"
                NEUTRAL     -> "Neutral"
                NEGATIVE    -> "Negativ"
            }
        }
    }

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)

    private val triggerWordsCount = loadTriggerWords()

    private val triggerWordsChartData = FXCollections.observableArrayList<PieChart.Data>()
    private val triggerWordsChart = create()

    private var firstUpdate = true

    /*
    Chart-Logik
     */
    override fun create(): PieChart {
        triggerWordsChartData.add(PieChart.Data("Keine Daten", 1.0))
        createContextMenu()

        return PieChart(triggerWordsChartData).apply {
            labelsVisible = true

            if (user == null) {
                title = "Triggerwörter"
            } else {
                title = "Triggerwörter Benutzer ${user.getIndex() + 1}"
            }
        }
    }

    override fun update() {
        guiScope.launch {
            val categoryCounts = getCategoryCounts()

            val atLeastOneWordUsed = categoryCounts.any { it.value > 0 }

            if (firstUpdate && atLeastOneWordUsed) {
                triggerWordsChart.data.clear()
                firstUpdate = false
            }

            //Aktualisiert die Chart-Daten
            for (category in WordCategory.values()) {
                var found = false

                for (data in triggerWordsChartData) {
                    if (data.name == category.toString()) {
                        data.pieValue = categoryCounts.getValue(category).toDouble()
                        found = true
                        break
                    }
                }

                if ((found.not()) and (categoryCounts.getValue(category) > 0)) {
                    triggerWordsChartData.add(PieChart.Data(category.toString(), categoryCounts.getValue(category).toDouble()))
                }
            }

            if (user != null) {
                triggerWordsChart.title = "Triggerwörter Benutzer ${user.getIndex() + 1}"
            }
        }
    }

    fun copy(): PieChart {
        val copiedChartData = FXCollections.observableArrayList<PieChart.Data>()
        for (data in triggerWordsChartData) {
            copiedChartData.add(PieChart.Data(data.name, data.pieValue))
        }

        val copiedChart = PieChart(copiedChartData).apply {
            labelsVisible = triggerWordsChart.labelsVisible
            title = triggerWordsChart.title
        }

        return copiedChart
    }

    override fun getChart() = triggerWordsChart

    fun save() {
        val fileChooser = FileChooser().apply {
            initialDirectory = File(System.getProperty("user.home"))
            title = "Speichern als"
            extensionFilters.add(FileChooser.ExtensionFilter("PNG-Dateien", "*.png"))
        }

        val file = fileChooser.showSaveDialog(triggerWordsChart.scene.window)

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

    /*
    Interne Logik
     */
    fun countIfTriggerWord(word: String) {
        for (triggerWordMap in triggerWordsCount) {
            if (triggerWordMap.containsKey(word)) {
                val currentCount = triggerWordMap[word] ?: 0
                triggerWordMap[word] = currentCount + 1
                break
            }
        }
    }

    fun isTriggerWord(word: String): Boolean {
        for (triggerWordMap in triggerWordsCount) {
            if (triggerWordMap.containsKey(word)) {
                return true
            }
        }
        return false
    }

    private fun getCategory(word: String): WordCategory {
        if (triggerWordsCount[0].containsKey(word)) {
            return WordCategory.POSITIVE
        } else if (triggerWordsCount[1].containsKey(word)) {
            return WordCategory.NEUTRAL
        } else if (triggerWordsCount[2].containsKey(word)) {
            return WordCategory.NEGATIVE
        }

        return WordCategory.NEUTRAL
    }

    private fun getCategoryCounts(): Map<WordCategory, Int> {
        val categoryCounts = mutableMapOf(
            WordCategory.POSITIVE to 0,
            WordCategory.NEUTRAL to 0,
            WordCategory.NEGATIVE to 0
        )

        for (index in triggerWordsCount.indices) {
            val category = when (index) {
                0 -> WordCategory.POSITIVE
                1 -> WordCategory.NEUTRAL
                2 -> WordCategory.NEGATIVE
                else -> continue
            }

            for (entry in triggerWordsCount[index].entries) {
                val sum = entry.value
                categoryCounts[category] = categoryCounts.getValue(category) + sum
            }
        }

        return categoryCounts
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

            triggerWordsChart.setOnMouseClicked {
                if (it.button == MouseButton.SECONDARY) {
                    contextMenu.show(triggerWordsChart, it.screenX, it.screenY)
                } else if (contextMenu.isShowing) {
                    contextMenu.hide()
                }
            }
        }
    }

    fun getData() = triggerWordsCount.toList()

    private fun loadTriggerWords() : List<MutableMap<String, Int>> {
        return listOf(
            mutableMapOf(
                "gut" to 0,
                "ja" to 0,
                "super" to 0,
                "perfekt" to 0,
                "optimal" to 0,
                "prima" to 0
            ),
            mutableMapOf(
                "ok" to 0,
                "einverstanden" to 0,
                "passt" to 0
            ),
            mutableMapOf(
                "schlecht" to 0,
                "nein" to 0,
                "schade" to 0
            )
        )
    }
}
