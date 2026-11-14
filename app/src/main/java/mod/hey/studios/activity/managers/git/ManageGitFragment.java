package mod.hey.studios.activity.managers.git;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.besome.sketch.design.DesignActivity;

import org.eclipse.jgit.api.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private String branchName = "main";

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

        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.tvNoChanges.setVisibility(View.GONE);
            loadStatus(true);
        });
        binding.btnRefreshGit.setOnClickListener(v -> loadStatus(true));
        binding.tvGitBranch.setOnClickListener(v -> showBranchDialog());
        binding.btnCommit.setOnClickListener(v -> handleCommit());
        binding.btnPush.setOnClickListener(v -> handlePush());
        binding.btnPull.setOnClickListener(v -> handlePull());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sc_id != null) loadStatus(true);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void runOnUi(Runnable action) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) return;
        activity.runOnUiThread(action);
    }

    private void loadStatus(boolean syncFirst) {
        if (sc_id == null || sc_id.isEmpty()) return;
        Context context = getContext();
        if (context == null) return;
        Activity host = getActivity();

        new Thread(() -> {
            try {
                if (syncFirst) {
                    if (host instanceof DesignActivity) {
                        ((DesignActivity) host).saveForGit();
                    }
                    GitManager.syncProjectToGitDir(sc_id);
                }

                branchName = GitManager.currentBranch(sc_id);
                Status status = GitManager.getStatus(sc_id);

                List<ChangedFileItem> changed = new ArrayList<>();
                if (status != null) {
                    for (String s : status.getModified()) changed.add(new ChangedFileItem(s, "M"));
                    for (String s : status.getChanged()) changed.add(new ChangedFileItem(s, "M"));
                    for (String s : status.getAdded()) changed.add(new ChangedFileItem(s, "A"));
                    for (String s : status.getUntracked()) changed.add(new ChangedFileItem(s, "A"));
                    for (String s : status.getMissing()) changed.add(new ChangedFileItem(s, "D"));
                    for (String s : status.getRemoved()) changed.add(new ChangedFileItem(s, "D"));
                }
                changed.removeIf(i -> i.path.equals("project"));

                int unpushed = GitManager.countUnpushedCommits(sc_id, branchName);

                String authorName = GitConfiguration.getUserName(context);
                String authorEmail = GitConfiguration.getUserEmail(context);
                String author = (authorName.isEmpty() ? "Unknown User" : authorName)
                        + " <" + (authorEmail.isEmpty() ? "no-email" : authorEmail) + ">";

                runOnUi(() -> {
                    if (binding == null) return;
                    binding.swipeRefresh.setRefreshing(false);
                    binding.tvGitUser.setText("Author: " + author);
                    binding.tvGitBranch.setText("Branch: " + branchName);

                    adapter.setItems(changed);
                    binding.tvNoChanges.setVisibility(changed.isEmpty() ? View.VISIBLE : View.GONE);

                    updateBadge(changed.size(), unpushed);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading git status: ", e);
                runOnUi(() -> {
                    if (binding == null) return;
                    binding.swipeRefresh.setRefreshing(false);
                    binding.tvNoChanges.setText("Error reading Git status: " + e.getMessage());
                    binding.tvNoChanges.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void updateBadge(int changedCount, int unpushedCount) {
        int count = changedCount > 0 ? changedCount : unpushedCount;
        if (count <= 0) {
            binding.tvGitBadge.setVisibility(View.GONE);
        } else {
            binding.tvGitBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            binding.tvGitBadge.setVisibility(View.VISIBLE);
        }
    }

    private void handleCommit() {
        String message = Objects.requireNonNull(binding.etCommitMessage.getText()).toString().trim();
        if (message.isEmpty()) {
            SketchwareUtil.toast("Please enter a commit message");
            return;
        }

        CheckBox cbAmend = buildCheckBox("Amend (timpa commit terakhir)");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Commit")
                .setView(wrapCheckBox(cbAmend))
                .setPositiveButton("Commit", (d, w) -> doCommit(message, cbAmend.isChecked()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doCommit(String message, boolean amend) {
        Context context = requireContext();
        AlertDialog busy = showBusy("Committing...");
        new Thread(() -> {
            try {
                GitManager.commit(context, sc_id, message, amend);
                runOnUi(() -> {
                    busy.dismiss();
                    if (binding != null) binding.etCommitMessage.setText("");
                    SketchwareUtil.toast("Committed successfully!");
                    loadStatus(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Commit failed: ", e);
                runOnUi(() -> {
                    busy.dismiss();
                    SketchwareUtil.toastError("Commit failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handlePush() {
        Context context = requireContext();
        String username = GitConfiguration.getUsername(context);
        String password = GitConfiguration.getPassword(context);
        if (username.isEmpty() || password.isEmpty()) {
            SketchwareUtil.toast("Please configure Git Credentials in App Settings -> Git");
            return;
        }

        CheckBox cbForce = buildCheckBox("Force push (timpa isi remote)");

        new MaterialAlertDialogBuilder(context)
                .setTitle("Push ke GitHub")
                .setView(wrapCheckBox(cbForce))
                .setPositiveButton("Push", (d, w) -> doPush(cbForce.isChecked()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doPush(boolean force) {
        Context context = requireContext();
        AlertDialog busy = showBusy(force ? "Force pushing..." : "Pushing...");
        new Thread(() -> {
            try {
                if (force) {
                    GitManager.forcePush(context, sc_id, branchName, null);
                } else {
                    GitManager.push(context, sc_id, branchName, null);
                }
                runOnUi(() -> {
                    busy.dismiss();
                    SketchwareUtil.toast("Pushed to GitHub successfully!");
                    loadStatus(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Push failed: ", e);
                runOnUi(() -> {
                    busy.dismiss();
                    SketchwareUtil.toastError("Push failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handlePull() {
        Context context = requireContext();
        String username = GitConfiguration.getUsername(context);
        String password = GitConfiguration.getPassword(context);
        if (username.isEmpty() || password.isEmpty()) {
            SketchwareUtil.toast("Please configure Git Credentials in App Settings -> Git");
            return;
        }

        AlertDialog busy = showBusy("Pulling...");
        new Thread(() -> {
            try {
                GitManager.pull(context, sc_id, null);
                runOnUi(() -> {
                    busy.dismiss();
                    SketchwareUtil.toast("Pulled from GitHub successfully!");
                    loadStatus(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Pull failed: ", e);
                runOnUi(() -> {
                    busy.dismiss();
                    SketchwareUtil.toastError("Pull failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showBranchDialog() {
        Context context = requireContext();
        new Thread(() -> {
            try {
                List<String> branches = GitManager.listLocalBranches(sc_id);
                runOnUi(() -> {
                    String[] items = branches.toArray(new String[0]);
                    int checked = branches.indexOf(branchName);
                    final int[] selected = {checked};

                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Pilih branch")
                            .setSingleChoiceItems(items, checked, (d, which) -> selected[0] = which)
                            .setPositiveButton("Pindah", (d, w) -> {
                                if (selected[0] >= 0 && !items[selected[0]].equals(branchName)) {
                                    confirmCheckout(items[selected[0]], false);
                                }
                            })
                            .setNeutralButton("Branch baru", (d, w) -> askNewBranchName())
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } catch (Exception e) {
                Log.e(TAG, "List branch failed: ", e);
                runOnUi(() -> SketchwareUtil.toastError("Gagal membaca branch: " + e.getMessage()));
            }
        }).start();
    }

    private void askNewBranchName() {
        Context context = requireContext();
        EditText input = new EditText(context);
        input.setHint("nama-branch");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Branch baru")
                .setView(input)
                .setPositiveButton("Buat", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) confirmCheckout(name, true);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmCheckout(String name, boolean create) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pindah branch?")
                .setMessage("Isi project akan diganti dengan isi branch \"" + name + "\". Perubahan yang belum di-commit akan hilang.")
                .setPositiveButton("Lanjut", (d, w) -> doCheckout(name, create))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doCheckout(String name, boolean create) {
        Context context = requireContext();
        AlertDialog busy = showBusy("Pindah branch...");
        new Thread(() -> {
            try {
                GitManager.checkoutBranch(sc_id, name, create);
                GitManager.restoreProjectFromGitDir(sc_id);
                runOnUi(() -> {
                    busy.dismiss();
                    DesignActivity.needReloadProjectData = true;
                    SketchwareUtil.toast("Pindah ke branch " + name);
                    loadStatus(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Checkout failed: ", e);
                runOnUi(() -> {
                    busy.dismiss();
                    SketchwareUtil.toastError("Gagal pindah branch: " + e.getMessage());
                });
            }
        }).start();
    }

    private AlertDialog showBusy(String title) {
        ProgressBar bar = new ProgressBar(requireContext());
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        bar.setPadding(pad, pad, pad, pad);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(bar)
                .setCancelable(false)
                .create();
        dialog.show();
        return dialog;
    }

    private CheckBox buildCheckBox(String text) {
        CheckBox cb = new CheckBox(requireContext());
        cb.setText(text);
        return cb;
    }

    private ViewGroup wrapCheckBox(CheckBox cb) {
        LinearLayout wrap = new LinearLayout(requireContext());
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        wrap.setPadding(pad, pad, pad, 0);
        wrap.addView(cb);
        return wrap;
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