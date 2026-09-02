package extensions.anbui.daydream.settings;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.json.JsonUtils;

public class SkSettings {

    public static String TAG = Configs.universalTAG + "SkSettings";

    public static String getBackupDir() {
        Map<String, Object> map = getSettingsData();
        if (map == null) return "/.sketchware/backups/";
        if (!map.containsKey("backup-dir")) return "/.sketchware/backups/";

        Log.i(TAG, "getBackupDir: " + map.get("backup-dir"));
        return Objects.requireNonNull(map.get("backup-dir")).toString();
    }

    public static void setDataString(String key, String value) {
        Log.i(TAG, "setDataString: " + key + " " + value);
        JsonObject json = JsonParser.parseString(readDataFile()).getAsJsonObject();
        json.addProperty(key, value);
        writeDataFile(new Gson().toJson(json));
    }

    public static Map<String, Object> getSettingsData() {
        Log.i(TAG, "getSettingsData");
        return JsonUtils.covertoMap(readDataFile());
    }

    public static String readDataFile() {
        String contentProjectFile = FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + "/settings.json");
        if (contentProjectFile.isEmpty()) contentProjectFile = "{}";
        Log.i(TAG, "readDataFile: " + contentProjectFile);
        return contentProjectFile;
    }

    public static void writeDataFile(String content) {
        Log.i(TAG, "writeDataFile: " + content);
        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + "/settings.json", content);
    }
}
