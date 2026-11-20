package org.sketchware.daygreen.ai

import a.a.a.jC
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.besome.sketch.beans.ProjectFileBean
import com.besome.sketch.editor.LogicEditorActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.pranav.filepicker.FilePickerCallback
import dev.pranav.filepicker.FilePickerDialogFragment
import dev.pranav.filepicker.FilePickerOptions
import org.sketchware.daygreen.ai.activities.AiSettingsActivity
import pro.sketchware.R
import pro.sketchware.databinding.DialogAiInputBinding
import pro.sketchware.databinding.FragmentAiChatBinding
import pro.sketchware.databinding.ItemAttachmentChipBinding
import pro.sketchware.utility.SketchwareUtil
import java.io.File

class AiChatFragment : Fragment() {

    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var client: AiChatClient
    private lateinit var store: AiSettingsStore
    private lateinit var adapter: ChatAdapter
    private lateinit var formatter: ChatTextFormatter
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pending: AiChatClient.Proposal? = null
    private var isBusy = false

    private data class Attachment(
        val uri: Uri,
        val name: String,
        val isImage: Boolean,
        val bitmap: Bitmap? = null
    )

    private val attachedList = mutableListOf<Attachment>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { addAttachmentChip(it, isImage = true) }
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        updateKeyboardPosition()
    }

    companion object {
        private const val ARG_SC_ID = "sc_id"
        private const val ARG_JAVA_NAME = "java_name"

        @JvmStatic
        fun newInstance(scId: String, javaName: String): AiChatFragment {
            val fragment = AiChatFragment()
            val args = Bundle().apply {
                putString(ARG_SC_ID, scId)
                putString(ARG_JAVA_NAME, javaName)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        store = AiSettingsStore(requireContext())
        val scId = getScId()
        val javaName = getJavaName()
        val xmlName = getXmlName()

        client = AiChatClient(requireContext(), scId, javaName, xmlName)
        formatter = ChatTextFormatter(requireContext())
        adapter = ChatAdapter(formatter) { eventName -> openEvent(eventName) }

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = false
        }
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = adapter

        // Listen for keyboard and view hierarchy size changes
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            updateKeyboardPosition()
            insets
        }

        // Show/hide "Scroll to bottom" pill button
        binding.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val canScrollDown = recyclerView.canScrollVertically(1)
                binding.btnScrollToBottom.visibility = if (canScrollDown) View.VISIBLE else View.GONE
            }
        })

        binding.btnScrollToBottom.setOnClickListener {
            scrollToBottom()
        }

        if (scId.isEmpty()) {
            adapter.add(
                ChatAdapter.ChatMessage.agent(
                    "Project ID is missing. Please close and reopen your project."
                )
            )
            binding.btnSend.isEnabled = false
            return
        }

        binding.btnPrompt1.setOnClickListener {
            sendPrompt("Fix the warnings and errors in the current file")
        }
        binding.btnPrompt2.setOnClickListener {
            sendPrompt("Add documentation to my current file")
        }
        binding.btnPrompt3.setOnClickListener {
            sendPrompt("Explain this code to me")
        }

        binding.btnSend.setOnClickListener {
            if (isBusy) {
                cancelRequest()
            } else {
                send()
            }
        }

        binding.btnApply.setOnClickListener { confirmApply() }
        binding.btnLink.setOnClickListener { showAttachmentOptions() }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.btnModel.setOnClickListener {
            AiModelPicker.show(requireContext(), store, store.providerIndex) {
                refreshModelLabel()
            }
        }

        updateEmptyState()
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateEmptyState()
            }
        })
    }

    private fun updateKeyboardPosition() {
        if (_binding == null) return
        val root = binding.root
        val insets = ViewCompat.getRootWindowInsets(root)
        val imeInsets = insets?.getInsets(WindowInsetsCompat.Type.ime())
        val imeHeight = imeInsets?.bottom ?: 0
        val imeVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true || imeHeight > 100

        val targetTranslationY = if (imeVisible) {
            val r = Rect()
            root.getWindowVisibleDisplayFrame(r)
            val loc = IntArray(2)
            root.getLocationOnScreen(loc)
            val rootBottom = loc[1] + root.height
            val overlap = (rootBottom - r.bottom).coerceAtLeast(0)
            -overlap.toFloat()
        } else {
            0f
        }

        if (binding.cvInput.translationY != targetTranslationY) {
            binding.cvInput.translationY = targetTranslationY
            binding.btnScrollToBottom.translationY = targetTranslationY
            if (targetTranslationY < 0f) {
                scrollToBottom()
            }
        }
    }

    private fun sendPrompt(text: String) {
        binding.etInput.setText(text)
        send()
    }

    private fun updateEmptyState() {
        if (_binding == null) return
        val empty = adapter.itemCount == 0
        binding.llEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        refreshModelLabel()
    }

    override fun onDestroyView() {
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        _binding = null
        super.onDestroyView()
    }

    private fun openSettings() {
        startActivity(Intent(requireContext(), AiSettingsActivity::class.java))
    }

    private fun refreshModelLabel() {
        if (_binding == null) return
        val providerIndex = store.providerIndex
        if (providerIndex < 0 || providerIndex >= AiProviders.ALL.size) return

        val provider = AiProviders.ALL[providerIndex]
        val modelId = store.getModelId("")
        binding.tvModelName.text = if (modelId.isEmpty()) provider.models[0] else modelId
    }

    /**
     * Add an attachment chip for photo or file.
     */
    private fun addAttachmentChip(uri: Uri, isImage: Boolean, customName: String? = null) {
        val fileName = customName ?: getFileName(uri)
        var bitmap: Bitmap? = null

        if (isImage) {
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                    val sampleSize = calculateInSampleSize(options, 72, 72)

                    requireContext().contentResolver.openInputStream(uri)?.use { stream2 ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        }
                        bitmap = BitmapFactory.decodeStream(stream2, null, decodeOptions)
                    }
                }
            } catch (_: Exception) {}
        }

        val attachment = Attachment(uri, fileName, isImage, bitmap)
        attachedList.add(attachment)

        val chipBinding = ItemAttachmentChipBinding.inflate(layoutInflater, binding.llAttachments, false)
        chipBinding.tvAttachmentName.text = fileName

        if (isImage && bitmap != null) {
            chipBinding.ivAttachmentPreview.setImageBitmap(bitmap)
            chipBinding.ivAttachmentPreview.imageTintList = null
        } else {
            chipBinding.ivAttachmentPreview.setImageResource(R.drawable.ic_mtrl_folder)
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            chipBinding.ivAttachmentPreview.setColorFilter(typedValue.data)
        }

        chipBinding.btnAttachmentRemove.setOnClickListener {
            binding.llAttachments.removeView(chipBinding.root)
            attachedList.remove(attachment)
            if (attachedList.isEmpty()) {
                binding.hsvAttachments.visibility = View.GONE
            }
        }

        binding.llAttachments.addView(chipBinding.root)
        binding.hsvAttachments.visibility = View.VISIBLE
        binding.hsvAttachments.post {
            binding.hsvAttachments.fullScroll(View.FOCUS_RIGHT)
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        if (uri.scheme == "content") {
            try {
                val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            name = it.getString(index)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "attachment"
            val cut = name.lastIndexOf('/')
            if (cut != -1) {
                name = name.substring(cut + 1)
            }
        }
        return name
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * BottomSheetDialog for attachments and quick actions.
     */
    private fun showAttachmentOptions() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(requireContext()).apply {
            text = "Add Attachments & Quick Actions"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, pad)
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
            setTextColor(typedValue.data)
        }
        layout.addView(title)

        data class ActionItem(val iconRes: Int, val title: String, val onClick: () -> Unit)

        val actions = listOf(
            ActionItem(R.drawable.photo_library_24px, "Attach Photo") {
                pickImageLauncher.launch("image/*")
            },
            ActionItem(R.drawable.ic_mtrl_folder, "Attach Project File / Layout (${getXmlName()})") {
                openFilePicker()
            },
            ActionItem(R.drawable.ic_mtrl_bug_report, "Analyze Compile Errors") {
                attachCompileError()
            },
            ActionItem(R.drawable.ic_mtrl_code, "Insert Logic Blocks (${getJavaName()})") {
                binding.etInput.setText("Please add logic blocks to ${getJavaName()} for event ")
                binding.etInput.setSelection(binding.etInput.text.length)
            },
            ActionItem(R.drawable.ic_mtrl_link, "Add URL Link") {
                showLinkDialog()
            }
        )

        val padV = (10 * resources.displayMetrics.density).toInt()
        val iconSize = (22 * resources.displayMetrics.density).toInt()
        val outValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)

        actions.forEach { action ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, padV, 0, padV)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener {
                    bottomSheet.dismiss()
                    action.onClick()
                }
            }

            val iconView = ImageView(requireContext()).apply {
                setImageResource(action.iconRes)
                val params = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    marginEnd = (14 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(R.attr.colorOnSurfaceVariant, typedValue, true)
                setColorFilter(typedValue.data)
            }
            row.addView(iconView)

            val textView = TextView(requireContext()).apply {
                text = action.title
                textSize = 15f
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
                setTextColor(typedValue.data)
            }
            row.addView(textView)

            layout.addView(row)
        }

        bottomSheet.setContentView(layout)
        bottomSheet.show()
    }

    private fun openFilePicker() {
        val options = FilePickerOptions().apply {
            title = "Select File"
        }
        val callback = object : FilePickerCallback() {
            override fun onFileSelected(file: File) {
                addAttachmentChip(Uri.fromFile(file), isImage = false, customName = file.name)
            }
        }
        FilePickerDialogFragment(options, callback).show(parentFragmentManager, "file_picker")
    }

    private fun attachCompileError() {
        val error = AiDiagnosticHelper.getCompileError(getScId())
        binding.etInput.setText("Please analyze the following compile errors and suggest fixes:\n$error")
        binding.etInput.setSelection(binding.etInput.text.length)
    }

    private fun showLinkDialog() {
        val dialog = DialogAiInputBinding.inflate(layoutInflater)
        dialog.tilInput.hint = "URL"
        dialog.etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add URL Link")
            .setView(dialog.root)
            .setPositiveButton("Add") { _, _ ->
                val url = dialog.etInput.text?.toString()?.trim() ?: ""
                if (url.isNotEmpty()) {
                    val current = binding.etInput.text?.toString() ?: ""
                    val separator = if (current.isEmpty() || current.endsWith(" ")) "" else " "
                    binding.etInput.append("$separator$url ")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEvent(eventName: String) {
        if (_binding == null) return
        val scId = getScId()
        val javaName = getJavaName()

        val file = findProjectFile(scId, javaName)
        if (file == null) {
            SketchwareUtil.toast("Java file not found in project.")
            return
        }

        val onCreateGroup = eventName == "onCreate" || eventName == "initializeLogic"
        val id = if (onCreateGroup) "onCreate" else eventName
        val event = if (eventName == "onCreate") "initializeLogic" else eventName

        val intent = Intent(requireContext(), LogicEditorActivity::class.java).apply {
            putExtra("sc_id", scId)
            putExtra("id", id)
            putExtra("event", event)
            putExtra("project_file", file)
        }
        startActivity(intent)
    }

    private fun findProjectFile(scId: String, javaName: String): ProjectFileBean? {
        return try {
            jC.b(scId).b().firstOrNull { it.javaName == javaName }
        } catch (e: Exception) {
            null
        }
    }

    private fun cancelRequest() {
        client.cancel()
        setBusy(false)
        adapter.removeStatus()
        adapter.add(ChatAdapter.ChatMessage.agent("Request canceled by user."))
        scrollToBottom()
    }

    private fun send() {
        var text = binding.etInput.text?.toString()?.trim() ?: ""
        if (text.isEmpty() && attachedList.isEmpty()) return

        if (text.isEmpty()) {
            text = "Please inspect the attached files/photos."
        }

        val promptBuilder = StringBuilder(text)
        if (attachedList.isNotEmpty()) {
            promptBuilder.append("\n\nAttached items:")
            for (att in attachedList) {
                if (att.isImage) {
                    promptBuilder.append("\n- [Image: ${att.name}]")
                } else {
                    val content = try {
                        val file = File(att.uri.path ?: "")
                        if (file.exists() && file.isFile) file.readText() else null
                    } catch (_: Exception) {
                        null
                    }
                    if (!content.isNullOrEmpty()) {
                        val trimmed = if (content.length > 2500) content.substring(0, 2500) + "\n... (truncated)" else content
                        promptBuilder.append("\n- File '${att.name}':\n```\n$trimmed\n```")
                    } else {
                        promptBuilder.append("\n- [File: ${att.name}]")
                    }
                }
            }
        }

        val finalPrompt = promptBuilder.toString()

        binding.etInput.setText("")
        attachedList.clear()
        binding.llAttachments.removeAllViews()
        binding.hsvAttachments.visibility = View.GONE

        adapter.add(ChatAdapter.ChatMessage.user(text))
        adapter.updateStatus("Thinking...")
        scrollToBottom()
        setBusy(true)

        client.ask(finalPrompt, object : AiChatClient.Callback {
            override fun onResult(reply: String, proposals: List<AiChatClient.Proposal>) {
                mainHandler.post {
                    if (_binding == null) return@post
                    setBusy(false)
                    adapter.removeStatus()
                    adapter.add(ChatAdapter.ChatMessage.agent(reply))

                    if (proposals.isEmpty()) {
                        pending = null
                        binding.btnApply.visibility = View.GONE
                    } else {
                        val p = proposals.last()
                        pending = p
                        binding.btnApply.visibility = View.VISIBLE

                        val buttonLabel: String
                        val proposalLabel: String
                        if (p.isLayout) {
                            buttonLabel = "Apply layout to ${p.xmlName}"
                            proposalLabel = "Proposed layout XML (not applied yet):\n"
                        } else if (p.isWidget) {
                            buttonLabel = "Add widget to ${p.xmlName}"
                            proposalLabel = "Proposed widget (not applied yet):\n"
                        } else if (p.isSource) {
                            buttonLabel = "Write source code to ${p.eventKey}"
                            proposalLabel = "Proposed source code (not written yet):\n"
                        } else {
                            buttonLabel = "Write blocks to ${p.eventKey}"
                            proposalLabel = "Proposed blocks (not written yet):\n"
                        }

                        binding.btnApply.text = buttonLabel
                        adapter.add(ChatAdapter.ChatMessage.agent(proposalLabel + p.summary))
                    }
                    scrollToBottom()
                }
            }

            override fun onError(error: String) {
                mainHandler.post {
                    if (_binding == null) return@post
                    setBusy(false)
                    adapter.removeStatus()
                    adapter.add(ChatAdapter.ChatMessage.agent("Error: $error"))
                    scrollToBottom()
                }
            }

            override fun onStatus(status: String) {
                mainHandler.post {
                    if (_binding == null) return@post
                    adapter.updateStatus(status)
                    scrollToBottom()
                }
            }

            override fun onThinking(thinking: String) {
                mainHandler.post {
                    if (_binding == null) return@post
                    adapter.removeStatus()
                    adapter.add(ChatAdapter.ChatMessage.thinking(thinking))
                    scrollToBottom()
                }
            }
        })
    }

    private fun confirmApply() {
        val p = pending ?: return
        if (p.isLayout) {
            confirmLayoutCode(p)
        } else if (p.isSource) {
            confirmSourceCode(p)
        } else if (p.isWidget) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add widget?")
                .setMessage("Add ${p.widgetType} (${p.widgetId}) to ${p.xmlName}?\n\n${p.summary}")
                .setPositiveButton("Add") { _, _ -> applyPending(p) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Write blocks?")
                .setMessage("Event: ${p.eventKey}\n\n${p.summary}")
                .setPositiveButton("Write") { _, _ -> applyPending(p) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun confirmLayoutCode(p: AiChatClient.Proposal) {
        val code = TextView(requireContext()).apply {
            text = p.layoutXml
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(requireContext()).apply { addView(code) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Apply layout XML to ${p.xmlName}?")
            .setMessage("This will update the project layout with the following XML code:")
            .setView(scroll)
            .setPositiveButton("Apply") { _, _ -> applyPending(p) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmSourceCode(p: AiChatClient.Proposal) {
        val code = TextView(requireContext()).apply {
            text = p.sourceCode
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(requireContext()).apply { addView(code) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Write this source code?")
            .setMessage("Event: ${p.eventKey}\nInserted using addSourceDirectly block.")
            .setView(scroll)
            .setPositiveButton("Write") { _, _ -> applyPending(p) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyPending(p: AiChatClient.Proposal) {
        val scId = getScId()
        Thread {
            val message: String
            val success: Boolean

            if (p.isLayout) {
                val r = AiLayoutHelper.applyLayoutXml(scId, p.xmlName, p.layoutXml)
                message = r.message
                success = r.success
            } else if (p.isWidget) {
                val r = AiLayoutHelper.addWidget(
                    scId, p.xmlName, p.widgetType, p.widgetId,
                    p.widgetParent, p.widgetText, p.widgetWidth, p.widgetHeight, p.widgetOrientation
                )
                message = r.message
                success = r.success
            } else if (p.isSource) {
                val r = AiBlockWriter.writeSourceCode(scId, p.javaName, p.eventKey, p.sourceCode)
                message = r.message
                success = r.success
            } else {
                val r = AiBlockWriter.appendBlocks(scId, p.javaName, p.eventKey, p.steps)
                message = r.message
                success = r.success
            }

            mainHandler.post {
                if (_binding == null) return@post
                adapter.add(ChatAdapter.ChatMessage.agent(message))
                if (success) {
                    pending = null
                    binding.btnApply.visibility = View.GONE
                    SketchwareUtil.toast("Applied successfully to project.")
                }
                scrollToBottom()
            }
        }.start()
    }

    private fun setBusy(busy: Boolean) {
        isBusy = busy
        if (busy) {
            binding.btnSend.setImageResource(R.drawable.ic_stop)
            binding.btnSend.contentDescription = "Stop"
        } else {
            binding.btnSend.setImageResource(R.drawable.ic_send_plane)
            binding.btnSend.contentDescription = "Send"
        }
    }

    private fun scrollToBottom() {
        if (_binding == null || adapter.itemCount == 0) return
        binding.rvChat.post { binding.rvChat.scrollToPosition(adapter.itemCount - 1) }
    }

    private fun getScId(): String = arguments?.getString(ARG_SC_ID, "") ?: ""

    private fun getJavaName(): String = arguments?.getString(ARG_JAVA_NAME, "MainActivity") ?: "MainActivity"

    private fun getXmlName(): String {
        val file = findProjectFile(getScId(), getJavaName())
        return file?.xmlName ?: "main.xml"
    }
}
