package teamtalk.server.handler

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ServerClient(private val socket: Socket, private val username: String) {

    private val output = PrintWriter(socket.getOutputStream())
    private val input = BufferedReader(InputStreamReader(socket.getInputStream()))

    fun getSocket() = socket

    fun getUsername() = username

    fun getOutput() = output

    fun getInput() = input
}