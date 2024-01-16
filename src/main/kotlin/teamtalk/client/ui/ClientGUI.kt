package teamtalk.client.ui

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage
import kotlinx.coroutines.delay
import org.json.JSONObject
import teamtalk.client.handler.ChatClient
import teamtalk.client.handler.ClientHeader
import teamtalk.client.messaging.Contact
import teamtalk.client.messaging.FileMessage
import teamtalk.client.messaging.TextMessage
import teamtalk.client.messaging.toFormattedString
import teamtalk.logger.debug
import kotlin.system.exitProcess

class ClientGUI(private val chatClient: ChatClient) {

    private val defaultIP = "127.0.0.1"
    private val defaultPort = "4444"

    private val connectBtn = Button("Verbinden")
    private val messageOutputLbl = Label("Bereit")

    private var contactList = ListView<String>()

    private var currentUserLbl = Label()
    private var currentUser = ""
        set(value) {
            field = value
            currentUserLbl.text = value
        }

    private val outputChatTa = TextArea()
    private val sendChatBtn = Button("Senden")

    private fun createBaseView(): VBox {
        val vBoxBase = VBox()
        val vBoxContent = createContentView()
        val menuBar = createMenuBar()

        with(vBoxBase.children) {
            add(menuBar)
            add(vBoxContent)
        }

        return vBoxBase
    }

    private fun createContentView(): SplitPane {
        val splitPane = SplitPane()

        with(splitPane) {
            items.addAll(createContactView())
            items.addAll(createChattingView())
        }

        splitPane.setDividerPositions(0.3)
        return splitPane
    }

    suspend fun waitForConnected() {
        while (true) {
            if (!(chatClient.isConnected())) {
                messageOutputLbl.text = chatClient.getHandler().getStatusMessage()
                delay(100)
                if (chatClient.getHandler().getStatusMessage() == "Timeout") {
                    connectBtn.isDisable = false
                }
            } else {
                messageOutputLbl.text = chatClient.getHandler().getStatusMessage()
                break
            }
        }
    }

    fun startConnectionGUI(stage: Stage) {
        val serverLbl = Label("Server")
        val serverTf = TextField(defaultIP)

        val serverHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 50.0
            padding = Insets(50.0, 0.0, 10.0, 50.0)
            spacing = 10.0
            with(children) {
                add(serverLbl)
                add(serverTf)
            }
        }

        val messageLbl = Label("Status:")

        val messageHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 100.0
            padding = Insets(10.0, 0.0, 0.0, 50.0)
            spacing = 10.0
            with(children) {
                add(messageLbl)
                add(messageOutputLbl)
            }
        }

        val portLbl = Label("Port")
        val portTf = TextField(defaultPort).apply {
            maxWidth = 68.0
        }

        val portTfVb = VBox().apply {
            padding = Insets(0.0, 0.0, 0.0, 11.0)
            children.add(portTf)
        }

        connectBtn.apply {
            setOnAction {
                chatClient.start(serverTf.text, portTf.text.toInt())
                connectBtn.isDisable = true
            }
        }

        val portHb = HBox().apply {
            prefWidth = 200.0
            prefHeight = 50.0
            padding = Insets(0.0, 0.0, 10.0, 50.0)
            spacing = 10.0
            with(children) {
                add(portLbl)
                add(portTfVb)
                add(connectBtn)
            }
        }

        val connectionGUIVb = VBox().apply {
            prefWidth = 350.0
            prefHeight = 200.0
            with(children) {
                add(serverHb)
                add(portHb)
                add(messageHb)
            }
        }

        with(stage) {
            scene = Scene(connectionGUIVb)
            title = "TeamTalk Serverauswahl"
            setOnCloseRequest { exitProcess(0) }
            show()
        }
    }

    fun startUserselectionGUI(stage: Stage) {
        Platform.runLater {
            var userChoice: ChoiceDialog<String> = ChoiceDialog()
            while (userChoice.items.isEmpty()) {
                val usernameList = mutableListOf<String>()
                for (user in chatClient.getServerUsers()) {
                    if(user.isOnline().not()) {
                        usernameList.add(user.getUsername())
                    }
                }
                userChoice = ChoiceDialog(usernameList[0], usernameList)
            }
            with(userChoice) {
                setTitle("Benutzerauswahl")
                setHeaderText(null)
                setContentText("Benutzername:")
                dialogPane.lookupButton(ButtonType.CANCEL).setVisible(false)
            }

            val result = userChoice.showAndWait()
            result.ifPresent { selectedUsername ->
                chatClient.setUsername(selectedUsername)
                chatClient.getHandler().send(ClientHeader.LOGIN.toJSON(chatClient))
                startMainGUI(stage)
            }
        }
    }

    private fun startMainGUI(stage: Stage) {
        with(stage) {
            scene = Scene(createBaseView(), 800.0, 600.0)
            title = "TeamTalk Client - Angemeldet als: ${chatClient.getUsername()}"
            setOnCloseRequest { exitProcess(0) }
            show()
        }
    }

    fun updateContactStatus(onlineContacts: JSONObject) {
        val onlineContactsFormatted = onlineContacts.getJSONArray("onlineUserList")
        for (contact in chatClient.getHandler().getContacts()) {
            contact.setOnline(false)
            for (onlineContact in onlineContactsFormatted) {
                if (contact.getUsername() == onlineContact.toString()) {
                    contact.setOnline(true)
                }
            }
        }
    }

    fun updateContactView() {
        val contactData = FXCollections.observableArrayList<String>()
        for (contact in chatClient.getHandler().getContacts()) {
            if (contact.isOnline()) {
                contactData.add(contact.getUsername())
            }
        }
        Platform.runLater {
            contactList.items = contactData
        }
    }

    private fun createContactView(): Node {
        contactList.apply {
            minWidth = 200.0
            maxWidth = 200.0
            prefHeight = 600.0
            setOnMouseClicked { _ ->
                val selectedContact = chatClient.getHandler().getContacts().find { it.getUsername() == selectionModel.selectedItem }
                if (selectedContact != null) {
                    currentUser = selectedContact.getUsername()
                    updateGuiMessagesFromContact(selectedContact)
                }
            }
        }
        return contactList
    }

    private fun createChattingView(): Node {
        var defaultUser = ""
        for (user in chatClient.getHandler().getContacts()) {
            if (user.getUsername() != currentUser) {
                defaultUser = user.getUsername()
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
                    val messageBytes = inputChatTa.text.toByteArray(Charsets.UTF_8)

                    chatClient.getHandler().send(
                        ClientHeader.MESSAGE.toJSON(chatClient, currentUser, messageBytes.size),
                        messageBytes)

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

    fun updateGuiMessagesFromContact(contact: Contact) {
        debug("Updatig GUI for Contact: ${contact.getUsername()}")
        var outputText = ""
        for (message in contact.getMessages()) {
            when (message) {
                is TextMessage -> outputText += ("${message.getTimestamp().toFormattedString()} - ${message.getSenderName()}\n ${message.getMessage()}\n\n")
                is FileMessage -> return //TODO: File-Anzeige im GUI
            }
        }
        outputChatTa.text = outputText
    }

    fun getCurrentUserChat(user: String) {
    }

    fun createMenuBar() = bar(
        menu(
            "Datei",
            item("Schliessen", { System.exit(0) })
        )
    )

    private fun bar(vararg elements: Menu) = MenuBar().apply { getMenus().addAll(elements) }
    private fun menu(text: String, vararg elements: MenuItem) = Menu(text).apply { getItems().addAll(elements) }
    private fun item(text: String, method: () -> Unit) = MenuItem(text).apply { setOnAction { method() } }
    private fun separator() = SeparatorMenuItem()
}