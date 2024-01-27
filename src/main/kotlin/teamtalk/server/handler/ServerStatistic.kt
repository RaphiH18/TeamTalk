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

    private var ratingKeywordChartData = listOf(
        XYChart.Series<String, Number>(),
        XYChart.Series<String, Number>(),
        XYChart.Series<String, Number>()
    )

    private val RATING_KEYWORDS = listOf(
        mutableMapOf<String, Double>(
            "gut" to 0.0,
            "ja" to 0.0,
            "super" to 0.0,
            "perfekt" to 0.0,
            "optimal" to 0.0,
            "prima" to 0.0
        ),
        mutableMapOf<String, Double>(
            "schlecht" to 0.0,
            "nein" to 0.0,
            "schade" to 0.0
        ),
        mutableMapOf<String, Double>(
            "ok" to 0.0,
            "einverstanden" to 0.0,
            "passt" to 0.0
        )
    )

    fun start() {
        var message: Message
        var formattedMessage: String
        handlerScope.launch {
            while (true) {
                while (newMessages.isNotEmpty()) {
                    message = newMessages.removeFirst()
                    formattedMessage = message.getMessage().toString().lowercase()
                    getFillWordUsage(formattedMessage)
                    getTriggerWordUsage(formattedMessage)
                    // Hier kommen alle Analysemethoden hin...

                    //
                    processedMessages.add(message)
                }
                server.getGUI().updateCharts(fillkeywordChartData, ratingKeywordChartData)
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
        //fun getTotalPerContactAddressing(contact: Contact): Int { // Implemntation via Kontaktobjekt -> Eventuell via ID?
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

    fun getTriggerWordUsage(message: String) {
        val trimmer = Regex("\\s+")
        val words = message.split(trimmer)
        for (word in words) {
            for (i in 0..2) {
                println("i: " + i)
                if (RATING_KEYWORDS[i].contains(word)) {
                    RATING_KEYWORDS[i][word] = (RATING_KEYWORDS[i][word]!!) + 1
                    break
                }
            }
        }
        /*for (i in 0..2) {
            for (data in RATING_KEYWORDS[i]) {
                ratingKeywordChartData[i].
            }
        }
        ratingKeywordChartData[0]
        */
    }
}
