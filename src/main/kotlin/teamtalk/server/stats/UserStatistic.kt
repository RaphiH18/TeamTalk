package teamtalk.server.stats

import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import teamtalk.server.handler.ServerUser
import teamtalk.server.handler.UserData
import teamtalk.server.stats.charts.FillWordChart
import teamtalk.server.stats.charts.StatisticChart
import teamtalk.server.stats.charts.TriggerWordChart
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.*
import java.time.Duration
import java.time.Instant

class UserStatistic(private val user: ServerUser) {

    var sentTextMessages = 0
    var receivedTextMessages = 0
    var sentFileMessages = 0
    var receivedFileMessages = 0
    var usageTime = Duration.ZERO
    var answerTime = mutableMapOf<ServerUser, Duration>()

    val charts = mutableListOf<StatisticChart>()
    val fillWordChart = FillWordChart(user)
    val triggerWordChart = TriggerWordChart(user)

    init {
        charts.add(fillWordChart)
        charts.add(triggerWordChart)
    }

    //Ruft alle nötigen userbezogenen Funktionen zur Verarbeitung einer Nachricht auf.
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

    //Verarbeitet die Verwendung von Füllwörtern in einer Nachricht und aktualisiert das userbezogene Chart dazu.
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

    //Verarbeitet die Verwendung von Triggerwörtern in einer Nachricht und aktualisiert das userbezogene Chart dazu.
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

    /**
     * Bereitet eine Nachricht vor, indem alle Satzzeichen entfernt und der Text in einzelne Wörter aufgeteilt wird.
     *
     * @param message Die zu verarbeitende Nachricht.
     * @return Eine Liste von Wörtern ohne Satzzeichen, mehrfache Leerzeichen werden ignoriert. Die Liste enthält keine leeren Strings.
     */
    private fun formatMessage(message: String): List<String> {
        val noWords = Regex("\\p{Punct}")
        val spaces = Regex("\\s+")

        val wordsOnly = message.replace(noWords, "")

        val words = wordsOnly.split(spaces).filter { it.isNotBlank() }
        return words
    }

    /**
     * Berechnet die durchschnittliche Antwortzeit zwischen diesem und einem anderen Benutzer.
     * Es werden alle empfangenen Nachrichten durchgegangen - sobald eine Antwort auf eine empfangene Nachricht gefunden wurde, wird daraus der zeitliche Unterschied berechnet.
     * Nach der Errechnung aller Antwortzeiten wird daraus (anhand der totalen Antworten) eine durchschnittliche Antwortzeit berechnet.
     *
     * @param otherUser Der andere Benutzer, mit dem die Kommunikation analysiert wird.
     */
    fun processAnswerTime(otherUser: ServerUser) {
        var averageAnsTime: Duration = Duration.ZERO
        var messageCount = 0

        val receivedMessages = user.getServer().getStats().processedMessages.filter {
            it.getReceiverName() == user.getName() && it.getSenderName() == otherUser.getName()
        }

        val sentMessages = user.getServer().getStats().processedMessages.filter {
            it.getSenderName() == user.getName() && it.getReceiverName() == otherUser.getName()
        }

        for (received in receivedMessages) {
            val reply = sentMessages.firstOrNull { it.getTimestamp().isAfter(received.getTimestamp()) }
            if (reply != null) {
                val answerTime = Duration.between(received.getTimestamp(), reply.getTimestamp())
                println(answerTime)
                averageAnsTime += answerTime
                messageCount++
            }
        }

        if (messageCount > 0) {
            val answerTimeDuration = averageAnsTime.dividedBy(messageCount.toLong())
            answerTime[otherUser] = getAverageAnswerTime(otherUser) + answerTimeDuration
        }
    }

    /**
     * Fügt die aktuelle Nutzungszeit zur gesamten Nutzungszeit hinzu.
     * Die Nutzungszeit ist jeweils die Differenz zwischen JETZT und der Loginzeit des Users.
     */
    fun updateUsageTime() {
        usageTime += Duration.between(user.getLoginTime(), Instant.now())
    }

    fun getAverageAnswerTime(otherUser: ServerUser): Duration {
        return this.answerTime[otherUser] ?: Duration.ZERO
    }

//    private fun toUserData(): UserData {
//        return UserData(
//            this.sentTextMessages,
//            this.receivedTextMessages,
//            this.sentFileMessages,
//            this.receivedFileMessages,
//            this.usageTime.toMillis(),
//            toSimpleAnswerTime(this.answerTime),
//            this.fillWordChart.getData(),
//            this.triggerWordChart.getData(),
//        )
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

    fun loadData() {
        val decoder = XMLDecoder(BufferedInputStream(FileInputStream("userdata/${user.getName()}.xml")))
        val userData = decoder.readObject() as UserData

        this.sentTextMessages = userData.sentTextMessages
        this.sentFileMessages = userData.sentFileMessages
        this.receivedTextMessages = userData.receivedTextMessages
        this.receivedFileMessages = userData.receivedFileMessages
        this.usageTime = Duration.ofMillis(userData.usageTime)
        this.answerTime = fromSimpleAnswerTime(userData.answerTime)

        this.fillWordChart.setData(userData.fillWordStats)
        this.triggerWordChart.setData(userData.triggerWordStats)
    }

    fun saveToFile() {
        val directory = File("userdata")
        if (directory.exists().not()) {
            directory.mkdirs()
        }

        val encoder = XMLEncoder(BufferedOutputStream(FileOutputStream("userdata/${user.getName()}.xml")))
        //encoder.writeObject(toUserData())
        encoder.close()
    }

    private fun toSimpleAnswerTime(data: Map<ServerUser, Duration>): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for ((user, duration) in data) {
            map[user.getName()] = duration.toMillis()
        }

        return map
    }

    private fun fromSimpleAnswerTime(data: Map<String, Long>): MutableMap<ServerUser, Duration> {
        val map = mutableMapOf<ServerUser, Duration>()
        for ((userName, durationLong) in data) {
            val foundUser = user.getServer().getUser(userName)
            if (foundUser != null) {
                map[foundUser] = Duration.ofMillis(durationLong)
            }
        }

        return map
    }
}