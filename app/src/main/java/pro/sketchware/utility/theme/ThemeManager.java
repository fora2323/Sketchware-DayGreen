package pro.sketchware.utility.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    private static final String THEME_PREF = "themedata";
    private static final String THEME_KEY = "idetheme";
    private static final String DYNAMIC_COLOR_KEY = "ide_dynamic_color";

    public static void applyTheme(Context context, int type) {
        saveTheme(context, type);

        switch (type) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        // Kirim broadcast untuk memberitahu semua Activity
        Intent intent = new Intent("THEME_CHANGED");
        intent.putExtra("theme", type);
        context.sendBroadcast(intent);
    }

    public static int getCurrentTheme(Context context) {
        return getPreferences(context).getInt(THEME_KEY, THEME_SYSTEM);
    }

    public static boolean isSystemTheme(Context context) {
        return getCurrentTheme(context) == THEME_SYSTEM;
    }

    public static int getSystemAppliedTheme(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_NO -> THEME_LIGHT;
            case Configuration.UI_MODE_NIGHT_YES -> THEME_DARK;
            default -> THEME_SYSTEM;
        };
    }

    public static boolean isIdeDynamicColorEnabled(Context context) {
        return getPreferences(context).getBoolean(DYNAMIC_COLOR_KEY, false);
    }

    public static void setIdeDynamicColorEnabled(Context context, boolean enabled) {
        getPreferences(context).edit().putBoolean(DYNAMIC_COLOR_KEY, enabled).apply();
        // Notify activities to recreate and apply dynamic colors
        Intent intent = new Intent("THEME_CHANGED");
        context.sendBroadcast(intent);
    }

    private static void saveTheme(Context context, int theme) {
        getPreferences(context).edit().putInt(THEME_KEY, theme).apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
    }
}