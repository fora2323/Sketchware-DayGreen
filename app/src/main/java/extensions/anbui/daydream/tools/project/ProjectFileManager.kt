package extensions.anbui.daydream.tools.project

import android.app.Activity
import android.util.Log
import android.widget.TextView
import androidx.core.net.toUri
import com.besome.sketch.export.ExportSource
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.file.FilesTools
import extensions.anbui.daydream.json.JsonUtils
import java.nio.file.Path

object ProjectFileManager {

    const val TAG: String = "ProjectFileManager"

    //Add ? to ignore warning when using null.
    @JvmStatic
    fun copyData(projectID: String, srcDir: String, destDir: String, statusTextView: TextView?, customBlocks: Boolean, includeGit: Boolean) {
        Log.i(TAG, "projectData: $projectID")

        FileUtils.createDirectory(destDir)

        val fileList = arrayListOf<String>()
        FileUtils.getFileListInDirectory(srcDir, fileList)

        fileList.forEach { filepath ->
            val fileName = filepath.toUri().lastPathSegment ?: return@forEach

            val shoudCopy = when (fileName) {
                "DataDayDreamGit.json" -> includeGit
                "custom_blocks" -> customBlocks
                else -> true
            }

            if (shoudCopy) {
                statusTextView?.let {
                    updateStatus(it, "Copying $fileName")
                }

                try {
                    FilesTools.startCopy(filepath, destDir)
                } catch (e: Exception) {
                    Log.e(TAG, "projectData: ${e.message}")
                }
            } else {
                Log.i(TAG, "projectData: Skip $fileName")
            }
        }
    }

    @JvmStatic
    fun copyResources(projectID: String, srcDir: String, destDir: String, statusTextView: TextView?) {
        Log.i(TAG, "resources: $projectID")

        FileUtils.createDirectory(destDir)

        try {
            statusTextView?.let {
                updateStatus(it, "Copying resources...")
            }
            FilesTools.copyFileOrDirectory(Path.of(srcDir), Path.of(destDir))
        } catch (e: Exception) {
            Log.e(TAG, "resources: ${e.message}")
        }
    }

    @JvmStatic
    fun copyProperties(projectID: String, srcDir: String, destDir: String, statusTextView: TextView?) {
        Log.i(TAG, "properties: $projectID")

        FileUtils.createDirectory(destDir)

        try {
            statusTextView?.let {
                updateStatus(it, "Copying properties...")
            }
            FilesTools.copyFileOrDirectory(Path.of(srcDir), Path.of(destDir))
        } catch (e: Exception) {
            Log.e(TAG, "properties: ${e.message}")
        }
    }

    @JvmStatic
    fun copyLocalLibrary(projectID: String, srcDir: String, destDir: String, isCopyAll : Boolean, statusTextView: TextView?) {
        Log.i(TAG, "localLibrary: $projectID")

        FileUtils.createDirectory(destDir)

        var usingLocalLibs = ""
        if (!isCopyAll) {
            usingLocalLibs = FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/local_library")
            if (!isUsingAnyLocalLibrary(projectID)) return
        }

        val fileList = arrayListOf<String>()
        FileUtils.getFileListInDirectory(srcDir, fileList)

        fileList.forEach { filepath ->
            val fileName = filepath.toUri().lastPathSegment ?: return@forEach

            if (isCopyAll || usingLocalLibs.contains(fileName)) {
                statusTextView?.let {
                    updateStatus(it, "Copying $fileName")
                }

                try {
                    FilesTools.startCopy(filepath, destDir)
                } catch (e: Exception) {
                    Log.e(TAG, "localLibrary: ${e.message}")
                }
            } else {
                Log.i(TAG, "localLibrary: Skip $fileName")
            }
        }
    }

    @JvmStatic
    fun copySourceCode(activity: Activity, projectID: String, srcDir : String, destDir: String, iskeepGradleFiles : Boolean, statusTextView: TextView?) {
        Log.i(TAG, "copySourceCode: $projectID")
        ExportSource.startExport(activity, projectID, statusTextView)

        if (!iskeepGradleFiles) {
            try {
                FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/build.gradle"))
                FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/gradle.properties"))
                FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/settings.gradle"))
                FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID + "/app/build.gradle"))
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Delete Gradle files error: ", e)
            }
        }

        FileUtils.createDirectory(destDir)

        val fileList = arrayListOf<String>()
        FileUtils.getFileListInDirectory(srcDir, fileList)

        fileList.forEach { filepath ->
            val fileName = filepath.toUri().lastPathSegment ?: return@forEach

            val shoudCopy = when (fileName) {
                "bin" -> false
                "gen" -> false
                else -> true
            }

            if (shoudCopy) {
                statusTextView?.let {
                    updateStatus(it, "Copying $fileName")
                }

                try {
                    FilesTools.startCopy(filepath, destDir)
                } catch (e: Exception) {
                    Log.e(TAG, "copySourceCode: ${e.message}")
                }
            } else {
                Log.i(TAG, "copySourceCode: Skip $fileName")
            }
        }
    }

    @JvmStatic
    fun isUsingAnyLocalLibrary(projectID: String) : Boolean {
        if (!FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/local_library")) return false
        val content = FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/local_library")
        return JsonUtils.isValidData(content) && !JsonUtils.isListMapEmpty(content)
    }

    //Add ? to ignore warning when using null.
    @JvmStatic
    fun updateStatus(textView: TextView?, msg: String) {
        textView?.let {
            if (it.context == null) return
            it.post {
                (it.context as Activity).runOnUiThread {
                    it.text = msg
                }
            }
        }
    }
}