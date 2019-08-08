package dk.nodes.nstack.kotlin.managers

import dk.nodes.nstack.kotlin.models.AppOpenResult
import dk.nodes.nstack.kotlin.models.AppOpenSettings
import dk.nodes.nstack.kotlin.models.AppUpdateData
import dk.nodes.nstack.kotlin.models.AppUpdateResponse
import dk.nodes.nstack.kotlin.models.Proposal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkManagerImpl(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val debugMode: Boolean
) :
    NetworkManager {

    private val json = Json(JsonConfiguration.Default)
    override fun loadTranslation(
        url: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        client.newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {

                         val translations =
                            json.parseJson(response.body()!!.string()).jsonObject["data"]
                        onSuccess(translations.toString())
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            })
    }

    override suspend fun loadTranslation(url: String): String? {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val responseBody = response.body()
        return when {
            response.isSuccessful && responseBody != null -> json.parseJson(responseBody.string()).jsonObject["data"]
            .toString()
            else -> null
        }
    }

    override fun postAppOpen(
        settings: AppOpenSettings,
        acceptLanguage: String,
        onSuccess: (AppUpdateData) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val formBuilder = FormBody.Builder()
            .add("guid", settings.guid)
            .add("version", settings.version)
            .add("old_version", settings.oldVersion)
            .add("platform", settings.platform)
            .add("last_updated", settings.lastUpdated.formatted)
            .add("dev", debugMode.toString())

        val request = Request.Builder()
            .url("${baseUrl}/api/v2/open")
            .header("Accept-Language", acceptLanguage)
            .post(formBuilder.build())
            .build()

        client
            .newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException) {
                    onError.invoke(e)
                }


                override fun onResponse(call: Call?, response: Response?) {
                    try {

                        val responseString = response?.body()?.string()!!
                        val appUpdate = json.parse(AppUpdateResponse.serializer(), responseString)
                        onSuccess(appUpdate.data)
                        print(appUpdate)
//                        onSuccess.invoke(appUpdate.data)
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            })
    }

    override suspend fun postAppOpen(
        settings: AppOpenSettings,
        acceptLanguage: String
    ): AppOpenResult {
        val formBuilder = FormBody.Builder()
            .add("guid", settings.guid)
            .add("version", settings.version)
            .add("old_version", settings.oldVersion)
            .add("platform", settings.platform)
            .add("last_updated", settings.lastUpdated.formatted)
            .add("dev", debugMode.toString())

        val request = Request.Builder()
            .url("${baseUrl}/api/v2/open")
            .header("Accept-Language", acceptLanguage)
            .post(formBuilder.build())
            .build()

        try {
            val response = client
                .newCall(request)
                .execute()

            val responseString = response?.body()?.string() ?: return AppOpenResult.Failure
//            val appUpdate =
//                AppUpdateResponse(responseString.asJsonObject ?: return AppOpenResult.Failure)
//            return AppOpenResult.Success(appUpdate)
            return AppOpenResult.Failure
        } catch (e: Exception) {
            return AppOpenResult.Failure
        }
    }

    /**
     * Notifies the backend that the message has been seen
     */
    override fun postMessageSeen(guid: String, messageId: Int) {
        val formBuilder = FormBody.Builder()
            .add("guid", guid)
            .add("message_id", messageId.toString())

        val request = Request.Builder()
            .url("${baseUrl}/api/v1/notify/messages/views")
            .post(formBuilder.build())
            .build()

        client
            .newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
//                    NLog.e(this, "Failure posting message seen", e)
                }

                override fun onResponse(call: Call, response: Response) {
//                    NLog.v(this, "Message seen")
                }
            })
    }

    /**
     * Notifies the backend that the rate reminder has been seen
     */
    override fun postRateReminderSeen(appOpenSettings: AppOpenSettings, rated: Boolean) {
        val answer = if (rated) "yes" else "no"

        val formBuilder = FormBody.Builder()
            .add("guid", appOpenSettings.guid)
            .add("platform", appOpenSettings.platform)
            .add("answer", answer)

        val request = Request.Builder()
            .url("${baseUrl}/api/v1/notify/rate_reminder/views")
            .post(formBuilder.build())
            .build()

        client
            .newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
//                    NLog.e(this, "Failure posting rate reminder seen", e)
                }

                override fun onResponse(call: Call, response: Response) {
//                    NLog.v(this, "Rate reminder seen")
                }
            })
    }

    /**
     * Get a Collection Response (as String) from NStack collections
     */
    override fun getResponse(
        slug: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/content/responses/$slug")
            .get()
            .build()

        client
            .newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
//                    NLog.e(this, "Failure getting slug: $slug", e)
                    onError.invoke(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body()
                    when {
                        response.isSuccessful && responseBody != null -> onSuccess.invoke(
                            responseBody.string()
                        )
                        else -> onError.invoke(RuntimeException("$slug returned: ${response.code()}"))
                    }
                }
            })
    }

    /**
     * Get a Collection Response (as String) synchronously in a coroutine from NStack collections
     */
    override suspend fun getResponseSync(slug: String): String? {
        val request = Request.Builder()
            .url("${baseUrl}/api/v1/content/responses/$slug")
            .get()
            .build()

        val response = client
            .newCall(request)
            .execute()
        val responseBody = response.body()

        return when {
            response.isSuccessful && responseBody != null -> responseBody.string()
            else -> null
        }
    }

    override fun postProposal(
        settings: AppOpenSettings,
        locale: String,
        key: String,
        section: String,
        newValue: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        val formBuilder = FormBody.Builder()
            .add("key", key)
            .add("section", section)
            .add("value", newValue)
            .add("locale", locale)
            .add("guid", settings.guid)
            .add("platform", "mobile")

        val request = Request.Builder()
            .url("${baseUrl}/api/v2/content/localize/proposals")
            .post(formBuilder.build())
            .build()

        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
//                    val jsonObject = response.body()?.string()?.asJsonObject
//                    if (jsonObject != null && jsonObject.has("data")) {
//                        onSuccess()
//                    } else {
                        onError(IOException())
//                    }
                }
            }
        )
    }

    override fun fetchProposals(
        onSuccess: (List<Proposal>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val request = Request.Builder()
            .url("${baseUrl}/api/v2/content/localize/proposals")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body()?.string()
                val proposals = mutableListOf<Proposal>()
//                proposals.parseFromString(responseString)
                onSuccess(proposals)
            }
        })
    }

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"
    }

    private val Date.formatted: String
        get() {
            return SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(this)
        }
}
