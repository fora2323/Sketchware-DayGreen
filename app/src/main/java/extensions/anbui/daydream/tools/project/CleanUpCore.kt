package extensions.anbui.daydream.tools.project

import android.util.Log
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.file.FilesTools
import extensions.anbui.daydream.tools.ToolCore
import java.nio.file.Path

object CleanUpCore {

    var TAG: String = Configs.universalTAG + "CleanUpCore"

    @JvmStatic
    fun removeTemporaryFiles(projectID: String?): Boolean {
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID))
            Log.i(
                TAG, "Cleaned: " + FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/"
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Clean failed: " + e.message, e)
            return false
        }
    }

    @JvmStatic
    fun cleanUpAfterBuildInDesign(projectID: String?) {
        for (f in FilesTools.getFilesList(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID).orEmpty()) {
            if (!f.name.equals("bin")) {
                FilesTools.deleteFileOrDirectory(Path.of(f.absolutePath))
            }
        }

        if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/bin")) {
            for (f in FilesTools.getFilesList(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/bin").orEmpty()) {
                if (!f.name.endsWith(".apk")) {
                    FilesTools.deleteFileOrDirectory(Path.of(f.absolutePath))
                }
            }
        }
    }

    @JvmStatic
    fun cleanAfterExitDesign(projectID: String?): Boolean {
        try {
            FilesTools.deleteFileOrDirectory(Path.of(ToolCore.getTempImageFilePath(projectID)))
            Log.i(
                TAG, "removeTemporaryFilesAfterExitDesign: " + ToolCore.getTempImageFilePath(Configs.currentProjectID)
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "removeTemporaryFilesAfterExitDesign: " + e.message, e)
            return false
        }
    }
}