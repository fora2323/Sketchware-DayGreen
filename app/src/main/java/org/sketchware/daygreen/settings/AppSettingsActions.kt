package org.sketchware.daygreen.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.besome.sketch.editor.view.ViewEditor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.pranav.filepicker.FilePickerCallback
import dev.pranav.filepicker.FilePickerDialogFragment
import dev.pranav.filepicker.FilePickerOptions
import dev.pranav.filepicker.SelectionMode
import mod.alucard.tn.apksigner.ApkSigner
import mod.hey.studios.code.SrcCodeEditor
import mod.hey.studios.util.Helper
import pro.sketchware.R
import pro.sketchware.databinding.DialogSelectApkToSignBinding
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.SketchwareUtil
import java.io.File

object AppSettingsActions {

    fun openWorkingDirectory(activity: AppCompatActivity) {
        val options = FilePickerOptions().apply {
            selectionMode = SelectionMode.BOTH
            multipleSelection = true
            title = "Select an entry to modify"
            initialDirectory = activity.filesDir.parentFile!!.absolutePath
        }

        val callback = object : FilePickerCallback() {
            override fun onFilesSelected(files: List<out File>) {
                val isDirectory = files[0].isDirectory
                val typeName = if (isDirectory) "folder" else "file"

                if (files.size > 1 || isDirectory) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Select an action")
                        .setSingleChoiceItems(arrayOf("Delete"), -1) { actionDialog, _ ->
                            MaterialAlertDialogBuilder(activity)
                                .setTitle("Delete $typeName?")
                                .setMessage("Are you sure you want to delete this $typeName permanently? This cannot be undone.")
                                .setPositiveButton(R.string.common_word_delete) { deleteConfirmationDialog, _ ->
                                    for (file in files) {
                                        FileUtil.deleteFile(file.absolutePath)
                                        deleteConfirmationDialog.dismiss()
                                    }
                                }
                                .setNegativeButton(R.string.common_word_cancel, null)
                                .show()
                            actionDialog.dismiss()
                        }
                        .show()
                } else {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Select an action")
                        .setSingleChoiceItems(arrayOf("Edit", "Delete"), -1) { actionDialog, which ->
                            when (which) {
                                0 -> {
                                    val intent = Intent(activity.applicationContext, SrcCodeEditor::class.java)
                                    intent.putExtra("title", Uri.fromFile(files[0]).lastPathSegment)
                                    intent.putExtra("content", files[0].absolutePath)
                                    intent.putExtra("xml", "")
                                    activity.startActivity(intent)
                                }
                                1 -> MaterialAlertDialogBuilder(activity)
                                    .setTitle("Delete file?")
                                    .setMessage("Are you sure you want to delete this file permanently? This cannot be undone.")
                                    .setPositiveButton(R.string.common_word_delete) { _, _ ->
                                        FileUtil.deleteFile(files[0].absolutePath)
                                    }
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .show()
                            }
                            actionDialog.dismiss()
                        }
                        .show()
                }
            }
        }

        FilePickerDialogFragment(options, callback).show(activity.supportFragmentManager, "file_picker")
    }

    fun signApkFileDialog(activity: AppCompatActivity) {
        var isApkSelected = false
        val apkPathDialog = MaterialAlertDialogBuilder(activity)
        apkPathDialog.setTitle("Sign APK with testkey")

        val binding = DialogSelectApkToSignBinding.inflate(activity.layoutInflater)
        val testkeyRoot: View = binding.root
        val apkPathTxt: TextView = binding.apkPathTxt

        binding.selectFile.setOnClickListener {
            val options = FilePickerOptions().apply {
                extensions = arrayOf("apk")
            }
            val callback = object : FilePickerCallback() {
                override fun onFileSelected(file: File) {
                    isApkSelected = true
                    apkPathTxt.text = file.absolutePath
                }
            }
            FilePickerDialogFragment(options, callback).show(activity.supportFragmentManager, "file_picker")
        }

        apkPathDialog.setPositiveButton("Continue") { v, _ ->
            if (!isApkSelected) {
                SketchwareUtil.toast("Please select an APK file to sign", Toast.LENGTH_SHORT)
                ViewEditor.shakeView(binding.selectFile)
                return@setPositiveButton
            }

            val inputApkPath = Helper.getText(apkPathTxt)
            val outputApkFileName = Uri.fromFile(File(inputApkPath)).lastPathSegment
            val outputApkPath = File(
                Environment.getExternalStorageDirectory(),
                "sketchware/signed_apk/$outputApkFileName"
            ).absolutePath

            if (File(outputApkPath).exists()) {
                MaterialAlertDialogBuilder(activity)
                    .setIcon(R.drawable.color_save_as_new_96)
                    .setTitle("File exists")
                    .setMessage("An APK named $outputApkFileName already exists at /sketchware/signed_apk/.  Overwrite it?")
                    .setNegativeButton(Helper.getResString(R.string.common_word_cancel), null)
                    .setPositiveButton("Overwrite") { _, _ ->
                        v.dismiss()
                        signApkFileWithDialog(activity, inputApkPath, outputApkPath)
                    }
                    .show()
            } else {
                signApkFileWithDialog(activity, inputApkPath, outputApkPath)
            }
        }

        apkPathDialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null)
        apkPathDialog.setView(testkeyRoot)
        apkPathDialog.setCancelable(false)
        apkPathDialog.show()
    }

    private fun signApkFileWithDialog(
        activity: AppCompatActivity,
        inputApkPath: String,
        outputApkPath: String,
    ) {
        val buildingRoot = activity.layoutInflater.inflate(R.layout.build_progress_msg_box, null, false)
        val layoutQuiz = buildingRoot.findViewById<LinearLayout>(R.id.layout_quiz)
        val tvProgress = buildingRoot.findViewById<TextView>(R.id.tv_progress)

        val scrollView = ScrollView(activity)
        val tvLog = TextView(activity)
        scrollView.addView(tvLog)
        layoutQuiz.addView(scrollView)

        tvProgress.text = "Signing APK..."

        val buildingDialog: AlertDialog = MaterialAlertDialogBuilder(activity)
            .setView(buildingRoot)
            .create()

        val signer = ApkSigner()
        Thread {
            val callback = ApkSigner.LogCallback { line ->
                activity.runOnUiThread {
                    tvLog.text = Helper.getText(tvLog) + line
                }
            }

            signer.signWithTestKey(inputApkPath, outputApkPath, callback)

            activity.runOnUiThread {
                if (ApkSigner.LogCallback.errorCount.get() == 0) {
                    buildingDialog.dismiss()
                    SketchwareUtil.toast(
                        "Successfully saved signed APK to: /Internal storage/sketchware/signed_apk/" +
                            Uri.fromFile(File(outputApkPath)).lastPathSegment,
                        Toast.LENGTH_LONG
                    )
                } else {
                    tvProgress.text = "An error occurred. Check the log for more details."
                }
            }
        }.start()

        buildingDialog.show()
    }
}