package org.sketchware.daygreen.ai.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.besome.sketch.editor.manage.library.LibraryCategoryView
import com.besome.sketch.editor.manage.library.LibraryItemView
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.sketchware.daygreen.ai.AiConnectionTester
import org.sketchware.daygreen.ai.AiModelPicker
import org.sketchware.daygreen.ai.AiProviders
import org.sketchware.daygreen.ai.AiSettingsStore
import pro.sketchware.databinding.ActivityAiSettingsBinding
import pro.sketchware.databinding.DialogAiInputBinding

class AiSettingsActivity : BaseAppCompatActivity() {

    private data class Tool(val name: String, val category: String, val policy: String)

    companion object {
        private val TOOLS = arrayOf(
            Tool("list_blocks", "Read", "runs freely"),
            Tool("list_event_keys", "Read", "runs freely"),
            Tool("list_events", "Read", "runs freely"),
            Tool("read_event_blocks", "Read", "runs freely"),
            Tool("append_blocks", "Write", "asks first"),
            Tool("clear_event_blocks", "Write", "runs freely"),
            Tool("create_more_block", "Write", "asks first"),
            Tool("propose_source_code", "Write", "asks first"),
            Tool("read_layout", "Layout", "runs freely"),
            Tool("add_widget", "Layout", "asks first"),
            Tool("apply_layout_xml", "Layout", "asks first"),
            Tool("get_compile_error", "Diagnostic", "runs freely"),
            Tool("analyze_code_bug", "Diagnostic", "runs freely")
        )

        private const val DEFAULT_SYSTEM_PROMPT =
            "Anda adalah pengembang aplikasi Android & Sketchware AI di Sketchware DayGreen."

        private const val COLOR_OK = 0xFF4CAF50.toInt()
        private const val COLOR_ERROR = 0xFFE53935.toInt()
        private const val COLOR_NEUTRAL = 0xFF9E9E9E.toInt()
    }

    private lateinit var binding: ActivityAiSettingsBinding
    private lateinit var store: AiSettingsStore
    private val mainHandler = Handler(Looper.getMainLooper())
    private var providerIndex = 0
    private var modelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)
        binding = ActivityAiSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainView = binding.main
        val left = mainView.paddingLeft
        val top = mainView.paddingTop
        val right = mainView.paddingRight
        val bottom = mainView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { _, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            mainView.setPadding(left + systemInsets.left, top, right + systemInsets.right, bottom)
            insets
        }

        val scroll = binding.contentScroll
        val sLeft = scroll.paddingLeft
        val sTop = scroll.paddingTop
        val sRight = scroll.paddingRight
        val sBottom = scroll.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(scroll) { _, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            scroll.setPadding(sLeft, sTop, sRight, sBottom + navBarInsets.bottom)
            insets
        }

        store = AiSettingsStore(this)
        providerIndex = store.providerIndex
        modelIndex = store.modelIndex

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rowProvider.setOnClickListener { showProviderDialog() }
        binding.rowApiKey.setOnClickListener { showApiKeyDialog() }
        binding.rowModel.setOnClickListener { showModelDialog() }
        binding.rowSystemPrompt.setOnClickListener { showSystemPromptDialog() }
        binding.rowTest.setOnClickListener { runTest() }

        binding.radioPermission.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbAsk.id -> store.permission = 0
                binding.rbAutoEdit.id -> store.permission = 1
                binding.rbPlan.id -> store.permission = 2
                binding.rbAutonomous.id -> store.permission = 3
            }
        }
        selectPermission(store.permission)

        buildToolRows()
        refreshValues()
    }

    private fun selectPermission(index: Int) {
        val buttons = arrayOf(binding.rbAsk, binding.rbAutoEdit, binding.rbPlan, binding.rbAutonomous)
        val validIndex = index.coerceIn(0, buttons.size - 1)
        buttons[validIndex].isChecked = true
    }

    private fun refreshValues() {
        val provider = AiProviders.ALL[providerIndex]
        val apiKey = store.getApiKey(providerIndex)

        binding.tvProviderValue.text = provider.name
        binding.tvApiKeyValue.text = if (apiKey.isEmpty()) "Not set" else maskKey(apiKey)
        val modelId = store.getModelId("")
        binding.tvModelValue.text = if (modelId.isEmpty()) provider.models[0] else modelId
        binding.tvSystemPromptValue.text = store.getSystemPrompt(DEFAULT_SYSTEM_PROMPT)

        if (apiKey.isEmpty()) {
            setTestStatus("Masukkan API key terlebih dahulu", COLOR_NEUTRAL)
        } else if (store.isVerified(providerIndex)) {
            setTestStatus("Terhubung", COLOR_OK)
        } else {
            setTestStatus("Belum diuji", COLOR_NEUTRAL)
        }
    }

    private fun setTestStatus(text: String, color: Int) {
        binding.tvTestStatus.text = text
        binding.tvTestStatus.setTextColor(color)
    }

    private fun maskKey(key: String): String {
        if (key.length <= 8) {
            return "••••••••"
        }
        return key.substring(0, 3) + "••••••••" + key.substring(key.length - 4)
    }

    private fun buildToolRows() {
        val toolsCategory = LibraryCategoryView(this).apply {
            setTitle("Tools (${TOOLS.size})")
        }

        TOOLS.forEachIndexed { i, tool ->
            val item = LibraryItemView(this).apply {
                setHideIcon(true)
                setHideEnabled()
                title.text = tool.name
                description.text = "${tool.category} · ${tool.policy}"
                sw_enable.visibility = View.VISIBLE
                sw_enable.isChecked = store.isToolEnabled(tool.name)
                sw_enable.setOnCheckedChangeListener { _, isChecked ->
                    store.setToolEnabled(tool.name, isChecked)
                }
                setOnClickListener { sw_enable.toggle() }
            }
            toolsCategory.addLibraryItem(item, i < TOOLS.lastIndex)
        }

        binding.toolsContainer.addView(toolsCategory)
    }

    private fun showProviderDialog() {
        val names = Array(AiProviders.ALL.size) { i -> AiProviders.ALL[i].name }
        val selected = intArrayOf(providerIndex)

        MaterialAlertDialogBuilder(this)
            .setTitle("Provider")
            .setSingleChoiceItems(names, providerIndex) { _, which -> selected[0] = which }
            .setPositiveButton("OK") { _, _ ->
                if (providerIndex != selected[0]) {
                    providerIndex = selected[0]
                    modelIndex = 0
                    store.providerIndex = providerIndex
                    store.modelIndex = modelIndex
                    store.setModelId("")
                }
                refreshValues()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showApiKeyDialog() {
        showInputDialog("API key", "API key", store.getApiKey(providerIndex), password = true, multiline = false) { key ->
            store.setApiKey(providerIndex, key)
            store.setVerified(providerIndex, false)
            refreshValues()
            if (key.isNotEmpty()) {
                runTest()
            }
        }
    }

    private fun showModelDialog() {
        AiModelPicker.show(this, store, providerIndex) { refreshValues() }
    }

    private fun showSystemPromptDialog() {
        showInputDialog("System prompt", "System prompt", store.getSystemPrompt(DEFAULT_SYSTEM_PROMPT), password = false, multiline = true) { prompt ->
            store.setSystemPrompt(prompt)
            refreshValues()
        }
    }

    private fun showInputDialog(
        title: String,
        hint: String,
        initial: String,
        password: Boolean,
        multiline: Boolean,
        onSave: (String) -> Unit
    ) {
        val dialogBinding = DialogAiInputBinding.inflate(layoutInflater)
        dialogBinding.tilInput.hint = hint

        if (password) {
            dialogBinding.etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            dialogBinding.tilInput.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        } else if (multiline) {
            dialogBinding.etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            dialogBinding.etInput.minLines = 4
            dialogBinding.etInput.gravity = Gravity.TOP
        } else {
            dialogBinding.etInput.inputType = InputType.TYPE_CLASS_TEXT
        }

        dialogBinding.etInput.setText(initial)

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("OK") { _, _ -> onSave(dialogBinding.etInput.text.toString().trim()) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun runTest() {
        val testProvider = providerIndex
        val key = store.getApiKey(testProvider)
        if (key.isEmpty()) {
            setTestStatus("Masukkan API key terlebih dahulu", COLOR_NEUTRAL)
            return
        }

        setTestStatus("Menguji...", COLOR_NEUTRAL)
        AiConnectionTester.test(testProvider, key, object : AiConnectionTester.Callback {
            override fun onResult(success: Boolean, message: String) {
                mainHandler.post {
                    if (isDestroyed) return@post
                    store.setVerified(testProvider, success)
                    if (testProvider == providerIndex) {
                        setTestStatus(message, if (success) COLOR_OK else COLOR_ERROR)
                    }
                }
            }
        })
    }
}
