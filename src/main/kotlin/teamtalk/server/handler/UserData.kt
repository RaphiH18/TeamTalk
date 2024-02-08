package teamtalk.server.handler

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val username: String,
    val sentTextMessages: Int,
    val receivedTextMessages: Int,
    val sentFileMessages: Int,
    val receivedFileMessages: Int,
    val usageTime: Long,
    val answerTime: Map<String, List<Long>>,
    val fillWordStats: Map<String, Int>,
    val triggerWordStats: List<Map<String, Int>>
)