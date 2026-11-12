package mod.hey.studios.activity.managers.git;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import mod.hilal.saif.activities.tools.GitConfiguration;
import mod.hilal.saif.activities.tools.GitManager;
import pro.sketchware.R;
import pro.sketchware.databinding.FragmentManageGitBinding;
import pro.sketchware.databinding.ItemGitChangedFileBinding;
import pro.sketchware.utility.SketchwareUtil;

public class ManageGitFragment extends Fragment {

    private static final String TAG = "ManageGitFragment";

    private String sc_id;
    private FragmentManageGitBinding binding;
    private ChangedFilesAdapter adapter;

    public static ManageGitFragment newInstance(String scId) {
        ManageGitFragment fragment = new ManageGitFragment();
        Bundle args = new Bundle();
        args.putString("sc_id", scId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sc_id = getArguments().getString("sc_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentManageGitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ChangedFilesAdapter();
        binding.rvChangedFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChangedFiles.setAdapter(adapter);

        binding.btnRefreshGit.setOnClickListener(v -> refreshGitStatus());

        binding.btnCommit.setOnClickListener(v -> handleCommit());
        binding.btnPush.setOnClickListener(v -> handlePush());
        binding.btnPull.setOnClickListener(v -> handlePull());

        refreshGitStatus();
    }

    private void refreshGitStatus() {
        if (sc_id == null || sc_id.isEmpty()) return;

        new Thread(() -> {
            GitManager.syncProjectToGitDir(sc_id);

            try {
                Status status = GitManager.getStatus(sc_id);

                List<ChangedFileItem> changedFiles = new ArrayList<>();
                if (status != null) {
                    for (String s : status.getModified()) {
                        changedFiles.add(new ChangedFileItem(s, "M"));
                    }
                    for (String s : status.getChanged()) {
                        changedFiles.add(new ChangedFileItem(s, "M"));
                    }
                    for (String s : status.getAdded()) {
                        changedFiles.add(new ChangedFileItem(s, "A"));
                    }
                    for (String s : status.getUntracked()) {
                        changedFiles.add(new ChangedFileItem(s, "A"));
                    }
                    for (String s : status.getMissing()) {
                        changedFiles.add(new ChangedFileItem(s, "D"));
                    }
                    for (String s : status.getRemoved()) {
                        changedFiles.add(new ChangedFileItem(s, "D"));
                    }
                }

                String authorName = GitConfiguration.getUserName(requireContext());
                String authorEmail = GitConfiguration.getUserEmail(requireContext());
                String authorText = (authorName.isEmpty() ? "Unknown User" : authorName) + " <" + (authorEmail.isEmpty() ? "no-email" : authorEmail) + ">";

                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        binding.tvGitUser.setText("Author: " + authorText);
                        binding.tvGitBranch.setText("Branch: main");

                        adapter.setItems(changedFiles);
                        if (changedFiles.isEmpty()) {
                            binding.tvNoChanges.setVisibility(View.VISIBLE);
                            binding.rvChangedFiles.setVisibility(View.GONE);
                        } else {
                            binding.tvNoChanges.setVisibility(View.GONE);
                            binding.rvChangedFiles.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing status: ", e);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        binding.tvNoChanges.setText("Error reading Git status: " + e.getMessage());
                        binding.tvNoChanges.setVisibility(View.VISIBLE);
                        binding.rvChangedFiles.setVisibility(View.GONE);
                    });
                }
            }
        }).start();
    }

    private void handleCommit() {
        String message = Objects.requireNonNull(binding.etCommitMessage.getText()).toString().trim();
        if (message.isEmpty()) {
            SketchwareUtil.toast("Please enter a commit message");
            return;
        }

        new Thread(() -> {
            try {
                GitManager.commit(requireContext(), sc_id, message);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        binding.etCommitMessage.setText("");
                        SketchwareUtil.toast("Committed successfully!");
                        refreshGitStatus();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Commit failed: ", e);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> SketchwareUtil.toastError("Commit failed: " + e.getMessage()));
                }
            }
        }).start();
    }

    private void handlePush() {
        String username = GitConfiguration.getUsername(requireContext());
        String password = GitConfiguration.getPassword(requireContext());
        if (username.isEmpty() || password.isEmpty()) {
            SketchwareUtil.toast("Please configure Git Credentials in App Settings -> Git");
            return;
        }

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pushing to GitHub...")
                .setMessage("Uploading commits...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            try {
                GitManager.push(requireContext(), sc_id, null);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        SketchwareUtil.toast("Pushed to GitHub successfully!");
                        refreshGitStatus();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Push failed: ", e);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        SketchwareUtil.toastError("Push failed: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    private void handlePull() {
        String username = GitConfiguration.getUsername(requireContext());
        String password = GitConfiguration.getPassword(requireContext());
        if (username.isEmpty() || password.isEmpty()) {
            SketchwareUtil.toast("Please configure Git Credentials in App Settings -> Git");
            return;
        }

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pulling from GitHub...")
                .setMessage("Downloading commits...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            try {
                GitManager.pull(requireContext(), sc_id, null);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        SketchwareUtil.toast("Pulled from GitHub successfully!");
                        refreshGitStatus();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Pull failed: ", e);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        SketchwareUtil.toastError("Pull failed: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    private static class ChangedFileItem {
        final String path;
        final String status;

        ChangedFileItem(String path, String status) {
            this.path = path;
            this.status = status;
        }
    }

    private static class ChangedFilesAdapter extends RecyclerView.Adapter<ChangedFilesAdapter.ViewHolder> {

        private final List<ChangedFileItem> items = new ArrayList<>();

        void setItems(List<ChangedFileItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemGitChangedFileBinding binding = ItemGitChangedFileBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChangedFileItem item = items.get(position);
            holder.binding.tvFilePath.setText(item.path);
            holder.binding.tvFileStatusTag.setText(item.status);

            if ("M".equals(item.status)) {
                holder.binding.tvFileStatusTag.setBackgroundResource(R.drawable.shape_git_status_modified);
            } else if ("A".equals(item.status)) {
                holder.binding.tvFileStatusTag.setBackgroundResource(R.drawable.shape_git_status_added);
            } else {
                holder.binding.tvFileStatusTag.setBackgroundResource(R.drawable.shape_git_status_deleted);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemGitChangedFileBinding binding;

            ViewHolder(ItemGitChangedFileBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
