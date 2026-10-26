package mod.hilal.saif.activities.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lightweight Cooper upload preflight. Findings intentionally omit matched secret text.
 * This scanner is defense-in-depth, not a guarantee that every secret can be detected.
 */
public final class GitSecretScanner {
    public enum Severity { REVIEW, BLOCK }
    public static final class Finding {
        public final String path;
        public final String rule;
        public final Severity severity;

        Finding(String path, String rule, Severity severity) {
            this.path = path;
            this.rule = rule;
            this.severity = severity;
        }
    }

    private static final Pattern[] BLOCK_PATTERNS = new Pattern[] {
        Pattern.compile("(?i)gh[pousr]_[A-Za-z0-9_]{20,}"),
        Pattern.compile("(?i)github_pat_[A-Za-z0-9_]{20,}"),
        Pattern.compile("(?i)(api[_-]?key|client[_-]?secret|access[_-]?token|password)\\s*[:=]\\s*[\"'][^\"'\\s]{8,}[\"']"),
        Pattern.compile("-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"),
        Pattern.compile("(?i)AKIA[0-9A-Z]{16}"),
        Pattern.compile("(?i)(api[_-]?key|client[_-]?secret|access[_-]?token|password)\\s*[:=]\\s*[A-Za-z0-9_./+\\-]{16,}"),
    };

    private GitSecretScanner() {}

    public static List<Finding> scan(String path, CharSequence content) {
        List<Finding> findings = new ArrayList<>();
        if (GitSafetyGuard.isSensitivePath(path)) {
            findings.add(new Finding(path, "sensitive-file-name", Severity.BLOCK));
            return Collections.unmodifiableList(findings);
        }
        if (content == null) return Collections.emptyList();
        String text = content.toString();
        for (int i = 0; i < BLOCK_PATTERNS.length; i++) {
            if (BLOCK_PATTERNS[i].matcher(text).find()) {
                findings.add(new Finding(path, "credential-pattern-" + (i + 1), Severity.BLOCK));
            }
        }
        if (text.contains("BEGIN CERTIFICATE") || text.contains("PRIVATE KEY")) {
            findings.add(new Finding(path, "certificate-or-key-material-review", Severity.REVIEW));
        }
        return Collections.unmodifiableList(findings);
    }

    public static boolean blocksUpload(List<Finding> findings) {
        if (findings == null) return false;
        for (Finding finding : findings) {
            if (finding != null && finding.severity == Severity.BLOCK) return true;
        }
        return false;
    }
}
