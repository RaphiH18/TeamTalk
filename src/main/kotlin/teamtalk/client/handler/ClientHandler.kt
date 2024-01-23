package teamtalk.client.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.message.Contact
import teamtalk.message.TextMessage
import teamtalk.jsonUtil
import teamtalk.logger.debug
import teamtalk.logger.log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.time.Instant

class ClientHandler(private var chatClient: ChatClient) {

    private lateinit var socket: Socket
    private lateinit var output: DataOutputStream
    private lateinit var input: DataInputStream

    private val handlerScope = CoroutineScope(Dispatchers.IO)

    private var status = "Bereit"

    /*
    In der "contacts"-Liste des Clients werden beim HELLO-Prozess alle User, die auf dem Server existieren (egal ob on- oder offline) hinzugefügt.
    Jedes "Contact"-Objekt besitzt einen Online-Status (Boolean), der durch den STATUS_UPDATE-Prozess aktualisiert wird.
    Das GUI zeigt in der Kontakt-Liste jedoch nur die Kontakte an, die Online sind.
     */
    private val contacts = mutableListOf<Contact>()

    fun connect(server: String, port: Int) {
        var timeoutCounter = 0
        val connectionLimit = 10

        handlerScope.launch {
            do {
                try {
                    debug("Verbindung zum Server $server mit Port $port herstellen...")
                    status = "Verbinde..."

                    socket = Socket(server, port)
                    output = DataOutputStream(socket.getOutputStream())
                    input = DataInputStream(socket.getInputStream())

                    status = "Verbunden"

                    debug("Verbindung erfolgreich hergestellt.")
                } catch (e: IOException) {
                    debug("Verbindung zum Server fehlgeschlagen... Neuer Versuch...")
                    delay(1000)

                    timeoutCounter++
                    if (timeoutCounter >= connectionLimit) {
                        status = "Timeout"
                    }
                }
            } while (!(isConnected()) and (timeoutCounter <= connectionLimit))

            send(ClientHeader.HELLO.toJSON(chatClient))

            while (isConnected()) {
                process()
            }
        }
    }

    private fun process() {
        val headerSize = input.readInt()
        val headerBytes = ByteArray(headerSize)
        input.readFully(headerBytes)
        val headerString = String(headerBytes, Charsets.UTF_8)

        debug("<- Von Server erhalten (Header): $headerString")

        if (jsonUtil.isJSON(headerString)) {
            val headerJSON = JSONObject(headerString)
            val payloadSize = headerJSON.getInt("payloadSize")

            when (headerJSON.get("type")) {
                "HELLO_RESPONSE" -> {
                    val userList = headerJSON.getJSONArray("userList")
                    for (user in userList) {
//                        if (user != chatClient.getUsername()) {
//                            contacts.add(Contact(user.toString()))
//                        }
                        contacts.add(Contact(user.toString()))
                    }
                    chatClient.getGUI().updateContactStatus(headerJSON)
                }

                "MESSAGE_RESPONSE" -> {
                    val messageBytes = ByteArray(payloadSize)
                    input.readFully(messageBytes)
                    val message = String(messageBytes, Charsets.UTF_8)

                    val contact = contacts.find { it.getUsername() == headerJSON.getString("receiverName") }
                    if (contact != null) {
                        contact.addMessage(
                            TextMessage(
                                chatClient.getUsername(),
                                contact.getUsername(),
                                Instant.now(),
                                message
                            )
                        )
                        chatClient.getGUI().updateGuiMessagesFromContact(contact)
                    }
                }

                "MESSAGE" -> {
                    val messageBytes = ByteArray(payloadSize)
                    input.readFully(messageBytes)
                    val message = String(messageBytes, Charsets.UTF_8)

                    val contact = contacts.find { it.getUsername() == headerJSON.getString("senderName") }
                    if (contact != null) {
                        contact.addMessage(
                            TextMessage(
                                contact.getUsername(),
                                chatClient.getUsername(),
                                Instant.now(),
                                message
                            )
                        )
                        chatClient.getGUI().updateGuiMessagesFromContact(contact)
                    }
                }

                "STATUS_UPDATE" -> {
                    chatClient.getGUI().updateContactStatus(headerJSON)
                    chatClient.getGUI().updateContactView()
                }
            }
        } else {
            log("Warnung beim Verarbeiten der Nachricht: Kein JSON-Objekt.")
        }
    }

    /*
    Die Methode kann für Textnachrichten sowie auch für Dateien verwendet werden.
    Sie setzt voraus, dass ein Header und Nutzdaten existieren, welche dann via DataOutputStream über den Socket gesendet werden.

    Der Header beinhaltet alle Kontrollinformationen wie z.B. den Typ oder die Grösse der Nutzdaten (payloadSize).
    Er kann mithilfe der ClientMessage für alle Nachrichten, die der Client senden muss, zusammengestellt werden.

    Die Nutzdaten werden direkt als ByteArray über den Socket gesendet.
     */
    fun send(header: JSONObject, payloadBytes: ByteArray = byteArrayOf()) {
        handlerScope.launch {
            if (isConnected()) {
                val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
                output.writeInt(headerBytes.size)
                output.write(headerBytes)

                if (payloadBytes.isNotEmpty()) {
                    output.write(payloadBytes)
                    output.flush()
                }

                debug("-> An Server gesendet (Header): $header")
            } else {
                throw IllegalStateException("Keine Verbindung - bitte zuerst eine Verbindung aufbauen.")
            }
        }
    }

    fun sendHeader(header: JSONObject) {
        handlerScope.launch {
            if (isConnected()) {
                val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
                output.writeInt(headerBytes.size)
                output.write(headerBytes)

                debug("-> An Server gesendet: Nur Header ($header)")
            } else {
                throw IllegalStateException("Keine Verbindung - bitte zuerst eine Verbindung aufbauen.")
            }
        }
    }

    fun sendPayload(payloadBytes: ByteArray = byteArrayOf()) {
        handlerScope.launch {
            if (isConnected()) {
                if (payloadBytes.isNotEmpty()) {
                    output.write(payloadBytes)
                    output.flush()
                }

                debug("-> An Server gesendet: Nur Daten mit der Grösse ${payloadBytes.size}")
            } else {
                throw IllegalStateException("Keine Verbindung - bitte zuerst eine Verbindung aufbauen.")
            }
        }
    }

    fun isConnected() = ((::socket.isInitialized && socket.isConnected))

    fun getStatusMessage() = status

    fun getContacts() = contacts
}