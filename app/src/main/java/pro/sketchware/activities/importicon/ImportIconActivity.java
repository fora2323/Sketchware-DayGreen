package pro.sketchware.activities.importicon;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.besome.sketch.lib.ui.ColorPickerDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import a.a.a.WB;
import a.a.a.mB;
import a.a.a.uq;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.importicon.adapters.IconAdapter;
import pro.sketchware.activities.importicon.data.IconCatalog;
import pro.sketchware.activities.importicon.data.IconDownloader;
import pro.sketchware.activities.resourceseditor.components.utils.ColorsEditorManager;
import pro.sketchware.databinding.BottomSheetFilterMaterialSymbolsBinding;
import pro.sketchware.databinding.DialogSaveIconBinding;
import pro.sketchware.databinding.ImportIconBinding;
import pro.sketchware.utility.PropertiesUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.SvgUtils;

public class ImportIconActivity extends BaseAppCompatActivity implements IconAdapter.OnIconSelectedListener {

    private static final int ITEMS_PER_PAGE = 40;
    private static final int DEFAULT_SAVE_ICON_SIZE_PX = 24;
    private static final int DEFAULT_ICON_COLOR = Color.parseColor("#9E9E9E");
    private static final String DEFAULT_ICON_COLOR_HEX = "#9E9E9E";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<IconCatalog.IconItem> allCatalogItems = new ArrayList<>();
    private final List<IconCatalog.IconItem> filteredSourceItems = new ArrayList<>();
    private final List<IconCatalog.IconItem> displayedItems = new ArrayList<>();

    private ImportIconBinding binding;
    private IconCatalog iconCatalog;
    private IconDownloader iconDownloader;
    private SvgUtils svgUtils;
    private IconAdapter adapter;

    private String sc_id;
    private ArrayList<String> alreadyAddedImageNames;
    private WB iconNameValidator;
    private MenuItem searchMenuItem;
    private SearchView searchView;

    private String currentQuery = "";
    private int currentPage = 0;
    private boolean isLoadingMore = false;
    private boolean isLastPage = false;

    // Source Modes
    private static final int SOURCE_MATERIAL_SYMBOLS = 0;
    private static final int SOURCE_BRAND = 1;

    private int currentSourceMode = SOURCE_MATERIAL_SYMBOLS;

    // Active Material Symbols variant filters
    private String selectedStyle = IconDownloader.DEFAULT_STYLE;
    private String selectedFillType = IconDownloader.DEFAULT_FILL_TYPE;
    private int selectedOpticalSize = IconDownloader.DEFAULT_OPTICAL_SIZE;

    // Active Brand filters
    private String selectedBrandSet = "simple-icons";

    // Active Save Dialog state
    private int selectedColor = DEFAULT_ICON_COLOR;
    private String selectedColorHex = DEFAULT_ICON_COLOR_HEX;
    private int selectedSaveSizePx = DEFAULT_SAVE_ICON_SIZE_PX;
    private int previewRequestToken = 0;

    private final OnBackPressedCallback searchViewCloser = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
                searchMenuItem.collapseActionView();
            } else {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        }
    };

    private int getGridLayoutColumnCount() {
        float density = getResources().getDisplayMetrics().density;
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels / density;
        int columns = (int) (screenWidthDp / 88f);
        return Math.max(1, columns);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (binding.imageList.getLayoutManager() instanceof GridLayoutManager manager) {
            manager.setSpanCount(getGridLayoutColumnCount());
        }
        binding.imageList.requestLayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ImportIconBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar.toolbar;
        binding.toolbar.layoutMainLogo.setVisibility(View.GONE);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.design_manager_icon_actionbar_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            if (!mB.a()) {
                onBackPressed();
            }
        });

        applyToolbarAndStatusBarColors(toolbar);

        getOnBackPressedDispatcher().addCallback(this, searchViewCloser);

        sc_id = getIntent().getStringExtra("sc_id");
        alreadyAddedImageNames = getIntent().getStringArrayListExtra("imageNames");
        if (alreadyAddedImageNames == null) {
            alreadyAddedImageNames = new ArrayList<>();
        }

        iconCatalog = new IconCatalog(this);
        iconDownloader = new IconDownloader(this);
        svgUtils = new SvgUtils(this);

        binding.imageList.setLayoutManager(new GridLayoutManager(getBaseContext(), getGridLayoutColumnCount()));
        adapter = new IconAdapter(this, iconDownloader, this);
        binding.imageList.setAdapter(adapter);

        binding.filterIconsButton.setOnClickListener(v -> {
            if (!mB.a()) {
                showFilterBottomSheet();
            }
        });

        binding.imageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoadingMore || isLastPage) return;

                if (recyclerView.getLayoutManager() instanceof GridLayoutManager layoutManager) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadNextPageWithIndicator();
                    }
                }
            }
        });

        loadIconCatalog();
    }

    private void applyToolbarAndStatusBarColors(@NonNull Toolbar toolbar) {
        TypedValue typedValue = new TypedValue();
        int surfaceColor = Color.WHITE;
        if (getTheme().resolveAttribute(R.attr.colorSurface, typedValue, true)) {
            surfaceColor = typedValue.data;
        }

        binding.appBar.setBackgroundColor(surfaceColor);
        toolbar.setBackgroundColor(surfaceColor);

        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(surfaceColor);

            double luminance = Color.red(surfaceColor) * 0.299
                    + Color.green(surfaceColor) * 0.587
                    + Color.blue(surfaceColor) * 0.114;
            boolean isLightSurface = luminance > 186;
            WindowInsetsControllerCompat insetsController =
                    new WindowInsetsControllerCompat(window, window.getDecorView());
            insetsController.setAppearanceLightStatusBars(isLightSurface);
        }
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (iconCatalog != null) {
            iconCatalog.shutdown();
        }
        if (iconDownloader != null) {
            iconDownloader.shutdown();
        }
        super.onDestroy();
    }

    private void updateToolbarSubtitle() {
        if (currentSourceMode == SOURCE_MATERIAL_SYMBOLS) {
            String styleText = selectedStyle.substring(0, 1).toUpperCase() + selectedStyle.substring(1);
            if (IconDownloader.FILL_FILLED.equals(selectedFillType)) {
                styleText += " Filled";
            }
            getSupportActionBar().setSubtitle("Material Symbols - " + styleText + " - " + selectedOpticalSize + "px");
        } else {
            String brandText = "simple-icons".equals(selectedBrandSet) ? "Simple Icons" : "Logos";
            getSupportActionBar().setSubtitle("Brand - " + brandText);
        }
    }

    private void loadIconCatalog() {
        binding.progressInitial.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);

        if (currentSourceMode == SOURCE_MATERIAL_SYMBOLS) {
            iconCatalog.loadCatalog(new IconCatalog.CatalogCallback() {
                @Override
                public void onCatalogLoaded(@NonNull List<IconCatalog.IconItem> items, boolean fromNetwork) {
                    if (isDestroyed() || isFinishing()) return;

                    binding.progressInitial.setVisibility(View.GONE);
                    allCatalogItems.clear();
                    allCatalogItems.addAll(items);
                    applyFilterAndResetPaging(currentQuery);
                }

                @Override
                public void onCatalogError(@NonNull String errorMessage) {
                    if (isDestroyed() || isFinishing()) return;

                    binding.progressInitial.setVisibility(View.GONE);
                    SketchwareUtil.toastError(errorMessage);
                    if (allCatalogItems.isEmpty()) {
                        binding.tvEmptyState.setText(errorMessage);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            iconCatalog.loadBrandCatalog(selectedBrandSet, new IconCatalog.CatalogCallback() {
                @Override
                public void onCatalogLoaded(@NonNull List<IconCatalog.IconItem> items, boolean fromNetwork) {
                    if (isDestroyed() || isFinishing()) return;

                    binding.progressInitial.setVisibility(View.GONE);
                    allCatalogItems.clear();
                    allCatalogItems.addAll(items);
                    adapter.updateBrandMode(selectedBrandSet);
                    applyFilterAndResetPaging(currentQuery);
                }

                @Override
                public void onCatalogError(@NonNull String errorMessage) {
                    if (isDestroyed() || isFinishing()) return;

                    binding.progressInitial.setVisibility(View.GONE);
                    SketchwareUtil.toastError(errorMessage);
                    if (allCatalogItems.isEmpty()) {
                        binding.tvEmptyState.setText(errorMessage);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        updateToolbarSubtitle();
    }

    private void applyFilterAndResetPaging(@NonNull String query) {
        currentQuery = query.trim();
        filteredSourceItems.clear();
        filteredSourceItems.addAll(IconCatalog.search(allCatalogItems, currentQuery));

        displayedItems.clear();
        currentPage = 0;
        isLastPage = false;
        isLoadingMore = false;
        binding.progressLoadMore.setVisibility(View.GONE);

        appendNextPage();
        updateEmptyState();
    }

    private void loadNextPageWithIndicator() {
        isLoadingMore = true;
        binding.progressLoadMore.setVisibility(View.VISIBLE);
        mainHandler.postDelayed(() -> {
            if (isDestroyed() || isFinishing()) return;
            appendNextPage();
            binding.progressLoadMore.setVisibility(View.GONE);
            isLoadingMore = false;
        }, 120L);
    }

    private void appendNextPage() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredSourceItems.size());

        if (start < end) {
            List<IconCatalog.IconItem> pageItems = filteredSourceItems.subList(start, end);
            displayedItems.addAll(pageItems);
            adapter.submitList(new ArrayList<>(displayedItems));
            currentPage++;
            if (end >= filteredSourceItems.size()) {
                isLastPage = true;
            }

            if (currentSourceMode == SOURCE_BRAND) {
                List<String> uncachedNames = new ArrayList<>();
                for (IconCatalog.IconItem item : pageItems) {
                    if (!iconDownloader.isBrandCached(selectedBrandSet, item.name)) {
                        uncachedNames.add(item.name);
                    }
                }
                if (!uncachedNames.isEmpty()) {
                    iconDownloader.prefetchBrandBatchAsync(selectedBrandSet, uncachedNames);
                }
            }
        } else {
            if (currentPage == 0) {
                adapter.submitList(new ArrayList<>());
            }
            isLastPage = true;
        }
    }

    private void updateEmptyState() {
        if (filteredSourceItems.isEmpty()) {
            if (currentQuery.isEmpty()) {
                binding.tvEmptyState.setText("No icons available. Connect to the internet and try again.");
            } else {
                binding.tvEmptyState.setText("No icons found matching \"" + currentQuery + "\"");
            }
            binding.tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        BottomSheetFilterMaterialSymbolsBinding sheetBinding =
                BottomSheetFilterMaterialSymbolsBinding.inflate(getLayoutInflater());
        bottomSheet.setContentView(sheetBinding.getRoot());

        // Setup Source selection
        if (currentSourceMode == SOURCE_BRAND) {
            sheetBinding.chipGroupSource.check(R.id.chip_source_brand);
            sheetBinding.containerBrandOptions.setVisibility(View.VISIBLE);
            sheetBinding.containerMaterialSymbolsOptions.setVisibility(View.GONE);
        } else {
            sheetBinding.chipGroupSource.check(R.id.chip_source_material_symbols);
            sheetBinding.containerBrandOptions.setVisibility(View.GONE);
            sheetBinding.containerMaterialSymbolsOptions.setVisibility(View.VISIBLE);
        }

        sheetBinding.chipGroupSource.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_source_brand) {
                sheetBinding.containerBrandOptions.setVisibility(View.VISIBLE);
                sheetBinding.containerMaterialSymbolsOptions.setVisibility(View.GONE);
            } else {
                sheetBinding.containerBrandOptions.setVisibility(View.GONE);
                sheetBinding.containerMaterialSymbolsOptions.setVisibility(View.VISIBLE);
            }
        });

        // Restore active Style selection
        switch (selectedStyle) {
            case IconDownloader.STYLE_ROUNDED -> sheetBinding.chipGroupStyle.check(R.id.chip_style_rounded);
            case IconDownloader.STYLE_SHARP -> sheetBinding.chipGroupStyle.check(R.id.chip_style_sharp);
            default -> sheetBinding.chipGroupStyle.check(R.id.chip_style_outlined);
        }

        // Restore active Fill selection
        sheetBinding.switchFill.setChecked(IconDownloader.FILL_FILLED.equals(selectedFillType));

        // Restore active Optical Size selection
        switch (selectedOpticalSize) {
            case 20 -> sheetBinding.chipGroupOpsz.check(R.id.chip_opsz_20);
            case 24 -> sheetBinding.chipGroupOpsz.check(R.id.chip_opsz_24);
            case 48 -> sheetBinding.chipGroupOpsz.check(R.id.chip_opsz_48);
            default -> sheetBinding.chipGroupOpsz.check(R.id.chip_opsz_40);
        }

        // Restore active Brand Set selection
        if ("logos".equals(selectedBrandSet)) {
            sheetBinding.chipGroupBrandSet.check(R.id.chip_brand_logos);
        } else {
            sheetBinding.chipGroupBrandSet.check(R.id.chip_brand_simple_icons);
        }

        sheetBinding.btnCancelFilter.setOnClickListener(v -> bottomSheet.dismiss());

        sheetBinding.btnApplyFilter.setOnClickListener(v -> {
            int checkedSourceId = sheetBinding.chipGroupSource.getCheckedChipId();
            boolean sourceChanged = false;
            boolean filterChanged = false;

            if (checkedSourceId == R.id.chip_source_brand) {
                if (currentSourceMode != SOURCE_BRAND) {
                    currentSourceMode = SOURCE_BRAND;
                    sourceChanged = true;
                }
                int checkedBrandSetId = sheetBinding.chipGroupBrandSet.getCheckedChipId();
                String newBrandSet = checkedBrandSetId == R.id.chip_brand_logos ? "logos" : "simple-icons";
                if (!newBrandSet.equals(selectedBrandSet)) {
                    selectedBrandSet = newBrandSet;
                    sourceChanged = true;
                }
                
                if (sourceChanged) {
                    adapter.submitList(new ArrayList<>());
                    loadIconCatalog();
                }
            } else {
                if (currentSourceMode != SOURCE_MATERIAL_SYMBOLS) {
                    currentSourceMode = SOURCE_MATERIAL_SYMBOLS;
                    sourceChanged = true;
                }
                
                int checkedStyleId = sheetBinding.chipGroupStyle.getCheckedChipId();
                String newStyle = (checkedStyleId == R.id.chip_style_rounded) ? IconDownloader.STYLE_ROUNDED :
                                  (checkedStyleId == R.id.chip_style_sharp) ? IconDownloader.STYLE_SHARP :
                                  IconDownloader.STYLE_OUTLINED;
                if (!newStyle.equals(selectedStyle)) {
                    selectedStyle = newStyle;
                    filterChanged = true;
                }

                String newFillType = sheetBinding.switchFill.isChecked() ? IconDownloader.FILL_FILLED : IconDownloader.FILL_DEFAULT;
                if (!newFillType.equals(selectedFillType)) {
                    selectedFillType = newFillType;
                    filterChanged = true;
                }

                int checkedOpszId = sheetBinding.chipGroupOpsz.getCheckedChipId();
                int newOpsz = (checkedOpszId == R.id.chip_opsz_20) ? 20 :
                              (checkedOpszId == R.id.chip_opsz_24) ? 24 :
                              (checkedOpszId == R.id.chip_opsz_48) ? 48 : 40;
                if (newOpsz != selectedOpticalSize) {
                    selectedOpticalSize = newOpsz;
                    filterChanged = true;
                }

                if (sourceChanged) {
                    adapter.submitList(new ArrayList<>());
                    loadIconCatalog();
                } else if (filterChanged) {
                    adapter.updateVariant(selectedStyle, selectedFillType, selectedOpticalSize);
                    updateToolbarSubtitle();
                }
            }
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_import_icon, menu);
        searchMenuItem = menu.findItem(R.id.menu_find);
        searchView = (SearchView) searchMenuItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("Search icons or tags");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    applyFilterAndResetPaging(query != null ? query : "");
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    applyFilterAndResetPaging(newText != null ? newText : "");
                    return true;
                }
            });
        }

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                searchViewCloser.setEnabled(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                searchViewCloser.setEnabled(false);
                applyFilterAndResetPaging("");
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onIconSelected(int position) {
        if (mB.a()) return;
        List<IconCatalog.IconItem> currentList = adapter.getCurrentList();
        if (position < 0 || position >= currentList.size()) return;

        showSaveDialog(currentList.get(position));
    }

    private void showSaveDialog(@NonNull IconCatalog.IconItem iconItem) {
        selectedColor = DEFAULT_ICON_COLOR;
        selectedColorHex = DEFAULT_ICON_COLOR_HEX;
        selectedSaveSizePx = DEFAULT_SAVE_ICON_SIZE_PX;

        final String activeStyle = selectedStyle;
        final String activeFill = selectedFillType;
        final int activeOpsz = selectedOpticalSize;

        DialogSaveIconBinding dialogBinding = DialogSaveIconBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle("Save")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Save", null)
                .create();

        String defaultResourceName;
        if (currentSourceMode == SOURCE_BRAND) {
            defaultResourceName = "icon_" + iconItem.name.replaceAll("[^a-zA-Z0-9_]", "_");
        } else {
            String fillSuffix = IconDownloader.FILL_FILLED.equals(activeFill) ? "_filled" : "";
            defaultResourceName = "icon_" + iconItem.name + "_" + activeStyle + fillSuffix;
        }
        
        iconNameValidator = new WB(getApplicationContext(), dialogBinding.textInputLayout, uq.b, alreadyAddedImageNames);
        dialogBinding.inputText.setText(makeUniqueResourceName(defaultResourceName));

        updatePreviewSizePx(dialogBinding.icon, selectedSaveSizePx);
        if (currentSourceMode == SOURCE_BRAND && "logos".equals(selectedBrandSet)) {
            dialogBinding.selectColour.setVisibility(View.GONE);
            dialogBinding.icon.clearColorFilter();
            dialogBinding.icon.setImageTintList(null);
        } else {
            dialogBinding.selectColour.setVisibility(View.VISIBLE);
            updateColorButtonAndPreview(dialogBinding);
        }
        loadDialogPreviewSvg(dialog, dialogBinding, iconItem.name, activeStyle, activeFill, activeOpsz, selectedSaveSizePx);

        dialogBinding.chipGroupSize.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_size_16) {
                selectedSaveSizePx = 16;
            } else if (checkedId == R.id.chip_size_24) {
                selectedSaveSizePx = 24;
            } else if (checkedId == R.id.chip_size_32) {
                selectedSaveSizePx = 32;
            } else if (checkedId == R.id.chip_size_48) {
                selectedSaveSizePx = 48;
            } else if (checkedId == R.id.chip_size_64) {
                selectedSaveSizePx = 64;
            } else if (checkedId == R.id.chip_size_96) {
                selectedSaveSizePx = 96;
            }
            updatePreviewSizePx(dialogBinding.icon, selectedSaveSizePx);
            loadDialogPreviewSvg(dialog, dialogBinding, iconItem.name, activeStyle, activeFill, activeOpsz, selectedSaveSizePx);
        });

        dialogBinding.selectColour.setOnClickListener(view -> {
            ColorPickerDialog colorPicker = new ColorPickerDialog(this, selectedColorHex, false, false, sc_id);
            colorPicker.a(new ColorPickerDialog.b() {
                @Override
                public void a(int color) {
                    selectedColor = color;
                    selectedColorHex = "#" + String.format("%06X", color & 0x00FFFFFF);
                    updateColorButtonAndPreview(dialogBinding);
                }

                @Override
                public void a(String colorName, int color) {
                    selectedColor = color;
                    selectedColorHex = "@color/" + colorName;
                    updateColorButtonAndPreview(dialogBinding);
                }
            });
            colorPicker.materialColorAttr((attr, attrColor) -> {
                String attrRef = "?attr/" + attr;
                selectedColor = PropertiesUtil.parseColor(
                        new ColorsEditorManager().getColorValue(getApplicationContext(), attrRef, 3)
                );
                selectedColorHex = attrRef;
                updateColorButtonAndPreview(dialogBinding);
            });
            colorPicker.showAtLocation(view, Gravity.CENTER, 0, 0);
        });

        dialogBinding.licenceInfo.setOnClickListener(v -> {
            Uri webpage = Uri.parse("https://www.apache.org/licenses/LICENSE-2.0.txt");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (!iconNameValidator.b()) {
                    return;
                }

                String outputResourceName = Helper.getText(dialogBinding.inputText).trim();
                positiveButton.setEnabled(false);
                dialogBinding.progressDownload.setVisibility(View.VISIBLE);

                if (currentSourceMode == SOURCE_BRAND) {
                    iconDownloader.saveBrandResizedSvgToProjectAsync(
                            selectedBrandSet,
                            iconItem.name,
                            sc_id,
                            outputResourceName,
                            selectedSaveSizePx,
                            new IconDownloader.DownloadCallback() {
                                @Override
                                public void onSuccess(@NonNull File savedSvgFile) {
                                    if (isDestroyed() || isFinishing()) return;
                                    dialogBinding.progressDownload.setVisibility(View.GONE);
                                    dialogInterface.dismiss();

                                    Intent intent = new Intent();
                                    intent.putExtra("iconName", outputResourceName);
                                    intent.putExtra("iconPath", savedSvgFile.getAbsolutePath());
                                    intent.putExtra("iconColor", selectedColor);
                                    intent.putExtra("iconColorHex", selectedColorHex);
                                    intent.putExtra("iconSize", selectedSaveSizePx);
                                    setResult(Activity.RESULT_OK, intent);
                                    finish();
                                }

                                @Override
                                public void onError(@NonNull String errorMessage) {
                                    if (isDestroyed() || isFinishing()) return;
                                    dialogBinding.progressDownload.setVisibility(View.GONE);
                                    positiveButton.setEnabled(true);
                                    SketchwareUtil.toastError(errorMessage);
                                }
                            }
                    );
                } else {
                    iconDownloader.saveResizedSvgToProjectAsync(
                            iconItem.name,
                            activeStyle,
                            activeFill,
                            activeOpsz,
                            sc_id,
                            outputResourceName,
                            selectedSaveSizePx,
                            new IconDownloader.DownloadCallback() {
                                @Override
                                public void onSuccess(@NonNull File savedSvgFile) {
                                    if (isDestroyed() || isFinishing()) return;
                                    dialogBinding.progressDownload.setVisibility(View.GONE);
                                    dialogInterface.dismiss();

                                    Intent intent = new Intent();
                                    intent.putExtra("iconName", outputResourceName);
                                    intent.putExtra("iconPath", savedSvgFile.getAbsolutePath());
                                    intent.putExtra("iconColor", selectedColor);
                                    intent.putExtra("iconColorHex", selectedColorHex);
                                    intent.putExtra("iconSize", selectedSaveSizePx);
                                    setResult(Activity.RESULT_OK, intent);
                                    finish();
                                }

                                @Override
                                public void onError(@NonNull String errorMessage) {
                                    if (isDestroyed() || isFinishing()) return;
                                    dialogBinding.progressDownload.setVisibility(View.GONE);
                                    positiveButton.setEnabled(true);
                                    SketchwareUtil.toastError(errorMessage);
                                }
                            }
                    );
                }
            });
        });

        dialog.show();
    }

    private void loadDialogPreviewSvg(
            @NonNull AlertDialog dialog,
            @NonNull DialogSaveIconBinding dialogBinding,
            @NonNull String iconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize,
            int outputSizePx
    ) {
        final int requestId = ++previewRequestToken;
        
        if (currentSourceMode == SOURCE_BRAND) {
            boolean alreadyCached = iconDownloader.isBrandCached(selectedBrandSet, iconName);
            if (!alreadyCached) {
                dialogBinding.icon.setImageDrawable(null);
                dialogBinding.progressDownload.setVisibility(View.VISIBLE);
            }

            iconDownloader.prepareBrandResizedSvgPreviewAsync(
                    selectedBrandSet,
                    iconName,
                    outputSizePx,
                    new IconDownloader.DownloadCallback() {
                        @Override
                        public void onSuccess(@NonNull File resizedPreviewSvg) {
                            if (isDestroyed() || isFinishing() || !dialog.isShowing() || requestId != previewRequestToken) {
                                return;
                            }
                            dialogBinding.progressDownload.setVisibility(View.GONE);
                            svgUtils.loadImage(dialogBinding.icon, resizedPreviewSvg.getAbsolutePath());
                            if ("logos".equals(selectedBrandSet)) {
                                dialogBinding.icon.clearColorFilter();
                                dialogBinding.icon.setImageTintList(null);
                            } else {
                                dialogBinding.icon.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
                            }
                        }

                        @Override
                        public void onError(@NonNull String errorMessage) {
                            if (isDestroyed() || isFinishing() || !dialog.isShowing() || requestId != previewRequestToken) {
                                return;
                            }
                            dialogBinding.progressDownload.setVisibility(View.GONE);
                            dialogBinding.icon.setImageResource(R.drawable.ic_remove_grey600_24dp);
                        }
                    }
            );
        } else {
            boolean alreadyCached = iconDownloader.isCached(iconName, style, fillType, opticalSize);
            if (!alreadyCached) {
                dialogBinding.icon.setImageDrawable(null);
                dialogBinding.progressDownload.setVisibility(View.VISIBLE);
            }

            iconDownloader.prepareResizedSvgPreviewAsync(
                    iconName,
                    style,
                    fillType,
                    opticalSize,
                    outputSizePx,
                    new IconDownloader.DownloadCallback() {
                        @Override
                        public void onSuccess(@NonNull File resizedPreviewSvg) {
                            if (isDestroyed() || isFinishing() || !dialog.isShowing() || requestId != previewRequestToken) {
                                return;
                            }
                            dialogBinding.progressDownload.setVisibility(View.GONE);
                            svgUtils.loadImage(dialogBinding.icon, resizedPreviewSvg.getAbsolutePath());
                            dialogBinding.icon.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
                        }

                        @Override
                        public void onError(@NonNull String errorMessage) {
                            if (isDestroyed() || isFinishing() || !dialog.isShowing() || requestId != previewRequestToken) {
                                return;
                            }
                            dialogBinding.progressDownload.setVisibility(View.GONE);
                            dialogBinding.icon.setImageResource(R.drawable.ic_remove_grey600_24dp);
                        }
                    }
            );
        }
    }
    
    private String makeUniqueResourceName(@NonNull String baseName) {
        if (!alreadyAddedImageNames.contains(baseName)) {
            return baseName;
        }
        int suffix = 2;
        while (alreadyAddedImageNames.contains(baseName + "_" + suffix)) {
            suffix++;
        }
        return baseName + "_" + suffix;
    }

    private void updatePreviewSizePx(@NonNull ImageView previewView, int sizePx) {
        float density = getResources().getDisplayMetrics().density;
        int targetPx = Math.max(1, Math.round(sizePx * density));
        ViewGroup.LayoutParams params = previewView.getLayoutParams();
        if (params != null) {
            params.width = targetPx;
            params.height = targetPx;
            previewView.setLayoutParams(params);
        }
    }

    private void updateColorButtonAndPreview(@NonNull DialogSaveIconBinding dialogBinding) {
        dialogBinding.selectColour.setText(selectedColorHex);
        dialogBinding.selectColour.setBackgroundColor(selectedColor);

        double luminance = Color.red(selectedColor) * 0.299
                + Color.green(selectedColor) * 0.587
                + Color.blue(selectedColor) * 0.114;
        dialogBinding.selectColour.setTextColor(luminance > 186 ? Color.BLACK : Color.WHITE);

        if (currentSourceMode == SOURCE_BRAND && "logos".equals(selectedBrandSet)) {
            dialogBinding.icon.clearColorFilter();
            dialogBinding.icon.setImageTintList(null);
        } else {
            dialogBinding.icon.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
        }
    }
}