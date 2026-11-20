package org.sketchware.daygreen.ai

import android.content.Context
import android.content.SharedPreferences

class AiSettingsStore(context: Context) {

    companion object {
        private const val PREFS = "ai_settings"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_MODEL = "model"
        private const val KEY_API_KEY = "api_key_"
        private const val KEY_VERIFIED = "verified_"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_PERMISSION = "permission"
        private const val KEY_TOOL = "tool_"
        private const val KEY_MODEL_ID = "model_id"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var providerIndex: Int
        get() = prefs.getInt(KEY_PROVIDER, 0)
        set(index) = prefs.edit().putInt(KEY_PROVIDER, index).apply()

    var modelIndex: Int
        get() = prefs.getInt(KEY_MODEL, 0)
        set(index) = prefs.edit().putInt(KEY_MODEL, index).apply()

    fun getApiKey(providerIndex: Int): String {
        return prefs.getString(KEY_API_KEY + providerIndex, "") ?: ""
    }

    fun setApiKey(providerIndex: Int, key: String) {
        prefs.edit().putString(KEY_API_KEY + providerIndex, key).apply()
    }

    fun isVerified(providerIndex: Int): Boolean {
        return prefs.getBoolean(KEY_VERIFIED + providerIndex, false)
    }

    fun setVerified(providerIndex: Int, verified: Boolean) {
        prefs.edit().putBoolean(KEY_VERIFIED + providerIndex, verified).apply()
    }

    fun getSystemPrompt(defaultPrompt: String): String {
        return prefs.getString(KEY_SYSTEM_PROMPT, defaultPrompt) ?: defaultPrompt
    }

    fun setSystemPrompt(prompt: String) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
    }

    var permission: Int
        get() = prefs.getInt(KEY_PERMISSION, 0)
        set(index) = prefs.edit().putInt(KEY_PERMISSION, index).apply()

    fun isToolEnabled(tool: String): Boolean {
        return prefs.getBoolean(KEY_TOOL + tool, true)
    }

    fun getModelId(fallback: String): String {
        return prefs.getString(KEY_MODEL_ID, fallback) ?: fallback
    }

    fun setModelId(id: String) {
        prefs.edit().putString(KEY_MODEL_ID, id).apply()
    }

    fun setToolEnabled(tool: String, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TOOL + tool, enabled).apply()
    }
}
