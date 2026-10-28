package mod.jbk.build.compiler.resource;

import static org.sketchware.daygreen.Config.VAR_DEFAULT_TARGET_SDK_VERSION;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import a.a.a.Jp;
import a.a.a.ProjectBuilder;
import a.a.a.zy;
import mod.agus.jcoderz.editor.manage.library.locallibrary.ManageLocalLibrary;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.project.ProjectSettings;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.diagnostic.MissingFileException;
import mod.jbk.util.LogUtil;
import pro.sketchware.SketchApplication;
import pro.sketchware.utility.BinaryExecutor;
import pro.sketchware.utility.FileUtil;

/**
 * A class responsible for compiling a Project's resources.
 * Supports AAPT2.
 */
public class ResourceCompiler {

    /**
     * About log tags: add ":" and the first letter of the function's name camelCase'd.
     * For example, in thisIsALongFunctionName, you should use this:
     * <pre>
     *     TAG + ":tIALFN"
     * </pre>
     */
    private static final String TAG = "AppBuilder";
    private final boolean willBuildAppBundle;
    private final File aaptFile;
    private final BuildProgressReceiver progressReceiver;
    private final ProjectBuilder builder;

    public ResourceCompiler(ProjectBuilder builder, File aapt, boolean willBuildAppBundle, BuildProgressReceiver receiver) {
        this.willBuildAppBundle = willBuildAppBundle;
        aaptFile = aapt;
        progressReceiver = receiver;
        this.builder = builder;
    }

    public void compile() throws IOException, zy, MissingFileException {
        Compiler resourceCompiler;
        resourceCompiler = new Aapt2Compiler(builder, aaptFile, willBuildAppBundle);

        resourceCompiler.setProgressListener(new Compiler.ProgressListener() {
            @Override
            void onProgressUpdate(String newProgress, int step) {
                if (progressReceiver != null) progressReceiver.onProgress(newProgress, step);
            }
        });
        resourceCompiler.compile();
    }
    
    // --- PERUBAHAN B3: Dukungan eksekusi Re-link dari luar (oleh ResourceShrinker) ---
    public void compileAndRelinkShrunk(String filteredResPath) throws zy, MissingFileException {
        if (progressReceiver != null) progressReceiver.onProgress("Re-linking shrunk resources...", 16);
        Aapt2Compiler compiler = new Aapt2Compiler(builder, aaptFile, willBuildAppBundle);
        compiler.compileAndRelinkShrunk(filteredResPath);
    }

    /**
     * A base class of a resource compiler.
     */
    interface Compiler {

        /**
         * Compile a project's resources fully.
         */
        void compile() throws zy, MissingFileException;

        /**
         * Set a progress listener to compiling.
         *
         * @param listener The listener object
         */
        void setProgressListener(ProgressListener listener);

        /**
         * A listener for progress on compilation.
         */
        abstract class ProgressListener {
            /**
             * The compiler has reached a new phase the user should know about.
             *
             * @param newProgress A String provided by the resource compiler the user should see.
             */
            abstract void onProgressUpdate(String newProgress, int step);
        }
    }

    /**
     * A {@link Compiler} implementing AAPT2.
     */
    static class Aapt2Compiler implements Compiler {

        private final boolean buildAppBundle;

        private final File aapt2;
        private final ProjectBuilder buildHelper;
        private final File compiledBuiltInLibraryResourcesDirectory;
        private ProgressListener progressListener;

        public Aapt2Compiler(ProjectBuilder buildHelper, File aapt2, boolean buildAppBundle) {
            this.buildHelper = buildHelper;
            this.aapt2 = aapt2;
            this.buildAppBundle = buildAppBundle;
            compiledBuiltInLibraryResourcesDirectory = new File(SketchApplication.getContext().getCacheDir(), "compiledLibs");
        }

        @Override
        public void compile() throws zy, MissingFileException {
            String outputPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            emptyOrCreateDirectory(outputPath);

            long savedTimeMillis = System.currentTimeMillis();
            if (progressListener != null) {
                progressListener.onProgressUpdate("Compiling resources with AAPT2...", 9);
            }

            int coreCount = Runtime.getRuntime().availableProcessors();
            int threadPoolSize = Math.max(2, coreCount - 1);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadPoolSize);
            java.util.List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();

            tasks.add(executor.submit(() -> {
                try { compileBuiltInLibraryResources(); } catch (Exception e) { throw new RuntimeException(e); }
            }));
            tasks.add(executor.submit(() -> {
                try { compileLocalLibraryResources(outputPath); } catch (Exception e) { throw new RuntimeException(e); }
            }));
            tasks.add(executor.submit(() -> {
                try { compileProjectResources(outputPath); } catch (Exception e) { throw new RuntimeException(e); }
            }));
            tasks.add(executor.submit(() -> {
                try { compileImportedResources(outputPath); } catch (Exception e) { throw new RuntimeException(e); }
            }));

            for (var task : tasks) {
                try {
                    task.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    executor.shutdownNow();
                    Throwable cause = e.getCause();
                    if (cause instanceof zy zye) throw zye;
                    if (cause instanceof MissingFileException mfe) throw mfe;
                    throw new RuntimeException(cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            executor.shutdown();

            LogUtil.d(TAG + ":c", "Compiling all resources (parallel) took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            savedTimeMillis = System.currentTimeMillis();
            link();
            LogUtil.d(TAG + ":c", "Linking resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
        }

        /**
         * Links the project's resources using AAPT2.
         *
         * @throws zy Thrown to be caught by DesignActivity to show an error Snackbar.
         */
        public void link() throws zy, MissingFileException {
            String resourcesPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            if (progressListener != null)
                progressListener.onProgressUpdate("Linking resources with AAPT2...", 10);

            ArrayList<String> args = new ArrayList<>();
            args.add(aapt2.getAbsolutePath());
            args.add("link");
            if (buildAppBundle) {
                args.add("--proto-format");
            }
            args.add("--allow-reserved-package-id");
            args.add("--auto-add-overlay");
            args.add("--no-version-vectors");
            args.add("--no-version-transitions");

            args.add("--min-sdk-version");
            args.add(String.valueOf(buildHelper.settings.getMinSdkVersion()));
            args.add("--target-sdk-version");
            args.add(buildHelper.settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION, String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION)));

            args.add("--version-code");
            String versionCode = buildHelper.yq.versionCode;
            args.add((versionCode == null || versionCode.isEmpty()) ? "1" : versionCode);
            args.add("--version-name");
            String versionName = buildHelper.yq.versionName;
            args.add((versionName == null || versionName.isEmpty()) ? "1.0" : versionName);

            // --- PERUBAHAN B3: Dukungan file ID untuk stable-ids ---
            // Tujuannya agar saat resource dihapus, ID resource yang tersisa tidak bergeser nilainya
            // dan tidak merusak class di dalam dex yang sudah terkompilasi
            File idFile = new File(buildHelper.yq.binDirectoryPath, "resource_ids.txt");
            args.add("--emit-ids");
            args.add(idFile.getAbsolutePath());
            if (idFile.exists() && idFile.length() > 0) {
                args.add("--stable-ids");
                args.add(idFile.getAbsolutePath());
            }

            args.add("-I");
            String customAndroidSdk = buildHelper.build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH, "");
            String effectiveAndroidJar = customAndroidSdk.isEmpty() ? buildHelper.androidJarPath : customAndroidSdk;
            File androidJarFile = new File(effectiveAndroidJar);
            if (!androidJarFile.exists() || !androidJarFile.isFile() || androidJarFile.length() == 0) {
                throw new zy("Android SDK platform JAR is missing or corrupted at:\n" + effectiveAndroidJar
                        + "\n\nPlease open App Settings > SDK Manager to download the Android SDK (e.g. API 34).");
            }
            args.add(effectiveAndroidJar);

            /* Add assets imported by vanilla method */
            linkingAssertDirectoryExists(buildHelper.yq.assetsPath);
            args.add("-A");
            args.add(buildHelper.yq.assetsPath);

            /* Add imported assets */
            String importedAssetsPath = buildHelper.fpu.getPathAssets(buildHelper.yq.sc_id);
            if (FileUtil.isExistFile(importedAssetsPath)) {
                args.add("-A");
                args.add(importedAssetsPath);
            }

            /* Add built-in libraries' assets */
            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasAssets()) {
                    String assetsPath = BuiltInLibraries.getLibraryAssetsPath(library.getName());

                    linkingAssertDirectoryExists(assetsPath);
                    args.add("-A");
                    args.add(assetsPath);
                }
            }

            /* Add local libraries' assets */
            for (String localLibraryAssetsDirectory : new ManageLocalLibrary(buildHelper.yq.sc_id).getAssets()) {
                linkingAssertDirectoryExists(localLibraryAssetsDirectory);
                args.add("-A");
                args.add(localLibraryAssetsDirectory);
            }

            /* Include compiled built-in library resources */
            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasResources()) {
                    File libZip = new File(compiledBuiltInLibraryResourcesDirectory, library.getName() + ".zip");
                    if (!libZip.exists() || libZip.length() == 0) {
                        throw new zy("Built-in library resource archive is missing/empty: " + libZip.getAbsolutePath()
                                + " (library: " + library.getName() + ")");
                    }
                    args.add("-R");
                    args.add(libZip.getAbsolutePath());
                }
            }

            /* Include compiled local libraries' resources */
            File[] filesInCompiledResourcesPath = new File(resourcesPath).listFiles();
            if (filesInCompiledResourcesPath != null) {
                java.util.Arrays.sort(filesInCompiledResourcesPath, (f1, f2) -> f1.getName().compareTo(f2.getName()));
                for (File file : filesInCompiledResourcesPath) {
                    if (file.isFile()) {
                        if (!file.getName().equals("project.zip") && !file.getName().equals("project-imported.zip")) {
                            if (file.length() == 0) {
                                throw new zy("Local library resource archive is empty: " + file.getAbsolutePath());
                            }
                            args.add("-R");
                            args.add(file.getAbsolutePath());
                        }
                    }
                }
            }

            /* Include compiled project resources */
            File projectArchive = new File(resourcesPath, "project.zip");
            if (projectArchive.exists()) {
                if (projectArchive.length() == 0) {
                    throw new zy("project.zip is empty: " + projectArchive.getAbsolutePath());
                }
                args.add("-R");
                args.add(projectArchive.getAbsolutePath());
            }

            /* Include compiled imported project resources */
            File projectImportedArchive = new File(resourcesPath, "project-imported.zip");
            if (projectImportedArchive.exists()) {
                if (projectImportedArchive.length() == 0) {
                    throw new zy("project-imported.zip is empty: " + projectImportedArchive.getAbsolutePath());
                }
                args.add("-R");
                args.add(projectImportedArchive.getAbsolutePath());
            }

            /* Add R.java */
            linkingAssertDirectoryExists(buildHelper.yq.rJavaDirectoryPath);
            args.add("--java");
            args.add(buildHelper.yq.rJavaDirectoryPath);

            /* Output AAPT2's generated ProGuard rules to a.a.a.yq.aapt_rules */
            args.add("--proguard");
            args.add(buildHelper.yq.proguardAaptRules);

            /* Add AndroidManifest.xml */
            linkingAssertFileExists(buildHelper.yq.androidManifestPath);
            args.add("--manifest");
            args.add(buildHelper.yq.androidManifestPath);

            /* Use the generated R.java for used libraries */
            String extraPackages = buildHelper.getLibraryPackageNames();
            if (!extraPackages.isEmpty()) {
                args.add("--extra-packages");
                args.add(extraPackages);
            }

            /* Output the APK only with resources to a.a.a.yq.C */
            args.add("-o");
            args.add(buildHelper.yq.resourcesApkPath);

            LogUtil.d(TAG + ":l", args.toString());
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(args);
            if (!executor.execute().isEmpty()) {
                String log = executor.getLog();
                LogUtil.e(TAG + ":l", log);
                if (log.contains("failed to open APK")) {
                    log += "\n\n[Tip] AAPT2 failed to open an archive (SDK jar or resource zip). Ensure your Android SDK is downloaded in App Settings > SDK Manager.";
                }
                throw new zy(log);
            }
        }
        
        // --- PERUBAHAN B3: Melakukan kompilasi ulang dari folder project (yang disusutkan) lalu menghubungkannya lagi
        public void compileAndRelinkShrunk(String filteredResPath) throws zy, MissingFileException {
            String outputPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            File shrunkZip = new File(outputPath, "project_shrunk.zip");
            
            ArrayList<String> commands = new ArrayList<>();
            commands.add(aapt2.getAbsolutePath());
            commands.add("compile");
            commands.add("--dir");
            commands.add(filteredResPath);
            commands.add("-o");
            commands.add(shrunkZip.getAbsolutePath());
            
            LogUtil.d(TAG + ":cARS", "Compiling filtered resources: " + commands);
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(commands);
            if (!executor.execute().isEmpty()) {
                throw new zy(executor.getLog());
            }

            relinkShrunk(shrunkZip);
        }

        private void relinkShrunk(File filteredProjectZip) throws zy, MissingFileException {
            String resourcesPath = buildHelper.yq.binDirectoryPath + File.separator + "res";

            ArrayList<String> args = new ArrayList<>();
            args.add(aapt2.getAbsolutePath());
            args.add("link");
            if (buildAppBundle) args.add("--proto-format");
            args.add("--allow-reserved-package-id");
            args.add("--auto-add-overlay");
            args.add("--no-version-vectors");
            args.add("--no-version-transitions");

            args.add("--min-sdk-version");
            args.add(String.valueOf(buildHelper.settings.getMinSdkVersion()));
            args.add("--target-sdk-version");
            args.add(buildHelper.settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION, String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION)));

            args.add("--version-code");
            String versionCode = buildHelper.yq.versionCode;
            args.add((versionCode == null || versionCode.isEmpty()) ? "1" : versionCode);
            args.add("--version-name");
            String versionName = buildHelper.yq.versionName;
            args.add((versionName == null || versionName.isEmpty()) ? "1.0" : versionName);

            File idFile = new File(buildHelper.yq.binDirectoryPath, "resource_ids.txt");
            if (idFile.exists() && idFile.length() > 0) {
                args.add("--stable-ids");
                args.add(idFile.getAbsolutePath());
            }

            args.add("-I");
            String customAndroidSdk = buildHelper.build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH, "");
            String effectiveAndroidJar = customAndroidSdk.isEmpty() ? buildHelper.androidJarPath : customAndroidSdk;
            args.add(effectiveAndroidJar);

            linkingAssertDirectoryExists(buildHelper.yq.assetsPath);
            args.add("-A"); args.add(buildHelper.yq.assetsPath);

            String importedAssetsPath = buildHelper.fpu.getPathAssets(buildHelper.yq.sc_id);
            if (FileUtil.isExistFile(importedAssetsPath)) {
                args.add("-A"); args.add(importedAssetsPath);
            }

            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasAssets()) {
                    args.add("-A"); args.add(BuiltInLibraries.getLibraryAssetsPath(library.getName()));
                }
            }

            for (String localLibraryAssetsDirectory : new ManageLocalLibrary(buildHelper.yq.sc_id).getAssets()) {
                args.add("-A"); args.add(localLibraryAssetsDirectory);
            }

            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasResources()) {
                    args.add("-R"); args.add(new File(compiledBuiltInLibraryResourcesDirectory, library.getName() + ".zip").getAbsolutePath());
                }
            }

            File[] filesInCompiledResourcesPath = new File(resourcesPath).listFiles();
            if (filesInCompiledResourcesPath != null) {
                java.util.Arrays.sort(filesInCompiledResourcesPath, (f1, f2) -> f1.getName().compareTo(f2.getName()));
                for (File file : filesInCompiledResourcesPath) {
                    if (file.isFile()) {
                        if (!file.getName().equals("project.zip") && !file.getName().equals("project-imported.zip") && !file.getName().equals("project_shrunk.zip")) {
                            args.add("-R"); args.add(file.getAbsolutePath());
                        }
                    }
                }
            }

            args.add("-R"); args.add(filteredProjectZip.getAbsolutePath());

            File projectImportedArchive = new File(resourcesPath, "project-imported.zip");
            if (projectImportedArchive.exists()) {
                args.add("-R"); args.add(projectImportedArchive.getAbsolutePath());
            }

            args.add("--java"); args.add(buildHelper.yq.rJavaDirectoryPath);
            args.add("--proguard"); args.add(buildHelper.yq.proguardAaptRules);
            args.add("--manifest"); args.add(buildHelper.yq.androidManifestPath);

            String extraPackages = buildHelper.getLibraryPackageNames();
            if (!extraPackages.isEmpty()) {
                args.add("--extra-packages"); args.add(extraPackages);
            }

            args.add("-o"); args.add(buildHelper.yq.resourcesApkPath);

            LogUtil.d(TAG + ":rs", args.toString());
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(args);
            if (!executor.execute().isEmpty()) {
                throw new zy(executor.getLog());
            }
        }

        private void compileProjectResources(String outputPath) throws zy, MissingFileException {
            compilingAssertDirectoryExists(buildHelper.yq.resDirectoryPath);

            ArrayList<String> commands = new ArrayList<>();
            commands.add(aapt2.getAbsolutePath());
            commands.add("compile");
            commands.add("--dir");
            commands.add(buildHelper.yq.resDirectoryPath);
            commands.add("-o");
            String outputZip = outputPath + File.separator + "project.zip";
            commands.add(outputZip);
            LogUtil.d(TAG + ":cPR", "Now executing: " + commands);
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(commands);
            if (!executor.execute().isEmpty()) {
                LogUtil.e(TAG, executor.getLog());
                throw new zy(executor.getLog());
            }

            File outFile = new File(outputZip);
            if (!outFile.exists() || outFile.length() == 0) {
                throw new zy("aapt2 compile produced an empty or missing project.zip (exit code: "
                        + executor.getExitCode() + "). The process may have crashed or been killed.");
            }
        }

        private void emptyOrCreateDirectory(String path) {
            if (FileUtil.isExistFile(path)) {
                FileUtil.deleteFile(path);
            }
            FileUtil.makeDir(path);
        }

        private void compileLocalLibraryResources(String outputPath) throws zy, MissingFileException {
            int localLibrariesCount = buildHelper.mll.getResLocalLibrary().size();
            LogUtil.d(TAG + ":cLLR", "About to compile " + localLibrariesCount
                    + " local " + (localLibrariesCount == 1 ? "library" : "libraries"));
            for (String localLibraryResDirectory : buildHelper.mll.getResLocalLibrary()) {
                File localLibraryDirectory = new File(localLibraryResDirectory).getParentFile();
                if (localLibraryDirectory != null) {
                    compilingAssertDirectoryExists(localLibraryResDirectory);

                    ArrayList<String> commands = new ArrayList<>();
                    commands.add(aapt2.getAbsolutePath());
                    commands.add("compile");
                    commands.add("--dir");
                    commands.add(localLibraryResDirectory);
                    commands.add("-o");
                    commands.add(outputPath + File.separator + localLibraryDirectory.getName() + ".zip");

                    LogUtil.d(TAG + ":cLLR", "Now executing: " + commands);
                    BinaryExecutor executor = new BinaryExecutor();
                    executor.setCommands(commands);
                    if (!executor.execute().isEmpty()) {
                        LogUtil.e(TAG, executor.getLog());
                        throw new zy(executor.getLog());
                    }
                }
            }
        }

        private void compileBuiltInLibraryResources() throws zy, MissingFileException {
            compiledBuiltInLibraryResourcesDirectory.mkdirs();
            for (Jp builtInLibrary : buildHelper.builtInLibraryManager.getLibraries()) {
                if (builtInLibrary.hasResources()) {
                    File cachedCompiledResources = new File(compiledBuiltInLibraryResourcesDirectory, builtInLibrary.getName() + ".zip");
                    String libraryResources = BuiltInLibraries.getLibraryResourcesPath(builtInLibrary.getName());

                    compilingAssertDirectoryExists(libraryResources);

                    if (isBuiltInLibraryRecompilingNeeded(cachedCompiledResources)) {
                        ArrayList<String> commands = new ArrayList<>();
                        commands.add(aapt2.getAbsolutePath());
                        commands.add("compile");
                        commands.add("--dir");
                        commands.add(libraryResources);
                        commands.add("-o");
                        commands.add(cachedCompiledResources.getAbsolutePath());

                        LogUtil.d(TAG + ":cBILR", "Now executing: " + commands);
                        BinaryExecutor executor = new BinaryExecutor();
                        executor.setCommands(commands);
                        if (!executor.execute().isEmpty()) {
                            LogUtil.e(TAG + ":cBILR", executor.getLog());
                            throw new zy(executor.getLog());
                        }
                    } else {
                        LogUtil.d(TAG + ":cBILR", "Skipped resource recompilation for built-in library " + builtInLibrary.getName());
                    }
                }
            }
        }

        private boolean isBuiltInLibraryRecompilingNeeded(File cachedCompiledResources) {
            if (cachedCompiledResources.exists()) {
                try {
                    Context context = SketchApplication.getContext();
                    return context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
                            .lastUpdateTime > cachedCompiledResources.lastModified();
                } catch (PackageManager.NameNotFoundException e) {
                    LogUtil.e(TAG + ":iBILRN", "Couldn't get package info about ourselves: " + e.getMessage(), e);
                }
            } else {
                LogUtil.d(TAG + ":iBILRN", "File " + cachedCompiledResources.getAbsolutePath()
                        + " doesn't exist, forcing compilation");
            }
            return true;
        }

        private void compileImportedResources(String outputPath) throws zy {
            if (FileUtil.isExistFile(buildHelper.fpu.getPathResource(buildHelper.yq.sc_id))
                    && new File(buildHelper.fpu.getPathResource(buildHelper.yq.sc_id)).length() != 0) {
                ArrayList<String> commands = new ArrayList<>();
                commands.add(aapt2.getAbsolutePath());
                commands.add("compile");
                commands.add("--dir");
                commands.add(buildHelper.fpu.getPathResource(buildHelper.yq.sc_id));
                commands.add("-o");
                commands.add(outputPath + File.separator + "project-imported.zip");
                LogUtil.d(TAG + ":cIR", "Now executing: " + commands);
                BinaryExecutor executor = new BinaryExecutor();
                executor.setCommands(commands);
                if (!executor.execute().isEmpty()) {
                    LogUtil.e(TAG, executor.getLog());
                    throw new zy(executor.getLog());
                }
            }
        }

        private void compilingAssertDirectoryExists(String directoryPath) throws MissingFileException {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                throw new MissingFileException(directory, MissingFileException.STEP_RESOURCE_COMPILING, true);
            }
        }

        public void linkingAssertFileExists(String filePath) throws MissingFileException {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new MissingFileException(file, MissingFileException.STEP_RESOURCE_LINKING, false);
            }
        }

        public void linkingAssertDirectoryExists(String filePath) throws MissingFileException {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new MissingFileException(file, MissingFileException.STEP_RESOURCE_LINKING, true);
            }
        }

        @Override
        public void setProgressListener(ProgressListener listener) {
            progressListener = listener;
        }
    }
}