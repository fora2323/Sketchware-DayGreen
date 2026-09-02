package extensions.anbui.daydream.project

import android.util.Log
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.project.DRProjectTracker.currentprojectID
import extensions.anbui.daydream.project.DRProjectTracker.viewData
import extensions.anbui.daydream.project.ProjectInjection.getActivityLauncher
import extensions.anbui.daydream.tools.ToolCore

object DRProjectView {
    var TAG: String = Configs.universalTAG + "ProjectView"

    @JvmStatic
    fun isActivityLauncher(projectID: String, viewName: String?): Boolean {
        Log.i(TAG, "isActivityLauncher: $projectID, $viewName")
        if (isViewExist(projectID, viewName)) {
            return getActivityLauncher(projectID) == viewName
        }
        return false
    }

    @JvmStatic
    fun isViewExist(projectID: String?, viewName: String?): Boolean {
        Log.i(TAG, "isViewExist: $projectID")
        return readViewData(projectID).contains("@$viewName.xml")
    }

    @JvmStatic
    fun readViewData(projectID: String?): String {
        Log.i(TAG, "readViewData: $projectID")
        if (!viewData.isEmpty() && currentprojectID == projectID) {
            Log.i(TAG, "readViewData: Data retrieved from DRProjectTracker.")
            return viewData
        }
        return read(projectID)
    }

    @JvmStatic
    fun read(projectID: String?): String {
        Log.i(TAG, "read: $projectID")
        ToolCore.copyToTemp(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/view")
        return ProjectDecryptor.decryptProjectFile(ToolCore.getTempFilePath("view"))
    }
}
