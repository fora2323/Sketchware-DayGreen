package mod.hey.studios.activity.managers.aidl

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.pranav.filepicker.FilePickerCallback
import dev.pranav.filepicker.FilePickerDialogFragment
import dev.pranav.filepicker.FilePickerOptions
import mod.hey.studios.code.SrcCodeEditor
import mod.hey.studios.util.Helper
import pro.sketchware.R
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding
import pro.sketchware.databinding.ManageFileBinding
import pro.sketchware.databinding.ManageJavaItemHsBinding
import pro.sketchware.utility.FilePathUtil
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.SketchwareUtil
import java.io.File

class ManageAidlActivity : BaseAppCompatActivity() {

    companion object {
        private const val AIDL_TEMPLATE = """package %s;

interface %s {
    // Define your interface methods here
}
"""
    }

    private lateinit var binding: ManageFileBinding
    private val currentTree = ArrayList<String>()
    private lateinit var current_path: String
    private lateinit var fpu: FilePathUtil
    private lateinit var sc_id: String
    private lateinit var filesAdapter: FilesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)
        binding = ManageFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sc_id = intent.getStringExtra("sc_id") ?: ""
        Helper.fixFileprovider()

        fpu = FilePathUtil()
        current_path = Uri.parse(getAidlPath()).getPath() ?: getAidlPath()

        setupUI()
        refresh()
    }

    private fun getAidlPath(): String {
        return File(FilePathUtil.SKETCHWARE_DATA, "$sc_id/files/aidl").absolutePath
    }

    override fun onBackPressed() {
        if (current_path == Uri.parse(getAidlPath()).getPath()) {
            super.onBackPressed()
        } else {
            current_path = current_path.substring(0, current_path.lastIndexOf("/"))
            refresh()
        }
    }

    private fun setupUI() {
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this))
        binding.topAppBar.title = "AIDL Manager"
        binding.showOptionsButton.setOnClickListener { hideShowOptionsButton(false) }
        binding.closeButton.setOnClickListener { hideShowOptionsButton(true) }
        binding.createNewButton.setOnClickListener {
            showCreateDialog()
            hideShowOptionsButton(true)
        }
        binding.importNewButton.setOnClickListener {
            showImportDialog()
            hideShowOptionsButton(true)
        }
    }

    private fun hideShowOptionsButton(isHide: Boolean) {
        binding.optionsLayout.animate()
            .translationY(if (isHide) 300f else 0f)
            .alpha(if (isHide) 0f else 1f)
        binding.showOptionsButton.animate()
            .translationY(if (isHide) 0f else 300f)
            .alpha(if (isHide) 1f else 0f)
    }

    private fun getCurrentPkgName(): String {
        val pkgName = intent.getStringExtra("pkgName") ?: ""
        return try {
            val trimmedPath = Helper.trimPath(getAidlPath())
            var substring = current_path.substring(
                current_path.indexOf(trimmedPath) + trimmedPath.length
            )
            if (substring.endsWith("/")) substring = substring.dropLast(1)
            if (substring.startsWith("/")) substring = substring.drop(1)
            val replace = substring.replace("/", ".")
            if (replace.isEmpty()) pkgName else "$pkgName.$replace"
        } catch (e: Exception) {
            pkgName
        }
    }

    private fun showCreateDialog() {
        val dialogBinding = DialogCreateNewFileLayoutBinding.inflate(layoutInflater)
        val inputText = dialogBinding.inputText

        // Sembunyiin chip yang ga relevan, tampilin AIDL interface aja
        dialogBinding.chipFolder.visibility = View.VISIBLE
        dialogBinding.chipJavaClass.visibility = View.GONE
        dialogBinding.chipJavaActivity.visibility = View.GONE
        dialogBinding.chipKotlinClass.visibility = View.GONE
        dialogBinding.chipKotlinActivity.visibility = View.GONE

        // Rename chip java class jadi AIDL Interface
        dialogBinding.chipJavaClass.text = "AIDL Interface"
        dialogBinding.chipJavaClass.visibility = View.VISIBLE

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setTitle("Create new AIDL")
            .setMessage("File extension .aidl will be added automatically")
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Create", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            inputText.requestFocus()

            val positiveButton = (it as androidx.appcompat.app.AlertDialog)
                .getButton(android.content.DialogInterface.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                val name = Helper.getText(inputText)
                if (name.isEmpty()) {
                    SketchwareUtil.toastError("Invalid file name")
                    return@setOnClickListener
                }

                val checkedId = dialogBinding.chipGroupTypes.checkedChipId
                if (checkedId == R.id.chip_folder) {
                    FileUtil.makeDir(File(current_path, name).absolutePath)
                    refresh()
                    SketchwareUtil.toast("Folder created successfully")
                    dialog.dismiss()
                    return@setOnClickListener
                }

                val packageName = getCurrentPkgName()
                val content = AIDL_TEMPLATE.format(packageName, name)
                FileUtil.writeFile(File(current_path, "$name.aidl").absolutePath, content)
                refresh()
                SketchwareUtil.toast("AIDL file created successfully")
                dialog.dismiss()
            }
            dialog.show()
        }

        dialog.show()
    }

    private fun showImportDialog() {
        val options = FilePickerOptions().apply {
            isMultipleSelection = true
            extensions = arrayOf("aidl")
            title = "Select AIDL file(s)"
        }

        val callback = object : FilePickerCallback() {
            override fun onFilesSelected(files: List<File>) {
                for (file in files) {
                    FileUtil.writeFile(
                        File(current_path, file.name).absolutePath,
                        FileUtil.readFile(file.absolutePath)
                    )
                }
                refresh()
            }
        }

        FilePickerDialogFragment(options, callback).show(supportFragmentManager, "filePicker")
    }

    private fun showRenameDialog(position: Int) {
        val dialogBinding = pro.sketchware.databinding.DialogInputLayoutBinding.inflate(layoutInflater)
        val inputText = dialogBinding.inputText

        MaterialAlertDialogBuilder(this)
            .setTitle("Rename ${filesAdapter.getFileName(position)}")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Rename") { d, _ ->
                val newName = Helper.getText(inputText)
                if (newName.isNotEmpty()) {
                    FileUtil.renameFile(
                        filesAdapter.getItem(position),
                        File(current_path, newName).absolutePath
                    )
                    refresh()
                    SketchwareUtil.toast("Renamed successfully")
                }
                d.dismiss()
            }
            .create()
            .also { dialog ->
                inputText.setText(filesAdapter.getFileName(position))
                dialog.show()
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                inputText.requestFocus()
            }
    }

    private fun showDeleteDialog(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete ${filesAdapter.getFileName(position)}?")
            .setMessage("Are you sure you want to delete this ${if (filesAdapter.isFolder(position)) "folder" else "file"}? This action cannot be undone.")
            .setPositiveButton(R.string.common_word_delete) { _, _ ->
                FileUtil.deleteFile(filesAdapter.getItem(position))
                refresh()
                SketchwareUtil.toast("Deleted successfully")
            }
            .setNegativeButton(R.string.common_word_cancel, null)
            .create()
            .show()
    }

    private fun refresh() {
        if (!FileUtil.isExistFile(getAidlPath())) {
            FileUtil.makeDir(getAidlPath())
        }

        currentTree.clear()
        FileUtil.listDir(current_path, currentTree)
        Helper.sortPaths(currentTree)

        filesAdapter = FilesAdapter(currentTree)
        binding.filesListRecyclerView.adapter = filesAdapter
        binding.noContentLayout.visibility = if (currentTree.isEmpty()) View.VISIBLE else View.GONE
    }

    inner class FilesAdapter(private val tree: List<String>) :
        RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ManageJavaItemHsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val b = holder.binding
            val fileName = getFileName(position)

            b.title.setText(fileName)
            b.root.setOnClickListener {
                if (isFolder(position)) {
                    current_path = getItem(position)
                    refresh()
                } else {
                    goEditFile(position)
                }
            }
            b.root.setOnLongClickListener {
                itemContextMenu(it, position, Gravity.CENTER)
                true
            }

            if (isFolder(position)) {
                b.icon.setImageResource(R.drawable.ic_mtrl_folder)
            } else {
                b.icon.setImageResource(R.drawable.ic_mtrl_code)
            }

            Helper.applyRipple(this@ManageAidlActivity, b.more)
            b.more.setOnClickListener { itemContextMenu(it, position, Gravity.RIGHT) }
        }

        override fun getItemCount() = tree.size

        fun getItem(position: Int) = tree[position]

        fun getFileName(position: Int): String {
            val item = getItem(position)
            return item.substring(item.lastIndexOf("/") + 1)
        }

        fun isFolder(position: Int) = FileUtil.isDirectory(getItem(position))

        fun goEditFile(position: Int) {
            val intent = Intent(applicationContext, SrcCodeEditor::class.java).apply {
                putExtra("java", "")
                putExtra("sc_id", sc_id)
                putExtra("title", getFileName(position))
                putExtra("content", getItem(position))
            }
            startActivity(intent)
        }

        private fun itemContextMenu(v: View, position: Int, gravity: Int) {
            val menu = PopupMenu(this@ManageAidlActivity, v, gravity)
            menu.menu.apply {
                clear()
                if (!isFolder(position)) add("Edit")
                add("Rename")
                add("Delete")
            }
            menu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Edit" -> goEditFile(position)
                    "Rename" -> showRenameDialog(position)
                    "Delete" -> showDeleteDialog(position)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            menu.show()
        }

        inner class ViewHolder(val binding: ManageJavaItemHsBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}