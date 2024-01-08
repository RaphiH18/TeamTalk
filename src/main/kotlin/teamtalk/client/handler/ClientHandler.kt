package teamtalk.client.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.client.messaging.Contact
import teamtalk.client.messaging.TextMessage
import teamtalk.jsonUtil
import teamtalk.logger.debug
import teamtalk.logger.log
import java.io.*
import java.net.Socket
import java.time.Instant

class ClientHandler(private var chatClient: ChatClient) {

    private lateinit var socket: Socket
    private lateinit var output: PrintWriter
    private lateinit var input: BufferedReader

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
                    output = PrintWriter(socket.getOutputStream())
                    input = BufferedReader(InputStreamReader(socket.getInputStream()))

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

            send(ClientMessage.HELLO.getJSONString(chatClient))

            while (isConnected()) {
                process(input.readLine())
            }
        }
    }

    private fun process(receivedString: String) {
        debug("<- Von Server erhalten: $receivedString")

        if (jsonUtil.isJSON(receivedString)) {
            val jsonObj = JSONObject(receivedString)
//            println("konvertiertes Object: $jsonObj")
//            println("Gefundender Type: ${jsonObj.get("type")}")

            when (jsonObj.get("type")) {
                "HELLO_RESPONSE" -> {
                    val jsonUsers = jsonObj.getJSONArray("userList")
                    for (user in jsonUsers) {
                        // AUSKOMMENTIEREN FUER TEST-ZWECKE
                        /*if (user != chatClient.getUsername()) {
                            contacts.add(Contact(user.toString()))
                        }*/
                        contacts.add(Contact(user.toString()))
                    }
                }

                "MESSAGE_RESPONSE" -> {
                    val message = jsonObj.getString("message")
                    val senderName = jsonObj.getString("senderName")
                    val contact = contacts.find { it.getUsername() == jsonObj.getString("receiverName") }
//                    println("Found Contact: ${contact?.getUsername()}")

                    if (contact != null) {
                        contact.addMessage(TextMessage(senderName, Instant.now(), message))
//                        println("neue Nachricht\n" + contact.getMessages())
                        chatClient.getGUI().updateGuiMessagesFromContact(contact)
                    }
                }

                "MESSAGE" -> {
                    val message = jsonObj.getString("message")
                    val contact = contacts.find { it.getUsername() == jsonObj.getString("senderName") }
//                    println("Found Contact: ${contact?.getUsername()}")

                    if (contact != null) {
                        contact.addMessage(TextMessage(contact.getUsername(), Instant.now(), message))
//                        println("Aktuelle Nachrichten von Kontakt: ${contact.getUsername()}")
                        for(item in contact.getMessages()){
//                            println(item.getMessage())
                        }
//                        println("neue Nachricht\n" + contact.getMessages())
                        chatClient.getGUI().updateGuiMessagesFromContact(contact)
                    }
                }

                "STATUS_UPDATE" -> {
//                    println("Update Contact Status")
                    chatClient.getGUI().updateContactStatus(jsonObj)
                    for(contact in contacts){
                        println("Kontaktname: " + contact.getUsername() + " Status: " + contact.isOnline())
                    }
//                    println("Update KontaktView")
                    chatClient.getGUI().updateContactView()
                }
            }
        } else {
            log("Warnung beim Verarbeiten der Nachricht: Kein JSON-Objekt.")
        }
    }

    fun send(string: String) {
        handlerScope.launch {
            if (isConnected()) {
                output.println(string)
                output.flush()
                debug("-> An Server gesendet: $string")
            } else {
                throw IllegalStateException("Keine Verbindung - bitte zuerst eine Verbindung aufbauen.")
            }
        }
    }

    fun send(file: File) {

    }

    fun isConnected() = ((::socket.isInitialized && socket.isConnected))

    fun getStatusMessage() = status

    fun getContacts() = contacts

    fun getContacts(status: String): MutableList<Contact> {
        val contactList = mutableListOf<Contact>()
        when (status){
            "offline" -> {
                for (contact in getContacts()){
                    if (contact.isOnline().not()){
                       contactList.add(contact)
                    }
                }
            }
            "online" ->{
                for (contact in getContacts()) {
                    if (contact.isOnline()) {
                        contactList.add(contact)
                    }
                }
            }
        }
        return contactList
    }
}