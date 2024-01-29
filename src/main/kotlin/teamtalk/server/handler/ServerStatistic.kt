package teamtalk.server.handler

import javafx.collections.FXCollections
import javafx.scene.chart.PieChart
import javafx.scene.chart.XYChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import java.time.Duration
import kotlin.collections.ArrayDeque

class ServerStatistic(private val server: ChatServer) {

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    /* Übersicht Diagramm-Arten

    Fullwörter:             Balkendiagramm / Kuchendiagramm
    Triggerwörter:          Kuchendiagramm
    Antwort- und Nutzungs-Zeiten:          Liniendiagramm

    Nutzungszeiten:         in Textform
    Filetransfer:           in Textform

     */

    val newMessages = ArrayDeque(listOf<Message>())
    val processedMessages = mutableListOf<Message>()
    var fillkeywordChartData = FXCollections.observableArrayList<PieChart.Data>()
    private val FILL_KEYWORDS = mutableMapOf<String, Double>(
        "der" to 0.0,
        "die" to 0.0,
        "das" to 0.0,
        "und" to 0.0,
        "oder" to 0.0,
        "also" to 0.0,
        "quasi" to 0.0,
        "sozusagen" to 0.0,
        "eigentlich" to 0.0
    )

    fun RatingKeywordGenerator(): List<MutableMap<String, Number>> {
        val keywords = listOf(
            mutableMapOf<String, Number>(
                "gut" to 0,
                "ja" to 0,
                "super" to 0,
                "perfekt" to 0,
                "optimal" to 0,
                "prima" to 0
            ),
            mutableMapOf<String, Number>(
                "ok" to 0,
                "einverstanden" to 0,
                "passt" to 0
            ),
            mutableMapOf<String, Number>(
                "schlecht" to 0,
                "nein" to 0,
                "schade" to 0
            )
        )
        return keywords
    }

    private var RATING_KEYWORDS = mutableListOf<Pair<String, List<MutableMap<String, Number>>>>()

    fun ratingKeywordChartDataGenerator(name: String): Pair<String, List<XYChart.Series<String, Number>>> {
        val newDataList = Pair<String, List<XYChart.Series<String, Number>>>(
            name,
            listOf(
                XYChart.Series<String, Number>(),
                XYChart.Series<String, Number>(),
                XYChart.Series<String, Number>()
            )
        )
        return newDataList
    }

    private var ratingKeywordChartData = mutableListOf<Pair<String, List<XYChart.Series<String, Number>>>>()

    fun start() {

        var message: Message
        var formattedMessage: String
        var senderName: String
        handlerScope.launch {
            while (true) {
                while (newMessages.isNotEmpty()) {
                    message = newMessages.removeFirst()
                    formattedMessage = message.getMessage().toString().lowercase()
                    senderName = message.getSenderName()
                    getFillWordUsage(formattedMessage)
                    getTriggerWordUsage(formattedMessage, senderName)
                    // Hier kommen alle Analysemethoden hin...

                    //
                    processedMessages.add(message)
                }
                server.getGUI().updateCharts(fillkeywordChartData ,ratingKeywordChartData)
                //server.getGUI().updateCharts(fillkeywordChartData, ratingKeywordChartData)
                delay(5000)
            }
        }
    }

    fun getTotalMessages() = processedMessages.size

    fun getTotalTextMessages() =
        processedMessages.count() { it is TextMessage }

    fun getTotalFileMessages() =
        processedMessages.count() { it is FileMessage }

    fun getTotalPositivity(): Double {
        return 0.0
    }

    fun getTotalNeutrality(): Double {
        return 0.0
    }

    fun getTotalNegativity(): Double {
        return 0.0
    }

    fun getTextMessages(serverClient: ServerClient) =
        processedMessages.count() { it.getSenderName() == serverClient.getUsername() && it is TextMessage }

    fun getFileMessages(serverClient: ServerClient) =
        processedMessages.count() { it.getSenderName() == serverClient.getUsername() && it is FileMessage }

    fun getAverageAnswerTime(serverClient: ServerClient): Duration {
        var averageAnsTime: Duration = Duration.ZERO
        var messageCount = 0

        val receivedMessages = processedMessages.filter { it.getReceiverName() == serverClient.getUsername() }
        val sentMessages = processedMessages.filter { it.getSenderName() == serverClient.getUsername() }

        for (received in receivedMessages) {
            val reply = sentMessages.firstOrNull { it.getTimestamp().isAfter(received.getTimestamp()) }
            if (reply != null) {
                averageAnsTime += Duration.between(received.getTimestamp(), reply.getTimestamp())
                messageCount++
            }
        }

        return if (messageCount > 0) {
            averageAnsTime.dividedBy(messageCount.toLong())
        } else {
            Duration.ZERO
        }
    }

    fun getTotalContactAddressing(contact: String): Int {
        val addressingTrigger = "@"
        val triggerFilter = Regex("$addressingTrigger\\w+\\s\\w+")
        var addressingAmount = 0

        for (message in processedMessages) {
            println("MESSAGE: " + message.getMessage())
            triggerFilter.findAll(message.getMessage().toString()).forEach { match ->
                val word = match.value.replace("@", "")
                println("CHECK: \n$word\n$contact")
                if (word == contact) {
                    addressingAmount++
                }
            }

        }
        return addressingAmount
    }

    fun getFillWordUsage(message: String) {
        val trimmer = Regex("\\s+")
        val words = message.split(trimmer)
        for (word in words) {
            if (FILL_KEYWORDS.contains(word)) {
                FILL_KEYWORDS[word] = (FILL_KEYWORDS[word]!!) + 1
            }
        }
        fillkeywordChartData.clear()
        for (row in FILL_KEYWORDS) {
            if (row.value > 0.0) {
                println("row: $row")
                var pieChartDataFormatter = PieChart.Data("", 0.0)
                pieChartDataFormatter.name = row.key
                pieChartDataFormatter.pieValue = row.value
                fillkeywordChartData.add(pieChartDataFormatter)
            }
        }
    }

    fun getTriggerWordUsage(message: String, sender: String) {
        val trimmer = Regex("\\s+")
        val words = message.split(trimmer)

        if (RATING_KEYWORDS.any { it.first == sender }.not()) {
            RATING_KEYWORDS.add(Pair(sender, RatingKeywordGenerator()))
            ratingKeywordChartData.add(ratingKeywordChartDataGenerator(sender))
        }
        if (RATING_KEYWORDS.any { it.first == sender }) {
            val senderIndex = RATING_KEYWORDS.indexOfFirst { it.first == sender }
            for (word in words) {
                for (i in 0..2) {
                    if (RATING_KEYWORDS[senderIndex].second[i].contains(word)) {
                        RATING_KEYWORDS[senderIndex].second[i][word] =
                            (RATING_KEYWORDS[senderIndex].second[i][word]!!).toLong() + 1
                        //println("Neuer Wert: " + RATING_KEYWORDS[senderIndex])
                        break
                    }
                }
            }
            val senderIndexChartData = ratingKeywordChartData.indexOfFirst { it.first == sender }
            for (i in 0..2) {
                ratingKeywordChartData[senderIndexChartData].second[i].data.clear()
            }
            for (i in 0..2) {
                for (senderData in RATING_KEYWORDS[senderIndex].second[i]) {
                    if (senderData.value.toLong() > 0) {
                        val dataFormatter = XYChart.Data(senderData.key, senderData.value)
                        ratingKeywordChartData[senderIndexChartData].second[i].data.add(dataFormatter)
                    }
                }
            }
        }
    }
}
