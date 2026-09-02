package com.besome.sketch.editor.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.view.AddCustomViewActivity;
import com.besome.sketch.editor.manage.view.AddViewActivity;
import com.besome.sketch.editor.manage.view.PresetSettingActivity;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import a.a.a.eC;
import a.a.a.hC;
import a.a.a.jC;
import a.a.a.mB;
import a.a.a.rq;
import a.a.a.wq;
import extensions.anbui.daydream.ui.QuickAnimation;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import extensions.anbui.daydream.tools.ToolCore;
import extensions.anbui.daydream.ui.UIController;
import pro.sketchware.R;
import pro.sketchware.databinding.FileSelectorPopupSelectXmlActivityItemBinding;
import pro.sketchware.databinding.FileSelectorPopupSelectXmlBinding;
import pro.sketchware.utility.UI;

//DR
public class ViewSelectorActivity extends BaseAppCompatActivity {
    private final int[] x = new int[19];
    private final int TAB_ACTIVITY = ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY;
    private final int TAB_FRAGMENT = ProjectFileBean.PROJECT_FILE_TYPE_FRAGMENT;
    private final int TAB_CUSTOM_VIEW = ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW;
    private ViewSelectorAdapter viewSelectorAdapter;
    private String sc_id;
    private ProjectFileBean projectFile;
    private String currentXml;
    private int selectedTab;
    private boolean isCustomView = false;
    private FileSelectorPopupSelectXmlBinding binding;

    private List<ProjectFileBean> listViews = new ArrayList<>();
    private List<Integer> truePosition = new ArrayList<>();

    private int getViewIcon(int i) {
        String replace = String.format("%4s", Integer.toBinaryString(i)).replace(' ', '0');
        return getApplicationContext().getResources().getIdentifier("activity_" + replace, "drawable", getApplicationContext().getPackageName());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.ani_fade_in, R.anim.ani_fade_out);
    }

    private ArrayList<String> getScreenNames() {
        ArrayList<String> screenNames = new ArrayList<>();
        ArrayList<ProjectFileBean> activities = jC.b(sc_id).b();
        if (activities != null) {
            for (ProjectFileBean projectFileBean : activities) {
                screenNames.add(projectFileBean.fileName);
            }
        }
        ArrayList<ProjectFileBean> customViews = jC.b(sc_id).c();
        if (customViews != null) {
            for (ProjectFileBean projectFileBean : customViews) {
                screenNames.add(projectFileBean.fileName);
            }
        }
        return screenNames;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 264:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean projectFile = data.getParcelableExtra("project_file");
                    jC.b(sc_id).a(projectFile);
                    if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                        jC.b(sc_id).a(2, projectFile.getDrawerName());
                    }
                    if (data.hasExtra("preset_views")) {
                        a(projectFile, data.getParcelableArrayListExtra("preset_views"));
                    }
                    jC.b(sc_id).j();
                    jC.b(sc_id).l();
                    viewSelectorAdapter.refreshData();
                }
                break;
            case 265:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean projectFile = data.getParcelableExtra("project_file");
                    ProjectFileBean activity = jC.b(sc_id).b().get(viewSelectorAdapter.selectedItem);
                    activity.keyboardSetting = projectFile.keyboardSetting;
                    activity.orientation = projectFile.orientation;
                    activity.options = projectFile.options;
                    if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                        if (jC.b(sc_id).b(projectFile.getDrawerXmlName()) == null) {
                            jC.b(sc_id).a(2, projectFile.getDrawerName());
                        }
                    } else {
                        jC.b(sc_id).b(2, projectFile.getDrawerName());
                    }
                    if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)
                            || projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                        jC.c(sc_id).c().useYn = "Y";
                    }
                    viewSelectorAdapter.notifyItemChanged(viewSelectorAdapter.selectedItem);
                    Intent intent = new Intent();
                    intent.putExtra("project_file", projectFile);
                    setResult(RESULT_OK, intent);
                }
                break;
            case 266:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean projectFile = data.getParcelableExtra("project_file");
                    jC.b(sc_id).a(projectFile);
                    if (data.hasExtra("preset_views")) {
                        a(projectFile, data.getParcelableArrayListExtra("preset_views"));
                    }
                    jC.b(sc_id).j();
                    jC.b(sc_id).l();
                    viewSelectorAdapter.refreshData();
                }
                break;
            case 276:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean presetData = data.getParcelableExtra("preset_data");
                    ProjectFileBean activity = jC.b(sc_id).b().get(viewSelectorAdapter.selectedItem);
                    activity.keyboardSetting = presetData.keyboardSetting;
                    activity.orientation = presetData.orientation;
                    activity.options = presetData.options;
                    if (presetData.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)
                            || presetData.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                        jC.c(sc_id).c().useYn = "Y";
                    }
                    a(presetData, activity, requestCode);
                    jC.b(sc_id).j();
                    viewSelectorAdapter.refreshData();
                    Intent intent2 = new Intent();
                    intent2.putExtra("project_file", activity);
                    setResult(RESULT_OK, intent2);
                }
                break;
            case 277:
            case 278:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean presetData = data.getParcelableExtra("preset_data");
                    ProjectFileBean customView = jC.b(sc_id).c().get(viewSelectorAdapter.selectedItem);
                    a(presetData, customView, requestCode);
                    jC.b(sc_id).j();
                    viewSelectorAdapter.refreshData();
                    Intent intent3 = new Intent();
                    intent3.putExtra("project_file", customView);
                    setResult(RESULT_OK, intent3);
                }
                break;
            default:
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = FileSelectorPopupSelectXmlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            sc_id = intent.getStringExtra("sc_id");
            currentXml = intent.getStringExtra("current_xml");
            isCustomView = intent.getBooleanExtra("is_custom_view", false);
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            currentXml = savedInstanceState.getString("current_xml");
            isCustomView = savedInstanceState.getBoolean("is_custom_view");
        }

        if (isCustomView) {
            selectedTab = TAB_CUSTOM_VIEW;
        } else {
            selectedTab = TAB_ACTIVITY;
        }

        binding.optionsSelector.check(selectedTab == TAB_ACTIVITY ? R.id.option_view : R.id.option_custom_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        viewSelectorAdapter = new ViewSelectorAdapter();
        binding.listXml.setLayoutManager(layoutManager);
        binding.listXml.setHasFixedSize(true);
        binding.listXml.setAdapter(viewSelectorAdapter);
        binding.listXml.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    binding.createNewView.hide();
                } else {
                    binding.createNewView.show();
                }
            }
        });

        viewSelectorAdapter.refreshData();

        if (getIntent().getBooleanExtra("isOnlyJava", false)) {
            findViewById(R.id.option_custom_view).setVisibility(View.GONE);
        }

        binding.optionsSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.option_view) {
                    selectedTab = TAB_ACTIVITY;
                } else if (checkedId == R.id.option_custom_view) {
                    selectedTab = TAB_CUSTOM_VIEW;
                } else if (checkedId == R.id.option_fragment_view) {
                    selectedTab = TAB_FRAGMENT;
                }
                viewSelectorAdapter.refreshData();
            }
        });

        binding.createNewView.setOnClickListener(v -> {
            if (!mB.a()) {
                if (selectedTab == TAB_ACTIVITY || selectedTab == TAB_FRAGMENT) {
                    Intent intent = new Intent(getApplicationContext(), AddViewActivity.class);
                    intent.putStringArrayListExtra("screen_names", getScreenNames());
                    intent.putExtra("request_code", 264);
                    startActivityForResult(intent, 264);
                } else if (selectedTab == TAB_CUSTOM_VIEW) {
                    Intent intent = new Intent(getApplicationContext(), AddCustomViewActivity.class);
                    intent.putStringArrayListExtra("screen_names", getScreenNames());
                    startActivityForResult(intent, 266);

                }
            }
        });
        binding.container.setOnClickListener(v -> finish());
        overridePendingTransition(R.anim.ani_fade_in, R.anim.ani_fade_out);

        UI.addSystemWindowInsetToPadding(binding.container, true, true, true, false);
        UI.addSystemWindowInsetToMargin(binding.createNewView, false, false, false, true);

        UIController.setLightStatusBar(false, this);

        if (ToolCore.isFragment(Configs.currentProjectID, currentXml))
            binding.optionsSelector.check(R.id.option_fragment_view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DesignActivity.needReloadProjectData) finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putString("current_xml", currentXml);
        outState.putBoolean("is_custom_view", isCustomView);
        super.onSaveInstanceState(outState);
    }

    private void a(ProjectFileBean projectFile, ArrayList<ViewBean> presetViews) {
        jC.a(sc_id);
        for (ViewBean view : eC.a(presetViews)) {
            view.id = a(view.type, projectFile.getXmlName());
            jC.a(sc_id).a(projectFile.getXmlName(), view);
            if (view.type == ViewBean.VIEW_TYPE_WIDGET_BUTTON
                    && projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                jC.a(sc_id).a(projectFile.getJavaName(), 1, view.type, view.id, "onClick");
            }
        }
    }

    private void a(ProjectFileBean presetData, ProjectFileBean projectFile, int requestCode) {
        ArrayList<ViewBean> d = jC.a(sc_id).d(projectFile.getXmlName());
        for (int size = d.size() - 1; size >= 0; size--) {
            jC.a(sc_id).a(projectFile, d.get(size));
        }
        ArrayList<ViewBean> a = a(presetData.presetName, requestCode);
        jC.a(sc_id);
        for (ViewBean view : eC.a(a)) {
            view.id = a(view.type, projectFile.getXmlName());
            jC.a(sc_id).a(projectFile.getXmlName(), view);
            if (view.type == ViewBean.VIEW_TYPE_WIDGET_BUTTON
                    && projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                jC.a(sc_id).a(projectFile.getJavaName(), 1, view.type, view.id, "onClick");
            }
        }
    }

    private ArrayList<ViewBean> a(String presetName, int requestCode) {
        ArrayList<ViewBean> views = new ArrayList<>();
        return switch (requestCode) {
            case 276 -> rq.f(presetName);
            case 277 -> rq.b(presetName);
            case 278 -> rq.d(presetName);
            default -> views;
        };
    }

    private String a(int viewType, String xmlName) {
        String b = wq.b(viewType);
        StringBuilder sb = new StringBuilder();
        sb.append(b);
        int i2 = x[viewType] + 1;
        x[viewType] = i2;
        sb.append(i2);
        String sb2 = sb.toString();
        ArrayList<ViewBean> d = jC.a(sc_id).d(xmlName);
        while (true) {
            boolean z = false;
            for (ViewBean viewBean : d) {
                if (sb2.equals(viewBean.id)) {
                    z = true;
                    break;
                }
            }
            if (!z) {
                return sb2;
            }
            sb = new StringBuilder();
            sb.append(b);
            i2 = x[viewType] + 1;
            x[viewType] = i2;
            sb.append(i2);
            sb2 = sb.toString();
        }
    }

    private int a(ProjectFileBean projectFileBean) {
        if (selectedTab == 0) {
            return 276;
        }
        return projectFileBean.fileType == ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW ? 277 : 278;
    }

    private class ViewSelectorAdapter extends RecyclerView.Adapter<ViewSelectorAdapter.ViewHolder> {
        private int selectedItem = -1;

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            if (selectedTab == TAB_ACTIVITY || selectedTab == TAB_FRAGMENT) {
                ProjectFileBean projectFileBean = listViews.get(position);
                String xmlName = projectFileBean.getXmlName();

                viewHolder.itemBinding.tvFilename.setVisibility(View.VISIBLE);
                viewHolder.itemBinding.tvLinkedFilename.setVisibility(View.VISIBLE);

                if (currentXml.equals(xmlName)) {
                    viewHolder.itemBinding.cardView.setScaleX(1f);
                    viewHolder.itemBinding.cardView.setScaleY(1f);
                    //viewHolder.itemBinding.cardView.setAlpha(1f);
                    viewHolder.itemBinding.lnSelected.setVisibility(View.VISIBLE);
                    QuickAnimation.scaleOutForViewSelected(viewHolder.itemBinding.cardView);
                } else {
                    viewHolder.itemBinding.cardView.setScaleX(0.95f);
                    viewHolder.itemBinding.cardView.setScaleY(0.95f);
                    //viewHolder.itemBinding.cardView.setAlpha(0.95f);
                    viewHolder.itemBinding.lnSelected.setVisibility(View.GONE);
                }

                String javaName = projectFileBean.getJavaName();
                viewHolder.itemBinding.imgEdit.setVisibility(View.VISIBLE);
                viewHolder.itemBinding.imgView.setImageResource(getViewIcon(projectFileBean.options));
                viewHolder.itemBinding.tvLinkedFilename.setVisibility(View.VISIBLE);

                if (getIntent().getBooleanExtra("isOnlyJava", false)) {
                    viewHolder.itemBinding.tvFilename.setText(javaName);
                    viewHolder.itemBinding.tvLinkedFilename.setText(xmlName);
                } else {
                    viewHolder.itemBinding.tvFilename.setText(xmlName);
                    viewHolder.itemBinding.tvLinkedFilename.setText(javaName);
                }

            } else if (selectedTab == TAB_CUSTOM_VIEW) {
                viewHolder.itemBinding.imgEdit.setVisibility(View.GONE);
                viewHolder.itemBinding.tvLinkedFilename.setVisibility(View.GONE);
                ProjectFileBean customView = listViews.get(position);

                if (currentXml.equals(customView.getXmlName())) {
                    //viewHolder.itemBinding.cardView.setScaleX(1f);
                    //viewHolder.itemBinding.cardView.setScaleY(1f);
                    //viewHolder.itemBinding.cardView.setAlpha(1f);
                    viewHolder.itemBinding.lnSelected.setVisibility(View.VISIBLE);
                    QuickAnimation.scaleOutForViewSelected(viewHolder.itemBinding.cardView);
                } else {
                    viewHolder.itemBinding.cardView.setScaleX(0.95f);
                    viewHolder.itemBinding.cardView.setScaleY(0.95f);
                    //viewHolder.itemBinding.cardView.setAlpha(0.95f);
                    viewHolder.itemBinding.lnSelected.setVisibility(View.GONE);
                }


                if (customView.fileType == ProjectFileBean.PROJECT_FILE_TYPE_DRAWER) {
                    viewHolder.itemBinding.imgView.setImageResource(getViewIcon(4));
                    viewHolder.itemBinding.tvFilename.setText(customView.fileName.substring(1));
                } else {
                    viewHolder.itemBinding.imgView.setImageResource(getViewIcon(3));
                    viewHolder.itemBinding.tvFilename.setText(customView.getXmlName());
                }
            }
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            FileSelectorPopupSelectXmlActivityItemBinding binding = FileSelectorPopupSelectXmlActivityItemBinding.inflate(inflater, parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public int getItemCount() {
            return listViews.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final FileSelectorPopupSelectXmlActivityItemBinding itemBinding;

            public ViewHolder(@NonNull FileSelectorPopupSelectXmlActivityItemBinding binding) {
                super(binding.getRoot());
                itemBinding = binding;
                itemBinding.cardView.setOnClickListener(v -> {
                    if (!mB.a()) {
                        int pos = getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;
                        selectedItem = truePosition.get(pos);

                        hC hC = jC.b(sc_id);
                        ArrayList<ProjectFileBean> list = switch (selectedTab) {
                            case TAB_ACTIVITY -> hC.b();
                            case TAB_CUSTOM_VIEW -> hC.c();
                            case TAB_FRAGMENT -> hC.b();
                            default -> null;
                        };
                        if (list != null) {
                            projectFile = list.get(selectedItem);
                        }
                        Intent intent = new Intent();
                        intent.putExtra("project_file", projectFile);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
                itemBinding.actionContainer.setOnClickListener(v -> {
                    if ((selectedTab == TAB_ACTIVITY || selectedTab == TAB_FRAGMENT) && !mB.a()) {
                        int pos = getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;
                        selectedItem = truePosition.get(pos);

                        Intent intent = new Intent(getApplicationContext(), AddViewActivity.class);
                        intent.putExtra("project_file", jC.b(sc_id).b().get(selectedItem));
                        intent.putExtra("request_code", 265);
                        startActivityForResult(intent, 265);
                    }
                });
                itemBinding.imgPresetSetting.setOnClickListener(v -> {
                    if (!mB.a()) {
                        int pos = getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;
                        selectedItem = truePosition.get(pos);

                        int requestCode = a(jC.b(sc_id).b().get(selectedItem));
                        Intent intent = new Intent(getApplicationContext(), PresetSettingActivity.class);
                        intent.putExtra("request_code", requestCode);
                        intent.putExtra("edit_mode", true);
                        startActivityForResult(intent, requestCode);
                    }
                });
            }
        }

        public void refreshData() {
            loadData();
            notifyDataSetChanged();
            binding.listXml.post(ViewSelectorActivity.this::movetoCurrentXmlName);
        }
    }

    private void movetoCurrentXmlName() {
        if (!(currentXml == null || currentXml.isEmpty() || listViews == null || listViews.isEmpty())) {
            int position = -1;

            for (int i = 0; i < listViews.size(); i++) {
                ProjectFileBean bean = listViews.get(i);
                if (currentXml.equals(bean.getXmlName())) {
                    position = i;
                }
            }

            try {
                if (position != -1) {
                    LinearLayoutManager lm = (LinearLayoutManager) binding.listXml.getLayoutManager();
                    if (lm != null) {
                        int offset = binding.listXml.getHeight() / 2
                                - binding.listXml.getChildAt(0).getHeight() / 2;
                        lm.scrollToPositionWithOffset(position, offset);
                    }
                }
            } catch (Exception ex) {

            }
        }
    }

    public void loadData() {
        hC hC = jC.b(sc_id);
        ArrayList<ProjectFileBean> allList;

        if (selectedTab == TAB_ACTIVITY || selectedTab == TAB_FRAGMENT) {
            allList = hC.b();
        } else {
            allList = hC.c();
        }
        listViews.clear();
        truePosition.clear();

        if (allList != null && !allList.isEmpty()) {
            for (int i = 0; i < allList.size(); i++) {
                ProjectFileBean bean = allList.get(i);
                String xmlName = bean.getXmlName();

                String activityType = DayDreamProjectSettings.getActivityType(
                        Configs.currentProjectID,
                        xmlName.replace(".xml", "")
                );
                boolean isFragment = xmlName.endsWith("_fragment.xml") || activityType.contains("_fragment");

                if (selectedTab == TAB_ACTIVITY && isFragment) continue;
                if (selectedTab == TAB_FRAGMENT && !isFragment) continue;

                truePosition.add(i);
                listViews.add(bean);
            }
        }

        binding.emptyMessage.setVisibility((listViews.isEmpty() ? View.VISIBLE : View.GONE));
    }
}