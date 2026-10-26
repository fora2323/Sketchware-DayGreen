package mod.hilal.saif.activities.tools;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Cooper local Git backend using JGit; it never invokes a shell or system Git executable.
 * All operations are scoped to an explicitly supplied project root.
 *
 * Network operations are deliberately not implemented here; remote GitHub API/auth must
 * remain in a separate layer. Callers must invoke this backend off the Android main thread.
 */
public final class LocalGitBackend {
    public static final class Snapshot {
        public final boolean initialized;
        public final String currentBranch;
        public final String headCommit;
        public final List<String> staged;
        public final List<String> modified;
        public final List<String> added;
        public final List<String> removed;
        public final List<String> untracked;
        public final List<String> conflicting;

        private Snapshot(boolean initialized, String currentBranch, String headCommit,
                         Set<String> staged, Set<String> modified, Set<String> added,
                         Set<String> removed, Set<String> untracked, Set<String> conflicting) {
            this.initialized = initialized;
            this.currentBranch = currentBranch;
            this.headCommit = headCommit;
            this.staged = immutableSorted(staged);
            this.modified = immutableSorted(modified);
            this.added = immutableSorted(added);
            this.removed = immutableSorted(removed);
            this.untracked = immutableSorted(untracked);
            this.conflicting = immutableSorted(conflicting);
        }
    }

    public static final class CommitOutcome {
        public final String commitId;
        public final String message;
        public final List<String> committedPaths;

        private CommitOutcome(String commitId, String message, List<String> paths) {
            this.commitId = commitId;
            this.message = message;
            this.committedPaths = Collections.unmodifiableList(new ArrayList<>(paths));
        }
    }

    public static final class GitFailure extends Exception {
        public final String safeCode;
        GitFailure(String safeCode, String message, Throwable cause) {
            super(message, cause);
            this.safeCode = safeCode;
        }
        GitFailure(String safeCode, String message) { this(safeCode, message, null); }
    }

    public void initialize(File projectRoot, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Initializing a local repository requires confirmation.");
        File root = canonicalRoot(projectRoot);
        if (new File(root, ".git").exists()) {
            throw new GitFailure("ALREADY_INITIALIZED", "A Git metadata directory already exists in this project.");
        }
        try (Git git = Git.init().setDirectory(root).call()) {
            if (git.getRepository().getDirectory() == null || !git.getRepository().getDirectory().isDirectory()) {
                throw new GitFailure("VERIFY_FAILED", "Git initialization could not be verified.");
            }
        } catch (GitAPIException e) {
            throw new GitFailure("GIT_INIT_FAILED", "Cooper could not initialize the local repository.", e);
        }
    }

    public Snapshot inspect(File projectRoot) throws GitFailure {
        File root = canonicalRoot(projectRoot);
        if (!new File(root, ".git").exists()) {
            return new Snapshot(false, "", "", Collections.emptySet(), Collections.emptySet(),
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }
        try (Git git = openRoot(root)) {
            Repository repository = git.getRepository();
            Status status = git.status().call();
            ObjectId head = repository.resolve("HEAD");
            return new Snapshot(true, repository.getBranch(), head == null ? "" : head.name(),
                    status.getChanged(), status.getModified(), status.getAdded(), status.getRemoved(),
                    status.getUntracked(), status.getConflicting());
        } catch (GitAPIException | IOException e) {
            throw new GitFailure("STATUS_FAILED", "Cooper could not inspect the local Git repository.", e);
        }
    }

    public void stage(File projectRoot, List<String> relativePaths, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Staging files requires explicit user confirmation.");
        File root = canonicalRoot(projectRoot);
        validatePaths(root, relativePaths);
        if (relativePaths.isEmpty()) throw new GitFailure("EMPTY_PLAN", "Select at least one file to stage.");
        try (Git git = openRoot(root)) {
            for (String path : relativePaths) {
                File file = new File(root, path);
                if (file.exists() && file.isFile() && !isBinaryPath(path)) {
                    String source = readTextBounded(file, 2 * 1024 * 1024);
                    if (source == null) {
                        throw new GitFailure("UNSCANNABLE_FILE", "Cooper cannot safely scan this large text file; it was not staged: " + path);
                    }
                    List<GitSecretScanner.Finding> findings = GitSecretScanner.scan(path, source);
                    if (GitSecretScanner.blocksUpload(findings)) {
                        throw new GitFailure("SENSITIVE_CONTENT", "A selected file was blocked by Cooper's secret preflight: " + path);
                    }
                }
                git.add().addFilepattern(path).call();
            }
        } catch (GitFailure e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            throw new GitFailure("STAGE_FAILED", "Cooper could not stage the selected project files.", e);
        }
    }

    public void unstage(File projectRoot, List<String> relativePaths, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Unstaging files requires explicit user confirmation.");
        File root = canonicalRoot(projectRoot);
        validatePaths(root, relativePaths);
        if (relativePaths.isEmpty()) throw new GitFailure("EMPTY_PLAN", "Select at least one file to unstage.");
        try (Git git = openRoot(root)) {
            for (String path : relativePaths) git.reset().addPath(path).call();
        } catch (GitAPIException e) {
            throw new GitFailure("UNSTAGE_FAILED", "Cooper could not unstage the selected files.", e);
        }
    }

    public CommitOutcome commit(File projectRoot, String message, String authorName, String authorEmail,
                                List<String> approvedPaths, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Creating a local commit requires explicit user confirmation.");
        if (message == null || message.trim().isEmpty()) throw new GitFailure("EMPTY_MESSAGE", "Enter a commit message.");
        if (blank(authorName) || blank(authorEmail) || authorEmail.indexOf('\n') >= 0) {
            throw new GitFailure("INVALID_AUTHOR", "A valid Git author name and email are required.");
        }
        File root = canonicalRoot(projectRoot);
        validatePaths(root, approvedPaths == null ? Collections.emptyList() : approvedPaths);
        Set<String> approved = new TreeSet<>(approvedPaths == null ? Collections.emptyList() : approvedPaths);
        if (approved.isEmpty()) throw new GitFailure("EMPTY_PLAN", "Select at least one file for this commit.");
        try (Git git = openRoot(root)) {
            Status before = git.status().call();
            Set<String> staged = new TreeSet<>();
            staged.addAll(before.getAdded());
            staged.addAll(before.getChanged());
            staged.addAll(before.getRemoved());
            if (staged.isEmpty()) throw new GitFailure("NOTHING_STAGED", "No staged changes are available to commit.");
            if (!staged.equals(approved)) {
                throw new GitFailure("APPROVED_FILE_SET_MISMATCH",
                        "The staged file set does not exactly match the approved commit plan. Review or unstage extra files before committing.");
            }
            RevCommit commit = git.commit().setMessage(message.trim())
                    .setAuthor(authorName.trim(), authorEmail.trim()).call();
            if (commit == null || commit.getId() == null) {
                throw new GitFailure("VERIFY_FAILED", "The commit result did not contain a valid commit identifier.");
            }
            RevCommit readBack = git.getRepository().parseCommit(commit.getId());
            if (!readBack.getFullMessage().equals(message.trim())) {
                throw new GitFailure("VERIFY_FAILED", "The saved commit message did not match the requested message.");
            }
            List<String> committed = new ArrayList<>(staged);
            Collections.sort(committed);
            return new CommitOutcome(commit.getId().name(), readBack.getFullMessage(), committed);
        } catch (GitFailure e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            throw new GitFailure("COMMIT_FAILED", "Cooper could not create or verify the local commit.", e);
        }
    }

    public CommitOutcome commitSelected(File projectRoot, List<String> approvedPaths, String message,
                                         String authorName, String authorEmail,
                                         boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Creating a local commit requires explicit user confirmation.");
        File root = canonicalRoot(projectRoot);
        validatePaths(root, approvedPaths == null ? Collections.emptyList() : approvedPaths);
        Set<String> selected = new TreeSet<>(approvedPaths == null ? Collections.emptyList() : approvedPaths);
        if (selected.isEmpty()) throw new GitFailure("EMPTY_PLAN", "Select at least one file for this commit.");
        Snapshot before = inspect(root);
        if (!before.initialized) throw new GitFailure("NOT_A_REPOSITORY", "Initialize local Git before committing.");
        Set<String> alreadyStaged = new TreeSet<>();
        alreadyStaged.addAll(before.staged);
        alreadyStaged.addAll(before.added);
        alreadyStaged.addAll(before.removed);
        if (!selected.containsAll(alreadyStaged)) {
            throw new GitFailure("STAGED_FILE_OUTSIDE_PLAN",
                    "One or more already-staged files are outside the selected commit plan. Unstage or include them explicitly.");
        }
        stage(root, new ArrayList<>(selected), true);
        return commit(root, message, authorName, authorEmail, new ArrayList<>(selected), true);
    }

    public void configureRemote(File projectRoot, String remoteUrl, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Linking a remote repository requires explicit confirmation.");
        if (remoteUrl == null || !remoteUrl.matches("https://github\\.com/[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}\\.git")) {
            throw new GitFailure("UNTRUSTED_REMOTE", "Cooper accepts only HTTPS GitHub repository URLs.");
        }
        File root = canonicalRoot(projectRoot);
        try (Git git = openRoot(root)) {
            String existing = git.getRepository().getConfig().getString("remote", "origin", "url");
            if (existing != null && !existing.equals(remoteUrl)) {
                throw new GitFailure("REMOTE_CHANGE_REQUIRES_REVIEW",
                        "A different origin is already configured. Review and explicitly replace the remote binding.");
            }
            git.getRepository().getConfig().setString("remote", "origin", "url", remoteUrl);
            git.getRepository().getConfig().setString("remote", "origin", "fetch",
                    "+refs/heads/*:refs/remotes/origin/*");
            git.getRepository().getConfig().save();
            String readBack = git.getRepository().getConfig().getString("remote", "origin", "url");
            if (!remoteUrl.equals(readBack)) throw new GitFailure("VERIFY_FAILED", "The remote URL did not pass read-back verification.");
        } catch (GitFailure e) {
            throw e;
        } catch (IOException e) {
            throw new GitFailure("REMOTE_CONFIG_FAILED", "Cooper could not configure the GitHub remote.", e);
        }
    }

    public String fetch(File projectRoot, String accessToken) throws GitFailure {
        if (blank(accessToken)) throw new GitFailure("NOT_AUTHENTICATED", "Connect a GitHub account before fetching.");
        File root = canonicalRoot(projectRoot);
        CredentialsProvider credentials = new UsernamePasswordCredentialsProvider("x-access-token", accessToken);
        try (Git git = openRoot(root)) {
            requireOrigin(git.getRepository());
            FetchResult result = git.fetch().setRemote("origin").setCredentialsProvider(credentials)
                    .setRemoveDeletedRefs(true).call();
            if (result == null) throw new GitFailure("FETCH_UNVERIFIED", "Git fetch returned no verifiable result.");
            return result.getMessages() == null ? "" : result.getMessages();
        } catch (GitFailure e) {
            throw e;
        } catch (GitAPIException e) {
            throw new GitFailure("FETCH_FAILED", "Cooper could not fetch the GitHub remote. Refresh remote state before retrying.", e);
        }
    }

    public String push(File projectRoot, String accessToken, boolean explicitlyApproved) throws GitFailure {
        return push(projectRoot, accessToken, explicitlyApproved, null);
    }

    /**
     * Pushes the reviewed local branch and exposes cooperative cancellation to JGit's
     * transfer monitor. Cancellation during transport is treated as an uncertain remote
     * outcome; callers must verify the remote ref before retrying.
     */
    public String push(File projectRoot, String accessToken, boolean explicitlyApproved,
                      final java.util.concurrent.atomic.AtomicBoolean cancelled) throws GitFailure {
        requireApproval(explicitlyApproved, "Pushing commits to GitHub requires explicit confirmation.");
        if (blank(accessToken)) throw new GitFailure("NOT_AUTHENTICATED", "Connect a GitHub account before pushing.");
        if (isPushCancelled(cancelled)) throw new GitFailure("PUSH_CANCELLED", "Push was cancelled before the network operation started.");
        File root = canonicalRoot(projectRoot);
        CredentialsProvider credentials = new UsernamePasswordCredentialsProvider("x-access-token", accessToken);
        org.eclipse.jgit.lib.ProgressMonitor monitor = new org.eclipse.jgit.lib.ProgressMonitor() {
            @Override public void start(int totalTasks) { }
            @Override public void beginTask(String title, int totalWork) { }
            @Override public void update(int completed) { }
            @Override public void endTask() { }
            @Override public boolean isCancelled() { return isPushCancelled(cancelled); }
            @Override public void showDuration(boolean enabled) { }
        };
        try (Git git = openRoot(root)) {
            Repository repository = git.getRepository();
            requireOrigin(repository);
            Status status = git.status().call();
            if (!status.isClean()) {
                throw new GitFailure("WORKTREE_NOT_CLEAN", "Push is blocked until local file changes are reviewed and committed.");
            }
            String branch = repository.getBranch();
            ObjectId head = repository.resolve("HEAD");
            if (head == null) throw new GitFailure("NO_COMMIT", "Create and verify a local commit before pushing.");
            scanOutgoingCommits(repository, branch);
            if (isPushCancelled(cancelled)) throw new GitFailure("PUSH_CANCELLED", "Push was cancelled before the network operation started.");
            Iterable<PushResult> results = git.push().setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch))
                    .setCredentialsProvider(credentials).setProgressMonitor(monitor).call();
            boolean found = false;
            for (PushResult result : results) {
                for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                    if (("refs/heads/" + branch).equals(update.getRemoteName())) {
                        found = true;
                        RemoteRefUpdate.Status statusValue = update.getStatus();
                        if (statusValue != RemoteRefUpdate.Status.OK && statusValue != RemoteRefUpdate.Status.UP_TO_DATE) {
                            throw new GitFailure("PUSH_REJECTED", "GitHub rejected the branch update (" + statusValue.name() + "). Fetch and review remote changes before retrying.");
                        }
                    }
                }
            }
            if (!found) throw new GitFailure("PUSH_UNVERIFIED", "Git push completed without a verifiable update for the target branch.");
            return head.name();
        } catch (GitFailure e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            if (isPushCancelled(cancelled)) {
                throw new GitFailure("PUSH_CANCELLED_UNCERTAIN",
                        "Stop was requested during Push. The remote result is uncertain; verify the GitHub branch before retrying.", e);
            }
            throw new GitFailure("PUSH_UNCERTAIN", "Push outcome may be uncertain. Verify the remote branch before retrying.", e);
        }
    }

    private static boolean isPushCancelled(java.util.concurrent.atomic.AtomicBoolean cancelled) {
        return (cancelled != null && cancelled.get()) || Thread.currentThread().isInterrupted();
    }

    public String pull(File projectRoot, String accessToken, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Pull may modify project files and requires explicit confirmation.");
        if (blank(accessToken)) throw new GitFailure("NOT_AUTHENTICATED", "Connect a GitHub account before pulling.");
        File root = canonicalRoot(projectRoot);
        CredentialsProvider credentials = new UsernamePasswordCredentialsProvider("x-access-token", accessToken);
        try (Git git = openRoot(root)) {
            Repository repository = git.getRepository();
            requireOrigin(repository);
            Status status = git.status().call();
            if (!status.isClean()) {
                throw new GitFailure("WORKTREE_NOT_CLEAN", "Pull is blocked because local changes must be committed or otherwise resolved first.");
            }
            String branch = repository.getBranch();
            PullResult result = git.pull().setRemote("origin").setRemoteBranchName(branch)
                    .setCredentialsProvider(credentials).call();
            if (result == null || !result.isSuccessful()) {
                throw new GitFailure("PULL_NOT_SUCCESSFUL",
                        "Pull did not complete cleanly. Inspect local status and conflicts before taking further action.");
            }
            ObjectId head = repository.resolve("HEAD");
            if (head == null) throw new GitFailure("PULL_VERIFY_FAILED", "The resulting local HEAD could not be verified.");
            return head.name();
        } catch (GitFailure e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            throw new GitFailure("PULL_FAILED", "Pull failed. Inspect local status before retrying.", e);
        }
    }

    private static void scanOutgoingCommits(Repository repository, String branch) throws GitFailure {
        final int maxTextBlobBytes = 2 * 1024 * 1024;
        try (RevWalk commits = new RevWalk(repository)) {
            ObjectId headId = repository.resolve("HEAD");
            ObjectId remoteId = repository.resolve("refs/remotes/origin/" + branch);
            if (headId == null) throw new GitFailure("NO_COMMIT", "No local commit is available to push.");
            RevCommit head = commits.parseCommit(headId);
            commits.markStart(head);
            if (remoteId != null) commits.markUninteresting(commits.parseCommit(remoteId));
            for (RevCommit commit : commits) {
                try (TreeWalk tree = new TreeWalk(repository)) {
                    tree.addTree(commit.getTree());
                    tree.setRecursive(true);
                    while (tree.next()) {
                        String path = tree.getPathString();
                        if (GitSafetyGuard.isExcludedByDefault(path)) {
                            throw new GitFailure("EXCLUDED_HISTORY_PATH",
                                    "Push blocked because outgoing commit history contains a generated or excluded file: " + path);
                        }
                        if (GitSafetyGuard.isSensitivePath(path)) {
                            throw new GitFailure("SENSITIVE_HISTORY_PATH",
                                    "Push blocked because commit history contains a sensitive file path: " + path);
                        }
                        ObjectLoader loader = repository.open(tree.getObjectId(0));
                        if (loader.getSize() > maxTextBlobBytes) {
                            if (isBinaryPath(path)) continue;
                            throw new GitFailure("UNSCANNABLE_LARGE_FILE",
                                    "Push blocked because a large text file could not be fully scanned: " + path);
                        }
                        String text = new String(loader.getBytes(maxTextBlobBytes), StandardCharsets.UTF_8);
                        if (GitSecretScanner.blocksUpload(GitSecretScanner.scan(path, text))) {
                            throw new GitFailure("SECRET_IN_HISTORY",
                                    "Push blocked because a secret pattern was found in commit history: " + path);
                        }
                    }
                }
            }
        } catch (GitFailure e) {
            throw e;
        } catch (IOException e) {
            throw new GitFailure("SECRET_SCAN_FAILED", "Cooper could not complete the outgoing history safety scan.", e);
        }
    }

    private static boolean isBinaryPath(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
                || lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".mp3")
                || lower.endsWith(".wav") || lower.endsWith(".mp4") || lower.endsWith(".zip")
                || lower.endsWith(".res") || lower.endsWith(".bin") || lower.endsWith(".db")
                || lower.endsWith(".sqlite") || lower.endsWith(".dex") || lower.endsWith(".so")
                || lower.endsWith(".aar") || lower.endsWith(".jar") || lower.endsWith(".pdf");
    }

    private static void requireOrigin(Repository repository) throws GitFailure {
        String url = repository.getConfig().getString("remote", "origin", "url");
        if (url == null || !url.matches("https://github\\.com/[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}\\.git")) {
            throw new GitFailure("REMOTE_NOT_CONFIGURED", "A verified GitHub origin must be linked before remote Git operations.");
        }
    }

    public List<String> listBranches(File projectRoot) throws GitFailure {
        File root = canonicalRoot(projectRoot);
        try (Git git = openRoot(root)) {
            List<String> names = new ArrayList<>();
            git.branchList().call().forEach(ref -> names.add(Repository.shortenRefName(ref.getName())));
            Collections.sort(names);
            return Collections.unmodifiableList(names);
        } catch (GitAPIException e) {
            throw new GitFailure("BRANCH_LIST_FAILED", "Cooper could not list local branches.", e);
        }
    }

    public void createBranch(File projectRoot, String branchName, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Creating a local branch requires explicit user confirmation.");
        if (blank(branchName) || branchName.startsWith("-") || branchName.contains("..")
                || branchName.contains(" ") || branchName.contains("~") || branchName.contains("^")
                || branchName.contains(":") || branchName.contains("\\") || branchName.contains("@{")) {
            throw new GitFailure("INVALID_BRANCH", "The requested branch name is invalid.");
        }
        File root = canonicalRoot(projectRoot);
        try (Git git = openRoot(root)) {
            git.branchCreate().setName(branchName).call();
        } catch (GitAPIException e) {
            throw new GitFailure("BRANCH_CREATE_FAILED", "Cooper could not create the local branch.", e);
        }
    }

    public void checkout(File projectRoot, String branchName, boolean explicitlyApproved) throws GitFailure {
        requireApproval(explicitlyApproved, "Switching branches requires explicit user confirmation.");
        if (blank(branchName)) throw new GitFailure("INVALID_BRANCH", "A target branch is required.");
        File root = canonicalRoot(projectRoot);
        try (Git git = openRoot(root)) {
            Status status = git.status().call();
            if (!status.isClean()) {
                throw new GitFailure("WORKTREE_NOT_CLEAN", "Branch switching is blocked because local changes need to be reviewed first.");
            }
            git.checkout().setName(branchName).call();
            if (!branchName.equals(git.getRepository().getBranch())) {
                throw new GitFailure("VERIFY_FAILED", "The active branch did not match the requested branch after checkout.");
            }
        } catch (GitFailure e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            throw new GitFailure("CHECKOUT_FAILED", "Cooper could not switch to the requested branch.", e);
        }
    }

    private static Git openRoot(File root) throws GitFailure {
        try {
            return Git.open(root);
        } catch (IOException e) {
            throw new GitFailure("NOT_A_REPOSITORY", "The selected Cooper project is not a valid Git repository.", e);
        }
    }

    private static File canonicalRoot(File root) throws GitFailure {
        if (root == null) throw new GitFailure("INVALID_ROOT", "A Cooper project root is required.");
        try {
            File canonical = root.getCanonicalFile();
            if (!canonical.exists() || !canonical.isDirectory()) {
                throw new GitFailure("INVALID_ROOT", "The selected Cooper project directory is unavailable.");
            }
            return canonical;
        } catch (IOException | SecurityException e) {
            throw new GitFailure("INVALID_ROOT", "The selected Cooper project path could not be verified.", e);
        }
    }

    private static void validatePaths(File root, List<String> paths) throws GitFailure {
        GitSafetyGuard.Validation validation = GitSafetyGuard.validatePaths(root, paths);
        if (!validation.isAllowed()) throw new GitFailure("PATH_REJECTED", validation.safeMessage);
        for (String path : validation.normalizedPaths) {
            if (GitSafetyGuard.isSensitivePath(path)) {
                throw new GitFailure("SENSITIVE_PATH", "Cooper blocks staging sensitive file paths by default: " + path);
            }
            if (GitSafetyGuard.isExcludedByDefault(path)) {
                throw new GitFailure("EXCLUDED_PATH", "Cooper excludes generated or temporary files by default: " + path);
            }
        }
    }

    private static String readTextBounded(File file, int maxBytes) throws IOException {
        if (file.length() > maxBytes) return null;
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                if (output.size() > maxBytes) return "";
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void requireApproval(boolean approved, String message) throws GitFailure {
        if (!approved) throw new GitFailure("APPROVAL_REQUIRED", message);
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    private static List<String> immutableSorted(Set<String> values) {
        List<String> result = new ArrayList<>(values);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }
}
