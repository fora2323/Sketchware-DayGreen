package extensions.anbui.daydream.project;

import android.util.Log;

import java.util.Map;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;

public class ProjectLibrary {

    public static String TAG = Configs.universalTAG + "ProjectLibrary";

    public static boolean isEnabledFirebase(String projectID) {
        Log.i(TAG, "isEnabledFirebase: " + projectID);
        Map<String, Object> map = getFirebaseSettingsData(projectID);
        if (map == null) return false;

        return Objects.equals(map.get("useYn"), "Y");
    }

    public static boolean isEnabledAppCompat(String projectID) {
        Log.i(TAG, "isEnabledAppCompat: " + projectID);
        Map<String, Object> map = getAppCompatSettingsData(projectID);
        if (map == null) return false;

        return Objects.equals(map.get("useYn"), "Y");
    }

    public static boolean isEnabledAdmob(String projectID) {
        Log.i(TAG, "isEnabledAdmob: " + projectID);
        Map<String, Object> map = getAdmobSettingsData(projectID);
        if (map == null) return false;

        return Objects.equals(map.get("useYn"), "Y");
    }

    public static boolean isEnabledGoogleMap(String projectID) {
        Log.i(TAG, "isEnabledGoogleMap: " + projectID);
        Map<String, Object> map = getGoogleMapSettingsData(projectID);
        if (map == null) return false;

        return Objects.equals(map.get("useYn"), "Y");
    }

    public static Map<String, Object> getFirebaseSettingsData(String projectID) {
        Log.i(TAG, "getFirebaseSettingsData: " + projectID);
        return readLibraryData(projectID, "@firebaseDB");
    }

    public static Map<String, Object> getAppCompatSettingsData(String projectID) {
        Log.i(TAG, "getAppCompatSettingsData: " + projectID);
        return readLibraryData(projectID, "@compat");
    }

    public static Map<String, Object> getAdmobSettingsData(String projectID) {
        Log.i(TAG, "getAdmobSettingsData: " + projectID);
        return readLibraryData(projectID, "@admob");
    }

    public static Map<String, Object> getGoogleMapSettingsData(String projectID) {
        Log.i(TAG, "getGoogleMapSettingsData: " + projectID);
        return readLibraryData(projectID, "@googleMap");
    }

    public static Map<String, Object> readLibraryData(String projectID, String dataType) {
        String contentProjectFile = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/library");
        Log.i(TAG, "readLibraryData: " + projectID + dataType + contentProjectFile);
        return ProjectData.readFirstLineDataWithDataTypeToMap(dataType, contentProjectFile);
    }
}
