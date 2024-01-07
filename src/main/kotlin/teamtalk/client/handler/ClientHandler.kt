package teamtalk.client.handler

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlinx.coroutines.*
import org.json.JSONObject
import teamtalk.client.messaging.Contact
import teamtalk.client.messaging.TextMessage
import teamtalk.jsonUtil
import teamtalk.logger.debug
import teamtalk.logger.log
import java.io.IOException
import java.time.Instant

class ClientHandler(private var client: ChatClient) {

    private lateinit var socket: Socket
    private lateinit var output: PrintWriter
    private lateinit var input: BufferedReader

    private val handlerScope = CoroutineScope(Dispatchers.IO)
    private var userListStatus = false
    private var status = "Bereit"

    private val contacts = mutableListOf<Contact>()

    fun connect(server: String, port: Int) {
        var timeoutCounter = 0
        val connectionLimit = 10

        handlerScope.launch {
            client = ChatClient()
            do {
                try {
                    debug("Verbindung zum Server $server mit Port $port herstellen...")
                    status = "Verbinde..."
                    socket = Socket(server, port)
                    status = "Verbunden"

                    output = PrintWriter(socket.getOutputStream())
                    input = BufferedReader(InputStreamReader(socket.getInputStream()))
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

            send(ClientMessage.HELLO.getJSONString(client))

            while (isConnected()) {
                process(input.readLine())
            }
        }
    }

    private fun process(receivedString: String) {
        debug("<- Von Server erhalten: $receivedString")

        if (jsonUtil.isJSON(receivedString)) {
            val jsonObj = JSONObject(receivedString)
            println("konvertiertes Object: $jsonObj")
            println("Gefundender Type: ${jsonObj.get("type")}")

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
                    userListStatus = true
                }

                "MESSAGE_RESPONSE" -> {
                    val message = jsonObj.getString("message")
                    val senderName = jsonObj.getString("senderName")
                    val contact = contacts.find { it.getUsername() == jsonObj.getString("receiverName") }
                    println("Found Contact: ${contact?.getUsername()}")

                    if (contact != null) {
                        contact.addMessage(TextMessage(senderName, Instant.now(), message))
                        println("neue Nachricht\n" + contact.getMessages())
                        client.getGUI().updateGuiMessagesFromContact(contact)
                    }
                }

                "MESSAGE" -> {
                    val message = jsonObj.getString("message")
                    val contact = contacts.find { it.getUsername() == jsonObj.getString("senderName") }
                    println("Found Contact: ${contact?.getUsername()}")

                    if (contact != null) {
                        contact.addMessage(TextMessage(contact.getUsername(), Instant.now(), message))
                        println("Aktuelle Nachrichten von Kontakt: ${contact.getUsername()}")
                        for(item in contact.getMessages()){
                            println(item.getMessage())
                        }
                        println("neue Nachricht\n" + contact.getMessages())
                        client.getGUI().updateGuiMessagesFromContact(contact)
                    }
                }

                "STATUS_UPDATE" -> {
                    println("Update Contact Status")
                    client.getGUI().updateContactStatus(jsonObj)
                    for(contact in contacts){
                        println("Kontaktname: " + contact.getUsername() + " Status: " + contact.getStatus())
                    }
                    println("Update KontaktView")
                    client.getGUI().updateContactView()
                }
            }
        } else {
            log("Warnung beim Verarbeiten der Nachricht: Kein JSON-Objekt.")
        }
    }

    fun send(string: String) {
        handlerScope.launch {
            if (isConnectionReady()) {
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

    fun isUserListStatus(): Boolean {
        while (!getUserListStatus()) {
            handlerScope.launch {
                delay(100)
            }
        }
        return getUserListStatus()
    }

    private fun isConnectionReady() =
        ((::socket.isInitialized) and (::input.isInitialized) and (::output.isInitialized) and (socket.isConnected))

    fun isConnected() = ((::socket.isInitialized && socket.isConnected))

    fun getStatusMessage() = status

    private fun getUserListStatus(): Boolean {
        return userListStatus
    }

    fun getServerUsers() = contacts
}