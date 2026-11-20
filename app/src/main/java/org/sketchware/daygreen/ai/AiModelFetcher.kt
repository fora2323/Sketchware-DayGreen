package org.sketchware.daygreen.ai

import org.json.JSONObject
import java.util.Locale
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

object AiModelFetcher {

    interface Callback {
        fun onResult(models: List<String>)
        fun onError(error: String)
    }

    private val HTTP = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @JvmStatic
    fun fetch(provider: AiProviders.Provider, apiKey: String?, callback: Callback) {
        Thread {
            if (provider.modelsUrl == null) {
                callback.onError("Provider ${provider.name} belum mendukung daftar model.")
                return@Thread
            }
            if (apiKey.isNullOrEmpty()) {
                callback.onError("API key belum diisi.")
                return@Thread
            }

            val request = Request.Builder()
                .url(provider.modelsUrl)
                .header("Authorization", "Bearer $apiKey")
                .build()

            try {
                HTTP.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        callback.onError("HTTP ${response.code}: $body")
                        return@Thread
                    }

                    val data = JSONObject(body).optJSONArray("data")
                    val ids = TreeSet<String>()
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            var id = data.getJSONObject(i).optString("id", "")
                            if (id.startsWith("models/")) {
                                id = id.substring("models/".length)
                            }
                            if (id.isNotEmpty() && isChatModel(id)) {
                                ids.add(id)
                            }
                        }
                    }
                    callback.onResult(ArrayList(ids))
                }
            } catch (e: Exception) {
                callback.onError(e.message ?: e.toString())
            }
        }.start()
    }

    private fun isChatModel(id: String): Boolean {
        val lower = id.lowercase(Locale.ROOT)
        return !(lower.contains("embed")
            || lower.contains("tts")
            || lower.contains("whisper")
            || lower.contains("transcribe")
            || lower.contains("dall-e")
            || lower.contains("image")
            || lower.contains("moderation")
            || lower.contains("audio")
            || lower.contains("realtime"))
    }
}
