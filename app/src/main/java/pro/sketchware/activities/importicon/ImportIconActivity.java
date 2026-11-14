package pro.sketchware.activities.importicon;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.besome.sketch.lib.ui.ColorPickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import pro.sketchware.databinding.DialogSaveIconBinding;
import pro.sketchware.databinding.ImportIconBinding;
import pro.sketchware.utility.PropertiesUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.SvgUtils;

public class ImportIconActivity extends BaseAppCompatActivity implements IconAdapter.OnIconSelectedListener {

    private static final int ITEMS_PER_PAGE = 40;
    private static final int DEFAULT_ICON_SIZE_PX = 24;
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

    private int selectedColor = DEFAULT_ICON_COLOR;
    private String selectedColorHex = DEFAULT_ICON_COLOR_HEX;
    private int selectedSizePx = DEFAULT_ICON_SIZE_PX;

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
        int columns = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density) / 80;
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

    private void loadIconCatalog() {
        boolean hasCache = iconCatalog.hasCache();
        binding.progressInitial.setVisibility(hasCache ? View.GONE : View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);

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
    }

    private void applyFilterAndResetPaging(@NonNull String query) {
        currentQuery = query.trim();
        filteredSourceItems.clear();

        if (currentQuery.isEmpty()) {
            filteredSourceItems.addAll(allCatalogItems);
        } else {
            String lowerQuery = currentQuery.toLowerCase(Locale.ROOT);
            for (IconCatalog.IconItem item : allCatalogItems) {
                if (item.matches(lowerQuery)) {
                    filteredSourceItems.add(item);
                }
            }
        }

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
            displayedItems.addAll(filteredSourceItems.subList(start, end));
            adapter.submitList(new ArrayList<>(displayedItems));
            currentPage++;
            if (end >= filteredSourceItems.size()) {
                isLastPage = true;
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
        selectedSizePx = DEFAULT_ICON_SIZE_PX;

        DialogSaveIconBinding dialogBinding = DialogSaveIconBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle("Save")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Save", null)
                .create();

        String defaultResourceName = "icon_" + iconItem.name + "_outlined";
        dialogBinding.inputText.setText(defaultResourceName);
        iconNameValidator = new WB(getApplicationContext(), dialogBinding.textInputLayout, uq.b, alreadyAddedImageNames);

        updatePreviewSize(dialogBinding.icon, selectedSizePx);
        updateColorButtonAndPreview(dialogBinding);

        if (iconDownloader.isCached(iconItem.name)) {
            File cachedFile = iconDownloader.getCachedSvgFile(iconItem.name);
            svgUtils.loadImage(dialogBinding.icon, cachedFile.getAbsolutePath());
        } else {
            GradientDrawable placeholder = new GradientDrawable();
            placeholder.setShape(GradientDrawable.RECTANGLE);
            placeholder.setCornerRadius(8f * getResources().getDisplayMetrics().density);
            placeholder.setColor(0x1F9E9E9E);
            dialogBinding.icon.setImageDrawable(placeholder);

            iconDownloader.downloadSvgAsync(iconItem.name, new IconDownloader.DownloadCallback() {
                @Override
                public void onSuccess(@NonNull File svgFile) {
                    if (isDestroyed() || isFinishing() || !dialog.isShowing()) return;
                    svgUtils.loadImage(dialogBinding.icon, svgFile.getAbsolutePath());
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    if (isDestroyed() || isFinishing() || !dialog.isShowing()) return;
                    dialogBinding.icon.setImageResource(R.drawable.ic_remove_grey600_24dp);
                }
            });
        }

        dialogBinding.chipGroupSize.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_size_16) {
                selectedSizePx = 16;
            } else if (checkedId == R.id.chip_size_24) {
                selectedSizePx = 24;
            } else if (checkedId == R.id.chip_size_32) {
                selectedSizePx = 32;
            } else if (checkedId == R.id.chip_size_48) {
                selectedSizePx = 48;
            } else if (checkedId == R.id.chip_size_64) {
                selectedSizePx = 64;
            } else if (checkedId == R.id.chip_size_96) {
                selectedSizePx = 96;
            }
            updatePreviewSize(dialogBinding.icon, selectedSizePx);
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

                iconDownloader.saveResizedSvgToProjectAsync(
                        iconItem.name,
                        sc_id,
                        outputResourceName,
                        selectedSizePx,
                        new IconDownloader.DownloadCallback() {
                            @Override
                            public void onSuccess(@NonNull File savedSvgFile) {
                                if (isDestroyed() || isFinishing()) return;
                                dialogInterface.dismiss();

                                Intent intent = new Intent();
                                intent.putExtra("iconName", outputResourceName);
                                intent.putExtra("iconPath", savedSvgFile.getAbsolutePath());
                                intent.putExtra("iconColor", selectedColor);
                                intent.putExtra("iconColorHex", selectedColorHex);
                                intent.putExtra("iconSize", selectedSizePx);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }

                            @Override
                            public void onError(@NonNull String errorMessage) {
                                if (isDestroyed() || isFinishing()) return;
                                positiveButton.setEnabled(true);
                                SketchwareUtil.toastError(errorMessage);
                            }
                        }
                );
            });
        });

        dialog.show();
    }

    private void updatePreviewSize(@NonNull ImageView previewView, int sizePx) {
        ViewGroup.LayoutParams params = previewView.getLayoutParams();
        if (params != null) {
            params.width = sizePx;
            params.height = sizePx;
            previewView.setLayoutParams(params);
        }
    }

    private void updateColorButtonAndPreview(@NonNull DialogSaveIconBinding dialogBinding) {
        dialogBinding.selectColour.setText(selectedColorHex);
        dialogBinding.selectColour.setBackgroundColor(selectedColor);

        double luminance = Color.red(selectedColor) * 0.299 + Color.green(selectedColor) * 0.587 + Color.blue(selectedColor) * 0.114;
        dialogBinding.selectColour.setTextColor(luminance > 186 ? Color.BLACK : Color.WHITE);

        dialogBinding.icon.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
    }
}