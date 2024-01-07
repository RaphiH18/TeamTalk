package teamtalk.client.messaging

import java.io.File
import java.time.Instant

class FileMessage(senderName: String,
                  timestamp: Instant,
                  private val fileName: String,
                  private val file: File,
                  private val fileBytes: Byte) : Message(senderName, timestamp) {

    override fun getMessage() = file

}