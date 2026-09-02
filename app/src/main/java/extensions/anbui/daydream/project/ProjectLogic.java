package extensions.anbui.daydream.project;

import android.util.Log;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.tools.ToolCore;

public class ProjectLogic {

    public static String TAG = Configs.universalTAG + "ProjectLogic";

    public static boolean isThisActivityHaveOnBackPressed(String projectID, String activityName) {
        Log.i(TAG, "isThisActivityHaveOnBackPressed: " + projectID + activityName);
        //activityName format: MainActivity
        String result = readActivityData(projectID, ProjectUtils.convertXMLNameToJavaName(activityName, false));
        if (result == null) return false;
        return result.contains("onBackPressed");

    }

    public static String readActivityData(String projectID, String activityName) {
        Log.i(TAG, "readActivityData: " + projectID + activityName);
        return ProjectData.readFullDataWithDataType(activityName, read(projectID));
    }

    public static String read(String projectID) {
        Log.i(TAG, "read: " + projectID);
        ToolCore.copyToTemp(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/logic");
        return ProjectDecryptor.decryptProjectFile(ToolCore.getTempFilePath("logic"));
    }
}
