package mod.agus.jcoderz.editor.manage.resource;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mod.bobur.VectorDrawableLoader;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.databinding.DialogInputLayoutBinding;
import pro.sketchware.databinding.FrManageFileBinding;
import pro.sketchware.databinding.ManageJavaItemHsBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ManageResourceFragment extends Fragment {

    private CustomAdapter adapter;
    private FilePathUtil fpu;
    private FileResConfig frc;
    private String numProj;
    private String temp;

    private FrManageFileBinding binding;

    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            try {
                temp = temp.substring(0, temp.lastIndexOf("/"));
                if (temp.contains("resource")) {
                    handleAdapter(temp);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    };

    public static ManageResourceFragment newInstance(String scId) {
        ManageResourceFragment fragment = new ManageResourceFragment();
        Bundle args = new Bundle();
        args.putString("sc_id", scId);
        fragment.setArguments(args);
        return fragment;
    }

    public static String getLastDirectory(String path) {
        // Get the index of the last occurrence of '/' character
        int lastSlashIndex = path.lastIndexOf('/');

        // Get the substring from the start to the lastSlashIndex
        String parentPath = path.substring(0, lastSlashIndex);

        // Get the index of the last occurrence of '/' in the parentPath
        lastSlashIndex = parentPath.lastIndexOf('/');

        // Extract the last directory name
        return parentPath.substring(lastSlashIndex + 1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            numProj = getArguments().getString("sc_id");
        }
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FrManageFileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Helper.fixFileprovider();
        frc = new FileResConfig(numProj);
        fpu = new FilePathUtil();
        checkDir();
        setupUI();
    }

    private void updateBackPressedCallback() {
        if (fpu != null && temp != null) {
            boolean isNotRoot = !temp.equals(fpu.getPathResource(numProj));
            backPressedCallback.setEnabled(isNotRoot);
        }
    }

    private void checkDir() {
        if (FileUtil.isExistFile(fpu.getPathResource(numProj))) {
            temp = fpu.getPathResource(numProj);
            handleAdapter(temp);
            return;
        }
        FileUtil.makeDir(fpu.getPathResource(numProj));
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/anim");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/drawable");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/drawable-xhdpi");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/layout");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/menu");
        FileUtil.makeDir(fpu.getPathResource(numProj) + "/values");
        checkDir();
    }

    private void handleAdapter(String str) {
        ArrayList<String> resourceFile = frc.getResourceFile(str);
        //noinspection Java8ListSort
        Collections.sort(resourceFile, String.CASE_INSENSITIVE_ORDER);
        adapter = new CustomAdapter(resourceFile);
        binding.filesListRecyclerView.setAdapter(adapter);

        binding.noContentLayout.setVisibility(resourceFile.isEmpty() ? View.VISIBLE : View.GONE);
        updateBackPressedCallback();
    }

    private boolean isInMainDirectory() {
        return temp != null && temp.equals(fpu.getPathResource(numProj));
    }

    private void setupUI() {
        binding.showOptionsButton.setOnClickListener(view -> createNewDialog(isInMainDirectory()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.filesListRecyclerView.getAdapter() != null) {
            binding.filesListRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void createNewDialog(boolean isFolder) {
        DialogCreateNewFileLayoutBinding dialogBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.getRoot())
                .setTitle(isFolder ? "Create a new folder" : "Create a new file")
                .setMessage("Enter a name for the new " + (isFolder ? "folder" : "file"))
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialogBinding.chipGroupTypes.setVisibility(View.GONE);
        if (!isFolder) {
            dialogBinding.inputText.setText(".xml");
        }

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((androidx.appcompat.app.AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (Helper.getText(inputText).isEmpty()) {
                    SketchwareUtil.toastError("Invalid name");
                    return;
                }

                String name = Helper.getText(inputText);
                String path;
                if (isFolder) {
                    path = fpu.getPathResource(numProj) + "/" + name;
                } else {
                    path = new File(temp + File.separator + name).getAbsolutePath();
                }

                if (FileUtil.isExistFile(path)) {
                    SketchwareUtil.toastError("File exists already");
                    return;
                }
                if (isFolder) {
                    FileUtil.makeDir(path);
                } else {
                    FileUtil.writeFile(path, "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                }
                handleAdapter(temp);
                SketchwareUtil.toast("Created file successfully");
                dialog.dismiss();
            });

            dialog.setView(dialogBinding.getRoot());
            dialog.show();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputText.requestFocus();

            if (!isFolder) {
                inputText.setSelection(0);
            }
        });

        dialog.show();
    }

    private void handleFab() {
    }

    private void showRenameDialog(String path) {
        DialogInputLayoutBinding dialogBinding = DialogInputLayoutBinding.inflate(getLayoutInflater());

        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename")
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Rename", (dialogInterface, i) -> {
                    if (!Helper.getText(inputText).isEmpty()) {
                        if (FileUtil.renameFile(path, path.substring(0, path.lastIndexOf("/")) + "/" + Helper.getText(inputText))) {
                            SketchwareUtil.toast("Renamed successfully");
                        } else {
                            SketchwareUtil.toastError("Renaming failed");
                        }
                        handleAdapter(temp);
                        handleFab();
                    }
                    dialogInterface.dismiss();
                })
                .create();


        try {
            inputText.setText(path.substring(path.lastIndexOf("/") + 1));
        } catch (IndexOutOfBoundsException e) {
            inputText.setText(path);
        }

        dialog.setView(dialogBinding.getRoot());
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showDeleteDialog(int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + Uri.fromFile(new File(adapter.getItem(position))).getLastPathSegment() + "?")
                .setMessage("Are you sure you want to delete this " + (FileUtil.isDirectory(adapter.getItem(position)) ? "folder" : "file") + "? "
                        + "This action cannot be undone.")
                .setPositiveButton(R.string.common_word_delete, (dialog, which) -> {
                    FileUtil.deleteFile(frc.listFileResource.get(position));
                    handleAdapter(temp);
                    SketchwareUtil.toast("Deleted");
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .create()
                .show();
    }

    private void goEdit(int position) {
        if (frc.listFileResource.get(position).endsWith("xml")) {
            Intent intent = new Intent();
            intent.setClass(requireContext(), SrcCodeEditor.class);
            intent.putExtra("title", Uri.parse(frc.listFileResource.get(position)).getLastPathSegment());
            intent.putExtra("content", frc.listFileResource.get(position));
            intent.putExtra("xml", "");
            intent.putExtra("sc_id", numProj);
            startActivity(intent);
        } else {
            SketchwareUtil.toast("Only XML files can be edited");
        }
    }

    private void goEdit2(int position) {
        if (frc.listFileResource.get(position).endsWith("xml")) {
            Intent intent = new Intent();
            intent.setClass(requireContext(), SrcCodeEditor.class);
            intent.putExtra("title", Uri.parse(frc.listFileResource.get(position)).getLastPathSegment());
            intent.putExtra("content", frc.listFileResource.get(position));
            intent.putExtra("xml", "");
            intent.putExtra("sc_id", numProj);
            startActivity(intent);
        } else {
            SketchwareUtil.toast("Only XML files can be edited");
        }
    }

    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private final ArrayList<String> data;

        public CustomAdapter(ArrayList<String> arrayList) {
            data = arrayList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageJavaItemHsBinding binding = ManageJavaItemHsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String path = data.get(position);
            var binding = holder.binding;

            binding.more.setVisibility(FileUtil.isDirectory(path) ? View.GONE : View.VISIBLE);
            binding.title.setText(Uri.parse(path).getLastPathSegment());

            if (FileUtil.isDirectory(path)) {
                binding.icon.setImageResource(R.drawable.ic_mtrl_folder);
            } else {
                try {
                    if (FileUtil.isImageFile(path)) {
                        Glide.with(ManageResourceFragment.this).load(new File(path)).into(binding.icon);
                    } else if (path.endsWith(".xml") && "drawable".equals(getLastDirectory(path))) {
                        new VectorDrawableLoader().setImageVectorFromFile(binding.icon, path);
                    } else {
                        binding.icon.setImageResource(R.drawable.ic_mtrl_file);
                    }
                } catch (Exception ignored) {
                    binding.icon.setImageResource(R.drawable.ic_mtrl_file);
                }
            }

            binding.more.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(requireContext(), v);
                popupMenu.inflate(R.menu.popup_menu_double);
                popupMenu.getMenu().getItem(0).setVisible(false);
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getTitle().toString()) {
                        case "Edit with..." -> {
                            if (frc.listFileResource.get(position).endsWith("xml")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(new File(frc.listFileResource.get(position))), "text/plain");
                                startActivity(intent);
                            } else {
                                SketchwareUtil.toast("Only XML files can be edited");
                            }
                        }
                        case "Edit" -> goEdit2(position);
                        case "Delete" -> showDeleteDialog(position);
                        case "Rename" -> showRenameDialog(frc.listFileResource.get(position));
                        default -> {
                            return false;
                        }
                    }
                    return true;
                });
                popupMenu.show();
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (FileUtil.isDirectory(frc.listFileResource.get(position))) {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), binding.more);
                    popupMenu.getMenu().add("Delete");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        showDeleteDialog(position);
                        return true;
                    });
                    popupMenu.show();
                } else {
                    binding.more.performClick();
                }
                return true;
            });
            binding.getRoot().setOnClickListener(view -> {
                if (FileUtil.isDirectory(frc.listFileResource.get(position))) {
                    temp = frc.listFileResource.get(position);
                    handleAdapter(temp);
                    handleFab();
                    return;
                }
                goEdit(position);
            });
        }

        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ManageJavaItemHsBinding binding;

            public ViewHolder(ManageJavaItemHsBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}