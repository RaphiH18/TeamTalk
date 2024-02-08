package teamtalk.server.handler.network

import org.json.JSONObject
import teamtalk.server.serverLogger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class ServerClient(private val socket: Socket) {

    private val output = DataOutputStream(socket.getOutputStream())
    private val input = DataInputStream(socket.getInputStream())

    fun getSocket() = socket

    fun getOutput() = output

    fun getInput() = input

    fun send(header: JSONObject, payloadBytes: ByteArray = byteArrayOf()) {
        val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
        output.writeInt(headerBytes.size)
        output.write(headerBytes)

        if (payloadBytes.isNotEmpty()) {
            output.write(payloadBytes)
        }

        serverLogger.debug("-> An Client gesendet (Header): $header")
    }

    fun sendHeader(header: JSONObject) {
        val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
        output.writeInt(headerBytes.size)
        output.write(headerBytes)

        serverLogger.debug("-> An Client gesendet: Nur Header ($header)")
    }

    fun sendPayload(payloadBytes: ByteArray = byteArrayOf()) {
        if (payloadBytes.isNotEmpty()) {
            output.write(payloadBytes)
            output.flush()
        }

        serverLogger.debug("-> An Client gesendet: Nur Daten mit der Grösse ${payloadBytes.size}")
    }
}