package teamtalk.client.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import teamtalk.client.clientLogger.debug
import teamtalk.client.clientLogger.log
import teamtalk.message.Contact
import teamtalk.message.FileMessage
import teamtalk.message.TextMessage
import teamtalk.utilities
import java.io.*
import java.net.Socket
import java.time.Instant

class ClientHandler(private var chatClient: ChatClient) {

    private lateinit var socket: Socket

    private lateinit var input: DataInputStream
    private lateinit var output: DataOutputStream
    val handlerScope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private var status = "Bereit"

    /**
    In der "contacts"-Liste des Clients werden beim HELLO-Prozess alle User, die auf dem Server existieren (egal ob on- oder offline) hinzugefügt.
    Jedes "Contact"-Objekt besitzt einen Online-Status (Boolean), der durch den STATUS_UPDATE-Prozess aktualisiert wird.
    Das GUI zeigt in der Kontakt-Liste jedoch nur die Kontakte an, die Online sind.
     */
    private val contacts = mutableListOf<Contact>()

    fun disconnect() {
        if (isConnected()) {
            input.close()
            output.close()
            status = "Bereit"
        }
    }

    /**
     * Stellt eine Verbindung zum Server her und verarbeitet eingehende Nachrichten.
     *
     * @param server Die Adresse des Servers.
     * @param port Der Port des Servers.
     */
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
                try {
                    process()
                } catch(e: Exception) {
                    log("Verbindung zum Server getrennt.")
                    chatClient.getGUI().exit(chatClient.getGUI().primaryStage)
                    break
                }
            }
        }
    }

    private fun process() {
        val headerSize = input.readInt()
        val headerBytes = ByteArray(headerSize)
        input.readFully(headerBytes)
        val headerString = String(headerBytes, Charsets.UTF_8)

        debug("<- Von Server erhalten (Header): $headerString")
        if (utilities.isJSON(headerString)) {
            val headerJSON = JSONObject(headerString)
            val payloadSize = headerJSON.getInt("payloadSize")

            when (headerJSON.get("type")) {
                "HELLO_RESPONSE" -> {
                    val userList = headerJSON.getJSONArray("userList")
                    for (user in userList) {
                        if (user != chatClient.getUsername()) {
                            contacts.add(Contact(user.toString()))
                        }
                    }
                    chatClient.getGUI().updateContactStatus(headerJSON)
                }

                "MESSAGE_RESPONSE" -> {
                    val messageBytes = ByteArray(payloadSize)
                    input.readFully(messageBytes)
                    val messageText = String(messageBytes, Charsets.UTF_8)

                    val contact = contacts.find { it.getUsername() == headerJSON.getString("receiverName") }
                    if (contact != null) {
                        val message = TextMessage(chatClient.getUsername(), contact.getUsername(), Instant.now(), messageText)
                        contact.addMessage(message)

                        chatClient.getGUI().updateMessages(contact, "NEW_MESSAGE")
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
                        chatClient.getGUI().updateMessages(contact, "NEW_MESSAGE")
                    }
                }

                "FILE" -> {
                    debug("<- Von Server erhalten: Nur Daten mit der Grösse $payloadSize")
                    val fileBytes = ByteArray(payloadSize)

                    var bytesReadTotal = 0
                    while (bytesReadTotal < payloadSize) {
                        val bytesRead = input.read(fileBytes, bytesReadTotal, payloadSize - bytesReadTotal)
                        bytesReadTotal += bytesRead
                        debug("<- Von Server erhalten: Nur Daten, eingelesen: $bytesRead")
                    }

                    val userHome = System.getProperty("user.home")
                    val fileName = headerJSON.getString("filename")
                    val file = File(userHome, fileName)
                    val fos = FileOutputStream(file)
                    fos.write(fileBytes)
                    fos.close()

                    val contact = contacts.find { it.getUsername() == headerJSON.getString("senderName") }
                    if (contact != null) {
                        contact.addMessage(
                            FileMessage(
                                contact.getUsername(),
                                chatClient.getUsername(),
                                Instant.now(),
                                file
                            )
                        )
                        chatClient.getGUI().updateMessages(contact, "NEW_MESSAGE")
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

    /**
    Die Methode kann für Textnachrichten sowie auch für Dateien verwendet werden.
    Sie setzt voraus, dass ein Header und Nutzdaten existieren, welche dann via DataOutputStream über den Socket gesendet werden.

    Der Header beinhaltet alle Kontrollinformationen wie z.B. den Typ oder die Grösse der Nutzdaten (payloadSize).
    Er kann mithilfe der ClientMessage für alle Nachrichten, die der Client senden muss, zusammengestellt werden.

    Die Nutzdaten werden direkt als ByteArray über den Socket gesendet.
     */

    /**
     * Sendet eine Nachricht an den Server.
     *
     * @param header Der JSON-Header der Nachricht.
     * @param payloadBytes Die Nutzdaten der Nachricht als Byte-Array.
     */
    fun send(header: JSONObject?, payloadBytes: ByteArray? = byteArrayOf()) {
        handlerScope.launch {
            if (header != null) {
                sendHeader(header)
            }

            if (payloadBytes != null) {
                sendPayload(payloadBytes)
            }
        }
    }

    /**
     * Sendet einen Header an den Server.
     * Die getrennte Übermittlung von Header und Nutzdaten wird für die Dateiübertragung verwendet.
     *
     * @param header Der JSON-Header der Nachricht.
     */
    suspend fun sendHeader(header: JSONObject) {
        if (isConnected()) {
            val headerBytes = header.toString().toByteArray(Charsets.UTF_8)

            if (headerBytes.isNotEmpty()) {
                mutex.withLock {
                    output.writeInt(headerBytes.size)
                    output.write(headerBytes)
                    output.flush()
                }
            }

            debug("-> An Server gesendet (${headerBytes.size} Bytes): Nur Header ($header)")
        } else {
            throw IllegalStateException("Keine Verbindung - bitte zuerst eine Verbindung aufbauen.")
        }
    }

    /**
     * Sendet nur Nutzdaten an den Server.
     * Die getrennte Übermittlung von Header und Nutzdaten wird für die Dateiübertragung verwendet.
     *
     * @param header Der JSON-Header der Nachricht.
     */
    suspend fun sendPayload(payloadBytes: ByteArray = byteArrayOf()) {
        if (isConnected()) {
            if (payloadBytes.isNotEmpty()) {
                mutex.withLock {
                    output.write(payloadBytes)
                    output.flush()
                }
            }

            debug("-> An Server gesendet (${payloadBytes.size} Bytes): Nur Daten")
        } else {
            throw IllegalStateException("Keine Verbindung - bitte zuerst eine Verbindung aufbauen.")
        }
    }

    fun isConnected() = ((::socket.isInitialized && socket.isConnected))

    /**
     * @return Status-Nachricht, welche im Verbindungsfenster des GUIs angezeigt wird.
     */
    fun getStatusMessage() = status

    /**
     * @return Alle Kontakte, welche der ChatClient kontaktieren kann.
     */
    fun getContacts() = contacts
}