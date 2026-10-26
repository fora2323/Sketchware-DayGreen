package mod.hilal.saif.activities.tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal GitHub REST client for Cooper. Uses only the fixed api.github.com origin.
 * Methods are synchronous and must be called from a worker thread, never the UI thread.
 * Access tokens are read from GitCredentialStore and are never included in errors.
 */
public final class GitHubApiClient {
    private static final String API_ORIGIN = "https://api.github.com";
    private static final String ACCEPT = "application/vnd.github+json";
    private static final String API_VERSION = "2022-11-28";
    private static final int CONNECT_TIMEOUT_MS = 12000;
    private static final int READ_TIMEOUT_MS = 20000;

    public static final class ApiException extends Exception {
        public final int httpStatus;
        public final String safeCode;
        ApiException(int status, String code, String message) {
            super(message);
            httpStatus = status;
            safeCode = code;
        }
    }

    private final GitHubAuthManager authManager;
    private final String clientId;

    public GitHubApiClient(GitHubAuthManager authManager, String clientId) {
        if (authManager == null) throw new IllegalArgumentException("Authentication manager is required.");
        this.authManager = authManager;
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public JSONObject getAuthenticatedAccount() throws ApiException {
        return request("GET", "/user", null);
    }

    public JSONArray listRepositories(int page, int perPage) throws ApiException {
        if (page < 1 || perPage < 1 || perPage > 100) {
            throw new IllegalArgumentException("Repository pagination is out of range.");
        }
        JSONObject response = requestArray("GET",
                "/user/repos?sort=updated&per_page=" + perPage + "&page=" + page, null);
        return response.optJSONArray("items") == null ? new JSONArray() : response.optJSONArray("items");
    }

    public JSONObject createRepository(String name, String description, boolean isPrivate,
                                       boolean explicitlyApproved) throws ApiException {
        requireApproval(explicitlyApproved, "Creating a GitHub repository requires explicit confirmation.");
        if (blank(name) || name.length() > 100 || name.contains("/") || name.contains("..")) {
            throw new IllegalArgumentException("A valid repository name is required.");
        }
        JSONObject body = new JSONObject();
        try {
            body.put("name", name.trim());
            body.put("description", description == null ? "" : description.trim());
            body.put("private", isPrivate);
            body.put("auto_init", false);
        } catch (JSONException e) {
            throw new ApiException(0, "INVALID_REQUEST", "Cooper could not prepare the repository request.");
        }
        return request("POST", "/user/repos", body);
    }

    public JSONObject getRepository(String owner, String repository) throws ApiException {
        validateSlug(owner, "owner");
        validateSlug(repository, "repository");
        return request("GET", "/repos/" + owner + "/" + repository, null);
    }

    public JSONObject getBranch(String owner, String repository, String branch) throws ApiException {
        validateSlug(owner, "owner");
        validateSlug(repository, "repository");
        if (blank(branch) || branch.startsWith("-") || branch.contains("..")) {
            throw new IllegalArgumentException("Invalid branch name.");
        }
        String encodedBranch;
        try { encodedBranch = java.net.URLEncoder.encode(branch, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException e) {
            throw new ApiException(0, "ENCODING_ERROR", "Cooper could not encode the branch name.");
        }
        return request("GET", "/repos/" + owner + "/" + repository + "/branches/" + encodedBranch, null);
    }

    public JSONArray listBranches(String owner, String repository, int page) throws ApiException {
        validateSlug(owner, "owner");
        validateSlug(repository, "repository");
        if (page < 1) throw new IllegalArgumentException("Page must be positive.");
        JSONObject response = requestArray("GET", "/repos/" + owner + "/" + repository
                + "/branches?per_page=100&page=" + page, null);
        JSONArray items = response.optJSONArray("items");
        return items == null ? new JSONArray() : items;
    }

    public JSONArray listCommits(String owner, String repository, String branch, int page) throws ApiException {
        validateSlug(owner, "owner");
        validateSlug(repository, "repository");
        if (blank(branch) || page < 1) throw new IllegalArgumentException("Branch and positive page are required.");
        String encodedBranch;
        try { encodedBranch = java.net.URLEncoder.encode(branch, "UTF-8"); }
        catch (java.io.UnsupportedEncodingException e) { throw new ApiException(0, "ENCODING_ERROR", "Cooper could not encode the branch name."); }
        JSONObject response = requestArray("GET", "/repos/" + owner + "/" + repository
                + "/commits?sha=" + encodedBranch + "&per_page=100&page=" + page, null);
        JSONArray items = response.optJSONArray("items");
        return items == null ? new JSONArray() : items;
    }

    public JSONObject getPullRequest(String owner, String repository, int number) throws ApiException {
        validateSlug(owner, "owner");
        validateSlug(repository, "repository");
        if (number < 1) throw new IllegalArgumentException("Pull Request number must be positive.");
        return request("GET", "/repos/" + owner + "/" + repository + "/pulls/" + number, null);
    }

    public JSONObject createPullRequest(String owner, String repository, String title, String body,
                                        String head, String base, boolean explicitlyApproved) throws ApiException {
        requireApproval(explicitlyApproved, "Creating a Pull Request requires explicit confirmation.");
        validateSlug(owner, "owner");
        validateSlug(repository, "repository");
        if (blank(title) || blank(head) || blank(base) || head.equals(base)) {
            throw new IllegalArgumentException("Title, source branch, and a different target branch are required.");
        }
        JSONObject payload = new JSONObject();
        try {
            payload.put("title", title.trim());
            payload.put("body", body == null ? "" : body);
            payload.put("head", head.trim());
            payload.put("base", base.trim());
            payload.put("draft", false);
        } catch (JSONException e) {
            throw new ApiException(0, "INVALID_REQUEST", "Cooper could not prepare the Pull Request request.");
        }
        return request("POST", "/repos/" + owner + "/" + repository + "/pulls", payload);
    }

    private JSONObject requestArray(String method, String path, JSONObject payload) throws ApiException {
        HttpURLConnection connection = null;
        try {
            connection = open(method, path);
            if (payload != null) writeBody(connection, payload);
            int status = connection.getResponseCode();
            String response = readResponse(connection, status);
            if (status < 200 || status >= 300) {
                if (status == 401) authManager.invalidateCredentials();
                throw httpError(status, response);
            }
            JSONArray array = new JSONArray(response);
            JSONObject wrapper = new JSONObject();
            wrapper.put("items", array);
            return wrapper;
        } catch (ApiException e) {
            throw e;
        } catch (GitHubAuthManager.AuthException e) {
            throw new ApiException(e.httpStatus, e.safeCode, e.getMessage());
        } catch (Exception e) {
            throw new ApiException(0, "NETWORK_OR_PARSE_ERROR",
                    "Cooper could not complete the GitHub request. Check the network and retry after verifying the remote state.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private JSONObject request(String method, String path, JSONObject payload) throws ApiException {
        HttpURLConnection connection = null;
        try {
            connection = open(method, path);
            if (payload != null) writeBody(connection, payload);
            int status = connection.getResponseCode();
            String response = readResponse(connection, status);
            if (status < 200 || status >= 300) {
                if (status == 401) authManager.invalidateCredentials();
                throw httpError(status, response);
            }
            return new JSONObject(response);
        } catch (ApiException e) {
            throw e;
        } catch (GitHubAuthManager.AuthException e) {
            throw new ApiException(e.httpStatus, e.safeCode, e.getMessage());
        } catch (Exception e) {
            throw new ApiException(0, "NETWORK_OR_PARSE_ERROR",
                    "Cooper could not complete the GitHub request. Check the network and retry after verifying the remote state.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpURLConnection open(String method, String path) throws Exception {
        if (path == null || !path.startsWith("/") || path.startsWith("//")
                || path.contains("\\r") || path.contains("\\n") || path.contains("://")) {
            throw new IllegalArgumentException("Invalid GitHub API path.");
        }
        final String token;
        try {
            token = authManager.getUsableAccessToken(clientId);
        } catch (GitHubAuthManager.AuthException e) {
            throw new ApiException(e.httpStatus, e.safeCode, e.getMessage());
        }
        if (blank(token)) throw new ApiException(401, "NOT_AUTHENTICATED", "Connect a GitHub account before using this operation.");
        URL url = new URL(API_ORIGIN + path);
        if (!"https".equals(url.getProtocol()) || !"api.github.com".equalsIgnoreCase(url.getHost())) {
            throw new ApiException(0, "HOST_REJECTED", "Cooper blocked a request to an untrusted host.");
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", ACCEPT);
        connection.setRequestProperty("X-GitHub-Api-Version", API_VERSION);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("User-Agent", "Cooper-Android-GitHub-Manager");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if ("POST".equals(method) || "PATCH".equals(method) || "PUT".equals(method)) {
            connection.setDoOutput(true);
        }
        return connection;
    }

    private static void writeBody(HttpURLConnection connection, JSONObject payload) throws IOException {
        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
            output.flush();
        }
    }

    private static String readResponse(HttpURLConnection connection, int status) throws IOException {
        InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                if (output.size() > 2 * 1024 * 1024) throw new IOException("Response too large.");
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static ApiException httpError(int status, String body) {
        String code;
        String message;
        if (status == 401) { code = "AUTH_EXPIRED_OR_INVALID"; message = "GitHub rejected the credentials. Reconnect the account."; }
        else if (status == 403 && body != null && body.toLowerCase(java.util.Locale.ROOT).contains("rate limit")) {
            code = "RATE_LIMITED"; message = "GitHub rate limit reached. Wait before retrying.";
        } else if (status == 403) { code = "PERMISSION_DENIED"; message = "GitHub denied this operation. Check repository permissions."; }
        else if (status == 404) { code = "NOT_FOUND"; message = "The requested GitHub resource was not found or is not accessible."; }
        else if (status == 409) { code = "REMOTE_CONFLICT"; message = "GitHub reported a conflict. Refresh remote state before retrying."; }
        else if (status == 422) { code = "VALIDATION_FAILED"; message = "GitHub rejected the request data. Review repository or branch settings."; }
        else if (status >= 500) { code = "GITHUB_UNAVAILABLE"; message = "GitHub is temporarily unavailable. Verify remote state before retrying."; }
        else { code = "HTTP_ERROR"; message = "GitHub request failed with HTTP status " + status + "."; }
        // Do not expose the raw response body: it can contain private repository metadata.
        return new ApiException(status, code, message);
    }

    private static void validateSlug(String value, String field) {
        if (blank(value) || !value.matches("[A-Za-z0-9_.-]{1,100}") || value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException("Invalid GitHub " + field + ".");
        }
    }

    private static void requireApproval(boolean approved, String message) throws ApiException {
        if (!approved) throw new ApiException(0, "APPROVAL_REQUIRED", message);
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
