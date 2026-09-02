package extensions.anbui.daydream.project

import android.util.Log
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.file.FilesTools
import java.nio.file.Path

object ProjectUnsavedData {
    var TAG : String = "ProjectUnsavedData"

    @JvmStatic
    fun renameActivity(projectID : String, oldName : String, newName : String) : Boolean {
        if (oldName == newName) return false

        //Decryp logic.
        val logic : String = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/logic")

        //Return when decryp fails.
        if (logic.isEmpty()) return false

        //Decryp view.
        val view : String = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/view")

        //Return when decryp fails.
        if (view.isEmpty()) return false

        //Decryp file.
        var file: String

        //If file is not in bak, get it from data.
        if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file")) {
            file = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file")
        } else {
            file = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/file")
        }

        //Return when decryp fails.
        if (file.isEmpty()) return false

        //Replace old name with new name
        val logicData : String = logic.replace(ProjectUtils.convertXMLNameToJavaName(oldName, false), ProjectUtils.convertXMLNameToJavaName(newName, false))
        val viewData : String = view.replace("@$oldName", "@$newName")
        val fileData : String = file.replace("\"" + oldName + "\"", "\"" + newName + "\"")

        //Create backup directory.
        if (!FileUtils.createDirectory(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir)) return false

        //Backup logic.
        try {
            FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/logic"), Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir))
        } catch (e : Exception) {
            Log.e(TAG, "renameActivity: ", e)
            return false
        }

        //Backup view.
        try {
            FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/view"), Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir))
        } catch (e : Exception) {
            Log.e(TAG, "renameActivity: ", e)
            return false
        }

        //Backup file.
        try {
            if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file")) {
                FilesTools.copyFileOrDirectory(
                    Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file"),
                    Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir)
                )
            } else {
                FilesTools.copyFileOrDirectory(
                    Path.of(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/file"),
                    Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir)
                )
            }
        } catch (e : Exception) {
            Log.e(TAG, "renameActivity: ", e)
            return false
        }

        //Save logic.
        if (!ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/logic", logicData)) {
            return false
        }

        //Save view.
        if (!ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/view", viewData)) {
            //Restore previous files if this step fails.
            try {
                FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir + "/logic"), Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
            } catch (e : Exception) {
                Log.e(TAG, "renameActivity: ", e)
            }

            return false
        }

        //Save file.
        if (!ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file", fileData)) {
            //Restore previous files if this step fails.
            try {
                FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir + "/logic"), Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
            } catch (e : Exception) {
                Log.e(TAG, "renameActivity: ", e)
            }

            try {
                FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir + "/view"), Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
            } catch (e : Exception) {
                Log.e(TAG, "renameActivity: ", e)
            }

            return false
        }

        //Clean up backup files.
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir))
        } catch (e : Exception) {
            Log.e(TAG, "renameActivity: ", e)
        }

        return true
    }

    @JvmStatic
    fun cloneActivity(projectID : String, currentName : String, newName : String) : Boolean {
        if (currentName == newName) return false

        //Decryp logic.
        val logic : String = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/logic")

        //Return when decryp fails.
        if (logic.isEmpty()) return false


        //Decryp view.
        val view : String = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/view")

        //Return when decryp fails.
        if (view.isEmpty()) return false


        //Decryp file.
        var file: String

        //If file is not in bak, get it from data.
        if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file")) {
            file = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file")
        } else {
            file = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/file")
        }

        //Return when decryp fails.
        if (file.isEmpty()) return false


        //Collect blocks and lines.
        val activityData : String = "\n" + ProjectData.collectAllBlocks(ProjectUtils.convertXMLNameToJavaName(currentName, false), logic)
        val viewData : String = "\n" + ProjectData.collectAllBlocks(currentName, view)
        val fileData : String = ProjectData.collectAllLines("\"" + currentName + "\"", file)

        //Create backup directory.
        if (!FileUtils.createDirectory(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir)) return false

        //Backup logic.
        try {
            FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/logic"), Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir))
        } catch (e : Exception) {
            Log.e(TAG, "cloneActivity: ", e)
            return false
        }

        //Backup view.
        try {
            FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/view"), Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir))
        } catch (e : Exception) {
            Log.e(TAG, "cloneActivity: ", e)
            return false
        }

        //Backup file.
        try {
            if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file")) {
                FilesTools.copyFileOrDirectory(
                    Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file"),
                    Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir)
                )
            } else {
                FilesTools.copyFileOrDirectory(
                    Path.of(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/file"),
                    Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir)
                )
            }
        } catch (e : Exception) {
            Log.e(TAG, "cloneActivity: ", e)
            return false
        }

        //Save logic.
        if (!ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/logic",
                logic + activityData.replace(ProjectUtils.convertXMLNameToJavaName(currentName, false), ProjectUtils.convertXMLNameToJavaName(newName, false)))) return false

        //Save view.
        if (!ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/view",
                view + viewData.replace(currentName, newName))) {

            //Restore previous files if this step fails.
            try {
                FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir + "/logic"), Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
            } catch (e : Exception) {
                Log.e(TAG, "cloneActivity: ", e)
            }

            return false
        }

        //Save file.
        if (!ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID + "/file",
                file.replace(fileData, fileData + "\n" + fileData.replace("\"fileName\":\"$currentName\"",
                    "\"fileName\":\"$newName\""
                )))) {

            //Restore previous files if this step fails.
            try {
                FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir + "/logic"), Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
            } catch (e : Exception) {
                Log.e(TAG, "cloneActivity: ", e)
            }

            try {
                FilesTools.copyFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir + "/view"), Path.of(FileUtils.getInternalStorageDir() + Configs.projectUnsavedDataFolderDir + projectID))
            } catch (e : Exception) {
                Log.e(TAG, "cloneActivity: ", e)
            }

            return false
        }

        //Clean up backup files.
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.tempUnsavedDataDayDreamFolderDir))
        } catch (e : Exception) {
            Log.e(TAG, "cloneActivity: ", e)
        }

        return true

//        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + "logicData.txt", activityData)
//        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + "finalLogicData.txt", logic + activityData.replace(ProjectUtils.convertXMLNameToJavaName(currentName, false), ProjectUtils.convertXMLNameToJavaName(newName, false)))
//
//        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + "viewData.txt", viewData)
//        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + "finalViewData.txt", view + viewData.replace(currentName, newName))
//
//        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + "fileData.txt", fileData)
//        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + "finalFileData.txt", file.replace(fileData, fileData + "\n" + fileData.replace("\"fileName\":\"" + currentName + "\"", "\"fileName\":\"" + newName + "\"")))
    }
}