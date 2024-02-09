package teamtalk

import org.apache.commons.configuration2.YAMLConfiguration
import org.json.JSONObject

object utilities {

    fun isJSON(testString: String): Boolean {
        try {
            JSONObject(testString)
            return true
        } catch(e: Exception) {
            return false
        }
    }
}