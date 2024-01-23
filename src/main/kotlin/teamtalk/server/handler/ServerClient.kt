package teamtalk.server.handler

import org.json.JSONObject
import teamtalk.message.Message
import teamtalk.logger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.time.Instant

class ServerClient(private val socket: Socket, private var username: String = "") {

    private lateinit var loginTime: Instant

    private val output = DataOutputStream(socket.getOutputStream())
    private val input = DataInputStream(socket.getInputStream())

    val messages = mutableListOf<Message>()

    fun getSocket() = socket

    fun getOutput() = output

    fun getInput() = input

    fun getUsername() = username

    fun setUsername(username: String) {
        this.username = username
    }

    fun setLoginTime(timestamp: Instant) {
        loginTime = timestamp
    }

    fun isLoggedIn() = username != ""

    fun send(header: JSONObject, payloadBytes: ByteArray = byteArrayOf()) {
        val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
        output.writeInt(headerBytes.size)
        output.write(headerBytes)

        if (payloadBytes.isNotEmpty()) {
            output.write(payloadBytes)
        }

        logger.debug("-> An Client gesendet (Header): $header")
    }
}