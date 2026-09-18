package extensions.anbui.daydream.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.besome.sketch.editor.manage.library.LibraryCategoryView;
import com.besome.sketch.editor.manage.library.LibraryItemView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.ui.DialogUtils;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.tools.ToolCore;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityDaydreamCleanupBinding;

public class DayDreamCleanUp extends AppCompatActivity {
    private ActivityDaydreamCleanupBinding binding;

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
        EdgeToEdge.enable(this);
        binding = ActivityDaydreamCleanupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        initialize();
    }

    private void initialize() {
        var preferences = new ArrayList<LibraryCategoryView>();

        LibraryCategoryView cleanCategory = new LibraryCategoryView(this);
        cleanCategory.setTitle(null);
        preferences.add(cleanCategory);

        cleanCategory.addLibraryItem(createPreference(R.drawable.cleaning_services_24px, "Temporary files", "Files generated during the build process.", v -> cleanUpTemporaryFiles()), true);
        cleanCategory.addLibraryItem(createPreference(R.drawable.ic_mtrl_box, "Local library", "Scan and move unused local libraries to recycle bin.", v -> cleanUpLocalLib()), true);
        cleanCategory.addLibraryItem(createPreference(R.drawable.ic_mtrl_delete, "Recycle bin", "Delete all in recycle bin.", v -> cleanOutTheRecyclingBin()), false);

        preferences.forEach(binding.lnAllOptions::addView);
    }

    private LibraryItemView createPreference(int icon, String title, String desc, View.OnClickListener listener) {
        LibraryItemView preference = new LibraryItemView(this);
        preference.setHideEnabled();
        preference.icon.setImageResource(icon);
        preference.title.setText(title);
        preference.description.setText(desc);
        preference.setOnClickListener(listener);
        return preference;
    }

    private void cleanUpLocalLib() {
        DialogUtils.twoDialog(DayDreamCleanUp.this,
                "Clean up local library",
                "Find and move unused local libraries to the recycle bin.",
                "Clean up",
                "Cancel",
                true,
                R.drawable.ic_mtrl_box,
                true,
                this::startleanUpLocalLib, null, null);
    }

    private void startleanUpLocalLib() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Cleaning up...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            String message;
            int cleared = ToolCore.cleanupLocalLib();
            if (cleared > 0)
                message = "Cleaned up " + cleared + " local libraries. And those libraries have been moved to"
                        + FileUtils.getInternalStorageDir() + Configs.recycleBinDayDreamFolderDir + "local_libs.";
            else {
                message = "No libraries need to be cleaned.";
            }

            runOnUiThread(() -> {
                progressDialog.dismiss();
                DialogUtils.oneDialog(DayDreamCleanUp.this,
                        "Done",
                        message,
                        "OK",
                        true,
                        R.drawable.ic_mtrl_check,
                        true, null, null);
            });
        }).start();
    }

    private void cleanOutTheRecyclingBin() {
        DialogUtils.twoDialog(DayDreamCleanUp.this,
                "Clean up the recycle bin",
                "All files in the recycle bin will be permanently deleted.",
                "Clean up",
                "Cancel",
                true,
                R.drawable.ic_mtrl_delete,
                true,
                this::startcleanOutTheRecyclingBin, null, null);
    }

    private void startcleanOutTheRecyclingBin() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Cleaning up...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            ToolCore.cleanOutTheRecyclingBin();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                DialogUtils.oneDialog(DayDreamCleanUp.this,
                        "Done",
                        "Cleaned the recycle bin.",
                        "OK",
                        true,
                        R.drawable.ic_mtrl_check,
                        true, null, null);
//                binding.lnCleanouttherecyclingbin.setVisibility(View.GONE);
            });
        }).start();
    }

    private void cleanUpTemporaryFiles() {
        DialogUtils.twoDialog(DayDreamCleanUp.this,
                "Clean up temporary files",
                "Clean up temporary files generated during build in all projects to free up storage space.",
                "Clean up",
                "Cancel",
                true,
                R.drawable.ic_mtrl_android,
                true,
                this::startcleanUpTemporaryFiles, null, null);
    }

    private void startcleanUpTemporaryFiles() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Cleaning up...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            String message;
            int cleared = ToolCore.cleanUpTemporaryFiles();
            if (cleared > 0)
                message = "Cleaned up temporary files in " + cleared + " projects.";
            else {
                message = "There is nothing to clean up.";
            }
            runOnUiThread(() -> {
                progressDialog.dismiss();
                DialogUtils.oneDialog(DayDreamCleanUp.this,
                        "Done",
                        message,
                        "OK",
                        true,
                        R.drawable.ic_mtrl_check,
                        true, null, null);
            });
        }).start();
    }
}