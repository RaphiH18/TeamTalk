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
import java.time.Duration

class StatisticHandler(private val chatServer: ChatServer) {

    //  CoroutineScope für Netzwerk und IO-Operationen / für JavaFX-Thread
    private val handlerScope = CoroutineScope(Dispatchers.IO)

    val newMessages = ArrayDeque(listOf<Message>())
    val messages = mutableListOf<Message>()

    val globalCharts = mutableListOf<StatisticChart>()
    private val fillWordGlobalChart = FillWordChart(chatServer)
    private val triggerWordGlobalChart = TriggerWordChart(chatServer)

    val detailedCharts = mutableListOf<StatisticChart>()
    val summarizedFillWordsChart = SummarizedFillWordsChart(chatServer)

    var totalTextMessages = 0
    var totalFileMessages = 0
    var averageAnswerTime = Duration.ZERO
    var averageUsageTime = Duration.ZERO

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
        messages.add(message)

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
                totalTextMessages++
            }

            is FileMessage -> {
                totalFileMessages++
                /*
                TODO: Verarbeitung von FileMessages
                 */
            }
        }

        //Aktualisierung der durchschnittlichen globalen Nutzungszeit
        updateTotalAverageAnswerTime()

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

    fun updateTotalAverageAnswerTime() {
        var totalAnswerTime = Duration.ZERO

        for (user in chatServer.getUsers()) {
            totalAnswerTime = totalAnswerTime.plus(user.getStats().getTotalAnswerTime())
        }

        averageAnswerTime = totalAnswerTime.dividedBy(chatServer.getUsers().size.toLong())
    }

    fun updateTotalAverageUsageTime() {
        var totalUsageTime = Duration.ZERO

        for (user in chatServer.getUsers()) {
            totalUsageTime = totalUsageTime.plus(user.getStats().usageTime)
        }

        averageUsageTime = totalUsageTime.dividedBy(chatServer.getUsers().size.toLong())
    }

    fun loadData(user: ServerUser) {
        loadGlobalFillWordsData(user)
        loadGlobalTriggerWordsData(user)
        loadQuickStats(user)
    }

    private fun loadQuickStats(user: ServerUser) {
        this.totalTextMessages += user.getStats().sentTextMessages
        this.totalFileMessages += user.getStats().sentFileMessages
    }

    private fun loadGlobalFillWordsData(user: ServerUser) {
        val newData = mutableMapOf<String, Int>()

        for ((word, count) in user.getStats().fillWordChart.getData()) {
            val currentWordData = fillWordGlobalChart.getData()[word]

            if (currentWordData != null) {
                newData[word] = currentWordData + count
            } else {
                newData[word] = count
            }
        }

        fillWordGlobalChart.setData(newData)
    }

    private fun loadGlobalTriggerWordsData(user: ServerUser) {
        val newData = mutableListOf<Map<String, Int>>()

        for (map in user.getStats().triggerWordChart.getData()) {
            val newMap = mutableMapOf<String, Int>()

            for ((word, count) in map) {
                val currentWordData = map[word]

                if (currentWordData != null) {
                    newMap[word] = currentWordData + count
                } else {
                    newMap[word] = count
                }
            }

            newData.add(map)
        }

        triggerWordGlobalChart.setData(newData)
    }
}
