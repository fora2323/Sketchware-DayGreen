package org.sketchware.daygreen.ai;

import android.content.Context;
import android.util.Log;

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

    private static final String TAG = "AiChatClient";

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

        /** Short progress line, e.g. "Thinking..." or "Running list_blocks...". */
        void onStatus(String status);

        /** Reasoning text returned by the model, when the provider sends it. */
        void onThinking(String text);
    }

    private static final int MAX_ROUNDS = 8;
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private static final String DEFAULT_PROMPT =
            "You are an AI assistant for Sketchware DayGreen. Be concise and precise.";

    private static final String RULES =
            "\n\nBlock rules:\n"
                    + "1. Call list_event_keys to see events that already have blocks. Call read_event_blocks "
                    + "to read an event before you add to it. Call list_events for built-in Activity events.\n"
                    + "2. Call list_blocks to find opcodes. Never invent opcodes.\n"
                    + "3. The number of params must match the params listed by list_blocks.\n"
                    + "4. Use append_blocks to add blocks. The user must approve before anything is written.\n"
                    + "5. Do not say blocks were written until the user approves.\n"
                    + "6. If a tool returns an error, read it, adjust, and try again or explain to the user.\n"
                    + "7. Only call tools that are listed to you. If a tool is missing, tell the user.";

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
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build system message", e);
        }
    }

    /**
     * Runs on a background thread. Every callback is also called from that thread,
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
                callback.onError("Provider " + provider.name + " is not supported in agent chat yet.");
                return;
            }

            String apiKey = store.getApiKey(providerIndex);
            if (apiKey.isEmpty()) {
                callback.onError("No API key set. Add one in the agent settings.");
                return;
            }

            String model = store.getModelId("");
            if (model.isEmpty()) {
                model = provider.models[0];
            }
            messages.put(message("user", userText));

            List<Proposal> proposals = new ArrayList<>();
            for (int round = 0; round < MAX_ROUNDS; round++) {
                callback.onStatus(round == 0 ? "Thinking..." : "Continuing...");

                JSONObject reply = post(provider, apiKey, model);
                JSONObject assistant = reply.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message");

                // Reasoning is shown to the user, but not sent back to the model in history.
                String reasoning = assistant.optString("reasoning_content", "");
                assistant.remove("reasoning_content");
                if (!reasoning.isEmpty()) {
                    callback.onThinking(reasoning);
                }
                messages.put(assistant);

                JSONArray calls = assistant.optJSONArray("tool_calls");
                if (calls == null || calls.length() == 0) {
                    String content = assistant.isNull("content") ? "" : assistant.optString("content", "");
                    callback.onResult(content, proposals);
                    return;
                }

                for (int i = 0; i < calls.length(); i++) {
                    JSONObject call = calls.getJSONObject(i);
                    JSONObject fn = call.getJSONObject("function");
                    String name = fn.getString("name");
                    callback.onStatus("Running " + name + "...");

                    String result;
                    try {
                        result = execute(name, new JSONObject(fn.getString("arguments")), proposals);
                    } catch (Exception e) {
                        Log.e(TAG, "Tool '" + name + "' failed", e);
                        result = "Tool " + name + " failed: " + e
                                + ". Check the arguments and try again.";
                    }
                    messages.put(toolMessage(call.getString("id"), result));
                }
            }
            callback.onResult("(Step limit reached. Try a narrower question.)", proposals);
        } catch (Exception e) {
            Log.e(TAG, "Chat request failed", e);
            callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private String execute(String name, JSONObject args, List<Proposal> proposals) throws JSONException {
        if (!store.isToolEnabled(name)) {
            return disabled(name);
        }

        switch (name) {
            case "list_blocks": {
                JSONArray out = new JSONArray();
                for (AiBlockCatalog.Entry e : AiBlockCatalog.search(args.optString("query", ""), 40)) {
                    JSONObject o = new JSONObject();
                    o.put("op", e.opCode);
                    o.put("spec", e.spec);
                    o.put("type", e.type);
                    o.put("params", new JSONArray(e.paramTypes));
                    out.put(o);
                }
                return out.length() == 0 ? "No blocks matched the query." : out.toString();
            }

            case "list_event_keys": {
                if (scId == null || scId.isEmpty()) {
                    return "Project id is missing. Ask the user to reopen the project.";
                }
                List<String> keys = AiBlockWriter.listEventKeys(scId, javaName);
                if (keys.isEmpty()) {
                    return "No events with blocks in " + javaName + ". Use list_events for built-in events.";
                }
                return new JSONArray(keys).toString();
            }

            case "list_events": {
                JSONArray out = new JSONArray();
                for (AiEventCatalog.Event e : AiEventCatalog.activityEvents()) {
                    out.put(new JSONObject().put("name", e.name).put("key", e.key));
                }
                return out.toString();
            }

            case "read_event_blocks": {
                if (scId == null || scId.isEmpty()) {
                    return "Project id is missing. Ask the user to reopen the project.";
                }
                String eventKey = args.optString("event_key", "");
                if (eventKey.isEmpty()) {
                    return "Missing event_key. Call list_event_keys first.";
                }
                return AiBlockWriter.readEventBlocks(scId, javaName, eventKey);
            }

            case "append_blocks": {
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

        JSONArray schemas = toolSchemas();
        if (schemas.length() > 0) {
            body.put("tools", schemas);
            body.put("tool_choice", "auto");
        }

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

    /**
     * Only tools that are enabled in the settings are sent to the model.
     */
    private JSONArray toolSchemas() throws JSONException {
        JSONArray tools = new JSONArray();

        if (store.isToolEnabled("list_blocks")) {
            JSONObject props = new JSONObject()
                    .put("query", new JSONObject()
                            .put("type", "string")
                            .put("description", "Keyword for opcode or block text"));
            tools.put(tool("list_blocks",
                    "Search available blocks. Leave query empty for a general list.",
                    props,
                    new JSONArray()));
        }

        if (store.isToolEnabled("list_event_keys")) {
            tools.put(tool("list_event_keys",
                    "List events that already have blocks in the active Java file.",
                    new JSONObject(),
                    new JSONArray()));
        }

        if (store.isToolEnabled("list_events")) {
            tools.put(tool("list_events",
                    "List the built-in Activity events and the key to use for each one.",
                    new JSONObject(),
                    new JSONArray()));
        }

        if (store.isToolEnabled("read_event_blocks")) {
            JSONObject props = new JSONObject()
                    .put("event_key", new JSONObject()
                            .put("type", "string")
                            .put("description", "Event key from list_event_keys or list_events"));
            tools.put(tool("read_event_blocks",
                    "Read the blocks in one event, in chain order. Use it before adding to an existing event.",
                    props,
                    new JSONArray().put("event_key")));
        }

        if (store.isToolEnabled("append_blocks")) {
            JSONObject stepItem = new JSONObject()
                    .put("type", "object")
                    .put("properties", new JSONObject()
                            .put("op", new JSONObject().put("type", "string"))
                            .put("params", new JSONObject()
                                    .put("type", "array")
                                    .put("items", new JSONObject().put("type", "string"))))
                    .put("required", new JSONArray().put("op").put("params"));

            JSONObject props = new JSONObject()
                    .put("event_key", new JSONObject().put("type", "string"))
                    .put("steps", new JSONObject()
                            .put("type", "array")
                            .put("items", stepItem));
            tools.put(tool("append_blocks",
                    "Propose blocks to add to the end of an event. The user must approve.",
                    props,
                    new JSONArray().put("event_key").put("steps")));
        }

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