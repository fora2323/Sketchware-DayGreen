package mod.hilal.saif.activities.tools;

import pro.sketchware.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.eclipse.jgit.api.Git;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Directly Git publishing pipeline. Every network call and filesystem operation is
 * synchronous by design and MUST be invoked on a worker thread.
 */
public final class DirectlyGitPublisher {
    private static final AtomicBoolean ACTIVE_PUBLISH = new AtomicBoolean(false);
    private static final String PENDING_SESSION_KEY = "pending_session_v1";
    public interface ProgressListener {
        void onPhase(String phase);
    }

    public static final class Plan {
        public final GitProjectBindingStore.Binding binding;
        public final String repositoryName;
        public final String description;
        public final String readmeMode;
        public final String readmeContent;
        public final boolean isPrivate;
        public final List<Uri> images;
        public final List<String> imageExtensions;
        public final List<String> files;
        public final int excludedCount;
        private final String owner;
        private final String authorName;
        private final String authorEmail;
        private final boolean initialized;
        private final String existingHead;

        private Plan(GitProjectBindingStore.Binding binding, String repositoryName, String description,
                     String readmeMode, String readmeContent, boolean isPrivate, List<Uri> images, List<String> imageExtensions, List<String> files,
                     int excludedCount, String owner, String authorName, String authorEmail,
                     boolean initialized, String existingHead) {
            this.binding = binding;
            this.repositoryName = repositoryName;
            this.description = description;
            this.readmeMode = readmeMode;
            this.readmeContent = readmeContent;
            this.isPrivate = isPrivate;
            this.images = Collections.unmodifiableList(new ArrayList<>(images));
            this.imageExtensions = Collections.unmodifiableList(new ArrayList<>(imageExtensions));
            this.files = Collections.unmodifiableList(new ArrayList<>(files));
            this.excludedCount = excludedCount;
            this.owner = owner;
            this.authorName = authorName;
            this.authorEmail = authorEmail;
            this.initialized = initialized;
            this.existingHead = existingHead;
        }

        public String getOwner() { return owner; }
        public String getSummary(Context context) {
            StringBuilder value = new StringBuilder();
            value.append(context.getString(R.string.cooper_github_directly_review_project, binding.workspaceName, binding.scId)).append("\n")
                    .append(context.getString(R.string.cooper_github_directly_review_repository, owner, repositoryName)).append("\n")
                    .append(context.getString(R.string.cooper_github_directly_review_visibility)).append(": ")
                    .append(isPrivate ? "Private" : "Public").append("\n")
                    .append(context.getString(R.string.cooper_github_directly_review_readme_mode, readmeMode)).append("\n")
                    .append(context.getString(R.string.cooper_github_directly_review_images, images.size())).append("\n")
                    .append(context.getString(R.string.cooper_github_directly_review_included_files, files.size())).append("\n")
                    .append(context.getString(R.string.cooper_github_directly_review_excluded_files, excludedCount)).append("\n\n");
            int limit = Math.min(files.size(), 160);
            for (int i = 0; i < limit; i++) value.append("• ").append(files.get(i)).append("\n");
            if (files.size() > limit) value.append("… and ").append(files.size() - limit).append(" more file(s).\n");
            return value.toString();
        }
    }

    public static final class Result {
        public final String repositoryUrl;
        public final String commitSha;
        public final String branch;
        public Result(String repositoryUrl, String commitSha, String branch) {
            this.repositoryUrl = repositoryUrl;
            this.commitSha = commitSha;
            this.branch = branch;
        }
    }

    public static final class PublishException extends Exception {
        public final boolean remoteCreated;
        public final String repositoryUrl;
        public PublishException(String message, boolean remoteCreated, String repositoryUrl, Throwable cause) {
            super(message, cause);
            this.remoteCreated = remoteCreated;
            this.repositoryUrl = repositoryUrl == null ? "" : repositoryUrl;
        }
    }

    private final Context context;
    private final String clientId;
    private final GitProjectBindingStore bindings;
    private final GitHubApiClient api;
    private final GitHubAuthManager auth;
    private final LocalGitBackend backend;
    private final SharedPreferences published;

    public DirectlyGitPublisher(Context context, String clientId) {
        if (context == null) throw new IllegalArgumentException("Context is required.");
        this.context = context.getApplicationContext();
        this.clientId = clientId == null ? "" : clientId.trim();
        this.bindings = new GitProjectBindingStore(this.context);
        this.auth = new GitHubAuthManager(this.context);
        this.api = new GitHubApiClient(auth, this.clientId);
        this.backend = new LocalGitBackend();
        this.published = this.context.getSharedPreferences("cooper_directly_git_publications", Context.MODE_PRIVATE);
    }

    public static boolean hasPendingSession(Context context) {
        if (context == null) return false;
        String value = context.getApplicationContext()
                .getSharedPreferences("cooper_directly_git_publications", Context.MODE_PRIVATE)
                .getString(PENDING_SESSION_KEY, null);
        return value != null && !value.trim().isEmpty();
    }

    public static org.json.JSONArray getPublicationHistory(Context context) {
        if (context == null) return new org.json.JSONArray();
        String value = context.getApplicationContext().getSharedPreferences(
                "cooper_directly_git_publications", Context.MODE_PRIVATE)
                .getString("publication_history_v1", "[]");
        try { return new org.json.JSONArray(value == null ? "[]" : value); }
        catch (Exception ignored) { return new org.json.JSONArray(); }
    }

    public static boolean recordVerifiedPublication(Context context, GitProjectBindingStore.Binding binding,
                                                    String owner, String name, String url, String sha,
                                                    String branch, boolean isPrivate, String method) {
        if (context == null || binding == null || owner == null || name == null || sha == null
                || owner.trim().isEmpty() || name.trim().isEmpty() || sha.trim().isEmpty()) return false;
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(
                "cooper_directly_git_publications", Context.MODE_PRIVATE);
        try {
            org.json.JSONArray history = getPublicationHistory(context);
            org.json.JSONObject entry = new org.json.JSONObject();
            entry.put("projectId", binding.scId);
            entry.put("workspaceName", binding.workspaceName);
            entry.put("appName", binding.appName);
            entry.put("packageName", binding.packageName);
            entry.put("owner", owner);
            entry.put("repositoryName", name);
            entry.put("repositoryUrl", url == null ? "" : url);
            entry.put("commitSha", sha);
            entry.put("branch", branch == null ? "" : branch);
            entry.put("private", isPrivate);
            entry.put("method", method == null ? "ADVANCED_GIT" : method);
            entry.put("publishedAt", System.currentTimeMillis());
            history.put(entry);
            return preferences.edit().putString("publication_history_v1", history.toString()).commit();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean clearPendingSession(Context context) {
        if (context == null) return false;
        return context.getApplicationContext().getSharedPreferences(
                "cooper_directly_git_publications", Context.MODE_PRIVATE)
                .edit().remove(PENDING_SESSION_KEY).commit();
    }

    private static org.json.JSONArray appendHistory(org.json.JSONArray history, GitProjectBindingStore.Binding binding,
                                                     String owner, String name, String url, String sha, String branch,
                                                     boolean isPrivate) throws org.json.JSONException {
        org.json.JSONObject entry = new org.json.JSONObject();
        entry.put("projectId", binding.scId);
        entry.put("workspaceName", binding.workspaceName);
        entry.put("appName", binding.appName);
        entry.put("packageName", binding.packageName);
        entry.put("owner", owner);
        entry.put("repositoryName", name);
        entry.put("repositoryUrl", url);
        entry.put("commitSha", sha);
        entry.put("branch", branch);
        entry.put("private", isPrivate);
        entry.put("method", "DIRECTLY_GIT");
        entry.put("publishedAt", System.currentTimeMillis());
        history.put(entry);
        return history;
    }

    public static JSONObject getPendingSession(Context context) {
        if (context == null) return null;
        String value = context.getApplicationContext()
                .getSharedPreferences("cooper_directly_git_publications", Context.MODE_PRIVATE)
                .getString(PENDING_SESSION_KEY, null);
        if (value == null || value.trim().isEmpty()) return null;
        try { return new JSONObject(value); } catch (Exception ignored) { return null; }
    }

    private void persistPendingSession(String state, GitProjectBindingStore.Binding binding,
                                       String owner, String repositoryName, String description,
                                       String branch, String commitSha, String repositoryUrl,
                                       long startedAt, String diagnostic) throws IOException {
        persistPendingSession(state, binding, owner, repositoryName, description, branch, commitSha, repositoryUrl, startedAt, diagnostic, true);
    }

    private void persistPendingSession(String state, GitProjectBindingStore.Binding binding,
                                       String owner, String repositoryName, String description,
                                       String branch, String commitSha, String repositoryUrl,
                                       long startedAt, String diagnostic, boolean isPrivate) throws IOException {
        try {
            JSONObject session = new JSONObject();
            session.put("schema", 1);
            session.put("state", state);
            session.put("projectId", binding.scId);
            session.put("projectRoot", binding.projectRoot.getCanonicalPath());
            session.put("owner", owner);
            session.put("repositoryName", repositoryName);
            session.put("description", description == null ? "" : description);
            session.put("private", isPrivate);
            session.put("branch", branch == null ? "" : branch);
            session.put("commitSha", commitSha == null ? "" : commitSha);
            session.put("repositoryUrl", repositoryUrl == null ? "" : repositoryUrl);
            session.put("startedAt", startedAt);
            session.put("updatedAt", System.currentTimeMillis());
            session.put("diagnostic", diagnostic == null ? "" : diagnostic);
            if (!published.edit().putString(PENDING_SESSION_KEY, session.toString()).commit()) {
                throw new IOException("Cooper could not persist the Directly Git recovery session.");
            }
        } catch (org.json.JSONException e) {
            throw new IOException("Cooper could not serialize the Directly Git recovery session.", e);
        }
    }

    public Plan prepare(GitProjectBindingStore.Binding selected, String name, String description,
                        String readmeMode, String readmeContent, boolean isPrivate, List<Uri> images) throws Exception {
        if (selected == null) throw new IllegalArgumentException("Select a Cooper project first.");
        JSONObject pendingSession = getPendingSession(context);
        if (pendingSession != null) {
            throw new IllegalStateException(context.getString(R.string.cooper_github_directly_pending_session_exists,
                    pendingSession.optString("owner", ""), pendingSession.optString("repositoryName", "")));
        }
        if (name == null || !name.matches("[A-Za-z0-9_.-]{1,100}") || name.equals(".") || name.equals("..")
                || name.contains("..") || name.startsWith(".") || name.endsWith(".")
                || name.toLowerCase(Locale.ROOT).endsWith(".git")) {
            throw new IllegalArgumentException("Enter a valid GitHub repository name (1–100 characters).");
        }
        String safeDescription = description == null ? "" : description.trim();
        if (safeDescription.length() > 350) throw new IllegalArgumentException("Repository descriptions are limited to 350 characters.");
        if (!"EMPTY".equals(readmeMode) && !"MARKDOWN".equals(readmeMode) && !"TEXT".equals(readmeMode)) {
            throw new IllegalArgumentException("Choose README or Empty before publishing.");
        }
        if (images == null || images.size() > 6) throw new IllegalArgumentException("Select no more than six images.");
        if (!new GitCredentialStore(context).hasCredential()) throw new IllegalStateException("Connect GitHub before publishing.");
        String token = auth.getUsableAccessToken(clientId);
        if (token == null || token.trim().isEmpty()) throw new IllegalStateException("GitHub credentials are no longer valid. Reconnect and retry.");
        JSONObject account = api.getAuthenticatedAccount();
        String owner = account.optString("login", "").trim();
        if (owner.isEmpty()) throw new IllegalStateException("GitHub did not return the authenticated account identity.");
        GitProjectBindingStore.Binding current = bindings.getBoundProject();
        if (current == null || !selected.scId.equals(current.scId)
                || !selected.projectRoot.getCanonicalPath().equals(current.projectRoot.getCanonicalPath())) {
            throw new IllegalStateException("The selected Cooper project changed. Select it again before publishing.");
        }
        if (!current.remoteUrl().isEmpty()) throw new IllegalStateException("This project already has a remote repository linked. Use GitHub Manager to inspect it.");
        if (published.contains("project_" + current.scId)) {
            throw new IllegalStateException("This Cooper project has already been published by Directly Git. Use the existing repository instead of creating a duplicate.");
        }
        ensureRepositoryDoesNotExist(owner, name);
        LocalGitBackend.Snapshot snapshot = backend.inspect(current.projectRoot);
        if (snapshot.initialized && (!snapshot.staged.isEmpty() || !snapshot.modified.isEmpty()
                || !snapshot.added.isEmpty() || !snapshot.removed.isEmpty()
                || !snapshot.untracked.isEmpty() || !snapshot.conflicting.isEmpty())) {
            throw new IllegalStateException("The local Git worktree is not clean. Review or commit existing changes in GitHub Manager before Directly Git.");
        }
        if (snapshot.initialized) {
            File gitDir = new File(current.projectRoot, ".git");
            if (gitDir.exists()) {
                try (Git git = Git.open(current.projectRoot)) {
                    String origin = git.getRepository().getConfig().getString("remote", "origin", "url");
                    if (origin != null && !origin.trim().isEmpty()) {
                        throw new IllegalStateException("An origin remote already exists. Directly Git will not replace it.");
                    }
                }
            }
        }
        ArrayList<String> files = new ArrayList<>();
        int excluded = collectManifest(current.projectRoot, current.projectRoot, files, new long[]{0L});
        if (!"EMPTY".equals(readmeMode)) {
            File[] rootEntries = current.projectRoot.listFiles();
            if (rootEntries != null) {
                for (File entry : rootEntries) {
                    if (entry.isFile() && "README.md".equalsIgnoreCase(entry.getName())) {
                        throw new IllegalStateException("A README already exists in this project. Directly Git will not overwrite it.");
                    }
                }
            }
            files.add("README.md");
        }
        int imageIndex = 0;
        ArrayList<String> imageExtensions = new ArrayList<>();
        for (Uri uri : images) {
            imageIndex++;
            String ext = validateImageUri(uri);
            imageExtensions.add(ext);
            String path = String.format(Locale.ROOT, "directly-git-assets/image-%02d.%s", imageIndex, ext);
            if (new File(current.projectRoot, path).exists()) throw new IllegalStateException("Image destination already exists: " + path);
            files.add(path);
        }
        Collections.sort(files);
        if (files.isEmpty()) throw new IllegalStateException("No publishable project files were found.");
        if (files.size() > 5000) throw new IllegalStateException("The project contains too many files for a safe guided publish (limit 5,000).");
        long userId = account.optLong("id", 0L);
        String authorName = account.optString("name", "").trim();
        if (authorName.isEmpty()) authorName = owner;
        String authorEmail = account.optString("email", "").trim();
        if (authorEmail.isEmpty() && userId > 0) authorEmail = userId + "+" + owner + "@users.noreply.github.com";
        if (authorEmail.isEmpty()) throw new IllegalStateException("GitHub did not provide an author identity. Add a verified email to your GitHub profile and retry.");
        return new Plan(current, name, safeDescription, readmeMode, readmeContent == null ? "" : readmeContent,
                isPrivate, images, imageExtensions, files, excluded, owner, authorName, authorEmail, snapshot.initialized, snapshot.headCommit);
    }

    public Result publish(Plan plan, AtomicBoolean cancelled, ProgressListener progress) throws Exception {
        if (!ACTIVE_PUBLISH.compareAndSet(false, true)) {
            throw new PublishException("Another Directly Git publishing operation is already running.", false, "", null);
        }
        try {
            return publishInternal(plan, cancelled, progress);
        } finally {
            ACTIVE_PUBLISH.set(false);
        }
    }

    private Result publishInternal(Plan plan, AtomicBoolean cancelled, ProgressListener progress) throws Exception {
        if (plan == null) throw new IllegalArgumentException("A reviewed publishing plan is required.");
        boolean remoteCreated = false;
        boolean localCommitCreated = false;
        String repositoryUrl = "https://github.com/" + plan.owner + "/" + plan.repositoryName;
        String expectedSha = "";
        String branchName = "";
        String sessionState = "PREPARING";
        ArrayList<File> generated = new ArrayList<>();
        long startedAt = System.currentTimeMillis();
        try {
            phase(progress, context.getString(R.string.cooper_github_directly_phase_validate));
            checkCancelled(cancelled);
            GitProjectBindingStore.Binding current = bindings.getBoundProject();
            if (current == null || !plan.binding.scId.equals(current.scId)
                    || !plan.binding.projectRoot.getCanonicalPath().equals(current.projectRoot.getCanonicalPath())) {
                throw new IllegalStateException("The selected project changed after review.");
            }
            String token = auth.getUsableAccessToken(clientId);
            JSONObject account = api.getAuthenticatedAccount();
            if (!plan.owner.equals(account.optString("login", ""))) throw new IllegalStateException("The authenticated GitHub account changed after review.");
            ensureRepositoryDoesNotExist(plan.owner, plan.repositoryName);
            checkCancelled(cancelled);

            phase(progress, context.getString(R.string.cooper_github_directly_phase_assets));
            if (!"EMPTY".equals(plan.readmeMode)) {
                File readme = new File(current.projectRoot, "README.md");
                if (readme.exists()) throw new IllegalStateException("README.md appeared after review; refusing to overwrite it.");
                String content = buildReadme(plan);
                writeNewFile(readme, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                generated.add(readme);
            }
            File assets = new File(current.projectRoot, "directly-git-assets");
            if (!plan.images.isEmpty() && !assets.exists() && !assets.mkdirs()) throw new IOException("Could not create the image directory.");
            for (int i = 0; i < plan.images.size(); i++) {
                checkCancelled(cancelled);
                Uri uri = plan.images.get(i);
                String ext = validateImageUri(uri);
                File target = new File(assets, String.format(Locale.ROOT, "image-%02d.%s", i + 1, ext));
                if (target.exists()) throw new IllegalStateException("Image destination appeared after review.");
                copyBounded(uri, target, 8L * 1024L * 1024L);
                generated.add(target);
            }

            phase(progress, context.getString(R.string.cooper_github_directly_phase_manifest));
            ArrayList<String> manifest = new ArrayList<>();
            collectManifest(current.projectRoot, current.projectRoot, manifest, new long[]{0L});
            Collections.sort(manifest);
            if (!manifest.equals(plan.files)) throw new IllegalStateException("The project file set changed after review. Review the project again.");
            for (String path : manifest) scanFile(current.projectRoot, path);
            checkCancelled(cancelled);

            phase(progress, context.getString(R.string.cooper_github_directly_phase_commit));
            LocalGitBackend.Snapshot before = backend.inspect(current.projectRoot);
            if (!before.initialized) backend.initialize(current.projectRoot, true);
            LocalGitBackend.Snapshot snapshot = backend.inspect(current.projectRoot);
            if (!snapshot.staged.isEmpty() || !snapshot.modified.isEmpty() || !snapshot.added.isEmpty()
                    || !snapshot.removed.isEmpty() || !snapshot.conflicting.isEmpty()) {
                throw new IllegalStateException("The local worktree changed during publishing. No remote repository was created.");
            }
            ArrayList<String> changed = new ArrayList<>(snapshot.untracked);
            if (!changed.isEmpty()) {
                LocalGitBackend.CommitOutcome outcome = backend.commitSelected(current.projectRoot, changed,
                        "Publish " + plan.repositoryName + " from Cooper", plan.authorName, plan.authorEmail, true);
                LocalGitBackend.Snapshot verified = backend.inspect(current.projectRoot);
                if (!outcome.commitId.equals(verified.headCommit)) throw new IllegalStateException("Local commit read-back verification failed.");
            }
            LocalGitBackend.Snapshot local = backend.inspect(current.projectRoot);
            if (local.headCommit == null || local.headCommit.trim().isEmpty()) throw new IllegalStateException("No local commit exists to publish.");
            expectedSha = local.headCommit;
            branchName = local.currentBranch;
            if (branchName == null || branchName.trim().isEmpty()) {
                throw new IllegalStateException("Cooper could not verify the local branch name before publication.");
            }
            localCommitCreated = true;
            sessionState = "READY_TO_CREATE";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);

            phase(progress, context.getString(R.string.cooper_github_directly_phase_create));
            checkCancelled(cancelled);
            sessionState = "CREATING_REPOSITORY";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);
            JSONObject remote;
            try {
                remote = api.createRepository(plan.repositoryName, plan.description, plan.isPrivate, true);
            } catch (Exception createError) {
                // A timeout is ambiguous. Look up the repository before any retry; never retry creation blindly.
                try {
                    JSONObject lookup = api.getRepository(plan.owner, plan.repositoryName);
                    boolean createdRecently = lookup.optString("created_at", "").compareTo(
                            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new java.util.Date(startedAt))) >= 0;
                    if (!createdRecently || !plan.owner.equals(lookup.optJSONObject("owner") == null ? "" : lookup.optJSONObject("owner").optString("login", ""))
                            || !plan.description.equals(lookup.optString("description", ""))) {
                        throw new PublishException("A repository with this name exists, but Cooper could not prove it was created by this session. No push was attempted.", false, "", createError);
                    }
                    remote = lookup;
                } catch (GitHubApiClient.ApiException lookupError) {
                    throw createError;
                }
            }
            String returnedOwner = remote.optJSONObject("owner") == null ? "" : remote.optJSONObject("owner").optString("login", "");
            if (!plan.owner.equals(returnedOwner) || !plan.repositoryName.equals(remote.optString("name", ""))) {
                throw new IllegalStateException("GitHub returned a repository identity different from the reviewed target.");
            }
            if (remote.optBoolean("private", true) != plan.isPrivate) {
                throw new IllegalStateException("GitHub repository visibility differs from the reviewed Public/Private choice. Cooper stopped before pushing.");
            }
            remoteCreated = true;
            repositoryUrl = remote.optString("html_url", repositoryUrl);
            if (!repositoryUrl.equals("https://github.com/" + plan.owner + "/" + plan.repositoryName)
                    && !repositoryUrl.equals("https://github.com/" + plan.owner + "/" + plan.repositoryName + "/")) {
                throw new IllegalStateException("GitHub returned an untrusted repository URL.");
            }
            sessionState = "REMOTE_CREATED";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);
            if (cancelled != null && cancelled.get()) throw new PublishException("Publishing was stopped after the remote repository was created. The recovery session was saved; resume it from GitHub Manager.", true, repositoryUrl, null);

            phase(progress, context.getString(R.string.cooper_github_directly_phase_remote));
            backend.configureRemote(current.projectRoot, "https://github.com/" + plan.owner + "/" + plan.repositoryName + ".git", true);
            sessionState = "PUSH_PENDING";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);
            if (cancelled != null && cancelled.get()) throw new PublishException("Publishing was stopped after repository creation. The recovery session was saved; resume it from GitHub Manager.", true, repositoryUrl, null);
            phase(progress, context.getString(R.string.cooper_github_directly_phase_push));
            sessionState = "PUSH_IN_PROGRESS";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);
            String pushedSha = backend.push(current.projectRoot, token, true, cancelled);
            if (!expectedSha.equals(pushedSha)) throw new IllegalStateException("The push result did not match the reviewed local commit.");
            sessionState = "PUSH_VERIFY_PENDING";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);

            phase(progress, context.getString(R.string.cooper_github_directly_phase_verify));
            JSONObject branch = api.getBranch(plan.owner, plan.repositoryName, local.currentBranch);
            JSONObject commit = branch.optJSONObject("commit");
            String remoteSha = commit == null ? "" : commit.optString("sha", "");
            if (!expectedSha.equals(remoteSha)) throw new IllegalStateException("GitHub's remote branch SHA does not match the local commit. Publication remains unverified.");

            phase(progress, context.getString(R.string.cooper_github_directly_phase_finalize));
            sessionState = "REMOTE_VERIFIED";
            persistPendingSession(sessionState, current, plan.owner, plan.repositoryName, plan.description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", plan.isPrivate);
            bindings.linkRemote(plan.owner, plan.repositoryName);
            org.json.JSONArray history;
            try {
                history = appendHistory(getPublicationHistory(context), plan.binding, plan.owner,
                        plan.repositoryName, repositoryUrl, expectedSha, branchName, plan.isPrivate);
            } catch (org.json.JSONException historyError) {
                throw new IOException("The verified publication history could not be prepared.", historyError);
            }
            if (!published.edit().putString("project_" + plan.binding.scId, plan.owner + "/" + plan.repositoryName)
                    .putString("commit_" + plan.binding.scId, expectedSha)
                    .putString("publication_history_v1", history.toString())
                    .remove(PENDING_SESSION_KEY).commit()) {
                throw new IllegalStateException("Remote commit was verified, but Cooper could not persist the publication record. Recovery data was retained.");
            }
            return new Result(repositoryUrl, expectedSha, branchName);
        } catch (Exception e) {
            if (!remoteCreated && !localCommitCreated) {
                for (File file : generated) {
                    try { if (file.isFile()) file.delete(); } catch (Exception ignored) { }
                }
            }
            if (localCommitCreated) {
                String stateToKeep = sessionState;
                if (remoteCreated && ("CREATING_REPOSITORY".equals(stateToKeep)
                        || "READY_TO_CREATE".equals(stateToKeep) || "PREPARING".equals(stateToKeep))) {
                    stateToKeep = "REMOTE_CREATED";
                }
                try {
                    persistPendingSession(stateToKeep, plan.binding, plan.owner, plan.repositoryName,
                            plan.description, branchName, expectedSha, repositoryUrl, startedAt,
                            e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), plan.isPrivate);
                } catch (Exception persistenceError) {
                    e.addSuppressed(persistenceError);
                }
            }
            if (e instanceof PublishException) throw e;
            throw new PublishException(e.getMessage() == null ? "Directly Git publishing failed; verify local and remote state before retrying." : e.getMessage(),
                    remoteCreated, repositoryUrl, e);
        }
    }

    /**
     * Resumes only a previously confirmed Directly Git session. It never pushes to a
     * repository with an unexpected owner, name, origin, branch, or commit.
     */
    public Result resumePending(AtomicBoolean cancelled, ProgressListener progress) throws Exception {
        if (!ACTIVE_PUBLISH.compareAndSet(false, true)) {
            throw new PublishException("Another Directly Git publishing operation is already running.", false, "", null);
        }
        try {
            JSONObject session = getPendingSession(context);
            if (session == null) throw new IllegalStateException("No saved Directly Git recovery session exists.");
            String state = session.optString("state", "");
            String scId = session.optString("projectId", "");
            String rootPath = session.optString("projectRoot", "");
            String owner = session.optString("owner", "");
            String name = session.optString("repositoryName", "");
            String description = session.optString("description", "");
            String branchName = session.optString("branch", "");
            String expectedSha = session.optString("commitSha", "");
            String repositoryUrl = session.optString("repositoryUrl", "");
            long startedAt = session.optLong("startedAt", 0L);
            if (scId.isEmpty() || rootPath.isEmpty() || owner.isEmpty() || name.isEmpty()
                    || branchName.isEmpty() || expectedSha.isEmpty() || startedAt <= 0L) {
                throw new IllegalStateException("The saved recovery record is incomplete. No remote write was attempted.");
            }
            GitProjectBindingStore.Binding current = bindings.getBoundProject();
            if (current == null || !scId.equals(current.scId)
                    || !rootPath.equals(current.projectRoot.getCanonicalPath())) {
                throw new IllegalStateException("The selected Cooper project no longer matches the saved recovery session. Re-select the original project in GitHub Manager.");
            }
            String token = auth.getUsableAccessToken(clientId);
            if (token == null || token.trim().isEmpty()) throw new IllegalStateException("Reconnect the original GitHub account before resuming publication.");
            JSONObject account = api.getAuthenticatedAccount();
            if (!owner.equals(account.optString("login", ""))) {
                throw new IllegalStateException("The connected GitHub account differs from the account that created this publishing session.");
            }
            LocalGitBackend.Snapshot local = backend.inspect(current.projectRoot);
            if (!local.initialized || !expectedSha.equals(local.headCommit) || !branchName.equals(local.currentBranch)) {
                throw new IllegalStateException("The local HEAD or branch changed since the interrupted publication. Cooper will not push a different commit.");
            }
            String expectedRemoteUrl = "https://github.com/" + owner + "/" + name + ".git";
            try (Git git = Git.open(current.projectRoot)) {
                String configured = git.getRepository().getConfig().getString("remote", "origin", "url");
                if (configured != null && !configured.trim().isEmpty() && !expectedRemoteUrl.equals(configured)) {
                    throw new IllegalStateException("The local origin points to a different repository. Recovery stopped without changing it.");
                }
            }
            phase(progress, context.getString(R.string.cooper_github_directly_phase_validate));
            checkCancelled(cancelled);
            JSONObject remote;
            try {
                remote = api.getRepository(owner, name);
            } catch (GitHubApiClient.ApiException missing) {
                if (!"NOT_FOUND".equals(missing.safeCode)) throw missing;
                if (!"READY_TO_CREATE".equals(state) && !"CREATING_REPOSITORY".equals(state)) {
                    throw new IllegalStateException("The saved repository is not currently visible. Reconnect the original account and inspect it before retrying.");
                }
                phase(progress, context.getString(R.string.cooper_github_directly_phase_create));
                remote = api.createRepository(name, description, session.optBoolean("private", true), true);
                repositoryUrl = remote.optString("html_url", "https://github.com/" + owner + "/" + name);
                persistPendingSession("REMOTE_CREATED", current, owner, name, description,
                        branchName, expectedSha, repositoryUrl, startedAt, "Recovered by recreating a confirmed repository");
                state = "REMOTE_CREATED";
            }
            String actualOwner = remote.optJSONObject("owner") == null ? "" : remote.optJSONObject("owner").optString("login", "");
            if (!owner.equals(actualOwner) || !name.equals(remote.optString("name", ""))) {
                throw new IllegalStateException("The saved recovery session points to a different GitHub repository. No push was attempted.");
            }
            if ("CREATING_REPOSITORY".equals(state)) {
                long createdAt = parseGitHubTimestamp(remote.optString("created_at", ""));
                if (createdAt < startedAt - 5000L
                        || !description.equals(remote.optString("description", ""))
                        || remote.optBoolean("private", true) != session.optBoolean("private", true)
                        || remote.optLong("size", -1L) != 0L) {
                    throw new IllegalStateException("Repository creation outcome is ambiguous. Cooper will not push to a repository it cannot safely identify as this session's newly created, empty private repository.");
                }
                repositoryUrl = remote.optString("html_url", repositoryUrl);
                persistPendingSession("REMOTE_CREATED", current, owner, name, description,
                        branchName, expectedSha, repositoryUrl, startedAt, "", session.optBoolean("private", true));
                state = "REMOTE_CREATED";
            } else if ("READY_TO_CREATE".equals(state)) {
                throw new IllegalStateException("A repository with this name exists before the saved creation step. Cooper refused to use or overwrite it.");
            }
            if (remote.optBoolean("private", true) != session.optBoolean("private", true)
                    || !description.equals(remote.optString("description", ""))) {
                throw new IllegalStateException("Remote repository visibility or description no longer matches the approved publication plan.");
            }
            if (cancelled != null && cancelled.get()) {
                throw new PublishException("Recovery was stopped before the remote write. The saved session was retained.", true, repositoryUrl, null);
            }
            String configured = "";
            try (Git git = Git.open(current.projectRoot)) {
                configured = git.getRepository().getConfig().getString("remote", "origin", "url");
            }
            if (configured == null || configured.trim().isEmpty()) {
                backend.configureRemote(current.projectRoot, expectedRemoteUrl, true);
            } else if (!expectedRemoteUrl.equals(configured)) {
                throw new IllegalStateException("The saved session remote does not match the local origin.");
            }
            boolean branchExists = false;
            String actualSha = "";
            try {
                JSONObject branch = api.getBranch(owner, name, branchName);
                JSONObject commit = branch.optJSONObject("commit");
                actualSha = commit == null ? "" : commit.optString("sha", "");
                branchExists = true;
            } catch (GitHubApiClient.ApiException missingBranch) {
                if (!"NOT_FOUND".equals(missingBranch.safeCode)) throw missingBranch;
            }
            if (branchExists && !expectedSha.equals(actualSha)) {
                throw new IllegalStateException("The remote branch exists but points to a different commit. Cooper will not overwrite it.");
            }
            if (!branchExists) {
                phase(progress, context.getString(R.string.cooper_github_directly_phase_push));
                persistPendingSession("PUSH_IN_PROGRESS", current, owner, name, description,
                        branchName, expectedSha, repositoryUrl, startedAt, "", session.optBoolean("private", true));
                String pushedSha = backend.push(current.projectRoot, token, true, cancelled);
                if (!expectedSha.equals(pushedSha)) throw new IllegalStateException("Push result differs from the saved commit SHA.");
                phase(progress, context.getString(R.string.cooper_github_directly_phase_verify));
                JSONObject branch = api.getBranch(owner, name, branchName);
                JSONObject commit = branch.optJSONObject("commit");
                actualSha = commit == null ? "" : commit.optString("sha", "");
                if (!expectedSha.equals(actualSha)) {
                    persistPendingSession("PUSH_VERIFY_PENDING", current, owner, name, description,
                            branchName, expectedSha, repositoryUrl, startedAt, "Remote SHA did not match the saved commit", session.optBoolean("private", true));
                    throw new IllegalStateException("Remote branch SHA could not be verified after recovery push.");
                }
            }
            phase(progress, context.getString(R.string.cooper_github_directly_phase_finalize));
            persistPendingSession("REMOTE_VERIFIED", current, owner, name, description,
                    branchName, expectedSha, repositoryUrl, startedAt, "", session.optBoolean("private", true));
            bindings.linkRemote(owner, name);
            org.json.JSONArray history;
            try {
                history = appendHistory(getPublicationHistory(context), current, owner, name,
                        repositoryUrl, expectedSha, branchName, session.optBoolean("private", true));
            } catch (org.json.JSONException historyError) {
                throw new IOException("The recovered publication history could not be prepared.", historyError);
            }
            if (!published.edit().putString("project_" + scId, owner + "/" + name)
                    .putString("commit_" + scId, expectedSha)
                    .putString("publication_history_v1", history.toString())
                    .remove(PENDING_SESSION_KEY).commit()) {
                throw new IllegalStateException("The remote commit was verified, but the final project association could not be saved. Recovery data remains available.");
            }
            return new Result(repositoryUrl, expectedSha, branchName);
        } finally {
            ACTIVE_PUBLISH.set(false);
        }
    }

    private static long parseGitHubTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) return -1L;
        try { return java.time.Instant.parse(value).toEpochMilli(); }
        catch (Exception ignored) { return -1L; }
    }

    private void ensureRepositoryDoesNotExist(String owner, String name) throws Exception {
        try {
            api.getRepository(owner, name);
            throw new IllegalStateException("That GitHub repository already exists or is accessible. Directly Git never overwrites existing repositories.");
        } catch (GitHubApiClient.ApiException e) {
            if (!"NOT_FOUND".equals(e.safeCode)) throw e;
        }
    }

    private int collectManifest(File root, File directory, List<String> files, long[] totalBytes) throws Exception {
        if (!directory.equals(root) && new File(directory, ".git").exists()) return 1;
        File[] children = directory.listFiles();
        if (children == null) throw new IOException("Cooper could not read the selected project directory.");
        java.util.Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        int excluded = 0;
        for (File child : children) {
            String path = relative(root, child);
            if (GitSafetyGuard.isExcludedByDefault(path)) { excluded++; continue; }
            File canonical = child.getCanonicalFile();
            String rootPath = root.getCanonicalPath();
            if (!canonical.getPath().startsWith(rootPath + File.separator)) throw new IOException("A project path resolves outside the selected project.");
            if (child.isDirectory()) {
                excluded += collectManifest(root, child, files, totalBytes);
            } else if (child.isFile()) {
                if (GitSafetyGuard.isSensitivePath(path)) throw new IllegalStateException("Sensitive file blocked from publication: " + path);
                if (child.length() > 10L * 1024L * 1024L) throw new IllegalStateException("File exceeds the 10 MB guided-publish limit: " + path);
                totalBytes[0] += child.length();
                if (totalBytes[0] > 50L * 1024L * 1024L) throw new IllegalStateException("The project exceeds the 50 MB guided-publish limit.");
                scanFile(root, path);
                files.add(path);
            }
            if (files.size() > 5000) throw new IllegalStateException("The project exceeds the 5,000-file guided-publish limit.");
        }
        return excluded;
    }

    private void scanFile(File root, String path) throws Exception {
        if (GitSafetyGuard.isSensitivePath(path)) throw new IllegalStateException("Sensitive file blocked from publication: " + path);
        File file = new File(root, path);
        if (!file.isFile() || !isTextPath(path)) return;
        if (file.length() > 2L * 1024L * 1024L) throw new IllegalStateException("Text file is too large to scan safely: " + path);
        byte[] bytes;
        try (InputStream input = new FileInputStream(file)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > 2 * 1024 * 1024) throw new IOException("Text file exceeds scanner limit.");
            }
            bytes = out.toByteArray();
        }
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        List<GitSecretScanner.Finding> findings = GitSecretScanner.scan(path, text);
        if (!findings.isEmpty()) {
            StringBuilder rules = new StringBuilder();
            for (GitSecretScanner.Finding finding : findings) {
                if (rules.length() > 0) rules.append(", ");
                rules.append(finding.rule);
            }
            throw new IllegalStateException("Secret/sensitive-content review required for " + path + " (" + rules + "). Remove or review this file in GitHub Manager before publishing.");
        }
    }

    private static boolean isTextPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".xml")
                || lower.endsWith(".json") || lower.endsWith(".gradle") || lower.endsWith(".properties")
                || lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".yml")
                || lower.endsWith(".yaml") || lower.endsWith(".html") || lower.endsWith(".css")
                || lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".sh")
                || lower.endsWith(".pro") || lower.endsWith(".cfg") || lower.endsWith(".toml")
                || lower.endsWith(".ini") || lower.endsWith(".sql") || lower.endsWith(".ktx");
    }

    private String validateImageUri(Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException("A selected image is unavailable.");
        String type = context.getContentResolver().getType(uri);
        if (type == null) throw new IllegalArgumentException("Cooper could not determine a selected image type.");
        String lower = type.toLowerCase(Locale.ROOT);
        String ext;
        if ("image/jpeg".equals(lower)) ext = "jpg";
        else if ("image/png".equals(lower)) ext = "png";
        else if ("image/webp".equals(lower)) ext = "webp";
        else if ("image/gif".equals(lower)) ext = "gif";
        else throw new IllegalArgumentException("Use JPEG, PNG, WebP, or GIF images.");
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null || input.read() < 0) throw new IOException("Selected image is unreadable.");
        }
        return ext;
    }

    private void copyBounded(Uri uri, File target, long maxBytes) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(target, false)) {
            if (input == null) throw new IOException("Selected image could not be opened.");
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) throw new IOException("Each selected image must be 8 MB or smaller.");
                output.write(buffer, 0, read);
            }
            if (total == 0) throw new IOException("Selected image is empty.");
        } catch (Exception e) {
            if (target.exists()) target.delete();
            throw e;
        }
    }

    private static String buildReadme(Plan plan) {
        String content = plan.readmeContent == null ? "" : plan.readmeContent.trim();
        if (content.isEmpty()) content = plan.binding.workspaceName;
        StringBuilder result = new StringBuilder(content).append("\n");
        if (!plan.images.isEmpty()) {
            boolean markdown = "MARKDOWN".equals(plan.readmeMode);
            result.append("\n").append(markdown ? "## Project images" : "Project images:").append("\n\n");
            for (int i = 0; i < plan.images.size(); i++) {
                String ext = plan.imageExtensions.get(i);
                String path = "directly-git-assets/image-" + String.format(Locale.ROOT, "%02d", i + 1) + "." + ext;
                if (markdown) result.append("![Project image ").append(i + 1).append("](").append(path).append(")\n");
                else result.append(path).append("\n");
            }
        }
        return result.toString();
    }

    private static void writeNewFile(File file, byte[] bytes) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create README parent directory.");
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(bytes);
            output.flush();
        }
    }

    private static String relative(File root, File file) throws IOException {
        String rootPath = root.getCanonicalPath();
        String path = file.getCanonicalPath();
        if (!path.startsWith(rootPath + File.separator)) throw new IOException("Path is outside the project root.");
        return path.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
    }

    private static void checkCancelled(AtomicBoolean cancelled) throws PublishException {
        if (cancelled != null && cancelled.get()) throw new PublishException("Publishing was stopped before the next operation. Any already-created remote repository was retained.", false, "", null);
    }

    private static void phase(ProgressListener listener, String value) {
        if (listener != null) listener.onPhase(value);
    }
}
