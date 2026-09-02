package com.besome.sketch.editor.manage.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Objects;

import a.a.a.YB;
import a.a.a.bB;
import a.a.a.rq;
import a.a.a.uq;
import a.a.a.wB;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.library.LibraryUtils;
import extensions.anbui.daydream.project.ProjectBuildConfigs;
import extensions.anbui.daydream.project.ProjectUnsavedData;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import extensions.anbui.daydream.project.ProjectLibrary;
import extensions.anbui.daydream.tools.ToolCore;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageScreenActivityAddTempBinding;
import pro.sketchware.utility.SketchwareUtil;

public class AddViewActivity extends BaseAppCompatActivity {

    private YB nameValidator;
    private boolean featureStatusBar, featureToolbar, featureFab, featureDrawer;
    private int requestCode;
    private ProjectFileBean projectFileBean;
    private String P;
    private ArrayList<FeatureItem> featureItems;
    private FeaturesAdapter featuresAdapter;

    private ManageScreenActivityAddTempBinding binding;

    private void a(FeatureItem featureItem) {
        int type = featureItem.type;
        switch (type) {
            case 0 -> {
                if (featureItem.isEnabled) {
                    resetTranslationY(binding.previewStatusbar);
                    if (featureToolbar) {
                        resetTranslationY(binding.previewToolbar);
                    }
                } else {
                    slideInVertically(binding.previewStatusbar);
                    if (featureToolbar) {
                        binding.previewToolbar.animate().translationY((float) -binding.previewStatusbar.getMeasuredHeight()).start();
                    } else {
                        slideOutPreviewToolbar();
                    }
                }
            }
            case 1 -> {
                if (featureItem.isEnabled) {
                    if (!featureStatusBar) {
                        binding.previewToolbar.animate().translationY((float) -binding.previewStatusbar.getMeasuredHeight()).start();
                    } else {
                        resetTranslationY(binding.previewToolbar);
                    }
                } else if (!featureStatusBar) {
                    slideOutPreviewToolbar();
                } else {
                    slideInVertically(binding.previewToolbar);
                }
            }
            case 2 -> {
                if (featureItem.isEnabled) {
                    resetTranslationX(binding.previewDrawer);
                } else {
                    slideOutHorizontally(binding.previewDrawer, "left");
                }
            }
            case 3 -> {
                if (featureItem.isEnabled) {
                    resetTranslationX(binding.previewFab);
                } else {
                    slideOutHorizontally(binding.previewFab, "right");
                }
            }
        }
    }

    private boolean isValid(YB validator) {
        return validator.b();
    }

    private void slideOutHorizontally(View view, String direction) {
        if ("left".equals(direction)) {
            view.animate().translationX((float) -view.getMeasuredWidth()).start();
        } else {
            view.animate().translationX((float) view.getMeasuredWidth()).start();
        }
    }

    private void slideOutVertically(View view) {
        view.animate().translationY((float) view.getMeasuredHeight()).start();
    }

    private void slideOutPreviewToolbar() {
        binding.previewToolbar.animate().translationY((float) -(binding.previewStatusbar.getMeasuredHeight() + binding.previewToolbar.getMeasuredHeight())).start();
    }

    private void slideInVertically(View view) {
        view.animate().translationY((float) -view.getMeasuredHeight()).start();
    }

    private void disableDrawer() {
        for (int i = 0; i < featureItems.size(); i++) {
            FeatureItem item = featureItems.get(i);
            if (item.type == 2) {
                item.isEnabled = false;
                featuresAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void enableToolbar() {
        for (int i = 0; i < featureItems.size(); i++) {
            FeatureItem item = featureItems.get(i);
            if (item.type == 1) {
                item.isEnabled = true;
                featuresAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void resetTranslationX(View view) {
        view.animate().translationX(0.0F).start();
    }

    private void resetTranslationY(View view) {
        view.animate().translationY(0.0F).start();
    }

    private ArrayList<ViewBean> getPresetData(String var1) {
        return rq.f(var1);
    }

    private void initItem(int option) {
        featureToolbar = (option & ProjectFileBean.OPTION_ACTIVITY_TOOLBAR) == ProjectFileBean.OPTION_ACTIVITY_TOOLBAR;
        featureStatusBar = (option & ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN) != ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN;
        featureFab = (option & ProjectFileBean.OPTION_ACTIVITY_FAB) == ProjectFileBean.OPTION_ACTIVITY_FAB;
        featureDrawer = (option & ProjectFileBean.OPTION_ACTIVITY_DRAWER) == ProjectFileBean.OPTION_ACTIVITY_DRAWER;
    }

    private void initializeItems() {
        featureItems = new ArrayList<>();
        featureItems.add(new FeatureItem(0, R.drawable.ic_statusbar_color_48dp, "StatusBar", featureStatusBar));
        featureItems.add(new FeatureItem(1, R.drawable.ic_toolbar_color_48dp, "Toolbar", featureToolbar));
        featureItems.add(new FeatureItem(2, R.drawable.ic_drawer_color_48dp, "Drawer", featureDrawer));
        featureItems.add(new FeatureItem(3, R.drawable.fab_color, "FAB", featureFab));
        featuresAdapter.notifyDataSetChanged();
    }

    private void initializeDayDreamFeature() {
        if (projectFileBean != null) {
            binding.edgetoedge.setChecked(DayDreamProjectSettings.isEnableEdgeToEdge(Configs.currentProjectID, projectFileBean.fileName));
            binding.windowinsetshandling.setChecked(DayDreamProjectSettings.isEnableWindowInsetsHandling(Configs.currentProjectID, projectFileBean.fileName));
            binding.disableautomaticpermissionrequests.setChecked(DayDreamProjectSettings.isDisableAutomaticPermissionRequests(Configs.currentProjectID, projectFileBean.fileName));
            binding.swContentprotection.setChecked(DayDreamProjectSettings.isContentProtection(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportworkmanager.setChecked(DayDreamProjectSettings.isImportWorkManager(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportmedia3.setChecked(DayDreamProjectSettings.isImportAndroidXMedia3(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportandroixbrowser.setChecked(DayDreamProjectSettings.isImportAndroidXBrowser(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportandroixcredentialmanager.setChecked(DayDreamProjectSettings.isImportAndroidXCredentialManager(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportshizuku.setChecked(DayDreamProjectSettings.isImportShizuku(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportglidetransformations.setChecked(DayDreamProjectSettings.isImportGlideTransformations(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportandroidbilling.setChecked(DayDreamProjectSettings.isImportAndroidBilling(Configs.currentProjectID, projectFileBean.fileName));
            binding.swImportretrofit2.setChecked(DayDreamProjectSettings.isImportRetrofit2(Configs.currentProjectID, projectFileBean.fileName));
        }

        binding.lnEdgetoedge.setOnClickListener(v -> binding.edgetoedge.toggle());
        binding.lnWindowinsetshandling.setOnClickListener(v -> binding.windowinsetshandling.toggle());
        binding.lnDisableautomaticpermissionrequests.setOnClickListener(v -> binding.disableautomaticpermissionrequests.toggle());
        binding.lnContentprotection.setOnClickListener(v -> binding.swContentprotection.toggle());
        binding.lnImportworkmanager.setOnClickListener(v -> binding.swImportworkmanager.toggle());
        binding.lnImportmedia3.setOnClickListener(v -> binding.swImportmedia3.toggle());
        binding.lnImportandroixbrowser.setOnClickListener(v -> binding.swImportandroixbrowser.toggle());
        binding.lnImportandroixcredentialmanager.setOnClickListener(v -> binding.swImportandroixcredentialmanager.toggle());
        binding.lnImportshizuku.setOnClickListener(v -> binding.swImportshizuku.toggle());
        binding.lnImportglidetransformations.setOnClickListener(v -> binding.swImportglidetransformations.toggle());
        binding.lnImportandroidbilling.setOnClickListener(v -> binding.swImportandroidbilling.toggle());
        binding.lnImportretrofit2.setOnClickListener(v -> binding.swImportretrofit2.toggle());

        if (!ProjectLibrary.isEnabledAppCompat(Configs.currentProjectID)) {
            binding.lnEdgetoedge.setEnabled(false);
            binding.lnWindowinsetshandling.setEnabled(false);
            binding.lnEdgetoedge.setAlpha(0.5f);
            binding.lnWindowinsetshandling.setAlpha(0.5f);
            binding.tvEdgetoedgenote.setText("To use, enable AppCompat. " + binding.tvEdgetoedgenote.getText().toString());
            binding.tvWindowinsetshandlingnote.setText("To use, enable AppCompat. " + binding.tvWindowinsetshandlingnote.getText().toString());
        } else {
            if (DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID) && DayDreamProjectSettings.isUniversalEdgeToEdge(Configs.currentProjectID)) {
                binding.lnEdgetoedge.setEnabled(false);
                binding.lnEdgetoedge.setAlpha(0.5f);
                binding.tvEdgetoedgenote.setText("It is enabled and cannot be changed because it applies to the entire project. " + binding.tvEdgetoedgenote.getText().toString());
            }

            if (ProjectBuildConfigs.isUseJava7(Configs.currentProjectID)) {
                binding.lnWindowinsetshandling.setEnabled(false);
                binding.lnWindowinsetshandling.setAlpha(0.5f);
                binding.tvWindowinsetshandlingnote.setText("To use, use a newer version of Java. " + binding.tvWindowinsetshandlingnote.getText().toString());
            } else if (DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID) && DayDreamProjectSettings.isUniversalWindowInsetsHandling(Configs.currentProjectID)) {
                binding.lnWindowinsetshandling.setEnabled(false);
                binding.lnWindowinsetshandling.setAlpha(0.5f);
                binding.tvWindowinsetshandlingnote.setText("It is enabled and cannot be changed because it applies to the entire project. " + binding.tvWindowinsetshandlingnote.getText().toString());
            }
        }

        if (DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)) {
            if (DayDreamProjectSettings.isUniversalDisableAutomaticPermissionRequests(Configs.currentProjectID)) {
                binding.lnDisableautomaticpermissionrequests.setEnabled(false);
                binding.lnDisableautomaticpermissionrequests.setAlpha(0.5f);
                binding.tvDisableautomaticpermissionrequestsnote.setText("It is enabled and cannot be changed because it applies to the entire project. " + binding.tvDisableautomaticpermissionrequestsnote.getText().toString());
            }

            if (DayDreamProjectSettings.isUniversalContentProtection(Configs.currentProjectID)) {
                binding.lnContentprotection.setEnabled(false);
                binding.lnContentprotection.setAlpha(0.5f);
                binding.tvContentprotectionnote.setText("It is enabled and cannot be changed because it applies to the entire project. " + binding.tvContentprotectionnote.getText().toString());
            }
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isForceAddWorkManager(Configs.currentProjectID)
                && LibraryUtils.isAllowUseAndroidXWorkManager(Configs.currentProjectID))) {
            binding.lnImportworkmanager.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isUniversalUseMedia3(Configs.currentProjectID)
                && LibraryUtils.isAllowUseAndroidXMedia3(Configs.currentProjectID))) {
            binding.lnImportmedia3.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isUniversalUseAndroidXBrowser(Configs.currentProjectID)
                && LibraryUtils.isAllowUseAndroidXBrowser(Configs.currentProjectID))) {
            binding.lnImportandroixbrowser.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isUniversalUseAndroidXCredentialManager(Configs.currentProjectID)
                && LibraryUtils.isAllowUseAndroidXCredentialManager(Configs.currentProjectID))) {
            binding.lnImportandroixcredentialmanager.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isUseShizuku(Configs.currentProjectID)
                && LibraryUtils.isAllowUseShizuku(Configs.currentProjectID))) {
            binding.lnImportshizuku.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isGlideTransformations(Configs.currentProjectID))) {
            binding.lnImportglidetransformations.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isUseAndroidBilling(Configs.currentProjectID)
                && LibraryUtils.isAllowUseAndroidBilling(Configs.currentProjectID))) {
            binding.lnImportandroidbilling.setVisibility(View.GONE);
        }

        if (!(DayDreamProjectSettings.isEnableDayDream(Configs.currentProjectID)
                && DayDreamProjectSettings.isUseRetrofit2(Configs.currentProjectID))) {
            binding.lnImportretrofit2.setVisibility(View.GONE);
        }
    }

    private void saveDayDreamData() {
        String filename;
        if (projectFileBean != null) {
            //On update
            filename = projectFileBean.fileName;

            //Rename
            if (!Objects.requireNonNull(binding.edName.getText()).toString().equals(filename)) {
                if(ProjectUnsavedData.renameActivity(Configs.currentProjectID, filename, Objects.requireNonNull(binding.edName.getText()).toString())) {
                    filename = Objects.requireNonNull(binding.edName.getText()).toString();
                    DayDreamProjectSettings.setActivityType(Configs.currentProjectID, Objects.requireNonNull(binding.edName.getText()).toString(), DayDreamProjectSettings.getActivityType(Configs.currentProjectID, projectFileBean.fileName));
                } else {
                    SketchwareUtil.toast("Cannot change name.");
                }
                DesignActivity.needReloadProjectData = true;
            }
        } else {
            //On Create new
            filename = Objects.requireNonNull(binding.edName.getText()).toString() + (binding.cbAddSuffix.isChecked() ? getSuffix(binding.viewTypeSelector) : "");
        }

        DayDreamProjectSettings.setEnableEdgeToEdge(Configs.currentProjectID, filename, binding.edgetoedge.isChecked());
        DayDreamProjectSettings.setEnableWindowInsetsHandling(Configs.currentProjectID, filename, binding.windowinsetshandling.isChecked());
        DayDreamProjectSettings.setDisableAutomaticPermissionRequests(Configs.currentProjectID, filename, binding.disableautomaticpermissionrequests.isChecked());
        DayDreamProjectSettings.setContentProtection(Configs.currentProjectID, filename, binding.swContentprotection.isChecked());
        DayDreamProjectSettings.setImportWorkManager(Configs.currentProjectID, filename, binding.swImportworkmanager.isChecked());
        DayDreamProjectSettings.setImportAndroidXMedia3(Configs.currentProjectID, filename, binding.swImportmedia3.isChecked());
        DayDreamProjectSettings.setImportAndroidXBrowser(Configs.currentProjectID, filename, binding.swImportandroixbrowser.isChecked());
        DayDreamProjectSettings.setImportAndroidXCredentialManager(Configs.currentProjectID, filename, binding.swImportandroixcredentialmanager.isChecked());
        DayDreamProjectSettings.setImportShizuku(Configs.currentProjectID, filename, binding.swImportshizuku.isChecked());
        DayDreamProjectSettings.setImportGlideTransformations(Configs.currentProjectID, filename, binding.swImportglidetransformations.isChecked());
        DayDreamProjectSettings.setImportAndroidBilling(Configs.currentProjectID, filename, binding.swImportandroidbilling.isChecked());
        DayDreamProjectSettings.setImportRetrofit2(Configs.currentProjectID, filename, binding.swImportretrofit2.isChecked());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 276 && resultCode == RESULT_OK) {
            ProjectFileBean presetData = data.getParcelableExtra("preset_data");
            P = presetData.presetName;
            initItem(presetData.options);
            initializeItems();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ManageScreenActivityAddTempBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle("Create new");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        Intent intent1 = getIntent();
        ArrayList<String> screenNames = intent1.getStringArrayListExtra("screen_names");
        requestCode = intent1.getIntExtra("request_code", 264);
        projectFileBean = intent1.getParcelableExtra("project_file");
        if (projectFileBean != null) {
            binding.toolbar.setTitle("Edit " + projectFileBean.fileName);
        }

        featuresAdapter = new FeaturesAdapter();
        binding.featureTypes.setAdapter(featuresAdapter);

        binding.keyboardSettingsSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.select_visible || checkedId == R.id.select_unspecified) {
                    resetTranslationY(binding.imgKeyboard);
                } else {
                    slideOutVertically(binding.imgKeyboard);
                }
            }
        });

        binding.viewTypeSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                setManifestViewState(checkedId == R.id.select_activity);
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            int options = ProjectFileBean.OPTION_ACTIVITY_TOOLBAR;
            if (265 == requestCode) {
                if (!Objects.requireNonNull(binding.edName.getText()).toString().equals(projectFileBean.fileName)) {
                    if (projectFileBean.fileName.equals("main") || !ToolCore.isXMLNameValid(Configs.currentProjectID, Objects.requireNonNull(binding.edName.getText()).toString(), projectFileBean.fileName, true, null))
                        return;
                }

                projectFileBean.orientation = getSelectedButtonIndex(binding.screenOrientationSelector);
                projectFileBean.keyboardSetting = getSelectedButtonIndex(binding.keyboardSettingsSelector);
                if (!featureToolbar) {
                    options = 0;
                }
                if (!featureStatusBar) {
                    options = options | ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN;
                }
                if (featureFab) {
                    options = options | ProjectFileBean.OPTION_ACTIVITY_FAB;
                }
                if (featureDrawer) {
                    options = options | ProjectFileBean.OPTION_ACTIVITY_DRAWER;
                }
                projectFileBean.options = options;
                Intent intent = new Intent();
                intent.putExtra("project_file", projectFileBean);
                setResult(RESULT_OK, intent);
                bB.a(getApplicationContext(), getString(R.string.design_manager_message_edit_complete, new Object[0]), bB.TOAST_NORMAL).show();

                saveDayDreamData();

                finish();
            } else if (isValid(nameValidator)) {
                String var4 = Helper.getText(binding.edName);

                if (binding.cbAddSuffix.isChecked()) var4 += getSuffix(binding.viewTypeSelector);
                DayDreamProjectSettings.setActivityType(Configs.currentProjectID, var4, getSuffix(binding.viewTypeSelector));

                ProjectFileBean projectFileBean = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, var4, getSelectedButtonIndex(binding.screenOrientationSelector), getSelectedButtonIndex(binding.keyboardSettingsSelector), featureToolbar, !featureStatusBar, featureFab, featureDrawer);
                Intent intent = new Intent();
                intent.putExtra("project_file", projectFileBean);
                if (P != null) {
                    intent.putExtra("preset_views", getPresetData(P));
                }
                setResult(RESULT_OK, intent);
                bB.a(getApplicationContext(), getString(R.string.design_manager_message_add_complete, new Object[0]), bB.TOAST_NORMAL).show();

                saveDayDreamData();

                finish();
            }

        });

        binding.btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (requestCode == 265) {
            //nameValidator = new YB(getApplicationContext(), binding.tiName, uq.b, new ArrayList<>(), projectFileBean.fileName);
            binding.edName.setText(projectFileBean.fileName);

            if (projectFileBean.fileName.equals("main")) {
                binding.edName.setEnabled(false);
            } else {
                binding.tiName.setHint("Rename");

                binding.edName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (ToolCore.isXMLNameValid(Configs.currentProjectID, s.toString(), (projectFileBean != null ? projectFileBean.fileName : ""), true, null) ||
                                (projectFileBean != null && s.toString().equals(projectFileBean.fileName))) {
                            binding.tiName.setError(null);
                        } else {
                            binding.tiName.setError("Please choose another name.");
                        }
                    }
                });
            }

            binding.edName.setBackgroundResource(R.color.transparent);
            initItem(projectFileBean.options);
            binding.addViewTypeSelectorLayout.setVisibility(View.GONE);
            if (projectFileBean.fileName.endsWith("_fragment")
                    || DayDreamProjectSettings.getActivityType(Configs.currentProjectID, projectFileBean.fileName).contains("_fragment")) {
                binding.viewOrientationSelectorLayout.setVisibility(View.GONE);
                binding.viewKeyboardSettingsSelectorLayout.setVisibility(View.GONE);
                binding.lnDaydreamfeaturesforactivity.setVisibility(View.GONE);
            }
            binding.screenOrientationSelector.check(binding.screenOrientationSelector.getChildAt(projectFileBean.orientation).getId());
            binding.keyboardSettingsSelector.check(binding.keyboardSettingsSelector.getChildAt(projectFileBean.keyboardSetting).getId());

            binding.imgKeyboard.post(() -> {
                if (binding.keyboardSettingsSelector.getCheckedButtonId() == R.id.select_hidden) {
                    slideOutVertically(binding.imgKeyboard);
                }
            });
        } else {
            featureToolbar = true;
            featureStatusBar = true;
            nameValidator = new YB(getApplicationContext(), binding.tiName, uq.b, screenNames);
        }
        initializeItems();
        initializeDayDreamFeature();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DesignActivity.needReloadProjectData) finish();
    }

    private String getSuffix(MaterialButtonToggleGroup toggleGroup) {
        return switch (getSelectedButtonIndex(toggleGroup)) {
            case 1 -> "_fragment";
            case 2 -> "_dialog_fragment";
            case 3 -> "_bottomdialog_fragment";
            default -> "";
        };
    }

    public int getSelectedButtonIndex(MaterialButtonToggleGroup toggleGroup) {
        for (int i = 0; i < toggleGroup.getChildCount(); i++) {
            var button = toggleGroup.getChildAt(i);
            if (toggleGroup.getCheckedButtonIds().contains(button.getId())) {
                return i;
            }
        }
        return -1;  // Return -1 if no button is selected
    }

    public void makeTransitionAnimation(LinearLayout linearLayout) {
        AutoTransition autoTransition = new android.transition.AutoTransition();
        autoTransition.setDuration(200);
        TransitionManager.beginDelayedTransition(linearLayout, autoTransition);
    }

    private static class FeatureItem {
        public int type;
        public int previewImg;
        public String name;
        public boolean isEnabled;

        public FeatureItem(int type, int previewImg, String name, boolean isEnabled) {
            this.type = type;
            this.previewImg = previewImg;
            this.name = name;
            this.isEnabled = isEnabled;
        }
    }

    public class FeaturesAdapter extends RecyclerView.Adapter<FeaturesAdapter.ViewHolder> {
        public int layoutPosition = -1;
        public boolean d;

        public FeaturesAdapter() {
        }

        @Override
        public int getItemCount() {
            return featureItems.size();
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            d = true;
            FeatureItem featureItem = featureItems.get(position);
            viewHolder.t.setImageResource(featureItem.previewImg);
            viewHolder.u.setText(featureItem.name);
            viewHolder.v.setChecked(featureItem.isEnabled);
            switch (featureItem.type) {
                case 0 -> featureStatusBar = featureItem.isEnabled;
                case 1 -> featureToolbar = featureItem.isEnabled;
                case 2 -> featureDrawer = featureItem.isEnabled;
                case 3 -> featureFab = featureItem.isEnabled;
            }

            if (featureFab || featureDrawer) {
                makeTransitionAnimation(binding.parentLayout);
                binding.fabDrawerTipView.setVisibility(View.VISIBLE);
            }

            a(featureItem);
            d = false;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup var1, int var2) {
            View var3 = wB.a(var1.getContext(), R.layout.manage_screen_activity_add_feature_item);
            var3.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(var3);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ImageView t;
            public TextView u;
            public CheckBox v;

            public ViewHolder(View var2) {
                super(var2);
                t = var2.findViewById(R.id.img_icon);
                u = var2.findViewById(R.id.tv_name);
                v = var2.findViewById(R.id.checkbox);
                v.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!d) {
                        layoutPosition = getLayoutPosition();
                        FeatureItem item = featureItems.get(layoutPosition);
                        item.isEnabled = isChecked;
                        if (item.type == 2 || item.isEnabled) {
                            enableToolbar();
                        } else if (item.type == 1 || !item.isEnabled) {
                            disableDrawer();
                        }
                        notifyItemChanged(layoutPosition);
                    }
                });
            }
        }
    }

    private void setManifestViewState(boolean vis) {
        if (vis) {
            binding.viewOrientationSelectorLayout.setVisibility(View.VISIBLE);
            binding.viewKeyboardSettingsSelectorLayout.setVisibility(View.VISIBLE);
            binding.lnDaydreamfeaturesforactivity.setVisibility(View.VISIBLE);
            binding.lnAddSuffix.setVisibility(View.GONE);
        } else {
            binding.viewOrientationSelectorLayout.setVisibility(View.GONE);
            binding.viewKeyboardSettingsSelectorLayout.setVisibility(View.GONE);
            binding.lnDaydreamfeaturesforactivity.setVisibility(View.GONE);
            binding.lnAddSuffix.setVisibility(View.VISIBLE);
            binding.cbAddSuffix.setText("Add " + getSuffix(binding.viewTypeSelector) + " suffix.");
        }
    }
}
