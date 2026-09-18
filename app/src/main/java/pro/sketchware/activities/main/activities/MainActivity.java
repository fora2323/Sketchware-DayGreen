package pro.sketchware.activities.main.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.besome.sketch.lib.base.BasePermissionAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import a.a.a.DB;
import a.a.a.GB;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.setup.DRSetup;
import mod.hey.studios.project.backup.BackupFactory;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.hey.studios.util.Helper;
import mod.tyron.backup.SingleCopyTask;
import pro.sketchware.R;
import pro.sketchware.activities.main.fragments.projects.ProjectsFragment;
import pro.sketchware.databinding.MainBinding;
import pro.sketchware.utility.DataResetter;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class MainActivity extends BasePermissionAppCompatActivity {

    private static final String PROJECTS_FRAGMENT_TAG = "projects_fragment";

    private ActionBarDrawerToggle drawerToggle;
    private DB u;
    private Snackbar storageAccessDenied;
    private MainBinding binding;

    private boolean isFabMenuOpen = false;

    private final OnBackPressedCallback closeDrawer = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isFabMenuOpen) {
                closeFabMenu();
                return;
            }
            setEnabled(false);
            binding.drawerLayout.closeDrawers();
        }
    };

    private ProjectsFragment projectsFragment;
    private Fragment activeFragment;
    private BackupRestoreManager backupRestoreManager;

    public static boolean needRefreshProjectList = false;

    @IdRes
    private int currentNavItemId = R.id.item_projects;

    @Override
    public void g(int i) {
        if (i == 9501) {
            allFilesAccessCheck();
            if (activeFragment instanceof ProjectsFragment) {
                projectsFragment.refreshProjectsList();
            }
        }
    }

    @Override
    public void h(int i) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        startActivityForResult(intent, i);
    }

    @Override
    public void l() {
    }

    @Override
    public void m() {
    }

    public void n() {
        if (activeFragment instanceof ProjectsFragment) {
            projectsFragment.refreshProjectsList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 105:
                    DataResetter.a(this, data.getBooleanExtra("onlyConfig", true));
                    break;

                case 111:
                    invalidateOptionsMenu();
                    break;

                case 113:
                    if (data != null && data.getBooleanExtra("not_show_popup_anymore", false)) {
                        u.a("U1I2", (Object) false);
                    }
                    break;

                case 212:
                    if (data != null && !(data.getStringExtra("save_as_new_id") == null ? "" : data.getStringExtra("save_as_new_id")).isEmpty() && isStoragePermissionGranted()) {
                        if (activeFragment instanceof ProjectsFragment) {
                            projectsFragment.refreshProjectsList();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeNoContrast();

        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // System Bar
        binding.statusBarOverlapper.setMinimumHeight(UI.getStatusBarHeight(this));
        UI.addSystemWindowInsetToPadding(binding.appbar, true, false, true, false);

        // Database
        u = new DB(getApplicationContext(), "U1");
        int u1I0 = u.a("U1I0", -1);
        long u1I1 = u.e("U1I1");

        if (u1I1 <= 0) {
            u.a("U1I1", System.currentTimeMillis());
        }

        if (System.currentTimeMillis() - u1I1 > 1000 * 60 * 60 * 24) {
            u.a("U1I0", Integer.valueOf(u1I0 + 1));
        }

        // Action Bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(null);

        // Drawer Toggle Setup
        drawerToggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                R.string.app_name,
                R.string.app_name
        );
        drawerToggle.setDrawerIndicatorEnabled(false);

        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setNavigationOnClickListener(null);
        
        // Custom Drawer Button Listener
        binding.drawerToggleBtn.setOnClickListener(v -> {
            clearSearchFocus();
            if (binding.drawerLayout.isDrawerOpen(binding.leftDrawer)) {
                binding.drawerLayout.closeDrawer(binding.leftDrawer);
            } else {
                binding.drawerLayout.openDrawer(binding.leftDrawer);
            }
        });

        // Drawer Listener
        binding.drawerLayout.addDrawerListener(drawerToggle);
        binding.drawerLayout.setScrimColor(Color.TRANSPARENT);
        binding.drawerLayout.setDrawerElevation(0f);

        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                float moveFactor = drawerView.getWidth() * slideOffset;
                binding.layoutCoordinator.setTranslationX(moveFactor);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawer.setEnabled(true);
                getOnBackPressedDispatcher().addCallback(closeDrawer);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                binding.layoutCoordinator.setTranslationX(0f);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        
        // Setup drawer button dengan oval drawable
GradientDrawable drawerCircle = new GradientDrawable();
drawerCircle.setShape(GradientDrawable.OVAL);
drawerCircle.setColor(MaterialColors.getColor(
        this,
        R.attr.colorSurfaceContainer,
        Color.TRANSPARENT
));
binding.drawerToggleBtn.setBackground(drawerCircle);

        // FAB Setup
        setupFabMenu();

        // FAB System Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabMenuContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = dp(16) + systemBars.bottom;
            params.rightMargin = dp(16);
            v.setLayoutParams(params);
            return insets;
        });

        // Search Setup
        setupProjectSearch();

        // Background Oval untuk Search Button
        GradientDrawable searchCircle = new GradientDrawable();
        searchCircle.setShape(GradientDrawable.OVAL);
        searchCircle.setColor(MaterialColors.getColor(
                this,
                R.attr.colorSurfaceContainer,
                Color.TRANSPARENT
        ));
        binding.searchButton.setBackground(searchCircle);

        // Storage Check
        boolean hasStorageAccess = isStoragePermissionGranted();
        if (!hasStorageAccess) {
            showNoticeNeedStorageAccess();
        } else {
            allFilesAccessCheck();
        }

        // Open Backup File Intent
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            if (data != null) {
                new SingleCopyTask(this, new SingleCopyTask.CallBackTask() {
                    @Override
                    public void onCopyPreExecute() {
                    }

                    @Override
                    public void onCopyProgressUpdate(int progress) {
                    }

                    @Override
                    public void onCopyPostExecute(@NonNull String path, boolean wasSuccessful, @NonNull String reason) {
                        if (wasSuccessful) {
                            BackupRestoreManager manager = new BackupRestoreManager(MainActivity.this, projectsFragment);

                            if (BackupFactory.zipContainsFile(path, "local_libs")) {
                                new MaterialAlertDialogBuilder(MainActivity.this)
                                        .setTitle("Warning")
                                        .setMessage(BackupRestoreManager.getRestoreIntegratedLocalLibrariesMessage(false, -1, -1, null))
                                        .setPositiveButton("Copy", (dialog, which) -> manager.doRestore(path, true))
                                        .setNegativeButton("Don't copy", (dialog, which) -> manager.doRestore(path, false))
                                        .setNeutralButton(R.string.common_word_cancel, null)
                                        .show();
                            } else {
                                manager.doRestore(path, true);
                            }
                            getIntent().setData(null);
                        } else {
                            SketchwareUtil.toastError("Failed to copy backup file to temporary location: " + reason, Toast.LENGTH_LONG);
                        }
                    }
                }).copyFile(data);
            }
        }

        // Restore State
        if (savedInstanceState != null) {
            projectsFragment = (ProjectsFragment) getSupportFragmentManager().findFragmentByTag(PROJECTS_FRAGMENT_TAG);
            currentNavItemId = savedInstanceState.getInt("selected_tab_id");
            Fragment current = getFragmentForNavId(currentNavItemId);

            if (current instanceof ProjectsFragment) {
                navigateToProjectsFragment();
            }
            return;
        }

        // Initial Fragment
        navigateToProjectsFragment();

        // Backup / Restore Manager Init
        backupRestoreManager = new BackupRestoreManager(this, projectsFragment);

        Configs.mainActivity = this;
        DRSetup.startNow(this);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void clearSearchFocus() {
        if (binding == null) return;

        if (binding.searchInput.hasFocus()) {
            binding.searchInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && binding != null && binding.searchInput.hasFocus()) {
            Rect searchRect = new Rect();
            Rect buttonRect = new Rect();

            binding.searchContainer.getGlobalVisibleRect(searchRect);
            binding.searchButton.getGlobalVisibleRect(buttonRect);

            if (!searchRect.contains((int) event.getRawX(), (int) event.getRawY())
                    && !buttonRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                clearSearchFocus();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void setupProjectSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.length() > 0;
                binding.searchButton.setImageResource(hasText ? R.drawable.ic_mtrl_close : R.drawable.ic_mtrl_search);

                if (projectsFragment != null) {
                    projectsFragment.filterProjects(s == null ? "" : s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.searchButton.setOnClickListener(v -> {
            String query = binding.searchInput.getText().toString();

            if (query.length() > 0) {
                binding.searchInput.setText("");
            } else {
                binding.searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    private void setupFabMenu() {
        binding.createNewProject.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                openFabMenu();
            }
        });

        binding.fabOverlay.setOnClickListener(v -> {
            closeFabMenu();
            clearSearchFocus();
        });

        binding.fabCreate.setOnClickListener(v -> {
            closeFabMenu();
            if (projectsFragment != null) {
                projectsFragment.toProjectSettingsActivity();
            }
        });

        binding.fabRestore.setOnClickListener(v -> {
            closeFabMenu();
            if (backupRestoreManager == null) {
                backupRestoreManager = new BackupRestoreManager(MainActivity.this, projectsFragment);
            }
            backupRestoreManager.restore();
        });
    }

    private void openFabMenu() {
        isFabMenuOpen = true;

        binding.createNewProject.animate().rotation(0f).setDuration(150).start();

        binding.layoutFabCreate.setVisibility(View.VISIBLE);
        binding.layoutFabRestore.setVisibility(View.VISIBLE);

        binding.layoutFabCreate.setAlpha(0f);
        binding.layoutFabCreate.setTranslationY(20f);

        binding.layoutFabRestore.setAlpha(0f);
        binding.layoutFabRestore.setTranslationY(20f);

        binding.layoutFabCreate.animate().alpha(1f).translationY(0f).setDuration(180).start();
        binding.layoutFabRestore.animate().alpha(1f).translationY(0f).setStartDelay(50).setDuration(180).start();

        binding.fabOverlay.setVisibility(View.VISIBLE);
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;

        binding.createNewProject.animate().rotation(0f).setDuration(150).start();

        binding.layoutFabCreate.setVisibility(View.GONE);
        binding.layoutFabRestore.setVisibility(View.GONE);
        binding.fabOverlay.setVisibility(View.GONE);
    }

    private Fragment getFragmentForNavId(int navItemId) {
        if (navItemId == R.id.item_projects) {
            return projectsFragment;
        }
        throw new IllegalArgumentException();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_tab_id", currentNavItemId);
    }

    private void navigateToProjectsFragment() {
        if (projectsFragment == null) {
            projectsFragment = new ProjectsFragment();
        }

        boolean shouldShow = true;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        binding.createNewProject.show();

        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        if (fm.findFragmentByTag(PROJECTS_FRAGMENT_TAG) == null) {
            shouldShow = false;
            transaction.add(binding.container.getId(), projectsFragment, PROJECTS_FRAGMENT_TAG);
        }

        if (shouldShow) {
            transaction.show(projectsFragment);
        }

        transaction.commit();

        activeFragment = projectsFragment;
        currentNavItemId = R.id.item_projects;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        binding.toolbar.setNavigationIcon(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        long freeMegabytes = GB.c();

        if (freeMegabytes < 100 && freeMegabytes > 0) {
            showNoticeNotEnoughFreeStorageSpace();
        }

        if (isStoragePermissionGranted() && storageAccessDenied != null && storageAccessDenied.isShown()) {
            storageAccessDenied.dismiss();
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity");

        mAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);

        if (needRefreshProjectList) {
            projectsFragment.refreshProjectsList();
            needRefreshProjectList = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void allFilesAccessCheck() {
        if (Build.VERSION.SDK_INT > 29) {
            File optOutFile = new File(getFilesDir(), ".skip_all_files_access_notice");
            boolean granted = Environment.isExternalStorageManager();

            if (!optOutFile.exists() && !granted) {
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);

                dialog.setIcon(R.drawable.ic_mtrl_warning);
                dialog.setTitle("Android 11 storage access");
                dialog.setMessage(
                        "Starting with Android 11, "
                                + "Sketchware Pro needs a new "
                                + "permission to avoid taking "
                                + "ages to build projects. "
                                + "Don't worry, we can't do "
                                + "more to storage than with "
                                + "current granted permissions."
                );

                dialog.setPositiveButton(Helper.getResString(R.string.common_word_settings), (v, which) -> {
                    FileUtil.requestAllFilesAccessPermission(this);
                    v.dismiss();
                });

                dialog.setNegativeButton("Skip", null);

                dialog.setNeutralButton("Don't show anymore", (v, which) -> {
                    try {
                        if (!optOutFile.createNewFile()) {
                            throw new IOException("Failed to create file " + optOutFile);
                        }
                    } catch (IOException e) {
                        Log.e("MainActivity", "Error while trying to create dialog file: " + e.getMessage(), e);
                    }
                    v.dismiss();
                });

                dialog.show();
            }
        }
    }

    private void showNoticeNeedStorageAccess() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);

        dialog.setTitle(Helper.getResString(R.string.common_message_permission_title_storage));
        dialog.setIcon(R.drawable.ic_mtrl_folder);
        dialog.setMessage(Helper.getResString(R.string.common_message_permission_need_load_project));

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), (v, which) -> {
            v.dismiss();
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    9501
            );
        });

        dialog.show();
    }

    private void showNoticeNotEnoughFreeStorageSpace() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);

        dialog.setTitle(Helper.getResString(R.string.common_message_insufficient_storage_space_title));
        dialog.setIcon(R.drawable.disc_full_24px);
        dialog.setMessage(Helper.getResString(R.string.common_message_insufficient_storage_space));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);

        dialog.show();
    }

    public void s() {
        if (storageAccessDenied == null || !storageAccessDenied.isShown()) {
            storageAccessDenied = Snackbar.make(
                    binding.layoutCoordinator,
                    Helper.getResString(R.string.common_message_permission_denied),
                    Snackbar.LENGTH_INDEFINITE
            );

            storageAccessDenied.setAction(Helper.getResString(R.string.common_word_settings), v -> {
                storageAccessDenied.dismiss();
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        9501
                );
            });

            storageAccessDenied.setActionTextColor(Color.YELLOW);
            storageAccessDenied.show();
        }
    }
}