package dev.aldi.sayuti.editor.manage;

import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.createLibraryMap;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.cosmic.ide.dependency.resolver.api.Artifact;
import org.sketchware.daygreen.FileCheckUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.SdkManagerActivity;
import mod.jbk.build.BuiltInLibraries;
import mod.pranav.dependency.resolver.DependencyResolver;
import pro.sketchware.R;
import pro.sketchware.databinding.LibraryDownloaderDialogBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

//DR
public class LibraryDownloaderDialogFragment extends BottomSheetDialogFragment {
    private LibraryDownloaderDialogBinding binding;
    private DependencyDownloadAdapter dependencyAdapter;
    private final List<DependencyDownloadItem> downloadItems = new ArrayList<>();
    private ExecutorService downloadExecutor;

    private final Gson gson = new Gson();
    private BuildSettings buildSettings;

    private boolean notAssociatedWithProject;
    private String dependencyName;
    private String localLibFile;
    private OnLibraryDownloadedTask onLibraryDownloadedTask;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LibraryDownloaderDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
            downloadExecutor.shutdownNow();
        }
        unregisterNetworkCallback();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() == null) return;

        dependencyAdapter = new DependencyDownloadAdapter();
        binding.dependenciesRecyclerView.setAdapter(dependencyAdapter);
        binding.dependenciesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        downloadExecutor = Executors.newSingleThreadExecutor();

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

        connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (binding != null) {
                    requireActivity().runOnUiThread(() -> binding.btnDownload.setEnabled(true));
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (binding != null) {
                    requireActivity().runOnUiThread(() -> binding.btnDownload.setEnabled(false));
                }
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        // Initial check
        binding.btnDownload.setEnabled(isNetworkAvailable());
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    public void setOnLibraryDownloadedTask(OnLibraryDownloadedTask onLibraryDownloadedTask) {
        this.onLibraryDownloadedTask = onLibraryDownloadedTask;
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

        cleaned = cleaned.replaceAll("(?i)^(implementation|api|compile|compileOnly|runtimeOnly|runtime|classpath|annotationProcessor|kapt|ksp|testImplementation|androidTestImplementation)\\s*\\(?\\s*", "");
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

    private void initDownloadFlow() {
        String rawInput = Helper.getText(binding.dependencyInput);
        if (rawInput.isEmpty()) {
            binding.dependencyInputLayout.setError("Please enter a dependency");
            binding.dependencyInputLayout.setErrorEnabled(true);
            return;
        }

        String[] parts = parseDependencyCoordinate(rawInput);
        if (parts == null) {
            binding.dependencyInputLayout.setError("Invalid dependency format. Example: groupId:artifactId:version");
            binding.dependencyInputLayout.setErrorEnabled(true);
            return;
        }

        dependencyName = parts[0] + ":" + parts[1] + ":" + parts[2];
        showDownloadConfirmationDialog(parts[0], parts[1], parts[2]);
    }

    private void showDownloadConfirmationDialog(String group, String artifact, String version) {
        boolean skipSubdependencies = binding.cbSkipSubdependencies.isChecked();

        String message;
        if (skipSubdependencies) {
            message = "Are you sure you want to download " + dependencyName;
        } else {
            message = "Are you sure you want to download " + dependencyName + " and its sub-dependencies?";
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Download")
                .setMessage(message)
                .setPositiveButton("Download", (dialog, which) -> startDownloadProcess(group, artifact, version))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startDownloadProcess(String group, String artifact, String version) {
        if (!FileCheckUtils.isSdkDownloaded(requireContext())) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Android SDK Missing")
                    .setMessage("Android SDK platform JAR is missing.\n\nPlease open Tools > SDK Manager to download an Android SDK (e.g. API 33 or 34) before downloading dependencies.")
                    .setPositiveButton("Open SDK Manager", (d, w) -> {
                        startActivity(new Intent(requireContext(), SdkManagerActivity.class));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        binding.dependencyInputLayout.setErrorEnabled(false);

        binding.dependencyInfo.setVisibility(View.GONE);
        binding.overallProgress.setVisibility(View.VISIBLE);
        binding.dependenciesRecyclerView.setVisibility(View.VISIBLE);

        setDownloadState(true);

        int minApi = 21;
        try {
            String minApiStr = binding.actChooseminapi.getText().toString().trim();
            if (!minApiStr.isEmpty()) {
                minApi = Integer.parseInt(minApiStr);
            }
        } catch (Exception ignored) {
        }

        var resolver = new DependencyResolver(group, artifact, version,
                binding.cbSkipSubdependencies.isChecked(), buildSettings, minApi);
        var handler = new Handler(Looper.getMainLooper());

        downloadExecutor.execute(() -> {
            BuiltInLibraries.maybeExtractCoreLambdaStubsJar();

            resolver.resolveDependency(new DependencyResolver.DependencyResolverCallback() {
                @Override
                public void onResolving(@NonNull Artifact artifact, @NonNull Artifact dependency) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dependency);
                        item.setState(DependencyDownloadItem.DownloadState.RESOLVING);
                        dependencyAdapter.updateDependency(item);
                    });
                }

                @Override
                public void onResolutionComplete(@NonNull Artifact dep) {
                    handler.post(() -> updateDependencyState(dep, DependencyDownloadItem.DownloadState.COMPLETED));
                }

                @Override
                public void onArtifactNotFound(@NonNull Artifact dep) {
                    handler.post(() -> {
                        setDownloadState(false);
                        SketchwareUtil.showAnErrorOccurredDialog(getActivity(), "Dependency '" + dep + "' not found");
                    });
                }

                @Override
                public void onSkippingResolution(@NonNull Artifact dep) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setState(DependencyDownloadItem.DownloadState.COMPLETED);
                        dependencyAdapter.updateDependency(item);
                    });
                }

                @Override
                public void onVersionNotFound(@NonNull Artifact dep) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setError("Version not available");
                        dependencyAdapter.updateDependency(item);
                    });
                }

                @Override
                public void onDependenciesNotFound(@NonNull Artifact dep) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setError("Dependencies not found");
                        dependencyAdapter.updateDependency(item);
                    });
                }

                @Override
                public void onInvalidScope(@NonNull Artifact dep, @NonNull String scope) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setState(DependencyDownloadItem.DownloadState.COMPLETED);
                        dependencyAdapter.updateDependency(item);
                    });
                }

                @Override
                public void invalidPackaging(@NonNull Artifact dep) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setError("Invalid packaging");
                        dependencyAdapter.updateDependency(item);
                    });
                }

                @Override
                public void onDownloadStart(@NonNull Artifact dep) {
                    handler.post(() -> {
                        setDownloadState(true);
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setState(DependencyDownloadItem.DownloadState.DOWNLOADING);
                        dependencyAdapter.updateDependency(item);
                        updateOverallProgress();
                    });
                }

                @Override
                public void onDownloadEnd(@NonNull Artifact dep) {
                    handler.post(() -> {
                        updateDependencyState(dep, DependencyDownloadItem.DownloadState.COMPLETED);
                        updateOverallProgress();
                    });
                }

                @Override
                public void onDownloadError(@NonNull Artifact dep, @NonNull Throwable e) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dep);
                        item.setError(e.getMessage());
                        dependencyAdapter.updateDependency(item);
                        setDownloadState(false);
                        SketchwareUtil.showAnErrorOccurredDialog(getActivity(),
                                "Downloading dependency '" + dep + "' failed: " + Log.getStackTraceString(e));
                    });
                }

                @Override
                public void unzipping(@NonNull Artifact artifact) {
                    handler.post(() -> updateDependencyState(artifact, DependencyDownloadItem.DownloadState.UNZIPPING));
                }

                @Override
                public void dexing(@NonNull Artifact dep) {
                    handler.post(() -> updateDependencyState(dep, DependencyDownloadItem.DownloadState.DEXING));
                }

                @Override
                public void dexingFailed(@NonNull Artifact dependency, @NonNull Exception e) {
                    handler.post(() -> {
                        DependencyDownloadItem item = findOrCreateDependencyItem(dependency);
                        item.setError("Dexing failed: " + e.getMessage());
                        dependencyAdapter.updateDependency(item);
                        setDownloadState(false);
                        SketchwareUtil.showAnErrorOccurredDialog(getActivity(),
                                "Dexing dependency '" + dependency + "' failed: " + Log.getStackTraceString(e));
                    });
                }

                @Override
                public void onTaskCompleted(@NonNull List<String> dependencies) {
                    handler.post(() -> {
                        SketchwareUtil.toast("Library downloaded successfully");
                        if (!notAssociatedWithProject) {
                            var fileContent = FileUtil.readFile(localLibFile);
                            var enabledLibs = gson.fromJson(fileContent, Helper.TYPE_MAP_LIST);
                            enabledLibs.addAll(dependencies.stream()
                                    .map(name -> createLibraryMap(name, dependencyName))
                                    .toList());
                            FileUtil.writeFile(localLibFile, gson.toJson(enabledLibs));
                        }
                        if (getActivity() == null) return;
                        dismissAllowingStateLoss();
                        if (onLibraryDownloadedTask != null) onLibraryDownloadedTask.invoke();
                    });
                }
            });
        });
    }

    private DependencyDownloadItem findOrCreateDependencyItem(Artifact artifact) {
        for (DependencyDownloadItem item : downloadItems) {
            if (item.getArtifact().equals(artifact)) {
                return item;
            }
        }
        DependencyDownloadItem newItem = new DependencyDownloadItem(artifact);
        downloadItems.add(newItem);
        dependencyAdapter.addDependency(newItem);
        return newItem;
    }

    private void updateDependencyState(Artifact artifact, DependencyDownloadItem.DownloadState state) {
        for (DependencyDownloadItem item : downloadItems) {
            if (item.getArtifact().equals(artifact)) {
                item.setState(state);
                dependencyAdapter.updateDependency(item);
                break;
            }
        }
    }

    private void updateOverallProgress() {
        int completed = 0;
        for (DependencyDownloadItem item : downloadItems) {
            if (item.isCompleted()) completed++;
        }

        if (!downloadItems.isEmpty()) {
            binding.overallProgress.setIndeterminate(false);
            binding.overallProgress.setProgress((completed * 100) / downloadItems.size());
        }
    }

    private void setDownloadState(boolean downloading) {
        if (downloading) {
            binding.btnDownload.setVisibility(View.GONE);
        } else {
            binding.btnDownload.setVisibility(View.VISIBLE);
            binding.btnDownload.setEnabled(true);
        }

        binding.dependencyInput.setEnabled(!downloading);
        binding.cbSkipSubdependencies.setEnabled(!downloading);
        setCancelable(!downloading);

        if (!downloading) {
            binding.dependencyInfo.setVisibility(View.VISIBLE);
            binding.overallProgress.setVisibility(View.GONE);
            binding.dependenciesRecyclerView.setVisibility(View.GONE);
            binding.dependencyInfo.setText(R.string.local_library_manager_dependency_info);

            downloadItems.clear();
            dependencyAdapter.setDependencies(new ArrayList<>());
        }
    }

    private boolean isNetworkAvailable() {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public interface OnLibraryDownloadedTask {
        void invoke();
    }
}