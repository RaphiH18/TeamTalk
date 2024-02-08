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

    fun defaultFillWords(): List<String> {
        return listOf(
            "der",
            "die",
            "das",
            "und",
            "oder",
            "also",
            "quasi",
            "sozusagen",
            "eigentlich"
        )
    }

    fun defaultPositiveTriggerWords(): List<String> {
        return listOf(
            "gut",
            "ja",
            "super",
            "perfekt",
            "optimal",
            "prima"
        )
    }

    fun defaultNeutralTriggerWords(): List<String> {
        return listOf(
            "ok",
            "einverstanden",
            "passt"
        )
    }

    fun defaultNegativeTriggerWords(): List<String> {
        return listOf(
            "schlecht",
            "nein",
            "schade"
        )
    }

    fun defaultConfigFile(): YAMLConfiguration {
        val yamlConfig = YAMLConfiguration()
        yamlConfig.addProperty("server.ip", "127.0.0.1")
        yamlConfig.addProperty("server.port", 4444)
        yamlConfig.addProperty("server.debug", false)
        yamlConfig.addProperty("stats.fillwords", defaultFillWords())
        yamlConfig.addProperty("stats.triggerwords.positive", defaultPositiveTriggerWords())
        yamlConfig.addProperty("stats.triggerwords.neutral", defaultNeutralTriggerWords())
        yamlConfig.addProperty("stats.triggerwords.negative", defaultNegativeTriggerWords())
        return yamlConfig
    }
}