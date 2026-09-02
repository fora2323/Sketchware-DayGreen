package extensions.anbui.daydream.file;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class FilesTools {
    public static String TAG = "FilesTools";

    //Copy folder
    public static void startCopy(String source, String target) throws IOException {
        String finalTarget = target;
        Path source1 = Path.of(source);
        if (Files.isDirectory(source1)) {
            finalTarget = target + "/" + source1.getFileName();
        }
        copyFileOrDirectory(source1, Path.of(finalTarget));
    }

    //Copy the contents inside the folder
    public static void copyFileOrDirectory(Path source, Path target) throws IOException {

        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        if (Files.isDirectory(source)) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                    Path newDir = target.resolve(source.relativize(dir));
                    try {
                        Files.copy(dir, newDir, StandardCopyOption.REPLACE_EXISTING);
                    } catch (FileAlreadyExistsException e) {
                        System.out.println("Directory already exists: " + newDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else if (Files.isRegularFile(source)) {
            Path targetFile = target;
            if (Files.isDirectory(target)) {
                targetFile = target.resolve(source.getFileName());
            }
            Files.copy(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IOException("Source is neither a directory nor a file: " + source);
        }
    }

    public static void moveRecursively(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        // If target does not exist, create parent directory.
        if (Files.notExists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }

        // If source is file -> move directly.
        if (Files.isRegularFile(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        // If source is a directory, iterate over all child files and move.
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);         // Relative path.
                Path targetDir = target.resolve(relative);      // New path.
                if (Files.notExists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path targetFile = target.resolve(relative);
                Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        // Delete the original directory after moving.
        Files.delete(source);
    }

    public static void deleteFileOrDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;

        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(path);
        }
    }

    public static void openFolder(Activity activity, String folderPath) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            Log.e(TAG, "openFolder: Folder not found!");
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".provider",
                folder
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openFolder: " + e.getMessage());
        }
    }

    public static void openFolderSAF(Activity activity, String folderPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(folderPath));
        intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openFolderSAF: " + e.getMessage());
        }
    }

    public static boolean isPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isPermissionGranted30(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static String getFileName(String path) {
        File file = new File(path);
        return file.getName();
    }

    public static File[] getFilesList(String path) {
        return new File(path).listFiles();
    }
}
