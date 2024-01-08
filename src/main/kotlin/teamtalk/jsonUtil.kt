package teamtalk

import org.json.JSONObject

object jsonUtil {

    fun isJSON(testString: String): Boolean {
        try {
            JSONObject(testString)
            return true
        } catch(e: Exception) {
            return false
        }
    }
}