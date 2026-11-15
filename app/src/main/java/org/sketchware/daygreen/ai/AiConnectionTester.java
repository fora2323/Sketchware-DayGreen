package org.sketchware.daygreen.ai;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class AiConnectionTester {

    public interface Callback {
        void onResult(boolean success, @NonNull String message);
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private AiConnectionTester() {
    }

    public static void test(int providerIndex, @NonNull String apiKey, @NonNull Callback callback) {
        new Thread(() -> {
            Request request = buildRequest(providerIndex, apiKey);
            if (request == null) {
                callback.onResult(false, "Unknown provider");
                return;
            }
            try (Response response = CLIENT.newCall(request).execute()) {
                int code = response.code();
                if (code == 200) {
                    callback.onResult(true, "Connected");
                } else if (code == 401 || code == 403) {
                    callback.onResult(false, "Invalid API key (HTTP " + code + ")");
                } else {
                    callback.onResult(false, "Failed (HTTP " + code + ")");
                }
            } catch (IOException e) {
                callback.onResult(false, "Network error: " + e.getMessage());
            }
        }).start();
    }

    private static Request buildRequest(int providerIndex, String apiKey) {
        switch (providerIndex) {
            case 0:
                return new Request.Builder()
                        .url("https://api.openai.com/v1/models")
                        .header("Authorization", "Bearer " + apiKey)
                        .build();
            case 1:
                return new Request.Builder()
                        .url("https://api.anthropic.com/v1/models")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .build();
            case 2:
                return new Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models")
                        .header("x-goog-api-key", apiKey)
                        .build();
            case 3:
                return new Request.Builder()
                        .url("https://api.deepseek.com/models")
                        .header("Authorization", "Bearer " + apiKey)
                        .build();
            case 4:
                return new Request.Builder()
                        .url("https://openrouter.ai/api/v1/auth/key")
                        .header("Authorization", "Bearer " + apiKey)
                        .build();
            case 5:
                return new Request.Builder()
                        .url("https://api.moonshot.ai/v1/models")
                        .header("Authorization", "Bearer " + apiKey)
                        .build();
            default:
                return null;
        }
    }
}