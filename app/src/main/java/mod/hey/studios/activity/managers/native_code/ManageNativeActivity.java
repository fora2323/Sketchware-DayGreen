package mod.hey.studios.activity.managers.native_code;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import dev.pranav.filepicker.SelectionMode;
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

public class ManageNativeActivity extends BaseAppCompatActivity {

    private final ArrayList<String> currentTree = new ArrayList<>();
    private String current_path;
    private FilePathUtil fpu;
    private NativeAdapter nativeAdapter;
    private String sc_id;

    private ManageFileBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sc_id = getIntent().getStringExtra("sc_id");
        Helper.fixFileprovider();
        setupUI();

        fpu = new FilePathUtil();
        current_path = Uri.parse(fpu.getPathNative(sc_id)).getPath();

        refresh();
    }

    private void setupUI() {
        binding.topAppBar.setTitle("Cpp/C Manager");
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
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

    private void hideShowOptionsButton(boolean isHide) {
        binding.optionsLayout.animate()
                .translationY(isHide ? 300 : 0)
                .alpha(isHide ? 0 : 1)
                .setInterpolator(new OvershootInterpolator());

        binding.showOptionsButton.animate()
                .translationY(isHide ? 0 : 300)
                .alpha(isHide ? 1 : 0)
                .setInterpolator(new OvershootInterpolator());
    }

    @Override
    public void onBackPressed() {
        if (Objects.equals(
                Uri.parse(current_path).getPath(),
                Uri.parse(fpu.getPathNative(sc_id)).getPath()
        )) {
            super.onBackPressed();
        } else {
            current_path = current_path.substring(0, current_path.lastIndexOf(File.separator));
            refresh();
        }
    }

    @SuppressLint("SetTextI18n")
    private void showCreateDialog() {
        DialogCreateNewFileLayoutBinding dialogBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        dialogBinding.chipCppFile.setVisibility(View.VISIBLE);
        dialogBinding.chipCFile.setVisibility(View.VISIBLE);
        dialogBinding.chipHeaderFile.setVisibility(View.VISIBLE);

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle("Create new")
                .setMessage("Enter the file name. Extension will be appended based on selected type.")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String editable = Helper.getText(inputText).trim();

                if (editable.isEmpty()) {
                    SketchwareUtil.toastError("Invalid name");
                    return;
                }

                String ext = ".cpp";
                int checkedChipId = dialogBinding.chipGroupTypes.getCheckedChipId();
                if (checkedChipId == R.id.chip_c_file) {
                    ext = ".c";
                } else if (checkedChipId == R.id.chip_header_file) {
                    ext = ".h";
                }

                if (!editable.endsWith(".cpp") && !editable.endsWith(".c") && !editable.endsWith(".h") && !editable.endsWith(".hpp")) {
                    editable += ext;
                }

                File newFile = new File(current_path, editable);
                if (newFile.exists()) {
                    SketchwareUtil.toastError("File already exists");
                    return;
                }

                FileUtil.writeFile(newFile.getAbsolutePath(), "// Native source code\n");
                refresh();
                SketchwareUtil.toast("File created successfully");
                dialogInterface.dismiss();
            });
        });

        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showRenameDialog(int position) {
        DialogInputLayoutBinding dialogBinding = DialogInputLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Rename " + nativeAdapter.getFileName(position))
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Rename", (dialogInterface, i) -> {
                    if (!Helper.getText(inputText).isEmpty()) {
                        FileUtil.renameFile(nativeAdapter.getItem(position), new File(current_path, Helper.getText(inputText)).getAbsolutePath());
                        refresh();
                        SketchwareUtil.toast("Renamed successfully");
                    }
                    dialogInterface.dismiss();
                })
                .create();

        inputText.setText(nativeAdapter.getFileName(position));
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showDeleteDialog(int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + nativeAdapter.getFileName(position) + "?")
                .setMessage("Are you sure you want to delete this " + (nativeAdapter.isFolder(position) ? "folder" : "file") + "? "
                        + "This action cannot be undone.")
                .setPositiveButton(R.string.common_word_delete, (dialog, which) -> {
                    FileUtil.deleteFile(nativeAdapter.getItem(position));
                    refresh();
                    SketchwareUtil.toast("Deleted successfully");
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .create()
                .show();
    }

    private void showImportDialog() {
        FilePickerOptions options = new FilePickerOptions();
        options.setSelectionMode(SelectionMode.BOTH);
        options.setMultipleSelection(true);
        options.setTitle("Select a native file");

        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFilesSelected(@NotNull List<? extends File> files) {
                for (File file : files) {
                    try {
                        FileUtil.copyDirectory(file, new File(current_path, file.getName()));
                        refresh();
                    } catch (IOException e) {
                        SketchwareUtil.toastError("Couldn't import file! [" + e.getMessage() + "]");
                    }
                }
            }
        };

        new FilePickerDialogFragment(options, callback).show(getSupportFragmentManager(), "filePicker");
    }

    private void refresh() {
        if (!FileUtil.isExistFile(fpu.getPathNative(sc_id))) {
            FileUtil.makeDir(fpu.getPathNative(sc_id));
        }

        currentTree.clear();
        FileUtil.listDir(current_path, currentTree);
        Helper.sortPaths(currentTree);

        nativeAdapter = new NativeAdapter();
        binding.filesListRecyclerView.setAdapter(nativeAdapter);
        if (currentTree.isEmpty()) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
        } else {
            binding.noContentLayout.setVisibility(View.GONE);
        }
    }

    public class NativeAdapter extends RecyclerView.Adapter<NativeAdapter.NativeViewHolder> {

        private static final String[] textExtensions = {
                ".c", ".h", ".hpp", ".cpp", ".cc", ".cxx", ".txt", ".md"
        };

        @NonNull
        @Override
        public NativeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ManageJavaItemHsBinding binding = ManageJavaItemHsBinding.inflate(inflater, parent, false);
            var layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            binding.getRoot().setLayoutParams(layoutParams);
            return new NativeViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull NativeViewHolder holder, int position) {
            var item = getItem(position);
            var binding = holder.binding;

            binding.title.setText(getFileName(position));
            binding.getRoot().setOnClickListener(view -> {
                if (isFolder(position)) {
                    current_path = getItem(position);
                    refresh();
                } else {
                    goEditFile(position);
                }
            });

            binding.getRoot().setOnLongClickListener(view -> {
                holder.binding.more.performClick();
                return false;
            });

            if (isFolder(position)) {
                holder.binding.icon.setImageResource(R.drawable.ic_mtrl_folder);
            } else {
                holder.binding.icon.setImageResource(R.drawable.ic_mtrl_apk_document);
            }

            Helper.applyRipple(holder.itemView.getContext(), holder.binding.more);
            holder.binding.more.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(holder.itemView.getContext(), v);

                if (!isFolder(position)) {
                    popupMenu.getMenu().add(0, 0, 0, "Edit");
                }

                popupMenu.getMenu().add(0, 1, 0, "Rename");
                popupMenu.getMenu().add(0, 2, 0, "Delete");

                popupMenu.setOnMenuItemClickListener(itemMenu -> {
                    switch (itemMenu.getItemId()) {
                        case 0 -> goEditFile(position);
                        case 1 -> showRenameDialog(position);
                        case 2 -> showDeleteDialog(position);
                        default -> {
                            return false;
                        }
                    }
                    return true;
                });
                popupMenu.show();
            });
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
            return item.substring(item.lastIndexOf(File.separator) + 1);
        }

        public boolean isFolder(int position) {
            return FileUtil.isDirectory(getItem(position));
        }

        public void goEditFile(int position) {
            if (Arrays.stream(textExtensions).anyMatch(getItem(position)::endsWith)) {
                Intent launchIntent = new Intent();
                launchIntent.setClass(getApplicationContext(), SrcCodeEditor.class);
                launchIntent.putExtra("title", getFileName(position));
                launchIntent.putExtra("content", getItem(position));
                launchIntent.putExtra("sc_id", sc_id);
                startActivity(launchIntent);
            } else {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(Uri.fromFile(new File(getItem(position))), "*/*");
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(viewIntent);
            }
        }

        public static class NativeViewHolder extends RecyclerView.ViewHolder {
            ManageJavaItemHsBinding binding;

            public NativeViewHolder(ManageJavaItemHsBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
