package teamtalk.server.stats

import javafx.scene.control.Label
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import teamtalk.server.handler.ServerUser
import teamtalk.server.stats.charts.FillWordChart
import teamtalk.server.stats.charts.StatisticChart
import teamtalk.server.stats.charts.TriggerWordChart
import java.time.Duration
import java.time.Instant

class UserStatistic(private val user: ServerUser) {

    private val guiScope = CoroutineScope(Dispatchers.JavaFx)

    var sentTextMessages = 0
    var sentFileMessages = 0
    var receivedTextMessages = 0
    var receivedFileMessages = 0
    var usageTime = Duration.ZERO

    val totalMessagesLBL = Label("0")
    val totalTextMessagesLBL = Label("0")
    val totalFileMessagesLBL = Label("0")
    val totalUsersTaggedLBL = Label("0")
    val averageAnswerTimeLBL = Label("0")
    val totalUsageTimeLBL = Label("0")

    private var answerTime: MutableMap<String, MutableList<Duration>> = mutableMapOf()
    private val repliedMessages = mutableSetOf<Message>()

    var taggedUsersCount: MutableMap<String, Int> = mutableMapOf()

    val charts = mutableListOf<StatisticChart>()
    val fillWordChart = FillWordChart(user.getServer(), user)
    val triggerWordChart = TriggerWordChart(user.getServer(), user)

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
                        processTaggedUsers(message.getMessage())
                    }

                    message.getReceiverName() -> {
                        receivedTextMessages += 1
                    }
                }
            }

            is FileMessage -> {
                when (user.getName()) {
                    message.getSenderName() -> {
                        sentFileMessages += 1 }
                    message.getReceiverName() -> {
                        receivedFileMessages += 1
                    }
                }
            }
        }

        processAnswerTime(message)
        updateGUI()
    }

    fun updateGUI() {
        guiScope.launch {
            totalTextMessagesLBL.text = sentTextMessages.toString()
            totalFileMessagesLBL.text = sentFileMessages.toString()
            totalMessagesLBL.text = (sentFileMessages + sentTextMessages).toString()
            averageAnswerTimeLBL.text = formatDuration(getAverageAnswerTime())
            totalUsageTimeLBL.text = formatDuration(usageTime)
            totalUsersTaggedLBL.text = taggedUsersCount.values.sum().toString()
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
    fun processAnswerTime(message: Message) {
        if (message.getSenderName() == user.getName()) {
            val originalMessage = user.getServer().getStats().messages.findLast {
                it.getSenderName() == message.getReceiverName() && it.getReceiverName() == message.getSenderName()
            }

            if (originalMessage != null  && repliedMessages.contains(originalMessage).not()) {
                val messageAnswerTime = Duration.between(originalMessage.getTimestamp(), message.getTimestamp())
                val answerTimeList = answerTime.getOrPut(message.getReceiverName()) { mutableListOf() }
                answerTimeList.add(messageAnswerTime)
                repliedMessages.add(originalMessage)
            }
        }
    }

    fun processTaggedUsers(message: String) {
        val tagRegex = Regex("@[^:]+:")
        val tags = tagRegex.findAll(message).map { it.value }.toList()

        for (tag in tags) {
            val username = tag.removePrefix("@").removeSuffix(":")
            val taggedUser = user.getServer().getUser(username)

            if (taggedUser != null) {
                val currentCount = taggedUsersCount.getOrDefault(taggedUser.getName(), 0)
                taggedUsersCount[taggedUser.getName()] = currentCount + 1
            }
        }
    }

    fun getTotalAnswerTime(): Duration {
        var newAnswerTime = Duration.ZERO

        for (otherUser in user.getServer().getUserNames()) {
            val replyTimes = answerTime[otherUser] ?: listOf()

            for (replyTime in replyTimes) {
                newAnswerTime = newAnswerTime.plus(replyTime)
            }
        }

        return newAnswerTime
    }

    /**
     * Berechnet die durchschnittliche Antwortzeit des Benutzers gegenüber allen anderen Benutzern.
     *
     * @return Die durchschnittliche Antwortzeit als [Duration]. Gibt [Duration.ZERO] zurück, wenn keine Antwortzeiten vorhanden sind.
     */
    fun getAverageAnswerTime(): Duration {
        var totalAnswerTime = Duration.ZERO
        var totalAnswers = 0

        for ((_, responseTimes) in answerTime) {
            for (responseTime in responseTimes) {
                totalAnswerTime = totalAnswerTime.plus(responseTime)
                totalAnswers++
            }
        }

        return if (totalAnswers > 0) totalAnswerTime.dividedBy(totalAnswers.toLong()) else Duration.ZERO
    }

    fun getAnswerTimeTotal(otherUser: ServerUser): Duration {
        val replyTimes = answerTime[otherUser.getName()] ?: listOf()
        var newAnswerTime = Duration.ZERO

        for (replyTime in replyTimes) {
            newAnswerTime = newAnswerTime.plus(replyTime)
        }

        return newAnswerTime
    }

    fun getAnswerTimeAverage(otherUser: ServerUser): Duration {
        val totalAnswerTime = getAnswerTimeTotal(otherUser)
        val answerCount = getAnswerTimes(otherUser).size

        if (answerCount > 0) {
            return totalAnswerTime.dividedBy(answerCount.toLong())
        } else {
            return Duration.ZERO
        }
    }

    fun getAnswerTimes(otherUser: ServerUser): List<Duration> {
        val answerTimes = answerTime[otherUser.getName()]
        if (answerTimes != null) {
            return answerTimes
        }

        return listOf()
    }

    /**
     * Fügt die aktuelle Nutzungszeit zur gesamten Nutzungszeit hinzu.
     * Die Nutzungszeit ist jeweils die Differenz zwischen JETZT und der Loginzeit des Users.
     */
    fun updateUsageTime() {
        usageTime += Duration.between(user.getLoginTime(), Instant.now())
        updateGUI()
    }

    fun getSimpleAnswerTime(): Map<String, List<Long>> {
        val convertedMap = mutableMapOf<String, List<Long>>()

        for ((userName, times) in answerTime) {
            val timesInMilliseconds = times.map { it.toMillis() }

            convertedMap[userName] = timesInMilliseconds
        }

        return convertedMap
    }

    fun setFromSimpleAnswerTime(data: Map<String, List<Long>>) {
        for ((userName, durations) in data) {
            val newDurationsList: MutableList<Duration> = mutableListOf()
            for (duration in durations) {
                newDurationsList.add(Duration.ofMillis(duration))
            }

            answerTime[userName] = newDurationsList
        }
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}