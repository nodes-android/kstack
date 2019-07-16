package dk.nodes.nstack.kotlin.managers

import dk.nodes.nstack.kotlin.util.Preferences
import dk.nodes.nstack.kotlin.util.asJsonObject
import dk.nodes.nstack.kotlin.util.formatted
import dk.nodes.nstack.kotlin.util.iso8601Date
import dk.nodes.nstack.models.Constants
import org.json.JSONObject
import java.util.Date
import java.util.Locale

/**
 * Manages last update date and translations
 */
class PrefManager(private val preferences: Preferences) {

    private fun setLastUpdateDate() {
        val updateString = Date().formatted
        preferences.saveString(Constants.spk_nstack_network_update_time, updateString)
    }

    fun getLastUpdateDate(): Date? {
        val updateString = preferences.loadString(Constants.spk_nstack_network_update_time)

        return if (updateString.isEmpty()) {
            null
        } else {
            updateString.iso8601Date
        }
    }

    fun setTranslations(locale: Locale, translations: String) {
        setLastUpdateDate()
        preferences.saveString("${Constants.spk_nstack_language_cache}_$locale", translations)
    }

    fun getTranslations(): Map<Locale, JSONObject> {
        return preferences
            .loadStringsWhereKey { it.startsWith(Constants.spk_nstack_languages_cache) }
            .mapKeys { it.key.localeFromKey }
            .mapValues { it.value.asJsonObject ?: JSONObject() }
    }

    private val String.localeFromKey: Locale
        get() {
            val pattern = "${Constants.spk_nstack_language_cache}_(\\w\\w[\\-_]\\w\\w)".toRegex()
            val result = pattern.find(this)!!
            return Locale(result.groupValues[1])
        }
}
