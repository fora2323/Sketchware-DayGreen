package mod.hilal.saif.activities.tools;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import pro.sketchware.utility.FileUtil;

public class GitConfiguration {

    public static final String PREF_NAME = "git_settings";

    public static final String KEY_COLORIZE_FILE_NAMES = "git_colorize_file_names";
    public static final String KEY_GUTTER_INDICATION = "git_gutter_indication";
    public static final String KEY_CONFLICT_DETECTION = "git_conflict_detection";

    public static final String KEY_ACCOUNT_USERNAME = "git_account_username";
    public static final String KEY_ACCOUNT_PASSWORD = "git_account_password";

    public static final String KEY_USER_NAME = "git_user_name";
    public static final String KEY_USER_EMAIL = "git_user_email";

    public static final String KEY_COMMIT_SIGNING = "git_commit_signing";
    public static final String KEY_SIGNING_FORMAT = "git_signing_format";
    public static final String KEY_GPG_KEY = "git_gpg_key";
    public static final String KEY_GPG_PASSPHRASE = "git_gpg_passphrase";

    public static final String KEY_SUBMODULES = "git_submodules";
    public static final String KEY_RECURSIVE_SUBMODULES = "git_recursive_submodules";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        return getPrefs(context).getBoolean(key, defValue);
    }

    public static void setBoolean(Context context, String key, boolean value) {
        getPrefs(context).edit().putBoolean(key, value).apply();
        try {
            ConfigActivity.setSetting(key, value);
        } catch (Exception ignored) {
        }
    }

    public static String getString(Context context, String key, String defValue) {
        return getPrefs(context).getString(key, defValue);
    }

    public static void setString(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).apply();
        try {
            ConfigActivity.setSetting(key, value);
        } catch (Exception ignored) {
        }
    }

    public static String getUsername(Context context) {
        return getString(context, KEY_ACCOUNT_USERNAME, "");
    }

    public static void setUsername(Context context, String username) {
        setString(context, KEY_ACCOUNT_USERNAME, username);
    }

    public static String getPassword(Context context) {
        return getString(context, KEY_ACCOUNT_PASSWORD, "");
    }

    public static void setPassword(Context context, String password) {
        setString(context, KEY_ACCOUNT_PASSWORD, password);
    }

    public static String getUserName(Context context) {
        return getString(context, KEY_USER_NAME, "");
    }

    public static void setUserName(Context context, String name) {
        setString(context, KEY_USER_NAME, name);
    }

    public static String getUserEmail(Context context) {
        return getString(context, KEY_USER_EMAIL, "");
    }

    public static void setUserEmail(Context context, String email) {
        setString(context, KEY_USER_EMAIL, email);
    }

    public static String getSigningFormat(Context context) {
        return getString(context, KEY_SIGNING_FORMAT, "GPG");
    }

    public static void setSigningFormat(Context context, String format) {
        setString(context, KEY_SIGNING_FORMAT, format);
    }

    public static String getGpgKey(Context context) {
        return getString(context, KEY_GPG_KEY, "");
    }

    public static void setGpgKey(Context context, String key) {
        setString(context, KEY_GPG_KEY, key);
    }

    public static String getGpgPassphrase(Context context) {
        return getString(context, KEY_GPG_PASSPHRASE, "");
    }

    public static void setGpgPassphrase(Context context, String passphrase) {
        setString(context, KEY_GPG_PASSPHRASE, passphrase);
    }

    public static void saveGitConfig(Context context) {
        try {
            String userName = getUserName(context);
            String userEmail = getUserEmail(context);
            File gitDir = new File(FileUtils.getInternalStorageDir(), Configs.gitFolderDir);
            if (!gitDir.exists()) {
                gitDir.mkdirs();
            }
            File configFile = new File(gitDir, "config");
            StringBuilder sb = new StringBuilder();
            sb.append("[user]\n");
            if (!userName.isEmpty()) {
                sb.append("\tname = ").append(userName).append("\n");
            }
            if (!userEmail.isEmpty()) {
                sb.append("\temail = ").append(userEmail).append("\n");
            }
            FileUtil.writeFile(configFile.getAbsolutePath(), sb.toString());
        } catch (Exception ignored) {
        }
    }
}
