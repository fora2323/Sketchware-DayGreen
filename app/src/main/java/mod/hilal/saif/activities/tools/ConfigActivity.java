package mod.hilal.saif.activities.tools;

import static pro.sketchware.utility.GsonUtils.getGson;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceDataStore;

import com.besome.sketch.editor.manage.library.LibraryCategoryView;
import com.besome.sketch.editor.manage.library.LibraryItemView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonParseException;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mod.hey.studios.util.Helper;
import mod.jbk.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.activities.settings.SettingsActivity;
import pro.sketchware.databinding.ActivityAppSettingsBinding;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ConfigActivity extends BaseAppCompatActivity {

    public static final File SETTINGS_FILE = new File(FileUtil.getExternalStorageDir(), ".sketchware/data/settings.json");
    public static final String SETTING_ALWAYS_SHOW_BLOCKS = "always-show-blocks";
    public static final String SETTING_BACKUP_DIRECTORY = "backup-dir";
    public static final String SETTING_ROOT_AUTO_INSTALL_PROJECTS = "root-auto-install-projects";
    public static final String SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING = "root-auto-open-after-installing";
    public static final String SETTING_BACKUP_FILENAME = "backup-filename";
    public static final String SETTING_SHOW_BUILT_IN_BLOCKS = "built-in-blocks";
    public static final String SETTING_SHOW_EVERY_SINGLE_BLOCK = "show-every-single-block";
    public static final String SETTING_USE_NEW_VERSION_CONTROL = "use-new-version-control";
    public static final String SETTING_USE_ASD_HIGHLIGHTER = "use-asd-highlighter";
    public static final String SETTING_CRITICAL_UPDATE_REMINDER = "critical-update-reminder";
    public static final String SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH = "palletteDir";
    public static final String SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH = "blockDir";

    public static String getBackupPath() {
        return DataStore.getInstance().getString(SETTING_BACKUP_DIRECTORY, "/.sketchware/backups/");
    }

    public static String getStringSettingValueOrSetAndGet(String settingKey, String toReturnAndSetIfNotFound) {
        var dataStore = DataStore.getInstance();
        Map<String, Object> settings = dataStore.getSettings();

        Object value = settings.get(settingKey);
        if (value instanceof String s) {
            return s;
        } else {
            dataStore.putString(settingKey, toReturnAndSetIfNotFound);
            dataStore.persist();

            return toReturnAndSetIfNotFound;
        }
    }

    public static String getBackupFileName() {
        return DataStore.getInstance().getString(SETTING_BACKUP_FILENAME, "$projectName v$versionName ($pkgName, $versionCode) $time(yyyy-MM-dd'T'HHmmss)");
    }

    public static boolean isSettingEnabled(String keyName) {
        return DataStore.getInstance().getBoolean(keyName, false);
    }

    public static void setSetting(String key, Object value) {
        var dataStore = DataStore.getInstance();
        if (value instanceof String s) {
            dataStore.putString(key, s);
        } else if (value instanceof Boolean b) {
            dataStore.putBoolean(key, b);
        } else {
            throw new IllegalArgumentException("Unhandled data type " + value.getClass());
        }
        dataStore.persist();
    }

    @NonNull
    private static HashMap<String, Object> readSettings() {
        HashMap<String, Object> settings;

        if (SETTINGS_FILE.exists()) {
            Exception toLog;

            try {
                settings = getGson().fromJson(FileUtil.readFile(SETTINGS_FILE.getAbsolutePath()), Helper.TYPE_MAP);

                if (settings != null) {
                    return settings;
                }

                toLog = new NullPointerException("settings == null");
                // fall-through to shared error handler
            } catch (JsonParseException e) {
                toLog = e;
                // fall-through to shared error handler
            }

            SketchwareUtil.toastError("Couldn't parse App Settings! Restoring defaults.");
            LogUtil.e("ConfigActivity", "Failed to parse App Settings.", toLog);
        }
        settings = new HashMap<>();
        restoreDefaultSettings(settings);

        return settings;
    }

    private static void restoreDefaultSettings(HashMap<String, Object> settings) {
        settings.clear();

        List<String> keys = Arrays.asList(SETTING_ALWAYS_SHOW_BLOCKS,
                SETTING_BACKUP_DIRECTORY,
                SETTING_ROOT_AUTO_INSTALL_PROJECTS,
                SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING,
                SETTING_SHOW_BUILT_IN_BLOCKS,
                SETTING_SHOW_EVERY_SINGLE_BLOCK,
                SETTING_USE_NEW_VERSION_CONTROL,
                SETTING_USE_ASD_HIGHLIGHTER,
                SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,
                SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH);

        for (String key : keys) {
            settings.put(key, getDefaultValue(key));
        }
        FileUtil.writeFile(SETTINGS_FILE.getAbsolutePath(), getGson().toJson(settings));
    }

    public static Object getDefaultValue(String key) {
        return switch (key) {
            case SETTING_ALWAYS_SHOW_BLOCKS,
                 SETTING_ROOT_AUTO_INSTALL_PROJECTS, SETTING_SHOW_BUILT_IN_BLOCKS,
                 SETTING_SHOW_EVERY_SINGLE_BLOCK, SETTING_USE_NEW_VERSION_CONTROL,
                 SETTING_USE_ASD_HIGHLIGHTER -> false;
            case SETTING_BACKUP_DIRECTORY -> "/.sketchware/backups/";
            case SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING -> true;
            case SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH ->
                    "/.sketchware/resources/block/My Block/palette.json";
            case SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH ->
                    "/.sketchware/resources/block/My Block/block.json";
            default -> throw new IllegalArgumentException("Unknown key '" + key + "'!");
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        var binding = ActivityAppSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.topAppBar.setTitle("App Settings");
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        setupPreferences(binding.content);

        {
            View view1 = binding.appBarLayout;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view1, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top + insets.top, right + insets.right, bottom);
                return i;
            });
        }

        {
            View view1 = binding.contentScroll;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view1, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top, right + insets.right, bottom + insets.bottom);
                return i;
            });
        }
    }

    private void setupPreferences(ViewGroup content) {
        var preferences = new ArrayList<LibraryCategoryView>();

        LibraryCategoryView logicCategory = new LibraryCategoryView(this);
        logicCategory.setTitle("Logic Editor");
        preferences.add(logicCategory);

        logicCategory.addLibraryItem(createSwitchPreference(R.drawable.ic_mtrl_block, "Built-in blocks", "May slow down loading blocks in Logic Editor.", SETTING_SHOW_BUILT_IN_BLOCKS, false), true);
        logicCategory.addLibraryItem(createSwitchPreference(R.drawable.ic_mtrl_list, "Show all variable blocks", "All variable blocks will be visible, even if you don't have variables for them.", SETTING_ALWAYS_SHOW_BLOCKS, false), true);
        logicCategory.addLibraryItem(createSwitchPreference(R.drawable.ic_mtrl_category, "Show all blocks of palettes", "Every single available block will be shown. Will slow down opening palettes!", SETTING_SHOW_EVERY_SINGLE_BLOCK, false), true);
        logicCategory.addLibraryItem(createSwitchPreference(R.drawable.ic_mtrl_code, "Enable block text input highlighting", "Enables syntax highlighting while editing blocks' text parameters.", SETTING_USE_ASD_HIGHLIGHTER, false), false);

        LibraryCategoryView rootCategory = new LibraryCategoryView(this);
        rootCategory.setTitle("Root Features");
        preferences.add(rootCategory);

        var installWithRoot = createSwitchPreference(R.drawable.ic_mtrl_android, "Install projects with root access", "Automatically installs project APKs after building using root access.", SETTING_ROOT_AUTO_INSTALL_PROJECTS, false);
        installWithRoot.sw_enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Shell.getShell(shell -> {
                    if (!shell.isRoot()) {
                        Snackbar.make(content, "Couldn't acquire root access", BaseTransientBottomBar.LENGTH_SHORT).show();
                        installWithRoot.sw_enable.setChecked(false);
                    } else {
                        setSetting(SETTING_ROOT_AUTO_INSTALL_PROJECTS, true);
                    }
                });
            } else {
                setSetting(SETTING_ROOT_AUTO_INSTALL_PROJECTS, false);
            }
        });
        rootCategory.addLibraryItem(installWithRoot, true);
        rootCategory.addLibraryItem(createSwitchPreference(R.drawable.ic_mtrl_apk_install, "Launch projects after installing", "Opens projects automatically after auto-installation using root.", SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING, true), false);

        LibraryCategoryView vcCategory = new LibraryCategoryView(this);
        vcCategory.setTitle("Version Control");
        preferences.add(vcCategory);

        vcCategory.addLibraryItem(createSwitchPreference(R.drawable.ic_mtrl_version_control, "Use new Version Control", "Enables custom version code and name for projects.", SETTING_USE_NEW_VERSION_CONTROL, false), false);

        LibraryCategoryView backupCategory = new LibraryCategoryView(this);
        backupCategory.setTitle("Backup");
        preferences.add(backupCategory);

        backupCategory.addLibraryItem(createPreference(R.drawable.ic_mtrl_folder, "Backup directory", "The default directory is /Internal storage" + getBackupPath(), v -> {
            DialogCreateNewFileLayoutBinding binding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
            binding.inputText.setText(getBackupPath());
            binding.chipGroupTypes.setVisibility(View.GONE);
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setView(binding.getRoot())
                    .setTitle("Backup directory")
                    .setMessage("Directory inside /Internal storage/, e.g. .sketchware/backups")
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .setPositiveButton(R.string.common_word_save, null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(
                        Helper.getDialogDismissListener(dialogInterface));
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(view -> {
                    setSetting(SETTING_BACKUP_DIRECTORY, Helper.getText(binding.inputText));
                    dialog.dismiss();
                });

                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                binding.inputText.requestFocus();
            });
            dialog.show();
        }), true);

        backupCategory.addLibraryItem(createPreference(R.drawable.ic_mtrl_edit, "Backup filename format", "Default is \"$projectName v$versionName ($pkgName, $versionCode) $time(yyyy-MM-dd'T'HHmmss)\"", v -> {
            DialogCreateNewFileLayoutBinding binding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
            binding.chipGroupTypes.setVisibility(View.GONE);
            binding.inputText.setText(getBackupFileName());

            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setView(binding.getRoot())
                    .setTitle("Backup filename format")
                    .setMessage("This defines how SWB backup files get named.\n" +
                            "Available variables:\n" +
                            " - $projectName - Project name\n" +
                            " - $versionCode - App version code\n" +
                            " - $versionName - App version name\n" +
                            " - $pkgName - App package name\n" +
                            " - $timeInMs - Time during backup in milliseconds\n" +
                            "\n" +
                            "Additionally, you can format your own time like this using Java's date formatter syntax:\n" +
                            "$time(yyyy-MM-dd'T'HHmmss)\n")
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .setPositiveButton(R.string.common_word_save, null)
                    .setNeutralButton(R.string.common_word_reset, (dialogInterface, which) -> {
                        DataStore.getInstance().putString(SETTING_BACKUP_FILENAME, null);
                        Snackbar.make(content, "Reset to default complete.", BaseTransientBottomBar.LENGTH_SHORT).show();
                    })
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(
                        Helper.getDialogDismissListener(dialog));
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(view -> {
                    setSetting(SETTING_BACKUP_FILENAME, Helper.getText(binding.inputText));
                    dialog.dismiss();
                });
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                binding.inputText.requestFocus();
            });
            dialog.show();
        }), false);

        LibraryCategoryView appearanceCategory = new LibraryCategoryView(this);
        appearanceCategory.setTitle(Helper.getResString(R.string.settings_appearance));
        preferences.add(appearanceCategory);

        appearanceCategory.addLibraryItem(createPreference(R.drawable.ic_mtrl_palette, Helper.getResString(R.string.settings_appearance), Helper.getResString(R.string.settings_appearance_description), v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.FRAGMENT_TAG_EXTRA, SettingsActivity.SETTINGS_APPEARANCE_FRAGMENT);
            startActivity(intent);
        }), false);

        LibraryCategoryView systemCategory = new LibraryCategoryView(this);
        systemCategory.setTitle(Helper.getResString(R.string.main_drawer_title_system_settings));
        preferences.add(systemCategory);

        systemCategory.addLibraryItem(createP12SwitchPreference(R.drawable.ic_mtrl_vibration, Helper.getResString(R.string.system_settings_title_setting_vibration), Helper.getResString(R.string.system_settings_description_setting_vibration), "P12I0", true), true);
        systemCategory.addLibraryItem(createP12SwitchPreference(R.drawable.ic_mtrl_save, Helper.getResString(R.string.system_settings_title_automatically_save), Helper.getResString(R.string.system_settings_description_automatically_save), "P12I2", false), false);

        preferences.forEach(content::addView);
    }

    private LibraryItemView createPreference(int icon, String title, String desc, View.OnClickListener listener) {
        LibraryItemView preference = new LibraryItemView(this);
        preference.enabled.setVisibility(View.GONE);
        preference.icon.setImageResource(icon);
        preference.title.setText(title);
        preference.description.setText(desc);
        preference.setOnClickListener(listener);
        return preference;
    }

    private LibraryItemView createSwitchPreference(int icon, String title, String desc, String key, boolean defaultValue) {
        LibraryItemView preference = new LibraryItemView(this);
        preference.enabled.setVisibility(View.GONE);
        preference.icon.setImageResource(icon);
        preference.title.setText(title);
        preference.description.setText(desc);
        preference.sw_enable.setVisibility(View.VISIBLE);
        preference.sw_enable.setChecked(DataStore.getInstance().getBoolean(key, defaultValue));
        preference.sw_enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setSetting(key, isChecked);
        });
        preference.setOnClickListener(v -> preference.sw_enable.toggle());
        return preference;
    }

    private LibraryItemView createP12SwitchPreference(int icon, String title, String desc, String key, boolean defaultValue) {
        LibraryItemView preference = new LibraryItemView(this);
        preference.enabled.setVisibility(View.GONE);
        preference.icon.setImageResource(icon);
        preference.title.setText(title);
        preference.description.setText(desc);
        preference.sw_enable.setVisibility(View.VISIBLE);

        android.content.SharedPreferences sp = getSharedPreferences("P12", android.content.Context.MODE_PRIVATE);
        preference.sw_enable.setChecked(sp.getBoolean(key, defaultValue));
        preference.sw_enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(key, isChecked).apply();
        });
        preference.setOnClickListener(v -> preference.sw_enable.toggle());
        return preference;
    }

    /**
     * An in-memory caching store for settings listed in {@link ConfigActivity}.
     * Persists to {@link #SETTINGS_FILE}.
     *
     * @see #persist()
     */
    public static class DataStore extends PreferenceDataStore {
        private static DataStore INSTANCE;
        private final Map<String, Object> settings;

        private DataStore() {
            settings = readSettings();
        }

        public static DataStore getInstance() {
            return INSTANCE == null ? (INSTANCE = new DataStore()) : INSTANCE;
        }

        private Map<String, Object> getSettings() {
            return settings;
        }

        /**
         * Blocking method that writes its data to {@link #SETTINGS_FILE}. Should be called manually,
         * since there's no automatic persist. Meaning, every write, unless they are in batches.
         */
        public void persist() {
            FileUtil.writeFile(SETTINGS_FILE.getAbsolutePath(), getGson().toJson(settings));
        }

        @Override
        public void putString(String key, @Nullable String value) {
            if (value == null) {
                settings.remove(key);
            } else {
                settings.put(key, value);
            }
            persist();
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            var value = settings.get(key);
            if (value instanceof String s) {
                return s;
            }
            return defValue;
        }

        @Override
        public void putBoolean(String key, boolean value) {
            settings.put(key, value);
            persist();
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            var value = settings.get(key);
            if (value instanceof Boolean b) {
                return b;
            }
            return defValue;
        }
    }
}