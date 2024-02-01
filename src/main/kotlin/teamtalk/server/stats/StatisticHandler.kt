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
import teamtalk.server.stats.charts.TriggerWordChart
import kotlin.collections.ArrayDeque

class StatisticHandler(private val chatServer: ChatServer) {

    //  CoroutineScope für Netzwerk und IO-Operationen / für JavaFX-Thread
    private val handlerScope = CoroutineScope(Dispatchers.IO)

    val newMessages = ArrayDeque(listOf<Message>())
    val processedMessages = mutableListOf<Message>()

    val globalCharts = mutableListOf<StatisticChart>()
    private val fillWordGlobalChart = FillWordChart()
    private val triggerWordGlobalChart = TriggerWordChart()

    private lateinit var userStatsSelected: ServerUser

    init {
        globalCharts.add(fillWordGlobalChart)
        globalCharts.add(triggerWordGlobalChart)
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
        when(message) {
            is TextMessage -> {
                processGlobalFillWords(message.getMessage())
                processGlobalTriggerWords(message.getMessage())

                val senderName = message.getSenderName()
                val senderUser = chatServer.getUser(senderName)
                val receiverName = message.getSenderName()
                val receiverUser = chatServer.getUser(receiverName)

                if (senderUser != null) {
                    senderUser.getStats().sentMessages += 1
                    senderUser.getStats().processMessage(message.getMessage())
                }

                if (receiverUser != null) {
                    receiverUser.getStats().receivedMessages += 1
                }
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

    private fun processGlobalFillWords(message: String) {
        val words = formatMessage(message)
        val containsFillWord = words.any() { fillWordGlobalChart.fillWordsCount.containsKey(it) }

        if (containsFillWord) {
            for (word in words) {
                fillWordGlobalChart.countIfFillWord(word)
            }

            fillWordGlobalChart.update()
        }
    }

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

//    private fun updateUserFillWords(serverClient: ServerClient, message: String) {
//        val trimmer = Regex("\\s+")
//        val words = message.split(trimmer)
//        val fillWordChart = getOrCreateFillWordChart(serverClient)
//        val containsFillWord = words.any() { fillWordChart.isFillWord(it) }
//
//        if (containsFillWord) {
//            for (word in words) {
//                fillWordChart.countIfFillWord(word)
//            }
//
//            fillWordChart.update()
//        }
//    }
//
//    fun getUserCharts(serverClient: ServerClient): List<StatisticChart> {
//        val fillWordChart = getOrCreateFillWordChart(serverClient)
//        val triggerWordChart = getOrCreateTriggerWordChart(serverClient)
//
//        return listOf(fillWordChart, triggerWordChart)
//    }
//
//    fun setSelected(serverClient: ServerClient) {
//        userStatsSelected = serverClient
//    }
//
//    private fun getOrCreateFillWordChart(serverClient: ServerClient): FillWordChart {
//        val charts = userCharts.getOrPut(serverClient) { mutableListOf() }
//        val fillWordChart = charts.filterIsInstance<FillWordChart>().firstOrNull()
//
//        if (fillWordChart != null) {
//            return fillWordChart
//        } else {
//            val newFillWordChart = FillWordChart(serverClient.getUsername())
//            charts.add(newFillWordChart)
//
//            return newFillWordChart
//        }
//    }
//
//    private fun getOrCreateTriggerWordChart(serverClient: ServerClient): TriggerWordChart {
//        val charts = userCharts.getOrPut(serverClient) { mutableListOf() }
//        val triggerWordChart = charts.filterIsInstance<TriggerWordChart>().firstOrNull()
//
//        if (triggerWordChart != null) {
//            return triggerWordChart
//        } else {
//            val newTriggerWordChart = TriggerWordChart(serverClient.getUsername())
//            charts.add(newTriggerWordChart)
//
//            return newTriggerWordChart
//        }
//    }
}
