package extensions.anbui.daydream.activity.project.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import java.util.Objects;

import extensions.anbui.daydream.activity.project.DayDreamBackupTool;
import extensions.anbui.daydream.library.LibraryUtils;
import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import pro.sketchware.databinding.ActivityDaydreamGeneralSettingsBinding;

public class DayDreamGeneralSettings extends AppCompatActivity {

    private String projectID;
    private ActivityDaydreamGeneralSettingsBinding binding;

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
        binding = ActivityDaydreamGeneralSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swEnabled.setChecked(DayDreamProjectSettings.isEnableDayDream(projectID));
        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DayDreamProjectSettings.setEnableDayDream(projectID, isChecked);
            universalUIController(isChecked);
        });

        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());

        binding.lnLibrarysettings.setOnClickListener(v -> goToSettings(LibrarySettings.class));
        binding.lnUisettings.setOnClickListener(v -> goToSettings(UISettings.class));
        binding.lnSecuritysettings.setOnClickListener(v -> goToSettings(SecuritySettings.class));
        binding.lnPermissionsettings.setOnClickListener(v -> goToSettings(PermissionSettings.class));
        binding.lnGooglesettings.setOnClickListener(v -> goToSettings(GoogleSettings.class));
        binding.lnBackup.setOnClickListener(v -> goToSettings(DayDreamBackupTool.class));
        binding.lnLayoutperformance.setOnClickListener(v -> goToSettings(PerformanceSettingsActivity.class));
        binding.lnLayoutsettings.setOnClickListener(v -> goToSettings(LayoutSettings.class));

        if (LibraryUtils.isAllowUseTheme(projectID)) {
            binding.lnThemesettings.setOnClickListener(v -> goToSettings(ThemeSettings.class));
        } else {
            binding.tvThemenote.setText("To use, enable AppCompat. " + binding.tvThemenote.getText().toString());
            binding.lnThemesettings.setAlpha(0.5f);
            binding.lnThemesettings.setEnabled(false);
        }

        universalUIController(binding.swEnabled.isChecked());
    }

    private void universalUIController(boolean isEnable) {
        binding.lnAllOptions.setAlpha(isEnable ? 1 : 0.5f);

        if (LibraryUtils.isAllowUseTheme(projectID)) {
            binding.lnThemesettings.setEnabled(isEnable);
        }
        binding.lnUisettings.setEnabled(isEnable);
        binding.lnLibrarysettings.setEnabled(isEnable);
        binding.lnGooglesettings.setEnabled(isEnable);
        binding.lnPermissionsettings.setEnabled(isEnable);
        binding.lnSecuritysettings.setEnabled(isEnable);
        binding.lnBackup.setEnabled(isEnable);
        binding.lnLayoutsettings.setEnabled(isEnable);
    }

    private void goToSettings(Class<?> cls) {
        Intent intent = new Intent(DayDreamGeneralSettings.this, cls);
        intent.putExtra("sc_id", projectID);
        startActivity(intent);
    }
}
