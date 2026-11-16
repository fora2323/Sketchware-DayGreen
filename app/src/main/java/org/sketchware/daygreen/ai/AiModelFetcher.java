package org.sketchware.daygreen.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class AiModelFetcher {

    public interface Callback {
        void onResult(List<String> models);

        void onError(String error);
    }

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private AiModelFetcher() {
    }

    /**
     * Runs on a background thread. The callback is also called from that thread.
     */
    public static void fetch(AiProviders.Provider provider, String apiKey, Callback callback) {
        new Thread(() -> {
            if (provider.modelsUrl == null) {
                callback.onError("Provider " + provider.name + " has no model list yet.");
                return;
            }
            if (apiKey == null || apiKey.isEmpty()) {
                callback.onError("No API key set.");
                return;
            }

            Request request = new Request.Builder()
                    .url(provider.modelsUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = HTTP.newCall(request).execute()) {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }

                JSONArray data = new JSONObject(body).optJSONArray("data");
                TreeSet<String> ids = new TreeSet<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        String id = data.getJSONObject(i).optString("id", "");
                        if (id.startsWith("models/")) {
                            id = id.substring("models/".length());
                        }
                        if (!id.isEmpty() && isChatModel(id)) {
                            ids.add(id);
                        }
                    }
                }
                callback.onResult(new ArrayList<>(ids));
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }).start();
    }

    /**
     * Skips models that cannot be used for chat (embeddings, audio, image, moderation).
     */
    private static boolean isChatModel(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        return !(lower.contains("embed")
                || lower.contains("tts")
                || lower.contains("whisper")
                || lower.contains("transcribe")
                || lower.contains("dall-e")
                || lower.contains("image")
                || lower.contains("moderation")
                || lower.contains("audio")
                || lower.contains("realtime"));
    }
}