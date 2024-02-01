package teamtalk.server.stats.charts

import javafx.collections.FXCollections
import javafx.scene.chart.PieChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch

class TriggerWordChart(private val titleSuffix: String = " ") : StatisticChart() {

    enum class WordCategory {
        POSITIVE, NEUTRAL, NEGATIVE, NONE;

        override fun toString(): String {
            return when(this) {
                POSITIVE    -> "Positiv"
                NEUTRAL     -> "Neutral"
                NEGATIVE    -> "Negativ"
                NONE        -> "Keine Kategorie"
            }
        }
    }

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)

    private val triggerWordsCount = loadTriggerWords()

    private val triggerWordsChartData = FXCollections.observableArrayList<PieChart.Data>()
    private val triggerWordsChart = create()

    /*
    Chart-Logik
     */
    override fun create(): PieChart {
        triggerWordsChartData.add(PieChart.Data("Keine Daten", 1.0))

        return PieChart(triggerWordsChartData).apply {
            labelsVisible = true
            title = "Triggerwörter $titleSuffix"
        }
    }

    override fun update() {
        guiScope.launch {
            triggerWordsChartData.clear()
            triggerWordsChartData.add(PieChart.Data(WordCategory.NEGATIVE.toString(), triggerWordsCount[0].values.sum().toDouble()))
            triggerWordsChartData.add(PieChart.Data(WordCategory.POSITIVE.toString(), triggerWordsCount[0].values.sum().toDouble()))
            triggerWordsChartData.add(PieChart.Data(WordCategory.NEUTRAL.toString(), triggerWordsCount[0].values.sum().toDouble()))
        }
    }

    override fun getChart() = triggerWordsChart

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

        return WordCategory.NONE
    }

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
