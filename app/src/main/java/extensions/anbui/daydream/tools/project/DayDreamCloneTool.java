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
import pro.sketchware.activities.main.activities.MainActivity;

public class DayDreamCloneTool {

    public static void showDialogNow(Activity activity, String projectID) {
        DialogUtils.twoDialog(activity,
                "Clone",
                "Clone your project.",
                "Clone",
                "Cancel",
                true,
                R.drawable.content_copy_24px,
                true,
                () -> startNow(activity, projectID), null, null);
    }

    private static void startNow(Activity activity, String projectID) {
        View progressView = LayoutInflater.from(activity).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Cloning...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            boolean result = CloneCore.startNow(projectID, progress_text);
            activity.runOnUiThread(() -> {
                progressDialog.dismiss();
                if (result) {
                    DialogUtils.oneDialog(activity,
                            "Done",
                            "Cloned your project.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_check,
                            true, null, null);

                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).n();
                    }
                } else {
                    DialogUtils.oneDialog(activity,
                            "Error",
                            "Unable to clone your project.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_warning,
                            true, null, null);
                }

            });
        }).start();
    }
}
