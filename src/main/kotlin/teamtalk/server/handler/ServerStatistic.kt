package teamtalk.server.handler

import teamtalk.client.messaging.Message

class ServerStatistic(private val server: ChatServer) {

    val messages = mutableListOf<Message>()

}