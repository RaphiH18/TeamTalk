package teamtalk.server.stats

import teamtalk.server.handler.ServerUser
import teamtalk.server.stats.charts.FillWordChart
import teamtalk.server.stats.charts.StatisticChart
import teamtalk.server.stats.charts.TriggerWordChart
import java.io.File

class UserStatistic(private val user: ServerUser) {

    val charts = mutableListOf<StatisticChart>()
    private val fillWordChart = FillWordChart(user.getName())
    private val triggerWordChart = TriggerWordChart(user.getName())

    var sentMessages = 0
    var receivedMessages = 0
    var sentFiles = 0
    var receivedFiles = 0

    init {
        createCharts()
    }

    private fun createCharts() {
        charts.add(fillWordChart)
        charts.add(triggerWordChart)
    }

    fun processMessage(message: String) {
        processFillWords(message)
        processTriggerWords(message)
    }

    fun processMessage(file: File) {
        /*
            TODO: Verarbeitung von FileMessages
        */
    }

    fun incrementSentMessage() {
        sentMessages += 1
    }

    fun incrementReceivedMsg() {
        receivedMessages += 1
    }

    private fun formatMessage(message: String): List<String> {
        val noWords = Regex("\\p{Punct}")
        val spaces = Regex("\\s+")

        val wordsOnly = message.replace(noWords, "")

        val words = wordsOnly.split(spaces).filter { it.isNotBlank() }
        return words
    }

    private fun processFillWords(message: String) {
        val words = formatMessage(message)
        val containsFillWord = words.any() { fillWordChart.isFillWord(it) }

        if (containsFillWord) {
            for (word in words) {
                fillWordChart.countIfFillWord(word)
            }

            fillWordChart.update()
        }
    }

    private fun processTriggerWords(message: String) {
        val words = formatMessage(message)
        val containsTriggerWord = words.any() { triggerWordChart.isTriggerWord(it) }

        if (containsTriggerWord) {
            for (word in words) {
                triggerWordChart.countIfTriggerWord(word)
            }

            triggerWordChart.update()
        }
    }

//    fun getAverageAnswerTime(serverClient: ServerClient): Duration {
//        var averageAnsTime: Duration = Duration.ZERO
//        var messageCount = 0
//
//        val receivedMessages = processedMessages.filter { it.getReceiverName() == serverClient.getUsername() }
//        val sentMessages = processedMessages.filter { it.getSenderName() == serverClient.getUsername() }
//
//        for (received in receivedMessages) {
//            val reply = sentMessages.firstOrNull { it.getTimestamp().isAfter(received.getTimestamp()) }
//            if (reply != null) {
//                averageAnsTime += Duration.between(received.getTimestamp(), reply.getTimestamp())
//                messageCount++
//            }
//        }
//
//        return if (messageCount > 0) {
//            averageAnsTime.dividedBy(messageCount.toLong())
//        } else {
//            Duration.ZERO
//        }
//    }

//    fun getTotalContactAddressing(contact: String): Int {
//        val addressingTrigger = "@"
//        val triggerFilter = Regex("$addressingTrigger\\w+\\s\\w+")
//        var addressingAmount = 0
//
//        for (message in processedMessages) {
//            println("MESSAGE: " + message.getMessage())
//            triggerFilter.findAll(message.getMessage().toString()).forEach { match ->
//                val word = match.value.replace("@", "")
//                println("CHECK: \n$word\n$contact")
//                if (word == contact) {
//                    addressingAmount++
//                }
//            }
//
//        }
//        return addressingAmount
//    }

}