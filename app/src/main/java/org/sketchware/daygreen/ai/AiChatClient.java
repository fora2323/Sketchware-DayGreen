package org.sketchware.daygreen.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class AiChatClient {

    public static final class Proposal {
        public final String javaName;
        public final String eventKey;
        public final List<AiBlockWriter.Step> steps;
        public final String summary;

        Proposal(String javaName, String eventKey, List<AiBlockWriter.Step> steps, String summary) {
            this.javaName = javaName;
            this.eventKey = eventKey;
            this.steps = steps;
            this.summary = summary;
        }
    }

    public interface Callback {
        void onResult(String reply, List<Proposal> proposals);

        void onError(String error);
    }

    private static final int MAX_ROUNDS = 4;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private static final String RULES =
            "\nAturan block:\n"
                    + "1. Panggil list_event_keys dulu untuk memilih event yang valid.\n"
                    + "2. Panggil list_blocks untuk mencari opcode. Jangan mengarang opcode.\n"
                    + "3. Jumlah params harus sama dengan params dari list_blocks.\n"
                    + "4. Gunakan append_blocks untuk menambah block. User harus menyetujui sebelum ditulis.\n"
                    + "5. Jangan bilang block sudah tertulis sebelum user menyetujui.\n";

    private final AiSettingsStore store;
    private final String scId;
    private final String javaName;
    private final JSONArray messages = new JSONArray();

    public AiChatClient(Context context, String scId, String javaName) {
        this.store = new AiSettingsStore(context);
        this.scId = scId;
        this.javaName = javaName;
        try {
            messages.put(message("system", store.getSystemPrompt(
                    "You are an AI assistant for Sketchware DayGreen. Be concise and precise.") + RULES));
        } catch (JSONException ignored) {
        }
    }

    /**
     * Jalan di background thread. Callback juga dipanggil dari background thread.
     */
    public void ask(String userText, Callback callback) {
        new Thread(() -> run(userText, callback)).start();
    }

    private void run(String userText, Callback callback) {
        try {
            int providerIndex = store.getProviderIndex();
            AiProviders.Provider provider = AiProviders.ALL[providerIndex];
            if (!provider.openAiCompatible) {
                callback.onError("Provider " + provider.name + " belum didukung di chat AI. "
                        + "Pilih OpenAI, DeepSeek, OpenRouter, atau Kimi.");
                return;
            }

            String apiKey = store.getApiKey(providerIndex);
            if (apiKey.isEmpty()) {
                callback.onError("API key belum diisi di pengaturan AI.");
                return;
            }

            String model = provider.models[store.getModelIndex()];
            messages.put(message("user", userText));

            List<Proposal> proposals = new ArrayList<>();
            for (int round = 0; round < MAX_ROUNDS; round++) {
                JSONObject reply = post(provider, apiKey, model);
                JSONObject assistant = reply.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message");
                messages.put(assistant);

                JSONArray calls = assistant.optJSONArray("tool_calls");
                if (calls == null || calls.length() == 0) {
                    callback.onResult(assistant.optString("content", ""), proposals);
                    return;
                }

                for (int i = 0; i < calls.length(); i++) {
                    JSONObject call = calls.getJSONObject(i);
                    JSONObject fn = call.getJSONObject("function");
                    String result = execute(
                            fn.getString("name"),
                            new JSONObject(fn.getString("arguments")),
                            proposals);
                    messages.put(toolMessage(call.getString("id"), result));
                }
            }
            callback.onResult("(Batas putaran tool tercapai. Coba pertanyaan yang lebih sempit.)", proposals);
        } catch (Exception e) {
            callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private String execute(String name, JSONObject args, List<Proposal> proposals) throws JSONException {
        switch (name) {
            case "list_blocks": {
                if (!store.isToolEnabled("list_blocks")) {
                    return disabled("list_blocks");
                }
                JSONArray out = new JSONArray();
                for (AiBlockCatalog.Entry e : AiBlockCatalog.search(args.optString("query", ""), 40)) {
                    JSONObject o = new JSONObject();
                    o.put("op", e.opCode);
                    o.put("spec", e.spec);
                    o.put("type", e.type);
                    o.put("params", new JSONArray(e.paramTypes));
                    out.put(o);
                }
                return out.toString();
            }

            case "list_event_keys": {
                if (!store.isToolEnabled("list_blocks")) {
                    return disabled("list_blocks");
                }
                return new JSONArray(AiBlockWriter.listEventKeys(scId, javaName)).toString();
            }

            case "append_blocks": {
                if (!store.isToolEnabled("append_blocks")) {
                    return disabled("append_blocks");
                }
                String eventKey = args.getString("event_key");
                JSONArray stepsJson = args.getJSONArray("steps");

                List<AiBlockWriter.Step> steps = new ArrayList<>();
                StringBuilder summary = new StringBuilder();
                for (int i = 0; i < stepsJson.length(); i++) {
                    JSONObject s = stepsJson.getJSONObject(i);
                    List<String> params = new ArrayList<>();
                    JSONArray p = s.optJSONArray("params");
                    if (p != null) {
                        for (int j = 0; j < p.length(); j++) {
                            params.add(p.optString(j, ""));
                        }
                    }
                    steps.add(new AiBlockWriter.Step(s.getString("op"), params));
                    summary.append(i + 1).append(". ").append(s.getString("op")).append(params).append('\n');
                }

                proposals.add(new Proposal(javaName, eventKey, steps, summary.toString().trim()));
                return "Menunggu persetujuan user. Jangan anggap sudah tertulis.";
            }

            default:
                return "Tool tidak dikenal: " + name;
        }
    }

    private static String disabled(String tool) {
        return "Tool " + tool + " dinonaktifkan user di Settings. Minta user mengaktifkannya dulu.";
    }

    private JSONObject post(AiProviders.Provider provider, String apiKey, String model)
            throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", toolSchemas());
        body.put("tool_choice", "auto");

        Request request = new Request.Builder()
                .url(provider.baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            ResponseBody rb = response.body();
            String text = rb == null ? "" : rb.string();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + text);
            }
            return new JSONObject(text);
        }
    }

    private static JSONArray toolSchemas() throws JSONException {
        JSONArray tools = new JSONArray();

        tools.put(tool("list_blocks", "Cari block yang tersedia. Kosongkan query untuk daftar umum.", props(prop("query", "string", "Kata kunci opcode atau teks block")), new String[0]));

        tools.put(tool("list_event_keys", "Daftar event yang sudah punya block di file Java aktif.", props(), new String[0]));

        JSONObject stepItem = new JSONObject();
        stepItem.put("type", "object");
        JSONObject stepProps = new JSONObject();
        stepProps.put("op", new JSONObject().put("type", "string"));
        stepProps.put("params", new JSONObject().put("type", "array").put("items", new JSONObject().put("type", "string")));
        stepItem.put("properties", stepProps);
        stepItem.put("required", new JSONArray().put("op").put("params"));

        JSONObject appendProps = new JSONObject();
        appendProps.put("event_key", new JSONObject().put("type", "string"));
        appendProps.put("steps", new JSONObject().put("type", "array").put("items", stepItem));
        tools.put(tool("append_blocks", "Usulkan block baru ditambahkan ke ekor event. User harus menyetujui.", appendProps, new String[]{"event_key", "steps"}));

        return tools;
    }

    private static JSONObject tool(String name, String description, JSONObject properties, String[] required) throws JSONException {
        JSONObject params = new JSONObject();
        params.put("type", "object");
        params.put("properties", properties);
        params.put("required", new JSONArray(required));

        JSONObject fn = new JSONObject();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", params);

        JSONObject tool = new JSONObject();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    private static JSONObject props(Object... pairs) throws JSONException {
        JSONObject o = new JSONObject();
        for (int i = 0; i < pairs.length; i += 2) {
            o.put((String) pairs[i], pairs[i + 1]);
        }
        return o;
    }

    private static Object[] prop(String name, String type, String desc) throws JSONException {
        return new Object[]{name, new JSONObject().put("type", type).put("description", desc)};
    }

    private static JSONObject message(String role, String content) throws JSONException {
        return new JSONObject().put("role", role).put("content", content);
    }

    private static JSONObject toolMessage(String id, String content) throws JSONException {
        return new JSONObject().put("role", "tool").put("tool_call_id", id).put("content", content);
    }
}