package mod.jbk.build.cache;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mod.jbk.util.LogUtil;
import pro.sketchware.utility.FileUtil;

/**
 * Simple content-hash based build cache.
 * <p>
 * For each build "stage" (resources, java, dex, merge), stores the last known-good
 * input hash plus a copy of that stage's output artifacts. If, on a later build, the
 * freshly computed input hash matches the stored one, the cached output is restored
 * instead of re-running the (expensive) build step.
 * <p>
 * Lives outside the project's mysc/&lt;sc_id&gt;/ folder so it survives the full
 * temp-file wipe ({@code FileUtil.deleteFile(q.projectMyscPath)}) that happens at the
 * start of every build.
 */
public class BuildCache {
    private static final String TAG = "BuildCache";

    private final File cacheRoot;

    public BuildCache(String sc_id) {
        cacheRoot = new File(FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/build_cache");
        cacheRoot.mkdirs();
    }

    private File hashFile(String stage) {
        return new File(cacheRoot, stage + ".hash");
    }

    /**
     * Directory where a stage should store/restore its cached output files.
     * Created if it doesn't exist yet.
     */
    public File stageOutputDir(String stage) {
        File dir = new File(cacheRoot, stage + "_output");
        dir.mkdirs();
        return dir;
    }

    /**
     * @return true if the stage's cached output can be reused as-is (hash matches AND
     * a hash was actually computed for the current input).
     */
    public boolean isUpToDate(String stage, String currentHash) {
        if (currentHash == null) return false;
        File hf = hashFile(stage);
        if (!hf.exists()) return false;
        String stored = FileUtil.readFile(hf.getAbsolutePath());
        boolean upToDate = currentHash.equals(stored);
        LogUtil.d(TAG, "Stage '" + stage + "' up to date: " + upToDate);
        return upToDate;
    }

    public void markUpToDate(String stage, String currentHash) {
        if (currentHash == null) return;
        FileUtil.writeFile(hashFile(stage).getAbsolutePath(), currentHash);
    }

    /**
     * Wipes the entire build cache for this project. Call this from the "Clean
     * temporary files" action so users have a manual escape hatch if something looks stale.
     */
    public void invalidateAll() {
        FileUtil.deleteFile(cacheRoot.getAbsolutePath());
        cacheRoot.mkdirs();
    }

    /* ---------------------------------------------------------------- */
    /* Hashing helpers                                                   */
    /* ---------------------------------------------------------------- */

    /**
     * Hashes the full recursive content of one or more directories (or single files).
     * Missing paths are silently skipped, so a nonexistent optional file doesn't break hashing.
     */
    public static String hashDirectory(String... paths) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<File> allFiles = new ArrayList<>();
            for (String path : paths) {
                if (path == null) continue;
                collectFiles(new File(path), allFiles);
            }
            allFiles.sort(Comparator.comparing(File::getAbsolutePath));
            for (File f : allFiles) {
                digest.update(f.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                hashFileContentInto(digest, f);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hash directories " + List.of(paths), e);
            return null;
        }
    }

    /**
     * Hashes an explicit list of files (used for things like the DEX merge input set,
     * which is scattered across many different directories).
     */
    public static String hashFiles(List<File> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<File> sorted = new ArrayList<>(files);
            sorted.sort(Comparator.comparing(File::getAbsolutePath));
            for (File f : sorted) {
                digest.update(f.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                hashFileContentInto(digest, f);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hash file list", e);
            return null;
        }
    }

    /**
     * Hashes arbitrary extra strings (build settings, classpath, version info, etc.)
     * that affect a stage's output but aren't represented as files.
     */
    public static String hashStrings(String... extra) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String s : extra) {
                digest.update((s == null ? "null" : s).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hash strings", e);
            return null;
        }
    }

    /**
     * Combines several sub-hashes into one. If any sub-hash is null (hashing failed),
     * the combined result is also null, which forces a full rebuild of that stage
     * instead of silently trusting an incomplete hash.
     */
    public static String combine(String... hashes) {
        for (String h : hashes) {
            if (h == null) return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String h : hashes) sb.append(h).append('|');
        return sb.toString();
    }

    private static void collectFiles(File file, List<File> out) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) collectFiles(child, out);
            }
        } else {
            out.add(file);
        }
    }

    private static void hashFileContentInto(MessageDigest digest, File f) throws Exception {
        if (!f.exists() || f.isDirectory()) return;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}