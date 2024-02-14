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

class TriggerWordChart(private val chatServer: ChatServer, private val user: ServerUser? = null) : StatisticChart() {

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

    private var triggerWordsCount: List<MutableMap<String, Int>> = listOf(mutableMapOf(), mutableMapOf(), mutableMapOf())

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
        if (isTriggerWord(word)) {
            val category = getCategory(word)
            when (category) {
                WordCategory.POSITIVE -> {
                    val currentCount = triggerWordsCount[0][word] ?: 0
                    triggerWordsCount[0][word] = currentCount + 1
                }
                WordCategory.NEUTRAL -> {
                    val currentCount = triggerWordsCount[1][word] ?: 0
                    triggerWordsCount[1][word] = currentCount + 1
                }
                WordCategory.NEGATIVE -> {
                    val currentCount = triggerWordsCount[2][word] ?: 0
                    triggerWordsCount[2][word] = currentCount + 1
                }
            }
        }
    }

    fun isTriggerWord(word: String)= chatServer.getConfig().triggerWordsList.any { it.contains(word) }

    private fun getCategory(word: String): WordCategory {
        if (chatServer.getConfig().positiveTriggerWords.contains(word)) {
            return WordCategory.POSITIVE
        } else if (chatServer.getConfig().neutraleTriggerWords.contains(word)) {
            return WordCategory.NEUTRAL
        } else if (chatServer.getConfig().negativeTriggerWords.contains(word)) {
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

    fun setData(data: List<Map<String, Int>>) {
        val updatedStats = mutableListOf<MutableMap<String, Int>>(mutableMapOf(), mutableMapOf(), mutableMapOf())

        // Füge das Wort mit seinem Count hinzu, wenn es in der aktuellen Liste enthalten ist
        for (map in data) {
            for ((word, count) in map) {
                if (chatServer.getConfig().positiveTriggerWords.contains(word)) {
                    updatedStats[0][word] = count
                } else if (chatServer.getConfig().neutraleTriggerWords.contains(word)) {
                    updatedStats[1][word] = count
                } else if (chatServer.getConfig().negativeTriggerWords.contains(word)) {
                    updatedStats[2][word] = count
                }
            }
        }

        // Füge das Wort mit dem Count 0 hinzu, wenn es nicht in den aktualisierten Statistiken vorhanden ist
        for ((listIndex, list) in chatServer.getConfig().triggerWordsList.withIndex()) {
            for (word in list) {
                if (updatedStats.any { it.containsKey(word) }.not()) {
                    updatedStats[listIndex][word] = 0
                }
            }
        }

        triggerWordsCount = updatedStats
        update()
    }

//    fun setData(data: List<Map<String, Int>>) {
//        val updatedListOfMaps = mutableListOf<MutableMap<String, Int>>()
//
//        println(data)
//
//        for (map in data) {
//            val updatedMap = mutableMapOf<String, Int>()
//
//            for ((word, count) in map) {
//                if (isTriggerWord(word)) {
//                    updatedMap[word] = count
//                }
//            }
//
////            for (word in chatServer.getConfig().positiveTriggerWords) {
////                updatedMap.putIfAbsent(word, 0)
////            }
//
//            updatedListOfMaps.add(updatedMap)
//        }
//
//        this.triggerWordsCount = updatedListOfMaps.toList()
//        update()
//    }
}
