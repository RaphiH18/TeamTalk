package teamtalk.client.handler

import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import org.json.JSONObject
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
import org.json.JSONArray
import java.io.IOException

class ClientHandler(private val client: ChatClient) {
    private lateinit var socket: Socket
    private lateinit var output: PrintWriter
    private lateinit var input: BufferedReader
    private val handlerScope = CoroutineScope(Dispatchers.IO)
    private val userList = ArrayList<String>()
    private var userListStatus = false
    private var connectStatus = false

    fun connect() {
        handlerScope.launch {
            while(true){
                try{
                    println("Versuche Verbindung zum Serverherzustellen..")
                    socket = Socket("localhost", 4444)
                    println("Verbindung erfolgreich hergestellt")
                    connectStatus = true
                    break
                } catch(e: IOException) {
                    println("Verbindung zum Server fehlgeschlagen")
                }
                delay(500)
            }
            output = PrintWriter(socket.getOutputStream())
            input = BufferedReader(InputStreamReader(socket.getInputStream()))


            //Initial HELLO - ReciveAvailableUsers
            send(getHelloString())
            var message = input.readLine()
            val users = JSONArray(message)
            for (user in users) {
                userList.add(user.toString())
            }
            userListStatus = true
        }
    }

    fun isUserListStatus(): Boolean{
        while(!getUserListStatus()){
            handlerScope.launch {
                delay(100)
            }
        }
        return getUserListStatus()
    }

    fun getConnectStatus(): Boolean{
        return connectStatus
    }
    fun getUserListStatus(): Boolean{
        return userListStatus
    }
    fun getUserList(): ArrayList<String> {
        return userList
    }

    fun createContactView(): Node {
        var contactData = observableArrayList("Lukas Ledergerber", "Yannick Meier", "Raphael Hegi")
        val contactList = ListView(contactData).apply {
            maxWidth = 200.0
            prefHeight = 600.0
        }
        return contactList
    }

    fun createChattingView(): Node {

        val currentUserLbl = Label("Lukas Ledergerber").apply {
            prefHeight = 50.0
            prefWidth = 580.0
            font = Font("Arial", 24.0)
            style = ("-fx-background-color: #aaaaaa;");
            alignment = Pos.CENTER
        }

        val outputChatTa = TextArea("Chatfenster...").apply {
            prefHeight = 300.0
            prefWidth = 280.0
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

    fun send(string: String) {
        if (this::output.isInitialized) {
            output.println(string)
            output.flush()
        } else {
            throw IllegalStateException("No connection - please establish connection first.")
        }
    }

    fun send(file: File) {

    }

    fun getHelloString(): String {
        val jsonObj = JSONObject()
        with(jsonObj) {
            put("type", "HELLO")
        }

        return jsonObj.toString()
    }
    fun getLoginString(): String {
        val jsonObj = JSONObject()
        with(jsonObj) {
            put("type", "LOGIN")
            put("uuid", client.getUUID())
            put("username", client.getUsername())
        }

        return jsonObj.toString()
    }

    fun getMessageString(message: String, receiverName: String): String {
        val jsonObj = JSONObject()
        with(jsonObj) {
            put("type", "MESSAGE")
            put("senderUUID", client.getUUID())
            put("senderName", client.getUsername())
            put("receiverName", receiverName)
            put("message", message)
        }

        return jsonObj.toString()
    }

    fun getByeString(): String {
        val jsonObj = JSONObject()
        with(jsonObj) {
            put("type", "BYE")
            put("uuid", client.getUUID())
            put("username", client.getUsername())
        }

        return jsonObj.toString()
    }
}