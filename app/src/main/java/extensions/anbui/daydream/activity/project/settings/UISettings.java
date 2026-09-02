package extensions.anbui.daydream.activity.project.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.project.ProjectBuildConfigs;
import extensions.anbui.daydream.project.ProjectLibrary;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import pro.sketchware.databinding.ActivityDaydreamUiSettingsBinding;

public class UISettings extends AppCompatActivity {

    private String projectID;
    private ActivityDaydreamUiSettingsBinding binding;

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
        binding = ActivityDaydreamUiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swEdgetoedge.setChecked(DayDreamProjectSettings.isUniversalEdgeToEdge(projectID));
        binding.swEdgetoedge.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUniversalEdgeToEdge(projectID, isChecked));

        binding.swWindowinsetshandling.setChecked(DayDreamProjectSettings.isUniversalWindowInsetsHandling(projectID));
        binding.swWindowinsetshandling.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUniversalWindowInsetsHandling(projectID, isChecked));

        binding.swEnableandroidtextcolorremoval.setChecked(DayDreamProjectSettings.isEnableAndroidTextColorRemoval(projectID));
        binding.swEnableandroidtextcolorremoval.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setEnableAndroidTextColorRemoval(projectID, isChecked));

        binding.swEnableOnBackInvokedCallback.setChecked(DayDreamProjectSettings.isUninversalEnableOnBackInvokedCallback(projectID));
        binding.swEnableOnBackInvokedCallback.setOnCheckedChangeListener((buttonView, isChecked) -> DayDreamProjectSettings.setUninversalEnableOnBackInvokedCallback(projectID, isChecked));

        binding.lnEdgetoedge.setOnClickListener(v -> binding.swEdgetoedge.toggle());
        binding.lnWindowinsetshandling.setOnClickListener(v -> binding.swWindowinsetshandling.toggle());
        binding.lnEnableandroidtextcolorremoval.setOnClickListener(v -> binding.swEnableandroidtextcolorremoval.toggle());
        binding.lnEnableOnBackInvokedCallback.setOnClickListener(v -> binding.swEnableOnBackInvokedCallback.toggle());

        initializeEdgeToEdge();
        initializeWindowinsetshandling();
    }

    private void initializeEdgeToEdge() {
        if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            binding.lnEdgetoedge.setEnabled(false);
            binding.lnEdgetoedge.setAlpha(0.5f);
            binding.tvEdgetoedgenote.setText("To use, enable AppCompat. " + binding.tvEdgetoedgenote.getText().toString());
        }
    }

    private void initializeWindowinsetshandling() {
        boolean finalstatus = true;
        if (!ProjectLibrary.isEnabledAppCompat(projectID)) {
            finalstatus = false;
            binding.tvWindowinsetshandlingnote.setText("To use, enable AppCompat. " + binding.tvWindowinsetshandlingnote.getText().toString());
        } else if (ProjectBuildConfigs.isUseJava7(projectID)) {
            finalstatus = false;
            binding.tvWindowinsetshandlingnote.setText("To use, use a newer version of Java. " + binding.tvWindowinsetshandlingnote.getText().toString());
        }

        binding.lnWindowinsetshandling.setEnabled(finalstatus);
        binding.lnWindowinsetshandling.setAlpha(finalstatus ? 1 : 0.5f);
    }
}