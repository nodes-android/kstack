package dk.nodes.nstack.kotlin.models

import org.json.JSONObject
import java.util.*

data class Language(
    val id: Int,
    val name: String,
    val locale: Locale,
    val direction: String,
    val isDefault: Boolean,
    val isBestFit: Boolean
) {

    constructor(jsonObject: JSONObject) : this(
        id = jsonObject.getInt("id"),
        name = jsonObject.getString("name"),
        locale = try { // Locale.forLanguageTag() would be better
            Locale(
                jsonObject.getString("locale").split("-|_").first(),
                jsonObject.getString("locale").split("-|_").last()
            )
        } catch (e : NullPointerException) {
            Locale(
                    jsonObject.getString("locale")
            )
        },
        direction = jsonObject.getString("direction"),
        isDefault = jsonObject.getBoolean("is_default"),
        isBestFit = jsonObject.getBoolean("is_best_fit")
    )
}
