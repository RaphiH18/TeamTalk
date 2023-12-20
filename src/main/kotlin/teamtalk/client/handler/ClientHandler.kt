package teamtalk.client.handler

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

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