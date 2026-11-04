package mod.hey.studios.activity.managers.java;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.utility.SketchwareUtil;

public class ManageJavaCreateDialog {

    public static void show(Context context, String defaultPkg, Consumer<String> onConfirm) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_java_create_file, null);

        TextInputEditText inputPackageText = view.findViewById(R.id.inputPackageText);
        inputPackageText.setText(defaultPkg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Direct Java editor")
                .setView(view)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputPackageText.requestFocus();
            inputPackageText.setSelection(inputPackageText.getText() != null ? inputPackageText.getText().length() : 0);

            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String p = inputPackageText.getText() != null ? inputPackageText.getText().toString().trim() : "";

                String regex = "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$";
                if (!p.matches(regex)) {
                    SketchwareUtil.toast("Invalid package name");
                    return;
                }

                dialog.dismiss();
                if (onConfirm != null) {
                    onConfirm.accept(p);
                }
            });
        });

        dialog.show();
    }
}