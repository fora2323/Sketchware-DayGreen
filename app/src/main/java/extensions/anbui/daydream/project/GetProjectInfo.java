package extensions.anbui.daydream.project;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;

public class GetProjectInfo {

    public static String TAG = Configs.universalTAG + "GetProjectInfo";

    @NonNull
    public static String getProjectName(String projectID) {
        Log.i(TAG, "getProjectName: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("my_ws_name")).toString();
    }

    @NonNull
    public static String getDateCreated(String projectID) {
        Log.i(TAG, "getDateCreated: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("my_sc_reg_dt")).toString();
    }

    @NonNull
    public static String getSketchwareVersionCode(String projectID) {
        Log.i(TAG, "getSketchwareVersionCode: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("sketchware_ver")).toString();
    }

    public static int getSketchwareVersionCodeInt(String projectID) {
        Log.i(TAG, "getSketchwareVersionCodeInt: " + projectID);
        return Integer.parseInt(Objects.requireNonNull(ProjectFileInfo.read(projectID).get("sketchware_ver")).toString());
    }

    @NonNull
    public static String getAppName(String projectID) {
        Log.i(TAG, "getAppName: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("my_app_name")).toString();
    }

    @NonNull
    public static String getVersionName(String projectID) {
        Log.i(TAG, "getVersionName: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("sc_ver_name")).toString();
    }

    @NonNull
    public static String getVersionCode(String projectID) {
        Log.i(TAG, "getVersionCode: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("sc_ver_code")).toString();
    }

    @NonNull
    public static String getPackageName(String projectID) {
        Log.i(TAG, "getPackageName: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("my_sc_pkg_name")).toString();
    }

    public static double getAccentColor(String projectID) {
        Log.i(TAG, "getAccentColor: " + projectID);
        return (double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_accent"));
    }

    public static int getAccentHexColor(String projectID) {
        Log.i(TAG, "getAccentHexColor: " + projectID);
        return ((Double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_accent"))).intValue();
    }

    public static double getPrimaryColor(String projectID) {
        Log.i(TAG, "getPrimaryColor: " + projectID);
        return (double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_primary"));
    }

    public static int getPrimaryHexColor(String projectID) {
        Log.i(TAG, "getPrimaryHexColor: " + projectID);
        return ((Double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_primary"))).intValue();
    }

    public static double getPrimaryDarkColor(String projectID) {
        Log.i(TAG, "getPrimaryDarkColor: " + projectID);
        return (double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_primary_dark"));
    }

    public static int getPrimaryDarkHexColor(String projectID) {
        Log.i(TAG, "getPrimaryDarkHexColor: " + projectID);
        return ((Double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_primary_dark"))).intValue();
    }

    public static double getControlHighLightColor(String projectID) {
        Log.i(TAG, "getControlHighLightColor: " + projectID);
        return (double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_control_highlight"));
    }

    public static int getControlHighLightHexColor(String projectID) {
        Log.i(TAG, "getControlHighLightHexColor: " + projectID);
        return ((Double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_control_highlight"))).intValue();
    }

    public static double getControlNormalColor(String projectID) {
        Log.i(TAG, "getControlNormalColor: " + projectID);
        return (double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_control_normal"));
    }

    public static int getControlNormalHexColor(String projectID) {
        Log.i(TAG, "getControlNormalHexColor: " + projectID);
        return ((Double) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("color_control_normal"))).intValue();
    }

    public static boolean isCustomIcon(String projectID) {
        Log.i(TAG, "isCustomIcon: " + projectID);
        return (boolean) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("custom_icon"));
    }

    public static boolean isIconAdaptive(String projectID) {
        Log.i(TAG, "isIconAdaptive: " + projectID);
        return (boolean) Objects.requireNonNull(ProjectFileInfo.read(projectID).get("isIconAdaptive"));
    }

    //It can be used for matching.
    public static String getProjectID(String projectID) {
        Log.i(TAG, "getProjectID: " + projectID);
        return Objects.requireNonNull(ProjectFileInfo.read(projectID).get("sc_id")).toString();
    }
}
