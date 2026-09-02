package extensions.anbui.daydream.tools.project;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.project.GetProjectInfo;
import extensions.anbui.daydream.project.ProjectDecryptor;
import extensions.anbui.daydream.secret.SecretUtils;
import extensions.anbui.daydream.settings.SkSettings;

import net.lingala.zip4j.ZipFile;

public class BackupCore {

    public static String TAG = Configs.universalTAG + "BackupCore";
    public static String backedupFilePath = "";

    public static boolean backup(String projectID, TextView statusTextView, String backupFileName, boolean locallibs, boolean customblocks, boolean includeApis, boolean includeGit) {
        Log.i(TAG, "backup: " + projectID);
        boolean result = true;
        try {
            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir));
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
        ProjectFileManager.copyData(projectID,
                FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID,
                FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "data/",
                statusTextView,
                customblocks,
                includeGit);

        updateStatus(statusTextView, "Copying fonts...");
        ProjectFileManager.copyResources(projectID,
                FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + projectID,
                FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "resources/fonts/",
                null);

        updateStatus(statusTextView, "Copying icons...");
        ProjectFileManager.copyResources(projectID,
                FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + projectID,
                FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "resources/icons/",
                null);

        updateStatus(statusTextView, "Copying images...");
        ProjectFileManager.copyResources(projectID,
                FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + projectID,
                FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "resources/images/",
                null);

        updateStatus(statusTextView, "Copying sounds...");
        ProjectFileManager.copyResources(projectID,
                FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + projectID,
                FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "resources/sounds/",
                null);

        if (locallibs)
            ProjectFileManager.copyLocalLibrary(projectID,
                    FileUtils.getInternalStorageDir() + Configs.projectLocalLibFolderDir,
                    FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "local_libs/",
                    false,
                    statusTextView);

        updateStatus(statusTextView, "Copying project info...");
        ProjectFileManager.copyProperties(projectID,
                FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID + "/project",
                FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir,
                null);

        if (!includeApis)
            ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir + "data/library", SecretUtils.hideLibrary(projectID));

        String fileName = backupFileName + ".swb";
        if (backupFileName.isEmpty())
            fileName = GetProjectInfo.getProjectName(projectID) + "-" + GetProjectInfo.getVersionName(projectID) + "-" + GetProjectInfo.getVersionCode(projectID) + "-" + System.currentTimeMillis() / 1000L + ".swb";

        String backupDir = FileUtils.getInternalStorageDir() + SkSettings.getBackupDir() + GetProjectInfo.getProjectName(projectID) + "/";
        if(!FileUtils.createDirectory(backupDir)) result = false;
        try {
            if (result) {
                updateStatus(statusTextView, "Creating backup...");
                ZipFile zipFile = new ZipFile(backupDir + fileName);
                ArrayList<String> filelist = new ArrayList<>();
                FileUtils.getFileListInDirectory(
                        FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir,
                        filelist
                );
                List<File> files = new ArrayList<>();
                for (String path : filelist) {
                    File currentfile = new File(path);
                    if (currentfile.isDirectory()) {
                        zipFile.addFolder(currentfile);
                    } else {
                        files.add(currentfile);
                    }
                }

                if (!files.isEmpty()) {
                    zipFile.addFiles(files);
                }
            }
            backedupFilePath = backupDir + fileName;
            Log.i(TAG, "backup: " + backedupFilePath);
        } catch (Exception e) {
            result = false;
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
        updateStatus(statusTextView, "Cleaning up...");
        try {
        FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.tempBackupDayDreamFolderDir));
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
        return result;
    }

    private static void updateStatus(TextView statusTextView, String msg) {
        Log.i(TAG, "updateStatus: " + msg);
        if (statusTextView == null || statusTextView.getContext() == null) return;
        ((Activity) statusTextView.getContext()).runOnUiThread(() -> statusTextView.setText(msg));
    }
}
