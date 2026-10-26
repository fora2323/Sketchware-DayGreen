package mod.hilal.saif.activities.tools;

import android.content.Context;
import android.os.SystemClock;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** GitHub OAuth Device Flow and expiring access-token lifecycle for Cooper. */
public final class GitHubAuthManager {
    private static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final long TOKEN_REFRESH_LEEWAY_MS = 120_000L;
    private static final Object TOKEN_REFRESH_LOCK = new Object();

    public static final class AuthException extends Exception {
        public final String safeCode;
        public final int httpStatus;
        public final boolean retryable;

        AuthException(String code, String message) {
            this(code, message, 0, false);
        }

        AuthException(String code, String message, boolean retryable) {
            this(code, message, 0, retryable);
        }

        AuthException(String code, String message, int httpStatus, boolean retryable) {
            super(message);
            this.safeCode = code;
            this.httpStatus = httpStatus;
            this.retryable = retryable;
        }
    }

    public static final class DeviceAuthorization {
        public final String deviceCode;
        public final String userCode;
        public final String verificationUri;
        public final int expiresInSeconds;
        public final int pollingIntervalSeconds;

        private DeviceAuthorization(String deviceCode, String userCode, String verificationUri,
                                    int expiresInSeconds, int pollingIntervalSeconds) {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.expiresInSeconds = expiresInSeconds;
            this.pollingIntervalSeconds = pollingIntervalSeconds;
        }
    }

    private final GitCredentialStore credentialStore;

    public GitHubAuthManager(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required.");
        credentialStore = new GitCredentialStore(context.getApplicationContext());
    }

    public DeviceAuthorization beginDeviceAuthorization(String clientId) throws AuthException {
        if (blank(clientId)) {
            throw new AuthException("CLIENT_ID_NOT_CONFIGURED",
                    "Set the public COOPER_GITHUB_CLIENT_ID build variable and enable Device Flow in the GitHub OAuth app.");
        }

        JSONObject body = new JSONObject();
        try {
            body.put("client_id", clientId.trim());
            // offline_access opts in to expiring access tokens plus a refresh token.
            body.put("scope", "repo offline_access");
        } catch (JSONException e) {
            throw new AuthException("INVALID_DEVICE_REQUEST",
                    "Cooper could not prepare the GitHub device authorization request.");
        }

        JSONObject response = postJson(DEVICE_CODE_URL, body, "DEVICE_CODE_FAILED");
        throwOAuthErrorIfPresent(response);
        String deviceCode = response.optString("device_code", "");
        String userCode = response.optString("user_code", "");
        String verificationUri = response.optString("verification_uri", "");
        int expires = response.optInt("expires_in", 0);
        int interval = response.optInt("interval", 5);

        if (blank(deviceCode) || blank(userCode) || expires <= 0
                || !isTrustedVerificationUri(verificationUri)) {
            throw new AuthException("INVALID_DEVICE_RESPONSE",
                    "GitHub returned an incomplete device authorization response.");
        }
        return new DeviceAuthorization(deviceCode, userCode, verificationUri,
                expires, Math.max(5, interval));
    }

    public GitCredentialStore.TokenSet pollForAccessToken(
            String clientId, DeviceAuthorization authorization, AtomicBoolean cancelled)
            throws AuthException {
        if (authorization == null || blank(authorization.deviceCode) || blank(clientId)) {
            throw new AuthException("INVALID_DEVICE_SESSION",
                    "The GitHub device authorization session is invalid.");
        }

        long deadline = SystemClock.elapsedRealtime()
                + authorization.expiresInSeconds * 1000L;
        int interval = authorization.pollingIntervalSeconds;

        while (SystemClock.elapsedRealtime() < deadline) {
            sleepCancellable(interval * 1000L, cancelled, deadline);

            JSONObject body = new JSONObject();
            try {
                body.put("client_id", clientId.trim());
                body.put("device_code", authorization.deviceCode);
                body.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            } catch (JSONException e) {
                throw new AuthException("INVALID_DEVICE_REQUEST",
                        "Cooper could not prepare the GitHub token request.");
            }

            final JSONObject response;
            try {
                response = postJson(TOKEN_URL, body, "TOKEN_POLL_FAILED");
            } catch (AuthException e) {
                if (!e.retryable) throw e;
                interval = Math.min(Math.max(interval, 5) + 5, 60);
                continue;
            }

            String accessToken = response.optString("access_token", "");
            if (!blank(accessToken)) {
                GitCredentialStore.TokenSet tokenSet = parseTokenResponse(response, "");
                if (tokenSet.accessTokenExpiresAtMillis > 0L
                        && blank(tokenSet.refreshToken)) {
                    throw new AuthException("REFRESH_TOKEN_MISSING",
                            "GitHub returned an expiring token without a refresh token. Enable offline_access support and authorize Cooper again.");
                }
                try {
                    credentialStore.saveTokenSet(tokenSet);
                } catch (Exception e) {
                    throw new AuthException("CREDENTIAL_STORE_FAILED",
                            "Cooper could not securely store the GitHub token set.");
                }
                return tokenSet;
            }

            String error = response.optString("error", "");
            if ("authorization_pending".equals(error)) continue;
            if ("slow_down".equals(error)) {
                interval = Math.max(interval + 5, response.optInt("interval", 0));
                continue;
            }
            AuthException oauthError = mapOAuthError(
                    error, response.optString("error_description", ""));
            if (oauthError.retryable) {
                interval = Math.min(Math.max(interval, 5) + 5, 60);
                continue;
            }
            throw oauthError;
        }

        throw new AuthException("DEVICE_CODE_EXPIRED",
                "The GitHub device code expired. Start sign-in again.");
    }

    /**
     * Return a usable token. Refresh tokens rotate on use, so refresh requests must be serialized.
     * A refresh attempt with an unknown outcome is not blindly replayed with the old token.
     */
    public String getUsableAccessToken(String clientId) throws AuthException {
        synchronized (TOKEN_REFRESH_LOCK) {
            final GitCredentialStore.TokenSet current;
            try {
                current = credentialStore.readTokenSet();
            } catch (Exception e) {
                credentialStore.clear();
                throw new AuthException("CREDENTIAL_STORE_UNAVAILABLE",
                        "Cooper could not read the encrypted GitHub credentials. Sign in again.");
            }

            if (current == null || blank(current.accessToken)) {
                throw new AuthException("NOT_AUTHENTICATED",
                        "Connect a GitHub account before using this operation.", 401, false);
            }

            long now = System.currentTimeMillis();
            if (!current.accessTokenNeedsRefresh(now, TOKEN_REFRESH_LEEWAY_MS)) {
                return current.accessToken;
            }

            if (blank(current.refreshToken)
                    || current.refreshTokenExpired(now)) {
                credentialStore.clear();
                throw new AuthException("REAUTH_REQUIRED",
                        "The GitHub access token expired and no valid refresh token remains. Sign in again.",
                        401, false);
            }

            if (blank(clientId)) {
                throw new AuthException("CLIENT_ID_NOT_CONFIGURED",
                        "The public GitHub OAuth client ID is missing from this build.");
            }

            JSONObject body = new JSONObject();
            try {
                body.put("client_id", clientId.trim());
                body.put("grant_type", "refresh_token");
                body.put("refresh_token", current.refreshToken);
            } catch (JSONException e) {
                throw new AuthException("INVALID_REFRESH_REQUEST",
                        "Cooper could not prepare the GitHub token refresh request.");
            }

            final JSONObject response;
            try {
                response = postJson(TOKEN_URL, body, "TOKEN_REFRESH_FAILED");
                throwOAuthErrorIfPresent(response);
            } catch (AuthException e) {
                if ("REFRESH_TOKEN_INVALID".equals(e.safeCode)
                        || "REAUTH_REQUIRED".equals(e.safeCode)) {
                    credentialStore.clear();
                    throw new AuthException("REAUTH_REQUIRED",
                            "The GitHub refresh token is invalid or expired. Sign in again.",
                            401, false);
                }
                if (e.retryable) {
                    // Keep the existing pair on an unconfirmed transient failure. A later retry can
                    // succeed if GitHub did not consume the refresh token; if it was rotated, the
                    // explicit bad_refresh_token response below will trigger Device Flow again.
                    throw new AuthException("TOKEN_REFRESH_RETRYABLE",
                            "GitHub token renewal could not be confirmed. Retry when the network is stable; Cooper will request a new sign-in if GitHub reports that the refresh token was already used.",
                            e.httpStatus, true);
                }
                throw e;
            }

            GitCredentialStore.TokenSet refreshed = parseTokenResponse(response, current.scope);
            if (refreshed.accessTokenExpiresAtMillis > 0L && blank(refreshed.refreshToken)) {
                credentialStore.clear();
                throw new AuthException("REFRESH_TOKEN_MISSING",
                        "GitHub did not return the renewed refresh token. Sign in again.",
                        401, false);
            }

            try {
                credentialStore.saveTokenSet(refreshed);
            } catch (Exception e) {
                // Token rotation may have invalidated the previous refresh token.
                credentialStore.clear();
                throw new AuthException("CREDENTIAL_STORE_FAILED",
                        "Cooper could not securely save the renewed GitHub token pair. Sign in again.");
            }
            return refreshed.accessToken;
        }
    }

    public void invalidateCredentials() {
        credentialStore.clear();
    }

    private static GitCredentialStore.TokenSet parseTokenResponse(
            JSONObject response, String fallbackScope) throws AuthException {
        String accessToken = response.optString("access_token", "");
        if (blank(accessToken)) {
            throw mapOAuthError(response.optString("error", ""),
                    response.optString("error_description", ""));
        }

        long now = System.currentTimeMillis();
        long accessExpiry = expiryFrom(now, response.optLong("expires_in", 0L));
        long refreshExpiry = expiryFrom(now, response.optLong("refresh_token_expires_in", 0L));
        String scope = response.optString("scope", fallbackScope == null ? "" : fallbackScope);
        String tokenType = response.optString("token_type", "bearer");
        try {
            return new GitCredentialStore.TokenSet(accessToken,
                    response.optString("refresh_token", ""),
                    accessExpiry, refreshExpiry, scope, tokenType);
        } catch (IllegalArgumentException e) {
            throw new AuthException("INVALID_TOKEN_RESPONSE",
                    "GitHub returned an invalid token response.");
        }
    }

    private static long expiryFrom(long nowMillis, long seconds) {
        if (seconds <= 0L) return 0L;
        if (seconds > (Long.MAX_VALUE - nowMillis) / 1000L) return Long.MAX_VALUE;
        return nowMillis + seconds * 1000L;
    }

    private static void sleepCancellable(long delayMillis, AtomicBoolean cancelled, long deadline)
            throws AuthException {
        long now = SystemClock.elapsedRealtime();
        long waitUntil = Math.min(deadline, now + Math.max(0L, delayMillis));
        while (SystemClock.elapsedRealtime() < waitUntil) {
            if (cancelled != null && cancelled.get()) {
                throw new AuthException("CANCELLED", "GitHub sign-in was cancelled.");
            }
            long remaining = waitUntil - SystemClock.elapsedRealtime();
            try {
                Thread.sleep(Math.min(250L, Math.max(1L, remaining)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AuthException("CANCELLED", "GitHub sign-in was cancelled.");
            }
        }
        if (cancelled != null && cancelled.get()) {
            throw new AuthException("CANCELLED", "GitHub sign-in was cancelled.");
        }
        if (SystemClock.elapsedRealtime() >= deadline) {
            throw new AuthException("DEVICE_CODE_EXPIRED",
                    "The GitHub device code expired. Start sign-in again.");
        }
    }

    private static JSONObject postJson(String endpoint, JSONObject payload, String errorCode)
            throws AuthException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint);
            if (!"https".equals(url.getProtocol()) || !"github.com".equalsIgnoreCase(url.getHost())) {
                throw new AuthException("HOST_REJECTED",
                        "Cooper blocked an untrusted authentication host.");
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("User-Agent", "Cooper-Android-GitHub-Manager");

            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
                output.flush();
            }

            int status = connection.getResponseCode();
            if (status == 429 || status >= 500) {
                throw new AuthException("AUTH_SERVICE_TEMPORARY",
                        "GitHub authentication is temporarily unavailable.",
                        status, true);
            }

            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (input == null) {
                throw new AuthException(errorCode,
                        "GitHub returned an empty authentication response.", status, false);
            }

            String response;
            try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    if (output.size() > 64 * 1024) {
                        throw new AuthException(errorCode,
                                "GitHub returned an oversized authentication response.", status, false);
                    }
                }
                response = new String(output.toByteArray(), StandardCharsets.UTF_8);
            }

            final JSONObject json;
            try {
                json = new JSONObject(response);
            } catch (JSONException e) {
                throw new AuthException(errorCode,
                        "GitHub returned an invalid authentication response.", status, false);
            }

            if (status < 200 || status >= 300) {
                String oauthError = json.optString("error", "");
                if (!blank(oauthError)) throw mapOAuthError(oauthError, json.optString("error_description", ""));
                throw new AuthException(errorCode,
                        "GitHub authentication request failed. Check Device Flow configuration.", status, false);
            }
            return json;
        } catch (AuthException e) {
            throw e;
        } catch (IOException e) {
            throw new AuthException("AUTH_NETWORK_ERROR",
                    "Could not reach GitHub authentication. Check the network and retry.", true);
        } catch (Exception e) {
            throw new AuthException(errorCode,
                    "GitHub authentication could not complete. Check the network and OAuth app settings.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void throwOAuthErrorIfPresent(JSONObject response) throws AuthException {
        String error = response == null ? "" : response.optString("error", "");
        if (!blank(error)) {
            throw mapOAuthError(error, response.optString("error_description", ""));
        }
    }

    private static AuthException mapOAuthError(String error, String detail) {
        if ("device_flow_disabled".equals(error)) {
            return new AuthException("DEVICE_FLOW_DISABLED",
                    "Enable Device Flow in the Cooper GitHub OAuth app settings, then retry.");
        }
        if ("incorrect_client_credentials".equals(error)) {
            return new AuthException("INCORRECT_CLIENT_ID",
                    "GitHub rejected COOPER_GITHUB_CLIENT_ID. Check the OAuth client ID supplied to this build.");
        }
        if ("access_denied".equals(error)) {
            return new AuthException("ACCESS_DENIED",
                    "GitHub authorization was denied. Start a new sign-in when ready.");
        }
        if ("expired_token".equals(error) || "token_expired".equals(error)) {
            return new AuthException("DEVICE_CODE_EXPIRED",
                    "The GitHub device code expired. Start sign-in again.");
        }
        if ("incorrect_device_code".equals(error) || "bad_verification_code".equals(error)) {
            return new AuthException("INVALID_DEVICE_CODE",
                    "GitHub rejected the device code. Start a new sign-in.");
        }
        if ("bad_refresh_token".equals(error)) {
            return new AuthException("REFRESH_TOKEN_INVALID",
                    "The GitHub refresh token is invalid or expired. Sign in again.");
        }
        if ("unsupported_grant_type".equals(error)) {
            return new AuthException("UNSUPPORTED_GRANT_TYPE",
                    "GitHub rejected the OAuth grant type. Check the Device Flow configuration.");
        }
        if ("temporarily_unavailable".equals(error) || "server_error".equals(error)) {
            return new AuthException("AUTH_SERVICE_TEMPORARY",
                    "GitHub authentication is temporarily unavailable.", true);
        }
        String safeError = blank(error) ? "unknown OAuth error"
                : error.replaceAll("[^A-Za-z0-9_ -]", "");
        return new AuthException("OAUTH_ERROR",
                "GitHub authentication failed (" + safeError
                        + "). Review Device Flow configuration and try again.");
    }

    private static boolean isTrustedVerificationUri(String value) {
        if (blank(value)) return false;
        try {
            URL url = new URL(value);
            return "https".equals(url.getProtocol())
                    && "github.com".equalsIgnoreCase(url.getHost())
                    && "/login/device".equals(url.getPath());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
