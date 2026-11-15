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
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private static final String DEFAULT_PROMPT =
            "You are an AI assistant for Sketchware DayGreen. Be concise and precise.";

    private static final String RULES =
            "\n\nBlock rules:\n"
                    + "1. Call list_event_keys first to choose a valid event.\n"
                    + "2. Call list_blocks to find opcodes. Never invent opcodes.\n"
                    + "3. The number of params must match the params listed by list_blocks.\n"
                    + "4. Use append_blocks to add blocks. The user must approve before anything is written.\n"
                    + "5. Do not say blocks were written until the user approves.";

    private final AiSettingsStore store;
    private final String scId;
    private final String javaName;
    private final JSONArray messages = new JSONArray();

    public AiChatClient(Context context, String scId, String javaName) {
        this.store = new AiSettingsStore(context);
        this.scId = scId;
        this.javaName = javaName;
        try {
            messages.put(message("system", store.getSystemPrompt(DEFAULT_PROMPT) + RULES));
        } catch (JSONException ignored) {
        }
    }

    /**
     * Runs on a background thread. The callback is also called from that thread,
     * so the caller must switch to the main thread.
     */
    public void ask(String userText, Callback callback) {
        new Thread(() -> run(userText, callback)).start();
    }

    private void run(String userText, Callback callback) {
        try {
            int providerIndex = store.getProviderIndex();
            AiProviders.Provider provider = AiProviders.ALL[providerIndex];
            if (!provider.openAiCompatible) {
                callback.onError("Provider " + provider.name + " is not supported in agent chat yet. "
                        + "Choose OpenAI, DeepSeek, OpenRouter, or Kimi.");
                return;
            }

            String apiKey = store.getApiKey(providerIndex);
            if (apiKey.isEmpty()) {
                callback.onError("No API key set. Add one in the agent settings.");
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
            callback.onResult("(Tool call limit reached. Try a narrower question.)", proposals);
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
                    String op = s.getString("op");
                    steps.add(new AiBlockWriter.Step(op, params));
                    summary.append(i + 1).append(". ").append(op).append(params).append('\n');
                }

                proposals.add(new Proposal(javaName, eventKey, steps, summary.toString().trim()));
                return "Waiting for user approval. Do not assume the blocks were written.";
            }

            default:
                return "Unknown tool: " + name;
        }
    }

    private static String disabled(String tool) {
        return "Tool " + tool + " is disabled in the agent settings. Ask the user to enable it first.";
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
                .post(RequestBody.create(body.toString(), JSON_TYPE))
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

        JSONObject listBlocksProps = new JSONObject()
                .put("query", new JSONObject()
                        .put("type", "string")
                        .put("description", "Keyword for opcode or block text"));
        tools.put(tool("list_blocks",
                "Search available blocks. Leave query empty for a general list.",
                listBlocksProps,
                new JSONArray()));

        tools.put(tool("list_event_keys",
                "List events that already have blocks in the active Java file.",
                new JSONObject(),
                new JSONArray()));

        JSONObject stepItem = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("op", new JSONObject().put("type", "string"))
                        .put("params", new JSONObject()
                                .put("type", "array")
                                .put("items", new JSONObject().put("type", "string"))))
                .put("required", new JSONArray().put("op").put("params"));

        JSONObject appendProps = new JSONObject()
                .put("event_key", new JSONObject().put("type", "string"))
                .put("steps", new JSONObject()
                        .put("type", "array")
                        .put("items", stepItem));
        tools.put(tool("append_blocks",
                "Propose blocks to add to the end of an event. The user must approve.",
                appendProps,
                new JSONArray().put("event_key").put("steps")));

        return tools;
    }

    private static JSONObject tool(String name, String description, JSONObject properties, JSONArray required)
            throws JSONException {
        JSONObject params = new JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", required);

        JSONObject fn = new JSONObject()
                .put("name", name)
                .put("description", description)
                .put("parameters", params);

        return new JSONObject()
                .put("type", "function")
                .put("function", fn);
    }

    private static JSONObject message(String role, String content) throws JSONException {
        return new JSONObject().put("role", role).put("content", content);
    }

    private static JSONObject toolMessage(String id, String content) throws JSONException {
        return new JSONObject()
                .put("role", "tool")
                .put("tool_call_id", id)
                .put("content", content);
    }
}