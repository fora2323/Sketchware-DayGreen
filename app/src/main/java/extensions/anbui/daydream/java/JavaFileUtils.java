package extensions.anbui.daydream.java;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import pro.sketchware.utility.FileUtil;

public class JavaFileUtils {

    public static String TAG = Configs.universalTAG + "JavaFileUtils";
    public static boolean isAdded = false;

    //Add files to project's java directory
    public static void addJavaFileToProject(String projectID, String fileName, String Content) {

        isAdded = false;

        File dir = new File(FileUtil.getExternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/java/" + fileName);
        if (!Objects.requireNonNull(dir.getParentFile()).exists()) {
            if (!dir.getParentFile().mkdirs()) return;
        }

        try (FileWriter writer = new FileWriter(FileUtil.getExternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/java/" + fileName)) {
            writer.write(Content);
            isAdded = true;
            Log.i(TAG, "addJavaFileToProject: " + fileName);
        } catch (IOException e) {
            Log.e(TAG, "addJavaFileToProject: " + e.getMessage());
        }
    }

    //Check if Java exists in the project's java directory.
    public static boolean isJavaFileExistInProject(String projectID, String fileName) {
        File dir = new File(FileUtil.getExternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/java/" + fileName);
        return dir.exists();
    }

    //Add files to project's java/lab directory.
    public static void addJavaFileToProjectLab(String projectID, String fileName, String Content) {

        isAdded = false;

        File dir = new File(FileUtil.getExternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/java/lab/");
        if (!dir.exists()) {
            if (!dir.mkdirs()) return;
        }

        try (FileWriter writer = new FileWriter(FileUtil.getExternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/java/lab/" + fileName)) {
            writer.write(Content);
            isAdded = true;
            Log.i(TAG, "addJavaFileToProjectLab: " + fileName);
        } catch (IOException e) {
            Log.e(TAG, "addJavaFileToProjectLab: " + e.getMessage());
        }
    }

    //Check if Java exists in the project's java/lab directory.
    public static boolean isJavaFileExistInProjectLab(String projectID, String fileName) {
        Log.i(TAG, "isJavaFileExistInProjectLab: " + fileName);
        File dir = new File(FileUtil.getExternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/java/lab/" + fileName);
        return dir.exists();
    }
}
