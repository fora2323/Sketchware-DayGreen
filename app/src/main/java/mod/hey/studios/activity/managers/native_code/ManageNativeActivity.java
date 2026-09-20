package mod.hey.studios.activity.managers.native_code;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.databinding.DialogInputLayoutBinding;
import pro.sketchware.databinding.ManageFileBinding;
import pro.sketchware.databinding.ManageJavaItemHsBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

/**
 * Manager for a project's native (C/C++) source files.
 */
public class ManageNativeActivity extends BaseAppCompatActivity {

    private static final String CPP_TEMPLATE = "#include <jni.h>\n\nextern \"C\" JNIEXPORT jstring JNICALL\nJava_%s_%s_stringFromJNI(\n        JNIEnv* env,\n        jobject /* this */) {\n    return env->NewStringUTF(\"Hello from C++\");\n}\n";
    private static final String C_TEMPLATE = "#include <jni.h>\n\nJNIEXPORT jstring JNICALL\nJava_%s_%s_stringFromJNI(\n        JNIEnv* env,\n        jobject thiz) {\n    return (*env)->NewStringUTF(env, \"Hello from C\");\n}\n";
    private static final String HEADER_TEMPLATE = "#ifndef %s_H\n#define %s_H\n\n#endif // %s_H\n";

    private final ArrayList<String> currentTree = new ArrayList<>();
    ManageFileBinding binding;
    private String current_path;
    private FilePathUtil fpu;
    private String sc_id;
    private FilesAdapter filesAdapter;
    private MaterialCardView statusBannerCard;

    /**
     * Escapes a name for use in a JNI function name:
     * {@code _} becomes {@code _1} and {@code .} becomes {@code _}.
     */
    private static String jniMangle(String name) {
        return name.replace("_", "_1").replace(".", "_");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sc_id = getIntent().getStringExtra("sc_id");
        if (sc_id == null || sc_id.isEmpty()) {
            sc_id = Configs.currentProjectID;
        }
        Helper.fixFileprovider();
        setupUI();
        fpu = new FilePathUtil();
        current_path = Uri.parse(fpu.getPathNative(sc_id)).getPath();
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatusUI();
    }

    @Override
    public void onBackPressed() {
        if (Objects.equals(Uri.parse(current_path).getPath(), Uri.parse(fpu.getPathNative(sc_id)).getPath())) {
            super.onBackPressed();
        } else {
            current_path = current_path.substring(0, current_path.lastIndexOf("/"));
            refresh();
        }
    }

    private void setupUI() {
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.topAppBar.setTitle("Cpp/C Manager");
        updateStatusUI();
        binding.showOptionsButton.setOnClickListener(view -> hideShowOptionsButton(false));
        binding.closeButton.setOnClickListener(view -> hideShowOptionsButton(true));
        binding.createNewButton.setOnClickListener(v -> {
            showCreateDialog();
            hideShowOptionsButton(true);
        });
        binding.importNewButton.setOnClickListener(v -> {
            showImportDialog();
            hideShowOptionsButton(true);
        });
    }

    private void updateStatusUI() {
        boolean isEnabled = (sc_id != null && !sc_id.isEmpty()) && DayDreamProjectSettings.isEnableDayDream(sc_id);
        binding.topAppBar.setSubtitle(isEnabled ? "Status: Enabled" : "Status: Disabled (Compilation Off)");

        if (statusBannerCard == null) {
            statusBannerCard = new MaterialCardView(this);
            int marginHoriz = (int) (16 * getResources().getDisplayMetrics().density);
            int marginVert = (int) (10 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(marginHoriz, marginVert, marginHoriz, (int) (6 * getResources().getDisplayMetrics().density));
            statusBannerCard.setLayoutParams(lp);
            statusBannerCard.setRadius(14 * getResources().getDisplayMetrics().density);
            statusBannerCard.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER_VERTICAL);
            int pad = (int) (14 * getResources().getDisplayMetrics().density);
            container.setPadding(pad, (int) (10 * getResources().getDisplayMetrics().density), pad, (int) (10 * getResources().getDisplayMetrics().density));

            ImageView icon = new ImageView(this);
            int iconSize = (int) (26 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
            icon.setLayoutParams(iconLp);
            container.addView(icon);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textCol.setLayoutParams(textLp);

            TextView title = new TextView(this);
            title.setTextSize(14f);
            title.setTypeface(null, Typeface.BOLD);
            textCol.addView(title);

            TextView desc = new TextView(this);
            desc.setTextSize(12f);
            desc.setPadding(0, (int) (2 * getResources().getDisplayMetrics().density), 0, 0);
            textCol.addView(desc);

            container.addView(textCol);

            TextView actionChip = new TextView(this);
            actionChip.setTextSize(12f);
            actionChip.setTypeface(null, Typeface.BOLD);
            actionChip.setPadding(
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density)
            );
            container.addView(actionChip);

            statusBannerCard.addView(container);
            statusBannerCard.setOnClickListener(v -> {
                if (sc_id != null && !sc_id.isEmpty()) {
                    Intent intent = new Intent(this, com.besome.sketch.editor.manage.library.daydream.DayDreamLibraryActivity.class);
                    intent.putExtra("sc_id", sc_id);
                    startActivity(intent);
                }
            });

            binding.contentLayout.addView(statusBannerCard, 0);
        }

        LinearLayout container = (LinearLayout) statusBannerCard.getChildAt(0);
        ImageView icon = (ImageView) container.getChildAt(0);
        LinearLayout textCol = (LinearLayout) container.getChildAt(1);
        TextView title = (TextView) textCol.getChildAt(0);
        TextView desc = (TextView) textCol.getChildAt(1);
        TextView actionChip = (TextView) container.getChildAt(2);

        if (isEnabled) {
            statusBannerCard.setCardBackgroundColor(getColor(R.color.primaryContainer));
            statusBannerCard.setStrokeColor(getColor(R.color.primary));
            icon.setImageResource(R.drawable.ic_mtrl_check);
            icon.setColorFilter(getColor(R.color.primary));
            title.setText("Native Tools: Enabled");
            title.setTextColor(getColor(R.color.onPrimaryContainer));
            desc.setText("C/C++ files will be compiled into your APK.");
            desc.setTextColor(getColor(R.color.onSurfaceVariant));
            actionChip.setText("SETTINGS");
            actionChip.setTextColor(getColor(R.color.primary));
        } else {
            statusBannerCard.setCardBackgroundColor(getColor(R.color.errorContainer));
            statusBannerCard.setStrokeColor(getColor(R.color.error));
            icon.setImageResource(R.drawable.ic_mtrl_warning);
            icon.setColorFilter(getColor(R.color.error));
            title.setText("Native Tools: Disabled");
            title.setTextColor(getColor(R.color.onErrorContainer));
            desc.setText("Native compilation is turned off. C/C++ files will NOT be compiled.");
            desc.setTextColor(getColor(R.color.onSurfaceVariant));
            actionChip.setText("ENABLE");
            actionChip.setTextColor(getColor(R.color.error));
        }

        if (binding.noContentLayout.getChildCount() >= 2) {
            View child1 = binding.noContentLayout.getChildAt(1);
            if (child1 instanceof TextView) {
                ((TextView) child1).setText(isEnabled
                        ? "Native tools is enabled. Tap + to add C/C++ files."
                        : "Native tools is currently disabled in Library Manager.\nTap here to configure or enable it.");
                binding.noContentLayout.setOnClickListener(v -> {
                    if (!isEnabled && sc_id != null && !sc_id.isEmpty()) {
                        Intent intent = new Intent(this, com.besome.sketch.editor.manage.library.daydream.DayDreamLibraryActivity.class);
                        intent.putExtra("sc_id", sc_id);
                        startActivity(intent);
                    }
                });
            }
        }
    }

    private void hideShowOptionsButton(boolean isHide) {
        binding.optionsLayout.animate().translationY(isHide ? 300 : 0).alpha(isHide ? 0 : 1).setInterpolator(new OvershootInterpolator());

        binding.showOptionsButton.animate().translationY(isHide ? 0 : 300).alpha(isHide ? 1 : 0).setInterpolator(new OvershootInterpolator());
    }

    private void showCreateDialog() {
        DialogCreateNewFileLayoutBinding dialogBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle("Create new")
                .setMessage("File extension will be added automatically")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputText.requestFocus();

            Button positiveButton = ((androidx.appcompat.app.AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (Helper.getText(inputText).isEmpty()) {
                    SketchwareUtil.toastError("Invalid file name");
                    return;
                }

                String name = Helper.getText(inputText);
                String pkgName = getIntent().getStringExtra("pkgName");
                String packageName = jniMangle(pkgName != null ? pkgName : "");
                String className = jniMangle(name);

                String extension;
                String newFileContent;
                int checkedChipId = dialogBinding.chipGroupTypes.getCheckedChipId();
                if (checkedChipId == R.id.chip_file) {
                    FileUtil.writeFile(new File(current_path, name).getAbsolutePath(), "");
                    refresh();
                    SketchwareUtil.toast("File was created successfully");
                    dialog.dismiss();
                    return;
                } else if (checkedChipId == R.id.chip_cpp_file) {
                    newFileContent = String.format(CPP_TEMPLATE, packageName, className);
                    extension = ".cpp";
                } else if (checkedChipId == R.id.chip_c_file) {
                    newFileContent = String.format(C_TEMPLATE, packageName, className);
                    extension = ".c";
                } else if (checkedChipId == R.id.chip_header_file) {
                    String guard = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
                    newFileContent = String.format(HEADER_TEMPLATE, guard, guard, guard);
                    extension = ".h";
                } else if (checkedChipId == R.id.chip_folder) {
                    FileUtil.makeDir(new File(current_path, name).getAbsolutePath());
                    refresh();
                    SketchwareUtil.toast("Folder was created successfully");
                    dialog.dismiss();
                    return;
                } else {
                    SketchwareUtil.toast("Select a file type");
                    return;
                }

                FileUtil.writeFile(new File(current_path, name + extension).getAbsolutePath(), newFileContent);
                refresh();
                SketchwareUtil.toast("File was created successfully");
                dialog.dismiss();
            });

            dialogBinding.chipFolder.setVisibility(View.VISIBLE);
            dialogBinding.chipCppFile.setVisibility(View.VISIBLE);
            dialogBinding.chipCFile.setVisibility(View.VISIBLE);
            dialogBinding.chipHeaderFile.setVisibility(View.VISIBLE);
            dialogBinding.chipFile.setVisibility(View.VISIBLE);
        });

        dialog.show();
    }

    private void showImportDialog() {
        FilePickerOptions options = new FilePickerOptions();
        options.setMultipleSelection(true);
        options.setExtensions(new String[]{"cpp", "c", "h", "hpp"});
        options.setTitle("Select Native file(s)");

        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFilesSelected(@NonNull List<? extends File> files) {
                for (File file : files) {
                    FileUtil.copyFile(file.getAbsolutePath(), new File(current_path, file.getName()).getAbsolutePath());
                }
                refresh();
            }
        };

        new FilePickerDialogFragment(options, callback).show(getSupportFragmentManager(), "filePicker");
    }

    private void showRenameDialog(int position) {
        DialogInputLayoutBinding dialogBinding = DialogInputLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Rename " + filesAdapter.getFileName(position))
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Rename", (dialogInterface, i) -> {
                    if (!Helper.getText(inputText).isEmpty()) {
                        FileUtil.renameFile(filesAdapter.getItem(position), new File(current_path, Helper.getText(inputText)).getAbsolutePath());
                        refresh();
                        SketchwareUtil.toast("Renamed successfully");
                    }
                    dialogInterface.dismiss();
                })
                .create();

        inputText.setText(filesAdapter.getFileName(position));
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showDeleteDialog(int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + filesAdapter.getFileName(position) + "?")
                .setMessage("Are you sure you want to delete this " + (filesAdapter.isFolder(position) ? "folder" : "file") + "? This action cannot be undone.")
                .setPositiveButton(R.string.common_word_delete, (dialog, which) -> {
                    FileUtil.deleteFile(filesAdapter.getItem(position));
                    refresh();
                    SketchwareUtil.toast("Deleted successfully");
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .create()
                .show();
    }

    private void refresh() {
        if (!FileUtil.isExistFile(fpu.getPathNative(sc_id))) {
            FileUtil.makeDir(fpu.getPathNative(sc_id));
        }

        currentTree.clear();
        FileUtil.listDir(current_path, currentTree);
        Helper.sortPaths(currentTree);

        filesAdapter = new FilesAdapter(currentTree);
        binding.filesListRecyclerView.setAdapter(filesAdapter);
        binding.noContentLayout.setVisibility(currentTree.isEmpty() ? View.VISIBLE : View.GONE);
        updateStatusUI();
    }

    public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
        private final List<String> currentTree;

        public FilesAdapter(List<String> currentTree) {
            this.currentTree = currentTree;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageJavaItemHsBinding binding = ManageJavaItemHsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            var binding = holder.binding;
            String fileName = getFileName(position);

            binding.title.setText(fileName);

            binding.getRoot().setOnClickListener(view -> {
                if (isFolder(position)) {
                    current_path = filesAdapter.getItem(position);
                    refresh();
                    return;
                }
                goEditFile(position);
            });
            binding.getRoot().setOnLongClickListener(view -> {
                itemContextMenu(view, position, Gravity.CENTER);
                return true;
            });

            if (isFolder(position)) {
                binding.icon.setImageResource(R.drawable.ic_mtrl_folder);
            } else {
                if (fileName.endsWith(".cpp")) {
                    binding.icon.setImageResource(R.drawable.ic_mtrl_cpp);
                } else {
                    if (fileName.endsWith(".c")) {
                        binding.icon.setImageResource(R.drawable.ic_mtrl_c);
                    } else {
                        if (fileName.endsWith(".h") || fileName.endsWith(".hpp")) {
                            binding.icon.setImageResource(R.drawable.ic_mtrl_c);
                        } else {
                            binding.icon.setImageResource(R.drawable.ic_mtrl_file);
                        }
                    }
                }
            }

            Helper.applyRipple(ManageNativeActivity.this, binding.more);

            binding.more.setOnClickListener(v -> itemContextMenu(v, position, Gravity.RIGHT));
        }

        @Override
        public int getItemCount() {
            return currentTree.size();
        }

        public String getItem(int position) {
            return currentTree.get(position);
        }

        public String getFileName(int position) {
            String item = getItem(position);
            return item.substring(item.lastIndexOf("/") + 1);
        }

        public boolean isFolder(int position) {
            return FileUtil.isDirectory(getItem(position));
        }

        public void goEditFile(int position) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), SrcCodeEditor.class);
            intent.putExtra("java", "");
            intent.putExtra("sc_id", sc_id);
            intent.putExtra("title", getFileName(position));
            intent.putExtra("content", getItem(position));

            startActivity(intent);
        }

        private void itemContextMenu(View v, int position, int gravity) {
            PopupMenu popupMenu = new PopupMenu(ManageNativeActivity.this, v, gravity);
            Menu menu = popupMenu.getMenu();

            if (!isFolder(position)) {
                menu.add("Edit");
            }
            menu.add("Rename");
            menu.add("Delete");

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getTitle().toString()) {
                    case "Edit" -> goEditFile(position);
                    case "Rename" -> showRenameDialog(position);
                    case "Delete" -> showDeleteDialog(position);
                    default -> {
                        return false;
                    }
                }

                return true;
            });

            popupMenu.show();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ManageJavaItemHsBinding binding;

            public ViewHolder(ManageJavaItemHsBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}