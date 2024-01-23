package teamtalk.server.handler

import teamtalk.message.FileMessage
import teamtalk.message.Message
import teamtalk.message.TextMessage
import java.time.Duration

class ServerStatistic(private val server: ChatServer) {

    /* Übersicht Diagramm-Arten

    Fullwörter:             Balkendiagramm / Kuchendiagramm
    Triggerwörter:          Kuchendiagramm
    Antwort- und Nutzungs-Zeiten:          Liniendiagramm

    Nutzungszeiten:         in Textform
    Filetransfer:           in Textform

     */

    val messages = mutableListOf<Message>()

    private val FILL_KEYWORDS =
        listOf("der", "die", "das", "und", "eoder", "Also", "Quasi", "Sozusagen", "Wie gesagt", "Eigentlich")

    private val POSITIVE_KEYWORDS = listOf("gut", "ja", "super", "perfekt", "optimal", "prima")
    private val NEGATIVE_KEYWORDS = listOf("schlecht", "nein", "schade")
    private val NEUTRAL_KEYWORDS = listOf("ok", "in Ordnung", "passt")

    fun getTotalMessages() = messages.size

    fun getTotalTextMessages() =
        messages.count() { it is TextMessage }

    fun getTotalFileMessages() =
        messages.count() { it is FileMessage }

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
        messages.count() { it.getSenderName() == serverClient.getUsername() && it is TextMessage }

    fun getFileMessages(serverClient: ServerClient) =
        messages.count() { it.getSenderName() == serverClient.getUsername() && it is FileMessage }

    fun getAverageAnswerTime(serverClient: ServerClient): Duration {
        var averageAnsTime: Duration = Duration.ZERO
        var messageCount = 0

        val receivedMessages = messages.filter { it.getReceiverName() == serverClient.getUsername() }
        val sentMessages = messages.filter { it.getSenderName() == serverClient.getUsername() }

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

        for (message in messages) {
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
}