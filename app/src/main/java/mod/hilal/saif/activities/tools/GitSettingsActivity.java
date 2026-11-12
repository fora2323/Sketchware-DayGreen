package mod.hilal.saif.activities.tools;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.editor.manage.library.LibraryCategoryView;
import com.besome.sketch.editor.manage.library.LibraryItemView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityGitSettingsBinding;
import pro.sketchware.databinding.DialogGitCredentialsBinding;
import pro.sketchware.databinding.DialogGitGpgKeyBinding;
import pro.sketchware.databinding.DialogGitUserDataBinding;
import pro.sketchware.utility.SketchwareUtil;

public class GitSettingsActivity extends BaseAppCompatActivity {

    private ActivityGitSettingsBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ActivityGitSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        {
            View view = binding.main;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + systemInsets.left, top, right + systemInsets.right, bottom);
                return insets;
            });
        }

        {
            View view = binding.contentScroll;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(left, top, right, bottom + navBarInsets.bottom);
                return insets;
            });
        }

        binding.collapsingToolbar.setTitle("Git");
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        setupPreferences(binding.contentContainer);
    }

    private void setupPreferences(ViewGroup content) {
        var categories = new ArrayList<LibraryCategoryView>();

        // General
        LibraryCategoryView generalCategory = new LibraryCategoryView(this);
        generalCategory.setTitle("General");
        categories.add(generalCategory);

        generalCategory.addLibraryItem(createSwitchPreference(
                "Colorize file names",
                "Colorize file names depending on Git status",
                GitConfiguration.KEY_COLORIZE_FILE_NAMES,
                true
        ), true);

        generalCategory.addLibraryItem(createSwitchPreference(
                "Gutter indication",
                "Show line change indicators for added, modified and deleted lines",
                GitConfiguration.KEY_GUTTER_INDICATION,
                true
        ), true);

        generalCategory.addLibraryItem(createSwitchPreference(
                "Conflict detection",
                "Show conflict markers and resolve options in the editor",
                GitConfiguration.KEY_CONFLICT_DETECTION,
                true
        ), false);

        // Account
        LibraryCategoryView accountCategory = new LibraryCategoryView(this);
        accountCategory.setTitle("Account");
        categories.add(accountCategory);

        accountCategory.addLibraryItem(createPreference(
                "Credentials",
                "Credentials for accessing remote repositories",
                v -> showCredentialsDialog()
        ), true);

        accountCategory.addLibraryItem(createPreference(
                "User data",
                "User data that will appear in your Git commits",
                v -> showUserDataDialog()
        ), false);

        // Signing
        LibraryCategoryView signingCategory = new LibraryCategoryView(this);
        signingCategory.setTitle("Signing");
        categories.add(signingCategory);

        signingCategory.addLibraryItem(createSwitchPreference(
                "Commit signing",
                "Sign Git commits using GPG or SSH key",
                GitConfiguration.KEY_COMMIT_SIGNING,
                false
        ), true);

        signingCategory.addLibraryItem(createPreference(
                "Signing format",
                "Select the key format used for commit signing",
                v -> showSigningFormatDialog()
        ), true);

        signingCategory.addLibraryItem(createPreference(
                "GPG key",
                "GPG private key and optional passphrase for commit signing",
                v -> showGpgKeyDialog()
        ), false);

        // Repository
        LibraryCategoryView repoCategory = new LibraryCategoryView(this);
        repoCategory.setTitle("Repository");
        categories.add(repoCategory);

        repoCategory.addLibraryItem(createSwitchPreference(
                "Submodules",
                "Include submodules when cloning a repository",
                GitConfiguration.KEY_SUBMODULES,
                true
        ), true);

        repoCategory.addLibraryItem(createSwitchPreference(
                "Recursive submodules",
                "Fetch submodules and their dependencies recursively",
                GitConfiguration.KEY_RECURSIVE_SUBMODULES,
                true
        ), false);

        categories.forEach(content::addView);
    }

    private LibraryItemView createPreference(String title, String desc, View.OnClickListener listener) {
        LibraryItemView preference = new LibraryItemView(this);
        preference.setHideIcon(true);
        preference.setHideEnabled();
        preference.title.setText(title);
        preference.description.setText(desc);
        preference.setOnClickListener(listener);
        return preference;
    }

    private LibraryItemView createSwitchPreference(String title, String desc, String key, boolean defaultValue) {
        LibraryItemView preference = new LibraryItemView(this);
        preference.setHideIcon(true);
        preference.setHideEnabled();
        preference.title.setText(title);
        preference.description.setText(desc);
        preference.sw_enable.setVisibility(View.VISIBLE);
        preference.sw_enable.setChecked(GitConfiguration.getBoolean(this, key, defaultValue));
        preference.sw_enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GitConfiguration.setBoolean(this, key, isChecked);
        });
        preference.setOnClickListener(v -> preference.sw_enable.toggle());
        return preference;
    }

    private void showCredentialsDialog() {
        DialogGitCredentialsBinding dialogBinding = DialogGitCredentialsBinding.inflate(getLayoutInflater());
        dialogBinding.etUsername.setText(GitConfiguration.getUsername(this));
        dialogBinding.etPassword.setText(GitConfiguration.getPassword(this));

        dialogBinding.btnOpenGithubTokens.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/tokens"));
                startActivity(intent);
            } catch (Exception e) {
                SketchwareUtil.toast("Could not open browser");
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Credentials")
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (d, which) -> {
                    String username = Helper.getText(dialogBinding.etUsername).trim();
                    String password = Helper.getText(dialogBinding.etPassword).trim();
                    GitConfiguration.setUsername(this, username);
                    GitConfiguration.setPassword(this, password);
                    SketchwareUtil.toast("Credentials saved");
                })
                .create();

        dialog.show();
    }

    private void showUserDataDialog() {
        DialogGitUserDataBinding dialogBinding = DialogGitUserDataBinding.inflate(getLayoutInflater());
        dialogBinding.etName.setText(GitConfiguration.getUserName(this));
        dialogBinding.etEmail.setText(GitConfiguration.getUserEmail(this));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("User data")
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (d, which) -> {
                    String name = Helper.getText(dialogBinding.etName).trim();
                    String email = Helper.getText(dialogBinding.etEmail).trim();
                    GitConfiguration.setUserName(this, name);
                    GitConfiguration.setUserEmail(this, email);
                    GitConfiguration.saveGitConfig(this);
                    SketchwareUtil.toast("User data saved");
                })
                .create();

        dialog.show();
    }

    private void showSigningFormatDialog() {
        String[] formats = new String[]{"GPG", "SSH"};
        int selectedIndex = "SSH".equalsIgnoreCase(GitConfiguration.getSigningFormat(this)) ? 1 : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Signing format")
                .setSingleChoiceItems(formats, selectedIndex, (dialog, which) -> {
                    GitConfiguration.setSigningFormat(this, formats[which]);
                    SketchwareUtil.toast("Format set to " + formats[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGpgKeyDialog() {
        DialogGitGpgKeyBinding dialogBinding = DialogGitGpgKeyBinding.inflate(getLayoutInflater());
        dialogBinding.etGpgKey.setText(GitConfiguration.getGpgKey(this));
        dialogBinding.etPassphrase.setText(GitConfiguration.getGpgPassphrase(this));

        new MaterialAlertDialogBuilder(this)
                .setTitle("GPG key")
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (d, which) -> {
                    String key = Helper.getText(dialogBinding.etGpgKey).trim();
                    String passphrase = Helper.getText(dialogBinding.etPassphrase).trim();
                    GitConfiguration.setGpgKey(this, key);
                    GitConfiguration.setGpgPassphrase(this, passphrase);
                    SketchwareUtil.toast("GPG key saved");
                })
                .show();
    }
}
