package a.a.a;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.editor.manage.image.AddImageActivity;
import com.besome.sketch.editor.manage.image.ManageImageActivity;
import com.besome.sketch.lib.ui.ColorPickerDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import coil.ComponentRegistry;
import coil.ImageLoader;
import coil.decode.SvgDecoder;
import coil.request.ImageRequest;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.importicon.ImportIconActivity;
import pro.sketchware.activities.importicon.data.IconDownloader;
import pro.sketchware.activities.resourceseditor.components.utils.ColorsEditorManager;
import pro.sketchware.databinding.DialogSaveIconBinding;
import pro.sketchware.databinding.FrManageImageListBinding;
import pro.sketchware.databinding.ManageImageListItemBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.PropertiesUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.SvgUtils;

public class pu extends qA {

    private static final int DEFAULT_ICON_SIZE_PX = 24;
    private static final int DEFAULT_ICON_COLOR = Color.parseColor("#9E9E9E");
    private static final String DEFAULT_ICON_COLOR_HEX = "#9E9E9E";

    private final FilePathUtil fpu = new FilePathUtil();
    public boolean isSelecting = false;
    public SvgUtils svgUtils;
    Map<Integer, Map<String, Object>> colorMap = new HashMap<>();
    private ImageLoader coilImageLoader;
    private FrManageImageListBinding binding;
    private String sc_id;
    private ArrayList<ProjectResourceBean> images;
    private MaterialCardView actionButtonContainer;
    private FloatingActionButton fab;
    private String projectImagesDirectory = "";
    private Adapter adapter = null;

    private final ActivityResultLauncher<Intent> openImportIconActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            var data = result.getData();
            if (data == null) {
                Log.e("pu.java", "data is null in openImportIconActivity");
                return;
            }
            ProjectResourceBean icon = new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                    data.getStringExtra("iconName"),
                    data.getStringExtra("iconPath")
            );
            icon.savedPos = 2;
            icon.isNew = true;

            int selectedColor = data.getIntExtra("iconColor", DEFAULT_ICON_COLOR);
            String selectedColorHex = data.getStringExtra("iconColorHex");
            if (selectedColorHex == null || selectedColorHex.isEmpty()) {
                selectedColorHex = DEFAULT_ICON_COLOR_HEX;
            }
            int selectedSize = data.getIntExtra("iconSize", DEFAULT_ICON_SIZE_PX);
            addNewColorFilterInfo(selectedColorHex, selectedColor, selectedSize, images.size());

            addImage(icon);
            bB.a(requireActivity(), getString(R.string.design_manager_message_add_complete), bB.TOAST_NORMAL).show();
        }
    });

    private final ActivityResultLauncher<Intent> showAddImageDialog = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            if (result.getData() == null) {
                Log.e("pu.java", "result data is null");
                return;
            }
            ArrayList<ProjectResourceBean> addedImages;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addedImages = result.getData().getParcelableArrayListExtra("images", ProjectResourceBean.class);
            } else {
                addedImages = result.getData().getParcelableArrayListExtra("images");
            }
            if (addedImages != null) {
                images.addAll(addedImages);
                adapter.notifyItemRangeInserted(images.size() - addedImages.size(), addedImages.size());
            }
            updateGuideVisibility();
            ((ManageImageActivity) requireActivity()).l().refreshData();
            bB.a(requireActivity(), getString(R.string.design_manager_message_add_complete), bB.TOAST_NORMAL).show();
        }
    });

    private final ActivityResultLauncher<Intent> showImageDetailsDialog = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            if (result.getData() == null) {
                Log.e("pu.java", "result data is null");
                return;
            }
            ProjectResourceBean editedImage;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                editedImage = result.getData().getParcelableExtra("image", ProjectResourceBean.class);
            } else {
                editedImage = result.getData().getParcelableExtra("image");
            }
            if (editedImage != null) {
                kC.z();
                for (ProjectResourceBean image : images) {
                    if (image.resName.equals(editedImage.resName)) {
                        image.copy(editedImage);
                        adapter.notifyItemChanged(images.indexOf(image));
                        break;
                    }
                }
            }
            updateGuideVisibility();
            ((ManageImageActivity) requireActivity()).l().refreshData();
            bB.a(requireActivity(), getString(R.string.design_manager_message_edit_complete), bB.TOAST_NORMAL).show();
        }
    });

    public static void copyFile(String srcPath, String destPath) throws IOException {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        if (srcFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    private void ensureImageLoaders() {
        if (svgUtils == null) {
            svgUtils = new SvgUtils(requireContext());
            svgUtils.initImageLoader();
        }
        if (coilImageLoader == null) {
            ComponentRegistry components = new ComponentRegistry.Builder()
                    .add(new SvgDecoder.Factory())
                    .build();
            coilImageLoader = new ImageLoader.Builder(requireContext().getApplicationContext())
                    .components(components)
                    .build();
        }
    }

    public ArrayList<ProjectResourceBean> d() {
        return images;
    }

    private void initialize() {
        sc_id = requireActivity().getIntent().getStringExtra("sc_id");
        projectImagesDirectory = jC.d(sc_id).l();
        ArrayList<ProjectResourceBean> arrayList = jC.d(sc_id).b;
        ensureImageLoaders();
        if (arrayList != null) {
            for (ProjectResourceBean next : arrayList) {
                if (next.flipVertical == 0) {
                    next.flipVertical = 1;
                }
                if (next.flipHorizontal == 0) {
                    next.flipHorizontal = 1;
                }
                next.savedPos = 0;
                images.add(next);
            }
        }
    }

    private void unselectAll() {
        for (ProjectResourceBean projectResourceBean : images) {
            projectResourceBean.isSelected = false;
        }
    }

    private void deleteSelected() {
        Map<Integer, Map<String, Object>> updatedColorMap = new HashMap<>();
        int newIndex = 0;
        for (int i = 0; i < images.size(); i++) {
            if (!images.get(i).isSelected) {
                if (colorMap.containsKey(i)) {
                    updatedColorMap.put(newIndex, colorMap.get(i));
                }
                newIndex++;
            }
        }
        colorMap.clear();
        colorMap.putAll(updatedColorMap);

        for (int i = images.size() - 1; i >= 0; i--) {
            if (images.get(i).isSelected) {
                images.remove(i);
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void saveImages() {
        Path svgDir = Paths.get(fpu.getPathSvg(sc_id));
        try {
            if (!Files.exists(svgDir)) {
                Files.createDirectories(svgDir);
            }
        } catch (IOException error) {
            Log.d("pu.java", "Failed to create directory for saving svgs at: " + fpu.getPathResource(sc_id));
        }
        int index = 0;
        for (ProjectResourceBean image : images) {
            if (image.isNew || image.isEdited) {
                try {
                    String path;
                    if (image.savedPos == 0) {
                        path = a(image);
                    } else {
                        path = image.resFullName;
                    }

                    String str = projectImagesDirectory + File.separator + image.resName;
                    Log.d("svg", "full name : " + image.resFullName);
                    if (image.resFullName.endsWith(".svg")) {
                        String svgPath = fpu.getSvgFullPath(sc_id, image.resName);
                        copyFile(path, svgPath);
                        Map<String, Object> colorInfo = colorMap.get(index);
                        String colorHex = colorInfo != null ? (String) colorInfo.get("colorHex") : null;
                        colorHex = (colorHex != null) ? colorHex : "#FFFFFF";
                        svgUtils.convert(svgPath, projectImagesDirectory, colorHex);
                    } else {
                        iB.a(path, image.isNinePatch() ? str + ".9.png" : str + ".png", image.rotate, image.flipHorizontal, image.flipVertical);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            index++;
        }
        for (int i = 0; i < images.size(); i++) {
            ProjectResourceBean image = images.get(i);
            if (image.isNew || image.isEdited) {
                String fileExtension;
                if (image.isNinePatch()) {
                    fileExtension = ".9.png";
                } else if (image.isSvg()) {
                    fileExtension = ".xml";
                } else {
                    fileExtension = ".png";
                }

                images.set(i, new ProjectResourceBean(
                        ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                        image.resName,
                        image.resName + fileExtension
                ));
            }
        }
        jC.d(sc_id).b(images);
        jC.d(sc_id).y();
        jC.a(sc_id).k();
    }

    private void updateGuideVisibility() {
        if (images.isEmpty()) {
            binding.tvGuide.setVisibility(View.VISIBLE);
            binding.imageList.setVisibility(View.GONE);
        } else {
            binding.imageList.setVisibility(View.VISIBLE);
            binding.tvGuide.setVisibility(View.GONE);
        }
    }

    private void showAddImageDialog() {
        Intent intent = new Intent(requireContext(), AddImageActivity.class);
        intent.putParcelableArrayListExtra("images", images);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("dir_path", projectImagesDirectory);
        showAddImageDialog.launch(intent);
    }

    private void openImportIconActivity() {
        Intent intent = new Intent(requireActivity(), ImportIconActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putStringArrayListExtra("imageNames", getAllImageNames());
        openImportIconActivity.launch(intent);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            initialize();
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            projectImagesDirectory = savedInstanceState.getString("dir_path");
            images = savedInstanceState.getParcelableArrayList("images");
        }
        new oB().f(projectImagesDirectory);
        adapter.notifyDataSetChanged();
        updateGuideVisibility();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (binding.imageList.getLayoutManager() instanceof GridLayoutManager manager) {
            manager.setSpanCount(ManageImageActivity.getImageGridColumnCount(requireContext()));
        }
        binding.imageList.requestLayout();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.manage_image_menu, menu);
        menu.findItem(R.id.menu_image_delete).setVisible(!isSelecting);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FrManageImageListBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        images = new ArrayList<>();
        binding.imageList.setHasFixedSize(true);
        binding.imageList.setLayoutManager(new GridLayoutManager(requireActivity(), ManageImageActivity.getImageGridColumnCount(requireContext())));
        adapter = new Adapter(binding.imageList);
        binding.imageList.setAdapter(adapter);
        binding.tvGuide.setText(R.string.design_manager_image_description_guide_add_image);
        actionButtonContainer = requireActivity().findViewById(R.id.layout_btn_group);
        MaterialButton delete = requireActivity().findViewById(R.id.btn_delete);
        MaterialButton cancel = requireActivity().findViewById(R.id.btn_cancel);
        delete.setOnClickListener(view -> {
            if (isSelecting) {
                deleteSelected();
                a(false);
                updateGuideVisibility();
                bB.a(requireActivity(), getString(R.string.common_message_complete_delete), bB.TOAST_WARNING).show();
                fab.show();
            }
        });
        cancel.setOnClickListener(view -> {
            if (isSelecting) {
                a(false);
            }
        });
        fab = requireActivity().findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(view -> {
            a(false);
            showAddImageDialog();
        });
        kC.z();
        return binding.getRoot();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_image_delete) {
            a(!isSelecting);
        } else if (id == R.id.menu_image_import) {
            openImportIconActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putString("dir_path", projectImagesDirectory);
        outState.putParcelableArrayList("images", images);
        super.onSaveInstanceState(outState);
    }

    private void showImageDetailsDialog(ProjectResourceBean projectResourceBean) {
        Intent intent = new Intent(requireContext(), AddImageActivity.class);
        intent.putParcelableArrayListExtra("images", images);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("dir_path", projectImagesDirectory);
        intent.putExtra("edit_target", projectResourceBean);
        showImageDetailsDialog.launch(intent);
    }

    private void showEditIconDialog(int position) {
        if (position < 0 || position >= images.size()) return;
        ProjectResourceBean image = images.get(position);
        ensureImageLoaders();

        File sourceSvgFile = resolveSourceSvgFile(image);
        if (sourceSvgFile == null || !sourceSvgFile.exists()) {
            SketchwareUtil.toastError("Source SVG file not found for " + image.resName);
            return;
        }

        final String oldName = image.resName;
        int initialColor = DEFAULT_ICON_COLOR;
        String initialColorHex = DEFAULT_ICON_COLOR_HEX;
        int initialSizePx = DEFAULT_ICON_SIZE_PX;

        Map<String, Object> savedMeta = colorMap.get(position);
        if (savedMeta != null) {
            if (savedMeta.get("colorHex") instanceof String hex && !hex.isEmpty()) {
                initialColorHex = hex;
            }
            if (savedMeta.get("color") instanceof Integer col) {
                initialColor = col;
            } else {
                initialColor = resolveColorFromHex(initialColorHex, DEFAULT_ICON_COLOR);
            }
            if (savedMeta.get("iconSize") instanceof Integer sz && sz > 0) {
                initialSizePx = sz;
            }
        } else {
            int extractedSize = extractSvgSizePx(sourceSvgFile);
            if (extractedSize > 0) {
                initialSizePx = extractedSize;
            }
            String extractedColorHex = extractVectorFillColorHex(image.resName);
            if (extractedColorHex != null && !extractedColorHex.isEmpty()) {
                initialColorHex = extractedColorHex;
                initialColor = resolveColorFromHex(extractedColorHex, DEFAULT_ICON_COLOR);
            }
        }

        final int[] editColor = {initialColor};
        final String[] editColorHex = {initialColorHex};
        final int[] editSizePx = {initialSizePx};

        DialogSaveIconBinding dialogBinding = DialogSaveIconBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.getRoot())
                .setTitle("Edit Icon")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Save", null)
                .create();

        dialogBinding.inputText.setText(oldName);
        ArrayList<String> existingOtherNames = getAllImageNamesExcept(oldName);
        WB iconNameValidator = new WB(
                requireContext().getApplicationContext(),
                dialogBinding.textInputLayout,
                uq.b,
                existingOtherNames
        );

        selectMatchingSizeChip(dialogBinding, editSizePx[0]);
        updateIconPreviewSize(dialogBinding.icon, editSizePx[0]);
        updateIconColorButtonAndPreview(dialogBinding, editColor[0], editColorHex[0]);
        renderSvgPreviewFile(dialogBinding.icon, sourceSvgFile, editColor[0]);

        dialogBinding.chipGroupSize.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_size_16) {
                editSizePx[0] = 16;
            } else if (checkedId == R.id.chip_size_24) {
                editSizePx[0] = 24;
            } else if (checkedId == R.id.chip_size_32) {
                editSizePx[0] = 32;
            } else if (checkedId == R.id.chip_size_48) {
                editSizePx[0] = 48;
            } else if (checkedId == R.id.chip_size_64) {
                editSizePx[0] = 64;
            } else if (checkedId == R.id.chip_size_96) {
                editSizePx[0] = 96;
            }
            updateIconPreviewSize(dialogBinding.icon, editSizePx[0]);
            prepareAndLoadResizedPreview(dialog, dialogBinding, sourceSvgFile, editSizePx[0], editColor[0]);
        });

        dialogBinding.selectColour.setOnClickListener(view -> {
            ColorPickerDialog colorPicker = new ColorPickerDialog(requireContext(), editColorHex[0], false, false, sc_id);
            colorPicker.a(new ColorPickerDialog.b() {
                @Override
                public void a(int color) {
                    editColor[0] = color;
                    editColorHex[0] = "#" + String.format("%06X", color & 0x00FFFFFF);
                    updateIconColorButtonAndPreview(dialogBinding, editColor[0], editColorHex[0]);
                }

                @Override
                public void a(String colorName, int color) {
                    editColor[0] = color;
                    editColorHex[0] = "@color/" + colorName;
                    updateIconColorButtonAndPreview(dialogBinding, editColor[0], editColorHex[0]);
                }
            });
            colorPicker.materialColorAttr((attr, attrColor) -> {
                String attrRef = "?attr/" + attr;
                editColor[0] = resolveColorFromHex(attrRef, DEFAULT_ICON_COLOR);
                editColorHex[0] = attrRef;
                updateIconColorButtonAndPreview(dialogBinding, editColor[0], editColorHex[0]);
            });
            colorPicker.showAtLocation(view, Gravity.CENTER, 0, 0);
        });

        dialogBinding.licenceInfo.setOnClickListener(v -> {
            Uri webpage = Uri.parse("https://www.apache.org/licenses/LICENSE-2.0.txt");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (!iconNameValidator.b()) {
                    return;
                }

                String newName = Helper.getText(dialogBinding.inputText).trim();
                positiveButton.setEnabled(false);
                dialogBinding.progressDownload.setVisibility(View.VISIBLE);

                new Thread(() -> {
                    try {
                        byte[] rawBytes = Files.readAllBytes(sourceSvgFile.toPath());
                        String rawSvg = new String(rawBytes, StandardCharsets.UTF_8);
                        String resizedSvg = IconDownloader.resizeSvgDimensionsOnly(rawSvg, editSizePx[0]);

                        String targetSvgPath = fpu.getSvgFullPath(sc_id, newName);
                        File targetSvgFile = new File(targetSvgPath);
                        File parentDir = targetSvgFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            parentDir.mkdirs();
                        }
                        Files.write(targetSvgFile.toPath(), resizedSvg.getBytes(StandardCharsets.UTF_8));

                        if (!newName.equals(oldName)) {
                            File oldSvgFile = new File(fpu.getSvgFullPath(sc_id, oldName));
                            if (oldSvgFile.exists() && !oldSvgFile.getAbsolutePath().equals(targetSvgFile.getAbsolutePath())) {
                                //noinspection ResultOfMethodCallIgnored
                                oldSvgFile.delete();
                            }
                            File oldXmlFile = new File(projectImagesDirectory, oldName + ".xml");
                            if (oldXmlFile.exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                oldXmlFile.delete();
                            }
                        }

                        svgUtils.convert(targetSvgPath, projectImagesDirectory, editColorHex[0]);

                        Activity activity = getActivity();
                        if (activity == null || activity.isDestroyed() || activity.isFinishing()) return;

                        activity.runOnUiThread(() -> {
                            dialogBinding.progressDownload.setVisibility(View.GONE);
                            dialogInterface.dismiss();

                            image.resName = newName;
                            image.resFullName = targetSvgPath;
                            image.savedPos = 2;
                            image.isNew = true;
                            image.isEdited = true;

                            addNewColorFilterInfo(editColorHex[0], editColor[0], editSizePx[0], position);
                            adapter.notifyItemChanged(position);
                            ((ManageImageActivity) activity).l().refreshData();
                            bB.a(activity, getString(R.string.design_manager_message_edit_complete), bB.TOAST_NORMAL).show();
                        });
                    } catch (Exception e) {
                        Log.e("pu.java", "Failed to save edited icon", e);
                        Activity activity = getActivity();
                        if (activity == null || activity.isDestroyed() || activity.isFinishing()) return;

                        activity.runOnUiThread(() -> {
                            dialogBinding.progressDownload.setVisibility(View.GONE);
                            positiveButton.setEnabled(true);
                            SketchwareUtil.toastError("Failed to edit icon: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                        });
                    }
                }).start();
            });
        });

        dialog.show();
    }

    @Nullable
    private File resolveSourceSvgFile(@NonNull ProjectResourceBean image) {
        if (image.isNew && image.resFullName != null && image.resFullName.endsWith(".svg")) {
            File direct = new File(image.resFullName);
            if (direct.exists()) {
                return direct;
            }
        }
        File convertedSvg = new File(fpu.getSvgFullPath(sc_id, image.resName));
        if (convertedSvg.exists()) {
            return convertedSvg;
        }
        return null;
    }

    private int extractSvgSizePx(@NonNull File svgFile) {
        try {
            String content = new String(Files.readAllBytes(svgFile.toPath()), StandardCharsets.UTF_8);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(content));
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG && "svg".equals(parser.getName())) {
                    String widthAttr = parser.getAttributeValue(null, "width");
                    if (widthAttr != null) {
                        String digits = widthAttr.replaceAll("[^0-9.]", "");
                        if (!digits.isEmpty()) {
                            return Math.round(Float.parseFloat(digits));
                        }
                    }
                    break;
                }
                parser.next();
            }
        } catch (Exception e) {
            Log.w("pu.java", "Failed to read SVG dimensions", e);
        }
        return DEFAULT_ICON_SIZE_PX;
    }

    @Nullable
    private String extractVectorFillColorHex(@NonNull String resName) {
        File xmlFile = new File(projectImagesDirectory, resName + ".xml");
        if (!xmlFile.exists()) return null;
        try {
            String content = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(content));
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG && "path".equals(parser.getName())) {
                    String fillColor = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "fillColor");
                    if (fillColor == null) {
                        fillColor = parser.getAttributeValue(null, "android:fillColor");
                    }
                    if (fillColor != null && !fillColor.isEmpty()) {
                        return fillColor;
                    }
                }
                parser.next();
            }
        } catch (Exception e) {
            Log.w("pu.java", "Failed to read vector fill color", e);
        }
        return null;
    }

    private int resolveColorFromHex(@Nullable String colorHex, int fallback) {
        if (colorHex == null || colorHex.isEmpty()) return fallback;
        try {
            if (colorHex.startsWith("?attr/") || colorHex.startsWith("@color/")) {
                String resolved = new ColorsEditorManager().getColorValue(requireContext().getApplicationContext(), colorHex, 3);
                if (resolved != null && !resolved.isEmpty()) {
                    return PropertiesUtil.parseColor(resolved);
                }
            }
            return Color.parseColor(colorHex);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void selectMatchingSizeChip(@NonNull DialogSaveIconBinding dialogBinding, int sizePx) {
        int[] options = {16, 24, 32, 48, 64, 96};
        int[] chipIds = {
                R.id.chip_size_16,
                R.id.chip_size_24,
                R.id.chip_size_32,
                R.id.chip_size_48,
                R.id.chip_size_64,
                R.id.chip_size_96
        };
        int bestIndex = 1; // default 24
        int minDiff = Integer.MAX_VALUE;
        for (int i = 0; i < options.length; i++) {
            int diff = Math.abs(options[i] - sizePx);
            if (diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        dialogBinding.chipGroupSize.check(chipIds[bestIndex]);
    }

    private void updateIconPreviewSize(@NonNull ImageView previewView, int sizePx) {
        float density = getResources().getDisplayMetrics().density;
        int targetPx = Math.max(1, Math.round(sizePx * density));
        ViewGroup.LayoutParams params = previewView.getLayoutParams();
        if (params != null) {
            params.width = targetPx;
            params.height = targetPx;
            previewView.setLayoutParams(params);
        }
    }

    private void updateIconColorButtonAndPreview(@NonNull DialogSaveIconBinding dialogBinding, int color, @NonNull String colorHex) {
        dialogBinding.selectColour.setText(colorHex);
        dialogBinding.selectColour.setBackgroundColor(color);

        double luminance = Color.red(color) * 0.299
                + Color.green(color) * 0.587
                + Color.blue(color) * 0.114;
        dialogBinding.selectColour.setTextColor(luminance > 186 ? Color.BLACK : Color.WHITE);

        dialogBinding.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void prepareAndLoadResizedPreview(
            @NonNull AlertDialog dialog,
            @NonNull DialogSaveIconBinding dialogBinding,
            @NonNull File sourceSvgFile,
            int sizePx,
            int tintColor
    ) {
        new Thread(() -> {
            try {
                byte[] rawBytes = Files.readAllBytes(sourceSvgFile.toPath());
                String rawSvg = new String(rawBytes, StandardCharsets.UTF_8);
                String resizedSvg = IconDownloader.resizeSvgDimensionsOnly(rawSvg, sizePx);

                File tempPreview = new File(requireContext().getCacheDir(), "edit_icon_preview_" + sizePx + "px.svg");
                Files.write(tempPreview.toPath(), resizedSvg.getBytes(StandardCharsets.UTF_8));

                Activity activity = getActivity();
                if (activity == null || activity.isDestroyed() || activity.isFinishing()) return;
                activity.runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        renderSvgPreviewFile(dialogBinding.icon, tempPreview, tintColor);
                    }
                });
            } catch (Exception e) {
                Log.w("pu.java", "Failed to render resized preview", e);
            }
        }).start();
    }

    private void renderSvgPreviewFile(@NonNull ImageView imageView, @NonNull File svgFile, int tintColor) {
        ensureImageLoaders();
        ImageRequest request = new ImageRequest.Builder(requireContext())
                .data(svgFile)
                .target(new coil.target.Target() {
                    @Override
                    public void onStart(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onSuccess(@NonNull Drawable result) {
                        imageView.setImageDrawable(result);
                        imageView.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onError(@Nullable Drawable error) {
                        imageView.setImageResource(R.drawable.ic_remove_grey600_24dp);
                    }
                })
                .build();
        coilImageLoader.enqueue(request);
    }

    private ArrayList<String> getAllImageNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean projectResourceBean : images) {
            names.add(projectResourceBean.resName);
        }
        return names;
    }

    private ArrayList<String> getAllImageNamesExcept(@NonNull String excludedName) {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean projectResourceBean : images) {
            if (!projectResourceBean.resName.equals(excludedName)) {
                names.add(projectResourceBean.resName);
            }
        }
        return names;
    }

    public void a(ArrayList<ProjectResourceBean> arrayList) {
        ArrayList<ProjectResourceBean> imagesToAdd = new ArrayList<>();
        ArrayList<String> duplicateNames = new ArrayList<>();
        for (ProjectResourceBean next : arrayList) {
            String imageName = next.resName;
            if (isImageNameDuplicate(imageName)) {
                duplicateNames.add(imageName);
            } else {
                ProjectResourceBean image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, imageName, next.resFullName);
                image.savedPos = 1;
                image.isNew = true;
                image.rotate = 0;
                image.flipVertical = 1;
                image.flipHorizontal = 1;
                imagesToAdd.add(image);
            }
        }
        addImages(imagesToAdd);
        if (!duplicateNames.isEmpty()) {
            bB.a(requireActivity(), getString(R.string.common_message_name_unavailable) + "\n" +
                    "[" + String.join(", ", duplicateNames) + "]", bB.TOAST_WARNING).show();
        } else {
            bB.a(requireActivity(), getString(R.string.design_manager_message_import_complete), bB.TOAST_WARNING).show();
        }
        adapter.notifyDataSetChanged();
        updateGuideVisibility();
    }

    private boolean isImageNameDuplicate(String imageName) {
        for (ProjectResourceBean image : images) {
            if (image.resName.equals(imageName)) {
                return true;
            }
        }
        return false;
    }

    private String a(ProjectResourceBean projectResourceBean) {
        return projectImagesDirectory + File.separator + projectResourceBean.resFullName;
    }

    public void a(boolean isSelecting) {
        this.isSelecting = isSelecting;
        requireActivity().invalidateOptionsMenu();
        unselectAll();
        actionButtonContainer.setVisibility(this.isSelecting ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void addImage(ProjectResourceBean projectResourceBean) {
        images.add(projectResourceBean);
        adapter.notifyDataSetChanged();
        adapter.notifyItemInserted(adapter.getItemCount());
        updateGuideVisibility();
    }

    private void addNewColorFilterInfo(String colorHex, int color, int forPosition) {
        addNewColorFilterInfo(colorHex, color, DEFAULT_ICON_SIZE_PX, forPosition);
    }

    private void addNewColorFilterInfo(String colorHex, int color, int iconSize, int forPosition) {
        Map<String, Object> colorItem1 = new HashMap<>();
        colorItem1.put("colorHex", colorHex);
        colorItem1.put("color", color);
        colorItem1.put("iconSize", iconSize);

        colorMap.put(forPosition, colorItem1);
        Log.d("color filter", "new color filter item at " + forPosition + ": " + colorHex + " " + color + " (" + iconSize + "px)");
    }

    private void addImages(ArrayList<ProjectResourceBean> arrayList) {
        images.addAll(arrayList);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        public Adapter(RecyclerView recyclerView) {
            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (dy > 2) {
                            if (fab.isEnabled()) {
                                fab.hide();
                            }
                        } else if (dy < -2) {
                            if (fab.isEnabled()) {
                                fab.show();
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProjectResourceBean image = images.get(position);
            String bindTag = image.resName + "|" + image.resFullName + "|" + position;
            holder.boundTag = bindTag;

            holder.binding.deleteImgContainer.setVisibility(isSelecting ? View.VISIBLE : View.GONE);
            holder.binding.imgNinePatch.setVisibility(image.isNinePatch() ? View.VISIBLE : View.GONE);
            holder.binding.imgDelete.setImageResource(image.isSelected ? R.drawable.ic_checkmark_green_48dp
                    : R.drawable.ic_trashcan_white_48dp);
            holder.binding.chkSelect.setChecked(image.isSelected);
            holder.binding.tvImageName.setText(image.resName);

            if (colorMap.get(position) != null) {
                int color = Objects.requireNonNullElse((int) colorMap.get(position).get("color"), 0xFFFFFFFF);
                Log.d("Applying filter to " + position, String.valueOf(color));
                holder.binding.img.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } else {
                holder.binding.img.clearColorFilter();
            }

            ensureImageLoaders();

            if (image.resFullName.endsWith(".svg") || image.resFullName.endsWith(".xml")) {
                String targetPath;
                if (image.resFullName.endsWith(".svg")) {
                    targetPath = image.isNew ? image.resFullName : String.join(File.separator, projectImagesDirectory, image.resFullName);
                } else {
                    targetPath = image.isNew ? image.resFullName : fpu.getSvgFullPath(sc_id, image.resName);
                }

                File svgFile = new File(targetPath);
                if (!svgFile.exists()) {
                    holder.binding.progressLoading.setVisibility(View.GONE);
                    holder.binding.img.setImageResource(R.drawable.ic_remove_grey600_24dp);
                    return;
                }

                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data(svgFile)
                        .target(new coil.target.Target() {
                            @Override
                            public void onStart(@Nullable Drawable placeholder) {
                                if (bindTag.equals(holder.boundTag)) {
                                    holder.binding.img.setImageDrawable(null);
                                    holder.binding.progressLoading.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onSuccess(@NonNull Drawable result) {
                                if (bindTag.equals(holder.boundTag)) {
                                    holder.binding.progressLoading.setVisibility(View.GONE);
                                    holder.binding.img.setImageDrawable(result);
                                }
                            }

                            @Override
                            public void onError(@Nullable Drawable error) {
                                if (bindTag.equals(holder.boundTag)) {
                                    holder.binding.progressLoading.setVisibility(View.GONE);
                                    holder.binding.img.setImageResource(R.drawable.ic_remove_grey600_24dp);
                                }
                            }
                        })
                        .build();
                coilImageLoader.enqueue(request);
            } else {
                holder.binding.progressLoading.setVisibility(View.VISIBLE);
                Glide.with(requireActivity())
                        .asBitmap()
                        .load(image.savedPos == 0 ? projectImagesDirectory + File.separator + image.resFullName
                                : images.get(position).resFullName)
                        .transform(new BitmapTransformation() {

                            final String ID = "my-transformation";
                            final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

                            @Override
                            protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
                                return iB.a(toTransform, image.rotate, image.flipHorizontal, image.flipVertical);
                            }

                            @Override
                            public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
                                messageDigest.update(ID_BYTES);
                            }
                        })
                        .centerCrop()
                        .signature(kC.n())
                        .listener(new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                                if (bindTag.equals(holder.boundTag)) {
                                    holder.binding.progressLoading.setVisibility(View.GONE);
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                if (bindTag.equals(holder.boundTag)) {
                                    holder.binding.progressLoading.setVisibility(View.GONE);
                                }
                                return false;
                            }
                        })
                        .error(R.drawable.ic_remove_grey600_24dp)
                        .into(holder.binding.img);
            }
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageImageListItemBinding binding = ManageImageListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ManageImageListItemBinding binding;
            private String boundTag;

            public ViewHolder(@NonNull ManageImageListItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.img.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    if (!isSelecting) {
                        ProjectResourceBean clicked = images.get(pos);
                        if (clicked.resFullName.endsWith(".svg") || clicked.resFullName.endsWith(".xml")) {
                            showEditIconDialog(pos);
                        } else {
                            showImageDetailsDialog(clicked);
                        }
                    } else {
                        binding.chkSelect.setChecked(!binding.chkSelect.isChecked());
                        images.get(pos).isSelected = binding.chkSelect.isChecked();
                        notifyItemChanged(pos);
                    }
                });

                binding.img.setOnLongClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return true;

                    a(true);
                    binding.chkSelect.setChecked(!binding.chkSelect.isChecked());
                    images.get(pos).isSelected = binding.chkSelect.isChecked();
                    return true;
                });
            }
        }
    }
}