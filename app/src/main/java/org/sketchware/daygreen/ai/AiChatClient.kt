package org.sketchware.daygreen.ai

import android.content.Context
import android.util.Log
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiChatClient(
    private val context: Context,
    private val scId: String,
    private val javaName: String,
    private val xmlName: String
) {

    private val store = AiSettingsStore(context)
    private val messages = JSONArray()

    @Volatile
    private var isCancelled = false
    @Volatile
    private var currentCall: Call? = null

    fun cancel() {
        isCancelled = true
        try {
            currentCall?.cancel()
        } catch (_: Exception) {}
    }

    class Proposal(
        val steps: List<AiBlockWriter.Step>?,
        val javaName: String?,
        val eventKey: String?,
        val sourceCode: String?,
        val xmlName: String?,
        val layoutXml: String?,
        val widgetType: String?,
        val widgetId: String?,
        val widgetParent: String?,
        val widgetText: String?,
        val widgetWidth: String?,
        val widgetHeight: String?,
        val widgetOrientation: String?,
        val summary: String
    ) {
        val isLayout: Boolean get() = layoutXml != null
        val isSource: Boolean get() = sourceCode != null
        val isWidget: Boolean get() = widgetType != null

        companion object {
            fun blocks(javaName: String, eventKey: String, steps: List<AiBlockWriter.Step>, summary: String): Proposal {
                return Proposal(steps, javaName, eventKey, null, null, null, null, null, null, null, null, null, null, summary)
            }

            fun source(javaName: String, eventKey: String, code: String): Proposal {
                val summary = "Write source code to event $eventKey in $javaName"
                return Proposal(null, javaName, eventKey, code, null, null, null, null, null, null, null, null, null, summary)
            }

            fun layout(xmlName: String, xmlContent: String): Proposal {
                val summary = "Apply layout XML to $xmlName"
                return Proposal(null, null, null, null, xmlName, xmlContent, null, null, null, null, null, null, null, summary)
            }

            fun widget(
                xmlName: String, widgetType: String, id: String, parentId: String,
                text: String, width: String, height: String, orientation: String
            ): Proposal {
                val summary = "Add widget $widgetType (id: $id) to layout $xmlName"
                return Proposal(null, null, null, null, xmlName, null, widgetType, id, parentId, text, width, height, orientation, summary)
            }
        }
    }

    interface Callback {
        fun onResult(reply: String, proposals: List<Proposal>)
        fun onError(error: String)
        fun onStatus(status: String)
        fun onThinking(text: String)
    }

    companion object {
        private const val TAG = "AiChatClient"
        private const val MAX_SOURCE_CHARS = 8000
        private const val MAX_ROUNDS = 12
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        private val HTTP = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()

        private const val DEFAULT_PROMPT =
            "You are an expert Android & Sketchware AI developer assistant inside Sketchware DayGreen. " +
            "Your task is to assist the user in designing layouts, writing logic code, adding blocks, and debugging project errors. " +
            "ALWAYS RESPOND IN CLEAR, CONCISE ENGLISH."

        private const val RULES =
            "\n\nAgent Rules & Capabilities:\n" +
            "1. LOGIC BLOCKS:\n" +
            "   - Call list_event_keys to inspect events that already have blocks.\n" +
            "   - Call list_events to see available Activity lifecycle events.\n" +
            "   - Call read_event_blocks to inspect existing blocks before modifying them.\n" +
            "   - Call list_blocks to search for valid opcodes and parameter types.\n" +
            "   - Call append_blocks to add blocks to an event.\n" +
            "   - If no suitable block exists, ask user permission to use 'addSourceDirectly' (propose_source_code).\n" +
            "2. LAYOUT:\n" +
            "   - Call read_layout to view the screen layout XML and widget hierarchy.\n" +
            "   - Call add_widget to insert widgets into the layout screen.\n" +
            "   - Call apply_layout_xml to create or replace the entire layout XML.\n" +
            "3. ERROR DIAGNOSIS & FIXING:\n" +
            "   - Call get_compile_error to read the latest compilation error log from the compiler.\n" +
            "   - Call analyze_code_bug to detect potential bugs between Java code and layout XML.\n" +
            "4. User approval: All modification actions (append_blocks, propose_source_code, add_widget, apply_layout_xml) require user confirmation.\n" +
            "5. Always respond in English."
    }

    init {
        try {
            messages.put(message("system", store.getSystemPrompt(DEFAULT_PROMPT) + RULES))
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to initialize system message", e)
        }
    }

    fun ask(userText: String, callback: Callback) {
        isCancelled = false
        Thread { run(userText, callback) }.start()
    }

    private fun run(userText: String, callback: Callback) {
        try {
            val providerIndex = store.providerIndex
            val provider = AiProviders.ALL[providerIndex]
            if (!provider.openAiCompatible) {
                callback.onError("Provider ${provider.name} is not supported for agent chat.")
                return
            }

            val apiKey = store.getApiKey(providerIndex)
            if (apiKey.isEmpty()) {
                callback.onError("API key not configured. Please set it in agent settings.")
                return
            }

            var model = store.getModelId("")
            if (model.isEmpty()) {
                model = provider.models[0]
            }
            messages.put(message("user", userText))

            val proposals = mutableListOf<Proposal>()
            for (round in 0 until MAX_ROUNDS) {
                if (isCancelled) {
                    callback.onError("Request canceled.")
                    return
                }

                callback.onStatus(if (round == 0) "Thinking..." else "Continuing...")

                val reply = post(provider, apiKey, model)
                if (isCancelled) {
                    callback.onError("Request canceled.")
                    return
                }

                val assistant = reply.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")

                val reasoning = assistant.optString("reasoning_content", "")
                assistant.remove("reasoning_content")
                if (reasoning.isNotEmpty()) {
                    callback.onThinking(reasoning)
                }
                messages.put(assistant)

                val calls = assistant.optJSONArray("tool_calls")
                if (calls == null || calls.length() == 0) {
                    val content = if (assistant.isNull("content")) "" else assistant.optString("content", "")
                    callback.onResult(content, proposals)
                    return
                }

                for (i in 0 until calls.length()) {
                    if (isCancelled) {
                        callback.onError("Request canceled.")
                        return
                    }

                    val call = calls.getJSONObject(i)
                    val fn = call.getJSONObject("function")
                    val name = fn.getString("name")
                    callback.onStatus("Running $name...")

                    val result = try {
                        execute(name, JSONObject(fn.getString("arguments")), proposals)
                    } catch (e: Exception) {
                        Log.e(TAG, "Tool '$name' failed", e)
                        "Tool $name failed: ${e.message}. Check arguments and try again."
                    }
                    messages.put(toolMessage(call.getString("id"), result))
                }
            }
            callback.onResult("(Step limit reached. Please try asking a more specific question.)", proposals)
        } catch (e: Exception) {
            if (isCancelled) {
                callback.onError("Request canceled.")
                return
            }
            Log.e(TAG, "Chat request failed", e)
            callback.onError(e.message ?: e.toString())
        }
    }

    private fun execute(name: String, args: JSONObject, proposals: MutableList<Proposal>): String {
        if (!store.isToolEnabled(name)) {
            return disabled(name)
        }

        return when (name) {
            "list_blocks" -> {
                val out = JSONArray()
                for (entry in AiBlockCatalog.search(args.optString("query", ""), 40)) {
                    val o = JSONObject().apply {
                        put("op", entry.opCode)
                        put("spec", entry.spec)
                        put("type", entry.type)
                        put("params", JSONArray(entry.paramTypes))
                    }
                    out.put(o)
                }
                if (out.length() == 0) "No blocks match the query." else out.toString()
            }

            "list_event_keys" -> {
                if (scId.isEmpty()) {
                    "Project ID is missing."
                } else {
                    val keys = AiBlockWriter.listEventKeys(scId, javaName)
                    if (keys.isEmpty()) "No events with blocks in $javaName." else JSONArray(keys).toString()
                }
            }

            "list_events" -> {
                val out = JSONArray()
                for (e in AiEventCatalog.activityEvents()) {
                    out.put(JSONObject().put("name", e.name).put("key", e.key))
                }
                out.toString()
            }

            "read_event_blocks" -> {
                if (scId.isEmpty()) {
                    "Project ID is missing."
                } else {
                    val eventKey = args.optString("event_key", "")
                    if (eventKey.isEmpty()) "event_key is required." else AiBlockWriter.readEventBlocks(scId, javaName, eventKey)
                }
            }

            "append_blocks" -> {
                val eventKey = args.getString("event_key")
                val stepsJson = args.getJSONArray("steps")
                val steps = mutableListOf<AiBlockWriter.Step>()
                val summary = StringBuilder()

                for (i in 0 until stepsJson.length()) {
                    val s = stepsJson.getJSONObject(i)
                    val params = mutableListOf<String>()
                    val p = s.optJSONArray("params")
                    if (p != null) {
                        for (j in 0 until p.length()) {
                            params.add(p.optString(j, ""))
                        }
                    }
                    val op = s.getString("op")
                    steps.add(AiBlockWriter.Step(op, params))
                    summary.append(i + 1).append(". ").append(op).append(params).append('\n')
                }

                proposals.add(Proposal.blocks(javaName, eventKey, steps, summary.toString().trim()))
                "Pending user approval."
            }

            "clear_event_blocks" -> {
                val eventKey = args.getString("event_key")
                AiBlockWriter.clearEventBlocks(scId, javaName, eventKey).message
            }

            "create_more_block" -> {
                val mbName = args.getString("name")
                val spec = args.getString("spec")
                AiBlockWriter.createMoreBlock(scId, javaName, mbName, spec).message
            }

            "propose_source_code" -> {
                val eventKey = args.optString("event_key", "")
                val code = args.optString("code", "")
                if (code.trim().isEmpty()) return "Code is empty."
                if (code.length > MAX_SOURCE_CHARS) return "Code too long (max $MAX_SOURCE_CHARS characters)."
                if (!AiBlockWriter.isValidEventKey(scId, javaName, eventKey)) return "Event '$eventKey' is invalid."

                proposals.add(Proposal.source(javaName, eventKey, code))
                "Pending user approval to insert source code."
            }

            "read_layout" -> {
                val targetXml = args.optString("xml_name", xmlName)
                AiLayoutHelper.readLayout(scId, targetXml)
            }

            "add_widget" -> {
                val targetXml = args.optString("xml_name", xmlName)
                val widgetType = args.getString("widget_type")
                val id = args.optString("id", "")
                val parent = args.optString("parent", "root")
                val text = args.optString("text", "")
                val width = args.optString("width", "wrap_content")
                val height = args.optString("height", "wrap_content")
                val orientation = args.optString("orientation", "vertical")

                proposals.add(Proposal.widget(targetXml, widgetType, id, parent, text, width, height, orientation))
                "Pending user approval to add widget $widgetType ($id)."
            }

            "apply_layout_xml" -> {
                val targetXml = args.optString("xml_name", xmlName)
                val content = args.getString("xml_content")
                proposals.add(Proposal.layout(targetXml, content))
                "Pending user approval to apply layout XML to $targetXml."
            }

            "get_compile_error" -> {
                AiDiagnosticHelper.getCompileError(scId)
            }

            "analyze_code_bug" -> {
                AiDiagnosticHelper.diagnoseProject(scId, javaName, xmlName)
            }

            else -> "Unknown tool: $name"
        }
    }

    private fun disabled(tool: String): String {
        return "Tool $tool is disabled in agent settings."
    }

    private fun post(provider: AiProviders.Provider, apiKey: String, model: String): JSONObject {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            val schemas = toolSchemas()
            if (schemas.length() > 0) {
                put("tools", schemas)
                put("tool_choice", "auto")
            }
        }

        val request = Request.Builder()
            .url(provider.baseUrl ?: "")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()

        val call = HTTP.newCall(request)
        currentCall = call
        try {
            call.execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: $text")
                }
                return JSONObject(text)
            }
        } finally {
            currentCall = null
        }
    }

    private fun toolSchemas(): JSONArray {
        val tools = JSONArray()

        if (store.isToolEnabled("list_blocks")) {
            val props = JSONObject().put("query", JSONObject().put("type", "string").put("description", "Opcode keyword or block text"))
            tools.put(tool("list_blocks", "Search available blocks.", props, JSONArray()))
        }

        if (store.isToolEnabled("list_event_keys")) {
            tools.put(tool("list_event_keys", "List events that contain blocks.", JSONObject(), JSONArray()))
        }

        if (store.isToolEnabled("list_events")) {
            tools.put(tool("list_events", "List standard Activity lifecycle events.", JSONObject(), JSONArray()))
        }

        if (store.isToolEnabled("read_event_blocks")) {
            val props = JSONObject().put("event_key", JSONObject().put("type", "string").put("description", "Event key"))
            tools.put(tool("read_event_blocks", "Read blocks within an event.", props, JSONArray().put("event_key")))
        }

        if (store.isToolEnabled("append_blocks")) {
            val stepItem = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put("op", JSONObject().put("type", "string")).put("params", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string"))))
                .put("required", JSONArray().put("op").put("params"))

            val props = JSONObject().put("event_key", JSONObject().put("type", "string")).put("steps", JSONObject().put("type", "array").put("items", stepItem))
            tools.put(tool("append_blocks", "Add blocks to an event.", props, JSONArray().put("event_key").put("steps")))
        }

        if (store.isToolEnabled("clear_event_blocks")) {
            val props = JSONObject().put("event_key", JSONObject().put("type", "string").put("description", "Event key"))
            tools.put(tool("clear_event_blocks", "Clear all blocks in an event.", props, JSONArray().put("event_key")))
        }

        if (store.isToolEnabled("create_more_block")) {
            val props = JSONObject().put("name", JSONObject().put("type", "string")).put("spec", JSONObject().put("type", "string"))
            tools.put(tool("create_more_block", "Create a new More Block.", props, JSONArray().put("name").put("spec")))
        }

        if (store.isToolEnabled("propose_source_code")) {
            val props = JSONObject().put("event_key", JSONObject().put("type", "string")).put("code", JSONObject().put("type", "string"))
            tools.put(tool("propose_source_code", "Propose direct source code (addSourceDirectly).", props, JSONArray().put("event_key").put("code")))
        }

        if (store.isToolEnabled("read_layout")) {
            val props = JSONObject().put("xml_name", JSONObject().put("type", "string"))
            tools.put(tool("read_layout", "Read widget hierarchy and layout XML.", props, JSONArray()))
        }

        if (store.isToolEnabled("add_widget")) {
            val props = JSONObject()
                .put("xml_name", JSONObject().put("type", "string"))
                .put("widget_type", JSONObject().put("type", "string"))
                .put("id", JSONObject().put("type", "string"))
                .put("parent", JSONObject().put("type", "string"))
                .put("text", JSONObject().put("type", "string"))
                .put("width", JSONObject().put("type", "string"))
                .put("height", JSONObject().put("type", "string"))
                .put("orientation", JSONObject().put("type", "string"))
            tools.put(tool("add_widget", "Add a new widget to layout.", props, JSONArray().put("widget_type")))
        }

        if (store.isToolEnabled("apply_layout_xml")) {
            val props = JSONObject().put("xml_name", JSONObject().put("type", "string")).put("xml_content", JSONObject().put("type", "string"))
            tools.put(tool("apply_layout_xml", "Replace or create layout XML.", props, JSONArray().put("xml_content")))
        }

        if (store.isToolEnabled("get_compile_error")) {
            tools.put(tool("get_compile_error", "Get the latest compile error log.", JSONObject(), JSONArray()))
        }

        if (store.isToolEnabled("analyze_code_bug")) {
            tools.put(tool("analyze_code_bug", "Analyze potential bugs in project.", JSONObject(), JSONArray()))
        }

        return tools
    }

    private fun tool(name: String, description: String, properties: JSONObject, required: JSONArray): JSONObject {
        val params = JSONObject().put("type", "object").put("properties", properties).put("required", required)
        val fn = JSONObject().put("name", name).put("description", description).put("parameters", params)
        return JSONObject().put("type", "function").put("function", fn)
    }

    private fun message(role: String, content: String): JSONObject {
        return JSONObject().put("role", role).put("content", content)
    }

    private fun toolMessage(id: String, content: String): JSONObject {
        return JSONObject().put("role", "tool").put("tool_call_id", id).put("content", content)
    }
}
