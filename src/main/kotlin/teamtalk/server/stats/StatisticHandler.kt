package teamtalk.server.stats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import teamtalk.server.handler.ChatServer
import teamtalk.server.handler.ServerUser
import teamtalk.server.stats.charts.FillWordChart
import teamtalk.server.stats.charts.StatisticChart
import teamtalk.server.stats.charts.SummarizedFillWordsChart
import teamtalk.server.stats.charts.TriggerWordChart
import kotlin.collections.ArrayDeque

class StatisticHandler(private val chatServer: ChatServer) {

    //  CoroutineScope für Netzwerk und IO-Operationen / für JavaFX-Thread
    private val handlerScope = CoroutineScope(Dispatchers.IO)

    val newMessages = ArrayDeque(listOf<Message>())
    private val processedMessages = mutableListOf<Message>()

    val globalCharts = mutableListOf<StatisticChart>()
    private val fillWordGlobalChart = FillWordChart()
    private val triggerWordGlobalChart = TriggerWordChart()

    val detailedCharts = mutableListOf<StatisticChart>()
    private val summarizedFillWordsChart = SummarizedFillWordsChart(chatServer)

    init {
        globalCharts.add(fillWordGlobalChart)
        globalCharts.add(triggerWordGlobalChart)
        detailedCharts.add(summarizedFillWordsChart)
    }

    fun start() {
        handlerScope.launch {
            while (true) {
                while (newMessages.isNotEmpty()) {
                    val message = newMessages.removeFirst()

                    process(message)
                }

                delay(5000)
            }
        }
    }

    private fun process(message: Message) {
        val senderName = message.getSenderName()
        val senderUser = chatServer.getUser(senderName)

        val receiverName = message.getReceiverName()
        val receiverUser = chatServer.getUser(receiverName)

        if (receiverUser != null) {
            receiverUser.getStats().processMessage(message)
        }

        if (senderUser != null) {
            senderUser.getStats().processMessage(message)
        }

        when(message) {
            is TextMessage -> {
                processGlobalFillWords(message.getMessage())
                processGlobalTriggerWords(message.getMessage())
                summarizedFillWordsChart.update()
            }

            is FileMessage -> {
                /*
                TODO: Verarbeitung von FileMessages
                 */
            }
        }

        processedMessages.add(message)
    }

    private fun formatMessage(message: String): List<String> {
        val noWords = Regex("\\p{Punct}")
        val spaces = Regex("\\s+")

        val wordsOnly = message.replace(noWords, "")

        val words = wordsOnly.split(spaces).filter { it.isNotBlank() }
        return words
    }

    fun getTotalMessages() = processedMessages.size

    fun getTotalTextMessages() = processedMessages.count() { it is TextMessage }

    fun getTotalFileMessages() = processedMessages.count() { it is FileMessage }

    /*
    Methoden für das globale FillWordChart
     */
    private fun processGlobalFillWords(message: String) {
        val words = formatMessage(message)
        val containsFillWord = words.any() { fillWordGlobalChart.isFillWord(it) }

        if (containsFillWord) {
            for (word in words) {
                fillWordGlobalChart.countIfFillWord(word)
            }

            fillWordGlobalChart.update()
        }
    }

    /*
    Methode für das globale TriggerWordChart
     */
    private fun processGlobalTriggerWords(message: String) {
        val words = formatMessage(message)
        val containsTriggerWord = words.any() { triggerWordGlobalChart.isTriggerWord(it) }

        if (containsTriggerWord) {
            for (word in words) {
                triggerWordGlobalChart.countIfTriggerWord(word)
            }

            triggerWordGlobalChart.update()
        }
    }
}
