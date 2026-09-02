package com.besome.sketch.editor.manage.image;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.lib.base.BaseDialogActivity;
import com.besome.sketch.lib.ui.EasyDeleteEditText;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import a.a.a.By;
import a.a.a.HB;
import a.a.a.MA;
import a.a.a.Op;
import a.a.a.PB;
import a.a.a.bB;
import a.a.a.iB;
import a.a.a.mB;
import a.a.a.uq;
import a.a.a.wq;
import a.a.a.yy;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.file.FilesTools;
import extensions.anbui.daydream.project.DRProjectImage;
import extensions.anbui.daydream.tools.ToolCore;
import extensions.anbui.daydream.utils.ColorUtils;
import extensions.anbui.daydream.utils.ImageUtils;
import extensions.anbui.daydream.utils.TextUtils;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

//DR
public class AddImageCollectionActivity extends BaseDialogActivity implements View.OnClickListener {
    private final String TAG = "AddImageCollectionActivity";
    private TextView tv_add_photo;
    private ImageView preview;
    private PB imageNameValidator;
    private EditText ed_input_edittext;
    private LinearLayout optionsContainer;
    private EasyDeleteEditText ed_input;
    private ImageView tv_desc;
    private CheckBox chk_collection;
    private String sc_id;
    private ArrayList<ProjectResourceBean> images;
    private LinearLayout layout_img_inform = null;
    private MaterialCardView layout_img_modify = null;
    private TextView tv_imgcnt = null;
    MaterialCardView copyContainer;
    TextView tvOringinalName;
    TextView tvRidName;
    TextView tvXmlName;
    ImageView ivCopyName;
    ImageView ivCopyRidName;
    ImageView ivCopyXmlName;
    private boolean z = false;
    private String imageFilePath = null;
    private int imageRotationDegrees = 0;
    private int imageExifOrientation = 0;
    private int imageScaleY = 1;
    private int imageScaleX = 1;
    private boolean editing = false;
    private ProjectResourceBean editTarget = null;
    private final HashMap<String, Object> mapPickedColor = new HashMap<>();
    private ImageView ivInvertColor;
    private ImageView ivFillColor;
    boolean isInvertedColor = false;
    String backupOringinalFilePath = "";

    private void flipImageHorizontally() {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            if (imageRotationDegrees != 90 && imageRotationDegrees != 270) {
                imageScaleX *= -1;
            } else {
                imageScaleY *= -1;
            }
            refreshPreview();
        }
    }

    private void flipImageVertically() {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            if (imageRotationDegrees != 90 && imageRotationDegrees != 270) {
                imageScaleY *= -1;
            } else {
                imageScaleX *= -1;
            }
            refreshPreview();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 215 && preview != null) {
            preview.setEnabled(true);
            if (resultCode == RESULT_OK) {
                preview.setColorFilter(null);
                mapPickedColor.clear();
                if (isInvertedColor) {
                    ivInvertColor.setImageResource(R.drawable.invert_colors_24px);
                    isInvertedColor = false;
                }

                imageRotationDegrees = 0;
                imageScaleY = 1;
                imageScaleX = 1;
                z = true;
                setImageFromUri(data.getData());
                if (imageNameValidator != null) {
                    imageNameValidator.a(1);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!mB.a()) {
            int id = v.getId();
            if (id == R.id.cancel_button) {
                setResult(RESULT_CANCELED);
                finish();
            } else if (id == R.id.common_dialog_cancel_button) {
                finish();
            } else if (id == R.id.common_dialog_ok_button) {
                save();
            } else if (id == R.id.img_horizontal) {
                flipImageHorizontally();
            } else if (id == R.id.img_rotate) {
                setImageRotation(imageRotationDegrees + 90);
            } else if (id == R.id.img_selected) {
                preview.setEnabled(false);
                if (!editing) {
                    pickImage();
                }
            } else if (id == R.id.img_vertical) {
                flipImageVertically();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        e(getString(R.string.design_manager_image_title_add_image));
        d(getString(R.string.common_word_save));
        setContentView(R.layout.manage_image_add);
        Intent intent = getIntent();
        images = intent.getParcelableArrayListExtra("images");
        sc_id = intent.getStringExtra("sc_id");
        editTarget = intent.getParcelableExtra("edit_target");
        if (editTarget != null) {
            editing = true;
        }
        layout_img_inform = findViewById(R.id.layout_img_inform);
        layout_img_modify = findViewById(R.id.layout_img_modify);
        chk_collection = findViewById(R.id.chk_collection);
        chk_collection.setVisibility(View.GONE);
        tv_desc = findViewById(R.id.tv_desc);
        tv_imgcnt = findViewById(R.id.tv_imgcnt);
        tv_add_photo = findViewById(R.id.tv_add_photo);
        preview = findViewById(R.id.img_selected);
        ImageView img_rotate = findViewById(R.id.img_rotate);
        ImageView img_vertical = findViewById(R.id.img_vertical);
        ImageView img_horizontal = findViewById(R.id.img_horizontal);
        ed_input = findViewById(R.id.ed_input);
        optionsContainer = findViewById(R.id.options_container);

        ivInvertColor = findViewById(R.id.img_invert_color);

        ivInvertColor.setOnClickListener(v -> {
            if (isInvertedColor) {
                preview.setColorFilter(null);
                ivInvertColor.setImageResource(R.drawable.invert_colors_24px);
            } else {
                mapPickedColor.clear();
                ImageUtils.invertColor(preview);
                ivInvertColor.setImageResource(R.drawable.invert_colors_off_24px);
            }

            isInvertedColor = !isInvertedColor;
        });

        ivFillColor = findViewById(R.id.img_fill_color);

        ivFillColor.setOnClickListener(v -> {
            mapPickedColor.clear();
            ColorUtils.colorPickerLegacy(AddImageCollectionActivity.this, v, mapPickedColor,this::onColorFillterPicked);
        });

        copyContainer = findViewById(R.id.copy_container);
        tvOringinalName = findViewById(R.id.tv_copy_name);
        tvRidName = findViewById(R.id.tv_copy_name_rid);
        tvXmlName = findViewById(R.id.tv_copy_name_xml);
        ImageView ivCopyName = findViewById(R.id.iv_copy_name);
        ImageView ivCopyRidName = findViewById(R.id.iv_copy_name_rid);
        ImageView ivCopyXmlName = findViewById(R.id.iv_copy_name_xml);

        ivCopyName.setOnClickListener(v -> TextUtils.copyToClipboard(AddImageCollectionActivity.this, tvOringinalName.getText().toString()));
        ivCopyRidName.setOnClickListener(v -> TextUtils.copyToClipboard(AddImageCollectionActivity.this, tvRidName.getText().toString()));
        ivCopyXmlName.setOnClickListener(v -> TextUtils.copyToClipboard(AddImageCollectionActivity.this, tvXmlName.getText().toString()));

        ed_input_edittext = ed_input.getEditText();
        ed_input_edittext.setPrivateImeOptions("defaultInputmode=english;");

        ed_input_edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                initializeCopy(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        ed_input.setHint(getString(R.string.design_manager_image_hint_enter_image_name));
        imageNameValidator = new PB(this, ed_input.getTextInputLayout(), uq.b, getReservedImageNames());
        imageNameValidator.a(1);
        tv_add_photo.setText(R.string.design_manager_image_title_add_image);
        preview.setOnClickListener(this);
        img_rotate.setOnClickListener(this);
        img_vertical.setOnClickListener(this);
        img_horizontal.setOnClickListener(this);
        r.setOnClickListener(this);
        s.setOnClickListener(this);
        z = false;
        imageRotationDegrees = 0;
        imageScaleY = 1;
        imageScaleX = 1;
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (editing) {
            editTarget.isEdited = true;
            e(getString(R.string.design_manager_image_title_edit_image_name));
            imageNameValidator = new PB(this, ed_input.getTextInputLayout(), uq.b, getReservedImageNames(), editTarget.resName);
            imageNameValidator.a(1);
            ed_input_edittext.setText(editTarget.resName);
            chk_collection.setVisibility(View.GONE);
            tv_add_photo.setVisibility(View.GONE);
            setImageFromFile(a(editTarget));
            layout_img_modify.setVisibility(View.GONE);
            initializeCopy(editTarget.resName);
        } else {
            copyContainer.setVisibility(View.GONE);
        }
    }

    private ArrayList<String> getReservedImageNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean image : images) {
            names.add(image.resName);
        }
        return names;
    }

    private void pickImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.common_word_choose)), 215);
        } catch (ActivityNotFoundException unused) {
            bB.b(this, getString(R.string.common_error_activity_not_found), bB.TOAST_NORMAL).show();
        }
    }

    private void refreshPreview() {
        preview.setImageBitmap(iB.a(iB.a(iB.a(imageFilePath, 1024, 1024), imageExifOrientation), imageRotationDegrees, imageScaleX, imageScaleY));
    }

    private void save() {
        if (a(imageNameValidator)) {
            new Handler().postDelayed(() -> {
                k();
                new SaveAsyncTask(this).execute();
            }, 500L);
        }
    }

    private void t() {
        if (tv_desc != null) {
            tv_desc.setVisibility(View.INVISIBLE);
        }
        if (layout_img_inform != null && layout_img_modify != null && tv_imgcnt != null) {
            layout_img_inform.setVisibility(View.GONE);
            layout_img_modify.setVisibility(View.VISIBLE);
            tv_imgcnt.setVisibility(View.GONE);
        }
    }

    private boolean a(PB validator) {
        if (!validator.b()) {
            return false;
        }
        if (z || imageFilePath != null) {
            return true;
        }
        tv_desc.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ani_1));
        return false;
    }

    private void setImageFromFile(String path) {
        imageFilePath = path;
        try {
            if (!imageFilePath.contains(Configs.resImagesFolderDir)) {
                backupOringinalFilePath = ToolCore.getTempFilePath(sc_id) + "/" + FilesTools.getFileName(path);
                FilesTools.startCopy(path, ToolCore.getTempFilePath(sc_id));
                imageFilePath = backupOringinalFilePath;
            }
        } catch (Exception e) {
            Log.e(TAG, "setImageFromFile: ", e);
        }
        preview.setImageBitmap(iB.a(path, 1024, 1024));
        int indexOfFilenameExtension = path.lastIndexOf(".");
        if (path.endsWith(".9.png")) {
            indexOfFilenameExtension = path.lastIndexOf(".9.png");
        }
        if (ed_input_edittext != null && (ed_input_edittext.getText() == null || ed_input_edittext.getText().length() <= 0)) {
            ed_input_edittext.setText(path.substring(path.lastIndexOf("/") + 1, indexOfFilenameExtension));
            initializeCopy(ed_input_edittext.getText().toString());
        }
        try {
            imageExifOrientation = iB.a(path);
            refreshPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        t();
    }

    private void setImageRotation(int degrees) {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            imageRotationDegrees = degrees;
            if (imageRotationDegrees == 360) {
                imageRotationDegrees = 0;
            }
            refreshPreview();
        }
    }

    private void setImageFromUri(Uri uri) {
        String filePath;
        if (uri != null && (filePath = HB.a(this, uri)) != null) {
            setImageFromFile(filePath);
        }
    }

    private String a(ProjectResourceBean projectResourceBean) {
        return wq.a() + File.separator + "image" + File.separator + "data" + File.separator + projectResourceBean.resFullName;
    }

    private static class SaveAsyncTask extends MA {
        private final WeakReference<AddImageCollectionActivity> activity;

        public SaveAsyncTask(AddImageCollectionActivity activity) {
            super(activity.getApplicationContext());
            this.activity = new WeakReference<>(activity);
            activity.a(this);
        }

        @Override
        public void a() {
            var activity = this.activity.get();
            bB.a(activity.getApplicationContext(), activity.getString(
                    activity.editing ? R.string.design_manager_message_edit_complete :
                            R.string.design_manager_message_add_complete), bB.TOAST_NORMAL).show();
            activity.h();
            activity.finish();
        }

        @Override
        public void b() throws By {
            var activity = this.activity.get();
            try {
                publishProgress("Now processing..");

                if (activity.isInvertedColor) {
                    DRProjectImage.invertColor(activity.imageFilePath);
                } else if (activity.mapPickedColor.containsKey("selected_color")) {
                    DRProjectImage.fillColor(Integer.parseInt(Objects.requireNonNull(activity.mapPickedColor.get("selected_color")).toString()), activity.imageFilePath);
                }

                if (!activity.editing) {
                    var image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                            Helper.getText(activity.ed_input_edittext).trim(), activity.imageFilePath);
                    image.savedPos = 1;
                    image.isNew = true;
                    image.rotate = activity.imageRotationDegrees;
                    image.flipVertical = activity.imageScaleY;
                    image.flipHorizontal = activity.imageScaleX;
                    Op.g().a(activity.sc_id, image);
                } else {
                    Op.g().a(activity.editTarget, Helper.getText(activity.ed_input_edittext), false);
                }
            } catch (Exception e) {
                // the bytecode's lying
                // noinspection ConstantValue
                if (e instanceof yy yy) {
                    var messageId = switch (yy.getMessage()) {
                        case "fail_to_copy" -> R.string.collection_failed_to_copy;
                        case "file_no_exist" -> R.string.collection_no_exist_file;
                        case "duplicate_name" -> R.string.collection_duplicated_name;
                        default -> 0;
                    };
                    var message = messageId != 0 ? activity.getString(messageId) : "";

                    var a = yy.a();
                    if (a != null && !a.isEmpty()) {
                        var names = "";
                        for (String name : a) {
                            if (!names.isEmpty()) {
                                names += ", ";
                            }
                            names += name;
                        }
                        message += "[" + names + "]";
                    }
                    throw new By(message);
                }
                e.printStackTrace();
                throw new By(e.getMessage());
            }
        }

        @Override
        public void a(String str) {
            activity.get().h();
        }
    }

    private void initializeCopy(String name) {
        if (name.isEmpty()) {
            copyContainer.setVisibility(View.GONE);
            return;
        } else {
            copyContainer.setVisibility(View.VISIBLE);
        }

        tvOringinalName.setText(name);
        tvRidName.setText("R.drawable." + name);
        tvXmlName.setText("@drawable/" + name);
    }

    private void onColorFillterPicked() {
        if (!mapPickedColor.containsKey("selected_color")) return;

        if (isInvertedColor) {
            ivInvertColor.setImageResource(R.drawable.invert_colors_24px);
            isInvertedColor = false;
        }

        preview.setColorFilter(Integer.parseInt(Objects.requireNonNull(mapPickedColor.get("selected_color")).toString()));
    }
}
