package mod.hilal.saif.activities.tools;

import android.content.Context;
import android.util.Log;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a.a.a.lC;
import a.a.a.oB;
import a.a.a.vB;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import mod.hey.studios.project.backup.BackupFactory;

public class GitManager {

    private static final String TAG = "GitManager";

    public static class CommitInfo {
        public final String fullHash;
        public final String shortHash;
        public final String message;
        public final String authorName;
        public final long commitTimeMillis;

        public CommitInfo(String fullHash, String shortHash, String message, String authorName, long commitTimeMillis) {
            this.fullHash = fullHash;
            this.shortHash = shortHash;
            this.message = message;
            this.authorName = authorName;
            this.commitTimeMillis = commitTimeMillis;
        }
    }

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
            try (Git git = Git.init().setDirectory(gitDir).setInitialBranch(initialBranch).call()) {
                ensureTrackingConfig(git.getRepository(), initialBranch);
            }
        }
    }

    private static void ensureTrackingConfig(Repository repo, String branch) {
        try {
            if (branch == null || branch.trim().isEmpty()) return;
            StoredConfig config = repo.getConfig();
            boolean changed = false;
            if (config.getString("remote", "origin", "url") != null
                    && config.getString("remote", "origin", "fetch") == null) {
                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                changed = true;
            }
            if (!"origin".equals(config.getString("branch", branch, "remote"))) {
                config.setString("branch", branch, "remote", "origin");
                changed = true;
            }
            String expectedMerge = "refs/heads/" + branch;
            if (!expectedMerge.equals(config.getString("branch", branch, "merge"))) {
                config.setString("branch", branch, "merge", expectedMerge);
                changed = true;
            }
            if (changed) {
                config.save();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update git tracking config", e);
        }
    }

    private static UsernamePasswordCredentialsProvider getCredentials(Context context) {
        String username = GitConfiguration.getUsername(context);
        String password = GitConfiguration.getPassword(context);
        if (!username.isEmpty() && !password.isEmpty()) {
            return new UsernamePasswordCredentialsProvider(username, password);
        }
        return null;
    }

    public static void fetch(Context context, String sc_id) {
        try {
            File gitDir = getGitDir(sc_id);
            if (!hasGitRepo(sc_id)) return;
            try (Git git = Git.open(gitDir)) {
                Repository repo = git.getRepository();
                if (repo.getConfig().getString("remote", "origin", "url") == null) return;

                var fetchCmd = git.fetch()
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
                        .setRemoveDeletedRefs(true);

                var creds = getCredentials(context);
                if (creds != null) {
                    fetchCmd.setCredentialsProvider(creds);
                }
                fetchCmd.call();
            }
        } catch (Exception e) {
            Log.w(TAG, "Background fetch skipped/failed: " + e.getMessage());
        }
    }

    public static void forcePush(Context context, String sc_id, String branch, ProgressMonitor progressMonitor) throws Exception {
        File gitDir = getGitDir(sc_id);
        String target = (branch == null || branch.trim().isEmpty()) ? "main" : branch.trim();

        try (Git git = Git.open(gitDir)) {
            ensureTrackingConfig(git.getRepository(), target);
            var pushCmd = git.push()
                    .setRemote("origin")
                    .setForce(true)
                    .setRefSpecs(new RefSpec("+HEAD:refs/heads/" + target));

            if (progressMonitor != null) {
                pushCmd.setProgressMonitor(progressMonitor);
            }

            var creds = getCredentials(context);
            if (creds != null) {
                pushCmd.setCredentialsProvider(creds);
            }

            pushCmd.call();
            fetch(context, sc_id);
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
     * Restores project files from cloned/pulled git repository into Sketchware internal directories
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
                try {
                    oB fileUtil = new oB();
                    HashMap<String, Object> map = vB.a(fileUtil.a(fileUtil.h(destProject.getAbsolutePath())));
                    if (map != null) {
                        map.put("sc_id", sc_id);
                        lC.a(sc_id, map);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Gagal mengganti sc_id di file project: ", e);
                }
            } else {
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
        commit(context, sc_id, message, false);
    }

    public static void commit(Context context, String sc_id, String message, boolean amend) throws Exception {
        File gitDir = getGitDir(sc_id);
        syncProjectToGitDir(sc_id);

        try (Git git = Git.open(gitDir)) {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();

            String name = GitConfiguration.getUserName(context);
            String email = GitConfiguration.getUserEmail(context);
            if (name.isEmpty()) name = "Sketchware User";
            if (email.isEmpty()) email = "user@sketchware.app";

            git.commit().setMessage(message).setAmend(amend).setAuthor(name, email).setCommitter(name, email).call();
        }
    }

    public static void push(Context context, String sc_id, ProgressMonitor progressMonitor) throws Exception {
        push(context, sc_id, null, progressMonitor);
    }

    public static void push(Context context, String sc_id, String branch, ProgressMonitor progressMonitor) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            Repository repo = git.getRepository();
            String target = (branch == null || branch.trim().isEmpty()) ? repo.getBranch() : branch.trim();
            ensureTrackingConfig(repo, target);

            var pushCmd = git.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("HEAD:refs/heads/" + target));

            if (progressMonitor != null) {
                pushCmd.setProgressMonitor(progressMonitor);
            }

            var creds = getCredentials(context);
            if (creds != null) {
                pushCmd.setCredentialsProvider(creds);
            }

            pushCmd.call();
            fetch(context, sc_id);
        }
    }

    public static void pull(Context context, String sc_id, ProgressMonitor progressMonitor) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            Repository repo = git.getRepository();
            String branch = repo.getBranch();
            if (branch == null || branch.trim().isEmpty()) {
                branch = "main";
            }
            ensureTrackingConfig(repo, branch);

            var creds = getCredentials(context);

            // 1. Fetch terbaru dari origin agar refs/remotes/origin/<branch> selalu up-to-date
            var fetchCmd = git.fetch()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
            if (creds != null) {
                fetchCmd.setCredentialsProvider(creds);
            }
            fetchCmd.call();

            // 2. Bersihkan perubahan working tree hasil auto-sync yang belum di-commit agar tidak bentrok CheckoutConflictException
            if (repo.resolve("HEAD") != null) {
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
            }

            // 3. Jalankan pull dengan target remote & branch eksplisit
            var pullCmd = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branch);
            if (progressMonitor != null) {
                pullCmd.setProgressMonitor(progressMonitor);
            }
            if (creds != null) {
                pullCmd.setCredentialsProvider(creds);
            }

            PullResult result = pullCmd.call();
            if (!result.isSuccessful()) {
                String reason = (result.getMergeResult() != null)
                        ? result.getMergeResult().getMergeStatus().toString()
                        : "Pull merge gagal";
                throw new Exception("Pull gagal: " + reason);
            }

            // 4. Salin hasil pull ke direktori project internal Sketchware
            restoreProjectFromGitDir(sc_id);
        }
    }

    public static void cloneRepo(Context context, String repoUrl, String branch, File targetDir, ProgressMonitor progressMonitor) throws Exception {
        CloneCommand cloneCmd = Git.cloneRepository().setURI(repoUrl).setDirectory(targetDir);

        if (branch != null && !branch.trim().isEmpty()) {
            cloneCmd.setBranch(branch.trim());
        }

        if (progressMonitor != null) {
            cloneCmd.setProgressMonitor(progressMonitor);
        }

        var creds = getCredentials(context);
        if (creds != null) {
            cloneCmd.setCredentialsProvider(creds);
        }

        try (Git git = cloneCmd.call()) {
            ensureTrackingConfig(git.getRepository(), git.getRepository().getBranch());
            Log.i(TAG, "Cloned successfully to " + targetDir.getAbsolutePath());
        }
    }

    public static String currentBranch(String sc_id) throws Exception {
        try (Git git = Git.open(getGitDir(sc_id))) {
            return git.getRepository().getBranch();
        }
    }

    public static List<String> listLocalBranches(String sc_id) throws Exception {
        List<String> names = new ArrayList<>();
        try (Git git = Git.open(getGitDir(sc_id))) {
            for (Ref ref : git.branchList().call()) {
                names.add(Repository.shortenRefName(ref.getName()));
            }
        }
        return names;
    }

    public static int countUnpushedCommits(String sc_id, String branch) throws Exception {
        try (Git git = Git.open(getGitDir(sc_id))) {
            Repository repo = git.getRepository();
            ObjectId local = repo.resolve("refs/heads/" + branch);
            if (local == null) return 0;
            ObjectId remote = repo.resolve("refs/remotes/origin/" + branch);

            LogCommand log = git.log().add(local);
            if (remote != null) log.not(remote);

            int count = 0;
            for (RevCommit ignored : log.call()) count++;
            return count;
        }
    }

    public static int countBehind(String sc_id, String branch) throws Exception {
        try (Git git = Git.open(getGitDir(sc_id))) {
            Repository repo = git.getRepository();
            ObjectId local = repo.resolve("refs/heads/" + branch);
            ObjectId remote = repo.resolve("refs/remotes/origin/" + branch);
            if (local == null || remote == null) return 0;

            LogCommand log = git.log().add(remote).not(local);
            int count = 0;
            for (RevCommit ignored : log.call()) count++;
            return count;
        }
    }

    public static List<CommitInfo> getCommitHistory(String sc_id, int maxCount) throws Exception {
        List<CommitInfo> list = new ArrayList<>();
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            Repository repo = git.getRepository();
            if (repo.resolve("HEAD") == null) return list;

            Iterable<RevCommit> commits = git.log().setMaxCount(maxCount).call();
            for (RevCommit c : commits) {
                String full = c.getName();
                String shortHash = full.length() >= 7 ? full.substring(0, 7) : full;
                String msg = c.getFullMessage() != null ? c.getFullMessage().trim() : "";
                String author = c.getAuthorIdent() != null ? c.getAuthorIdent().getName() : "Unknown";
                long timeMs = c.getCommitTime() * 1000L;
                list.add(new CommitInfo(full, shortHash, msg, author, timeMs));
            }
        }
        return list;
    }

    public static List<String> getCommitChangedFiles(String sc_id, String commitHash) throws Exception {
        List<String> files = new ArrayList<>();
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            Repository repo = git.getRepository();
            ObjectId commitId = repo.resolve(commitHash);
            if (commitId == null) return files;

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(commitId);
                AbstractTreeIterator oldTreeParser;
                if (commit.getParentCount() > 0) {
                    RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                    CanonicalTreeParser treeParser = new CanonicalTreeParser();
                    try (ObjectReader reader = repo.newObjectReader()) {
                        treeParser.reset(reader, parent.getTree().getId());
                    }
                    oldTreeParser = treeParser;
                } else {
                    oldTreeParser = new EmptyTreeIterator();
                }

                CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                try (ObjectReader reader = repo.newObjectReader()) {
                    newTreeParser.reset(reader, commit.getTree().getId());
                }

                List<DiffEntry> diffs = git.diff()
                        .setOldTree(oldTreeParser)
                        .setNewTree(newTreeParser)
                        .call();
                for (DiffEntry entry : diffs) {
                    String path = entry.getChangeType() == DiffEntry.ChangeType.DELETE
                            ? entry.getOldPath()
                            : entry.getNewPath();
                    files.add(entry.getChangeType().name().charAt(0) + "  " + path);
                }
            }
        }
        return files;
    }

    public static void checkoutBranch(String sc_id, String name, boolean create) throws Exception {
        try (Git git = Git.open(getGitDir(sc_id))) {
            git.checkout().setName(name).setCreateBranch(create).call();
            ensureTrackingConfig(git.getRepository(), name);
        }
    }

    public static void addRemoteOrigin(String sc_id, String repoUrl) throws Exception {
        File gitDir = getGitDir(sc_id);
        try (Git git = Git.open(gitDir)) {
            try {
                git.remoteRemove().setRemoteName("origin").call();
            } catch (Exception ignored) {}
            git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(repoUrl)).call();
            ensureTrackingConfig(git.getRepository(), git.getRepository().getBranch());
        }
    }
}