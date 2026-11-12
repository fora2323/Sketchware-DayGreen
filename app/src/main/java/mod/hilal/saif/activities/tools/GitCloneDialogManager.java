package mod.hilal.saif.activities.tools;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.File;
import java.util.Objects;

import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.file.FilesTools;
import mod.hey.studios.project.backup.BackupFactory;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogGitCloneBinding;
import pro.sketchware.databinding.DialogGitProgressBinding;
import pro.sketchware.utility.SketchwareUtil;

public class GitCloneDialogManager {

    private static final String TAG = "GitCloneDialogManager";

    public static void showCloneDialog(Activity activity, Runnable onCompleteRunnable) {
        DialogGitCloneBinding binding = DialogGitCloneBinding.inflate(activity.getLayoutInflater());

        AlertDialog inputDialog = new MaterialAlertDialogBuilder(activity)
                .setView(binding.getRoot())
                .setCancelable(true)
                .create();

        if (inputDialog.getWindow() != null) {
            inputDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        binding.btnCancel.setOnClickListener(v -> inputDialog.dismiss());

        binding.btnOk.setOnClickListener(v -> {
            String repoUrl = Objects.requireNonNull(binding.etRepoUrl.getText()).toString().trim();
            String branch = Objects.requireNonNull(binding.etBranch.getText()).toString().trim();

            if (repoUrl.isEmpty()) {
                SketchwareUtil.toast("Please enter a repository URL");
                return;
            }

            if (branch.isEmpty()) {
                branch = "main";
            }

            inputDialog.dismiss();
            startCloning(activity, repoUrl, branch, onCompleteRunnable);
        });

        inputDialog.show();
        setupDialogWindow(activity, inputDialog);
    }

    private static void startCloning(Activity activity, String repoUrl, String branch, Runnable onCompleteRunnable) {
        DialogGitProgressBinding progressBinding = DialogGitProgressBinding.inflate(activity.getLayoutInflater());

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity)
                .setView(progressBinding.getRoot())
                .setCancelable(false)
                .create();

        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final boolean[] isCancelled = {false};

        progressBinding.btnCancelProgress.setOnClickListener(v -> {
            isCancelled[0] = true;
            progressBinding.tvProgressStatus.setText("Cancelling clone...");
        });

        progressDialog.show();
        setupDialogWindow(activity, progressDialog);

        new Thread(() -> {
            String sc_id = BackupFactory.getNewScId();
            File targetGitDir = GitManager.getGitDir(sc_id);

            ProgressMonitor monitor = new ProgressMonitor() {
                private String currentTask = "Cloning repository...";
                private int totalWork = 0;

                @Override
                public void start(int totalTasks) {}

                @Override
                public void beginTask(String title, int totalWork) {
                    this.currentTask = title;
                    this.totalWork = totalWork;
                    updateUI(title, 0, totalWork);
                }

                @Override
                public void update(int completed) {
                    if (totalWork > 0) {
                        updateUI(currentTask, completed, totalWork);
                    } else {
                        updateUI(currentTask, completed, -1);
                    }
                }

                @Override
                public void endTask() {}

                @Override
                public boolean isCancelled() {
                    return isCancelled[0];
                }

                private void updateUI(String title, int completed, int total) {
                    activity.runOnUiThread(() -> {
                        if (total > 0) {
                            progressBinding.tvProgressStatus.setText(title + " (" + completed + "/" + total + ")");
                            progressBinding.progressIndicator.setIndeterminate(false);
                            progressBinding.progressIndicator.setMax(total);
                            progressBinding.progressIndicator.setProgress(completed);
                        } else {
                            progressBinding.tvProgressStatus.setText(title);
                            progressBinding.progressIndicator.setIndeterminate(true);
                        }
                    });
                }
            };

            try {
                GitManager.cloneRepo(activity, repoUrl, branch, targetGitDir, monitor);

                if (isCancelled[0]) {
                    try {
                        FilesTools.deleteFileOrDirectory(targetGitDir.toPath());
                    } catch (Exception ignored) {}
                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        SketchwareUtil.toast("Clone cancelled");
                    });
                    return;
                }

                boolean restored = GitManager.restoreProjectFromGitDir(sc_id);

                activity.runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (restored) {
                        SketchwareUtil.toast("Project cloned successfully! ID: " + sc_id);
                        if (onCompleteRunnable != null) {
                            onCompleteRunnable.run();
                        }
                    } else {
                        SketchwareUtil.toastError("Failed to import cloned project files");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Cloning failed: ", e);
                try {
                    FilesTools.deleteFileOrDirectory(targetGitDir.toPath());
                } catch (Exception ignored) {}
                activity.runOnUiThread(() -> {
                    progressDialog.dismiss();
                    SketchwareUtil.toastError("Clone failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private static void setupDialogWindow(Activity activity, AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int dialogWidth = (int) (screenWidth * 0.85f);
            int maxDialogWidth = (int) (420 * activity.getResources().getDisplayMetrics().density);
            if (dialogWidth > maxDialogWidth) {
                dialogWidth = maxDialogWidth;
            }
            dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
