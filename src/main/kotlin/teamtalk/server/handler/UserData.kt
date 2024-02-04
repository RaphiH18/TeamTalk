package teamtalk.server.handler

data class UserData(
    val sentTextMessages: Int,
    val receivedTextMessages: Int,
    val sentFileMessages: Int,
    val receivedFileMessages: Int,
    val fillWordStats: Map<String, Int>,
    val triggerWordStats: List<Map<String, Int>>
)