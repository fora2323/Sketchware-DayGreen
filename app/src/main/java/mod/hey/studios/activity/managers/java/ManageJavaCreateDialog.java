package mod.hey.studios.activity.managers.java;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.HashMap;

import a.a.a.lC;
import pro.sketchware.R;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ManageJavaCreateDialog {

    private static final String ACTIVITY_TEMPLATE =
            "package %s;\n\n" +
            "import android.app.Activity;\n" +
            "import android.os.Bundle;\n\n" +
            "public class %s extends Activity {\n\n" +
            "    @Override\n" +
            "    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "    }\n" +
            "}\n";

    private static final String FRAGMENT_TEMPLATE =
            "package %s;\n\n" +
            "import android.os.Bundle;\n" +
            "import android.view.LayoutInflater;\n" +
            "import android.view.View;\n" +
            "import android.view.ViewGroup;\n\n" +
            "import androidx.annotation.NonNull;\n" +
            "import androidx.annotation.Nullable;\n" +
            "import androidx.fragment.app.Fragment;\n\n" +
            "public class %s extends Fragment {\n\n" +
            "    @Nullable\n" +
            "    @Override\n" +
            "    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {\n" +
            "        return null;\n" +
            "    }\n" +
            "}\n";

    private static final String DIALOG_FRAGMENT_TEMPLATE =
            "package %s;\n\n" +
            "import androidx.fragment.app.DialogFragment;\n\n" +
            "public class %s extends DialogFragment {\n\n" +
            "}\n";

    private static final String BOTTOM_DIALOG_FRAGMENT_TEMPLATE =
            "package %s;\n\n" +
            "import com.google.android.material.bottomsheet.BottomSheetDialogFragment;\n\n" +
            "public class %s extends BottomSheetDialogFragment {\n\n" +
            "}\n";

    public static void show(Context context, String scId, String pkgName, Runnable onCreated) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_java_create_file, null);

        TextInputEditText inputText = view.findViewById(R.id.inputText);
        TextInputEditText inputPackageText = view.findViewById(R.id.inputPackageText);
        TextInputLayout packageLayout = view.findViewById(R.id.packageLayout);
        MaterialSwitch switchSubPackage = view.findViewById(R.id.switchSubPackage);
        ChipGroup chipGroupTypes = view.findViewById(R.id.chipGroupTypes);

        // Load metadata
        HashMap<String, Object> projectMetadata = lC.b(scId);
        String lastPackage = pkgName;
        if (projectMetadata != null && projectMetadata.containsKey("java_last_package")) {
            Object savedPkg = projectMetadata.get("java_last_package");
            if (savedPkg instanceof String && !((String) savedPkg).isEmpty()) {
                lastPackage = (String) savedPkg;
            }
        }
        inputPackageText.setText(lastPackage);

        Runnable updateSwitchVisibility = () -> {
            String p = inputPackageText.getText() != null ? inputPackageText.getText().toString().trim() : "";
            if (!p.equals(pkgName) && !p.startsWith(pkgName + ".")) {
                switchSubPackage.setVisibility(View.VISIBLE);
            } else {
                switchSubPackage.setVisibility(View.GONE);
            }
        };

        inputPackageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chipGroupTypes.getCheckedChipId() != R.id.chip_folder) {
                    updateSwitchVisibility.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroupTypes.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isFolder = (checkedId == R.id.chip_folder);
            packageLayout.setVisibility(isFolder ? View.GONE : View.VISIBLE);
            if (isFolder) {
                switchSubPackage.setVisibility(View.GONE);
            } else {
                updateSwitchVisibility.run();
            }
        });

        // Initialize view states
        updateSwitchVisibility.run();
        switchSubPackage.setChecked(true);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Create new")
                .setView(view)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputText.requestFocus();

            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String name = inputText.getText() != null ? inputText.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    SketchwareUtil.toast("Invalid file name");
                    return;
                }

                int checkedId = chipGroupTypes.getCheckedChipId();
                boolean isFolder = (checkedId == R.id.chip_folder);
                String p = inputPackageText.getText() != null ? inputPackageText.getText().toString().trim() : "";

                if (!isFolder) {
                    String regex = "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$";
                    if (!p.matches(regex)) {
                        SketchwareUtil.toast("Invalid package name");
                        return; 
                    }
                }

                String root = new FilePathUtil().getPathJava(scId);
                String dirPath = root;

                if (!isFolder) {
                    if (p.equals(pkgName)) {
                        dirPath = root;
                    } else if (p.startsWith(pkgName + ".")) {
                        String sisa = p.substring(pkgName.length() + 1);
                        dirPath = root + "/" + sisa.replace('.', '/');
                    } else {
                        if (switchSubPackage.getVisibility() == View.VISIBLE && switchSubPackage.isChecked()) {
                            dirPath = root + "/" + p.replace('.', '/');
                        } else {
                            dirPath = root;
                        }
                    }
                }

                if (isFolder) {
                    File dir = new File(root, name);
                    if (dir.exists()) {
                        SketchwareUtil.toast("File already exists");
                        return;
                    }
                    dir.mkdirs();
                    SketchwareUtil.toast("File was created successfully");
                } else {
                    File dir = new File(dirPath);
                    dir.mkdirs();
                    File file = new File(dir, name + ".java");
                    if (file.exists()) {
                        SketchwareUtil.toast("File already exists");
                        return;
                    }

                    String content = "";
                    if (checkedId == R.id.chip_activity) {
                        content = String.format(ACTIVITY_TEMPLATE, p, name);
                    } else if (checkedId == R.id.chip_fragment) {
                        content = String.format(FRAGMENT_TEMPLATE, p, name);
                    } else if (checkedId == R.id.chip_dialog_fragment) {
                        content = String.format(DIALOG_FRAGMENT_TEMPLATE, p, name);
                    } else if (checkedId == R.id.chip_bottom_dialog_fragment) {
                        content = String.format(BOTTOM_DIALOG_FRAGMENT_TEMPLATE, p, name);
                    }

                    FileUtil.writeFile(file.getAbsolutePath(), content);
                    SketchwareUtil.toast("File was created successfully");

                    HashMap<String, Object> map = lC.b(scId);
                    if (map == null) map = new HashMap<>();
                    map.put("java_last_package", p);
                    lC.b(scId, map);
                }

                dialog.dismiss();
                if (onCreated != null) {
                    onCreated.run();
                }
            });
        });

        dialog.show();
    }
}