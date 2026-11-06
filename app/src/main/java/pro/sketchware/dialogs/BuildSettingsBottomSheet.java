package pro.sketchware.dialogs;

import static mod.hey.studios.build.BuildSettings.*;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import extensions.anbui.daydream.library.DRFeatureManager;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.project.ProjectSettings;
import pro.sketchware.databinding.ProjectConfigLayoutBinding;
import pro.sketchware.utility.SketchwareUtil;

public class BuildSettingsBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = BuildSettingsBottomSheet.class.getSimpleName();
    private static int totalViews = 0;

    private static final int VIEW_ANDROIR_JAR_PATH = totalViews++;
    private static final int VIEW_CLASS_PATH = totalViews++;
    private static final int VIEW_DEXER = totalViews++;
    private static final int VIEW_JAVA_VERSION = totalViews++;
    private static final int VIEW_NO_WARNINGS = totalViews++;
    private static final int VIEW_NO_HTTP_LEGACY = totalViews++;
    private static final int VIEW_ENABLE_LOGCAT = totalViews++;
    private static final int VIEW_ABI_ARM64_V8A = totalViews++;
    private static final int VIEW_ABI_ARMEABI_V7A = totalViews++;
    private static final int VIEW_ABI_X86 = totalViews++;
    private static final int VIEW_ABI_X86_64 = totalViews++;
    
    // ====== VIEWS BARU ======
    private static final int VIEW_BUILD_MODE = totalViews++;
    private static final int VIEW_RELEASE_KEYSTORE_PATH = totalViews++;
    private static final int VIEW_RELEASE_KEYSTORE_ALIAS = totalViews++;
    private static final int VIEW_RELEASE_STORE_PASS = totalViews++;
    private static final int VIEW_RELEASE_KEY_PASS = totalViews++;
    // ========================
    
    private View[] views;

    private ProjectConfigLayoutBinding binding;
    private BuildSettings projectSettings;

    public static BuildSettingsBottomSheet newInstance(String sc_id) {
        BuildSettingsBottomSheet sheet = new BuildSettingsBottomSheet();
        Bundle arguments = new Bundle();
        arguments.putString("sc_id", sc_id);
        sheet.setArguments(arguments);
        return sheet;
    }

    public static String[] getAvailableJavaVersions() {
        return new String[]{SETTING_JAVA_VERSION_1_7, SETTING_JAVA_VERSION_1_8, SETTING_JAVA_VERSION_11,
                DRFeatureManager.getForMinSDK() < 33 ? SETTING_JAVA_VERSION_15 : SETTING_JAVA_VERSION_17,
                DRFeatureManager.getForMinSDK() < 33 ? SETTING_JAVA_VERSION_16 : SETTING_JAVA_VERSION_20};
    }

    public static void handleJavaVersionChange(String choice) {
        if (!choice.equals(SETTING_JAVA_VERSION_1_7)) {
            SketchwareUtil.toast("Don't forget to enable D8 to be able to compile Java 8+ code");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        projectSettings = new BuildSettings(arguments.getString("sc_id"));
        views = new View[totalViews];
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ProjectConfigLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews();
        setupKeystorePickers();

        binding.noWarnings.setOnClickListener(v -> binding.cbNoWarnings.performClick());
        binding.noHttpLegacy.setOnClickListener(v -> binding.cbNoHttpLegacy.performClick());
        binding.enableLogcat.setOnClickListener(v -> binding.cbEnableLogcat.performClick());

        binding.tilAndroidJar.getEditText().setText(projectSettings.getValue(SETTING_ANDROID_JAR_PATH, ""));
        binding.tilClasspath.getEditText().setText(projectSettings.getValue(SETTING_CLASSPATH, ""));

        setRadioGroupOptions(binding.rgDexer, new String[]{"Dx", "D8"}, SETTING_DEXER, "Dx");
        setRadioGroupOptions(binding.rgJavaVersion, getAvailableJavaVersions(), SETTING_JAVA_VERSION, "1.7");
        setRadioGroupOptions(binding.rgBuildMode, new String[]{BUILD_MODE_DEBUG, BUILD_MODE_RELEASE}, SETTING_BUILD_MODE, BUILD_MODE_DEBUG);
        updateKeystoreVisibility(projectSettings.getValue(SETTING_BUILD_MODE, BUILD_MODE_DEBUG));

        binding.tilReleaseKeystorePath.getEditText().setText(projectSettings.getValue(SETTING_RELEASE_KEYSTORE_PATH, ""));
        binding.tilReleaseKeystoreAlias.getEditText().setText(projectSettings.getValue(SETTING_RELEASE_KEYSTORE_ALIAS, ""));
        binding.tilReleaseStorePass.getEditText().setText(projectSettings.getValue(SETTING_RELEASE_STORE_PASS, ""));
        binding.tilReleaseKeyPass.getEditText().setText(projectSettings.getValue(SETTING_RELEASE_KEY_PASS, ""));

        boolean nativeEnable = new ProjectSettings(getArguments().getString("sc_id")).getValue(ProjectSettings.SETTING_ENABLE_NATIVE, "false").equals("true");
        binding.cmakeSection.setVisibility(nativeEnable ? View.VISIBLE : View.GONE);

        String configuredAbis = projectSettings.getValue(SETTING_CMAKE_ABIS, "");
        if (!configuredAbis.isEmpty()) {
            binding.chipAbiArm64V8a.setChecked(configuredAbis.contains(ABI_ARM64_V8A));
            binding.chipAbiArmeabiV7a.setChecked(configuredAbis.contains(ABI_ARMEABI_V7A));
            binding.chipAbiX86.setChecked(configuredAbis.contains(ABI_X86));
            binding.chipAbiX8664.setChecked(configuredAbis.contains(ABI_X86_64));
        } else {
            setCheckboxValue(binding.chipAbiArm64V8a, SETTING_CMAKE_ABI_ARM64_V8A, true);
            setCheckboxValue(binding.chipAbiArmeabiV7a, SETTING_CMAKE_ABI_ARMEABI_V7A, true);
            setCheckboxValue(binding.chipAbiX86, SETTING_CMAKE_ABI_X86, false);
            setCheckboxValue(binding.chipAbiX8664, SETTING_CMAKE_ABI_X86_64, false);
        }

        setCheckboxValue(binding.cbNoWarnings, SETTING_NO_WARNINGS, true);
        setCheckboxValue(binding.cbNoHttpLegacy, SETTING_NO_HTTP_LEGACY, false);
        setCheckboxValue(binding.cbEnableLogcat, SETTING_ENABLE_LOGCAT, true);

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> {
            projectSettings.setValues(views);

            List<String> selectedAbis = new ArrayList<>();
            if (binding.chipAbiArm64V8a.isChecked()) selectedAbis.add(ABI_ARM64_V8A);
            if (binding.chipAbiArmeabiV7a.isChecked()) selectedAbis.add(ABI_ARMEABI_V7A);
            if (binding.chipAbiX86.isChecked()) selectedAbis.add(ABI_X86);
            if (binding.chipAbiX8664.isChecked()) selectedAbis.add(ABI_X86_64);

            projectSettings.setValue(SETTING_CMAKE_ABIS, TextUtils.join(",", selectedAbis));
            dismiss();
        });
    }

    private void updateKeystoreVisibility(String buildMode) {
        boolean isRelease = BUILD_MODE_RELEASE.equals(buildMode);
        binding.releaseKeystoreSection.setVisibility(isRelease ? View.VISIBLE : View.GONE);
    }

    private void setupKeystorePickers() {
        binding.tilReleaseKeystorePath.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
        binding.tilReleaseKeystorePath.setEndIconDrawable(android.R.drawable.ic_menu_search);
        binding.tilReleaseKeystorePath.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, 124);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            String path = data.getData().getPath();
            if (path != null) {
                if (path.contains("primary:")) {
                    path = android.os.Environment.getExternalStorageDirectory() + "/" + path.split("primary:")[1];
                } else if (path.contains("raw:")) {
                    path = path.replaceFirst(".*raw:", "");
                }
                if (requestCode == 124) {
                    binding.tilReleaseKeystorePath.getEditText().setText(path);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initializeViews() {
        binding.tilAndroidJar.getEditText().setTag(SETTING_ANDROID_JAR_PATH);
        binding.tilClasspath.getEditText().setTag(SETTING_CLASSPATH);
        binding.rgDexer.setTag(SETTING_DEXER);
        binding.rgJavaVersion.setTag(SETTING_JAVA_VERSION);
        binding.cbNoWarnings.setTag(SETTING_NO_WARNINGS);
        binding.cbNoHttpLegacy.setTag(SETTING_NO_HTTP_LEGACY);
        binding.cbEnableLogcat.setTag(SETTING_ENABLE_LOGCAT);
        binding.chipAbiArm64V8a.setTag(SETTING_CMAKE_ABI_ARM64_V8A);
        binding.chipAbiArmeabiV7a.setTag(SETTING_CMAKE_ABI_ARMEABI_V7A);
        binding.chipAbiX86.setTag(SETTING_CMAKE_ABI_X86);
        binding.chipAbiX8664.setTag(SETTING_CMAKE_ABI_X86_64);

        binding.rgBuildMode.setTag(SETTING_BUILD_MODE);
        binding.tilReleaseKeystorePath.getEditText().setTag(SETTING_RELEASE_KEYSTORE_PATH);
        binding.tilReleaseKeystoreAlias.getEditText().setTag(SETTING_RELEASE_KEYSTORE_ALIAS);
        binding.tilReleaseStorePass.getEditText().setTag(SETTING_RELEASE_STORE_PASS);
        binding.tilReleaseKeyPass.getEditText().setTag(SETTING_RELEASE_KEY_PASS);

        views[VIEW_ANDROIR_JAR_PATH] = binding.tilAndroidJar.getEditText();
        views[VIEW_CLASS_PATH] = binding.tilClasspath.getEditText();
        views[VIEW_DEXER] = binding.rgDexer;
        views[VIEW_ENABLE_LOGCAT] = binding.cbEnableLogcat;
        views[VIEW_JAVA_VERSION] = binding.rgJavaVersion;
        views[VIEW_NO_HTTP_LEGACY] = binding.cbNoHttpLegacy;
        views[VIEW_NO_WARNINGS] = binding.cbNoWarnings;
        views[VIEW_ABI_ARM64_V8A] = binding.chipAbiArm64V8a;
        views[VIEW_ABI_ARMEABI_V7A] = binding.chipAbiArmeabiV7a;
        views[VIEW_ABI_X86] = binding.chipAbiX86;
        views[VIEW_ABI_X86_64] = binding.chipAbiX8664;
        
        views[VIEW_BUILD_MODE] = binding.rgBuildMode;
        views[VIEW_RELEASE_KEYSTORE_PATH] = binding.tilReleaseKeystorePath.getEditText();
        views[VIEW_RELEASE_KEYSTORE_ALIAS] = binding.tilReleaseKeystoreAlias.getEditText();
        views[VIEW_RELEASE_STORE_PASS] = binding.tilReleaseStorePass.getEditText();
        views[VIEW_RELEASE_KEY_PASS] = binding.tilReleaseKeyPass.getEditText();

        if (DRFeatureManager.getForMinSDK() < 33)
            binding.lnNote.setVisibility(View.GONE);
    }

    private void setRadioGroupOptions(RadioGroup radioGroup, String[] options, String key, String defaultValue) {
        radioGroup.removeAllViews();
        String value = projectSettings.getValue(key, defaultValue);
        for (String option : options) {
            RadioButton radioButton = new RadioButton(radioGroup.getContext());
            radioButton.setText(option);
            radioButton.setId(View.generateViewId());
            radioButton.setLayoutParams(new RadioGroup.LayoutParams(0, -2, 1f));
            if (value.equals(option)) {
                radioButton.setChecked(true);
            }
            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) return;
                if (key.equals(SETTING_JAVA_VERSION)) {
                    handleJavaVersionChange(option);
                }
                if (key.equals(SETTING_BUILD_MODE)) {
                    updateKeystoreVisibility(option);
                }
            });
            radioGroup.addView(radioButton);
        }
    }

    private void setCheckboxValue(CompoundButton checkBox, String key, boolean defaultValue) {
        String value = projectSettings.getValue(key, defaultValue ? "true" : "false");
        checkBox.setChecked(value.equals("true"));

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (key.equals(SETTING_NO_HTTP_LEGACY)) {
                    SketchwareUtil.toast("Note that this option may cause issues if RequestNetwork component is used");
                }
            }
        });
    }
}
