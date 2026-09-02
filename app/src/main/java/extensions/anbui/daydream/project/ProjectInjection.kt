package extensions.anbui.daydream.project

import android.util.Log
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils

object ProjectInjection {
    @JvmStatic
    var TAG: String = Configs.universalTAG + "ProjectInjection"

    @JvmStatic
    fun getActivityLauncher(projectId: String): String {
        return if (DRProjectTracker.activityLauncher.isNotEmpty() && DRProjectTracker.currentprojectID == projectId) {
            Log.i(TAG, "getLauncherActivity: Data retrieved from DRProjectTracker.")
            DRProjectTracker.activityLauncher
        } else {
            readActivityLauncher(projectId)
        }
    }

    @JvmStatic
    fun readActivityLauncher(projectId: String): String {
        return if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectId + Configs.customActivityLauncherConfigDataPath)) {
            val content =
                FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectId + Configs.customActivityLauncherConfigDataPath)
                    .trim()
            content.ifEmpty { "main" }
        } else {
            "main"
        }
    }
}