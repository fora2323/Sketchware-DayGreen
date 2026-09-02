package extensions.anbui.daydream.activity.project.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import extensions.anbui.daydream.activity.project.settings.library.OneSignalSettingsActivity;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.library.LibraryUtils;
import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.project.ProjectBuildConfigs;
import extensions.anbui.daydream.project.ProjectConfigs;
import extensions.anbui.daydream.project.ProjectLibrary;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;

import pro.sketchware.databinding.ActivityDaydreamLibrarySettingsBinding;

public class LibrarySettings extends AppCompatActivity {

    private String projectID;
    private ActivityDaydreamLibrarySettingsBinding binding;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().hasExtra("sc_id")) {
            projectID = getIntent().getStringExtra("sc_id");
            DRProjectTracker.startNow(projectID);
        } else {
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityDaydreamLibrarySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swForceaddworkmanager.setChecked(DayDreamProjectSettings.isForceAddWorkManager(projectID));
        binding.swForceaddworkmanager.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setForceAddWorkManager(projectID, isChecked));
        binding.swUseandroixbrowser.setChecked(DayDreamProjectSettings.isUniversalUseAndroidXBrowser(projectID));
        binding.swUseandroixcredentialmanager.setChecked(DayDreamProjectSettings.isUniversalUseAndroidXCredentialManager(projectID));
        binding.swUseshizuku.setChecked(DayDreamProjectSettings.isUseShizuku(projectID));
        binding.swGlidetransformations.setChecked(DayDreamProjectSettings.isGlideTransformations(projectID));
        binding.swUseandroidbilling.setChecked(DayDreamProjectSettings.isUseAndroidBilling(projectID));
        binding.swUseretrofit2.setChecked(DayDreamProjectSettings.isUseRetrofit2(projectID));

        binding.swUsemedia3.setChecked((DayDreamProjectSettings.isUniversalUseMedia3(projectID)));
        binding.swUsemedia3.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUniversalUseMedia3(projectID, isChecked));
        binding.swUseandroixbrowser.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUniversalUseAndroidXBrowser(projectID, isChecked));
        binding.swUseandroixcredentialmanager.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUniversalUseAndroidXCredentialManager(projectID, isChecked));
        binding.swUseshizuku.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUseShizuku(projectID, isChecked));
        binding.swGlidetransformations.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setGlideTransformations(projectID, isChecked));
        binding.swUseandroidbilling.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUseAndroidBilling(projectID, isChecked));
        binding.swUseretrofit2.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUseRetrofit2(projectID, isChecked));

        binding.lnForceaddworkmanager.setOnClickListener(v -> binding.swForceaddworkmanager.toggle());
        binding.lnUsemedia3.setOnClickListener(v -> binding.swUsemedia3.toggle());
        binding.lnUseandroixbrowser.setOnClickListener(v -> binding.swUseandroixbrowser.toggle());
        binding.lnUseandroixcredentialmanager.setOnClickListener(v -> binding.swUseandroixcredentialmanager.toggle());
        binding.lnUseshizuku.setOnClickListener(v -> binding.swUseshizuku.toggle());
        binding.lnGlidetransformations.setOnClickListener(v -> binding.swGlidetransformations.toggle());
        binding.lnUseandroidbilling.setOnClickListener(v -> binding.swUseandroidbilling.toggle());
        binding.lnUseretrofit2.setOnClickListener(v -> binding.swUseretrofit2.toggle());
        binding.lnUseonesignal.setOnClickListener(v -> goToSettings(OneSignalSettingsActivity.class));


        initializeForceAddWorkmanager();
        initializeUseMedia3();
        initializeUseAndroidXBrowser();
        initializeUseAndroidXCredentialManager();
        initializeUseShizuku();
        initializeUseAndroidBilling();
        initializeUseOneSignal();
    }

    private void initializeForceAddWorkmanager() {
        if (!LibraryUtils.isAllowUseAndroidXWorkManager(Configs.currentProjectID)) {
            binding.lnForceaddworkmanager.setEnabled(false);
            binding.lnForceaddworkmanager.setAlpha(0.5f);
            binding.tvForceaddworkmanagernote.setText("To use, enable AppCompat. " + binding.tvForceaddworkmanagernote.getText().toString());
        }
    }

    private void initializeUseMedia3() {
        boolean finalstatus = true;
        if (!ProjectConfigs.isMinSDKNewerThan23(projectID)) {
            finalstatus = false;
            binding.tvUsemedia3note.setText("To use, min SDK required is 24 or newer (Android 7+). " + binding.tvUsemedia3note.getText().toString());
        } else if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            finalstatus = false;
            binding.tvUsemedia3note.setText("To use, enable AppCompat. " + binding.tvUsemedia3note.getText().toString());
        } else if (ProjectBuildConfigs.isUseJava7(projectID)) {
            finalstatus = false;
            binding.tvUsemedia3note.setText("To use, use a newer version of Java. " + binding.tvUsemedia3note.getText().toString());
        }

        binding.lnUsemedia3.setEnabled(finalstatus);
        binding.lnUsemedia3.setAlpha(finalstatus ? 1 : 0.5f);
    }

    private void initializeUseAndroidXBrowser() {
        if (!LibraryUtils.isAllowUseAndroidXWorkManager(Configs.currentProjectID)) {
            binding.lnUseandroixbrowser.setEnabled(false);
            binding.lnUseandroixbrowser.setAlpha(0.5f);
            binding.tvUseandroixbrowsernote.setText("To use, enable AppCompat. " + binding.tvForceaddworkmanagernote.getText().toString());
        }
    }

    private void initializeUseAndroidXCredentialManager() {
        boolean finalstatus = true;
        if (!ProjectConfigs.isMinSDKNewerThan23(projectID)) {
            finalstatus = false;
            binding.tvUseandroixcredentialmanagernote.setText("To use, min SDK required is 24 or newer (Android 7+). " + binding.tvUseandroixcredentialmanagernote.getText().toString());
        } else if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            finalstatus = false;
            binding.tvUseandroixcredentialmanagernote.setText("To use, enable AppCompat. " + binding.tvUseandroixcredentialmanagernote.getText().toString());
        } else if (ProjectBuildConfigs.isUseJava7(projectID)) {
            finalstatus = false;
            binding.tvUseandroixcredentialmanagernote.setText("To use, use a newer version of Java. " + binding.tvUseandroixcredentialmanagernote.getText().toString());
        }

        binding.lnUseandroixcredentialmanager.setEnabled(finalstatus);
        binding.lnUseandroixcredentialmanager.setAlpha(finalstatus ? 1 : 0.5f);
    }

    private void initializeUseShizuku() {
        boolean finalstatus = true;
        if (!ProjectConfigs.isMinSDKNewerThan23(projectID)) {
            finalstatus = false;
            binding.tvUseshizukunote.setText("To use, min SDK required is 24 or newer (Android 7+). " + binding.tvUseshizukunote.getText().toString());
        } else if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            finalstatus = false;
            binding.tvUseshizukunote.setText("To use, enable AppCompat. " + binding.tvUseshizukunote.getText().toString());
        }

        if (!finalstatus) {
            binding.lnUseshizuku.setEnabled(false);
            binding.lnUseshizuku.setAlpha(0.5f);
        }
    }

    private void initializeUseAndroidBilling() {
        boolean finalstatus = true;
        if (!ProjectLibrary.isEnabledFirebase(projectID)) {
            finalstatus = false;
            binding.tvUseandroidbillingnote.setText("To use, enable Firebase. " + binding.tvUseandroidbillingnote.getText().toString());
        } else if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            finalstatus = false;
            binding.tvUseandroidbillingnote.setText("To use, enable AppCompat. " + binding.tvUseandroidbillingnote.getText().toString());
        }

        if (!finalstatus) {
            binding.lnUseandroidbilling.setEnabled(false);
            binding.lnUseandroidbilling.setAlpha(0.5f);
        }
    }

    private void initializeUseOneSignal() {
        boolean finalstatus = true;
        if (!ProjectLibrary.isEnabledFirebase(projectID)) {
            finalstatus = false;
            binding.tvUseonesignalnote.setText("To use, enable Firebase. " + binding.tvUseonesignalnote.getText().toString());
        } else if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            finalstatus = false;
            binding.tvUseonesignalnote.setText("To use, enable AppCompat. " + binding.tvUseonesignalnote.getText().toString());
        }

        if (!finalstatus) {
            binding.lnUseonesignal.setEnabled(false);
            binding.lnUseonesignal.setAlpha(0.5f);
        }
    }

    private void goToSettings(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("sc_id", projectID);
        startActivity(intent);
    }
}