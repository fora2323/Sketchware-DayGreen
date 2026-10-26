package mod.hilal.saif.activities.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted storage for GitHub OAuth access and refresh tokens. */
public final class GitCredentialStore {
    private static final String PREFS = "cooper_github_credentials";
    private static final String TOKEN_CIPHER_TEXT = "token_ciphertext";
    private static final String TOKEN_IV = "token_iv";
    private static final String KEY_ALIAS = "CooperGitHubCredentialKey";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final int GCM_TAG_BITS = 128;
    private static final int FORMAT_VERSION = 1;

    private final SharedPreferences preferences;

    public static final class TokenSet {
        public final String accessToken;
        public final String refreshToken;
        public final long accessTokenExpiresAtMillis;
        public final long refreshTokenExpiresAtMillis;
        public final String scope;
        public final String tokenType;

        public TokenSet(String accessToken, String refreshToken,
                        long accessTokenExpiresAtMillis, long refreshTokenExpiresAtMillis,
                        String scope, String tokenType) {
            if (blank(accessToken) || hasLineBreak(accessToken)) {
                throw new IllegalArgumentException("A valid access token is required.");
            }
            if (refreshToken != null && hasLineBreak(refreshToken)) {
                throw new IllegalArgumentException("Invalid refresh token.");
            }
            this.accessToken = accessToken;
            this.refreshToken = blank(refreshToken) ? null : refreshToken;
            this.accessTokenExpiresAtMillis = Math.max(0L, accessTokenExpiresAtMillis);
            this.refreshTokenExpiresAtMillis = Math.max(0L, refreshTokenExpiresAtMillis);
            this.scope = scope == null ? "" : scope;
            this.tokenType = blank(tokenType) ? "bearer" : tokenType;
        }

        public boolean accessTokenNeedsRefresh(long nowMillis, long leewayMillis) {
            if (accessTokenExpiresAtMillis <= 0L) return false;
            long leeway = Math.max(0L, leewayMillis);
            long threshold = nowMillis > Long.MAX_VALUE - leeway
                    ? Long.MAX_VALUE : nowMillis + leeway;
            return accessTokenExpiresAtMillis <= threshold;
        }

        public boolean refreshTokenExpired(long nowMillis) {
            return refreshTokenExpiresAtMillis > 0L && refreshTokenExpiresAtMillis <= nowMillis;
        }
    }

    public GitCredentialStore(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required.");
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Compatibility and migration path for older releases that stored only an access token. */
    public synchronized void saveAccessToken(String token) throws Exception {
        saveTokenSet(new TokenSet(token, null, 0L, 0L, "", "bearer"));
    }

    public synchronized void saveTokenSet(TokenSet tokenSet) throws Exception {
        if (tokenSet == null) throw new IllegalArgumentException("Token set is required.");
        JSONObject payload = new JSONObject();
        try {
            payload.put("format_version", FORMAT_VERSION);
            payload.put("access_token", tokenSet.accessToken);
            if (!blank(tokenSet.refreshToken)) payload.put("refresh_token", tokenSet.refreshToken);
            payload.put("access_token_expires_at_ms", tokenSet.accessTokenExpiresAtMillis);
            payload.put("refresh_token_expires_at_ms", tokenSet.refreshTokenExpiresAtMillis);
            payload.put("scope", tokenSet.scope);
            payload.put("token_type", tokenSet.tokenType);
        } catch (JSONException e) {
            throw new IllegalStateException("Cooper could not prepare encrypted credentials.");
        }

        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));
        boolean saved = preferences.edit()
                .putString(TOKEN_CIPHER_TEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(TOKEN_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .commit();
        if (!saved) throw new IllegalStateException("Cooper could not persist encrypted credentials.");
    }

    public synchronized TokenSet readTokenSet() throws Exception {
        String ciphertext = preferences.getString(TOKEN_CIPHER_TEXT, null);
        String ivValue = preferences.getString(TOKEN_IV, null);
        if (ciphertext == null || ivValue == null) return null;

        // Never mint a new key to decrypt old ciphertext. Missing key means re-authentication.
        SecretKey key = getExistingKey();
        if (key == null) {
            clearEncryptedValues();
            return null;
        }

        final String plaintext;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivValue, Base64.NO_WRAP)));
            plaintext = new String(cipher.doFinal(
                    Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (BadPaddingException | IllegalArgumentException e) {
            clearEncryptedValues();
            deleteKeyQuietly();
            return null;
        }

        if (blank(plaintext)) {
            clearEncryptedValues();
            return null;
        }

        // Migrate ciphertext produced by older builds, which encrypted the raw token string.
        if (!plaintext.trim().startsWith("{")) {
            TokenSet legacy = new TokenSet(plaintext, null, 0L, 0L, "", "bearer");
            saveTokenSet(legacy);
            return legacy;
        }

        try {
            JSONObject payload = new JSONObject(plaintext);
            String accessToken = payload.optString("access_token", "");
            if (blank(accessToken)) {
                clearEncryptedValues();
                deleteKeyQuietly();
                return null;
            }
            return new TokenSet(
                    accessToken,
                    payload.optString("refresh_token", null),
                    payload.optLong("access_token_expires_at_ms", 0L),
                    payload.optLong("refresh_token_expires_at_ms", 0L),
                    payload.optString("scope", ""),
                    payload.optString("token_type", "bearer"));
        } catch (JSONException | IllegalArgumentException e) {
            clearEncryptedValues();
            deleteKeyQuietly();
            return null;
        }
    }

    public synchronized String readAccessToken() throws Exception {
        TokenSet tokenSet = readTokenSet();
        return tokenSet == null ? null : tokenSet.accessToken;
    }

    public synchronized boolean hasCredential() {
        try {
            TokenSet tokenSet = readTokenSet();
            return tokenSet != null && !blank(tokenSet.accessToken);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void clear() {
        preferences.edit().clear().commit();
        deleteKeyQuietly();
    }

    private SecretKey getExistingKey() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
        if (!store.containsAlias(KEY_ALIAS)) return null;
        java.security.Key existing = store.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        throw new IllegalStateException("Cooper credential key is unavailable.");
    }

    private SecretKey getOrCreateKey() throws Exception {
        SecretKey existing = getExistingKey();
        if (existing != null) return existing;

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private void clearEncryptedValues() {
        preferences.edit().remove(TOKEN_CIPHER_TEXT).remove(TOKEN_IV).commit();
    }

    private static void deleteKeyQuietly() {
        try {
            KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
            store.load(null);
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS);
        } catch (Exception ignored) {
            // Never log credential or cryptographic failure details that could expose secrets.
        }
    }

    private static boolean hasLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
