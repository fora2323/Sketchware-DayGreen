package extensions.anbui.daydream.library;

import android.util.Log;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.project.ProjectBuildConfigs;
import extensions.anbui.daydream.project.ProjectConfigs;
import extensions.anbui.daydream.project.ProjectLibrary;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;

public class LibraryUtils {

    public static String TAG = Configs.universalTAG + "LibraryUtils";

    public static boolean isAllowUseEdgeToEdge(String projectID) {
        Log.i(TAG, "isAllowUseWindowInsetsHandling: " + projectID);
        return ProjectLibrary.isEnabledAppCompat(projectID);
    }

    public static boolean isAllowUseWindowInsetsHandling(String projectID) {
        Log.i(TAG, "isAllowUseWindowInsetsHandling: " + projectID);
        return (ProjectLibrary.isEnabledAppCompat(projectID)
                && !ProjectBuildConfigs.isUseJava7(projectID));
    }

    public static boolean isAllowUseAndroidXWorkManager(String projectID) {
        Log.i(TAG, "isAllowUseAndroidXWorkManager: " + projectID);
        return ProjectLibrary.isEnabledAppCompat(projectID);
    }

   public static boolean isAllowUseAndroidXMedia3(String projectID) {
       Log.i(TAG, "isAllowUseAndroidXMedia3: " + projectID);
       return (ProjectLibrary.isEnabledAppCompat(projectID)
               && ProjectConfigs.isMinSDKNewerThan23(projectID)
               && !ProjectBuildConfigs.isUseJava7(projectID));
   }

    public static boolean isAllowUseAndroidXBrowser(String projectID) {
        Log.i(TAG, "isAllowUseAndroidXBrowser: " + projectID);
        return ProjectLibrary.isEnabledAppCompat(projectID);
    }

    public static boolean isAllowUseAndroidXCredentialManager(String projectID) {
        Log.i(TAG, "isAllowUseAndroidXCredentialManager: " + projectID);
        return (ProjectLibrary.isEnabledAppCompat(projectID)
                && ProjectConfigs.isMinSDKNewerThan23(projectID)
                && !ProjectBuildConfigs.isUseJava7(projectID));
    }

    public static boolean isAllowUseGoogleAnalytics(String projectID) {
        Log.i(TAG, "isAllowUseGoogleAnalytics: " + projectID);
        return (ProjectLibrary.isEnabledAppCompat(projectID)
                && ProjectConfigs.isMinSDKNewerThan23(projectID)
                && !ProjectBuildConfigs.isUseJava7(projectID)
                && ProjectLibrary.isEnabledFirebase(projectID));
    }

    public static boolean isAllowUseOneSignal(String projectID) {
        Log.i(TAG, "isAllowUseOneSignal: " + projectID);
        return (ProjectLibrary.isEnabledAppCompat(projectID)
                && ProjectLibrary.isEnabledFirebase(projectID));
    }

    public static boolean isAllowUseShizuku(String projectID) {
        Log.i(TAG, "isAllowUseShizuku: " + projectID);
        return (ProjectLibrary.isEnabledAppCompat(projectID)
                && ProjectConfigs.isMinSDKNewerThan23(projectID));
    }

    public static boolean isAllowUseAndroidBilling(String projectID) {
        Log.i(TAG, "isAllowUseAndroidBilling: " + projectID);
        return (ProjectLibrary.isEnabledAppCompat(projectID)
                && ProjectLibrary.isEnabledFirebase(projectID));
    }

    public static boolean isAllowUseTheme(String projectID) {
        Log.i(TAG, "isAllowUseTheme");
        return ProjectLibrary.isEnabledAppCompat(projectID);
    }

    public static boolean isAllowUseDynamicColor(String projectID) {
        Log.i(TAG, "isAllowUseTheme");
        return DayDreamProjectSettings.getTheme(projectID) > Configs.material2Theme;
    }
}
