package extensions.anbui.daydream.tools.project

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import extensions.anbui.daydream.activity.project.DayDreamBackupTool
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.ui.DialogUtils
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.file.FilesTools
import extensions.anbui.daydream.project.GetProjectInfo
import pro.sketchware.activities.main.activities.MainActivity
import java.nio.file.Path
import pro.sketchware.R

object RemoveCore {
    var TAG: String = Configs.universalTAG + "RemoveCore"

    @JvmStatic
    fun showDialogNow(activity: Activity, projectID: String?) {
        DialogUtils.threeDialog(
            activity,
            "Remove " + GetProjectInfo.getProjectName(projectID),
            "Do you want to remove the project? The data will not be restored. You can back it up before removing.",
            "Remove",
            "Cancel",
            "Backup",
            true,
            R.drawable.ic_mtrl_delete,
            true,
            { startNow(activity, projectID) },
            null,
            {
                val intent = Intent(activity, DayDreamBackupTool::class.java).apply {
                    putExtra("sc_id", projectID)
                }
                activity.startActivity(intent)
            }, null
        )
    }

    @JvmStatic
    fun startNow(activity: Activity?, projectID: String?) {
        var progressDialog : AlertDialog? = null

            val progressView: View =
                LayoutInflater.from(activity).inflate(R.layout.progress_msg_box, null)
            val progressText = progressView.findViewById<TextView?>(R.id.tv_progress)
            progressText?.text =
                if (projectID.equals(Configs.defaultQuickLookProjectID)) "Cleaning up..." else "Removing..."

        activity?.let {
            progressDialog = MaterialAlertDialogBuilder(it)
                .setView(progressView)
                .setCancelable(false)
                .create()
            progressDialog.show()
        }

        Thread {
            startRemove(projectID, progressText)
            activity?.runOnUiThread {
                progressDialog?.dismiss()
                if (!projectID.equals(Configs.defaultQuickLookProjectID)) {
                    DialogUtils.oneDialog(
                        activity,
                        "Done",
                        "Removed your project.",
                        "OK",
                        true,
                        R.drawable.ic_mtrl_check,
                        true, null, null
                    )
                }

                if (activity is MainActivity) {
                    activity.n()
                }
            }
        }.start()
    }

    @JvmStatic
    fun startRemove(projectID: String?, statusTextView: TextView?) {

        if (projectID == null || projectID.isEmpty()) return

        updateStatus(statusTextView, "Removing data...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing project info...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing fonts...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing icons...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing images...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing sounds...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing unsaved data...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }

        updateStatus(statusTextView, "Removing temporary files...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }


        updateStatus(statusTextView, "Removing Git...")
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID))
        } catch (e : Exception) {
            Log.e(TAG, "Removing failed: ", e)
        }
    }

    private fun updateStatus(statusTextView: TextView?, msg: String?) {
        Log.i(TAG, "updateStatus: $msg")
        if (statusTextView == null || statusTextView.context == null) return
        (statusTextView.context as Activity).runOnUiThread {
            statusTextView.text = msg
        }
    }
}