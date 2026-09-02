package extensions.anbui.daydream.activity.project.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import pro.sketchware.databinding.ActivityDaydreamPermissionSettingsBinding;

public class PermissionSettings extends AppCompatActivity {

    private String projectID;
    private ActivityDaydreamPermissionSettingsBinding binding;

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
        binding = ActivityDaydreamPermissionSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swDisableautomaticpermissionrequests.setChecked(DayDreamProjectSettings.isUniversalDisableAutomaticPermissionRequests(projectID));
        binding.swDisableautomaticpermissionrequests.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUniversalDisableAutomaticPermissionRequests(projectID, isChecked));

        binding.lnDisableautomaticpermissionrequests.setOnClickListener(v -> binding.swDisableautomaticpermissionrequests.toggle());
    }
}