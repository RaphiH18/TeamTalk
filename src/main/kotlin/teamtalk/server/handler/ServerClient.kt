package teamtalk.server.handler

import org.json.JSONObject
import teamtalk.logger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class ServerClient(private val socket: Socket, private var username: String = "") {

    private val output = DataOutputStream(socket.getOutputStream())
    private val input = DataInputStream(socket.getInputStream())

    fun getSocket() = socket

    fun getOutput() = output

    fun getInput() = input

    fun getUsername() = username

    fun setUsername(username: String) {
        this.username = username
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