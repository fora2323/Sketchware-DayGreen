package extensions.anbui.daydream.project;

import android.util.Log;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;

public class ProjectFileInfo {

    public static String TAG = Configs.universalTAG + "ProjectFileInfo";

    //Read project file and convert data to Map.
    public static Map<String, Object> read(String projectID) {
        String contentProjectFile = ProjectDecryptor.decryptProjectFile(FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID + "/project");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Log.i(TAG, "read: " + projectID + " " + contentProjectFile);
        return gson.fromJson(contentProjectFile, type);
    }
}
