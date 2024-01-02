package teamtalk.client.message

import java.io.File
import java.time.Instant

class FileMessage(senderName: String,
                  timeStamp: String,
                  private val fileName: String,
                  private val file: File,
                  private val fileBytes: Byte): Message(senderName, timeStamp) {
}