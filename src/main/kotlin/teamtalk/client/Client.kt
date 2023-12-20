package teamtalk.client

import teamtalk.client.handler.ChatClient

suspend fun main() {
    Thread.sleep(2000)

    val client = ChatClient()
    client.start()
}