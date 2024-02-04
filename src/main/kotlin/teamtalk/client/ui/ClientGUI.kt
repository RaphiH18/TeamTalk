package teamtalk.client.ui

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.json.JSONObject
import teamtalk.client.handler.ChatClient
import teamtalk.client.handler.ClientHeader
import teamtalk.message.*
import java.awt.Desktop
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.system.exitProcess


class ClientGUI(private val chatClient: ChatClient) {

    val guiScope = CoroutineScope(Dispatchers.JavaFx)

    lateinit var primaryStage: Stage

    private val defaultIP = "127.0.0.1"
    private val defaultPort = "4444"

    private val connectBtn = Button("Verbinden")
    private val messageOutputLbl = Label("Bereit")

    private var contactList = ListView<String>()

    private var sendFileBtn = Button("Senden")
    private var chosenFileLbl = Label("Keine Datei zum Senden ausgewählt")
    private var receivedFilesVBox = VBox()
    private lateinit var fileToSend: File

    private val CHAT_EMPTY = "Wähle einen Kontakt aus."

    private var currentUserLbl = Label()
    private var currentUser = ""
        set(value) {
            field = value
            currentUserLbl.text = value
        }

    private val conversationTF = TextFlow()

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
        guiScope.launch {
            var userChoice: ChoiceDialog<String> = ChoiceDialog()
            while (userChoice.items.isEmpty()) {
                val usernameList = mutableListOf<String>()
                for (user in chatClient.getServerUsers()) {
                    if (user.isOnline().not()) {
                        usernameList.add(user.getUsername())
                    }
                }
                println(usernameList)
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
            if (contact.getUsername() != chatClient.getUsername()) { //Client soll sich selbst nicht als Kontakt in der Liste anzeigen
                if (contact.isOnline()) {
                    contactData.add(contact.getUsername())
                }
            }
        }

        guiScope.launch {
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
                if ((selectedContact != null)) {
                    if (selectedContact.getUsername() != currentUser) {
                        currentUser = selectedContact.getUsername()
                        updateMessages(selectedContact, "GUI_CLICK")
                    }
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

        conversationTF.apply {
            prefHeight = 300.0
            prefWidth = 280.0
            children.add(Text(CHAT_EMPTY))
        }

        val inputChatTa = TextArea("Schreiben...").apply {
            prefHeight = 100.0
            prefWidth = 280.0
        }

        val inputChatVb = VBox().apply {
            padding = Insets(40.0, 0.0, 25.0, 0.0)
            children.add(inputChatTa)
        }

        val sendChatBtn = Button("Senden").apply {
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            prefHeight = 30.0
            prefWidth = 280.0
            setOnAction {
                if (inputChatTa.text.isEmpty().not()) {
                    val messageBytes = inputChatTa.text.toByteArray(Charsets.UTF_8)

                    chatClient.getHandler().send(
                        ClientHeader.MESSAGE.toJSON(chatClient, currentUser, messageBytes.size.toLong()),
                        messageBytes
                    )

                    inputChatTa.clear()
                }
            }
        }

        val chatContentVb = VBox().apply {
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            with(children) {
                //add(outputChatTa)
                add(conversationTF)
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

        receivedFilesVBox = VBox().apply {
            prefHeight = 390.0
            padding = Insets(5.0, 0.0, 0.0, 20.0)
            with(children) {
                add(receiveLbl)
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

        chosenFileLbl.apply {
            prefWidth = 250.0
            alignment = Pos.CENTER
        }

        val filePickerBtn = Button("Datei auswählen").apply {
            prefHeight = 30.0
            prefWidth = 170.0
            setOnAction {
                val fileChooser = FileChooser()
                fileChooser.title = "Datei zum Versenden auswählen"
                fileToSend = fileChooser.showOpenDialog(primaryStage)
                if (fileToSend.exists()) {
                    text = fileToSend.name
                    chosenFileLbl.text = fileToSend.name
                    sendFileBtn.isDisable = false
                }
            }
        }

        val filePickerVb = VBox().apply {
            padding = Insets(0.0, 5.0, 0.0, 0.0)
            children.add(filePickerBtn)
        }

        //Button, um ein File zu senden
        sendFileBtn.apply {
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            prefHeight = 30.0
            prefWidth = 75.0
            isDisable = true
            setOnAction {
                if (currentUser != "") {
                    if (fileToSend.exists()) {
                        chatClient.getHandler().handlerScope.launch {
                            val messageHeader = ClientHeader.FILE.toJSON(chatClient, currentUser, fileToSend.length(), fileToSend.name)
                            chatClient.getHandler().sendHeader(messageHeader)

                            val fileInputStream = FileInputStream(fileToSend)
                            val bufferFileBytes = ByteArray(4 * 1024)

                            while (true) {
                                val amountBytesRead = fileInputStream.read(bufferFileBytes)
                                if (amountBytesRead == -1) {
                                    break
                                }
                                chatClient.getHandler().sendPayload(bufferFileBytes.copyOf(amountBytesRead))
                            }
                            fileInputStream.close()
                        }
                    }
                }

                isDisable = true
                filePickerBtn.text = "Datei auswählen"
                chosenFileLbl.text = "Noch keine Datei ausgewählt"
            }
        }

        val chooseSentHb = HBox().apply {
            padding = Insets(5.0, 0.0, 0.0, 0.0)
            with(children) {
                add(filePickerVb)
                add(sendFileBtn)
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
                add(receivedFilesVBox)
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

    /*
    TODO: BUG-Behebung
    User A sendet User B eine Nachricht.
    Bei User B ist User A NICHT ausgewählt -> kein Update der Nachricht bei User B. -> OK.
    User B wählt nun User A aus, User B sieht nun die gesendete Nachricht von User A -> OK.
    Wenn nun User B eine Antwort an User A schreibt, wird User B die erste Nachricht von User A zweimal angezeigt,
    dazu noch seine Antwort an User A. -> Das ist falsch.

    Es müsste die Nachricht von User A und die antwort darauf von User B bei User B nur einmal angezeigt werden.
     */

    fun updateMessages(contact: Contact, updateCause: String) {
        guiScope.launch {
            val messages = contact.getMessages()

            println("**********************")
            println("Updating GUI for contact: ${contact.getUsername()}")
            println("Currently selected chat: $currentUser")
            println("Updating reason: $updateCause")

            if (contact.getUsername() == currentUser) {
                when (updateCause) {
                    "GUI_CLICK" -> {
                        //outputChatTa.text = ""
                        conversationTF.children.clear()
                        receivedFilesVBox.children.clear()

                        for (message in messages) {
                            addMessage(message)
                        }

                        contact.clearNewMessagesQueue()
                    }

                    "NEW_MESSAGE" -> {
                        for (message in contact.getNewMessages()) {
                            addMessage(message)
                        }
                    }
                }
            }
        }
    }

    private fun addMessage(message: Message) {
        println("Adding message: $message")
        when (message) {
            is TextMessage -> {
                val infoText = Text("${message.getTimestamp().toFormattedString()} - ${message.getSenderName()}:\n").apply {
                    style = "-fx-font-weight: bold;"
                }
                val messageText = Text("${message.getMessage()}\n\n")
                conversationTF.children.addAll(infoText, messageText)
            }

            is FileMessage -> {
                addFileToGUI(message.getMessage())
            }
        }
    }

    private fun addFileToGUI(file: File) {
        guiScope.launch {
            val fileButton = Button(file.name).apply {
                prefWidth = 250.0
                setOnAction {
                    startSaveFileGUI(this, file)
                }
            }

            val fileVb = VBox().apply {
                padding = Insets(5.0, 0.0, 0.0, 0.0)
                children.add(fileButton)
            }

            receivedFilesVBox.children.add(fileVb)
        }
    }

    private fun startSaveFileGUI(button: Button, file: File) {
        val alert = Alert(AlertType.CONFIRMATION)
        alert.title = file.name
        alert.headerText = "Was möchten Sie tun?"

        val openButton = ButtonType("Öffnen")
        val saveAsButton = ButtonType("Speichern unter...")
        val cancelButton = ButtonType("Abbrechen", ButtonData.CANCEL_CLOSE)

        if (Desktop.isDesktopSupported()) {
            alert.buttonTypes.setAll(openButton, saveAsButton, cancelButton)
        } else {
            alert.buttonTypes.setAll(saveAsButton, cancelButton)
        }

        val result = alert.showAndWait()

        if (result.get() == openButton) {
            alert.close()
            Desktop.getDesktop().open(file)
        } else if (result.get() == saveAsButton) {
            alert.close()
            val fileChooser = FileChooser()
            val fileExtension = ExtensionFilter(file.extension.uppercase(Locale.getDefault()) + "-Datei", "*.${file.extension}")
            fileChooser.extensionFilters.add(fileExtension)

            val chosenFile = fileChooser.showSaveDialog(primaryStage)
            if (chosenFile != null) {
                file.renameTo(chosenFile)
                button.text = chosenFile.name
                button.setOnAction {
                    Desktop.getDesktop().open(chosenFile)
                }
            }
        } else {
            alert.close()
        }
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