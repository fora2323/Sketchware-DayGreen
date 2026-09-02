package extensions.anbui.daydream.file;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;

public class FileUtils {

    public static String TAG = Configs.universalTAG + "FileUtils";

    //Get the internal storage directory.
    public static String getInternalStorageDir() {
        File storageDir = Environment.getExternalStorageDirectory();
        Log.i(TAG, "getInternalStorageDir: " + storageDir.getAbsolutePath());
        return storageDir.getAbsolutePath();
    }

    //Is file exist.
    public static boolean isFileExist(String path) {
        Log.i(TAG, "isFileExist: " + path);
        if (path == null || path.isEmpty()) return false;
        File file = new File(path);
        Log.i(TAG, "isFileExist: " + file.exists());
        return file.exists();
    }

    public static boolean createDirectory(String path) {
        Log.i(TAG, "createDirectory: " + path);
        File file = new File(path);
        if (file.exists()) return true;
        return file.mkdirs();
    }

    //Get file path from uri.
    public static String getFilePathFromUri(Context context, Uri uri) {
        String filePath = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    filePath = cursor.getString(index);
                }
                Log.i(TAG, "getFilePathFromUri: " + filePath);
            } catch (Exception e) {
                Log.e(TAG, "getFilePathFromUri: " + e.getMessage());
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
            Log.i(TAG, "getFilePathFromUri: " + filePath);
        }
        return filePath;
    }

    public static String readTextFile(String path) {
        if (!isFileExist(path)) return "";

        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            Log.i(TAG, "readTextFile: " + path);
        } catch (IOException e) {
            Log.e(TAG, "readTextFile: " + e.getMessage());
        }
        return content.toString();
    }

    public static void writeTextFile(String path, String content) {
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) return;
        }

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
            Log.i(TAG, "writeTextFile: " + path);
        } catch (IOException e) {
            Log.e(TAG, "writeTextFile: " + e.getMessage());
        }
    }

    public static void copyDirectory(String sourceDir, String destDir) throws IOException {

        File source = new File(sourceDir);
        File dest = new File(destDir);


//        if (source.isFile()) {
//            copyFile(sourceDir, destDir);
//            return;
//        }

        File[] files = source.listFiles();
        File newFile = new File(destDir);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        for (File file : files) {
            if (file.isFile()) {
                copyFile(file.getPath(), destDir);
            } else if (file.isDirectory()) {
                copyDirectory(file.getAbsolutePath(), dest.getAbsoluteFile() + "/" + file.getName());
            }
        }
    }

    public static void copyFile(String source, String dest) {
        File sourceFile = new File(source);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            Log.e(TAG, "copyFile: Source file does not exist or is not a file: " + source);
            return;
        }

        if (!sourceFile.isFile()) {
            try {
                copyDirectory(source, dest);
            } catch (Exception e) {
                Log.e(TAG, "copyFile: " + e.getMessage());
            }
            return;
        }

        try {
            File destFile = new File(dest);

            if (destFile.exists() && destFile.isDirectory() || dest.endsWith("/")) {
                destFile = new File(destFile, sourceFile.getName());
            } else {
                if (!destFile.exists()) {
                    if(!destFile.mkdirs()) {
                        Log.i(TAG, "copyFile: Create failed directory " + destFile.getAbsolutePath());
                        return;
                    }
                }
            }

            File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                Log.e(TAG, "copyFile: Failed to create parent directory: " + parentDir.getAbsolutePath());
                return;
            }

            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
            Log.i(TAG, "copyFile: " + source + " to " + dest);
        } catch (Exception e) {
            Log.e(TAG, "copyFile: " + e.getMessage());
        }
    }

    public static void moveAFile(String from, String to) {
        File filefrom = new File(from);
        File finalTarget = filefrom.isDirectory() ? new File(to, filefrom.getName()) : new File(to);

        File parentDir = finalTarget.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) return;

        if (filefrom.renameTo(finalTarget)) {
            Log.d(TAG, "Moved " + from + " to " + to);
            return;
        }

        try {
            if (filefrom.isDirectory()) {
                copyDirectory(filefrom.getAbsolutePath(), finalTarget.getAbsolutePath());
                deleteRecursive(filefrom);
            } else {
                copyFile(filefrom.getAbsolutePath(), finalTarget.getAbsolutePath());
                deleteRecursive(filefrom);
            }
            Log.d(TAG, "Moved " + from + " to " + to + ". With copy and delete.");
        } catch (Exception e) {
            Log.e(TAG, "Error moving file: " + e.getMessage());
        }
    }

    public static void deleteFile(String path) {
        if (!isFileExist(path)) return;

        File file = new File(path);
        if (file.delete()) {
            Log.i(TAG, "deleteFile: " + path);
        } else {
            Log.e(TAG, "deleteFile: " + path);
        }
    }

    public static boolean deleteRecursive(File fileOrDir) throws InterruptedException {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }

        boolean deleted = fileOrDir.delete();
        int retries = 5;
        while (!deleted && retries-- > 0) {
            Thread.sleep(50);
            deleted = fileOrDir.delete();
        }

        Log.i(TAG, "deleteRecursive: " + fileOrDir.getAbsolutePath() + " deleted=" + deleted);
        return deleted;
    }


    public static void getFileListInDirectory(String path, ArrayList<String> list) {
        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return;

        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length <= 0) return;

        if (list == null) return;
        list.clear();
        for (File file : listFiles) {
            list.add(file.getAbsolutePath());
        }
        Log.i(TAG, "getFileListInDirectory: " + path);
    }
}
