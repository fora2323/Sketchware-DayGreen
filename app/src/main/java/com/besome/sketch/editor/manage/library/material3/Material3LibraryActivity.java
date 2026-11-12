package com.besome.sketch.editor.manage.library.material3;

import android.content.Intent;
import android.os.Bundle;


import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageLibraryMaterial3Binding;

public class Material3LibraryActivity extends BaseAppCompatActivity {

    private ManageLibraryMaterial3Binding binding;
    private Material3LibraryManager material3LibraryManager;
    private String scId;
    private boolean isUpdatingUi = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageLibraryMaterial3Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft() + systemInsets.left, v.getPaddingTop(), v.getPaddingRight() + systemInsets.right, v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScroll, (v, insets) -> {
            Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom() + navBarInsets.bottom);
            return insets;
        });

        binding.collapsingToolbar.setTitle("Material3 Manager");
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        if (getIntent().hasExtra("sc_id")) {
            scId = getIntent().getStringExtra("sc_id");
        }

        initialize();
    }

    private void initialize() {
        material3LibraryManager = new Material3LibraryManager((ProjectLibraryBean) Objects.requireNonNull(getIntent().getParcelableExtra("compat")));
        if (!material3LibraryManager.isAppCompatEnabled()) {
            new MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_mtrl_warning)
                    .setTitle("AppCompat is disabled!")
                    .setMessage("Please enable AppCompat first to use this feature")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }

        isUpdatingUi = true;

        boolean isM3Enabled = material3LibraryManager.isMaterial3Enabled();
        int currentTheme = Configs.material3Theme;

        Object configType = material3LibraryManager.getAppCombatLibraryBean().configurations.get("material3_type");
        if (configType instanceof Integer) {
            currentTheme = (Integer) configType;
        } else if (configType instanceof Number) {
            currentTheme = ((Number) configType).intValue();
        } else if (scId != null) {
            currentTheme = DayDreamProjectSettings.getTheme(scId);
        }

        if (!isM3Enabled && currentTheme == Configs.material2Theme) {
            currentTheme = Configs.material2Theme;
        }

        switch (currentTheme) {
            case Configs.material2Theme -> {
                binding.selectM2.setChecked(true);
                binding.libSwitch.setChecked(false);
            }
            case Configs.material3ExpressiveTheme -> {
                binding.selectM3e.setChecked(true);
                binding.libSwitch.setChecked(true);
            }
            default -> {
                binding.selectM3.setChecked(true);
                binding.libSwitch.setChecked(true);
            }
        }

        binding.dynamicColorsSwitch.setChecked(material3LibraryManager.isDynamicColorsEnabled());

        boolean enableControls = binding.libSwitch.isChecked();
        binding.toggleGroup.setEnabled(enableControls);
        binding.dynamicColorsSwitch.setEnabled(enableControls);
        binding.themeSelector.setEnabled(enableControls);

        switch (material3LibraryManager.getTheme()) {
            case "DayNight" -> binding.selectDayNight.setChecked(true);
            case "Dark" -> binding.selectDark.setChecked(true);
            case "Light" -> binding.selectLight.setChecked(true);
        }

        isUpdatingUi = false;

        binding.libSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingUi) return;
            isUpdatingUi = true;

            binding.toggleGroup.setEnabled(isChecked);
            binding.dynamicColorsSwitch.setEnabled(isChecked);
            binding.themeSelector.setEnabled(isChecked);

            if (!isChecked) {
                binding.selectM2.setChecked(true);
            } else {
                if (binding.selectM2.isChecked()) {
                    binding.selectM3.setChecked(true);
                }
            }

            isUpdatingUi = false;
        });

        binding.layoutSwitchLib.setOnClickListener(view -> binding.libSwitch.setChecked(!binding.libSwitch.isChecked()));
        binding.layoutSwitchDynamicColors.setOnClickListener(view -> {
            if (binding.libSwitch.isChecked())
                binding.dynamicColorsSwitch.setChecked(!binding.dynamicColorsSwitch.isChecked());
        });

        binding.themeSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isUpdatingUi || !isChecked) return;
            isUpdatingUi = true;

            if (checkedId == binding.selectM2.getId()) {
                binding.libSwitch.setChecked(false);
                binding.toggleGroup.setEnabled(false);
                binding.dynamicColorsSwitch.setEnabled(false);
                binding.themeSelector.setEnabled(false);
            } else {
                binding.libSwitch.setChecked(true);
                binding.toggleGroup.setEnabled(true);
                binding.dynamicColorsSwitch.setEnabled(true);
                binding.themeSelector.setEnabled(true);
            }

            isUpdatingUi = false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int selectedThemeType = Configs.material3Theme;
                if (binding.selectM2.isChecked() || !binding.libSwitch.isChecked()) {
                    selectedThemeType = Configs.material2Theme;
                } else if (binding.selectM3e.isChecked()) {
                    selectedThemeType = Configs.material3ExpressiveTheme;
                }

                boolean enableM3 = selectedThemeType > Configs.material2Theme;
                material3LibraryManager.getAppCombatLibraryBean().configurations.put("material3", enableM3);
                material3LibraryManager.getAppCombatLibraryBean().configurations.put("material3_type", selectedThemeType);
                material3LibraryManager.getAppCombatLibraryBean().configurations.put("dynamic_colors", binding.dynamicColorsSwitch.isChecked());

                if (binding.selectDayNight.isChecked()) {
                    material3LibraryManager.getAppCombatLibraryBean().configurations.put("theme", "DayNight");
                } else if (binding.selectLight.isChecked()) {
                    material3LibraryManager.getAppCombatLibraryBean().configurations.put("theme", "Light");
                } else if (binding.selectDark.isChecked()) {
                    material3LibraryManager.getAppCombatLibraryBean().configurations.put("theme", "Dark");
                }

                if (scId != null) {
                    DayDreamProjectSettings.setEnableDayDream(scId, true);
                    DayDreamProjectSettings.setUseDynamicColor(scId, binding.dynamicColorsSwitch.isChecked());
                    DayDreamProjectSettings.setUseTheme(scId, enableM3);
                    DayDreamProjectSettings.setTheme(scId, selectedThemeType);

                    if (binding.selectDayNight.isChecked()) {
                        DayDreamProjectSettings.setThemeDayNight(scId, Configs.DayNightTheme);
                    } else if (binding.selectLight.isChecked()) {
                        DayDreamProjectSettings.setThemeDayNight(scId, Configs.DayTheme);
                    } else if (binding.selectDark.isChecked()) {
                        DayDreamProjectSettings.setThemeDayNight(scId, Configs.NightTheme);
                    }
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra("compat", material3LibraryManager.getAppCombatLibraryBean());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}