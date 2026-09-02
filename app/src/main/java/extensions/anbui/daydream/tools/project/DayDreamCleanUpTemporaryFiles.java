package extensions.anbui.daydream.tools.project;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import extensions.anbui.daydream.ui.DialogUtils;
import pro.sketchware.R;

public class DayDreamCleanUpTemporaryFiles {

    public static void showDialogNow(Activity activity, String projectID) {
        DialogUtils.twoDialog(activity,
                "Clean up temporary files",
                "Clean up temporary files generated during build to free up storage space.",
                "Clean up",
                "Cancel",
                true,
                R.drawable.ic_mtrl_android,
                true,
                () -> startNow(activity, projectID), null, null);
    }

    public static void startNow(Activity activity, String projectID) {
        View progressView = LayoutInflater.from(activity).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Cleaning up...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            boolean result = CleanUpCore.removeTemporaryFiles(projectID);
            activity.runOnUiThread(() -> {
                progressDialog.dismiss();
                if (result) {
                    DialogUtils.oneDialog(activity,
                            "Done",
                            "Cleaned up temporary files.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_check,
                            true, null, null);
                } else {
                    DialogUtils.oneDialog(activity,
                            "Error",
                            "Unable to clean up temporary files.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_warning,
                            true, null, null);
                }

            });
        }).start();
    }
}
