package mod.hilal.saif.activities.tools;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;

/**
 * Computes Cooper local/remote divergence from Git object history.
 * It does not perform network requests or modify project files; callers fetch first when needed.
 */
public final class GitSyncEngine {
    public enum State {
        CLEAN, LOCAL_CHANGES, AHEAD, BEHIND, DIVERGED, CONFLICTED, REMOTE_NOT_FOUND, UNKNOWN
    }

    public static final class Snapshot {
        public final State state;
        public final String branch;
        public final String localHead;
        public final String remoteHead;
        public final int aheadBy;
        public final int behindBy;
        public final int changedFileCount;

        private Snapshot(State state, String branch, String localHead, String remoteHead,
                         int aheadBy, int behindBy, int changedFileCount) {
            this.state = state;
            this.branch = branch;
            this.localHead = localHead;
            this.remoteHead = remoteHead;
            this.aheadBy = aheadBy;
            this.behindBy = behindBy;
            this.changedFileCount = changedFileCount;
        }
    }

    public Snapshot inspect(File projectRoot) throws LocalGitBackend.GitFailure {
        if (projectRoot == null) throw new LocalGitBackend.GitFailure("INVALID_ROOT", "A Cooper project root is required.");
        try (Git git = Git.open(projectRoot.getCanonicalFile())) {
            Repository repository = git.getRepository();
            Status status = git.status().call();
            String branch = repository.getBranch();
            ObjectId local = repository.resolve("HEAD");
            ObjectId remote = repository.resolve("refs/remotes/origin/" + branch);
            String localName = local == null ? "" : local.name();
            String remoteName = remote == null ? "" : remote.name();
            int changed = status.getAdded().size() + status.getChanged().size()
                    + status.getRemoved().size() + status.getModified().size()
                    + status.getMissing().size() + status.getUntracked().size()
                    + status.getConflicting().size();
            if (!status.getConflicting().isEmpty()) {
                return new Snapshot(State.CONFLICTED, branch, localName, remoteName, 0, 0, changed);
            }
            if (!status.isClean()) {
                return new Snapshot(State.LOCAL_CHANGES, branch, localName, remoteName, 0, 0, changed);
            }
            if (local == null) {
                return new Snapshot(State.UNKNOWN, branch, "", remoteName, 0, 0, changed);
            }
            if (remote == null) {
                return new Snapshot(State.REMOTE_NOT_FOUND, branch, localName, "", 1, 0, changed);
            }
            if (local.equals(remote)) {
                return new Snapshot(State.CLEAN, branch, localName, remoteName, 0, 0, changed);
            }
            int ahead = countReachable(repository, local, remote);
            int behind = countReachable(repository, remote, local);
            State state = ahead > 0 && behind > 0 ? State.DIVERGED
                    : ahead > 0 ? State.AHEAD
                    : behind > 0 ? State.BEHIND : State.UNKNOWN;
            return new Snapshot(state, branch, localName, remoteName, ahead, behind, changed);
        } catch (IOException | org.eclipse.jgit.api.errors.GitAPIException e) {
            throw new LocalGitBackend.GitFailure("SYNC_INSPECTION_FAILED",
                    "Cooper could not calculate local/remote sync state.", e);
        }
    }

    private static int countReachable(Repository repository, ObjectId start, ObjectId exclude) throws IOException {
        int count = 0;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit startCommit = walk.parseCommit(start);
            walk.markStart(startCommit);
            if (exclude != null) walk.markUninteresting(walk.parseCommit(exclude));
            for (RevCommit ignored : walk) {
                count++;
                if (count > 100000) throw new IOException("Commit history exceeds the safety limit.");
            }
        }
        return count;
    }
}
