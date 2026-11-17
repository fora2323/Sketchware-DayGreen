package dev.aldi.sayuti.editor.manage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mod.agus.jcoderz.editor.manage.library.locallibrary.ManageLocalLibrary;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.util.Helper;
import pro.sketchware.databinding.LibraryDownloaderDialogBinding;
import pro.sketchware.utility.SketchwareUtil;

//DR
public class LibraryDownloaderDialogFragment extends BottomSheetDialogFragment {
    private LibraryDownloaderDialogBinding binding;

    private final Gson gson = new Gson();
    private BuildSettings buildSettings;

    private boolean notAssociatedWithProject;
    private String localLibFile;
    private OnLibraryDownloadedTask onLibraryDownloadedTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LibraryDownloaderDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() == null) return;

        notAssociatedWithProject = getArguments().getBoolean("notAssociatedWithProject", false);
        buildSettings = (BuildSettings) getArguments().getSerializable("buildSettings");
        localLibFile = getArguments().getString("localLibFile");

        binding.btnDownload.setOnClickListener(v -> initDownloadFlow());

        String[] options = new String[]{"21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options
        );
        binding.actChooseminapi.setAdapter(adapter);
        if (binding.actChooseminapi.getText().toString().trim().isEmpty()) {
            binding.actChooseminapi.setText("21", false);
        }
    }

    public void setOnLibraryDownloadedTask(OnLibraryDownloadedTask onLibraryDownloadedTask) {
        this.onLibraryDownloadedTask = onLibraryDownloadedTask;
    }

    private String detectScope(String rawInput) {
        if (rawInput == null) {
            return ManageLocalLibrary.SCOPE_IMPLEMENTATION;
        }
        Matcher matcher = Pattern.compile("^\\s*(compileOnly|runtimeOnly|runtime|compileRuntime)\\b")
                .matcher(rawInput);
        if (matcher.find()) {
            String matched = matcher.group(1);
            if ("compileOnly".equals(matched)) {
                return ManageLocalLibrary.SCOPE_COMPILE_ONLY;
            }
            return ManageLocalLibrary.SCOPE_RUNTIME;
        }
        return ManageLocalLibrary.SCOPE_IMPLEMENTATION;
    }

    private String[] parseDependencyCoordinate(String input) {
        if (input == null) return null;
        String cleaned = input.trim();

        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        if (cleaned.contains("group:") && cleaned.contains("name:") && cleaned.contains("version:")) {
            Pattern groupPat = Pattern.compile("group\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
            Pattern namePat = Pattern.compile("name\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
            Pattern verPat = Pattern.compile("version\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
            Matcher mG = groupPat.matcher(cleaned);
            Matcher mN = namePat.matcher(cleaned);
            Matcher mV = verPat.matcher(cleaned);
            if (mG.find() && mN.find() && mV.find() && mG.group(1) != null && mN.group(1) != null && mV.group(1) != null) {
                return new String[]{mG.group(1).trim(), mN.group(1).trim(), mV.group(1).trim()};
            }
        }

        cleaned = cleaned.replaceAll("(?i)^(implementation|api|compile|compileOnly|runtimeOnly|runtime|compileRuntime|classpath|annotationProcessor|kapt|ksp|testImplementation|androidTestImplementation)\\s*\\(?\\s*", "");
        cleaned = cleaned.replaceAll("^[(\"']+|[)\"']+$", "").trim();
        cleaned = cleaned.replace("'", "").replace("\"", "").trim();

        if (cleaned.endsWith("@aar") || cleaned.endsWith("@jar")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf('@')).trim();
        }

        var parts = cleaned.split(":");
        if (parts.length >= 3) {
            String group = parts[0].trim();
            String artifact = parts[1].trim();
            String version = parts[2].trim();
            if (!group.isEmpty() && !artifact.isEmpty() && !version.isEmpty()) {
                return new String[]{group, artifact, version};
            }
        }
        return null;
    }

    private String resolveProjectId() {
        if (getArguments() != null) {
            String scId = getArguments().getString("scId");
            if (scId != null && !scId.isEmpty()) return scId;
            scId = getArguments().getString("sc_id");
            if (scId != null && !scId.isEmpty()) return scId;
        }
        if (localLibFile != null && !localLibFile.isEmpty()) {
            File parent = new File(localLibFile).getParentFile();
            if (parent != null) {
                return parent.getName();
            }
        }
        return null;
    }

    private void initDownloadFlow() {
        String rawInput = Helper.getText(binding.dependencyInput);
        if (rawInput.isEmpty()) {
            binding.dependencyInputLayout.setError("Please enter a dependency");
            binding.dependencyInputLayout.setErrorEnabled(true);
            return;
        }

        String scope = detectScope(rawInput);
        String[] parts = parseDependencyCoordinate(rawInput);
        if (parts == null) {
            binding.dependencyInputLayout.setError("Invalid dependency format. Example: groupId:artifactId:version");
            binding.dependencyInputLayout.setErrorEnabled(true);
            return;
        }

        binding.dependencyInputLayout.setErrorEnabled(false);

        String dep = parts[0] + ":" + parts[1] + ":" + parts[2];
        boolean skipSubDeps = binding.cbSkipSubdependencies.isChecked();

        int minApi = 21;
        try {
            String minApiStr = binding.actChooseminapi.getText().toString().trim();
            if (!minApiStr.isEmpty()) {
                minApi = Integer.parseInt(minApiStr);
            }
        } catch (Exception ignored) {
        }

        String scId = resolveProjectId();
        if (notAssociatedWithProject || scId == null || scId.isEmpty()) {
            SketchwareUtil.toastError("Project ID not found");
            return;
        }

        ArrayList<HashMap<String, Object>> list = LocalLibrariesUtil.getLocalLibraries(scId);
        list.add(LocalLibrariesUtil.createPendingLibraryMap(dep, scope, skipSubDeps, minApi));
        LocalLibrariesUtil.rewriteLocalLibFile(scId, gson.toJson(list));

        SketchwareUtil.toast("Added " + dep + " (" + scope + ")");
        if (getActivity() == null) return;
        dismissAllowingStateLoss();
        if (onLibraryDownloadedTask != null) {
            onLibraryDownloadedTask.invoke();
        }
    }

    public interface OnLibraryDownloadedTask {
        void invoke();
    }
}