package teamtalk.message

import java.io.File
import java.time.Instant

class FileMessage(senderName: String,
                  receiverName: String,
                  timestamp: Instant,
                  private val file: File) : Message(senderName, receiverName, timestamp) {

    override fun getMessage() = file

}