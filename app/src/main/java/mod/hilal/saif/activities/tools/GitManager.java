package mod.hilal.saif.activities.tools;

import android.content.Context;
import android.util.Log;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.RefSpec;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.HashMap;

import a.a.a.lC;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import mod.hey.studios.project.backup.BackupFactory;

public class GitManager {

    private static final String TAG = "GitManager";

    public static boolean hasGitRepo(String sc_id) {
        if (sc_id == null || sc_id.isEmpty()) return false;
        File gitDir = getGitDir(sc_id);
        File dotGit = new File(gitDir, ".git");
        return gitDir.exists() && dotGit.exists();
    }

    public static File getGitDir(String sc_id) {
        return new File(FileUtils.getInternalStorageDir(), Configs.gitFolderDir + sc_id);
    }

    public static void initGitRepo(String sc_id, String branch) throws Exception {
        File gitDir = getGitDir(sc_id);
        if (!gitDir.exists()) {
            gitDir.mkdirs();
        }
        File dotGit = new File(gitDir, ".git");
        if (!dotGit.exists()) {
            String initialBranch = (branch == null || branch.trim().isEmpty()) ? "main" : branch.trim();
            Git.init().setDirectory(gitDir).setInitialBranch(initialBranch).call();
        }
    }

    public static void forcePush(Context context, String sc_id, String branch, ProgressMonitor progressMonitor) throws Exception {
        File gitDir = getGitDir(sc_id);
        String target = (branch == null || branch.trim().isEmpty()) ? "main" : branch.trim();

        try (Git git = Git.open(gitDir)) {
            var pushCmd = git.push().setForce(true).setRefSpecs(new RefSpec("+HEAD:refs/heads/" + target));

            if (progressMonitor != null) {
                pushCmd.setProgressMonitor(progressMonitor);
            }

            String username = GitConfiguration.getUsername(context);
            String password = GitConfiguration.getPassword(context);
            if (!username.isEmpty() && !password.isEmpty()) {
                pushCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            pushCmd.call();
        }
    }

    /**
     * Copies Sketchware project data & resources into the git repository folder
     * maintaining .swb backup folder structure (.sketchware/git/<sc_id>/data, resources, project)
     */
    public static void syncProjectToGitDir(String sc_id) {
        try {
            File gitDir = getGitDir(sc_id);
            if (!gitDir.exists()) {
                gitDir.mkdirs();
            }

            // 1. Data folder
            File srcData = new File(FileUtils.getInternalStorageDir(), Configs.projectDataFolderDir + sc_id);
            File destData = new File(gitDir, "data");
            if (srcData.exists()) {
                BackupFactory.copySafe(srcData, destData);
            }

            // 2. Resources folder
            File srcRes = new File(FileUtils.getInternalStorageDir(), Configs.resFolderDir);
            File destRes = new File(gitDir, "resources");
            String[] subfolders = new String[]{"fonts", "icons", "images", "sounds"};
            for (String sub : subfolders) {
                File srcSub = new File(srcRes, sub + "/" + sc_id);
                File destSub = new File(destRes, sub);
                if (srcSub.exists()) {
                    BackupFactory.copySafe(srcSub, destSub);
                }
            }

            // 3. Project properties file
            File srcProject = new File(FileUtils.getInternalStorageDir(), Configs.projectInfoFolderDir + sc_id + "/project");
            File destProject = new File(gitDir, "project");
            if (srcProject.exists()) {
                BackupFactory.copy(srcProject, destProject);
            }

            // 4. Local libs if any
            File srcLocalLibs = new File(FileUtils.getInternalStorageDir(), Configs.projectLocalLibFolderDir + sc_id);
            File destLocalLibs = new File(gitDir, "local_libs");
            if (srcLocalLibs.exists()) {
                BackupFactory.copySafe(srcLocalLibs, destLocalLibs);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error syncing project to git dir: ", e);
        }
    }

    /**
     * Restores project files from cloned git repository into Sketchware internal directories
     */
    public static boolean restoreProjectFromGitDir(String sc_id) {
        try {
            File gitDir = getGitDir(sc_id);
            if (!gitDir.exists()) return false;

            // 1. Project properties
            File gitProject = new File(gitDir, "project");
            File destProjectDir = new File(FileUtils.getInternalStorageDir(), Configs.projectInfoFolderDir + sc_id);
            destProjectDir.mkdirs();
            File destProject = new File(destProjectDir, "project");

            if (gitProject.exists()) {
                BackupFactory.copy(gitProject, destProject);
                // Update sc_id in encrypted project file if needed
                try {
                    HashMap<String, Object> map = lC.b(sc_id);
                    if (map != null) {
                        map.put("sc_id", sc_id);
                        lC.a(sc_id, map);
                    }
                } catch (Exception ignored) {}
            } else {
                // If project metadata doesn't exist in repo, create a basic default project file
                HashMap<String, Object> map = new HashMap<>();
                map.put("sc_id", sc_id);
                map.put("my_ws_name", "ClonedProject_" + sc_id);
                map.put("my_app_name", "Cloned App");
                map.put("my_sc_pkg_name", "com.my.cloned" + sc_id);
                map.put("sc_ver_code", "1");
                map.put("sc_ver_name", "1.0");
                map.put("sketchware_ver", "1.0");
                lC.a(sc_id, map);
            }

            // 2. Data folder
            File gitData = new File(gitDir, "data");
            File destData = new File(FileUtils.getInternalStorageDir(), Configs.projectDataFolderDir + sc_id);
            destData.mkdirs();
            if (gitData.exists()) {
                BackupFactory.copySafe(gitData, destData);
            }

            // 3. Resources folder
            File gitRes = new File(gitDir, "resources");
            String[] subfolders = new String[]{"fonts", "icons", "images", "sounds"};
            for (String sub : subfolders) {
                File gitSub = new File(gitRes, sub);
                File destSub = new File(FileUtils.getInternalStorageDir(), Configs.resFolderDir + sub + "/" + sc_id);
                destSub.mkdirs();
                if (gitSub.exists()) {
                    BackupFactory.copySafe(gitSub, destSub);
                }
            }

            // 4. Local libs
            File gitLocalLibs = new File(gitDir, "local_libs");
            File destLocalLibs = new File(FileUtils.getInternalStorageDir(), Configs.projectLocalLibFolderDir + sc_id);
            if (gitLocalLibs.exists()) {
                destLocalLibs.mkdirs();
                BackupFactory.copySafe(gitLocalLibs, destLocalLibs);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error restoring project from git dir: ", e);
            return false;
        }
    }

    public static Status getStatus(String sc_id) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            return git.status().call();
        }
    }

    public static void commit(Context context, String sc_id, String message) throws Exception {
        File gitDir = getGitDir(sc_id);
        syncProjectToGitDir(sc_id);

        try (Git git = Git.open(gitDir)) {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();

            String name = GitConfiguration.getUserName(context);
            String email = GitConfiguration.getUserEmail(context);
            if (name.isEmpty()) name = "Sketchware User";
            if (email.isEmpty()) email = "user@sketchware.app";

            git.commit()
                    .setMessage(message)
                    .setAuthor(name, email)
                    .setCommitter(name, email)
                    .call();
        }
    }

    public static void push(Context context, String sc_id, ProgressMonitor progressMonitor) throws Exception {
        push(context, sc_id, null, progressMonitor);
    }

    public static void push(Context context, String sc_id, String branch, ProgressMonitor progressMonitor) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            var pushCmd = git.push();
            if (branch != null && !branch.trim().isEmpty()) {
                pushCmd.setRefSpecs(new org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/" + branch.trim()));
            }
            if (progressMonitor != null) {
                pushCmd.setProgressMonitor(progressMonitor);
            }

            String username = GitConfiguration.getUsername(context);
            String password = GitConfiguration.getPassword(context);
            if (!username.isEmpty() && !password.isEmpty()) {
                pushCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            pushCmd.call();
        }
    }

    public static void pull(Context context, String sc_id, ProgressMonitor progressMonitor) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            var pullCmd = git.pull();
            if (progressMonitor != null) {
                pullCmd.setProgressMonitor(progressMonitor);
            }

            String username = GitConfiguration.getUsername(context);
            String password = GitConfiguration.getPassword(context);
            if (!username.isEmpty() && !password.isEmpty()) {
                pullCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            pullCmd.call();
            restoreProjectFromGitDir(sc_id);
        }
    }

    public static void cloneRepo(Context context, String repoUrl, String branch, File targetDir, ProgressMonitor progressMonitor) throws Exception {
        CloneCommand cloneCmd = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir);

        if (branch != null && !branch.trim().isEmpty()) {
            cloneCmd.setBranch(branch.trim());
        }

        if (progressMonitor != null) {
            cloneCmd.setProgressMonitor(progressMonitor);
        }

        String username = GitConfiguration.getUsername(context);
        String password = GitConfiguration.getPassword(context);
        if (!username.isEmpty() && !password.isEmpty()) {
            cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        }

        try (Git git = cloneCmd.call()) {
            Log.i(TAG, "Cloned successfully to " + targetDir.getAbsolutePath());
        }
    }

    public static void addRemoteOrigin(String sc_id, String repoUrl) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            try {
                git.remoteRemove().setRemoteName("origin").call();
            } catch (Exception ignored) {}
            git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(repoUrl)).call();
        }
    }
}