package extensions.anbui.daydream.settings;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import extensions.anbui.daydream.file.FileUtils;

public class FilePickerSettings {
    public static String getLastOpenedFolder(Context context) {
        SharedPreferences sp = context.getSharedPreferences("drfilepicker", MODE_PRIVATE);
        String lastOpenedFolder = sp.getString("lastOpenedFolder", "");
        if (lastOpenedFolder.isEmpty() || !FileUtils.isFileExist(lastOpenedFolder))
            lastOpenedFolder = FileUtils.getInternalStorageDir();

        return lastOpenedFolder;
    }

    public static void setLastOpenedFolder(Context context, String path) {
        SharedPreferences sp = context.getSharedPreferences("drfilepicker", MODE_PRIVATE);
        sp.edit().putString("lastOpenedFolder", path).apply();
    }
}
