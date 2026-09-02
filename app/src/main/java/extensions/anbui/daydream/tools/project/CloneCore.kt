package extensions.anbui.daydream.tools.project

import android.app.Activity
import android.util.Log
import android.widget.TextView
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.tools.ToolCore

object CloneCore {

    var TAG: String = Configs.universalTAG + "CloneCore"

    @JvmStatic
    fun startNow(projectID: String?, statusTextView: TextView?): Boolean {
        val newID = (ToolCore.getLastID() + 1).toString()

        if (!FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID)) return false
        if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + newID + "/logic")) return false

        try {
            updateStatus(statusTextView, "Cloning data...")
            FileUtils.copyDirectory(
                FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/",
                FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + newID + "/"
            )
            updateStatus(statusTextView, "Cloning info...")
            FileUtils.copyFile(
                FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID + "/project",
                FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + newID + "/"
            )
            updateStatus(statusTextView, "Cloning fonts...")
            FileUtils.copyDirectory(
                FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + projectID + "/",
                FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + newID + "/"
            )
            updateStatus(statusTextView, "Cloning icons...")
            FileUtils.copyDirectory(
                FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + projectID + "/",
                FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + newID + "/"
            )
            updateStatus(statusTextView, "Cloning images...")
            FileUtils.copyDirectory(
                FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + projectID + "/",
                FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + newID + "/"
            )
            updateStatus(statusTextView, "Cloning sounds...")
            FileUtils.copyDirectory(
                FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + projectID + "/",
                FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + newID + "/"
            )
            updateStatus(statusTextView, "Fixing ID...")
            ToolCore.fixID(newID)
            Log.i(
                TAG,
                "Cloned: " + FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + newID + "/"
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Clone failed: " + e.message, e)
            return false
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