package mod.hilal.saif.activities.tools;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only Cooper file diff view. Includes staged and unstaged textual diffs.
 * It never writes to the project or changes the Git index.
 */
public final class GitDiffEngine {
    public static final class DiffResult {
        public final String path;
        public final String text;
        public final boolean truncated;

        private DiffResult(String path, String text, boolean truncated) {
            this.path = path;
            this.text = text;
            this.truncated = truncated;
        }
    }

    public DiffResult diff(File projectRoot, String relativePath) throws LocalGitBackend.GitFailure {
        GitSafetyGuard.Validation validation = GitSafetyGuard.validatePaths(projectRoot,
                Collections.singletonList(relativePath));
        if (!validation.isAllowed() || GitSafetyGuard.isExcludedByDefault(relativePath)) {
            throw new LocalGitBackend.GitFailure("PATH_REJECTED", "Cooper rejected the selected path for diff inspection.");
        }
        File root;
        try {
            root = projectRoot.getCanonicalFile();
        } catch (IOException e) {
            throw new LocalGitBackend.GitFailure("INVALID_ROOT", "Cooper could not resolve the project directory.", e);
        }
        File file = new File(root, relativePath);
        StringBuilder output = new StringBuilder();
        boolean truncated = false;
        try (Git git = Git.open(root)) {
            Repository repository = git.getRepository();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DiffFormatter formatter = new DiffFormatter(bytes)) {
                formatter.setRepository(repository);
                formatter.setPathFilter(PathFilter.create(relativePath));
                formatter.setDetectRenames(true);
                List<DiffEntry> staged = git.diff().setCached(true).setPathFilter(PathFilter.create(relativePath)).call();
                for (DiffEntry entry : staged) {
                    bytes.reset();
                    formatter.format(entry);
                    output.append("=== STAGED ===\n").append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                }
                List<DiffEntry> unstaged = git.diff().setPathFilter(PathFilter.create(relativePath)).call();
                for (DiffEntry entry : unstaged) {
                    bytes.reset();
                    formatter.format(entry);
                    output.append("=== UNSTAGED ===\n").append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                }
            }
            if (output.length() == 0 && file.isFile()) {
                if (file.length() > 256 * 1024) {
                    truncated = true;
                    output.append("Untracked file preview omitted because it exceeds 256 KiB.");
                } else {
                    output.append("=== UNTRACKED / FILE CONTENT ===\n");
                    output.append(readText(file, 256 * 1024));
                }
            }
            if (output.length() == 0) output.append("No textual diff is available for this path.");
            if (output.length() > 256 * 1024) {
                output.setLength(256 * 1024);
                output.append("\n… Diff truncated by Cooper safety limit.");
                truncated = true;
            }
            return new DiffResult(relativePath, output.toString(), truncated);
        } catch (IOException | org.eclipse.jgit.api.errors.GitAPIException e) {
            throw new LocalGitBackend.GitFailure("DIFF_FAILED", "Cooper could not calculate the selected file diff.", e);
        }
    }

    private static String readText(File file, int maxBytes) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                if (output.size() > maxBytes) throw new IOException("Diff preview exceeded the size limit.");
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
