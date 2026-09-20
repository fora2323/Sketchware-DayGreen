package com.besome.sketch.editor.manage.library.daydream;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.io.File;

import extensions.anbui.daydream.activity.project.settings.DayDreamUniversalSettingsActivity;
import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import extensions.anbui.daydream.tools.NativeToolchainManager;
import mod.hey.studios.activity.managers.native_code.ManageNativeActivity;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageLibraryDaydreamBinding;
import pro.sketchware.utility.FilePathUtil;

public class DayDreamLibraryActivity extends BaseAppCompatActivity {

    private ManageLibraryDaydreamBinding binding;
    private String sc_id;
    private final FilePathUtil fpu = new FilePathUtil();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageLibraryDaydreamBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        sc_id = getIntent().getStringExtra("sc_id");
        if (sc_id == null || sc_id.isEmpty()) {
            finish();
            return;
        }

        DRProjectTracker.startNow(sc_id);
        initialize();
    }

    private void initialize() {
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        boolean isMasterEnabled = DayDreamProjectSettings.isEnableDayDream(sc_id);
        binding.switchMaster.setChecked(isMasterEnabled);
        updateOptionsContainerState(isMasterEnabled);

        binding.switchMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DayDreamProjectSettings.setEnableDayDream(sc_id, isChecked);
            if (isChecked) {
                ensureProjectNativeDirExists();
            }
            updateOptionsContainerState(isChecked);
            refreshProjectNativeStatus();
        });
        binding.layoutSwitchMaster.setOnClickListener(v -> binding.switchMaster.toggle());

        // Manage C/C++ Files Action
        binding.btnManageNativeFiles.setOnClickListener(v -> {
            ensureProjectNativeDirExists();
            Intent intent = new Intent(this, ManageNativeActivity.class);
            intent.putExtra("sc_id", sc_id);
            startActivity(intent);
        });

        // Open Universal Settings (NDK & CMake Toolchains)
        binding.btnOpenUniversalSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, DayDreamUniversalSettingsActivity.class);
            startActivity(intent);
        });

        refreshUI();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void ensureProjectNativeDirExists() {
        File nativeDir = new File(fpu.getPathNative(sc_id));
        if (!nativeDir.exists()) {
            nativeDir.mkdirs();
        }
    }

    private void refreshUI() {
        refreshProjectNativeStatus();
        refreshToolchainStatus();
    }

    private void refreshProjectNativeStatus() {
        String nativePath = fpu.getPathNative(sc_id);
        binding.tvProjectNativePath.setText(nativePath);

        File nativeDir = new File(nativePath);
        if (nativeDir.exists() && nativeDir.isDirectory()) {
            File[] files = nativeDir.listFiles();
            int count = (files != null) ? files.length : 0;
            binding.tvProjectNativeStatus.setText("Directory ready • " + count + " file" + (count == 1 ? "" : "s") + " present in native folder");
        } else {
            binding.tvProjectNativeStatus.setText("Directory ready • files will be stored in " + nativePath);
        }
    }

    private void refreshToolchainStatus() {
        // NDK status
        boolean ndkInstalled = NativeToolchainManager.INSTANCE.isNdkInstalled(this);
        if (ndkInstalled) {
            binding.tvNdkBadge.setText("Installed");
            binding.tvNdkBadge.setBackgroundResource(R.drawable.bg_round_green);
            binding.tvNdkBadge.setTextColor(getColor(android.R.color.white));
            binding.tvNdkInfo.setText("Installed • Space used: " + NativeToolchainManager.INSTANCE.getNdkSizeFormatted(this));
        } else {
            binding.tvNdkBadge.setText("Not Installed");
            binding.tvNdkBadge.setBackgroundResource(R.drawable.bg_round_gray);
            binding.tvNdkBadge.setTextColor(getColor(R.color.onSurfaceVariant));
            binding.tvNdkInfo.setText("Location: files/bin/android-ndk/ (~360 MB)");
        }

        // CMake status
        boolean cmakeInstalled = NativeToolchainManager.INSTANCE.isCmakeInstalled(this);
        if (cmakeInstalled) {
            binding.tvCmakeBadge.setText("Installed");
            binding.tvCmakeBadge.setBackgroundResource(R.drawable.bg_round_green);
            binding.tvCmakeBadge.setTextColor(getColor(android.R.color.white));
            binding.tvCmakeInfo.setText("Installed • Space used: " + NativeToolchainManager.INSTANCE.getCmakeSizeFormatted(this));
        } else {
            binding.tvCmakeBadge.setText("Not Installed");
            binding.tvCmakeBadge.setBackgroundResource(R.drawable.bg_round_gray);
            binding.tvCmakeBadge.setTextColor(getColor(R.color.onSurfaceVariant));
            binding.tvCmakeInfo.setText("Location: files/bin/cmake/ (~48 MB)");
        }
    }

    private void updateOptionsContainerState(boolean isEnabled) {
        binding.layoutOptionsContainer.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }
}
