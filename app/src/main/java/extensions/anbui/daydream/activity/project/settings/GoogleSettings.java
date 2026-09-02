package extensions.anbui.daydream.activity.project.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import java.util.Objects;

import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.project.ProjectBuildConfigs;
import extensions.anbui.daydream.project.ProjectConfigs;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import extensions.anbui.daydream.project.ProjectLibrary;
import pro.sketchware.databinding.ActivityDaydreamGoogleSettingsBinding;

public class GoogleSettings extends AppCompatActivity {

    private String projectID;
    private ActivityDaydreamGoogleSettingsBinding binding;

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
        binding = ActivityDaydreamGoogleSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swAnalytics.setChecked(DayDreamProjectSettings.isUseGoogleAnalytics(projectID));
        binding.swAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUseGoogleAnalytics(projectID, isChecked));

        binding.lnAnalytics.setOnClickListener(v -> binding.swAnalytics.toggle());

        initializeAnalytics();
    }

    public void initializeAnalytics() {
        boolean finalstatus = true;
        if (!ProjectConfigs.isMinSDKNewerThan23(projectID)) {
            finalstatus = false;
            binding.tvAnalyticsnote.setText("To use, min SDK required is 24 or newer (Android 7+). " + binding.tvAnalyticsnote.getText().toString());
        }  else if (ProjectBuildConfigs.isUseJava7(projectID)) {
            finalstatus = false;
            binding.tvAnalyticsnote.setText("To use, use a newer version of Java. " + binding.tvAnalyticsnote.getText().toString());
        } else if (!ProjectLibrary.isEnabledFirebase(projectID)) {
            finalstatus = false;
            binding.tvAnalyticsnote.setText("To use, turn on Firebase. " + binding.tvAnalyticsnote.getText().toString());
        }

        if (!finalstatus) {
            binding.lnAnalytics.setEnabled(false);
            binding.lnAnalytics.setAlpha(0.5f);
        }
    }
}
