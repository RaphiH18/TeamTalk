package teamtalk.server.stats

import org.w3c.dom.Text
import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import teamtalk.server.handler.ServerUser
import teamtalk.server.handler.UserData
import teamtalk.server.stats.charts.FillWordChart
import teamtalk.server.stats.charts.StatisticChart
import teamtalk.server.stats.charts.TriggerWordChart
import java.io.File
import kotlin.time.Duration

class UserStatistic(private val user: ServerUser) {

    var sentTextMessages = 0
    var receivedTextMessages = 0
    var sentFileMessages = 0
    var receivedFileMessages = 0

    val charts = mutableListOf<StatisticChart>()
    val fillWordChart = FillWordChart(user)
    val triggerWordChart = TriggerWordChart(user)

    init {
        charts.add(fillWordChart)
        charts.add(triggerWordChart)
    }

    fun processMessage(message: Message) {
        when(message) {
            is TextMessage -> {
                when (user.getName()) {
                    message.getSenderName() -> {
                        sentTextMessages += 1
                        processFillWords(message.getMessage())
                        processTriggerWords(message.getMessage())
                    }

                    message.getReceiverName() -> {
                        receivedTextMessages += 1
                    }
                }
            }
            is FileMessage -> {
                println("FILE!")
                when (user.getName()) {
                    message.getSenderName() -> {
                        sentFileMessages += 1
                    }
                    message.getReceiverName() -> {
                        receivedFileMessages += 1
                    }
                }
            }
        }
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

    fun toUserData(): UserData {
        return UserData(
            this.sentTextMessages,
            this.receivedTextMessages,
            this.sentFileMessages,
            this.receivedFileMessages,
            this.fillWordChart.getData(),
            this.triggerWordChart.getData(),
        )
    }

//    fun getAverageAnswerTime(otherUser: ServerUser): Duration {
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