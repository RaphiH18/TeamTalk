package teamtalk.client.handler

import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javafx.scene.control.ListView
import javafx.scene.control.TextArea
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import kotlinx.coroutines.*
import org.json.JSONObject
import teamtalk.client.ClientMessage
import teamtalk.client.message.Contact
import teamtalk.jsonUtil
import teamtalk.server.logger.debug
import teamtalk.server.logger.log
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClientHandler(private val client: ChatClient) {

    private lateinit var socket: Socket
    private lateinit var output: PrintWriter
    private lateinit var input: BufferedReader
    private lateinit var chatClient: ChatClient

    private val handlerScope = CoroutineScope(Dispatchers.IO)
    private val serverUsers = mutableListOf<String>()
    private var userListStatus = false
    private var status = "Bereit"

    private var currentUser = ""

    private var currentUserLbl = Label()
    private val outputChatTa = TextArea()
    val sendChatBtn = Button("Senden")

    private val contacts = mutableListOf<Contact>()

    fun connect(server: String, port: Int) {
        var timeoutCounter = 0
        val connectionLimit = 10

        handlerScope.launch {
            chatClient = ChatClient()
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

            when (jsonObj.get("type")) {
                "HELLO_RESPONSE" -> {
                    val jsonUsers = jsonObj.getJSONArray("userList")
                    for (user in jsonUsers) {
                        serverUsers.add(user.toString())
                    }
                    createContacts()

                    userListStatus = true
                }

                "MESSAGE_RESPONSE" -> { // Bestätigung für Nachrichtenversand (Falls notwendig)
                    println("MESSAGE_RESPONSE aufgerufen mit $jsonObj")
                    //val jsonMessage = jsonObj.get("message")
                    //println("MESSAGE_RESPONSE Erhaltene Nachricht: $jsonMessage")
                    addMessageToContact(jsonObj)
                    updateGuiMessagesFromContact(currentUser)
                }

                "MESSAGE" -> {
                    println("MESSAGE aufgerufen mit $jsonObj")
                    //val jsonMessage = jsonObj.get("message")
                    //println("Erhaltene Message: $jsonMessage")
                    addMessageToContact(jsonObj)
                    updateGuiMessagesFromContact(jsonObj.get("receiverName").toString())
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

    fun isConnectionReady() =
        ((::socket.isInitialized) and (::input.isInitialized) and (::output.isInitialized) and (socket.isConnected))

    fun isConnected() = ((::socket.isInitialized && socket.isConnected))

    fun getStatusMessage() = status

    fun getUserListStatus(): Boolean {
        return userListStatus
    }

    fun getServerUsers() = serverUsers

    fun createContactView(currentUser: String): Node {
        var contactData = observableArrayList<String>()
        for (user in getServerUsers()) {
            contactData.add(user)
            /*if (user != currentUser) {
                contactData.add(user)
            }*/
        }
        val contactList = ListView(contactData).apply {
            minWidth = 200.0
            maxWidth = 200.0
            prefHeight = 600.0
            setOnMouseClicked { _ ->
                var selectedUser = selectionModel.selectedItem
                setCurrentUserLbl(selectedUser)
                updateGuiMessagesFromContact(selectedUser)
            }
        }
        return contactList
    }

    fun createContacts() {
        for (user in getServerUsers()) {
            /*if (user != chatClient.getUsername()) {
                contacts.add(Contact(user))
            }*/
            contacts.add(Contact(user))
        }
        println("Generierte Kontaktliste: $contacts")
    }

    fun createChattingView(currentUser: String): Node {
        var defaultUser: String = ""
        for (user in getServerUsers()) {
            if (user != currentUser) {
                defaultUser = user
                break
            }
        }

        currentUserLbl.apply {
            prefHeight = 50.0
            prefWidth = 580.0
            font = Font("Arial", 24.0)
            style = ("-fx-background-color: #aaaaaa;");
            alignment = Pos.CENTER
            text = defaultUser
        }

        outputChatTa.apply {
            prefHeight = 300.0
            prefWidth = 280.0
            text = "Chatfenster..."
            isEditable = false
        }

        val inputChatTa = TextArea("Schreiben...").apply {
            prefHeight = 100.0
            prefWidth = 280.0
        }

        val inputChatVb = VBox().apply {
            padding = Insets(40.0, 0.0, 25.0, 0.0)
            children.add(inputChatTa)
        }

        sendChatBtn.apply {
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            prefHeight = 30.0
            prefWidth = 280.0
            setOnAction {
                if (inputChatTa.text.isEmpty().not()) {
                    send(ClientMessage.MESSAGE.getJSONString(client, inputChatTa.text, currentUser))
                    inputChatTa.clear()
                }
            }
        }

        val chatContentVb = VBox().apply {
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            with(children) {
                add(outputChatTa)
                add(inputChatVb)
                add(sendChatBtn)
            }
        }

        val dataTransferLbl = Label("Datentransfer").apply {
            prefHeight = 25.0
            prefWidth = 290.0
            font = Font("Arial", 20.0)
            style = ("-fx-background-color: #C9C9C9;");
            alignment = Pos.CENTER
        }

        val receiveLbl = Label("Empfangen").apply {
            prefWidth = 250.0
            font = Font("Arial", 18.0)
            style = ("-fx-background-color: #E8E8E8")
        }

        // Ergänzen dynamische Gruppe für Downloadable Files
        val testFile1Btn = Button("Meilensteintrendanalyse.xlsx").apply {
            prefWidth = 250.0
        }
        val testFile1Vb = VBox().apply {
            padding = Insets(5.0, 0.0, 0.0, 0.0)
            children.add(testFile1Btn)
        }

        val testFile2Btn = Button("Praesentation.pptx").apply {
            prefWidth = 250.0
        }
        val testFile2Content = VBox().apply {
            padding = Insets(5.0, 0.0, 0.0, 0.0)
            children.add(testFile2Btn)
        }

        val receiveContentVb = VBox().apply {
            prefHeight = 390.0
            padding = Insets(5.0, 0.0, 0.0, 20.0)
            with(children) {
                add(receiveLbl)
                add(testFile1Vb)
                add(testFile2Content)
            }
        }

        val sendLbl = Label("Senden").apply {
            prefWidth = 250.0
            font = Font("Arial", 18.0)
            style = ("-fx-background-color: #E8E8E8")
        }
        val sendVb = VBox().apply {
            padding = Insets(0.0, 0.0, 5.0, 0.0)
            children.add(sendLbl)
        }

        val chosenFileLbl = Label("Noch keine Datei ausgewählt").apply {
            prefWidth = 250.0
            alignment = Pos.CENTER
        }

        val filePickerBtn = Button("Datei auswählen").apply {
            prefHeight = 30.0
            prefWidth = 170.0
        }

        val filePickerVb = VBox().apply {
            padding = Insets(0.0, 5.0, 0.0, 0.0)
            children.add(filePickerBtn)
        }

        val sendDataBtn = Button("Senden").apply {
            prefHeight = 30.0
            prefWidth = 75.0
        }

        val chooseSentHb = HBox().apply {
            padding = Insets(5.0, 0.0, 0.0, 0.0)
            with(children) {
                add(filePickerVb)
                add(sendDataBtn)
            }
        }
        val sendContentVb = VBox().apply {
            padding = Insets(0.0, 0.0, 0.0, 20.0)
            with(children) {
                add(sendVb)
                add(chosenFileLbl)
                add(chooseSentHb)
            }
        }

        val dataTransferContentVb = VBox().apply {
            padding = Insets(10.0, 0.0, 0.0, 10.0)
            with(children) {
                add(dataTransferLbl)
                add(receiveContentVb)
                add(sendContentVb)
            }
        }

        val allContentDividerHb = HBox().apply {
            with(children) {
                add(chatContentVb)
                add(dataTransferContentVb)
            }
        }

        val allContentVb = VBox().apply {
            padding = Insets(5.0)
            with(children) {
                add(currentUserLbl)
                add(allContentDividerHb)
            }
        }
        return allContentVb
    }

    fun setCurrentUserLbl(newCurrentUser: String) {
        currentUserLbl.text = newCurrentUser
    }

    fun getCurrentUserChat(user: String) {
    }

    fun setCurrentUser(newCurrentUser: String) {
        currentUser = newCurrentUser
    }

    fun addMessageToContact(jsonObj: JSONObject) {
        println("ReceiverName: ${jsonObj.get("receiverName")}")
        for (contact in contacts) {
            println("Available Contacts: ${contact.getName()}")
            if (contact.getName() == jsonObj.get("receiverName").toString()) {
                contact.addMessage(jsonObj.get("receiverName").toString(), getTimeStamp(), jsonObj.get("message").toString())
            }
        }
    }

    fun updateGuiMessagesFromContact(username: String) {
        for (contact in contacts) {
            if (contact.getName() == username) {
                var outputText = ""
                for (message in contact.getMessages()) {
                    outputText = outputText + (
                            "${message.getMessage()[0]} - " +
                                    " ${message.getMessage()[1]}\n" +
                                    " ${message.getMessage()[2]}\n\n")
                }
                outputChatTa.text = outputText
            }
        }
    }

    fun getTimeStamp(): String {
        val currentInstant: Instant = Instant.now()
        val zoneId: ZoneId = ZoneId.of("Europe/Zurich")

        val zonedDateTime = currentInstant.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("ddMMyyyy HH:mm")

        val formattedDateTime = zonedDateTime.format(formatter)

        return formattedDateTime
    }
}