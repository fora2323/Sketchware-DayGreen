package extensions.anbui.daydream.tools;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.project.ProjectDecryptor;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;

public class ToolCore {

    public static String TAG = Configs.universalTAG + "ToolCore";

    public static void copyToTemp(String path) {
        Log.i(TAG, "copyToTemp: " + path);
        FileUtils.copyFile(path, FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir);
    }

    public static String getTempFilePath(String path) {
        Log.i(TAG, "getTempFilePath: " + path);
        return FileUtils.getInternalStorageDir() + Configs.tempDayDreamFolderDir + path;
    }

    public static String getTempImageFilePath(String path) {
        Log.i(TAG, "getTempFilePath: " + path);
        return FileUtils.getInternalStorageDir() + Configs.tempImageDayDreamFolderDir + path;
    }

    public static void cleanOutTheRecyclingBin() {
        Log.i(TAG, "cleanOutTheRecyclingBin");
        try {
            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.recycleBinDayDreamFolderDir));
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    public static int cleanUpTemporaryFiles() {
        int cleaned = 0;
        ArrayList<String> filelist = new ArrayList<>();
        FileUtils.getFileListInDirectory(
                FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir,
                filelist
        );

        for (String filePath : filelist) {
            if (FileUtils.isFileExist(filePath)) {
                if (!filePath.contains("list")) {
                    try {
                        FileUtils.deleteRecursive(new File(filePath));
                    } catch (Exception e) {
                        Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                    }
                    cleaned++;
                }
            }
        }
        Log.i(TAG, "Cleaned: " + cleaned);
        return cleaned;
    }

    public static int cleanupLocalLib() {
        int moved = 0;
        String allUsingLocalLib = getAllUsingLocalLib();

        if (allUsingLocalLib.isEmpty()) return 0;

        ArrayList<String> filelist = new ArrayList<>();
        FileUtils.getFileListInDirectory(
                FileUtils.getInternalStorageDir() + Configs.projectLocalLibFolderDir,
                filelist
        );

        for (String filePath : filelist) {
            if (FileUtils.isFileExist(filePath)) {
                if (!allUsingLocalLib.contains(filePath)) {
                    FileUtils.moveAFile(filePath, FileUtils.getInternalStorageDir() + Configs.recycleBinDayDreamFolderDir + "local_libs/");
                    moved++;
                }
            }
        }
        Log.i(TAG, "Moved: " + moved);
        return moved;
    }

    public static String getAllUsingLocalLib() {
        StringBuilder result = new StringBuilder();
        ArrayList<String> filelist = new ArrayList<>();
        FileUtils.getFileListInDirectory(
                FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir,
                filelist
        );

        for (String filePath : filelist) {
            String localLibPath = filePath + "/local_library";
            if (FileUtils.isFileExist(localLibPath)) {
                String content = FileUtils.readTextFile(localLibPath);
                result.append(content).append("\n");
            }
        }
        Log.i(TAG, "getAllUsingLocalLib: " + result);
        return result.toString();
    }

    public static int getLastID() {
        int result = 0;
        ArrayList<String> filelist = new ArrayList<>();
        FileUtils.getFileListInDirectory(
                FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir,
                filelist
        );

        for (String filePath : filelist) {
            if (FileUtils.isFileExist(filePath)) {
                String folderName = new File(filePath).getName().replaceAll("/", "");
                if (folderName.matches("\\d+") && Integer.parseInt(folderName) > result) {
                    result = Integer.parseInt(folderName);
                }
            }
        }
        Log.i(TAG, "getLastID: " + result);
        return result;
    }

    public static void fixID(String projectID) {
        Log.i(TAG, "fixID: " + projectID);
        String projectInfo = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID + "/project");
        JsonObject json = JsonParser.parseString(projectInfo).getAsJsonObject();
        json.addProperty("sc_id", projectID);
        ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID + "/project", new Gson().toJson(json));
    }

    public static boolean isXMLNameValid(String projectID, String name, String oldName, boolean isCheckInUnsavedData, String viewFileContent) {
        //Check if this name is null, empty, first character is a number, shorter than 3, longer than 25 characters or contains the suffix "_fragment".
        if (name == null ||
                name.isEmpty() ||
                Character.isDigit(name.charAt(0)) ||
                name.length() < 3 ||
                name.length() > 25 ) return false;

        if (oldName != null && !oldName.isEmpty() && !oldName.equals(name) && name.endsWith("_fragment")) {
            String finalSuffix;
            if (name.endsWith("_dialog_fragment")) {
                finalSuffix = "_dialog_fragment";
            } else if (name.endsWith("_bottomdialog_fragment")) {
                finalSuffix = "_bottomdialog_fragment";
            } else {
                finalSuffix = "_fragment";
            }

            if (!oldName.endsWith(finalSuffix)) {
                String daydreamData = DayDreamProjectSettings.getActivityType(projectID, oldName);
                if (!daydreamData.isEmpty()) {
                    if (!finalSuffix.equals(daydreamData)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        //Check contains only a-z, 0-9 and _
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }

        //Check if this name is already taken
        String finalViewFileContent;
        if (viewFileContent != null && !viewFileContent.isEmpty()) {
            finalViewFileContent = viewFileContent;
        } else {
            finalViewFileContent = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir()
                    + (isCheckInUnsavedData ? Configs.projectUnsavedDataFolderDir : Configs.projectDataFolderDir)
                    + projectID + "/view");
        }

        return !finalViewFileContent.contains("@" + name + ".xml");
    }

    public static boolean isFragment(String projectID, String xmlName) {
        String activityType = DayDreamProjectSettings.getActivityType(
                projectID,
                xmlName.replace(".xml", "")
        );
        return xmlName.endsWith("_fragment.xml") || activityType.contains("_fragment");
    }
}
