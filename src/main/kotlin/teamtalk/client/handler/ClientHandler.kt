package teamtalk.client.handler

import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

class ClientHandler(private val client: ChatClient) {

    private lateinit var socket: Socket
    private lateinit var output: PrintWriter
    private lateinit var input: BufferedReader

    suspend fun connect() {
        coroutineScope {
            launch {
                socket = Socket("localhost", 4444)
                output = PrintWriter(socket.getOutputStream())
                input = BufferedReader(InputStreamReader(socket.getInputStream()))

                send(getHelloString())

                val receiveString = input.readLine()
                println(receiveString)
            }
        }
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

        val allContent = VBox().apply{
            padding = Insets(5.0)
        }

        val currentUserLbl = Label("Lukas Ledergerber").apply{
            prefHeight = 50.0
            prefWidth = 580.0
            font = Font("Arial", 24.0)
            style = ("-fx-background-color: #aaaaaa;");
            alignment= Pos.CENTER
        }

        val outputChatTa = TextArea("Chatfenster...").apply{
            prefHeight = 300.0
            prefWidth = 280.0
        }

        val inputChatTa = TextArea("Schreiben...").apply {
            prefHeight = 100.0
            prefWidth = 280.0
        }

        val inputChatBox = VBox().apply{
            padding = Insets(40.0, 0.0, 25.0, 0.0)
            children.add(inputChatTa)
        }

        val sendenBtn = Button("Senden").apply{
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            prefHeight = 30.0
            prefWidth = 280.0
        }

        val chatContent = VBox().apply{
            padding = Insets(10.0,0.0,10.0,0.0)
            with(children){
                add(outputChatTa)
                add(inputChatBox)
                add(sendenBtn)
            }
        }

        val datentransferLbl = Label("Datentransfer").apply {
            prefHeight = 25.0
            prefWidth = 290.0
            font = Font("Arial", 20.0)
            style = ("-fx-background-color: #C9C9C9;");
            alignment = Pos.CENTER
        }

        val empfangenLbl = Label("Empfangen").apply {
            prefWidth = 250.0
            font = Font("Arial", 18.0)
            style = ("-fx-background-color: #E8E8E8")
        }

        // Ergänzen dynamische Gruppe für Downloadable Files
        val testFile1Btn = Button("Meilensteintrendanalyse.xlsx").apply {
            prefWidth = 250.0
        }
        val testFile1Content = VBox().apply {
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

        val empfangenContent = VBox().apply {
            prefHeight = 390.0
            padding = Insets(5.0 ,0.0 ,0.0, 20.0)
            with(children) {
                add(empfangenLbl)
                add(testFile1Content)
                add(testFile2Content)
            }
        }

        val sendenLbl = Label("Senden").apply {
            prefWidth = 250.0
            font = Font("Arial", 18.0)
            style = ("-fx-background-color: #E8E8E8")
        }
        val sendenLblContent = VBox().apply {
            padding = Insets(0.0, 0.0, 5.0, 0.0)
            children.add(sendenLbl)
        }

        val chosenFileLbl = Label("Noch keine Datei ausgewählt").apply {
            prefWidth = 250.0
            alignment = Pos.CENTER
        }

        val filePickerBtn = Button("Datei auswählen").apply {
            prefHeight = 30.0
            prefWidth = 170.0
        }

        val filePicketContent = VBox().apply{
            padding = Insets(0.0, 5.0, 0.0, 0.0)
            children.add(filePickerBtn)
        }

        val sentBtn = Button("Senden").apply{
            prefHeight = 30.0
            prefWidth = 75.0
        }

        val chooseSentBtnContent = HBox().apply {
            padding = Insets(5.0, 0.0, 0.0, 0.0)
            with(children){
                add(filePicketContent)
                add(sentBtn)
            }
        }
        val sendenContent = VBox().apply {
            padding = Insets(0.0 ,0.0 ,0.0, 20.0)
            with(children){
                add(sendenLblContent)
                add(chosenFileLbl)
                add(chooseSentBtnContent)
            }
        }

        val dataTransferContent = VBox().apply {
            padding = Insets(10.0 ,0.0 ,0.0, 10.0)
            with(children) {
                add(datentransferLbl)
                add(empfangenContent)
                add(sendenContent)
            }
        }

        val allContentDevider = HBox().apply{
            with(children) {
                add(chatContent)
                add(dataTransferContent)
            }
        }

        with(allContent.children){
            add(currentUserLbl)
            add(allContentDevider)
        }

        return allContent
    }

    fun send(string: String) {
        if(this::output.isInitialized) {
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