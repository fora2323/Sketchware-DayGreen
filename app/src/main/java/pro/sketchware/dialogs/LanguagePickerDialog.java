package pro.sketchware.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

import pro.sketchware.R;

public class LanguagePickerDialog {

    public static final String PREF_LANGUAGE = "app_language";
    private static final String PREF_NAME = "language_pref";

    private static final List<String[]> LANGUAGES = Arrays.asList(
            new String[]{"system", "Follow system"},
            new String[]{"en", "English"},
            new String[]{"id", "Bahasa Indonesia"},
            new String[]{"ko", "한국어 (Korean)"},
            new String[]{"zh-CN", "简体中文 (Chinese Simplified)"},
            new String[]{"zh-TW", "繁體中文 (Chinese Traditional)"},
            new String[]{"ja", "日本語 (Japanese)"},
            new String[]{"tr", "Türkçe (Turkish)"},
            new String[]{"es", "Español (Spanish)"},
            new String[]{"fr", "Français (French)"},
            new String[]{"de", "Deutsch (German)"},
            new String[]{"ru", "Русский (Russian)"},
            new String[]{"pt", "Português (Portuguese)"}
    );

    public static void show(Activity activity) {
        String currentLang = getSavedLanguage(activity);

        // Cari indeks bahasa yang sedang aktif
        int selectedIndex = 0;
        for (int i = 0; i < LANGUAGES.size(); i++) {
            if (LANGUAGES.get(i)[0].equals(currentLang)) {
                selectedIndex = i;
                break;
            }
        }

        // Ambil daftar nama label bahasa untuk ditampilkan di Radio Group
        CharSequence[] labels = new CharSequence[LANGUAGES.size()];
        for (int i = 0; i < LANGUAGES.size(); i++) {
            labels[i] = LANGUAGES.get(i)[1];
        }

        final int[] checkedItem = {selectedIndex};

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Choose Language")
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    checkedItem[0] = which;
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton("OK", (dialog, which) -> {
                    String picked = LANGUAGES.get(checkedItem[0])[0];
                    saveLanguage(activity, picked);
                    applyLanguage(picked);
                })
                .show();
    }

    public static void applyLanguage(String langCode) {
        if (langCode.equals("system")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            LocaleListCompat localeList = LocaleListCompat.forLanguageTags(langCode);
            AppCompatDelegate.setApplicationLocales(localeList);
        }
        // Also update Application context configuration for immediate effect on pre-33
        updateApplicationContextLocale(pro.sketchware.SketchApplication.getContext(), langCode);
    }

    private static void updateApplicationContextLocale(Context context, String langCode) {
        if (context == null) return;
        java.util.Locale locale;
        if (langCode.equals("system")) {
            locale = android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            locale = java.util.Locale.forLanguageTag(langCode);
        }
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    public static Context attachBaseContext(Context base, String langCode) {
        if (langCode == null || langCode.equals("system")) {
            return base;
        }
        java.util.Locale locale = java.util.Locale.forLanguageTag(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }

    public static void applyOnStartup(Context context) {
        String saved = getSavedLanguage(context);
        applyLanguage(saved);
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(PREF_LANGUAGE, "system");
    }

    private static void saveLanguage(Context context, String langCode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(PREF_LANGUAGE, langCode).apply();
    }
}