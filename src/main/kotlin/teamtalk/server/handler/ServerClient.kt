package teamtalk.server.handler

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
}