package dk.nodes.nstack.kotlin.managers

import android.content.Context
import dk.nodes.nstack.kotlin.NStack
import dk.nodes.nstack.kotlin.util.asJsonObject
import org.json.JSONObject
import java.io.IOException
import java.util.*


class AssetCacheManager(private val context: Context) {

    fun loadTranslations(): Map<Locale, JSONObject> {
        return context.resources.assets.list("")
            ?.filter { it.startsWith("translations") }
            ?.mapNotNull { loadTranslation(it) }
            ?.filter { it.translations != null }
            ?.sortedBy { it.index }
            ?.map { it.locale to it.translations!! }
            ?.toMap() ?: mapOf()
    }

    private fun getDefaultTranslation(): String {
        return try {
            context.resources.assets.open("defaultLanguage.txt").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            ""
        }
    }

    private fun loadTranslation(translationFile: String): Translation? {
        val defaultFileName = getDefaultTranslation()
        val pattern = "^translations_(\\d+)_(\\w{2}[-_]\\w{2})\\.json$".toRegex()
        val result = pattern.find(translationFile) ?: return null
        val groups = result.groupValues
        val index = groups[1]
        val locale = Locale(groups[2])
        val translations = try {
            val inputStream = context.resources.assets.open(translationFile)
            inputStream.bufferedReader().use { it.readText() }.asJsonObject
        } catch (e: Exception) {
            null
        }
        // setting up default language for fallback
        if (defaultFileName == translationFile) {
            NStack.defaultLanguage = locale
        }
        return Translation(index = index, locale = locale, translations = translations)
    }

    private data class Translation(
        val index: String,
        val locale: Locale,
        val translations: JSONObject?
    )
}
