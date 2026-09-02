package extensions.anbui.daydream.project

import android.app.Activity
import android.content.Context
import android.util.Log
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.ui.DialogUtils
import pro.sketchware.R

object DRProjectTracker {
    @JvmStatic
    var TAG: String = Configs.universalTAG + "DRProjectTracker"

    @JvmStatic
    var configData: String = ""
    @JvmStatic
    var buildConfigData: String = ""
    @JvmStatic
    var viewData: String = ""
    @JvmStatic
    var activityLauncher: String = ""
    @JvmStatic
    var currentprojectID: String = ""
    @JvmStatic
    var isBuildForRelease: Boolean = false
    @JvmStatic
    var isExportForAndroidStudio: Boolean = false

    @JvmStatic
    fun startNowForAndroidStudio(projectID: String?) {
        startNow(projectID, isForRelease = false, isForAndroidStudio = true)

        Log.i(TAG, "You're in export for Android Studio mode.")
    }

    @JvmStatic
    fun startNow(projectID: String?) {
        startNow(projectID, isForRelease = false, isForAndroidStudio = false)
    }

    @JvmStatic
    fun startNow(projectID: String?, isForRelease: Boolean) {
        startNow(projectID, isForRelease, false)
    }

    @JvmStatic
    fun startNow(projectID: String?, isForRelease: Boolean, isForAndroidStudio: Boolean) {
        if (projectID == null) return

        isBuildForRelease = isForRelease
        isExportForAndroidStudio = isForAndroidStudio

        Configs.currentProjectID = projectID
        currentprojectID = projectID
        val contentDRConfigs =
            FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/DataDayDream.json")
        configData = contentDRConfigs.ifEmpty { "{}" }

        val contentBuildConfigs =
            FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/build_config")
        buildConfigData = contentBuildConfigs.ifEmpty { "{}" }

        viewData = DRProjectView.read(projectID)

        activityLauncher = ProjectInjection.readActivityLauncher(projectID)

        Log.i(TAG, "Loaded configs data.")

        if (isForRelease) {
            Log.i(TAG, "You're in release mode.")
        } else {
            Log.i(TAG, "You're in debug mode.")
        }
    }

    @JvmStatic
    fun isAllowBuildNow(context: Context?): Boolean {
        if (context != null && Configs.isBuilding) {
            DialogUtils.oneDialog(
                context as Activity?,
                "Oops!",
                "Another project is building, please wait for it to complete before building in this project.",
                "OK",
                true,
                R.drawable.ic_mtrl_warning,
                true,
                null,
                null
            )
        }

        return !Configs.isBuilding
    }
}