package mod.hilal.saif.activities.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.BuildConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cooper GitHub Manager landing screen.
 *
 * This screen intentionally exposes only verified UI entry points until the local Git
 * backend and OAuth configuration are implemented. It never reports a remote operation
 * as successful based solely on a user action.
 */
public class GithubManagerActivity extends BaseAppCompatActivity {
    private LinearLayout content;
    private final ExecutorService authExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService authPollingExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean authCancelled = new AtomicBoolean(false);
    @Nullable private AlertDialog authDialog;
    @Nullable private GitHubAuthManager.DeviceAuthorization pendingDeviceAuthorization;
    private boolean authInProgress = false;
    @Nullable private TextView directlyPendingImageState;
    @Nullable private java.util.ArrayList<Uri> directlyPendingImages;
    @Nullable private LinearLayout directlyPendingImagePreview;
    private boolean directlyDraftFormVisible = false;
    @Nullable private String directlyDraftProjectId;
    private String directlyDraftRepositoryName = "";
    private String directlyDraftDescription = "";
    private String directlyDraftReadmeMode = "";
    private String directlyDraftReadmeContent = "";
    private boolean directlyDraftIsPrivate = true;
    private boolean directlyDraftVisibilityChosen = false;
    private boolean directlyRecoveryPromptShown = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getBoolean("cooper_directly_draft_visible", false)) {
            directlyDraftFormVisible = true;
            directlyDraftProjectId = savedInstanceState.getString("cooper_directly_draft_project_id");
            directlyDraftRepositoryName = savedInstanceState.getString("cooper_directly_draft_repository_name", "");
            directlyDraftDescription = savedInstanceState.getString("cooper_directly_draft_description", "");
            directlyDraftReadmeMode = savedInstanceState.getString("cooper_directly_draft_readme_mode", "");
            directlyDraftReadmeContent = savedInstanceState.getString("cooper_directly_draft_readme_content", "");
            directlyDraftIsPrivate = savedInstanceState.getBoolean("cooper_directly_draft_private", true);
            directlyDraftVisibilityChosen = savedInstanceState.getBoolean("cooper_directly_draft_visibility_chosen", false);
            directlyPendingImages = new java.util.ArrayList<>();
            java.util.ArrayList<String> savedUris = savedInstanceState.getStringArrayList("cooper_directly_draft_image_uris");
            if (savedUris != null) {
                for (String value : savedUris) {
                    try { if (value != null) directlyPendingImages.add(Uri.parse(value)); }
                    catch (Exception ignored) { }
                }
            }
        }

        androidx.coordinatorlayout.widget.CoordinatorLayout root = new androidx.coordinatorlayout.widget.CoordinatorLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setClipToPadding(false);

        com.google.android.material.appbar.AppBarLayout appBar = new com.google.android.material.appbar.AppBarLayout(this);
        appBar.setFitsSystemWindows(true);
        appBar.setLayoutParams(new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        com.google.android.material.appbar.CollapsingToolbarLayout collapsing = new com.google.android.material.appbar.CollapsingToolbarLayout(this);
        collapsing.setTitle(getString(R.string.cooper_github_manager_title));
        collapsing.setFitsSystemWindows(true);
        collapsing.setExpandedTitleTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_DisplaySmall);
        collapsing.setCollapsedTitleTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        collapsing.setLayoutParams(new com.google.android.material.appbar.AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));
        ((com.google.android.material.appbar.AppBarLayout.LayoutParams) collapsing.getLayoutParams()).setScrollFlags(com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        com.google.android.material.appbar.CollapsingToolbarLayout.LayoutParams toolbarParams = new com.google.android.material.appbar.CollapsingToolbarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        toolbarParams.setCollapseMode(com.google.android.material.appbar.CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN);
        collapsing.addView(toolbar, toolbarParams);
        appBar.addView(collapsing);
        root.addView(appBar);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(false);
        scroll.setNestedScrollingEnabled(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(content, new androidx.core.widget.NestedScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams scrollParams = new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
        root.addView(scroll, scrollParams);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            appBar.setPadding(0, bars.top, 0, 0);
            return insets;
        });
        setContentView(root);
        addSectionTitle(getString(R.string.cooper_github_smart_safe));
        boolean credentialStored = new GitCredentialStore(this).hasCredential();
        if (!credentialStored) {
            addInfoCard(getString(R.string.cooper_github_account),
                    getString(R.string.cooper_github_not_connected),
                    getString(R.string.cooper_github_auth_not_configured));
        }
        if (credentialStored) {
            addGitHubProfileCard();
        } else {
            addActionCard(getString(R.string.cooper_github_connect), getString(R.string.cooper_github_connect_description),
                    v -> startGitHubAuthorization());
        }

        addSectionTitle(getString(R.string.cooper_github_directly_section));
        addActionCard(getString(R.string.cooper_github_directly_title), getString(R.string.cooper_github_directly_description),
                v -> showDirectlyGitBottomSheet());
        addActionCard(getString(R.string.cooper_github_history_action_title), getString(R.string.cooper_github_history_action_description),
                v -> showCooperGitPublicationHistory());
        addActionCard(getString(R.string.cooper_github_recovery_clean_action_title), getString(R.string.cooper_github_recovery_clean_action_description),
                v -> confirmClearDirectlyGitRecovery());

        addSectionTitle(getString(R.string.cooper_github_repositories));
        addActionCard(getString(R.string.cooper_github_browse_repositories), getString(R.string.cooper_github_browse_repositories_description),
                v -> loadRepositories());
        addActionCard(getString(R.string.cooper_github_link_project), getString(R.string.cooper_github_link_project_description),
                v -> loadProjectSelection());
        addActionCard(getString(R.string.cooper_github_create_repository), getString(R.string.cooper_github_create_repository_description),
                v -> showCreateRepositoryDialog());

        addSectionTitle(getString(R.string.cooper_github_pull_requests));
        addActionCard(getString(R.string.cooper_github_create_pull_request),
                "Create a Pull Request with explicit review and remote read-back verification.",
                v -> showCreatePullRequestDialog());

        addSectionTitle(getString(R.string.cooper_github_local_git));
        addInfoCard(getString(R.string.cooper_github_local_status), getString(R.string.cooper_github_not_initialized),
                getString(R.string.cooper_github_local_status_description));
        addActionCard(getString(R.string.cooper_github_inspect_changes), getString(R.string.cooper_github_inspect_changes_description),
                v -> inspectLocalGit());
        addActionCard(getString(R.string.cooper_github_review_diff), "Inspect staged, unstaged, and untracked file content without changing the project.",
                v -> reviewFileDiff());
        addActionCard(getString(R.string.cooper_github_commit_local), getString(R.string.cooper_github_commit_local_description),
                v -> inspectLocalGit());
        addActionCard(getString(R.string.cooper_github_fetch_remote), "Fetch remote refs without changing project files.",
                v -> runRemoteGitOperation("FETCH"));
        addActionCard(getString(R.string.cooper_github_push_remote), "Push the current committed branch after explicit approval and a secret scan.",
                v -> runRemoteGitOperation("PUSH"));
        addActionCard(getString(R.string.cooper_github_pull_remote), "Apply remote changes only when the local worktree is clean and you approve the operation.",
                v -> runRemoteGitOperation("PULL"));
        addActionCard(getString(R.string.cooper_github_sync_state), "Calculate ahead, behind, diverged, and conflict state from local Git objects.",
                v -> inspectSyncState());
        addActionCard(getString(R.string.cooper_github_local_branches), "List, create, and switch local branches without discarding uncommitted work.",
                v -> showLocalBranches());

        addSectionTitle(getString(R.string.cooper_github_safety));
        addInfoCard(getString(R.string.cooper_github_operation_policy), getString(R.string.cooper_github_smart_review),
                getString(R.string.cooper_github_safety_description));
        addActionCard(getString(R.string.cooper_github_implementation_status), getString(R.string.cooper_github_implementation_status_description),
                v -> new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cooper_github_status_dialog_title)
                        .setMessage(R.string.cooper_github_status_dialog_body)
                        .setPositiveButton(R.string.common_word_ok, null)
                        .show());
        if (directlyDraftFormVisible && directlyDraftProjectId != null) {
            content.post(this::restoreDirectlyGitDraft);
        }
    }

    private void restoreDirectlyGitDraft() {
        if (!directlyDraftFormVisible || directlyDraftProjectId == null || isFinishing() || isDestroyed()) return;
        try {
            GitProjectBindingStore.Binding binding = new GitProjectBindingStore(getApplicationContext()).getBoundProject();
            if (binding == null || !directlyDraftProjectId.equals(binding.scId)) {
                directlyDraftFormVisible = false;
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cooper_github_directly_title)
                        .setMessage(R.string.cooper_github_directly_recovery_project_mismatch)
                        .setPositiveButton(R.string.common_word_ok, null)
                        .show();
                return;
            }
            showDirectlyGitRepositoryForm(binding);
        } catch (Exception e) {
            directlyDraftFormVisible = false;
            showProjectBindingError(e);
        }
    }


    /**
     * Beginner-oriented entry point. The advanced Git Manager remains available below.
     * This first stage deliberately reuses the existing project binding and repository
     * creation safeguards instead of maintaining a second Git execution backend.
     */
    private void showDirectlyGitBottomSheet() {
        if (!new GitCredentialStore(this).hasCredential()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cooper_github_directly_title)
                    .setMessage(R.string.cooper_github_directly_login_required)
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .setPositiveButton(R.string.cooper_github_connect, (dialog, which) -> startGitHubAuthorization())
                    .show();
            return;
        }
        GitProjectBindingStore.Binding binding = null;
        try { binding = new GitProjectBindingStore(getApplicationContext()).getBoundProject(); }
        catch (Exception e) { showProjectBindingError(e); return; }
        showDirectlyGitSelectionSheet(binding);
    }

    private void showDirectlyGitSelectionSheet(@Nullable GitProjectBindingStore.Binding binding) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        LinearLayout panel = directlyPanel();
        TextView title = text(getString(R.string.cooper_github_directly_title), 25, true);
        title.setGravity(Gravity.CENTER);
        title.setAlpha(0f);
        panel.addView(title);
        title.animate().alpha(1f).setDuration(550).start();
        TextView intro = text(getString(R.string.cooper_github_directly_intro), 14, false);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, dp(8), 0, dp(18));
        panel.addView(intro);
        MaterialCardView projectCard = new MaterialCardView(this);
        projectCard.setRadius(dp(22));
        projectCard.setCardElevation(dp(2));
        projectCard.setStrokeWidth(dp(1));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_manage);
        icon.setContentDescription(getString(R.string.cooper_github_directly_project_icon));
        row.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        detailParams.leftMargin = dp(14);
        details.addView(text(binding == null ? getString(R.string.cooper_github_directly_no_project)
                : binding.workspaceName, 20, true));
        details.addView(text(binding == null ? getString(R.string.cooper_github_directly_project_details)
                : getString(R.string.cooper_github_directly_selected_project_details,
                binding.scId, binding.projectRoot.getAbsolutePath()), 12, false));
        if (binding != null) addDirectlyGitProjectMetadata(details, binding);
        row.addView(details, detailParams);
        projectCard.addView(row);
        panel.addView(projectCard);
        com.google.android.material.button.MaterialButton projectButton =
                new com.google.android.material.button.MaterialButton(this);
        projectButton.setText(binding == null ? R.string.cooper_github_directly_choose_project
                : R.string.cooper_github_directly_change_project);
        projectButton.setIconResource(android.R.drawable.ic_menu_search);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        buttonParams.topMargin = dp(16);
        panel.addView(projectButton, buttonParams);
        if (binding != null) {
            com.google.android.material.button.MaterialButton approve =
                    new com.google.android.material.button.MaterialButton(this);
            approve.setText(R.string.cooper_github_directly_approve);
            approve.setIconResource(android.R.drawable.checkbox_on_background);
            LinearLayout.LayoutParams approveParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
            approveParams.topMargin = dp(10);
            panel.addView(approve, approveParams);
            approve.setOnClickListener(v -> {
                sheet.dismiss();
                showDirectlyGitRepositoryForm(binding);
            });
        }
        TextView note = text(getString(R.string.cooper_github_directly_safety_note), 13, false);
        note.setPadding(dp(4), dp(14), dp(4), dp(8));
        panel.addView(note);
        com.google.android.material.button.MaterialButton advanced =
                new com.google.android.material.button.MaterialButton(this);
        advanced.setText(R.string.cooper_github_directly_advanced);
        panel.addView(advanced);
        advanced.setOnClickListener(v -> sheet.dismiss());
        projectButton.setOnClickListener(v -> {
            sheet.dismiss();
            loadDirectlyGitProjectSelection();
        });
        sheet.setContentView(panel);
        sheet.show();
    }

    private void addDirectlyGitProjectMetadata(LinearLayout target,
                                              GitProjectBindingStore.Binding binding) {
        String packageName = binding.packageName == null ? "" : binding.packageName.trim();
        target.addView(text(packageName.isEmpty()
                ? getString(R.string.cooper_github_directly_package_unavailable)
                : getString(R.string.cooper_github_directly_package_name, packageName), 13, false));

        String versionName = binding.versionName == null ? "" : binding.versionName.trim();
        String versionCode = binding.versionCode == null ? "" : binding.versionCode.trim();
        if (!versionName.isEmpty() || !versionCode.isEmpty()) {
            target.addView(text(getString(R.string.cooper_github_directly_application_version,
                    versionName.isEmpty() ? getString(R.string.cooper_github_directly_metadata_unavailable) : versionName,
                    versionCode.isEmpty() ? getString(R.string.cooper_github_directly_metadata_unavailable) : versionCode), 13, false));
        } else {
            target.addView(text(getString(R.string.cooper_github_directly_version_unavailable), 12, false));
        }
    }

    private LinearLayout directlyPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(16), dp(24), dp(24));
        panel.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF));
        return panel;
    }

    private void showDirectlyGitRepositoryForm(GitProjectBindingStore.Binding binding) {
        if (isFinishing() || isDestroyed()) return;
        final boolean restoreDraft = directlyDraftFormVisible
                && binding.scId.equals(directlyDraftProjectId);
        directlyDraftFormVisible = true;
        directlyDraftProjectId = binding.scId;
        if (!restoreDraft) {
            directlyDraftRepositoryName = "";
            directlyDraftDescription = "";
            directlyDraftReadmeMode = "";
            directlyDraftReadmeContent = "";
            directlyDraftIsPrivate = true;
            directlyPendingImages = new java.util.ArrayList<>();
        }
        final com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        sheet.setCancelable(false);
        sheet.setCanceledOnTouchOutside(false);
        LinearLayout panel = directlyPanel();
        com.google.android.material.button.MaterialButton closeForm =
                new com.google.android.material.button.MaterialButton(this);
        closeForm.setText(R.string.cooper_github_directly_close);
        closeForm.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeForm.setOnClickListener(v -> confirmDirectlyDismiss(() -> {
            directlyDraftFormVisible = false;
            directlyDraftProjectId = null;
            directlyDraftRepositoryName = "";
            directlyDraftDescription = "";
            directlyDraftReadmeMode = "";
            directlyDraftReadmeContent = "";
            directlyPendingImages = new java.util.ArrayList<>();
            sheet.dismiss();
        }));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        closeParams.gravity = Gravity.END;
        panel.addView(closeForm, closeParams);
        TextView title = text(getString(R.string.cooper_github_directly_title), 23, true);
        title.setGravity(Gravity.CENTER);
        title.setAlpha(0f);
        panel.addView(title);
        title.animate().alpha(1f).setDuration(450).start();
        panel.addView(text(binding.workspaceName + "  •  ID " + binding.scId, 16, true));
        if (binding.appName != null && !binding.appName.trim().isEmpty()) {
            panel.addView(text(getString(R.string.cooper_github_directly_application_name, binding.appName), 14, false));
        }
        addDirectlyGitProjectMetadata(panel, binding);
        panel.addView(text(binding.projectRoot.getAbsolutePath(), 12, false));
        EditText repoName = new EditText(this);
        repoName.setSingleLine(true);
        repoName.setHint(R.string.cooper_github_directly_repository_name);
        final String defaultRepositoryName = binding.workspaceName.replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("^[.-]+|[.-]+$", "");
        repoName.setText(restoreDraft ? directlyDraftRepositoryName : defaultRepositoryName);
        repoName.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(100)});
        LinearLayout repositoryNameRow = new LinearLayout(this);
        repositoryNameRow.setOrientation(LinearLayout.HORIZONTAL);
        repositoryNameRow.setGravity(Gravity.CENTER_VERTICAL);
        repositoryNameRow.addView(repoName, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        com.google.android.material.button.MaterialButton resetName = new com.google.android.material.button.MaterialButton(this);
        resetName.setIconResource(android.R.drawable.ic_menu_revert);
        resetName.setContentDescription(getString(R.string.cooper_github_directly_reset_repository_name));
        resetName.setMinWidth(dp(48));
        resetName.setOnClickListener(v -> {
            repoName.setText(defaultRepositoryName);
            repoName.setError(null);
        });
        repositoryNameRow.addView(resetName, new LinearLayout.LayoutParams(dp(52), dp(52)));
        panel.addView(repositoryNameRow);
        TextView nameRules = text(getString(R.string.cooper_github_directly_repository_rules), 12, false);
        panel.addView(nameRules);
        repoName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                int remaining = Math.max(0, 100 - s.length());
                nameRules.setText(getString(R.string.cooper_github_directly_name_counter, s.length(), remaining));
                directlyDraftRepositoryName = s.toString();
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });
        directlyDraftRepositoryName = repoName.getText().toString();
        EditText description = new EditText(this);
        description.setHint(R.string.cooper_github_directly_description_hint);
        description.setMinLines(2);
        description.setMaxLines(4);
        description.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(350)});
        if (restoreDraft) description.setText(directlyDraftDescription);
        description.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                directlyDraftDescription = s.toString();
            }
            @Override public void afterTextChanged(android.text.Editable e) { }
        });
        panel.addView(description);
        com.google.android.material.button.MaterialButton gallery = new com.google.android.material.button.MaterialButton(this);
        gallery.setText(R.string.cooper_github_directly_choose_images);
        gallery.setIconResource(android.R.drawable.ic_menu_gallery);
        panel.addView(gallery);
        TextView imageState = text(getString(R.string.cooper_github_directly_no_images), 13, false);
        panel.addView(imageState);
        LinearLayout imagePreviewRow = new LinearLayout(this);
        imagePreviewRow.setOrientation(LinearLayout.HORIZONTAL);
        imagePreviewRow.setPadding(0, dp(8), 0, dp(8));
        panel.addView(imagePreviewRow);
        final java.util.ArrayList<Uri> selectedImages = new java.util.ArrayList<>();
        if (restoreDraft && directlyPendingImages != null) selectedImages.addAll(directlyPendingImages);
        directlyPendingImageState = imageState;
        directlyPendingImages = selectedImages;
        directlyPendingImagePreview = imagePreviewRow;
        imageState.setText(selectedImages.isEmpty()
                ? getString(R.string.cooper_github_directly_no_images)
                : getString(R.string.cooper_github_directly_images_selected, selectedImages.size()));
        renderDirectlyGitImagePreviews(selectedImages, imagePreviewRow);
        gallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, 7314);
        });
        final boolean[] repositoryPrivate = new boolean[]{directlyDraftIsPrivate};
        final boolean[] visibilityChosen = new boolean[]{restoreDraft && directlyDraftVisibilityChosen};
        com.google.android.material.chip.ChipGroup visibilityChoices = new com.google.android.material.chip.ChipGroup(this);
        visibilityChoices.setSingleSelection(true);
        visibilityChoices.setSelectionRequired(true);
        com.google.android.material.chip.Chip privateChip = new com.google.android.material.chip.Chip(this);
        privateChip.setText(R.string.cooper_github_directly_visibility_private);
        privateChip.setCheckable(true);
        com.google.android.material.chip.Chip publicChip = new com.google.android.material.chip.Chip(this);
        publicChip.setText(R.string.cooper_github_directly_visibility_public);
        publicChip.setCheckable(true);
        visibilityChoices.addView(privateChip);
        visibilityChoices.addView(publicChip);
        privateChip.setChecked(visibilityChosen[0] && repositoryPrivate[0]);
        publicChip.setChecked(visibilityChosen[0] && !repositoryPrivate[0]);
        privateChip.setOnClickListener(v -> { repositoryPrivate[0] = true; visibilityChosen[0] = true; directlyDraftIsPrivate = true; directlyDraftVisibilityChosen = true; });
        publicChip.setOnClickListener(v -> { repositoryPrivate[0] = false; visibilityChosen[0] = true; directlyDraftIsPrivate = false; directlyDraftVisibilityChosen = true; });
        panel.addView(text(getString(R.string.cooper_github_directly_visibility_hint), 14, true));
        panel.addView(visibilityChoices);
        final String[] readmeMode = new String[]{restoreDraft ? directlyDraftReadmeMode : ""};
        com.google.android.material.chip.ChipGroup choices = new com.google.android.material.chip.ChipGroup(this);
        choices.setSingleSelection(true);
        choices.setSelectionRequired(false);
        com.google.android.material.chip.Chip readme = new com.google.android.material.chip.Chip(this);
        readme.setText(R.string.cooper_github_directly_readme);
        readme.setCheckable(true);
        com.google.android.material.chip.Chip empty = new com.google.android.material.chip.Chip(this);
        empty.setText(R.string.cooper_github_directly_empty);
        empty.setCheckable(true);
        choices.addView(readme);
        choices.addView(empty);
        panel.addView(text(getString(R.string.cooper_github_directly_readme_choice_required), 14, true));
        panel.addView(choices);
        final String[] readmeContent = new String[]{restoreDraft ? directlyDraftReadmeContent : ""};
        readme.setOnClickListener(v -> showDirectlyReadmeDialog(binding, readmeMode, readmeContent, choices, readme, empty));
        empty.setOnClickListener(v -> {
            readmeMode[0] = "EMPTY";
            readmeContent[0] = "";
            directlyDraftReadmeMode = "EMPTY";
            directlyDraftReadmeContent = "";
        });
        if ("EMPTY".equals(readmeMode[0])) empty.setChecked(true);
        else if ("MARKDOWN".equals(readmeMode[0]) || "TEXT".equals(readmeMode[0])) readme.setChecked(true);
        com.google.android.material.button.MaterialButton push = new com.google.android.material.button.MaterialButton(this);
        push.setText(R.string.cooper_github_directly_git_push);
        push.setIconResource(android.R.drawable.ic_menu_upload);
        LinearLayout.LayoutParams pushParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        pushParams.topMargin = dp(18);
        panel.addView(push, pushParams);
        push.setOnClickListener(v -> {
            String name = repoName.getText().toString().trim();
            if (!name.matches("[A-Za-z0-9_.-]{1,100}") || name.equals(".") || name.equals("..")
                    || name.contains("..") || name.startsWith(".") || name.endsWith(".")
                    || name.toLowerCase(java.util.Locale.ROOT).endsWith(".git")) {
                repoName.setError(getString(R.string.cooper_github_directly_invalid_repository_name));
                return;
            }
            if (readmeMode[0].isEmpty()) {
                Snackbar.make(panel, R.string.cooper_github_directly_readme_choice_required, Snackbar.LENGTH_LONG).show();
                return;
            }
            if (!visibilityChosen[0]) {
                Snackbar.make(panel, R.string.cooper_github_directly_visibility_required, Snackbar.LENGTH_LONG).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cooper_github_directly_publish_confirm)
                    .setMessage(getString(R.string.cooper_github_directly_publish_review, name, binding.workspaceName))
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .setPositiveButton(R.string.cooper_github_directly_git_push, (d, w) ->
                            beginDirectlyGitPublish(sheet, binding, name, description.getText().toString().trim(),
                                    readmeMode[0], readmeContent[0], repositoryPrivate[0], new java.util.ArrayList<>(selectedImages)))
                    .show();
        });
        sheet.setContentView(panel);
        sheet.show();
    }

    private void showDirectlyReadmeDialog(GitProjectBindingStore.Binding binding,
                                          String[] mode, String[] content,
                                          com.google.android.material.chip.ChipGroup choices,
                                          com.google.android.material.chip.Chip readme,
                                          com.google.android.material.chip.Chip empty) {
        EditText project = new EditText(this); project.setHint(R.string.cooper_github_directly_readme_project);
        project.setText(binding.workspaceName);
        EditText developers = new EditText(this); developers.setHint(R.string.cooper_github_directly_readme_developers);
        EditText contributors = new EditText(this); contributors.setHint(R.string.cooper_github_directly_readme_contributors);
        EditText information = new EditText(this); information.setHint(R.string.cooper_github_directly_readme_information);
        information.setMinLines(3);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        com.google.android.material.button.MaterialButtonToggleGroup format =
                new com.google.android.material.button.MaterialButtonToggleGroup(this);
        com.google.android.material.button.MaterialButton markdown = new com.google.android.material.button.MaterialButton(this);
        markdown.setText(R.string.cooper_github_directly_readme_format_markdown); markdown.setCheckable(true); markdown.setId(View.generateViewId());
        com.google.android.material.button.MaterialButton plain = new com.google.android.material.button.MaterialButton(this);
        plain.setText(R.string.cooper_github_directly_readme_format_text); plain.setCheckable(true); plain.setId(View.generateViewId());
        format.addView(markdown); format.addView(plain); format.check(markdown.getId());
        form.addView(format); form.addView(project); form.addView(developers);
        form.addView(contributors); form.addView(information);
        AlertDialog readmeDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_directly_readme_title)
                .setView(form)
                .setNegativeButton(R.string.common_word_cancel,
                        (d, w) -> restoreDirectlyReadmeChoice(mode, choices, readme, empty))
                .setPositiveButton(R.string.common_word_save, (d, w) -> {
                    mode[0] = markdown.getId() == format.getCheckedButtonId() ? "MARKDOWN" : "TEXT";
                    String separator = mode[0].equals("MARKDOWN") ? "\n\n" : "\n";
                    content[0] = (mode[0].equals("MARKDOWN") ? "# " : "") + project.getText().toString().trim()
                            + separator + getString(R.string.cooper_github_directly_readme_developers) + ": "
                            + developers.getText().toString().trim() + separator
                            + getString(R.string.cooper_github_directly_readme_contributors) + ": "
                            + contributors.getText().toString().trim() + separator
                            + getString(R.string.cooper_github_directly_readme_information) + ": "
                            + information.getText().toString().trim();
                    directlyDraftReadmeMode = mode[0];
                    directlyDraftReadmeContent = content[0];
                }).create();
        readmeDialog.setOnCancelListener(d -> restoreDirectlyReadmeChoice(mode, choices, readme, empty));
        readmeDialog.show();
    }

    private void restoreDirectlyReadmeChoice(String[] mode,
            com.google.android.material.chip.ChipGroup choices,
            com.google.android.material.chip.Chip readme,
            com.google.android.material.chip.Chip empty) {
        choices.clearCheck();
        if ("EMPTY".equals(mode[0])) empty.setChecked(true);
        else if ("MARKDOWN".equals(mode[0]) || "TEXT".equals(mode[0])) readme.setChecked(true);
    }

    private void confirmDirectlyDismiss(Runnable discard) {
        if (isFinishing() || isDestroyed()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_directly_discard_title)
                .setMessage(R.string.cooper_github_directly_discard_message)
                .setNegativeButton(R.string.cooper_github_directly_keep_editing, null)
                .setPositiveButton(R.string.cooper_github_directly_discard, (d, w) -> discard.run())
                .show();
    }

    private void beginDirectlyGitPublish(com.google.android.material.bottomsheet.BottomSheetDialog formSheet,
                                         GitProjectBindingStore.Binding binding, String name,
                                         String description, String readmeMode, String readmeContent,
                                         boolean repositoryPrivate, java.util.ArrayList<Uri> images) {
        if (isFinishing() || isDestroyed()) return;
        authExecutor.execute(() -> {
            final DirectlyGitPublisher publisher = new DirectlyGitPublisher(
                    getApplicationContext(), BuildConfig.COOPER_GITHUB_CLIENT_ID);
            final DirectlyGitPublisher.Plan plan;
            try {
                plan = publisher.prepare(binding, name, description, readmeMode, readmeContent, repositoryPrivate, images);
            } catch (Exception error) {
                String message = error.getMessage() == null
                        ? "Cooper could not prepare the publishing plan." : error.getMessage();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_directly_publish_failed_title)
                            .setMessage(getString(R.string.cooper_github_directly_publish_failed, message))
                            .setPositiveButton(R.string.common_word_ok, null)
                            .show();
                });
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isFinishing() || isDestroyed()) return;
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cooper_github_directly_publish_confirm)
                        .setMessage(plan.getSummary(this))
                        .setNegativeButton(R.string.common_word_cancel, null)
                        .setPositiveButton(R.string.cooper_github_directly_git_push,
                                (dialog, which) -> {
                                    if (formSheet.isShowing()) formSheet.dismiss();
                                    runDirectlyGitPublish(publisher, plan);
                                })
                        .show();
            });
        });
    }

    private void runDirectlyGitPublish(DirectlyGitPublisher publisher, DirectlyGitPublisher.Plan plan) {
        if (isFinishing() || isDestroyed()) return;
        directlyDraftFormVisible = false;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(12), dp(24), dp(12));
        TextView summary = text(plan.binding.workspaceName + "  →  " + plan.getOwner()
                + "/" + plan.repositoryName, 14, true);
        panel.addView(summary);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36));
        progressParams.topMargin = dp(12);
        panel.addView(progress, progressParams);
        TextView phase = text(getString(R.string.cooper_github_directly_phase_validate), 14, false);
        phase.setPadding(0, dp(8), 0, dp(8));
        panel.addView(phase);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_directly_publish_progress_title)
                .setView(panel)
                .setNegativeButton(R.string.cooper_github_directly_stop_push, null)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(ignored -> {
            android.widget.Button stop = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (stop != null) stop.setOnClickListener(v -> {
                cancelled.set(true);
                stop.setEnabled(false);
                phase.setText(R.string.cooper_github_directly_stop_requested);
            });
        });
        dialog.show();

        authExecutor.execute(() -> {
            try {
                DirectlyGitPublisher.Result result = publisher.publish(plan, cancelled, value ->
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isFinishing() && !isDestroyed() && dialog.isShowing()) phase.setText(value);
                        }));
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_directly_publish_success_title)
                            .setMessage(getString(R.string.cooper_github_directly_publish_success,
                                    result.repositoryUrl, result.branch, result.commitSha))
                            .setPositiveButton(R.string.cooper_github_open_repository, (d, which) -> {
                                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result.repositoryUrl))); }
                                catch (Exception ignored) { }
                            })
                            .setNegativeButton(R.string.common_word_ok, null)
                            .show();
                });
            } catch (Exception error) {
                boolean remoteCreated = error instanceof DirectlyGitPublisher.PublishException
                        && ((DirectlyGitPublisher.PublishException) error).remoteCreated;
                String remoteUrl = error instanceof DirectlyGitPublisher.PublishException
                        ? ((DirectlyGitPublisher.PublishException) error).repositoryUrl : "";
                String detail = error.getMessage() == null
                        ? "Publishing failed. Inspect the local and remote state before retrying." : error.getMessage();
                final String message = remoteCreated
                        ? getString(R.string.cooper_github_directly_publish_partial,
                                remoteUrl.isEmpty() ? plan.getOwner() + "/" + plan.repositoryName : remoteUrl, detail)
                        : getString(R.string.cooper_github_directly_publish_failed, detail);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    MaterialAlertDialogBuilder errorDialog = new MaterialAlertDialogBuilder(this)
                            .setTitle(remoteCreated ? R.string.cooper_github_directly_publish_partial_title
                                    : R.string.cooper_github_directly_publish_failed_title)
                            .setMessage(message)
                            .setPositiveButton(R.string.common_word_ok, (d, which) ->
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        directlyRecoveryPromptShown = false;
                                        if (DirectlyGitPublisher.hasPendingSession(this)) {
                                            showDirectlyGitRecoveryPrompt();
                                        }
                                    }));
                    if (remoteCreated && !remoteUrl.isEmpty()) {
                        errorDialog.setNeutralButton(R.string.cooper_github_open_repository, (d, which) -> {
                            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(remoteUrl))); }
                            catch (Exception ignored) { }
                        });
                    }
                    errorDialog.show();
                });
            }
        });
    }


    private void loadDirectlyGitProjectSelection() {
        authExecutor.execute(() -> {
            try {
                java.util.ArrayList<java.util.HashMap<String, Object>> projects = a.a.a.lC.a();
                java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                java.util.ArrayList<String> ids = new java.util.ArrayList<>();
                java.util.ArrayList<Bitmap> icons = new java.util.ArrayList<>();
                for (java.util.HashMap<String, Object> project : projects) {
                    String id = String.valueOf(project.get("sc_id"));
                    if (!id.matches("[0-9]{1,12}")) continue;
                    String workspace = String.valueOf(project.get("my_ws_name"));
                    String appName = String.valueOf(project.get("my_app_name"));
                    String packageName = String.valueOf(project.get("my_sc_pkg_name"));
                    labels.add(workspace + " — " + appName + "\n" + packageName + "  ·  ID " + id);
                    ids.add(id);
                    icons.add(loadDirectlyGitProjectIcon(id, project));
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (labels.isEmpty()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_project_picker_title)
                                .setMessage(R.string.cooper_github_project_picker_empty)
                                .setPositiveButton(R.string.common_word_ok, null).show();
                        return;
                    }
                    android.widget.BaseAdapter projectAdapter = new android.widget.BaseAdapter() {
                        @Override public int getCount() { return labels.size(); }
                        @Override public Object getItem(int position) { return labels.get(position); }
                        @Override public long getItemId(int position) { return position; }
                        @Override public View getView(int position, View convertView, ViewGroup parent) {
                            LinearLayout row = new LinearLayout(GithubManagerActivity.this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(Gravity.CENTER_VERTICAL);
                            row.setPadding(dp(16), dp(10), dp(16), dp(10));
                            ImageView icon = new ImageView(GithubManagerActivity.this);
                            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Bitmap bitmap = icons.get(position);
                            if (bitmap != null) icon.setImageBitmap(bitmap);
                            else icon.setImageResource(pro.sketchware.R.drawable.ic_mtrl_android);
                            icon.setContentDescription(getString(R.string.cooper_github_project_icon_content_description));
                            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
                            iconParams.rightMargin = dp(14);
                            row.addView(icon, iconParams);
                            TextView label = text(labels.get(position), 14, true);
                            label.setGravity(Gravity.CENTER_VERTICAL);
                            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                            return row;
                        }
                    };
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_directly_choose_project)
                            .setAdapter(projectAdapter, (dialog, which) -> bindProjectForDirectlyGit(ids.get(which)))
                            .setNegativeButton(R.string.common_word_cancel, null).show();
                });
            } catch (Exception e) {
                showProjectBindingError(e);
            }
        });
    }

    @Nullable
    private Bitmap loadDirectlyGitProjectIcon(String scId, java.util.HashMap<String, Object> metadata) {
        // Match the canonical project-list implementation in ProjectsAdapter:
        // custom icons live at wq.e()/scId/icon.png and are enabled by custom_icon.
        try {
            java.io.File iconFile = new java.io.File(
                    new java.io.File(a.a.a.wq.e(), scId), "icon.png");
            java.util.HashMap<String, Object> projectMetadata = metadata;
            if (projectMetadata == null) projectMetadata = a.a.a.lC.b(scId);
            boolean customIcon = projectMetadata != null
                    && a.a.a.yB.a(projectMetadata, "custom_icon");
            if (customIcon && iconFile.isFile() && iconFile.length() > 0L) {
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(iconFile.getAbsolutePath(), options);
                if (options.outWidth > 0 && options.outHeight > 0) {
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = Math.max(1, Math.min(options.outWidth, options.outHeight) / 192);
                    Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(iconFile.getAbsolutePath(), options);
                    if (bitmap != null) return bitmap;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private void bindProjectForDirectlyGit(String scId) {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).bind(scId);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Snackbar.make(content, getString(R.string.cooper_github_project_bound,
                            binding.workspaceName, binding.scId), Snackbar.LENGTH_SHORT).show();
                    showDirectlyGitBottomSheet();
                });
            } catch (Exception e) {
                showProjectBindingError(e);
            }
        });
    }

    private void startGitHubAuthorization() {
        if (new GitCredentialStore(this).hasCredential()) {
            verifyGitHubAccount();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Connect to GitHub")
                .setItems(new String[]{"OAuth — Recommended", "Personal Access Token"}, (dialog, which) -> {
                    if (which == 0) {
                        if (BuildConfig.COOPER_GITHUB_CLIENT_ID == null
                                || BuildConfig.COOPER_GITHUB_CLIENT_ID.trim().isEmpty()) {
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_connection_title)
                                    .setMessage(R.string.cooper_github_oauth_not_configured)
                                    .setPositiveButton(R.string.cooper_github_open_settings, (d, w) ->
                                            startActivity(new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("https://github.com/settings/developers"))))
                                    .setNegativeButton(R.string.common_word_cancel, null).show();
                        } else {
                            startDeviceAuthorization();
                        }
                    } else {
                        showPersonalAccessTokenDialog();
                    }
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void showPersonalAccessTokenDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(4));
        TextView guidance = text("Create a token in GitHub Settings. Use the minimum permissions required for the Cooper features you need. The token is verified before it is saved.", 14, false);
        guidance.setPadding(0, 0, 0, dp(12));
        form.addView(guidance);
        EditText tokenInput = new EditText(this);
        tokenInput.setSingleLine(true);
        tokenInput.setHint("GitHub personal access token");
        tokenInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenInput.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        form.addView(tokenInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Connect with Access Token")
                .setView(form)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton("Verify and connect", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String token = tokenInput.getText().toString().trim();
            if (token.isEmpty() || token.contains("\n") || token.contains("\r")) {
                tokenInput.setError("Enter a valid token");
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            authExecutor.execute(() -> {
                GitCredentialStore store = new GitCredentialStore(getApplicationContext());
                try {
                    store.saveTokenSet(new GitCredentialStore.TokenSet(token, null, 0L, 0L, "", "bearer"));
                    org.json.JSONObject account = apiClient().getAuthenticatedAccount();
                    String login = account.optString("login", "").trim();
                    if (login.isEmpty()) throw new IllegalStateException("GitHub did not return a valid account identity.");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        dialog.dismiss();
                        Snackbar.make(content, "Access token verified for " + login, Snackbar.LENGTH_LONG).show();
                        recreate();
                    });
                } catch (Exception e) {
                    store.clear();
                    String message = e.getMessage() == null ? "GitHub could not verify this token." : e.getMessage();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Access token verification failed")
                                .setMessage(message + "\n\nCheck that the token is active and has the permissions needed for your intended Cooper operations.")
                                .setPositiveButton(R.string.common_word_ok, null).show();
                    });
                }
            });
        }));
        dialog.show();
    }

    private void startDeviceAuthorization() {
        if (authInProgress) {
            showDeviceAuthorizationDialog();
            return;
        }
        authCancelled.set(false);
        authInProgress = true;
        authExecutor.execute(() -> {
            try {
                GitHubAuthManager manager = new GitHubAuthManager(getApplicationContext());
                GitHubAuthManager.DeviceAuthorization device =
                        manager.beginDeviceAuthorization(BuildConfig.COOPER_GITHUB_CLIENT_ID);
                pendingDeviceAuthorization = device;
                new Handler(Looper.getMainLooper()).post(this::showDeviceAuthorizationDialog);
                authPollingExecutor.execute(() -> {
                    try {
                        manager.pollForAccessToken(BuildConfig.COOPER_GITHUB_CLIENT_ID, device, authCancelled);
                        org.json.JSONObject account = apiClient().getAuthenticatedAccount();
                        String login = account.optString("login", "GitHub user");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            authInProgress = false;
                            pendingDeviceAuthorization = null;
                            if (authDialog != null && authDialog.isShowing()) authDialog.dismiss();
                            authDialog = null;
                            if (isFinishing() || isDestroyed()) return;
                            Snackbar.make(content,
                                    getString(R.string.cooper_github_account_verified, login),
                                    Snackbar.LENGTH_LONG).show();
                            recreate();
                        });
                    } catch (Exception e) {
                        if (e instanceof GitHubAuthManager.AuthException
                                && "CANCELLED".equals(((GitHubAuthManager.AuthException) e).safeCode)) return;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            authInProgress = false;
                            pendingDeviceAuthorization = null;
                            if (authDialog != null && authDialog.isShowing()) authDialog.dismiss();
                            authDialog = null;
                            showAuthError(e.getMessage() == null ? "GitHub sign-in failed." : e.getMessage());
                        });
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    authInProgress = false;
                    pendingDeviceAuthorization = null;
                    showAuthError(e.getMessage() == null ? "GitHub sign-in could not start." : e.getMessage());
                });
            }
        });
    }

    private void showDeviceAuthorizationDialog() {
        if (isFinishing() || isDestroyed() || pendingDeviceAuthorization == null) return;
        if (authDialog != null && authDialog.isShowing()) return;

        GitHubAuthManager.DeviceAuthorization device = pendingDeviceAuthorization;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(8), dp(24), dp(8));

        TextView instruction = text("Enter this one-time code on GitHub:", 16, false);
        instruction.setPadding(0, 0, 0, dp(12));
        panel.addView(instruction);

        TextView code = text(device.userCode, 24, true);
        code.setGravity(Gravity.CENTER);
        code.setLetterSpacing(0.08f);
        code.setPadding(dp(12), dp(14), dp(12), dp(14));
        code.setClickable(true);
        code.setFocusable(true);
        code.setContentDescription("Copy GitHub device code " + device.userCode);
        code.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("GitHub device code", device.userCode));
                Snackbar.make(content, "Device code copied", Snackbar.LENGTH_SHORT).show();
            }
        });
        panel.addView(code, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = text("Tap the code to copy it. Cooper will keep checking for approval while you authorize this device.", 14, false);
        hint.setPadding(0, dp(12), 0, dp(12));
        panel.addView(hint);

        LinearLayout waitingRow = new LinearLayout(this);
        waitingRow.setGravity(Gravity.CENTER_VERTICAL);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        waitingRow.addView(progress, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView waiting = text("Waiting for GitHub authorization…", 14, true);
        LinearLayout.LayoutParams waitingParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        waitingParams.leftMargin = dp(12);
        waitingRow.addView(waiting, waitingParams);
        panel.addView(waitingRow);

        authDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_device_code_title)
                .setView(panel)
                .setPositiveButton(R.string.cooper_github_open_settings, null)
                .setNegativeButton(R.string.common_word_cancel, (dialog, which) -> {
                    authCancelled.set(true);
                    authInProgress = false;
                    pendingDeviceAuthorization = null;
                    authDialog = null;
                })
                .setOnCancelListener(dialog -> {
                    authCancelled.set(true);
                    authInProgress = false;
                    pendingDeviceAuthorization = null;
                    authDialog = null;
                })
                .create();
        authDialog.setOnShowListener(dialog -> {
            android.widget.Button openButton = authDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (openButton != null) {
                openButton.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(device.verificationUri)));
                    } catch (Exception e) {
                        Snackbar.make(content, "Could not open GitHub. Visit github.com/login/device.", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
        authDialog.show();
    }

    private void loadProjectSelection() {
        authExecutor.execute(() -> {
            try {
                java.util.ArrayList<java.util.HashMap<String, Object>> projects = a.a.a.lC.a();
                java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                java.util.ArrayList<String> ids = new java.util.ArrayList<>();
                for (java.util.HashMap<String, Object> project : projects) {
                    String id = String.valueOf(project.get("sc_id"));
                    if (!id.matches("[0-9]{1,12}")) continue;
                    String workspace = String.valueOf(project.get("my_ws_name"));
                    String appName = String.valueOf(project.get("my_app_name"));
                    labels.add(workspace + " — " + appName + "  (ID " + id + ")");
                    ids.add(id);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (labels.isEmpty()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_project_picker_title)
                                .setMessage(R.string.cooper_github_project_picker_empty)
                                .setPositiveButton(R.string.common_word_ok, null).show();
                        return;
                    }
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_project_picker_title)
                            .setItems(labels.toArray(new String[0]), (dialog, which) -> bindProject(ids.get(which)))
                            .setNegativeButton(R.string.common_word_cancel, null).show();
                });
            } catch (Exception e) {
                showProjectBindingError(e);
            }
        });
    }

    private void bindProject(String scId) {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding = new GitProjectBindingStore(getApplicationContext()).bind(scId);
                new Handler(Looper.getMainLooper()).post(() ->
                        Snackbar.make(content, getString(R.string.cooper_github_project_bound,
                                binding.workspaceName, binding.scId), Snackbar.LENGTH_LONG).show());
            } catch (Exception e) {
                showProjectBindingError(e);
            }
        });
    }

    private void inspectLocalGit() {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).getBoundProject();
                if (binding == null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_local_status_title)
                                    .setMessage(R.string.cooper_github_project_not_bound)
                                    .setPositiveButton(R.string.cooper_github_select_project,
                                            (dialog, which) -> loadProjectSelection())
                                    .setNegativeButton(R.string.common_word_cancel, null).show());
                    return;
                }
                LocalGitBackend.Snapshot snapshot = new LocalGitBackend().inspect(binding.projectRoot);
                new Handler(Looper.getMainLooper()).post(() -> showLocalStatus(binding, snapshot));
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void showLocalStatus(GitProjectBindingStore.Binding binding, LocalGitBackend.Snapshot snapshot) {
        if (!snapshot.initialized) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cooper_github_local_status_title)
                    .setMessage(getString(R.string.cooper_github_local_status_detail,
                            binding.workspaceName, binding.projectRoot.getAbsolutePath(),
                            "—", "—", 0, 0, 0, 0, 0, 0))
                    .setPositiveButton(R.string.cooper_github_initialize_git, (dialog, which) ->
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_initialize_git)
                                    .setMessage(getString(R.string.cooper_github_initialize_git_confirm,
                                            binding.workspaceName))
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .setPositiveButton(R.string.cooper_github_initialize_git,
                                            (confirm, pressed) -> initializeLocalGit(binding))
                                    .show())
                    .setNegativeButton(R.string.common_word_cancel, null).show();
            return;
        }
        String detail = getString(R.string.cooper_github_local_status_detail,
                binding.workspaceName, binding.projectRoot.getAbsolutePath(),
                snapshot.currentBranch, shortCommit(snapshot.headCommit),
                snapshot.staged.size(), snapshot.modified.size(), snapshot.added.size(),
                snapshot.removed.size(), snapshot.untracked.size(), snapshot.conflicting.size());
        StringBuilder paths = new StringBuilder(detail);
        appendPaths(paths, "Staged", snapshot.staged);
        appendPaths(paths, "Modified", snapshot.modified);
        appendPaths(paths, "Added", snapshot.added);
        appendPaths(paths, "Removed", snapshot.removed);
        appendPaths(paths, "Untracked", snapshot.untracked);
        appendPaths(paths, "Conflicts", snapshot.conflicting);
        MaterialAlertDialogBuilder statusDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_local_status_title)
                .setMessage(paths.toString());
        boolean hasChanges = !snapshot.staged.isEmpty() || !snapshot.modified.isEmpty()
                || !snapshot.added.isEmpty() || !snapshot.removed.isEmpty() || !snapshot.untracked.isEmpty();
        if (!snapshot.conflicting.isEmpty()) {
            statusDialog.setPositiveButton(R.string.common_word_ok, null);
            statusDialog.setMessage(paths + "\n\n" + getString(R.string.cooper_github_conflicts_block_commit));
        } else if (hasChanges) {
            statusDialog.setPositiveButton(R.string.cooper_github_commit_local,
                    (dialog, which) -> showCommitDialog(binding, snapshot));
        } else {
            statusDialog.setPositiveButton(R.string.common_word_ok, null);
        }
        statusDialog.setNegativeButton(R.string.common_word_cancel, null).show();
    }

    private void showCommitDialog(GitProjectBindingStore.Binding binding, LocalGitBackend.Snapshot snapshot) {
        if (!snapshot.conflicting.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cooper_github_commit_review_title)
                    .setMessage(R.string.cooper_github_conflicts_block_commit)
                    .setPositiveButton(R.string.common_word_ok, null).show();
            return;
        }
        java.util.TreeSet<String> paths = new java.util.TreeSet<>();
        paths.addAll(snapshot.staged);
        paths.addAll(snapshot.modified);
        paths.addAll(snapshot.added);
        paths.addAll(snapshot.removed);
        paths.addAll(snapshot.untracked);
        if (paths.isEmpty()) {
            Snackbar.make(content, R.string.cooper_github_no_changes, Snackbar.LENGTH_LONG).show();
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        EditText message = new EditText(this);
        message.setHint(R.string.cooper_github_commit_message_hint);
        message.setSingleLine(true);
        form.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        EditText authorName = new EditText(this);
        authorName.setHint(R.string.cooper_github_author_name_hint);
        authorName.setSingleLine(true);
        form.addView(authorName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        EditText authorEmail = new EditText(this);
        authorEmail.setHint(R.string.cooper_github_author_email_hint);
        authorEmail.setSingleLine(true);
        authorEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(authorEmail, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView pathScroll = new ScrollView(this);
        LinearLayout pathList = new LinearLayout(this);
        pathList.setOrientation(LinearLayout.VERTICAL);
        java.util.ArrayList<CheckBox> checkBoxes = new java.util.ArrayList<>();
        java.util.HashSet<String> alreadyStaged = new java.util.HashSet<>();
        alreadyStaged.addAll(snapshot.staged);
        alreadyStaged.addAll(snapshot.added);
        alreadyStaged.addAll(snapshot.removed);
        for (String path : paths) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(path);
            boolean staged = alreadyStaged.contains(path);
            checkBox.setChecked(staged);
            checkBox.setEnabled(!staged);
            checkBoxes.add(checkBox);
            pathList.addView(checkBox);
        }
        pathScroll.addView(pathList);
        form.addView(pathScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.min(dp(260), dp(48) * Math.max(1, paths.size()))));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_commit_review_title)
                .setView(form)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton(R.string.cooper_github_commit_local, (dialog, which) -> {
                    String commitMessage = message.getText().toString().trim();
                    String name = authorName.getText().toString().trim();
                    String email = authorEmail.getText().toString().trim();
                    java.util.ArrayList<String> selected = new java.util.ArrayList<>();
                    int index = 0;
                    for (String path : paths) {
                        if (checkBoxes.get(index++).isChecked()) selected.add(path);
                    }
                    if (commitMessage.isEmpty() || name.isEmpty() || email.isEmpty() || selected.isEmpty()) {
                        Snackbar.make(content, R.string.cooper_github_commit_message_hint, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    String reviewedPaths = android.text.TextUtils.join("\n", selected);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_commit_review_title)
                            .setMessage(getString(R.string.cooper_github_commit_review_body, selected.size())
                                    + "\\n\\n" + reviewedPaths)
                            .setNegativeButton(R.string.common_word_cancel, null)
                            .setPositiveButton(R.string.cooper_github_commit_local,
                                    (confirm, pressed) -> performLocalCommit(binding, selected, commitMessage, name, email))
                            .show();
                }).show();
    }

    private void performLocalCommit(GitProjectBindingStore.Binding binding, java.util.List<String> paths,
                                    String message, String authorName, String authorEmail) {
        authExecutor.execute(() -> {
            GitOperationStore store = new GitOperationStore(getApplicationContext());
            String operationId = java.util.UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            GitOperationStateMachine.State state = GitOperationStateMachine.State.CREATED;
            try {
                store.upsert(new GitOperationStore.Record(operationId, "LOCAL_COMMIT", binding.scId, "",
                        "", state, now, now, "Commit plan created; no credentials or file contents recorded."));
                state = GitOperationStateMachine.transition(state, GitOperationStateMachine.State.INSPECTING);
                persistOperationState(store, operationId, binding, state, now, "Revalidating project and worktree.");
                GitProjectBindingStore.Binding current = new GitProjectBindingStore(getApplicationContext()).getBoundProject();
                if (current == null || !current.scId.equals(binding.scId)
                        || !current.projectRoot.getCanonicalPath().equals(binding.projectRoot.getCanonicalPath())) {
                    throw new IllegalStateException("The bound Cooper project changed after the commit plan was prepared.");
                }
                LocalGitBackend backend = new LocalGitBackend();
                LocalGitBackend.Snapshot currentSnapshot = backend.inspect(current.projectRoot);
                if (!currentSnapshot.initialized || !currentSnapshot.conflicting.isEmpty()) {
                    throw new IllegalStateException("The Git repository is unavailable or contains unresolved conflicts.");
                }
                state = GitOperationStateMachine.transition(state, GitOperationStateMachine.State.PLANNED);
                persistOperationState(store, operationId, binding, state, now, "Selected file plan validated.");
                state = GitOperationStateMachine.transition(state, GitOperationStateMachine.State.AWAITING_APPROVAL);
                persistOperationState(store, operationId, binding, state, now, "Waiting for the explicit confirmation recorded by the UI.");
                state = GitOperationStateMachine.transition(state, GitOperationStateMachine.State.EXECUTING);
                persistOperationState(store, operationId, binding, state, now, "Staging only the selected paths and creating a local commit.");
                LocalGitBackend.CommitOutcome outcome = backend.commitSelected(current.projectRoot, paths,
                        message, authorName, authorEmail, true);
                state = GitOperationStateMachine.transition(state, GitOperationStateMachine.State.VERIFYING);
                persistOperationState(store, operationId, binding, state, now, "Reading back the local commit.");
                LocalGitBackend.Snapshot verified = backend.inspect(current.projectRoot);
                if (!verified.initialized || !outcome.commitId.equals(verified.headCommit)) {
                    state = GitOperationStateMachine.State.RECOVERY_REQUIRED;
                    persistOperationState(store, operationId, binding, state, now, "Commit result could not be verified; inspect repository before retrying.");
                    throw new IllegalStateException("Commit was created but read-back verification failed. Do not retry blindly.");
                }
                state = GitOperationStateMachine.transition(state, GitOperationStateMachine.State.SUCCEEDED);
                persistOperationState(store, operationId, binding, state, now, "Verified local commit " + outcome.commitId);
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_commit_local)
                                .setMessage(getString(R.string.cooper_github_commit_success, shortCommit(outcome.commitId))
                                        + "\\n\\nRemote sync has not been performed.")
                                .setPositiveButton(R.string.common_word_ok, null).show());
            } catch (Exception e) {
                try {
                    if (state == GitOperationStateMachine.State.EXECUTING || state == GitOperationStateMachine.State.VERIFYING) {
                        state = GitOperationStateMachine.State.RECOVERY_REQUIRED;
                        persistOperationState(store, operationId, binding, state, now,
                                "Commit attempt ended without a verified success; inspect local Git state before retrying.");
                    } else if (state != GitOperationStateMachine.State.SUCCEEDED) {
                        state = GitOperationStateMachine.State.FAILED;
                        persistOperationState(store, operationId, binding, state, now, "Commit operation failed before verified success.");
                    }
                } catch (Exception ignored) { }
                String safeMessage = e.getMessage() == null ? "Local commit failed." : e.getMessage();
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_commit_local)
                                .setMessage(getString(R.string.cooper_github_commit_failed, safeMessage))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            }
        });
    }

    private void persistOperationState(GitOperationStore store, String id,
                                       GitProjectBindingStore.Binding binding,
                                       GitOperationStateMachine.State state, long createdAt,
                                       String diagnostic) throws java.io.IOException {
        store.upsert(new GitOperationStore.Record(id, "LOCAL_COMMIT", binding.scId, "",
                "", state, createdAt, System.currentTimeMillis(), diagnostic));
    }

    private void initializeLocalGit(GitProjectBindingStore.Binding binding) {
        authExecutor.execute(() -> {
            try {
                new LocalGitBackend().initialize(binding.projectRoot, true);
                LocalGitBackend.Snapshot verified = new LocalGitBackend().inspect(binding.projectRoot);
                if (!verified.initialized) throw new IllegalStateException("Git initialization read-back failed.");
                if (!binding.remoteUrl().isEmpty()) {
                    new LocalGitBackend().configureRemote(binding.projectRoot, binding.remoteUrl(), true);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    Snackbar.make(content, R.string.cooper_github_initialize_git_success, Snackbar.LENGTH_LONG).show();
                    showLocalStatus(binding, verified);
                });
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 7314) return;
        TextView stateView = directlyPendingImageState;
        java.util.ArrayList<Uri> current = directlyPendingImages;
        if (resultCode != RESULT_OK || data == null || current == null) return;
        java.util.ArrayList<Uri> picked = new java.util.ArrayList<>();
        android.content.ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null && !picked.contains(uri)) picked.add(uri);
            }
        } else if (data.getData() != null) {
            picked.add(data.getData());
        }
        java.util.ArrayList<Uri> combined = new java.util.ArrayList<>(current);
        for (Uri uri : picked) if (!combined.contains(uri)) combined.add(uri);
        if (combined.size() > 6) {
            Snackbar.make(content, R.string.cooper_github_directly_image_limit, Snackbar.LENGTH_LONG).show();
            return;
        }
        for (Uri uri : picked) {
            try {
                String type = getContentResolver().getType(uri);
                if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
                    Snackbar.make(content, R.string.cooper_github_directly_invalid_image, Snackbar.LENGTH_LONG).show();
                    return;
                }
                try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null || input.read() < 0) {
                        Snackbar.make(content, R.string.cooper_github_directly_unreadable_image, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                }
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) { }
            } catch (Exception e) {
                Snackbar.make(content, R.string.cooper_github_directly_unreadable_image, Snackbar.LENGTH_LONG).show();
                return;
            }
        }
        current.clear();
        current.addAll(combined);
        if (stateView != null) {
            stateView.setText(current.isEmpty()
                    ? getString(R.string.cooper_github_directly_no_images)
                    : getString(R.string.cooper_github_directly_images_selected, current.size()));
        }
        if (directlyPendingImagePreview != null) {
            renderDirectlyGitImagePreviews(current, directlyPendingImagePreview);
        }
    }

    private void renderDirectlyGitImagePreviews(java.util.ArrayList<Uri> images, @Nullable LinearLayout row) {
        if (row == null) return;
        row.removeAllViews();
        for (int index = 0; index < images.size(); index++) {
            final Uri uri = images.get(index);
            final int selectedIndex = index;
            com.google.android.material.card.MaterialCardView card =
                    new com.google.android.material.card.MaterialCardView(this);
            card.setRadius(dp(12));
            card.setCardElevation(dp(2));
            card.setStrokeWidth(dp(1));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(78), dp(92));
            cardParams.rightMargin = dp(8);
            FrameLayout frame = new FrameLayout(this);
            ImageView preview = new ImageView(this);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setContentDescription(getString(R.string.cooper_github_directly_image_preview, index + 1));
            preview.setTag(uri.toString());
            frame.addView(preview, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(78)));
            TextView badge = text(String.valueOf(index + 1), 12, true);
            badge.setGravity(Gravity.CENTER);
            badge.setTextColor(android.graphics.Color.WHITE);
            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setShape(GradientDrawable.OVAL);
            badgeBackground.setColor(com.google.android.material.color.MaterialColors.getColor(
                    this, androidx.appcompat.R.attr.colorPrimary, 0xFF365D4A));
            badge.setBackground(badgeBackground);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP | Gravity.START);
            frame.addView(badge, badgeParams);
            card.addView(frame);
            card.setContentDescription(getString(R.string.cooper_github_directly_remove_image, index + 1));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> {
                if (selectedIndex < images.size() && uri.equals(images.get(selectedIndex))) {
                    images.remove(selectedIndex);
                } else {
                    images.remove(uri);
                }
                if (directlyPendingImageState != null) {
                    directlyPendingImageState.setText(images.isEmpty()
                            ? getString(R.string.cooper_github_directly_no_images)
                            : getString(R.string.cooper_github_directly_images_selected, images.size()));
                }
                renderDirectlyGitImagePreviews(images, row);
            });
            row.addView(card, cardParams);
            authExecutor.execute(() -> {
                android.graphics.Bitmap bitmap = decodeDirectlyGitThumbnail(uri);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (bitmap != null && !isFinishing() && !isDestroyed()
                            && uri.toString().equals(preview.getTag())) preview.setImageBitmap(bitmap);
                });
            });
        }
    }

    @Nullable
    private android.graphics.Bitmap decodeDirectlyGitThumbnail(Uri uri) {
        try {
            android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                android.graphics.BitmapFactory.decodeStream(input, null, bounds);
            }
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (largest / sample > 160) sample *= 2;
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = Math.max(1, sample);
            options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
            try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                return input == null ? null : android.graphics.BitmapFactory.decodeStream(input, null, options);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private void showProjectBindingError(Exception error) {
        String message = error.getMessage() == null ? "Project binding failed." : error.getMessage();
        new Handler(Looper.getMainLooper()).post(() ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cooper_github_project_picker_title)
                        .setMessage(getString(R.string.cooper_github_project_binding_failed, message))
                        .setPositiveButton(R.string.common_word_ok, null).show());
    }

    private boolean handleReauthenticationFailure(Exception error) {
        boolean needsSignIn = false;
        if (error instanceof GitHubAuthManager.AuthException) {
            String code = ((GitHubAuthManager.AuthException) error).safeCode;
            needsSignIn = "NOT_AUTHENTICATED".equals(code)
                    || "REAUTH_REQUIRED".equals(code)
                    || "REFRESH_TOKEN_INVALID".equals(code)
                    || "REFRESH_TOKEN_MISSING".equals(code)
                    || "CREDENTIAL_STORE_UNAVAILABLE".equals(code);
        } else if (error instanceof GitHubApiClient.ApiException) {
            GitHubApiClient.ApiException apiError = (GitHubApiClient.ApiException) error;
            needsSignIn = apiError.httpStatus == 401
                    || "REAUTH_REQUIRED".equals(apiError.safeCode)
                    || "NOT_AUTHENTICATED".equals(apiError.safeCode);
        }
        if (!needsSignIn) return false;

        new Handler(Looper.getMainLooper()).post(() -> {
            if (isFinishing() || isDestroyed()) return;
            String message = error.getMessage() == null
                    ? "The GitHub session needs to be renewed." : error.getMessage();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cooper_github_connection_title)
                    .setMessage(message)
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .setPositiveButton(R.string.cooper_github_reconnect,
                            (dialog, which) -> startDeviceAuthorization())
                    .show();
        });
        return true;
    }

    private void showLocalGitError(Exception error) {
        if (handleReauthenticationFailure(error)) return;
        String message = error.getMessage() == null ? "Local Git operation failed." : error.getMessage();
        new Handler(Looper.getMainLooper()).post(() ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cooper_github_local_status_title)
                        .setMessage(getString(R.string.cooper_github_local_git_failed, message))
                        .setPositiveButton(R.string.common_word_ok, null).show());
    }

    private static String shortCommit(String commit) {
        if (commit == null || commit.isEmpty()) return "—";
        return commit.length() > 12 ? commit.substring(0, 12) : commit;
    }

    private static void appendPaths(StringBuilder builder, String label, java.util.List<String> paths) {
        if (paths.isEmpty()) return;
        builder.append("\n\n").append(label).append(":");
        for (String path : paths) builder.append("\n• ").append(path);
    }

    private GitHubApiClient apiClient() {
        return new GitHubApiClient(
                new GitHubAuthManager(getApplicationContext()),
                BuildConfig.COOPER_GITHUB_CLIENT_ID);
    }

    private void verifyGitHubAccount() {
        authExecutor.execute(() -> {
            try {
                org.json.JSONObject account = apiClient().getAuthenticatedAccount();
                String login = account.optString("login", "GitHub user");
                new Handler(Looper.getMainLooper()).post(() ->
                        Snackbar.make(content, getString(R.string.cooper_github_account_verified, login),
                                Snackbar.LENGTH_LONG).show());
            } catch (Exception e) {
                if (handleReauthenticationFailure(e)) return;
                String message = e.getMessage() == null ? "Account verification failed." : e.getMessage();
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_connection_title)
                                .setMessage(getString(R.string.cooper_github_account_verify_failed, message))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            }
        });
    }

    private void loadRepositories() {
        if (!new GitCredentialStore(this).hasCredential()) {
            startGitHubAuthorization();
            return;
        }
        authExecutor.execute(() -> {
            try {
                org.json.JSONArray repositories = apiClient().listRepositories(1, 50);
                java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                java.util.ArrayList<String> urls = new java.util.ArrayList<>();
                java.util.ArrayList<String> fullNames = new java.util.ArrayList<>();
                for (int i = 0; i < repositories.length(); i++) {
                    org.json.JSONObject repository = repositories.getJSONObject(i);
                    String fullName = repository.optString("full_name", "");
                    if (fullName.isEmpty()) continue;
                    labels.add(fullName + (repository.optBoolean("private") ? "  •  Private" : "  •  Public"));
                    urls.add(repository.optString("html_url", ""));
                    fullNames.add(fullName);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (labels.isEmpty()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_repository_list_title)
                                .setMessage(R.string.cooper_github_repository_list_empty)
                                .setPositiveButton(R.string.common_word_ok, null).show();
                        return;
                    }
                    String[] items = labels.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_repository_list_title)
                            .setItems(items, (dialog, which) ->
                                    showRepositoryActions(fullNames.get(which), urls.get(which)))
                            .setNegativeButton(R.string.common_word_cancel, null).show();
                });
            } catch (Exception e) {
                if (handleReauthenticationFailure(e)) return;
                String message = e.getMessage() == null ? "Repository request failed." : e.getMessage();
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_repository_list_title)
                                .setMessage(getString(R.string.cooper_github_repository_list_failed, message))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            }
        });
    }

    private void addGitHubProfileCard() {
        final android.content.SharedPreferences profileCache = getSharedPreferences("cooper_github_profile", MODE_PRIVATE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(20), dp(20), dp(20), dp(18));

        ImageView avatar = new ImageView(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF526C63);
        avatar.setBackground(circle);
        avatar.setClipToOutline(true);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(108), dp(108));
        imageParams.bottomMargin = dp(12);
        panel.addView(avatar, imageParams);

        TextView accountName = text(profileCache.getString("name", "Loading GitHub profile…"), 21, true);
        accountName.setGravity(Gravity.CENTER);
        panel.addView(accountName);
        TextView accountBio = text(profileCache.getString("bio", "Connecting to GitHub to verify the saved profile…"), 14, false);
        accountBio.setGravity(Gravity.CENTER);
        accountBio.setPadding(0, dp(8), 0, dp(16));
        panel.addView(accountBio);
        panel.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        profileParams.bottomMargin = dp(12);
        content.addView(panel, 1, profileParams);

        String cachedAvatar = profileCache.getString("avatar_png", "");
        if (!cachedAvatar.isEmpty()) {
            try {
                byte[] bytes = android.util.Base64.decode(cachedAvatar, android.util.Base64.DEFAULT);
                Bitmap cachedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (cachedBitmap != null) avatar.setImageBitmap(cachedBitmap);
            } catch (Exception ignored) { }
        }
        avatar.setScaleX(0.88f);
        avatar.setScaleY(0.88f);
        avatar.animate().scaleX(1f).scaleY(1f).setDuration(450).start();
        avatar.setContentDescription("Cached GitHub profile picture");
        addActionCard("Disconnect GitHub", "Sign out of this account from Cooper.", v -> confirmDisconnectGitHub());

        // Cached profile is displayed immediately. A live /user request refreshes it only
        // when successful; network failures never replace real saved data with placeholders.
        authExecutor.execute(() -> {
            try {
                org.json.JSONObject user = apiClient().getAuthenticatedAccount();
                String login = user.optString("login", "").trim();
                if (login.isEmpty()) throw new IllegalStateException("GitHub did not return an account login.");
                String name = user.optString("name", "").trim();
                String displayName = name.isEmpty() ? login : name;
                String bio = user.optString("bio", "").trim();
                String avatarUrl = user.optString("avatar_url", "").trim();

                Bitmap bitmap = null;
                if (avatarUrl.startsWith("https://")) {
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(avatarUrl).openConnection();
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    try (java.io.InputStream stream = connection.getInputStream()) {
                        bitmap = BitmapFactory.decodeStream(stream);
                    } finally {
                        connection.disconnect();
                    }
                }

                android.content.SharedPreferences.Editor editor = profileCache.edit()
                        .putString("login", login)
                        .putString("name", displayName)
                        .putString("bio", bio)
                        .putString("avatar_url", avatarUrl)
                        .putLong("verified_at", System.currentTimeMillis());
                String avatarBase64 = "";
                if (bitmap != null) {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                    avatarBase64 = android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP);
                    editor.putString("avatar_png", avatarBase64);
                }
                editor.commit();

                final Bitmap profileBitmap = bitmap;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    accountName.setText(displayName);
                    accountBio.setText(bio);
                    avatar.setContentDescription("GitHub profile picture for " + login);
                    if (profileBitmap != null) avatar.setImageBitmap(profileBitmap);
                });
            } catch (Exception error) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    // Preserve cached account data; only explain the offline state if no cache exists.
                    if (!profileCache.contains("login")) {
                        accountName.setText("GitHub profile unavailable");
                        accountBio.setText("Connect to the internet to load and save your real GitHub profile.");
                    } else {
                        accountBio.setText(profileCache.getString("bio", ""));
                        Snackbar.make(content, "Showing the saved GitHub profile. Connect to the internet to verify it.", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void confirmDisconnectGitHub() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_disconnect)
                .setMessage(R.string.cooper_github_disconnect_confirm)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton(R.string.cooper_github_disconnect, (dialog, which) -> {
                    new GitCredentialStore(getApplicationContext()).clear();
                    // Clear the cached profile on logout so another account is never shown stale details.
                    getSharedPreferences("cooper_github_profile", MODE_PRIVATE).edit().clear().commit();
                    Snackbar.make(content, R.string.cooper_github_disconnected, Snackbar.LENGTH_LONG).show();
                    recreate();
                }).show();
    }

    private void showCreatePullRequestDialog() {
        if (!new GitCredentialStore(this).hasCredential()) {
            startGitHubAuthorization();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(4));
        EditText owner = field(form, R.string.cooper_github_pr_owner_hint, true);
        EditText repository = field(form, R.string.cooper_github_pr_repository_hint, true);
        EditText head = field(form, R.string.cooper_github_pr_head_hint, true);
        EditText base = field(form, R.string.cooper_github_pr_base_hint, true);
        EditText title = field(form, R.string.cooper_github_pr_title_hint, true);
        EditText body = field(form, R.string.cooper_github_pr_body_hint, false);
        body.setMinLines(2);
        body.setMaxLines(4);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_create_pull_request)
                .setView(form)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton(R.string.cooper_github_create_pull_request, (dialog, which) -> {
                    String repoOwner = owner.getText().toString().trim();
                    String repoName = repository.getText().toString().trim();
                    String source = head.getText().toString().trim();
                    String target = base.getText().toString().trim();
                    String prTitle = title.getText().toString().trim();
                    String prBody = body.getText().toString();
                    if (repoOwner.isEmpty() || repoName.isEmpty() || source.isEmpty()
                            || target.isEmpty() || prTitle.isEmpty() || source.equals(target)) {
                        Snackbar.make(content, R.string.cooper_github_create_pull_request, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_create_pull_request)
                            .setMessage(getString(R.string.cooper_github_pr_review, source, target, repoOwner, repoName)
                                    + "\n\n" + prTitle + "\n\n" + prBody)
                            .setNegativeButton(R.string.common_word_cancel, null)
                            .setPositiveButton(R.string.cooper_github_create_pull_request,
                                    (confirm, pressed) -> createPullRequest(repoOwner, repoName, prTitle, prBody, source, target))
                            .show();
                }).show();
    }

    private EditText field(LinearLayout form, int hint, boolean singleLine) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setSingleLine(singleLine);
        form.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }

    private void createPullRequest(String owner, String repository, String title,
                                   String body, String head, String base) {
        authExecutor.execute(() -> {
            try {
                org.json.JSONObject created = apiClient().createPullRequest(
                        owner, repository, title, body, head, base, true);
                int number = created.optInt("number", 0);
                if (number < 1) throw new IllegalStateException("GitHub did not return a valid Pull Request number.");
                org.json.JSONObject verified = apiClient().getPullRequest(owner, repository, number);
                if (verified.optInt("number", 0) != number
                        || !head.equals(verified.optJSONObject("head") == null ? "" :
                        verified.optJSONObject("head").optString("ref", ""))
                        || !base.equals(verified.optJSONObject("base") == null ? "" :
                        verified.optJSONObject("base").optString("ref", ""))) {
                    throw new IllegalStateException("Pull Request read-back did not match the requested branches.");
                }
                String url = verified.optString("html_url", "");
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_create_pull_request)
                                .setMessage(getString(R.string.cooper_github_pr_created, number) + "\n" + url)
                                .setPositiveButton(R.string.cooper_github_open_settings, (dialog, which) -> {
                                    if (url.startsWith("https://github.com/")) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                    }
                                })
                                .setNegativeButton(R.string.common_word_ok, null).show());
            } catch (Exception e) {
                if (handleReauthenticationFailure(e)) return;
                String message = e.getMessage() == null ? "Pull Request operation failed." : e.getMessage();
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_create_pull_request)
                                .setMessage(getString(R.string.cooper_github_pr_failed, message))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            }
        });
    }

    private void reviewFileDiff() {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).getBoundProject();
                if (binding == null) throw new IllegalStateException("Select a Cooper project first.");
                LocalGitBackend.Snapshot snapshot = new LocalGitBackend().inspect(binding.projectRoot);
                java.util.TreeSet<String> paths = new java.util.TreeSet<>();
                paths.addAll(snapshot.staged);
                paths.addAll(snapshot.modified);
                paths.addAll(snapshot.added);
                paths.addAll(snapshot.removed);
                paths.addAll(snapshot.untracked);
                paths.addAll(snapshot.conflicting);
                paths.removeIf(GitSafetyGuard::isExcludedByDefault);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (paths.isEmpty()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_review_diff)
                                .setMessage(R.string.cooper_github_diff_empty)
                                .setPositiveButton(R.string.common_word_ok, null).show();
                        return;
                    }
                    String[] items = paths.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_review_diff)
                            .setItems(items, (dialog, which) -> showFileDiff(binding, items[which]))
                            .setNegativeButton(R.string.common_word_cancel, null).show();
                });
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void showFileDiff(GitProjectBindingStore.Binding binding, String path) {
        authExecutor.execute(() -> {
            try {
                GitDiffEngine.DiffResult result = new GitDiffEngine().diff(binding.projectRoot, path);
                new Handler(Looper.getMainLooper()).post(() -> {
                    android.widget.TextView text = new android.widget.TextView(this);
                    text.setText(result.text);
                    text.setTextIsSelectable(true);
                    text.setTextSize(12);
                    text.setTypeface(android.graphics.Typeface.MONOSPACE);
                    text.setPadding(dp(16), dp(12), dp(16), dp(12));
                    ScrollView scroll = new ScrollView(this);
                    scroll.addView(text);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.cooper_github_diff_title, path))
                            .setView(scroll)
                            .setPositiveButton(R.string.common_word_ok, null).show();
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Diff calculation failed." : e.getMessage();
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_review_diff)
                                .setMessage(getString(R.string.cooper_github_diff_failed, message))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            }
        });
    }

    private void inspectSyncState() {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).getBoundProject();
                if (binding == null) throw new IllegalStateException("Select a Cooper project first.");
                GitSyncEngine.Snapshot snapshot = new GitSyncEngine().inspect(binding.projectRoot);
                String detail = getString(R.string.cooper_github_sync_state_detail,
                        snapshot.state.name(), snapshot.branch, shortCommit(snapshot.localHead),
                        shortCommit(snapshot.remoteHead), snapshot.aheadBy, snapshot.behindBy,
                        snapshot.changedFileCount);
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_sync_state)
                                .setMessage(detail)
                                .setPositiveButton(R.string.common_word_ok, null).show());
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void showLocalBranches() {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).getBoundProject();
                if (binding == null) throw new IllegalStateException("Select a Cooper project first.");
                LocalGitBackend backend = new LocalGitBackend();
                LocalGitBackend.Snapshot snapshot = backend.inspect(binding.projectRoot);
                if (!snapshot.initialized) throw new IllegalStateException("Initialize local Git first.");
                java.util.List<String> branches = backend.listBranches(binding.projectRoot);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (branches.isEmpty()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_local_branches)
                                .setMessage("No local branches are available yet.")
                                .setPositiveButton(R.string.cooper_github_create_branch,
                                        (dialog, which) -> showCreateBranchDialog(binding))
                                .setNegativeButton(R.string.common_word_cancel, null).show();
                        return;
                    }
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_local_branches)
                            .setItems(branches.toArray(new String[0]), (dialog, which) -> {
                                String target = branches.get(which);
                                if (target.equals(snapshot.currentBranch)) {
                                    Snackbar.make(content, target, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    new MaterialAlertDialogBuilder(this)
                                            .setTitle(R.string.cooper_github_checkout_branch)
                                            .setMessage(getString(R.string.cooper_github_checkout_confirm,
                                                    snapshot.currentBranch, target))
                                            .setNegativeButton(R.string.common_word_cancel, null)
                                            .setPositiveButton(R.string.cooper_github_checkout_branch,
                                                    (confirm, pressed) -> checkoutLocalBranch(binding, target))
                                            .show();
                                }
                            })
                            .setNeutralButton(R.string.cooper_github_create_branch,
                                    (dialog, which) -> showCreateBranchDialog(binding))
                            .setNegativeButton(R.string.common_word_cancel, null).show();
                });
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void showCreateBranchDialog(GitProjectBindingStore.Binding binding) {
        EditText branchName = new EditText(this);
        branchName.setHint(R.string.cooper_github_branch_name_hint);
        branchName.setSingleLine(true);
        branchName.setPadding(dp(24), dp(8), dp(24), dp(8));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_create_branch)
                .setView(branchName)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton(R.string.cooper_github_create_branch, (dialog, which) -> {
                    String name = branchName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Snackbar.make(content, R.string.cooper_github_branch_name_hint, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    authExecutor.execute(() -> {
                        try {
                            LocalGitBackend backend = new LocalGitBackend();
                            backend.createBranch(binding.projectRoot, name, true);
                            if (!backend.listBranches(binding.projectRoot).contains(name)) {
                                throw new IllegalStateException("New branch was not found during read-back verification.");
                            }
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Snackbar.make(content, getString(R.string.cooper_github_branch_created, name),
                                            Snackbar.LENGTH_LONG).show());
                        } catch (Exception e) {
                            showBranchError(e);
                        }
                    });
                }).show();
    }

    private void checkoutLocalBranch(GitProjectBindingStore.Binding binding, String target) {
        authExecutor.execute(() -> {
            try {
                LocalGitBackend backend = new LocalGitBackend();
                backend.checkout(binding.projectRoot, target, true);
                LocalGitBackend.Snapshot verified = backend.inspect(binding.projectRoot);
                if (!target.equals(verified.currentBranch)) {
                    throw new IllegalStateException("The active branch did not match the requested branch after checkout.");
                }
                new Handler(Looper.getMainLooper()).post(() ->
                        Snackbar.make(content, getString(R.string.cooper_github_branch_switched, target),
                                Snackbar.LENGTH_LONG).show());
            } catch (Exception e) {
                showBranchError(e);
            }
        });
    }

    private void showBranchError(Exception error) {
        String message = error.getMessage() == null ? "Branch operation failed." : error.getMessage();
        new Handler(Looper.getMainLooper()).post(() ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cooper_github_local_branches)
                        .setMessage(getString(R.string.cooper_github_branch_failed, message))
                        .setPositiveButton(R.string.common_word_ok, null).show());
    }

    private void showRepositoryActions(String fullName, String url) {
        String[] options = new String[] {
                getString(R.string.cooper_github_open_settings),
                getString(R.string.cooper_github_link_remote)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(fullName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (url.startsWith("https://github.com/")) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        }
                    } else {
                        String[] parts = fullName.split("/", 2);
                        if (parts.length == 2) linkRemoteToBoundProject(parts[0], parts[1]);
                    }
                })
                .setNegativeButton(R.string.common_word_cancel, null).show();
    }

    private void linkRemoteToBoundProject(String owner, String repository) {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore store = new GitProjectBindingStore(getApplicationContext());
                GitProjectBindingStore.Binding binding = store.getBoundProject();
                if (binding == null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_link_remote)
                                    .setMessage(R.string.cooper_github_project_not_bound)
                                    .setPositiveButton(R.string.cooper_github_select_project,
                                            (dialog, which) -> loadProjectSelection())
                                    .setNegativeButton(R.string.common_word_cancel, null).show());
                    return;
                }
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_link_remote)
                                .setMessage(getString(R.string.cooper_github_link_remote_confirm,
                                        owner, repository, binding.workspaceName))
                                .setNegativeButton(R.string.common_word_cancel, null)
                                .setPositiveButton(R.string.cooper_github_link_remote,
                                        (dialog, which) -> persistRemoteBinding(owner, repository, binding))
                                .show());
            } catch (Exception e) {
                showProjectBindingError(e);
            }
        });
    }

    private void persistRemoteBinding(String owner, String repository, GitProjectBindingStore.Binding binding) {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore store = new GitProjectBindingStore(getApplicationContext());
                store.linkRemote(owner, repository);
                LocalGitBackend backend = new LocalGitBackend();
                LocalGitBackend.Snapshot snapshot = backend.inspect(binding.projectRoot);
                if (snapshot.initialized) {
                    backend.configureRemote(binding.projectRoot,
                            "https://github.com/" + owner + "/" + repository + ".git", true);
                }
                new Handler(Looper.getMainLooper()).post(() ->
                        Snackbar.make(content, getString(R.string.cooper_github_remote_linked,
                                owner + "/" + repository, binding.workspaceName), Snackbar.LENGTH_LONG).show());
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void runRemoteGitOperation(String operation) {
        authExecutor.execute(() -> {
            try {
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).getBoundProject();
                if (binding == null) throw new IllegalStateException("Select a Cooper project first.");
                if (binding.remoteUrl().isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_link_remote)
                                    .setMessage(R.string.cooper_github_remote_not_linked)
                                    .setPositiveButton(R.string.cooper_github_browse_repositories,
                                            (dialog, which) -> loadRepositories())
                                    .setNegativeButton(R.string.common_word_cancel, null).show());
                    return;
                }
                LocalGitBackend backend = new LocalGitBackend();
                LocalGitBackend.Snapshot snapshot = backend.inspect(binding.projectRoot);
                if (!snapshot.initialized) throw new IllegalStateException("Initialize local Git before remote synchronization.");
                if (operation.equals("FETCH")) {
                    String token = new GitHubAuthManager(getApplicationContext())
                            .getUsableAccessToken(BuildConfig.COOPER_GITHUB_CLIENT_ID);
                    backend.configureRemote(binding.projectRoot, binding.remoteUrl(), true);
                    backend.fetch(binding.projectRoot, token);
                    new Handler(Looper.getMainLooper()).post(() ->
                            Snackbar.make(content, R.string.cooper_github_fetch_success, Snackbar.LENGTH_LONG).show());
                    return;
                }
                if (operation.equals("PUSH")) {
                    if (!snapshot.conflicting.isEmpty() || !snapshot.modified.isEmpty() || !snapshot.added.isEmpty()
                            || !snapshot.removed.isEmpty() || !snapshot.untracked.isEmpty() || !snapshot.staged.isEmpty()) {
                        throw new IllegalStateException("Push is blocked until all local changes are reviewed and committed.");
                    }
                    new Handler(Looper.getMainLooper()).post(() ->
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_push_remote)
                                    .setMessage(getString(R.string.cooper_github_push_confirm,
                                            snapshot.currentBranch, shortCommit(snapshot.headCommit),
                                            binding.remoteOwner + "/" + binding.remoteRepository))
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .setPositiveButton(R.string.cooper_github_push_remote,
                                            (dialog, which) -> performRemotePush(binding, snapshot))
                                    .show());
                    return;
                }
                if (operation.equals("PULL")) {
                    if (!snapshot.conflicting.isEmpty() || !snapshot.modified.isEmpty() || !snapshot.added.isEmpty()
                            || !snapshot.removed.isEmpty() || !snapshot.untracked.isEmpty() || !snapshot.staged.isEmpty()) {
                        throw new IllegalStateException("Pull is blocked until all local changes are reviewed and committed.");
                    }
                    new Handler(Looper.getMainLooper()).post(() ->
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.cooper_github_pull_remote)
                                    .setMessage(getString(R.string.cooper_github_pull_confirm,
                                            snapshot.currentBranch, binding.remoteOwner + "/" + binding.remoteRepository))
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .setPositiveButton(R.string.cooper_github_pull_remote,
                                            (dialog, which) -> performRemotePull(binding, snapshot))
                                    .show());
                }
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void performRemotePush(GitProjectBindingStore.Binding binding,
                                   LocalGitBackend.Snapshot planned) {
        authExecutor.execute(() -> {
            try {
                LocalGitBackend backend = new LocalGitBackend();
                backend.configureRemote(binding.projectRoot, binding.remoteUrl(), true);
                String token = new GitHubAuthManager(getApplicationContext())
                        .getUsableAccessToken(BuildConfig.COOPER_GITHUB_CLIENT_ID);
                LocalGitBackend.Snapshot current = backend.inspect(binding.projectRoot);
                if (!current.initialized
                        || !java.util.Objects.equals(current.headCommit, planned.headCommit)
                        || !java.util.Objects.equals(current.currentBranch, planned.currentBranch)
                        || !current.conflicting.isEmpty() || !current.modified.isEmpty()
                        || !current.added.isEmpty() || !current.removed.isEmpty()
                        || !current.untracked.isEmpty() || !current.staged.isEmpty()) {
                    throw new IllegalStateException("The project changed after Push approval. Inspect and approve a new plan.");
                }
                String expected = backend.push(binding.projectRoot, token, true);
                org.json.JSONObject remoteBranch = apiClient().getBranch(
                        binding.remoteOwner, binding.remoteRepository, planned.currentBranch);
                org.json.JSONObject commit = remoteBranch.optJSONObject("commit");
                String remoteSha = commit == null ? "" : commit.optString("sha", "");
                if (!expected.equals(remoteSha)) {
                    throw new IllegalStateException("Push result is not yet verified on GitHub. Check the remote branch before retrying.");
                }
                org.json.JSONObject remoteRepository = apiClient().getRepository(
                        binding.remoteOwner, binding.remoteRepository);
                boolean historySaved = DirectlyGitPublisher.recordVerifiedPublication(
                        getApplicationContext(), binding, binding.remoteOwner, binding.remoteRepository,
                        remoteRepository.optString("html_url", "https://github.com/" + binding.remoteOwner + "/" + binding.remoteRepository),
                        expected, planned.currentBranch, remoteRepository.optBoolean("private", true), "ADVANCED_GIT");
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_push_remote)
                                .setMessage(getString(R.string.cooper_github_push_success, shortCommit(expected))
                                        + "\n\n" + getString(historySaved ? R.string.cooper_github_history_saved
                                        : R.string.cooper_github_history_save_failed))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void performRemotePull(GitProjectBindingStore.Binding binding,
                                   LocalGitBackend.Snapshot planned) {
        authExecutor.execute(() -> {
            try {
                LocalGitBackend backend = new LocalGitBackend();
                backend.configureRemote(binding.projectRoot, binding.remoteUrl(), true);
                String token = new GitHubAuthManager(getApplicationContext())
                        .getUsableAccessToken(BuildConfig.COOPER_GITHUB_CLIENT_ID);
                LocalGitBackend.Snapshot current = backend.inspect(binding.projectRoot);
                if (!current.initialized
                        || !java.util.Objects.equals(current.headCommit, planned.headCommit)
                        || !java.util.Objects.equals(current.currentBranch, planned.currentBranch)
                        || !current.conflicting.isEmpty() || !current.modified.isEmpty()
                        || !current.added.isEmpty() || !current.removed.isEmpty()
                        || !current.untracked.isEmpty() || !current.staged.isEmpty()) {
                    throw new IllegalStateException("The project changed after Pull approval. Inspect and approve a new plan.");
                }
                String expected = backend.pull(binding.projectRoot, token, true);
                LocalGitBackend.Snapshot verified = backend.inspect(binding.projectRoot);
                if (!expected.equals(verified.headCommit)) {
                    throw new IllegalStateException("Pull result could not be verified against local HEAD.");
                }
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_pull_remote)
                                .setMessage(getString(R.string.cooper_github_pull_success, shortCommit(expected)))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            } catch (Exception e) {
                showLocalGitError(e);
            }
        });
    }

    private void showCreateRepositoryDialog() {
        if (!new GitCredentialStore(this).hasCredential()) {
            startGitHubAuthorization();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(4));
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint(R.string.cooper_github_repository_name_hint);
        form.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        EditText description = new EditText(this);
        description.setHint(R.string.cooper_github_repository_description_hint);
        description.setMinLines(1);
        description.setMaxLines(3);
        form.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        CheckBox privateRepository = new CheckBox(this);
        privateRepository.setText(R.string.cooper_github_repository_private);
        form.addView(privateRepository);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_repository_create_title)
                .setView(form)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton(R.string.cooper_github_create_repository, (dialog, which) -> {
                    String repositoryName = name.getText().toString().trim();
                    String repositoryDescription = description.getText().toString().trim();
                    boolean isPrivate = privateRepository.isChecked();
                    if (repositoryName.isEmpty()) {
                        Snackbar.make(content, R.string.cooper_github_repository_name_hint, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    String visibility = isPrivate ? "private" : "public";
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_repository_create_title)
                            .setMessage(getString(R.string.cooper_github_repository_create_review, repositoryName, visibility))
                            .setNegativeButton(R.string.common_word_cancel, null)
                            .setPositiveButton(R.string.cooper_github_create_repository,
                                    (confirmDialog, confirmWhich) -> createRepository(repositoryName, repositoryDescription, isPrivate))
                            .show();
                }).show();
    }

    private void createRepository(String name, String description, boolean isPrivate) {
        authExecutor.execute(() -> {
            try {
                org.json.JSONObject created = apiClient().createRepository(name, description, isPrivate, true);
                String fullName = created.optString("full_name", "");
                String owner = created.optJSONObject("owner") == null ? "" :
                        created.optJSONObject("owner").optString("login", "");
                String repositoryName = created.optString("name", "");
                if (owner.isEmpty() || repositoryName.isEmpty()) {
                    throw new IllegalStateException("GitHub did not return repository identity for verification.");
                }
                org.json.JSONObject verified = apiClient().getRepository(owner, repositoryName);
                if (!fullName.equals(verified.optString("full_name", ""))) {
                    throw new IllegalStateException("The repository read-back did not match the created repository.");
                }
                String url = verified.optString("html_url", "");
                new Handler(Looper.getMainLooper()).post(() -> {
                    Snackbar.make(content, getString(R.string.cooper_github_repository_create_success, fullName),
                            Snackbar.LENGTH_LONG).show();
                    if (url.startsWith("https://github.com/")) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_repository_create_title)
                                .setMessage(url)
                                .setPositiveButton(R.string.cooper_github_open_settings, (dialog, which) ->
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
                                .setNegativeButton(R.string.common_word_ok, null).show();
                    }
                });
            } catch (Exception e) {
                if (handleReauthenticationFailure(e)) return;
                String message = e.getMessage() == null ? "Repository creation failed." : e.getMessage();
                new Handler(Looper.getMainLooper()).post(() ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.cooper_github_repository_create_title)
                                .setMessage(getString(R.string.cooper_github_repository_list_failed, message))
                                .setPositiveButton(R.string.common_word_ok, null).show());
            }
        });
    }

    private void showAuthError(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_connection_title)
                .setMessage(getString(R.string.cooper_github_signin_failed, message))
                .setPositiveButton(R.string.common_word_ok, null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("cooper_directly_draft_visible", directlyDraftFormVisible);
        if (!directlyDraftFormVisible) return;
        outState.putString("cooper_directly_draft_project_id", directlyDraftProjectId);
        outState.putString("cooper_directly_draft_repository_name", directlyDraftRepositoryName);
        outState.putString("cooper_directly_draft_description", directlyDraftDescription);
        outState.putString("cooper_directly_draft_readme_mode", directlyDraftReadmeMode);
        outState.putString("cooper_directly_draft_readme_content", directlyDraftReadmeContent);
        outState.putBoolean("cooper_directly_draft_private", directlyDraftIsPrivate);
        outState.putBoolean("cooper_directly_draft_visibility_chosen", directlyDraftVisibilityChosen);
        java.util.ArrayList<String> imageUris = new java.util.ArrayList<>();
        if (directlyPendingImages != null) {
            for (Uri uri : directlyPendingImages) if (uri != null) imageUris.add(uri.toString());
        }
        outState.putStringArrayList("cooper_directly_draft_image_uris", imageUris);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!directlyDraftFormVisible && DirectlyGitPublisher.hasPendingSession(this) && !directlyRecoveryPromptShown) {
            new Handler(Looper.getMainLooper()).post(this::showDirectlyGitRecoveryPrompt);
        }
    }

    private void showDirectlyGitRecoveryPrompt() {
        if (isFinishing() || isDestroyed() || directlyRecoveryPromptShown || !DirectlyGitPublisher.hasPendingSession(this)) return;
        org.json.JSONObject session = DirectlyGitPublisher.getPendingSession(this);
        if (session == null) return;
        directlyRecoveryPromptShown = true;
        String scId = session.optString("projectId", "");
        String owner = session.optString("owner", "");
        String repository = session.optString("repositoryName", "");
        String state = session.optString("state", "UNKNOWN");
        String branch = session.optString("branch", "");
        String sha = session.optString("commitSha", "");
        String summary = getString(R.string.cooper_github_directly_recovery_message,
                owner, repository, state, branch, sha.isEmpty() ? getString(R.string.unknown_attr) : sha);
        boolean connected = new GitCredentialStore(this).hasCredential();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_directly_recovery_title)
                .setMessage(summary)
                .setNegativeButton(R.string.cooper_github_directly_recovery_later, (dialog, which) -> directlyRecoveryPromptShown = false)
                .setPositiveButton(connected ? R.string.cooper_github_directly_resume_publish : R.string.cooper_github_connect,
                        (dialog, which) -> {
                            directlyRecoveryPromptShown = false;
                            if (connected) runDirectlyGitRecovery();
                            else startGitHubAuthorization();
                        });
        builder.setNeutralButton(R.string.cooper_github_recovery_select_saved_project, (dialog, which) -> {
            directlyRecoveryPromptShown = false;
            rebindSavedRecoveryProject(scId, session.optString("projectRoot", ""));
        });
        builder.setOnCancelListener(dialog -> directlyRecoveryPromptShown = false);
        builder.show();
    }

    private void rebindSavedRecoveryProject(String scId, String expectedRoot) {
        authExecutor.execute(() -> {
            try {
                if (scId == null || !scId.matches("[0-9]{1,12}") || expectedRoot == null || expectedRoot.isEmpty()) {
                    throw new IllegalStateException("The saved recovery record does not contain a verifiable project identity.");
                }
                java.io.File actualRoot = new java.io.File(a.a.a.wq.b(scId)).getCanonicalFile();
                java.io.File metadataFile = new java.io.File(a.a.a.wq.c(scId), "project").getCanonicalFile();
                if (!actualRoot.isDirectory() || !metadataFile.isFile()
                        || !actualRoot.getCanonicalPath().equals(new java.io.File(expectedRoot).getCanonicalPath())) {
                    throw new IllegalStateException(getString(R.string.cooper_github_recovery_original_project_mismatch));
                }
                GitProjectBindingStore.Binding binding =
                        new GitProjectBindingStore(getApplicationContext()).bind(scId);
                if (!binding.projectRoot.getCanonicalPath().equals(new java.io.File(expectedRoot).getCanonicalPath())) {
                    throw new IllegalStateException("Cooper could not re-verify the original project binding.");
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Snackbar.make(content, getString(R.string.cooper_github_recovery_original_project_selected, binding.workspaceName), Snackbar.LENGTH_LONG).show();
                    directlyRecoveryPromptShown = false;
                    showDirectlyGitRecoveryPrompt();
                });
            } catch (Exception error) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    directlyRecoveryPromptShown = false;
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_directly_recovery_title)
                            .setMessage(error.getMessage() == null ? getString(R.string.cooper_github_recovery_original_project_failed) : error.getMessage())
                            .setPositiveButton(R.string.common_word_ok, null).show();
                });
            }
        });
    }

    private void runDirectlyGitRecovery() {
        if (isFinishing() || isDestroyed()) return;
        directlyRecoveryPromptShown = true;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(12), dp(24), dp(12));
        TextView phase = text(getString(R.string.cooper_github_directly_phase_validate), 14, false);
        panel.addView(phase);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        panel.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_directly_recovery_title)
                .setView(panel)
                .setNegativeButton(R.string.cooper_github_directly_stop_push, null)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(ignored -> {
            android.widget.Button stop = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (stop != null) stop.setOnClickListener(v -> {
                cancelled.set(true);
                stop.setEnabled(false);
                phase.setText(R.string.cooper_github_directly_stop_requested);
            });
        });
        dialog.show();
        authExecutor.execute(() -> {
            try {
                DirectlyGitPublisher publisher = new DirectlyGitPublisher(getApplicationContext(), BuildConfig.COOPER_GITHUB_CLIENT_ID);
                DirectlyGitPublisher.Result result = publisher.resumePending(cancelled, value ->
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isFinishing() && !isDestroyed() && dialog.isShowing()) phase.setText(value);
                        }));
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_directly_publish_success_title)
                            .setMessage(getString(R.string.cooper_github_directly_publish_success,
                                    result.repositoryUrl, result.branch, result.commitSha))
                            .setPositiveButton(R.string.cooper_github_open_repository, (d, which) -> {
                                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result.repositoryUrl))); }
                                catch (Exception ignored) { }
                            })
                            .setNegativeButton(R.string.common_word_ok, null)
                            .show();
                });
            } catch (Exception error) {
                String detail = error.getMessage() == null ? "Recovery did not complete. The saved session was retained." : error.getMessage();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    directlyRecoveryPromptShown = false;
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.cooper_github_directly_recovery_title)
                            .setMessage(getString(R.string.cooper_github_directly_recovery_failed, detail))
                            .setPositiveButton(R.string.common_word_ok, null)
                            .show();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        authCancelled.set(true);
        authExecutor.shutdown();
        authPollingExecutor.shutdown();
    }

    private void addSectionTitle(String title) {
        TextView view = new TextView(this);
        view.setText(title);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(4), dp(18), dp(4), dp(8));
        content.addView(view);
    }

    private void addInfoCard(String title, String status, String description) {
        MaterialCardView card = card();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView leadingIcon = text(iconForTitle(title), 22, false);
        leadingIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconParams.rightMargin = dp(12);
        row.addView(leadingIcon, iconParams);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView heading = text(title, 16, true);
        TextView state = text(status, 14, true);
        state.setPadding(0, dp(6), 0, dp(4));
        TextView detail = text(description, 13, false);
        column.addView(heading);
        column.addView(state);
        column.addView(detail);
        row.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        content.addView(card);
    }

    private void showCooperGitPublicationHistory() {
        final org.json.JSONArray history = DirectlyGitPublisher.getPublicationHistory(this);
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        LinearLayout panel = directlyPanel();
        TextView title = text(getString(R.string.cooper_github_history_title), 25, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(title);
        TextView subtitle = text(getString(R.string.cooper_github_history_subtitle, history.length()), 13, false);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        panel.addView(subtitle);
        if (history.length() == 0) {
            TextView empty = text(getString(R.string.cooper_github_history_empty), 14, false);
            empty.setPadding(0, dp(16), 0, dp(20));
            panel.addView(empty);
        } else {
            for (int i = history.length() - 1; i >= 0; i--) {
                org.json.JSONObject entry = history.optJSONObject(i);
                if (entry == null) continue;
                String appName = entry.optString("appName", "").trim();
                String workspace = entry.optString("workspaceName", "").trim();
                String displayName = appName.isEmpty() ? workspace : appName;
                if (displayName.isEmpty()) displayName = entry.optString("repositoryName", "Git repository");
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(12), dp(12), dp(12));
                MaterialCardView iconCard = card();
                iconCard.setRadius(dp(18));
                ImageView projectIcon = new ImageView(this);
                projectIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                String projectIdForIcon = entry.optString("projectId", "").trim();
                Bitmap projectBitmap = projectIdForIcon.isEmpty() ? null
                        : loadDirectlyGitProjectIcon(projectIdForIcon, null);
                if (projectBitmap != null) {
                    projectIcon.setImageBitmap(projectBitmap);
                    iconCard.addView(projectIcon, new MaterialCardView.LayoutParams(dp(52), dp(52)));
                } else {
                    projectIcon.setImageResource(pro.sketchware.R.drawable.ic_mtrl_android);
                    iconCard.addView(projectIcon, new MaterialCardView.LayoutParams(dp(52), dp(52)));
                }
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(52), dp(52));
                iconParams.rightMargin = dp(12);
                row.addView(iconCard, iconParams);
                LinearLayout details = new LinearLayout(this);
                details.setOrientation(LinearLayout.VERTICAL);
                details.addView(text(displayName, 18, true));
                details.addView(text(entry.optString("owner", "") + "/" + entry.optString("repositoryName", ""), 13, false));
                String packageName = entry.optString("packageName", "").trim();
                if (!packageName.isEmpty()) details.addView(text(getString(R.string.cooper_github_history_package, packageName), 12, false));
                String projectId = entry.optString("projectId", "").trim();
                if (!projectId.isEmpty()) details.addView(text(getString(R.string.cooper_github_history_project_id, projectId), 12, false));
                long timestamp = entry.optLong("publishedAt", 0L);
                if (timestamp > 0L) {
                    String date = java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                            .format(new java.util.Date(timestamp));
                    details.addView(text(getString(R.string.cooper_github_history_date, date), 12, false));
                }
                details.addView(text(getString(R.string.cooper_github_history_method,
                        entry.optString("method", "DIRECTLY_GIT").replace('_', ' '),
                        entry.optBoolean("private", true) ? getString(R.string.cooper_github_directly_visibility_private)
                                : getString(R.string.cooper_github_directly_visibility_public),
                        entry.optString("branch", "")), 12, true));
                String sha = entry.optString("commitSha", "");
                if (!sha.isEmpty()) details.addView(text(getString(R.string.cooper_github_history_commit, sha), 11, false));
                row.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.setClickable(true);
                row.setFocusable(true);
                String url = entry.optString("repositoryUrl", "");
                row.setOnClickListener(v -> {
                    if (url.startsWith("https://github.com/")) {
                        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                        catch (Exception ignored) { }
                    }
                });
                panel.addView(row);
                View divider = new View(this);
                divider.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorOutlineVariant, 0x33808080));
                panel.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }
        com.google.android.material.button.MaterialButton close = new com.google.android.material.button.MaterialButton(this);
        close.setText(R.string.common_word_ok);
        close.setOnClickListener(v -> sheet.dismiss());
        panel.addView(close);
        sheet.setContentView(panel);
        sheet.show();
    }

    private void confirmClearDirectlyGitRecovery() {
        org.json.JSONObject pending = DirectlyGitPublisher.getPendingSession(this);
        if (pending == null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cooper_github_recovery_no_session_title)
                    .setMessage(R.string.cooper_github_recovery_no_session_message)
                    .setPositiveButton(R.string.common_word_ok, null).show();
            return;
        }
        String target = pending.optString("owner", "") + "/" + pending.optString("repositoryName", "");
        String unavailable = getString(R.string.cooper_github_directly_metadata_unavailable);
        String details = getString(R.string.cooper_github_recovery_saved_stage) + ": "
                + pending.optString("state", unavailable) + "\n"
                + getString(R.string.cooper_github_recovery_project_id) + ": "
                + pending.optString("projectId", unavailable) + "\n"
                + getString(R.string.cooper_github_recovery_expected_commit) + ": "
                + pending.optString("commitSha", unavailable) + "\n\n"
                + getString(R.string.cooper_github_recovery_clear_details) + "\n\n"
                + getString(R.string.cooper_github_recovery_target) + ": " + target;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cooper_github_recovery_clear_title)
                .setMessage(details)
                .setNegativeButton(R.string.common_word_cancel, null)
                .setPositiveButton(R.string.cooper_github_recovery_clear_button, (dialog, which) -> {
                    if (DirectlyGitPublisher.clearPendingSession(this)) {
                        Snackbar.make(content, R.string.cooper_github_recovery_clear_success, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(content, R.string.cooper_github_recovery_clear_failed, Snackbar.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void addActionCard(String title, String description, View.OnClickListener listener) {
        MaterialCardView card = card();
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(listener);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView leadingIcon = text(iconForTitle(title), 22, false);
        leadingIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconParams.rightMargin = dp(12);
        row.addView(leadingIcon, iconParams);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(text(title, 15, true));
        TextView detail = text(description, 13, false);
        detail.setPadding(0, dp(5), 0, 0);
        column.addView(detail);
        row.addView(column, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("›", 26, false);
        row.addView(arrow);
        card.addView(row);
        content.addView(card);
    }

    private String iconForTitle(String title) {
        String value = title == null ? "" : title.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("disconnect") || value.contains("logout")) return "↪";
        if (value.contains("account") || value.contains("profile")) return "●";
        if (value.contains("repository") || value.contains("repositories")) return "⑂";
        if (value.contains("project") || value.contains("link")) return "🔗";
        if (value.contains("pull request")) return "⇄";
        if (value.contains("branch")) return "⑂";
        if (value.contains("commit")) return "✓";
        if (value.contains("push") || value.contains("upload")) return "↑";
        if (value.contains("pull") || value.contains("fetch") || value.contains("sync")) return "↓";
        if (value.contains("diff") || value.contains("change") || value.contains("inspect")) return "⌕";
        if (value.contains("safety") || value.contains("policy") || value.contains("secure")) return "♢";
        if (value.contains("status") || value.contains("initialize")) return "◷";
        if (value.contains("implementation")) return "ⓘ";
        if (value.contains("connect")) return "↗";
        return "›";
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setUseCompatPadding(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showNotReady(String feature) {
        Snackbar.make(content, getString(R.string.cooper_github_not_ready_message, feature), Snackbar.LENGTH_LONG).show();
    }
}
