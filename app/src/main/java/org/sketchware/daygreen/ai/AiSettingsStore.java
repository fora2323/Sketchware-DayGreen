package org.sketchware.daygreen.ai;

import android.content.Context;
import android.content.SharedPreferences;

public final class AiSettingsStore {

    private static final String PREFS = "ai_settings";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_MODEL = "model";
    private static final String KEY_API_KEY = "api_key_";
    private static final String KEY_VERIFIED = "verified_";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";
    private static final String KEY_PERMISSION = "permission";
    private static final String KEY_TOOL = "tool_";
    private static final String KEY_MODEL_ID = "model_id";    

    private final SharedPreferences prefs;

    public AiSettingsStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getProviderIndex() {
        return prefs.getInt(KEY_PROVIDER, 0);
    }

    public void setProviderIndex(int index) {
        prefs.edit().putInt(KEY_PROVIDER, index).apply();
    }

    public int getModelIndex() {
        return prefs.getInt(KEY_MODEL, 0);
    }

    public void setModelIndex(int index) {
        prefs.edit().putInt(KEY_MODEL, index).apply();
    }

    public String getApiKey(int providerIndex) {
        return prefs.getString(KEY_API_KEY + providerIndex, "");
    }

    public void setApiKey(int providerIndex, String key) {
        prefs.edit().putString(KEY_API_KEY + providerIndex, key).apply();
    }

    public boolean isVerified(int providerIndex) {
        return prefs.getBoolean(KEY_VERIFIED + providerIndex, false);
    }

    public void setVerified(int providerIndex, boolean verified) {
        prefs.edit().putBoolean(KEY_VERIFIED + providerIndex, verified).apply();
    }

    public String getSystemPrompt(String defaultPrompt) {
        return prefs.getString(KEY_SYSTEM_PROMPT, defaultPrompt);
    }

    public void setSystemPrompt(String prompt) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply();
    }

    public int getPermission() {
        return prefs.getInt(KEY_PERMISSION, 0);
    }

    public void setPermission(int index) {
        prefs.edit().putInt(KEY_PERMISSION, index).apply();
    }

    public boolean isToolEnabled(String tool) {
        return prefs.getBoolean(KEY_TOOL + tool, true);
    }

    public String getModelId(String fallback) {
        return prefs.getString(KEY_MODEL_ID, fallback);
    }

    public void setModelId(String id) {
        prefs.edit().putString(KEY_MODEL_ID, id).apply();
    }    

    public void setToolEnabled(String tool, boolean enabled) {
        prefs.edit().putBoolean(KEY_TOOL + tool, enabled).apply();
    }
}