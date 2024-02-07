package teamtalk.server.stats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import teamtalk.server.handler.ChatServer
import teamtalk.server.stats.charts.FillWordChart
import teamtalk.server.stats.charts.StatisticChart
import teamtalk.server.stats.charts.SummarizedFillWordsChart
import teamtalk.server.stats.charts.TriggerWordChart
import java.time.Duration

class StatisticHandler(private val chatServer: ChatServer) {

    //  CoroutineScope für Netzwerk und IO-Operationen / für JavaFX-Thread
    private val handlerScope = CoroutineScope(Dispatchers.IO)

    val newMessages = ArrayDeque(listOf<Message>())
    val processedMessages = mutableListOf<Message>()

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
        //Verarbeitung von userbezogenen Statistiken
        val senderUser = chatServer.getUser(message.getSenderName())
        val receiverUser = chatServer.getUser(message.getReceiverName())

        receiverUser?.getStats()?.processMessage(message)
        senderUser?.getStats()?.processMessage(message)

        //Verarbeitung von globalen Statistiken
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

        //Wortanalyse beendet, Wort wird als "verarbeitet" markiert.
        processedMessages.add(message)

        //Verarbeitung der Antwortzeit
        if (receiverUser != null) {
            senderUser?.getStats()?.processAnswerTime(receiverUser)
        }

        //Anzeige aller gewonnenen Statistiken im Statistiken > Übersicht-Bereich des Server-GUI.
        chatServer.getGUI().updateQuickStats()
    }

    private fun formatMessage(message: String): List<String> {
        val noWords = Regex("\\p{Punct}")
        val spaces = Regex("\\s+")

        val wordsOnly = message.replace(noWords, "")

        val words = wordsOnly.split(spaces).filter { it.isNotBlank() }
        return words
    }

    /*
    Methode für das globale FillWordChart
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

    fun getTotalMessages() = processedMessages.size

    fun getTotalTextMessages() = processedMessages.count() { it is TextMessage }

    fun getTotalFileMessages() = processedMessages.count() { it is FileMessage }

    fun getTotalAvgAnswerTime(): Duration {
        var totalAvgAnswerTime: Duration = Duration.ZERO
        var messageCount = 0

        val users = chatServer.getUsers()

        for (index1 in users.indices) {
            for (index2 in index1 + 1 until users.size) {
                val user1 = users[index1]
                val user2 = users[index2]

                val avgTimeUser1ToUser2 = user1.getStats().getAverageAnswerTime(user2)
                if (avgTimeUser1ToUser2 > Duration.ZERO) {
                    totalAvgAnswerTime += avgTimeUser1ToUser2
                    messageCount++
                }

                val avgTimeUser2ToUser1 = user2.getStats().getAverageAnswerTime(user1)
                if (avgTimeUser2ToUser1 > Duration.ZERO) {
                    totalAvgAnswerTime += avgTimeUser2ToUser1
                    messageCount++
                }
            }
        }

        return if (messageCount > 0) {
            totalAvgAnswerTime.dividedBy(messageCount.toLong())
        } else {
            Duration.ZERO
        }
    }
}
