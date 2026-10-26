package mod.hilal.saif.activities.tools;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small durable journal for Cooper Git operations.
 * Stores operation metadata only: never store access tokens, passwords, or private keys here.
 * Writes use a temporary file and rename so a process interruption is less likely to corrupt
 * the last known journal. This class does not execute Git or network operations.
 */
public final class GitOperationStore {
    private static final String JOURNAL_NAME = "cooper_git_operations.json";
    private final File journal;
    private final Object lock = new Object();

    public static final class Record {
        public final String id;
        public final String type;
        public final String projectKey;
        public final String repository;
        public final String branch;
        public final GitOperationStateMachine.State state;
        public final long createdAt;
        public final long updatedAt;
        public final String safeDiagnostic;

        public Record(String id, String type, String projectKey, String repository, String branch,
                      GitOperationStateMachine.State state, long createdAt, long updatedAt,
                      String safeDiagnostic) {
            if (isBlank(id) || isBlank(type) || state == null) {
                throw new IllegalArgumentException("Operation id, type, and state are required.");
            }
            this.id = id;
            this.type = type;
            this.projectKey = nullToEmpty(projectKey);
            this.repository = nullToEmpty(repository);
            this.branch = nullToEmpty(branch);
            this.state = state;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.safeDiagnostic = nullToEmpty(safeDiagnostic);
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("type", type);
            json.put("projectKey", projectKey);
            json.put("repository", repository);
            json.put("branch", branch);
            json.put("state", state.name());
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("safeDiagnostic", safeDiagnostic);
            return json;
        }

        static Record fromJson(JSONObject json) throws JSONException {
            return new Record(json.getString("id"), json.getString("type"),
                    json.optString("projectKey", ""), json.optString("repository", ""),
                    json.optString("branch", ""),
                    GitOperationStateMachine.State.valueOf(json.getString("state")),
                    json.getLong("createdAt"), json.getLong("updatedAt"),
                    json.optString("safeDiagnostic", ""));
        }
    }

    public GitOperationStore(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required.");
        journal = new File(context.getApplicationContext().getFilesDir(), JOURNAL_NAME);
    }

    public void upsert(Record record) throws IOException {
        if (record == null) throw new IllegalArgumentException("Record is required.");
        synchronized (lock) {
            List<Record> records = readUnsafe();
            boolean replaced = false;
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).id.equals(record.id)) {
                    records.set(i, record);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) records.add(record);
            writeUnsafe(records);
        }
    }

    public List<Record> getAll() throws IOException {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(readUnsafe()));
        }
    }

    public List<Record> getRecoveryCandidates() throws IOException {
        List<Record> result = new ArrayList<>();
        for (Record record : getAll()) {
            GitOperationStateMachine.State state = record.state;
            if (state == GitOperationStateMachine.State.EXECUTING
                    || state == GitOperationStateMachine.State.VERIFYING
                    || state == GitOperationStateMachine.State.RECOVERY_REQUIRED
                    || state == GitOperationStateMachine.State.PARTIAL) {
                result.add(record);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private List<Record> readUnsafe() throws IOException {
        if (!journal.exists()) return new ArrayList<>();
        byte[] bytes = java.nio.file.Files.readAllBytes(journal.toPath());
        try {
            JSONArray array = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            List<Record> records = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) records.add(Record.fromJson(array.getJSONObject(i)));
            return records;
        } catch (JSONException | IllegalArgumentException e) {
            throw new IOException("Cooper Git operation journal is unreadable; preserve it for recovery review.", e);
        }
    }

    private void writeUnsafe(List<Record> records) throws IOException {
        JSONArray array = new JSONArray();
        try {
            for (Record record : records) array.put(record.toJson());
        } catch (JSONException e) {
            throw new IOException("Could not serialize Cooper Git operation metadata.", e);
        }
        File parent = journal.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            throw new IOException("Could not create Cooper Git operation journal directory.");
        }
        File temp = new File(parent, JOURNAL_NAME + ".tmp");
        byte[] bytes = array.toString().getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream stream = new FileOutputStream(temp, false)) {
            stream.write(bytes);
            stream.getFD().sync();
        }
        try {
            java.nio.file.Files.move(temp.toPath(), journal.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                java.nio.file.Files.move(temp.toPath(), journal.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailure) {
                temp.delete();
                throw new IOException("Could not finalize Cooper Git operation journal.", moveFailure);
            }
        } catch (IOException e) {
            temp.delete();
            throw new IOException("Could not finalize Cooper Git operation journal.", e);
        }
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
