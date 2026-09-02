package extensions.anbui.daydream.secret;

import android.util.Log;

import extensions.anbui.daydream.project.ProjectLibrary;

public class SecretUtils {
    public static String TAG = "SecretUtils";

    public static String hideLibrary(String projectID) {
        Log.i(TAG, "replaceLibraryData: " + projectID);
        String result = replaceFirebaseLibrary(ProjectLibrary.isEnabledFirebase(projectID)) + "\n";
        result += replaceAppCompatLibrary(ProjectLibrary.isEnabledAppCompat(projectID)) + "\n";
        result += replaceAdmobLibrary(ProjectLibrary.isEnabledAdmob(projectID)) + "\n";
        result += replaceGoogleMapLibrary(ProjectLibrary.isEnabledGoogleMap(projectID));
        return result;
    }

    public static String replaceFirebaseLibrary(boolean isUsingFirebase) {
        Log.i(TAG, "replaceFirebaseLibraryData: " + isUsingFirebase);
        if (isUsingFirebase) {
            return "@firebaseDB\n" +
                    "{\"adUnits\":[],\"data\":\"xxxxx-xxxxx-default-rtdb.firebaseio.com\",\"libType\":0,\"reserved1\":\"0:0000000000000:android:0000000000000000000000\",\"reserved2\":\"0000000000000000000000000000000000000\",\"reserved3\":\"xxxxx-xxxxx.appspot.com\",\"testDevices\":[],\"useYn\":\"Y\"}";
        } else {
            return "@firebaseDB\n" +
                    "{\"adUnits\":[],\"appId\":\"\",\"configurations\":{},\"data\":\"\",\"libType\":0,\"reserved1\":\"\",\"reserved2\":\"\",\"reserved3\":\"\",\"testDevices\":[],\"useYn\":\"N\"}";
        }
    }

    public static String replaceAppCompatLibrary(boolean isUsingAppCompat) {
        Log.i(TAG, "replaceAppCompatLibraryData: " + isUsingAppCompat);
        if (isUsingAppCompat) {
            return "@compat\n" +
                    "{\"adUnits\":[],\"appId\":\"\",\"configurations\":{},\"data\":\"\",\"libType\":1,\"reserved1\":\"\",\"reserved2\":\"\",\"reserved3\":\"\",\"testDevices\":[],\"useYn\":\"Y\"}";
        } else {
            return "@compat\n" +
                    "{\"adUnits\":[],\"appId\":\"\",\"configurations\":{},\"data\":\"\",\"libType\":1,\"reserved1\":\"\",\"reserved2\":\"\",\"reserved3\":\"\",\"testDevices\":[],\"useYn\":\"N\"}";
        }
    }

    public static String replaceAdmobLibrary(boolean isUsingAdmob) {
        Log.i(TAG, "replaceAdmobLibraryData: " + isUsingAdmob);
        if (isUsingAdmob) {
            return "@admob\n" +
                    "{\"adUnits\":[{\"id\":\"ca-app-pub-0000000000000000/0000000000\",\"name\":\"bn\"},{\"id\":\"ca-app-pub-0000000000000000/0000000000\",\"name\":\"tg\"},{\"id\":\"ca-app-pub-0000000000000000/0000000000\",\"name\":\"ctt\"}],\"appId\":\"ca-app-pub-0000000000000000~0000000000\",\"data\":\"\",\"libType\":2,\"reserved1\":\"bn : ca-app-pub-0000000000000000/0000000000\",\"reserved2\":\"tg : ca-app-pub-0000000000000000/0000000000\",\"reserved3\":\"ctt : ca-app-pub-0000000000000000/0000000000\",\"testDevices\":[],\"useYn\":\"Y\"}";
        } else {
            return "@admob\n" +
                    "{\"adUnits\":[],\"appId\":\"\",\"configurations\":{},\"data\":\"\",\"libType\":2,\"reserved1\":\"\",\"reserved2\":\"\",\"reserved3\":\"\",\"testDevices\":[],\"useYn\":\"N\"}";
        }
    }

    public static String replaceGoogleMapLibrary(boolean isUsingGoogleMap) {
        Log.i(TAG, "replaceGoogleMapLibraryData: " + isUsingGoogleMap);
        if (isUsingGoogleMap) {
            return "@googleMap\n" +
                    "{\"adUnits\":[],\"appId\":\"\",\"configurations\":{},\"data\":\"AIzaSyA-00000000000000000000000000000\\n\",\"libType\":3,\"reserved1\":\"\",\"reserved2\":\"\",\"reserved3\":\"\",\"testDevices\":[],\"useYn\":\"Y\"}";
        } else {
            return "@googleMap\n" +
                    "{\"adUnits\":[],\"appId\":\"\",\"configurations\":{},\"data\":\"\",\"libType\":3,\"reserved1\":\"\",\"reserved2\":\"\",\"reserved3\":\"\",\"testDevices\":[],\"useYn\":\"N\"}";
        }
    }

    public static String hideXML(String projectID) {
        Log.i(TAG, "hideSecrets: " + projectID);
        return """
                <resources>
                \t<integer name="google_play_services_version">12451000</integer>
                \t<string name="firebase_database_url" translatable="false">https://xxxxx-xxxxx-default-rtdb.firebaseio.com</string>
                \t<string name="project_id" translatable="false">xxxxx-xxxxx</string>
                \t<string name="google_app_id" translatable="false">0:0000000000000:android:0000000000000000000000</string>
                \t<string name="google_api_key" translatable="false">0000000000000000000000000000000000000</string>
                \t<string name="google_storage_bucket" translatable="false">xxxxx-xxxxx.appspot.com</string>
                \t<string name="google_maps_key" translatable="false">AIzaSyA-00000000000000000000000000000</string>
                </resources>
                """;
    }
}
