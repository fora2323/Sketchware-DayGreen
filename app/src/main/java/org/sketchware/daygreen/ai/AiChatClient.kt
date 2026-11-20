package org.sketchware.daygreen.ai

import android.content.Context
import android.util.Log
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
    context: Context,
    private val scId: String,
    javaName: String?,
    xmlName: String? = "main.xml"
) {
    private val store: AiSettingsStore = AiSettingsStore(context)
    val javaName: String = javaName ?: "MainActivity"
    val xmlName: String = xmlName ?: "main.xml"
    private val messages = JSONArray()

    class Proposal private constructor(
        val javaName: String?,
        val eventKey: String?,
        val steps: List<AiBlockWriter.Step>?,
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
        val isSource: Boolean get() = sourceCode != null
        val isLayout: Boolean get() = layoutXml != null
        val isWidget: Boolean get() = widgetType != null

        companion object {
            fun blocks(javaName: String, eventKey: String, steps: List<AiBlockWriter.Step>, summary: String): Proposal {
                return Proposal(javaName, eventKey, steps, null, null, null, null, null, null, null, null, null, null, summary)
            }

            fun source(javaName: String, eventKey: String, code: String): Proposal {
                val lines = code.split("\n").size
                val summary = "Kode sumber langsung ($lines baris) untuk event $eventKey"
                return Proposal(javaName, eventKey, null, code, null, null, null, null, null, null, null, null, null, summary)
            }

            fun layout(xmlName: String, xmlContent: String): Proposal {
                val lines = xmlContent.split("\n").size
                val summary = "Layout XML ($lines baris) untuk $xmlName"
                return Proposal(null, null, null, null, xmlName, xmlContent, null, null, null, null, null, null, null, summary)
            }

            fun widget(
                xmlName: String, widgetType: String, id: String, parentId: String,
                text: String, width: String, height: String, orientation: String
            ): Proposal {
                val summary = "Tambah widget $widgetType (id: $id) ke layout $xmlName"
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
            "Anda adalah asisten pengembang aplikasi Android & Sketchware AI di dalam Sketchware DayGreen. " +
            "Tugas Anda adalah membantu pengguna membuat, merancang layout, menulis kode logic, dan memperbaiki error project. " +
            "SELALU JAWAB DALAM BAHASA INDONESIA YANG RAMAH DAN JELAS."

        private const val RULES =
            "\n\nAturan & Kemampuan Agent:\n" +
            "1. LOGIC BLOCKS:\n" +
            "   - Panggil list_event_keys untuk melihat event yang sudah ada block.\n" +
            "   - Panggil list_events untuk daftar event Activity bawaan.\n" +
            "   - Panggil read_event_blocks untuk membaca block dalam event sebelum mengubahnya.\n" +
            "   - Panggil list_blocks untuk mencari opcode dan parameter yang tepat.\n" +
            "   - Panggil append_blocks untuk menambahkan block ke suatu event.\n" +
            "   - Jika tidak ada block yang cocok, minta izin pengguna untuk menggunakan 'addSourceDirectly' (propose_source_code).\n" +
            "2. LAYOUT:\n" +
            "   - Panggil read_layout untuk melihat tampilan dan kode XML layout.\n" +
            "   - Panggil add_widget untuk menambahkan widget ke layout screen (main.xml).\n" +
            "   - Panggil apply_layout_xml untuk membuat atau memperbarui seluruh layout XML.\n" +
            "3. DIAGNOSIS ERROR:\n" +
            "   - Panggil get_compile_error untuk membaca log error compile terakhir dari compiler.\n" +
            "   - Panggil analyze_code_bug untuk mendeteksi potensi bug antara kode Java dan layout XML.\n" +
            "4. Persetujuan pengguna: Semua operasi penulisan (append_blocks, propose_source_code, add_widget, apply_layout_xml) membutuhkan konfirmasi pengguna.\n" +
            "5. Jawablah selalu dalam bahasa Indonesia."
    }

    init {
        try {
            messages.put(message("system", store.getSystemPrompt(DEFAULT_PROMPT) + RULES))
        } catch (e: JSONException) {
            Log.e(TAG, "Gagal membuat pesan sistem", e)
        }
    }

    fun ask(userText: String, callback: Callback) {
        Thread { run(userText, callback) }.start()
    }

    private fun run(userText: String, callback: Callback) {
        try {
            val providerIndex = store.providerIndex
            val provider = AiProviders.ALL[providerIndex]
            if (!provider.openAiCompatible) {
                callback.onError("Provider ${provider.name} belum didukung untuk chat agent.")
                return
            }

            val apiKey = store.getApiKey(providerIndex)
            if (apiKey.isEmpty()) {
                callback.onError("API key belum diatur. Silakan atur di pengaturan agent.")
                return
            }

            var model = store.getModelId("")
            if (model.isEmpty()) {
                model = provider.models[0]
            }
            messages.put(message("user", userText))

            val proposals = mutableListOf<Proposal>()
            for (round in 0 until MAX_ROUNDS) {
                callback.onStatus(if (round == 0) "Sedang berpikir..." else "Lanjut memproses...")

                val reply = post(provider, apiKey, model)
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
                    val call = calls.getJSONObject(i)
                    val fn = call.getJSONObject("function")
                    val name = fn.getString("name")
                    callback.onStatus("Running $name...")

                    val result = try {
                        execute(name, JSONObject(fn.getString("arguments")), proposals)
                    } catch (e: Exception) {
                        Log.e(TAG, "Tool '$name' gagal", e)
                        "Tool $name gagal: ${e.message}. Periksa argument dan coba lagi."
                    }
                    messages.put(toolMessage(call.getString("id"), result))
                }
            }
            callback.onResult("(Batas langkah tercapai. Coba ajukan pertanyaan lebih spesifik.)", proposals)
        } catch (e: Exception) {
            Log.e(TAG, "Permintaan chat gagal", e)
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
                if (out.length() == 0) "Tidak ada block yang cocok dengan kata kunci." else out.toString()
            }

            "list_event_keys" -> {
                if (scId.isEmpty()) {
                    "ID Project tidak ada."
                } else {
                    val keys = AiBlockWriter.listEventKeys(scId, javaName)
                    if (keys.isEmpty()) "Tidak ada event dengan block di $javaName." else JSONArray(keys).toString()
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
                    "ID Project tidak ada."
                } else {
                    val eventKey = args.optString("event_key", "")
                    if (eventKey.isEmpty()) "event_key wajib diisi." else AiBlockWriter.readEventBlocks(scId, javaName, eventKey)
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
                "Menunggu persetujuan pengguna."
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
                if (code.trim().isEmpty()) return "Kode kosong."
                if (code.length > MAX_SOURCE_CHARS) return "Kode terlalu panjang (maksimal $MAX_SOURCE_CHARS karakter)."
                if (!AiBlockWriter.isValidEventKey(scId, javaName, eventKey)) return "Event '$eventKey' tidak valid."

                proposals.add(Proposal.source(javaName, eventKey, code))
                "Menunggu persetujuan pengguna untuk memasukkan source code."
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
                "Menunggu persetujuan pengguna untuk menambah widget $widgetType ($id)."
            }

            "apply_layout_xml" -> {
                val targetXml = args.optString("xml_name", xmlName)
                val content = args.getString("xml_content")
                proposals.add(Proposal.layout(targetXml, content))
                "Menunggu persetujuan pengguna untuk menerapkan layout XML ke $targetXml."
            }

            "get_compile_error" -> {
                AiDiagnosticHelper.getCompileError(scId)
            }

            "analyze_code_bug" -> {
                AiDiagnosticHelper.diagnoseProject(scId, javaName, xmlName)
            }

            else -> "Tool tidak dikenal: $name"
        }
    }

    private fun disabled(tool: String): String {
        return "Tool $tool dinonaktifkan di pengaturan agent."
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

        HTTP.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $text")
            }
            return JSONObject(text)
        }
    }

    private fun toolSchemas(): JSONArray {
        val tools = JSONArray()

        if (store.isToolEnabled("list_blocks")) {
            val props = JSONObject().put("query", JSONObject().put("type", "string").put("description", "Kata kunci opcode atau teks block"))
            tools.put(tool("list_blocks", "Cari block yang tersedia.", props, JSONArray()))
        }

        if (store.isToolEnabled("list_event_keys")) {
            tools.put(tool("list_event_keys", "Daftar event yang sudah berisi block.", JSONObject(), JSONArray()))
        }

        if (store.isToolEnabled("list_events")) {
            tools.put(tool("list_events", "Daftar event Activity bawaan.", JSONObject(), JSONArray()))
        }

        if (store.isToolEnabled("read_event_blocks")) {
            val props = JSONObject().put("event_key", JSONObject().put("type", "string").put("description", "Event key"))
            tools.put(tool("read_event_blocks", "Baca block dalam suatu event.", props, JSONArray().put("event_key")))
        }

        if (store.isToolEnabled("append_blocks")) {
            val stepItem = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put("op", JSONObject().put("type", "string")).put("params", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string"))))
                .put("required", JSONArray().put("op").put("params"))

            val props = JSONObject().put("event_key", JSONObject().put("type", "string")).put("steps", JSONObject().put("type", "array").put("items", stepItem))
            tools.put(tool("append_blocks", "Tambahkan block ke event.", props, JSONArray().put("event_key").put("steps")))
        }

        if (store.isToolEnabled("clear_event_blocks")) {
            val props = JSONObject().put("event_key", JSONObject().put("type", "string").put("description", "Event key"))
            tools.put(tool("clear_event_blocks", "Hapus semua block di event.", props, JSONArray().put("event_key")))
        }

        if (store.isToolEnabled("create_more_block")) {
            val props = JSONObject().put("name", JSONObject().put("type", "string")).put("spec", JSONObject().put("type", "string"))
            tools.put(tool("create_more_block", "Buat More Block baru.", props, JSONArray().put("name").put("spec")))
        }

        if (store.isToolEnabled("propose_source_code")) {
            val props = JSONObject().put("event_key", JSONObject().put("type", "string")).put("code", JSONObject().put("type", "string"))
            tools.put(tool("propose_source_code", "Ajukan source code langsung (addSourceDirectly).", props, JSONArray().put("event_key").put("code")))
        }

        if (store.isToolEnabled("read_layout")) {
            val props = JSONObject().put("xml_name", JSONObject().put("type", "string"))
            tools.put(tool("read_layout", "Baca hirarki widget dan XML layout.", props, JSONArray()))
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
            tools.put(tool("add_widget", "Tambah widget baru ke layout.", props, JSONArray().put("widget_type")))
        }

        if (store.isToolEnabled("apply_layout_xml")) {
            val props = JSONObject().put("xml_name", JSONObject().put("type", "string")).put("xml_content", JSONObject().put("type", "string"))
            tools.put(tool("apply_layout_xml", "Ganti atau buat layout XML.", props, JSONArray().put("xml_content")))
        }

        if (store.isToolEnabled("get_compile_error")) {
            tools.put(tool("get_compile_error", "Dapatkan log error compile terbaru.", JSONObject(), JSONArray()))
        }

        if (store.isToolEnabled("analyze_code_bug")) {
            tools.put(tool("analyze_code_bug", "Analisis potensi bug pada project.", JSONObject(), JSONArray()))
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
