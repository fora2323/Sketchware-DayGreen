package mod.hilal.saif.activities.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cooper project-boundary checks. This is a safety primitive, not a replacement for
 * per-operation authorization or a filesystem transaction/backup strategy.
 */
public final class GitSafetyGuard {
    public enum Code { ALLOW, INVALID_ROOT, PATH_OUTSIDE_PROJECT, INVALID_PATH, FILE_NOT_ALLOWED }

    public static final class Validation {
        public final Code code;
        public final String safeMessage;
        public final List<String> normalizedPaths;

        private Validation(Code code, String message, List<String> paths) {
            this.code = code;
            this.safeMessage = message;
            this.normalizedPaths = Collections.unmodifiableList(new ArrayList<>(paths));
        }

        public boolean isAllowed() { return code == Code.ALLOW; }
    }

    private GitSafetyGuard() {}

    public static Validation validatePaths(File projectRoot, List<String> relativePaths) {
        if (projectRoot == null || relativePaths == null) {
            return new Validation(Code.INVALID_ROOT, "Project root or path list is missing.", Collections.emptyList());
        }
        try {
            File canonicalRoot = projectRoot.getCanonicalFile();
            if (!canonicalRoot.exists() || !canonicalRoot.isDirectory()) {
                return new Validation(Code.INVALID_ROOT, "The selected Cooper project directory is unavailable.", Collections.emptyList());
            }
            String rootPath = canonicalRoot.getPath();
            List<String> safe = new ArrayList<>();
            for (String raw : relativePaths) {
                if (raw == null || raw.trim().isEmpty() || raw.indexOf('\0') >= 0) {
                    return new Validation(Code.INVALID_PATH, "An empty or invalid project path was rejected.", safe);
                }
                String portable = raw.replace('\\', '/');
                if (portable.equals(".") || portable.equals("./") || portable.trim().isEmpty()) {
                    return new Validation(Code.INVALID_PATH, "The project root itself cannot be staged as a file path.", safe);
                }
                if (portable.startsWith("/") || portable.matches("^[A-Za-z]:.*")) {
                    return new Validation(Code.PATH_OUTSIDE_PROJECT, "Absolute paths are not permitted.", safe);
                }
                String[] parts = portable.split("/");
                for (String part : parts) {
                    if ("..".equals(part)) {
                        return new Validation(Code.PATH_OUTSIDE_PROJECT, "Parent-directory traversal is not permitted.", safe);
                    }
                }
                File candidate = new File(canonicalRoot, portable).getCanonicalFile();
                String candidatePath = candidate.getPath();
                if (!candidatePath.equals(rootPath) && !candidatePath.startsWith(rootPath + File.separator)) {
                    return new Validation(Code.PATH_OUTSIDE_PROJECT, "A path resolves outside the authorized project directory.", safe);
                }
                safe.add(portable);
            }
            return new Validation(Code.ALLOW, "All paths remain within the authorized project directory.", safe);
        } catch (IOException | SecurityException e) {
            return new Validation(Code.INVALID_PATH, "A project path could not be safely resolved.", Collections.emptyList());
        }
    }

    public static boolean isExcludedByDefault(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) return true;
        String path = relativePath.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.equals(".git") || segment.equals(".gradle") || segment.equals("build")
                    || segment.equals("bin") || segment.equals("gen") || segment.equals("cache")
                    || segment.equals("caches") || segment.equals("tmp") || segment.equals("temp")
                    || segment.equals("outputs") || segment.equals("intermediates")) return true;
        }
        String name = segments[segments.length - 1];
        return name.endsWith(".apk") || name.endsWith(".aab") || name.endsWith(".dex")
                || name.endsWith(".class") || name.endsWith(".odex") || name.endsWith(".oat")
                || name.endsWith(".so") || name.endsWith(".jar") || name.endsWith(".aar")
                || name.endsWith(".log") || name.endsWith(".tmp") || name.endsWith(".swp")
                || name.endsWith("~");
    }

    public static boolean isSensitivePath(String relativePath) {
        if (relativePath == null) return true;
        String path = relativePath.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.equals(".env") || name.startsWith(".env.")
                || name.endsWith(".jks") || name.endsWith(".keystore")
                || name.endsWith(".p12") || name.endsWith(".pfx")
                || name.equals("credentials") || name.equals("credentials.json")
                || name.equals("secrets.properties") || name.equals("local.properties")
                || name.contains("service-account") || name.contains("private_key");
    }
}
