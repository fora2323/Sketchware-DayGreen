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

    /** Max size of raw source code proposed in one call. */
    private static final int MAX_SOURCE_CHARS = 8000;

    public static final class Proposal {
        public final String javaName;
        public final String eventKey;
        /** Non-null only for block proposals. */
        public final List<AiBlockWriter.Step> steps;
        /** Non-null only for raw source code proposals. */
        public final String sourceCode;
        /** Non-null only for layout XML proposals. */
        public final String layoutXml;
        public final String xmlName;
        /** Non-null only for single widget addition proposals. */
        public final String widgetType;
        public final String widgetId;
        public final String widgetParent;
        public final String widgetText;
        public final String widgetWidth;
        public final String widgetHeight;
        public final String widgetOrientation;

        public final String summary;

        private Proposal(String javaName, String eventKey, List<AiBlockWriter.Step> steps,
                         String sourceCode, String xmlName, String layoutXml,
                         String widgetType, String widgetId, String widgetParent, String widgetText,
                         String widgetWidth, String widgetHeight, String widgetOrientation,
                         String summary) {
            this.javaName = javaName;
            this.eventKey = eventKey;
            this.steps = steps;
            this.sourceCode = sourceCode;
            this.xmlName = xmlName;
            this.layoutXml = layoutXml;
            this.widgetType = widgetType;
            this.widgetId = widgetId;
            this.widgetParent = widgetParent;
            this.widgetText = widgetText;
            this.widgetWidth = widgetWidth;
            this.widgetHeight = widgetHeight;
            this.widgetOrientation = widgetOrientation;
            this.summary = summary;
        }

        static Proposal blocks(String javaName, String eventKey, List<AiBlockWriter.Step> steps, String summary) {
            return new Proposal(javaName, eventKey, steps, null, null, null,
                    null, null, null, null, null, null, null, summary);
        }

        static Proposal source(String javaName, String eventKey, String code) {
            int lines = code.split("\n", -1).length;
            String summary = "Raw source code (" + lines + " line(s)) for event " + eventKey;
            return new Proposal(javaName, eventKey, null, code, null, null,
                    null, null, null, null, null, null, null, summary);
        }

        static Proposal layout(String xmlName, String xmlContent) {
            int lines = xmlContent.split("\n", -1).length;
            String summary = "Layout XML (" + lines + " line(s)) for " + xmlName;
            return new Proposal(null, null, null, null, xmlName, xmlContent,
                    null, null, null, null, null, null, null, summary);
        }

        static Proposal widget(String xmlName, String widgetType, String id, String parentId,
                               String text, String width, String height, String orientation) {
            String summary = "Add widget " + widgetType + " (id: " + id + ") to layout " + xmlName;
            return new Proposal(null, null, null, null, xmlName, null,
                    widgetType, id, parentId, text, width, height, orientation, summary);
        }

        public boolean isSource() {
            return sourceCode != null;
        }

        public boolean isLayout() {
            return layoutXml != null;
        }

        public boolean isWidget() {
            return widgetType != null;
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

    private static final int MAX_ROUNDS = 12;
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private static final String DEFAULT_PROMPT =
            "You are an expert Android & Sketchware AI developer agent inside Sketchware DayGreen. "
                    + "Your job is to assist the user in building, designing, coding, and debugging their Android app.";

    private static final String RULES =
            "\n\nCapabilities & Rules:\n"
                    + "1. LOGIC BLOCKS:\n"
                    + "   - Call list_event_keys to see existing events with blocks.\n"
                    + "   - Call list_events for built-in Activity events.\n"
                    + "   - Call read_event_blocks to inspect an event's blocks before modifying.\n"
                    + "   - Call list_blocks to find exact opcodes and parameters. Never invent opcodes.\n"
                    + "   - Call append_blocks to add blocks to an event.\n"
                    + "   - If complex custom code is needed that has no block, use propose_source_code.\n"
                    + "2. LAYOUTS:\n"
                    + "   - Call read_layout to inspect the current layout views and XML.\n"
                    + "   - Call add_widget to quickly add a widget (Button, TextView, EditText, ImageView, LinearLayout, etc.).\n"
                    + "   - Call apply_layout_xml to generate or replace a complete Android XML layout.\n"
                    + "3. DIAGNOSTICS & BUG FIXING:\n"
                    + "   - Call get_compile_error to retrieve the last build/compilation error log from the compiler.\n"
                    + "   - Call analyze_code_bug to scan for inconsistencies (e.g., view ID mismatches between events and layout).\n"
                    + "   - When a bug or compile error is reported, read the error, pinpoint the exact cause, explain it clearly, and use the tools to fix it.\n"
                    + "4. User approval: all write operations (append_blocks, propose_source_code, add_widget, apply_layout_xml) require user confirmation before applying.\n"
                    + "5. Always provide friendly, concise, and helpful explanations.";

    private final AiSettingsStore store;
    private final String scId;
    private final String javaName;
    private final String xmlName;
    private final JSONArray messages = new JSONArray();

    public AiChatClient(Context context, String scId, String javaName) {
        this(context, scId, javaName, "main.xml");
    }

    public AiChatClient(Context context, String scId, String javaName, String xmlName) {
        this.store = new AiSettingsStore(context);
        this.scId = scId;
        this.javaName = javaName != null ? javaName : "MainActivity";
        this.xmlName = xmlName != null ? xmlName : "main.xml";
        try {
            messages.put(message("system", store.getSystemPrompt(DEFAULT_PROMPT) + RULES));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build system message", e);
        }
    }

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
                        result = "Tool " + name + " failed: " + e + ". Check the arguments and try again.";
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

                proposals.add(Proposal.blocks(javaName, eventKey, steps, summary.toString().trim()));
                return "Waiting for user approval. Do not assume the blocks were written.";
            }

            case "clear_event_blocks": {
                String eventKey = args.getString("event_key");
                return AiBlockWriter.clearEventBlocks(scId, javaName, eventKey).message;
            }

            case "create_more_block": {
                String mbName = args.getString("name");
                String spec = args.getString("spec");
                return AiBlockWriter.createMoreBlock(scId, javaName, mbName, spec).message;
            }

            case "propose_source_code": {
                String eventKey = args.optString("event_key", "");
                String code = args.optString("code", "");

                if (code.trim().isEmpty()) {
                    return "Code is empty. Nothing was proposed.";
                }
                if (code.length() > MAX_SOURCE_CHARS) {
                    return "Code is longer than " + MAX_SOURCE_CHARS + " characters. Split it or shorten it.";
                }
                if (!AiBlockWriter.isValidEventKey(scId, javaName, eventKey)) {
                    return "Event \"" + eventKey + "\" is not valid for this file. Call list_event_keys or list_events.";
                }

                proposals.add(Proposal.source(javaName, eventKey, code));
                return "Waiting for user approval. The user will see the full code. Do not assume it was written.";
            }

            case "read_layout": {
                String targetXml = args.optString("xml_name", xmlName);
                return AiLayoutHelper.readLayout(scId, targetXml);
            }

            case "add_widget": {
                String targetXml = args.optString("xml_name", xmlName);
                String widgetType = args.getString("widget_type");
                String id = args.optString("id", "");
                String parent = args.optString("parent", "root");
                String text = args.optString("text", "");
                String width = args.optString("width", "wrap_content");
                String height = args.optString("height", "wrap_content");
                String orientation = args.optString("orientation", "vertical");

                proposals.add(Proposal.widget(targetXml, widgetType, id, parent, text, width, height, orientation));
                return "Waiting for user approval to add " + widgetType + " (" + id + ") to " + targetXml + ".";
            }

            case "apply_layout_xml": {
                String targetXml = args.optString("xml_name", xmlName);
                String content = args.getString("xml_content");
                proposals.add(Proposal.layout(targetXml, content));
                return "Waiting for user approval to apply layout XML to " + targetXml + ".";
            }

            case "get_compile_error": {
                return AiDiagnosticHelper.getCompileError(scId);
            }

            case "analyze_code_bug": {
                return AiDiagnosticHelper.diagnoseProject(scId, javaName, xmlName);
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

        if (store.isToolEnabled("clear_event_blocks")) {
            JSONObject props = new JSONObject()
                    .put("event_key", new JSONObject()
                            .put("type", "string")
                            .put("description", "Event key to clear blocks from"));
            tools.put(tool("clear_event_blocks",
                    "Clear all blocks in a specific event.",
                    props,
                    new JSONArray().put("event_key")));
        }

        if (store.isToolEnabled("create_more_block")) {
            JSONObject props = new JSONObject()
                    .put("name", new JSONObject().put("type", "string").put("description", "Method name for the More Block"))
                    .put("spec", new JSONObject().put("type", "string").put("description", "Block spec, e.g. myMethod %s %d"));
            tools.put(tool("create_more_block",
                    "Create a new More Block (custom method) in this Java file.",
                    props,
                    new JSONArray().put("name").put("spec")));
        }

        if (store.isToolEnabled("propose_source_code")) {
            JSONObject props = new JSONObject()
                    .put("event_key", new JSONObject()
                            .put("type", "string")
                            .put("description", "Event key from list_event_keys or list_events"))
                    .put("code", new JSONObject()
                            .put("type", "string")
                            .put("description", "Raw Java source to insert."));
            tools.put(tool("propose_source_code",
                    "Propose raw source code for an event to insert via addSourceDirectly. The user must approve in full.",
                    props,
                    new JSONArray().put("event_key").put("code")));
        }

        if (store.isToolEnabled("read_layout")) {
            JSONObject props = new JSONObject()
                    .put("xml_name", new JSONObject()
                            .put("type", "string")
                            .put("description", "Layout name, e.g. main.xml"));
            tools.put(tool("read_layout",
                    "Read the views, widget hierarchy, and XML code of a layout screen.",
                    props,
                    new JSONArray()));
        }

        if (store.isToolEnabled("add_widget")) {
            JSONObject props = new JSONObject()
                    .put("xml_name", new JSONObject().put("type", "string").put("description", "Layout name, e.g. main.xml"))
                    .put("widget_type", new JSONObject().put("type", "string").put("description", "Button, TextView, EditText, ImageView, LinearLayout, etc."))
                    .put("id", new JSONObject().put("type", "string").put("description", "Unique view ID, e.g. btn_submit, tv_counter"))
                    .put("parent", new JSONObject().put("type", "string").put("description", "Parent view ID or 'root'"))
                    .put("text", new JSONObject().put("type", "string").put("description", "Text to display on the widget"))
                    .put("width", new JSONObject().put("type", "string").put("description", "match_parent, wrap_content, or integer dp"))
                    .put("height", new JSONObject().put("type", "string").put("description", "match_parent, wrap_content, or integer dp"))
                    .put("orientation", new JSONObject().put("type", "string").put("description", "For LinearLayout: vertical or horizontal"));
            tools.put(tool("add_widget",
                    "Add a new widget or layout container to the screen layout. User must approve.",
                    props,
                    new JSONArray().put("widget_type")));
        }

        if (store.isToolEnabled("apply_layout_xml")) {
            JSONObject props = new JSONObject()
                    .put("xml_name", new JSONObject().put("type", "string").put("description", "Layout name, e.g. main.xml"))
                    .put("xml_content", new JSONObject().put("type", "string").put("description", "Complete Android XML layout string"));
            tools.put(tool("apply_layout_xml",
                    "Replace or generate the entire layout using Android XML. User must approve.",
                    props,
                    new JSONArray().put("xml_content")));
        }

        if (store.isToolEnabled("get_compile_error")) {
            tools.put(tool("get_compile_error",
                    "Get the last compiler error log / crash log from AAPT, javac, D8, or kotlinc.",
                    new JSONObject(),
                    new JSONArray()));
        }

        if (store.isToolEnabled("analyze_code_bug")) {
            tools.put(tool("analyze_code_bug",
                    "Run automated diagnostic checks on the project for missing view IDs, syntax issues, or broken block logic.",
                    new JSONObject(),
                    new JSONArray()));
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
