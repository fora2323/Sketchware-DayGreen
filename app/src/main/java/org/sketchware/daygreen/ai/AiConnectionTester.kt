package org.sketchware.daygreen.ai

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object AiConnectionTester {

    interface Callback {
        fun onResult(success: Boolean, message: String)
    }

    private val CLIENT = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @JvmStatic
    fun test(providerIndex: Int, apiKey: String, callback: Callback) {
        Thread {
            val request = buildRequest(providerIndex, apiKey)
            if (request == null) {
                callback.onResult(false, "Provider tidak dikenal")
                return@Thread
            }
            try {
                CLIENT.newCall(request).execute().use { response ->
                    val code = response.code
                    if (code == 200) {
                        callback.onResult(true, "Terhubung")
                    } else if (code == 401 || code == 403) {
                        callback.onResult(false, "API key tidak valid (HTTP $code)")
                    } else {
                        callback.onResult(false, "Gagal (HTTP $code)")
                    }
                }
            } catch (e: IOException) {
                callback.onResult(false, "Kesalahan jaringan: ${e.message}")
            }
        }.start()
    }

    private fun buildRequest(providerIndex: Int, apiKey: String): Request? {
        return when (providerIndex) {
            0 -> Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            1 -> Request.Builder()
                .url("https://api.anthropic.com/v1/models")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .build()
            2 -> Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models")
                .header("x-goog-api-key", apiKey)
                .build()
            3 -> Request.Builder()
                .url("https://api.deepseek.com/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            4 -> Request.Builder()
                .url("https://openrouter.ai/api/v1/auth/key")
                .header("Authorization", "Bearer $apiKey")
                .build()
            5 -> Request.Builder()
                .url("https://api.moonshot.ai/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            else -> null
        }
    }
}
