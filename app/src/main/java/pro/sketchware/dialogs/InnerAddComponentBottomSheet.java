package pro.sketchware.dialogs;

import android.content.Intent;
import android.hardware.Sensor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import a.a.a.FB;
import a.a.a.GB;
import a.a.a.Gx;
import a.a.a.SB;
import a.a.a.ZB;
import a.a.a.bB;
import a.a.a.eC;
import a.a.a.jC;
import a.a.a.mB;
import a.a.a.mq;
import a.a.a.oq;
import a.a.a.uq;
import extensions.anbui.daydream.project.ProjectConfigs;
import extensions.anbui.daydream.project.ProjectLibrary;

import mod.hey.studios.util.Helper;
import mod.hilal.saif.components.ComponentsHandler;
import pro.sketchware.R;
import pro.sketchware.databinding.LogicAddComponentDialogBinding;
import pro.sketchware.lib.DebouncedClickListener;

public class InnerAddComponentBottomSheet extends BottomSheetDialogFragment {
    private final AtomicInteger selectedMime = new AtomicInteger(0);
    private LogicAddComponentDialogBinding binding;
    private OnSaveClickListener onSaveClickListener;
    private String scId;
    private ComponentBean componentBean;
    private ProjectFileBean projectFileBean;
    private boolean isEdit;
    private String originalComponentId;

    private ZB componentNameValidator;
    private SB componentFileNameValidator;
    private SB componentFirebasePathValidator;
    private SB componentMimeTypeValidator;

    public static InnerAddComponentBottomSheet newInstance(String scId, ProjectFileBean projectFileBean, ComponentBean componentBean, OnSaveClickListener onSaveClickListener) {
        InnerAddComponentBottomSheet sheet = new InnerAddComponentBottomSheet();
        sheet.setOnSaveClickListener(onSaveClickListener);

        Bundle args = new Bundle();
        args.putString("sc_id", scId);
        args.putParcelable("project_file_bean", projectFileBean);
        args.putParcelable("component_bean", componentBean);
        sheet.setArguments(args);
        return sheet;
    }

    public static InnerAddComponentBottomSheet newInstanceForEdit(String scId, ProjectFileBean projectFileBean, ComponentBean componentBean, OnSaveClickListener onSaveClickListener) {
        InnerAddComponentBottomSheet sheet = new InnerAddComponentBottomSheet();
        sheet.setOnSaveClickListener(onSaveClickListener);

        Bundle args = new Bundle();
        args.putString("sc_id", scId);
        args.putParcelable("project_file_bean", projectFileBean);
        args.putParcelable("component_bean", componentBean);
        args.putBoolean("is_edit", true);
        args.putString("original_component_id", componentBean.componentId);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        scId = args.getString("sc_id");
        componentBean = args.getParcelable("component_bean");
        projectFileBean = args.getParcelable("project_file_bean");
        isEdit = args.getBoolean("is_edit", false);
        originalComponentId = args.getString("original_component_id", componentBean != null ? componentBean.componentId : "");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LogicAddComponentDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switch (componentBean.type) {
            case ComponentBean.COMPONENT_TYPE_SHAREDPREF:
                binding.tiInputFilename.setVisibility(View.VISIBLE);
                break;

            case ComponentBean.COMPONENT_TYPE_FIREBASE:
            case ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE:
                binding.tiInputFirebasePath.setVisibility(View.VISIBLE);
                break;

            case ComponentBean.COMPONENT_TYPE_GYROSCOPE:
                if (!GB.b(getContext(), Sensor.TYPE_GYROSCOPE)) {
                    binding.cardWarning.setVisibility(View.VISIBLE);
                    binding.warning.setText(Helper.getResString(R.string.message_device_not_support));
                }
                break;

            case ComponentBean.COMPONENT_TYPE_FILE_PICKER:
                binding.tiInputFilePicker.setVisibility(View.VISIBLE);
                break;
        }

        binding.title.setText(ComponentBean.getComponentName(getContext(), componentBean.type));
        binding.icon.setImageResource(ComponentBean.getIconResource(componentBean.type));
        binding.description.setText(ComponentsHandler.description(componentBean.type));

        binding.btnSave.setText(Helper.getResString(isEdit ? R.string.common_word_save : R.string.common_word_add));
        binding.btnReadDocs.setText(Helper.getResString(R.string.component_add_docs_button_title_go_to_docs));

        binding.tiInput.setHint(Helper.getResString(R.string.component_hint_enter_name));
        binding.tiInputFilename.setHint(Helper.getResString(R.string.component_file_hint_enter_file_name));
        binding.tiInputFirebasePath.setHint(Helper.getResString(R.string.design_library_firebase_hint_enter_data_location));
        binding.tiInputFilePicker.setHint(Helper.getResString(R.string.component_file_picker_hint_mime_type));

        binding.tiInputFirebasePath.setHelperText(Helper.getResString(R.string.design_library_firebase_guide_path_example));
        binding.tiInputFilePicker.setHelperText(Helper.getResString(R.string.component_description_file_picker_guide_mime_type_example));

        binding.tiInputFilePicker.setEndIconOnClickListener(v -> showFilePickerMimeTypeSelectionDialog());

        ArrayList<String> excludedNames = new ArrayList<>(jC.a(scId).a(projectFileBean));
        if (isEdit && originalComponentId != null) {
            excludedNames.remove(originalComponentId);
        }
        componentNameValidator = new ZB(getContext(), binding.tiInput, uq.b, uq.a(), excludedNames);
        componentFileNameValidator = new SB(getContext(), binding.tiInputFilename, 1, 20);
        componentFirebasePathValidator = new SB(getContext(), binding.tiInputFirebasePath, 0, 100);
        componentMimeTypeValidator = new SB(getContext(), binding.tiInputFilePicker, 1, 50);

        if (isEdit) {
            String currentId = componentBean.componentId != null ? componentBean.componentId : "";
            String currentParam1 = componentBean.param1 != null ? componentBean.param1 : "";
            componentNameValidator.a(currentId);
            if (binding.edInput.getText() != null) {
                binding.edInput.setSelection(binding.edInput.getText().length());
            }
            switch (componentBean.type) {
                case ComponentBean.COMPONENT_TYPE_SHAREDPREF:
                    componentFileNameValidator.a(currentParam1);
                    break;
                case ComponentBean.COMPONENT_TYPE_FIREBASE:
                case ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE:
                    componentFirebasePathValidator.a(currentParam1);
                    break;
                case ComponentBean.COMPONENT_TYPE_FILE_PICKER:
                    componentMimeTypeValidator.a(currentParam1);
                    int mimeIndex = Arrays.asList("*/*", "image/*", "audio/*", "text/*").indexOf(currentParam1);
                    if (mimeIndex >= 0) {
                        selectedMime.set(mimeIndex);
                    }
                    break;
            }
        }

        binding.btnReadDocs.setOnClickListener(v -> {
            if (!mB.a()) {
                String componentDocsUrlByTypeName = ComponentBean.getComponentDocsUrlByTypeName(componentBean.type);
                if (componentDocsUrlByTypeName.isEmpty()) {
                    bB.a(getContext(), Helper.getResString(R.string.component_add_message_docs_updated_soon), bB.TOAST_NORMAL).show();
                    return;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse(componentDocsUrlByTypeName));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception unused) {
                    showInstallBrowserDialog();
                }
            }
        });

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(new DebouncedClickListener() {
            @Override
            protected void onDebouncedClick(View v) {
                if (checks() && getContext() != null) {
                    bB.a(
                            requireContext(),
                            Helper.getResString(isEdit ? R.string.common_word_saved : R.string.component_message_component_block_added),
                            isEdit ? bB.TOAST_NORMAL : bB.TOAST_WARNING
                    ).show();
                    mB.a(requireContext(), binding.edInput);
                    if (onSaveClickListener != null) {
                        onSaveClickListener.onSaveClick(InnerAddComponentBottomSheet.this);
                    } else {
                        dismiss();
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        onSaveClickListener = null;
    }

    private void setOnSaveClickListener(OnSaveClickListener onSaveClickListener) {
        this.onSaveClickListener = onSaveClickListener;
    }

    private void showFilePickerMimeTypeSelectionDialog() {
        AtomicInteger selectedChoice = new AtomicInteger(selectedMime.get());

        List<String> mimeTypes = Arrays.asList("*/*", "image/*", "audio/*", "text/*");
        List<String> mimeTypeLabels = Arrays.asList(
                getString(R.string.component_file_picker_title_select_mime_type_all_files),
                getString(R.string.component_file_picker_title_select_mime_type_image_files),
                getString(R.string.component_file_picker_title_select_mime_type_audio_files),
                getString(R.string.component_file_picker_title_select_mime_type_text_files)
        );

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.component_file_picker_title_select_mime_type)
                .setSingleChoiceItems(mimeTypeLabels.toArray(new String[0]), selectedMime.get(), (dialog, which) -> selectedChoice.set(which))
                .setPositiveButton(R.string.common_word_select, (dialog, which) -> {
                    String selectedMimeType = mimeTypes.get(selectedChoice.get());
                    selectedMime.set(selectedChoice.get());
                    binding.edInputFilePicker.setText(selectedMimeType);
                })
                .setNegativeButton(R.string.common_word_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean checks() {
        int componentType = componentBean.type;
        String componentId = Helper.getText(binding.edInput);
        if (!componentNameValidator.b()) {
            return false;
        }

        String param1 = "";
        boolean hasParam1 = false;

        switch (componentType) {
            case ComponentBean.COMPONENT_TYPE_SHAREDPREF:
                if (!componentFileNameValidator.b()) {
                    return false;
                }
                param1 = Helper.getText(binding.edInputFilename);
                hasParam1 = true;
                break;

            case ComponentBean.COMPONENT_TYPE_FIREBASE:
            case ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE:
                if (!componentFirebasePathValidator.b()) {
                    return false;
                }
                if (jC.c(scId).d().useYn.equals(ProjectLibraryBean.LIB_USE_N)) {
                    bB.b(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), bB.TOAST_WARNING).show();
                    return false;
                }
                param1 = Helper.getText(binding.edInputFirebasePath);
                hasParam1 = true;
                break;

            case ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH:
                if (jC.c(scId).d().useYn.equals(ProjectLibraryBean.LIB_USE_N)) {
                    bB.b(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), bB.TOAST_WARNING).show();
                    return false;
                } else if (jC.c(scId).d().reserved2.trim().isEmpty()) {
                    bB.b(getContext(), Helper.getResString(R.string.design_library_firebase_guide_setup_first), bB.TOAST_WARNING).show();
                    return false;
                }
                param1 = Helper.getText(binding.edInputFirebasePath);
                hasParam1 = true;
                break;

            case ComponentBean.COMPONENT_TYPE_FRAGMENT_ADAPTER:
                if (jC.c(scId).c().useYn.equals(ProjectLibraryBean.LIB_USE_N)) {
                    bB.b(getContext(), Helper.getResString(R.string.design_library_guide_setup_first), bB.TOAST_WARNING).show();
                    return false;
                }
                break;

            case ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD:
            case ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD:
                if (jC.c(scId).b().useYn.equals(ProjectLibraryBean.LIB_USE_N)) {
                    bB.b(getContext(), Helper.getResString(R.string.design_library_admob_component_setup_first), bB.TOAST_WARNING).show();
                    return false;
                }
                break;

            case ComponentBean.COMPONENT_TYPE_FILE_PICKER:
                if (Helper.getText(binding.edInputFilePicker).isEmpty() || !componentMimeTypeValidator.b()) {
                    return false;
                }
                param1 = Helper.getText(binding.edInputFilePicker);
                hasParam1 = true;
                break;

            case ComponentBean.COMPONENT_TYPE_SHIZUKU:
                if (!ProjectLibrary.isEnabledAppCompat(scId) || !ProjectConfigs.isMinSDKNewerThan23(scId)) {
                    bB.b(getContext(), "Shizuku requires AppCompat enabled and min SDK 24 or newer.", bB.TOAST_WARNING).show();
                    return false;
                }
                break;
        }

        if (isEdit) {
            updateComponent(componentType, componentId, hasParam1 ? param1 : (componentBean.param1 != null ? componentBean.param1 : ""));
        } else if (hasParam1) {
            jC.a(scId).a(projectFileBean.getJavaName(), componentType, componentId, param1);
        } else {
            jC.a(scId).a(projectFileBean.getJavaName(), componentType, componentId);
        }
        jC.a(scId).k();
        return true;
    }

    private void updateComponent(int componentType, String newId, String newParam1) {
        String javaName = projectFileBean.getJavaName();
        eC dataManager = jC.a(scId);
        ArrayList<ComponentBean> components = dataManager.e(javaName);
        if (components != null) {
            for (ComponentBean item : components) {
                if (item.type == componentType && originalComponentId.equals(item.componentId)) {
                    item.componentId = newId;
                    item.param1 = newParam1;
                    break;
                }
            }
        }
        componentBean.componentId = newId;
        componentBean.param1 = newParam1;

        if (!newId.equals(originalComponentId)) {
            HashMap<String, ArrayList<BlockBean>> beanMap = dataManager.b(javaName);
            ArrayList<EventBean> events = dataManager.g(javaName);
            if (events != null) {
                for (EventBean eventBean : events) {
                    if (eventBean.eventType == EventBean.EVENT_TYPE_COMPONENT
                            && originalComponentId.equals(eventBean.targetId)) {
                        String oldKey = eventBean.getEventKey();
                        eventBean.targetId = newId;
                        String newKey = eventBean.getEventKey();
                        if (beanMap != null && beanMap.containsKey(oldKey)) {
                            ArrayList<BlockBean> blocks = beanMap.remove(oldKey);
                            beanMap.put(newKey, blocks);
                        }
                    }
                }
            }

            if (beanMap != null) {
                for (String eventName : oq.getComponentEventsForClass(componentBean.getClassInfo())) {
                    String oldKey = originalComponentId + EventBean.SEPARATOR + eventName;
                    String newKey = newId + EventBean.SEPARATOR + eventName;
                    if (beanMap.containsKey(oldKey)) {
                        ArrayList<BlockBean> blocks = beanMap.remove(oldKey);
                        beanMap.put(newKey, blocks);
                    }
                }

                Gx componentClass = componentBean.getClassInfo();
                String componentTypeName = ComponentBean.getComponentTypeName(componentType);
                for (ArrayList<BlockBean> blockList : beanMap.values()) {
                    if (blockList == null) continue;
                    for (BlockBean blockBean : blockList) {
                        if ("p".equals(blockBean.type)
                                && originalComponentId.equals(blockBean.spec)
                                && (componentTypeName.equals(blockBean.typeName)
                                || (componentClass != null && componentClass.a().equals(blockBean.typeName)))) {
                            blockBean.spec = newId;
                        }
                        if (blockBean.parameters != null && !blockBean.parameters.isEmpty()) {
                            ArrayList<String> paramTokens = extractParameterTokens(blockBean.spec);
                            for (int i = 0; i < blockBean.parameters.size() && i < paramTokens.size(); i++) {
                                if (originalComponentId.equals(blockBean.parameters.get(i))
                                        && isComponentParameterToken(paramTokens.get(i), componentType, componentClass)) {
                                    blockBean.parameters.set(i, newId);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static ArrayList<String> extractParameterTokens(String spec) {
        ArrayList<String> tokens = new ArrayList<>();
        if (spec == null || spec.isEmpty()) {
            return tokens;
        }
        for (String part : FB.c(spec)) {
            if (!part.isEmpty() && part.charAt(0) == '%' && part.length() >= 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private static boolean isComponentParameterToken(String token, int componentType, Gx componentClass) {
        if (token.length() <= 3 || (!token.startsWith("%m.") && !token.startsWith("%p."))) {
            return false;
        }
        String menuName = token.substring(3);
        if ("!".equals(menuName)) {
            return false;
        }
        String componentTypeName = ComponentBean.getComponentTypeName(componentType);
        if (menuName.equalsIgnoreCase(componentTypeName)) {
            return true;
        }
        if (componentClass != null) {
            String mappedType = mq.b(menuName);
            if (!"!".equals(mappedType) && componentClass.a(mappedType)) {
                return true;
            }
        }
        return switch (componentType) {
            case ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD -> "videoad".equals(menuName);
            case ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE -> "phoneauth".equals(menuName);
            case ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE -> "cloudmessage".equals(menuName);
            case ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN -> "googlelogin".equals(menuName);
            default -> false;
        };
    }

    /**
     * Show dialog about installing Google Chrome.
     */
    public void showInstallBrowserDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext());
        dialog.setIcon(R.drawable.chrome_96);
        dialog.setTitle(Helper.getResString(R.string.title_compatible_chrome_browser));
        dialog.setMessage(Helper.getResString(R.string.message_compatible_chrome_brower));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), (v, which) -> {
            if (!mB.a()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.android.chrome"));
                startActivity(intent);
                v.dismiss();
            }
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    public interface OnSaveClickListener {
        void onSaveClick(InnerAddComponentBottomSheet sheet);
    }
}