package com.besome.sketch.design;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.MaterialShapeUtils;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.io.File;
import java.util.HashMap;

import a.a.a.lC;
import a.a.a.wq;
import a.a.a.yB;
import mod.hey.studios.project.ProjectSettings;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DesignDrawerItemBinding;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.ThemeUtils;
import pro.sketchware.utility.UI;

//DR
public class DesignDrawer extends LinearLayout {

    private final LinearLayout headerContainer;
    private final LinearLayout content;

    private static Activity findActivity(Context context) {
        if (context == null) return null;
        if (context instanceof Activity activity) return activity;
        if (context instanceof ContextWrapper wrapper) {
            return findActivity(wrapper.getBaseContext());
        }
        return null;
    }

    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener drawerItemClickListener = v -> {
        Activity activity = findActivity(getContext());
        if (!(activity instanceof DesignActivity designActivity)) return;
        int id = v.getId();

        if (id == R.id.item_library_manager) {
            designActivity.toLibraryManager();
        } else if (id == R.id.item_daydream) {
            designActivity.toDayDream();
        } else if (id == R.id.item_view_manager) {
            designActivity.toViewManager();
        } else if (id == R.id.item_image_manager) {
            designActivity.toImageManager();
        } else if (id == R.id.item_sound_manager) {
            designActivity.toSoundManager();
        } else if (id == R.id.item_font_manager) {
            designActivity.toFontManager();
        } else if (id == R.id.item_native_manager) {
            designActivity.toNativeManager();
        } else if (id == R.id.item_resource_editor) {
            designActivity.toResourceEditor();
        } else if (id == R.id.item_assets_manager) {
            designActivity.toAssetManager();
        } else if (id == R.id.item_permission_manager) {
            designActivity.toPermissionManager();
        } else if (id == R.id.item_appcompat_manager) {
            designActivity.toAppCompatInjectionManager();
        } else if (id == R.id.item_manifest_manager) {
            designActivity.toAndroidManifestManager();
        } else if (id == R.id.item_used_custom_blocks) {
            designActivity.toCustomBlocksViewer();
        } else if (id == R.id.item_code_shrinking_manager) {
            designActivity.toProguardManager();
        } else if (id == R.id.item_stringfog_manager) {
            designActivity.toStringFogManager();
        } else if (id == R.id.item_show_src) {
            designActivity.toSourceCodeViewer();
        } else if (id == R.id.item_xml_command_manager) {
            designActivity.toXMLCommandManager();
        } else if (id == R.id.item_logcat_reader) {
            designActivity.toLogReader();
        } else if (id == R.id.item_collection_manager) {
            designActivity.toCollectionManager();
        } else if (id == R.id.item_aidl_manager) {
            designActivity.toAidlManager();
        } else if (id == R.id.item_app_settings) {
            designActivity.toAppSettings();
        } else {
            throw new IllegalArgumentException("Invalid item id: " + id);
        }
    };

    public DesignDrawer(Context context) {
        this(context, null);
    }

    @SuppressLint("NonConstantResourceId")
    public DesignDrawer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setFocusable(true);
        setClickable(true);

        ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(SketchwareUtil.getDip(28))
                .setBottomLeftCornerSize(SketchwareUtil.getDip(28))
                .build();

        MaterialShapeDrawable background = new MaterialShapeDrawable(shape);
        background.setFillColor(ColorStateList.valueOf(ThemeUtils.getColor(context, R.attr.colorSurfaceContainerLow)));
        background.initializeElevationOverlay(context);
        setBackground(background);
        setElevation(3f);

        UI.addSystemWindowInsetToPadding(this, false, true, true, true);

        headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(VERTICAL);
        addView(headerContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        MaterialDivider topDivider = new MaterialDivider(context);
        topDivider.setDividerColor(MaterialColors.getColor(topDivider, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        addView(topDivider, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setVerticalScrollBarEnabled(false);
        addView(scrollView, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));

        content = new LinearLayout(getContext());
        content.setOrientation(VERTICAL);
        content.setPadding(0, SketchwareUtil.dpToPx(6), 0, SketchwareUtil.dpToPx(8));
        scrollView.addView(content);

        refreshDrawerItems();

        MaterialDivider bottomDivider = new MaterialDivider(context);
        bottomDivider.setDividerColor(MaterialColors.getColor(bottomDivider, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        addView(bottomDivider, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout footerCard = createSectionCard(this, 8, 8);
        addDrawerItem(R.id.item_collection_manager, R.drawable.ic_mtrl_bookmark, R.string.design_drawer_menu_title_collection, R.string.design_drawer_menu_description_collection, footerCard, false);
        addGroupDivider(footerCard);
        addDrawerItem(R.id.item_app_settings, R.drawable.ic_mtrl_settings, "Settings", "Sketchware app settings", footerCard);
    }

    public void refreshDrawerItems() {
        if (content == null || headerContainer == null) return;
        headerContainer.removeAllViews();
        content.removeAllViews();

        String drawerScId = DesignActivity.sc_id;
        addHeaderCard(drawerScId);

        addSectionHeader(getContext().getString(R.string.design_drawer_menu_title), content);
        LinearLayout projectSection = createSectionCard(content, 0, 10);
        addDrawerItem(R.id.item_library_manager, R.drawable.ic_mtrl_category, R.string.design_drawer_menu_title_library, R.string.design_drawer_menu_description_library, projectSection, false);
        addGroupDivider(projectSection);
        addDrawerItem(R.id.item_daydream, R.drawable.wand_stars_24px, R.string.daydream, R.string.daydream_subtitile, projectSection, true);
        addGroupDivider(projectSection);
        addDrawerItem(R.id.item_view_manager, R.drawable.ic_mtrl_devices, R.string.design_drawer_menu_title_view, R.string.design_drawer_menu_description_view, projectSection, false);

        addSectionHeader("Resources & Code", content);
        LinearLayout resourcesSection = createSectionCard(content, 0, 10);
        addDrawerItem(R.id.item_image_manager, R.drawable.ic_mtrl_image, R.string.design_drawer_menu_title_image, R.string.design_drawer_menu_description_image, resourcesSection, false);
        addGroupDivider(resourcesSection);
        addDrawerItem(R.id.item_sound_manager, R.drawable.ic_mtrl_music, R.string.design_drawer_menu_title_sound, R.string.design_drawer_menu_description_sound, resourcesSection, false);
        addGroupDivider(resourcesSection);
        addDrawerItem(R.id.item_font_manager, R.drawable.ic_mtrl_font, R.string.design_drawer_menu_title_font, R.string.design_drawer_menu_description_font, resourcesSection, false);

        if (!TextUtils.isEmpty(drawerScId)) {
            ProjectSettings ps = new ProjectSettings(drawerScId);
            if ("true".equals(ps.getValue(ProjectSettings.SETTING_ENABLE_NATIVE, "false"))) {
                addGroupDivider(resourcesSection);
                addDrawerItem(R.id.item_native_manager, R.drawable.ic_mtrl_cpp, R.string.text_title_menu_native, R.string.text_subtitle_menu_native, resourcesSection, false);
            }
        }

        addGroupDivider(resourcesSection);
        addDrawerItem(R.id.item_aidl_manager, R.drawable.ic_mtrl_code, "AIDL Manager", "Manage AIDL interface files", resourcesSection);
        addGroupDivider(resourcesSection);
        addDrawerItem(R.id.item_resource_editor, R.drawable.ic_mtrl_folder_code, R.string.text_title_menu_resource_editor, R.string.text_subtitle_menu_resource_editor, resourcesSection, false);
        addGroupDivider(resourcesSection);
        addDrawerItem(R.id.item_assets_manager, R.drawable.ic_mtrl_file_present, R.string.text_title_menu_assets, R.string.text_subtitle_menu_assets, resourcesSection, false);

        addSectionHeader("Build & Developer Tools", content);
        LinearLayout toolsSection = createSectionCard(content, 0, 8);
        addDrawerItem(R.id.item_permission_manager, R.drawable.ic_mtrl_shield_check, R.string.text_title_menu_permission, R.string.text_subtitle_menu_permission, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_appcompat_manager, R.drawable.ic_mtrl_inject, R.string.design_drawer_menu_injection, R.string.design_drawer_menu_injection_subtitle, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_manifest_manager, R.drawable.ic_mtrl_deployed_code, R.string.design_drawer_menu_androidmanifest, R.string.design_drawer_menu_androidmanifest_subtitle, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_used_custom_blocks, R.drawable.ic_mtrl_block, R.string.design_drawer_menu_customblocks, R.string.design_drawer_menu_customblocks_subtitle, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_code_shrinking_manager, R.drawable.ic_mtrl_shield_lock, R.string.design_drawer_menu_proguard, R.string.design_drawer_menu_proguard_subtitle, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_stringfog_manager, R.drawable.ic_mtrl_regular_expression, R.string.design_drawer_menu_stringfog, R.string.design_drawer_menu_stringfog_subtitle, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_show_src, R.drawable.ic_mtrl_frame_source, R.string.design_drawer_menu_title_source_code, R.string.design_drawer_menu_description_source_code, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_xml_command_manager, R.drawable.ic_mtrl_code, R.string.design_drawer_menu_title_xml_command, R.string.design_drawer_menu_description_xml_command, toolsSection, false);
        addGroupDivider(toolsSection);
        addDrawerItem(R.id.item_logcat_reader, R.drawable.ic_mtrl_article, R.string.design_drawer_menu_title_logcat_reader, R.string.design_drawer_menu_subtitle_logcat_reader, toolsSection, false);
    }

    private void addHeaderCard(String drawerScId) {
        Context context = getContext();
        MaterialCardView headerCard = new MaterialCardView(context);
        LayoutParams cardLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(SketchwareUtil.dpToPx(12), SketchwareUtil.dpToPx(8), SketchwareUtil.dpToPx(12), SketchwareUtil.dpToPx(10));
        headerCard.setLayoutParams(cardLp);
        headerCard.setRadius(SketchwareUtil.dpToPx(20));
        headerCard.setCardElevation(0);
        headerCard.setStrokeWidth(0);
        headerCard.setCardBackgroundColor(MaterialColors.getColor(headerCard, com.google.android.material.R.attr.colorSurfaceContainerHigh));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = SketchwareUtil.dpToPx(14);
        int padV = SketchwareUtil.dpToPx(12);
        row.setPadding(padH, padV, padH, padV);
        headerCard.addView(row);

        MaterialCardView badge = new MaterialCardView(context);
        int badgeSize = SketchwareUtil.dpToPx(40);
        badge.setLayoutParams(new LayoutParams(badgeSize, badgeSize));
        badge.setRadius(SketchwareUtil.dpToPx(12));
        badge.setCardElevation(0);
        badge.setStrokeWidth(0);

        String resolvedTitle = null;
        String subtitleText = "Project Tools";
        Bitmap customIconBitmap = null;
        if (!TextUtils.isEmpty(drawerScId)) {
            try {
                HashMap<String, Object> projectInfo = lC.b(drawerScId);
                String wsName = yB.c(projectInfo, "my_ws_name");
                if (!TextUtils.isEmpty(wsName)) {
                    resolvedTitle = wsName;
                }
                subtitleText = "Project #" + drawerScId + " · " + context.getString(R.string.design_drawer_menu_title);
                if (yB.a(projectInfo, "custom_icon")) {
                    File iconFile = new File(wq.e() + File.separator + drawerScId, "icon.png");
                    if (iconFile.exists()) {
                        customIconBitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        ImageView icon = new ImageView(context);
        if (customIconBitmap != null) {
            badge.setCardBackgroundColor(Color.TRANSPARENT);
            MaterialCardView.LayoutParams iconLp = new MaterialCardView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            icon.setLayoutParams(iconLp);
            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            icon.setImageBitmap(customIconBitmap);
        } else {
            badge.setCardBackgroundColor(MaterialColors.getColor(badge, com.google.android.material.R.attr.colorPrimaryContainer));
            int iconSize = SketchwareUtil.dpToPx(22);
            MaterialCardView.LayoutParams iconLp = new MaterialCardView.LayoutParams(iconSize, iconSize);
            iconLp.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconLp);
            icon.setImageResource(R.drawable.ic_mtrl_android);
            icon.setColorFilter(MaterialColors.getColor(icon, com.google.android.material.R.attr.colorOnPrimaryContainer));
        }
        badge.addView(icon);
        row.addView(badge);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(VERTICAL);
        LayoutParams textColLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        textColLp.setMarginStart(SketchwareUtil.dpToPx(12));
        textCol.setLayoutParams(textColLp);

        String titleText = resolvedTitle != null ? resolvedTitle : context.getString(R.string.design_drawer_menu_title);

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setTextColor(MaterialColors.getColor(title, com.google.android.material.R.attr.colorOnSurface));
        textCol.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText(subtitleText);
        subtitle.setMaxLines(1);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        subtitle.setTextColor(MaterialColors.getColor(subtitle, com.google.android.material.R.attr.colorOnSurfaceVariant));
        textCol.addView(subtitle);

        row.addView(textCol);
        headerContainer.addView(headerCard);
    }

    private LinearLayout createSectionCard(ViewGroup parent, int topMarginDp, int bottomMarginDp) {
        Context context = getContext();
        MaterialCardView card = new MaterialCardView(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(SketchwareUtil.dpToPx(12), SketchwareUtil.dpToPx(topMarginDp), SketchwareUtil.dpToPx(12), SketchwareUtil.dpToPx(bottomMarginDp));
        card.setLayoutParams(lp);
        card.setRadius(SketchwareUtil.dpToPx(20));
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurfaceContainer));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        card.addView(container, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        parent.addView(card);
        return container;
    }

    private void addSectionHeader(String titleText, ViewGroup view) {
        TextView subheader = new TextView(getContext());
        subheader.setEllipsize(TextUtils.TruncateAt.END);
        subheader.setMaxLines(1);
        subheader.setGravity(Gravity.CENTER_VERTICAL);
        subheader.setText(titleText);
        subheader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subheader.setTypeface(subheader.getTypeface(), android.graphics.Typeface.BOLD);
        subheader.setTextColor(MaterialColors.getColor(subheader, R.attr.colorPrimary));

        LayoutParams textLp = new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = SketchwareUtil.dpToPx(6);
        textLp.bottomMargin = SketchwareUtil.dpToPx(6);
        textLp.setMarginStart(SketchwareUtil.dpToPx(20));

        subheader.setLayoutParams(textLp);
        view.addView(subheader);
    }

    private void addGroupDivider(ViewGroup view) {
        MaterialDivider divider = new MaterialDivider(getContext());
        divider.setDividerInsetStart(SketchwareUtil.dpToPx(64));
        divider.setDividerInsetEnd(SketchwareUtil.dpToPx(14));
        divider.setDividerColor(MaterialColors.getColor(divider, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        view.addView(divider, new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            refreshDrawerItems();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MaterialShapeUtils.setParentAbsoluteElevation(this);
    }

    @Override
    public void setElevation(float elevation) {
        super.setElevation(elevation);
        MaterialShapeUtils.setElevation(this, elevation);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int maxWidth = SketchwareUtil.dpToPx(316);
        int adjustedSpec = switch (MeasureSpec.getMode(widthSpec)) {
            case MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(widthSpec), maxWidth), MeasureSpec.EXACTLY);
            case MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
            default -> widthSpec;
        };
        super.onMeasure(adjustedSpec, heightSpec);
    }

    private void addDrawerItem(int id, int iconResId, int titleResId, int descriptionResId, ViewGroup view, boolean highlight) {
        DrawerItem drawerItem = new DrawerItem(getContext());
        drawerItem.setContent(iconResId, Helper.getResString(drawerItem, titleResId), Helper.getResString(drawerItem, descriptionResId), highlight);
        drawerItem.setOnClickListener(id, drawerItemClickListener);
        view.addView(drawerItem);
    }

    private void addDrawerItem(int id, int iconResId, String title, String description, ViewGroup view) {
        DrawerItem drawerItem = new DrawerItem(getContext());
        drawerItem.setContent(iconResId, title, description, false);
        drawerItem.setOnClickListener(id, drawerItemClickListener);
        view.addView(drawerItem);
    }

    private static class DrawerItem extends LinearLayout {
        private final DesignDrawerItemBinding binding;

        public DrawerItem(Context context) {
            this(context, null);
        }

        public DrawerItem(Context context, AttributeSet attrs) {
            super(context, attrs);
            LayoutInflater inflater = LayoutInflater.from(context);
            binding = DesignDrawerItemBinding.inflate(inflater, this, true);
        }

        public void setContent(int iconResId, String rootTitleText, String subTitleText, boolean highlight) {
            binding.imgIcon.setImageResource(iconResId);
            binding.tvRootTitle.setText(rootTitleText);
            binding.tvSubTitle.setText(subTitleText);
            if (highlight) {
                binding.iconContainer.setCardBackgroundColor(MaterialColors.getColor(binding.iconContainer, com.google.android.material.R.attr.colorPrimaryContainer));
                binding.imgIcon.setColorFilter(MaterialColors.getColor(binding.imgIcon, com.google.android.material.R.attr.colorOnPrimaryContainer));
            } else {
                binding.iconContainer.setCardBackgroundColor(MaterialColors.getColor(binding.iconContainer, com.google.android.material.R.attr.colorSurfaceContainerHigh));
                binding.imgIcon.setColorFilter(MaterialColors.getColor(binding.imgIcon, com.google.android.material.R.attr.colorOnSurfaceVariant));
            }
        }

        public void setOnClickListener(int id, OnClickListener listener) {
            binding.getRoot().setId(id);
            binding.getRoot().setOnClickListener(listener);
        }
    }
}
