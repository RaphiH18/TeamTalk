package teamtalk.server.stats.charts

import javafx.collections.FXCollections
import javafx.scene.chart.PieChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch

class FillWordChart(private val titleSuffix: String = " ") : StatisticChart() {

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)

    val fillWordsCount = loadFillWords()

    private val fillWordsChartData = FXCollections.observableArrayList<PieChart.Data>()
    private val fillWordsChart = create()

    /*
    Chart-Logik
     */
    override fun create(): PieChart {
        fillWordsChartData.add(PieChart.Data("Keine Daten", 1.0))

        return PieChart(fillWordsChartData).apply {
            labelsVisible = true
            title = "Fullwörter $titleSuffix"
        }
    }

    override fun update() {
        guiScope.launch {
            fillWordsChartData.clear()
            for ((word, count) in fillWordsCount) {
                if (count > 0) {
                    fillWordsChartData.add(PieChart.Data(word, count.toDouble()))
                }
            }
            println(fillWordsCount)
        }
    }

    override fun getChart() = fillWordsChart

    /*
    Interne Logik
     */
    fun countIfFillWord(fillWord: String): Boolean {
        if (isFillWord(fillWord)) {
            val currentCount = fillWordsCount[fillWord] ?: 0
            fillWordsCount[fillWord] = currentCount + 1
            return true
        }
        return false
    }

    fun isFillWord(word: String) = fillWordsCount.containsKey(word)

    private fun loadFillWords() : MutableMap<String, Int> {
        return mutableMapOf(
            "der" to 0,
            "die" to 0,
            "das" to 0,
            "und" to 0,
            "oder" to 0,
            "also" to 0,
            "quasi" to 0,
            "sozusagen" to 0,
            "eigentlich" to 0
        )
    }
}