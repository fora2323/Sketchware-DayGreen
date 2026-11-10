package mod.jbk.build.compiler.native_code;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a.a.a.ProjectBuilder;
import a.a.a.zy;
import mod.hey.studios.build.BuildSettings;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.util.LogUtil;
import pro.sketchware.SketchApplication;
import pro.sketchware.utility.FileUtil;

/**
 * Compiles a project's native (C/C++) code with the Android NDK.
 * <p>
 * Priority:
 * 1. A custom CMakeLists.txt (CMake is used when it is runnable on the device).
 * 2. Android.mk (+ optional Application.mk), read and compiled directly with the NDK's Clang.
 * 3. Plain C/C++ sources, compiled directly with the NDK's Clang.
 */
public class NativeCompiler {
    private static final String TAG = "NativeCompiler";

    private static final String AUTO_GENERATED_MARKER = "# DayGreen: auto-generated. Remove this line to keep your own edits.";
    private static final String[] AUTO_ABIS = {"arm64-v8a", "armeabi-v7a"};
    private static final int DEFAULT_PLATFORM_LEVEL = 21;
    private static final int MAX_LOG_CHARS_IN_ERROR = 4000;

    private static final Pattern MK_ASSIGN =
            Pattern.compile("^([A-Za-z0-9_]+)\\s*(:=|\\+=|\\?=|=)\\s*(.*)$");

    private final ProjectBuilder builder;
    private final BuildProgressReceiver progressReceiver;

    public NativeCompiler(ProjectBuilder builder, BuildProgressReceiver receiver) {
        this.builder = builder;
        this.progressReceiver = receiver;
    }

    public static File getToolsDirectory() {
        return new File(SketchApplication.getContext().getFilesDir(), "native");
    }

    public void compile() throws Exception {
        String nativeSourcePath = builder.fpu.getPathNative(builder.yq.sc_id);
        File sourceDir = new File(nativeSourcePath);
        if (!sourceDir.isDirectory()) {
            return;
        }

        File[] initialFiles = sourceDir.listFiles();
        if (initialFiles == null || initialFiles.length == 0) {
            return;
        }

        List<File> nativeSourceFiles = new ArrayList<>();
        findSourceFiles(sourceDir, nativeSourceFiles);

        File cmakeLists = findCMakeListsFile(sourceDir);
        if (cmakeLists != null && cmakeLists.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cmakeLists))) {
                String firstLine = reader.readLine();
                if (firstLine != null && (firstLine.contains("auto-generated") || firstLine.equals(AUTO_GENERATED_MARKER))) {
                    cmakeLists.delete();
                    cmakeLists = null;
                }
            } catch (Exception ignored) {}
        }

        boolean hasCustomCMakeLists = cmakeLists != null && cmakeLists.isFile();
        File androidMk = findAndroidMk(sourceDir);
        File applicationMk = findApplicationMk(sourceDir);

        if (nativeSourceFiles.isEmpty() && !hasCustomCMakeLists && androidMk == null) {
            LogUtil.d(TAG, "No native C/C++ source files or custom CMakeLists.txt found in " + nativeSourcePath + "; skipping native compilation.");
            return;
        }

        if (progressReceiver != null) {
            progressReceiver.onProgress("Compiling Native code...", 14);
        }

        File binDir = new File(SketchApplication.getContext().getFilesDir(), "bin");
        File toolsDir = getToolsDirectory();

        File ndkDir = findNdkDirectory(binDir, toolsDir);
        File toolchainFile = findToolchainFile(ndkDir, toolsDir);
        File cmakeBinary = findCmakeBinary(binDir, toolsDir);

        int platformLevel = DEFAULT_PLATFORM_LEVEL;
        if (builder.settings != null) {
            int minSdk = builder.settings.getMinSdkVersion();
            if (minSdk > 0) {
                platformLevel = minSdk;
            }
        }

        boolean cmakeUsable = hasCustomCMakeLists
                && toolchainFile != null && toolchainFile.isFile()
                && isRunnable(cmakeBinary);

        if (cmakeUsable) {
            // The user's CMakeLists.txt is the source of truth. If it fails, the real CMake
            // error is shown instead of silently switching to a guessed build.
            compileWithCmake(sourceDir, cmakeBinary, toolchainFile, toolsDir, nativeSourcePath, platformLevel);
            return;
        }

        if (ndkDir == null || !ndkDir.isDirectory()) {
            throw new zy("Android NDK not found. Please install Android NDK (r29) in App Settings > Build Tools.");
        }

        if (hasCustomCMakeLists) {
            LogUtil.w(TAG, "CMakeLists.txt found but CMake is not runnable on this device; using Clang with values parsed from CMakeLists.txt.");
            compileDirectWithClang(ndkDir, sourceDir, nativeSourceFiles, parseCMakeLists(cmakeLists), platformLevel);
            return;
        }

        if (androidMk != null) {
            compileWithAndroidMk(ndkDir, sourceDir, androidMk, applicationMk, nativeSourceFiles, platformLevel);
            return;
        }

        CMakeInfo defaultInfo = new CMakeInfo();
        applyApplicationMk(defaultInfo, parseApplicationMk(applicationMk));
        compileDirectWithClang(ndkDir, sourceDir, nativeSourceFiles, defaultInfo, platformLevel);
    }

    public static File findCMakeListsFile(File sourceDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) return null;
        File[] candidates = {
                new File(sourceDir, "CMakeLists.txt"),
                new File(sourceDir, "CMakeList.txt"),
                new File(sourceDir, "cmakelists.txt"),
                new File(sourceDir, "cmakelist.txt")
        };
        for (File c : candidates) {
            if (c.isFile()) return c;
        }
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    String name = f.getName();
                    if (name.equalsIgnoreCase("cmakelists.txt") || name.equalsIgnoreCase("cmakelist.txt")) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isNdkRoot(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        String name = dir.getName().toLowerCase(Locale.ROOT);
        if (name.equals("simpleperf") || name.equals("sources") || name.equals("platforms") || name.equals("cmake")) {
            return false;
        }
        return new File(dir, "build/cmake/android.toolchain.cmake").isFile() ||
               new File(dir, "source.properties").isFile() ||
               new File(dir, "toolchains").isDirectory() ||
               (new File(dir, "bin").isDirectory() && containsClang(new File(dir, "bin")));
    }

    private static File findNdkDirectory(File binDir, File toolsDir) {
        File[] candidates = {
                new File(toolsDir, "ndk"),
                new File(binDir, "android-ndk"),
                new File(binDir, "ndk"),
                new File(toolsDir, "android-ndk")
        };
        for (File candidate : candidates) {
            if (candidate.isDirectory()) {
                if (isNdkRoot(candidate)) {
                    return candidate;
                }
                File[] children = candidate.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (child.isDirectory() && isNdkRoot(child)) {
                            return child;
                        }
                    }
                }
                return candidate;
            }
        }
        return new File(toolsDir, "ndk");
    }

    public static File findCmakeBinary(File binDir, File toolsDir) {
        File[] candidates = {
                new File(binDir, "cmake/bin/cmake"),
                new File(binDir, "cmake"),
                new File(toolsDir, "cmake/bin/cmake"),
                new File(toolsDir, "cmake/usr/bin/cmake"),
                new File(toolsDir, "cmake/cmake"),
                new File(toolsDir, "cmake")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                makeExecutable(candidate);
                return candidate;
            }
        }
        return null;
    }

    public static File findCmakeBinary(File toolsDir) {
        File binDir = new File(SketchApplication.getContext().getFilesDir(), "bin");
        return findCmakeBinary(binDir, toolsDir);
    }

    public static File findToolchainFile(File ndkDir, File toolsDir) {
        File[] candidates = {
                new File(ndkDir, "build/cmake/android.toolchain.cmake"),
                new File(ndkDir, "android.toolchain.cmake"),
                new File(toolsDir, "ndk/build/cmake/android.toolchain.cmake"),
                new File(toolsDir, "ndk/android.toolchain.cmake")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    public static File findToolchainFile(File toolsDir) {
        File ndkDir = new File(toolsDir, "ndk");
        return findToolchainFile(ndkDir, toolsDir);
    }

    private void compileWithCmake(
            File sourceDir,
            File cmakeBinary,
            File toolchainFile,
            File toolsDir,
            String nativeSourcePath,
            int platformLevel
    ) throws zy {
        File ninjaBinary = findNinja(toolsDir);
        List<String> abis = getAbis(sourceDir);
        File buildRoot = new File(builder.yq.binDirectoryPath, "native");

        for (String abi : abis) {
            if (progressReceiver != null) {
                progressReceiver.onProgress("Compiling Native code (" + abi + ")...", 14);
            }

            File abiBuildDir = new File(buildRoot, abi);
            FileUtil.makeDir(abiBuildDir.getAbsolutePath());

            List<String> configure = new ArrayList<>();
            configure.add(cmakeBinary.getAbsolutePath());
            configure.add("-S" + nativeSourcePath);
            configure.add("-B" + abiBuildDir.getAbsolutePath());
            configure.add("-DANDROID_ABI=" + abi);
            configure.add("-DANDROID_PLATFORM=android-" + platformLevel);
            configure.add("-DCMAKE_BUILD_TYPE=Release");
            configure.add("-DCMAKE_TOOLCHAIN_FILE=" + toolchainFile.getAbsolutePath());
            if (ninjaBinary != null && isRunnable(ninjaBinary)) {
                configure.add("-GNinja");
                configure.add("-DCMAKE_MAKE_PROGRAM=" + ninjaBinary.getAbsolutePath());
            }
            runCommand(configure, "CMake configure failed for " + abi);

            List<String> build = new ArrayList<>();
            build.add(cmakeBinary.getAbsolutePath());
            build.add("--build");
            build.add(abiBuildDir.getAbsolutePath());
            runCommand(build, "Native build failed for " + abi);

            String nativeLibsDir = builder.fpu.getPathNativelibs(builder.yq.sc_id) + File.separator + abi;
            FileUtil.makeDir(nativeLibsDir);
            int copied = searchAndCopySo(abiBuildDir, nativeLibsDir);
            if (copied == 0) {
                LogUtil.w(TAG, "No .so files were produced for " + abi);
            }
        }
    }

    public static class CMakeInfo {
        public String libraryName = "native-lib";
        public List<String> sources = new ArrayList<>();
        public List<String> linkedLibraries = new ArrayList<>();
        public List<String> includeDirs = new ArrayList<>();
        public List<String> cflags = new ArrayList<>();
        public List<String> cppflags = new ArrayList<>();
        public List<String> ldflags = new ArrayList<>();
        public List<String> abis = new ArrayList<>();
        public int platform = -1;
        public String stl = null;
    }

    public static CMakeInfo parseCMakeLists(File cmakeLists) {
        CMakeInfo info = new CMakeInfo();
        if (cmakeLists == null || !cmakeLists.isFile()) {
            return info;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(cmakeLists))) {
            StringBuilder fullText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                int commentIndex = line.indexOf('#');
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                fullText.append(line).append(' ');
            }

            String content = fullText.toString();
            Map<String, String> variables = new HashMap<>();

            // Parse set(VAR value)
            Pattern setPattern = Pattern.compile("set\\s*\\(\\s*([A-Za-z0-9_]+)\\s+([^)]+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher setMatcher = setPattern.matcher(content);
            while (setMatcher.find()) {
                String varName = setMatcher.group(1).trim();
                String varVal = setMatcher.group(2).replaceAll("[\"']", "").trim();
                variables.put(varName, varVal);
            }

            // Parse find_library(VAR NAME)
            Pattern findLibPattern = Pattern.compile("find_library\\s*\\(\\s*([A-Za-z0-9_-]+)\\s+([^)]+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher findLibMatcher = findLibPattern.matcher(content);
            while (findLibMatcher.find()) {
                String varName = findLibMatcher.group(1).trim();
                String varVal = findLibMatcher.group(2).replaceAll("[\"']", "").trim();
                String[] parts = varVal.split("\\s+");
                if (parts.length > 0) {
                    variables.put(varName, parts[0]);
                }
            }

            // Parse project(NAME)
            Pattern projectPattern = Pattern.compile("project\\s*\\(\\s*[\"']?([A-Za-z0-9_-]+)[\"']?", Pattern.CASE_INSENSITIVE);
            Matcher projectMatcher = projectPattern.matcher(content);
            if (projectMatcher.find()) {
                String proj = projectMatcher.group(1).trim();
                if (!proj.isEmpty()) {
                    info.libraryName = proj;
                }
            }

            // Parse add_library(NAME [SHARED|STATIC] source1 source2...)
            Pattern addLibPattern = Pattern.compile("add_library\\s*\\(\\s*([A-Za-z0-9_-]+)\\s*(?:SHARED|STATIC)?\\s*([^)]*)\\)", Pattern.CASE_INSENSITIVE);
            Matcher addLibMatcher = addLibPattern.matcher(content);
            if (addLibMatcher.find()) {
                String libName = addLibMatcher.group(1).replaceAll("[\"']", "").trim();
                if (!libName.isEmpty()) {
                    info.libraryName = libName;
                }
                String sourcesStr = addLibMatcher.group(2).trim();
                if (!sourcesStr.isEmpty()) {
                    String[] srcTokens = sourcesStr.split("[\\s,]+");
                    for (String token : srcTokens) {
                        token = token.replaceAll("[\"']", "").trim();
                        if (token.endsWith(".c") || token.endsWith(".cpp") || token.endsWith(".cc") || token.endsWith(".cxx")) {
                            info.sources.add(token);
                        }
                    }
                }
            }

            // Parse target_link_libraries(NAME lib1 lib2...)
            Pattern linkPattern = Pattern.compile("target_link_libraries\\s*\\(\\s*([A-Za-z0-9_-]+)\\s+([^)]+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher linkMatcher = linkPattern.matcher(content);
            while (linkMatcher.find()) {
                String libsStr = linkMatcher.group(2).trim();
                String[] tokens = libsStr.split("[\\s,]+");
                for (String token : tokens) {
                    token = token.replaceAll("[\"']", "").trim();
                    if (token.isEmpty() || token.equalsIgnoreCase("PUBLIC") ||
                        token.equalsIgnoreCase("PRIVATE") || token.equalsIgnoreCase("INTERFACE")) {
                        continue;
                    }
                    if (token.startsWith("${") && token.endsWith("}")) {
                        String varName = token.substring(2, token.length() - 1).trim();
                        String resolved = variables.get(varName);
                        if (resolved != null && !resolved.isEmpty()) {
                            token = resolved;
                        } else {
                            token = varName;
                        }
                    }
                    if (token.startsWith("-l")) {
                        token = token.substring(2);
                    }
                    if (!token.isEmpty() && !info.linkedLibraries.contains(token)) {
                        info.linkedLibraries.add(token);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "Failed to parse CMakeLists.txt: " + e.getMessage());
        }

        return info;
    }

    // ---------------------------------------------------------------------------------------
    // Android.mk / Application.mk support
    // ---------------------------------------------------------------------------------------

    public static File findAndroidMk(File sourceDir) {
        return findNamedFile(sourceDir, "Android.mk");
    }

    public static File findApplicationMk(File sourceDir) {
        return findNamedFile(sourceDir, "Application.mk");
    }

    private static File findNamedFile(File sourceDir, String name) {
        if (sourceDir == null || !sourceDir.isDirectory()) return null;
        File[] dirs = {sourceDir, new File(sourceDir, "jni")};
        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.isFile() && f.getName().equalsIgnoreCase(name)) return f;
            }
        }
        return null;
    }

    private static List<String> readMakeLines(File file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder pending = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                int comment = line.indexOf('#');
                if (comment != -1) line = line.substring(0, comment);
                line = line.trim();
                if (line.endsWith("\\")) {
                    pending.append(line, 0, line.length() - 1).append(' ');
                    continue;
                }
                pending.append(line);
                if (pending.length() > 0) lines.add(pending.toString().trim());
                pending.setLength(0);
            }
            if (pending.length() > 0) lines.add(pending.toString().trim());
        } catch (IOException e) {
            LogUtil.w(TAG, "Failed to read " + file.getName() + ": " + e.getMessage());
        }
        return lines;
    }

    private static List<String> mkTokens(String value, File mkDir) {
        String v = value.replace("$(LOCAL_PATH)", mkDir.getAbsolutePath())
                .replace("${LOCAL_PATH}", mkDir.getAbsolutePath());
        List<String> tokens = new ArrayList<>();
        for (String t : v.trim().split("\\s+")) {
            t = t.replaceAll("[\"']", "").trim();
            if (!t.isEmpty()) tokens.add(t);
        }
        return tokens;
    }

    private static String resolveMkPath(String token, File baseDir) {
        File f = new File(token);
        if (!f.isAbsolute()) f = new File(baseDir, token);
        return f.getAbsolutePath();
    }

    private static CMakeInfo newMkModule() {
        CMakeInfo info = new CMakeInfo();
        info.libraryName = "";
        return info;
    }

    public static List<CMakeInfo> parseAndroidMk(File androidMk) {
        List<CMakeInfo> modules = new ArrayList<>();
        if (androidMk == null || !androidMk.isFile()) return modules;
        File mkDir = androidMk.getParentFile();
        CMakeInfo current = newMkModule();

        for (String line : readMakeLines(androidMk)) {
            if (line.startsWith("include")) {
                if (line.contains("BUILD_SHARED_LIBRARY")) {
                    if (!current.libraryName.isEmpty()) modules.add(current);
                } else if (line.contains("BUILD_STATIC_LIBRARY") || line.contains("BUILD_EXECUTABLE")
                        || line.contains("BUILD_PREBUILT")) {
                    LogUtil.w(TAG, "Android.mk: only BUILD_SHARED_LIBRARY is supported, skipping: " + line);
                } else if (!line.contains("CLEAR_VARS")) {
                    continue;
                }
                current = newMkModule();
                continue;
            }

            Matcher m = MK_ASSIGN.matcher(line);
            if (!m.matches()) continue;
            boolean append = m.group(2).equals("+=");
            List<String> tokens = mkTokens(m.group(3), mkDir);

            switch (m.group(1)) {
                case "LOCAL_MODULE":
                    if (!tokens.isEmpty()) {
                        String name = tokens.get(0);
                        current.libraryName = (name.startsWith("lib") && name.length() > 3) ? name.substring(3) : name;
                    }
                    break;
                case "LOCAL_SRC_FILES":
                    if (!append) current.sources.clear();
                    for (String t : tokens) {
                        String lower = t.toLowerCase(Locale.ROOT);
                        if (lower.endsWith(".c") || lower.endsWith(".cpp")
                                || lower.endsWith(".cc") || lower.endsWith(".cxx")) {
                            current.sources.add(resolveMkPath(t, mkDir));
                        }
                    }
                    break;
                case "LOCAL_C_INCLUDES":
                    if (!append) current.includeDirs.clear();
                    for (String t : tokens) current.includeDirs.add(resolveMkPath(t, mkDir));
                    break;
                case "LOCAL_CFLAGS":
                    if (!append) current.cflags.clear();
                    current.cflags.addAll(tokens);
                    break;
                case "LOCAL_CPPFLAGS":
                    if (!append) current.cppflags.clear();
                    current.cppflags.addAll(tokens);
                    break;
                case "LOCAL_LDFLAGS":
                    if (!append) current.ldflags.clear();
                    current.ldflags.addAll(tokens);
                    break;
                case "LOCAL_LDLIBS":
                    for (String t : tokens) {
                        if (t.startsWith("-l") && t.length() > 2) {
                            String lib = t.substring(2);
                            if (!current.linkedLibraries.contains(lib)) current.linkedLibraries.add(lib);
                        } else {
                            current.ldflags.add(t);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return modules;
    }

    public static CMakeInfo parseApplicationMk(File applicationMk) {
        CMakeInfo app = new CMakeInfo();
        if (applicationMk == null || !applicationMk.isFile()) return app;
        File mkDir = applicationMk.getParentFile();

        for (String line : readMakeLines(applicationMk)) {
            Matcher m = MK_ASSIGN.matcher(line);
            if (!m.matches()) continue;
            boolean append = m.group(2).equals("+=");
            List<String> tokens = mkTokens(m.group(3), mkDir);

            switch (m.group(1)) {
                case "APP_ABI":
                    for (String t : tokens) {
                        List<String> expanded;
                        if (t.equals("all")) {
                            expanded = Arrays.asList("arm64-v8a", "armeabi-v7a", "x86_64", "x86");
                        } else if (t.equals("all32")) {
                            expanded = Arrays.asList("armeabi-v7a", "x86");
                        } else if (t.equals("all64")) {
                            expanded = Arrays.asList("arm64-v8a", "x86_64");
                        } else {
                            expanded = Arrays.asList(t);
                        }
                        for (String abi : expanded) {
                            if (!app.abis.contains(abi)) app.abis.add(abi);
                        }
                    }
                    break;
                case "APP_PLATFORM":
                    if (!tokens.isEmpty()) {
                        Matcher pm = Pattern.compile("(\\d+)").matcher(tokens.get(0));
                        if (pm.find()) {
                            try {
                                app.platform = Integer.parseInt(pm.group(1));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    break;
                case "APP_STL":
                    if (!tokens.isEmpty()) app.stl = tokens.get(0);
                    break;
                case "APP_CFLAGS":
                    if (!append) app.cflags.clear();
                    app.cflags.addAll(tokens);
                    break;
                case "APP_CPPFLAGS":
                    if (!append) app.cppflags.clear();
                    app.cppflags.addAll(tokens);
                    break;
                case "APP_LDFLAGS":
                    if (!append) app.ldflags.clear();
                    app.ldflags.addAll(tokens);
                    break;
                default:
                    break;
            }
        }
        return app;
    }

    private static void applyApplicationMk(CMakeInfo target, CMakeInfo app) {
        target.abis = new ArrayList<>(app.abis);
        target.platform = app.platform;
        target.stl = app.stl;
        target.cflags.addAll(0, app.cflags);
        target.cppflags.addAll(0, app.cppflags);
        target.ldflags.addAll(app.ldflags);
    }

    private void compileWithAndroidMk(File ndkDir, File sourceDir, File androidMk, File applicationMk,
                                      List<File> nativeSourceFiles, int platformLevel) throws zy {
        List<CMakeInfo> modules = parseAndroidMk(androidMk);
        if (modules.isEmpty()) {
            throw new zy("Android.mk has no BUILD_SHARED_LIBRARY module to compile: " + androidMk.getAbsolutePath());
        }
        CMakeInfo app = parseApplicationMk(applicationMk);
        for (CMakeInfo module : modules) {
            applyApplicationMk(module, app);
            compileDirectWithClang(ndkDir, sourceDir, nativeSourceFiles, module, platformLevel);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Direct Clang compilation
    // ---------------------------------------------------------------------------------------

    private void compileDirectWithClang(File ndkDir, File sourceDir, List<File> sourceFiles, CMakeInfo cmakeInfo, int defaultPlatformLevel) throws zy {
        int platformLevel = cmakeInfo.platform > 0 ? cmakeInfo.platform : defaultPlatformLevel;

        List<File> filesToCompile = new ArrayList<>();
        for (String srcName : cmakeInfo.sources) {
            File srcFile = new File(srcName);
            if (!srcFile.isAbsolute()) {
                srcFile = new File(sourceDir, srcName);
            }
            if (srcFile.isFile()) {
                filesToCompile.add(srcFile);
            }
        }
        if (filesToCompile.isEmpty()) {
            filesToCompile.addAll(sourceFiles);
        }

        if (filesToCompile.isEmpty()) {
            throw new zy("Native build failed: No C/C++ source files (.c, .cpp, .cc) were found to compile.");
        }

        boolean hasCpp = false;
        boolean hasC = false;
        for (File src : filesToCompile) {
            String name = src.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx")) {
                hasCpp = true;
            } else if (name.endsWith(".c")) {
                hasC = true;
            }
        }

        File binDir = findLlvmBinDir(ndkDir);
        if (binDir != null) {
            repairNdkBinaries(binDir);
        }

        List<String> abis = cmakeInfo.abis.isEmpty() ? getAbis(sourceDir) : cmakeInfo.abis;
        int successCount = 0;
        String lastError = null;

        for (String abi : abis) {
            if (progressReceiver != null) {
                progressReceiver.onProgress("Compiling Native code with Clang (" + abi + ")...", 14);
            }

            File clangBinary = findClang(ndkDir, hasCpp, abi, platformLevel);
            if (clangBinary == null && hasCpp) {
                clangBinary = findClang(ndkDir, false, abi, platformLevel);
            }
            if (clangBinary == null) {
                throw new zy("Clang compiler not found in NDK directory: " + ndkDir.getAbsolutePath() +
                        ".\nPlease install Android NDK (r29) in App Settings > Build Tools.");
            }

            makeExecutable(clangBinary);

            try {
                compileSingleAbiWithClang(clangBinary, ndkDir, sourceDir, filesToCompile, abi, cmakeInfo, hasCpp, hasC, platformLevel);
                successCount++;
            } catch (zy e) {
                lastError = e.getMessage();
                LogUtil.w(TAG, "Clang compilation failed for " + abi + ": " + e.getMessage());
            }
        }

        if (successCount == 0) {
            throw new zy("Native compilation with Clang failed: " + lastError);
        }
    }

    private void compileSingleAbiWithClang(
            File clangBinary,
            File ndkDir,
            File sourceDir,
            List<File> sourceFiles,
            String abi,
            CMakeInfo info,
            boolean hasCpp,
            boolean hasC,
            int platformLevel
    ) throws zy {
        String libName = info.libraryName;
        File buildDir = new File(SketchApplication.getContext().getCacheDir(), "native_build" + File.separator + abi);
        FileUtil.makeDir(buildDir.getAbsolutePath());
        File tempOutFile = new File(buildDir, "lib" + libName + ".so");
        if (tempOutFile.exists()) {
            tempOutFile.delete();
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(clangBinary.getAbsolutePath());

        String targetTriple = getTargetTriple(abi, platformLevel);
        if (!clangBinary.getName().startsWith(targetTriple.substring(0, Math.min(6, targetTriple.length())))) {
            cmd.add("-target");
            cmd.add(targetTriple);
        }

        File sysroot = findSysroot(ndkDir);
        if (sysroot != null && sysroot.isDirectory()) {
            cmd.add("--sysroot=" + sysroot.getAbsolutePath());
        }

        cmd.add("-shared");
        cmd.add("-fPIC");
        boolean customOptimization = false;
        for (String flag : info.cflags) {
            if (flag.startsWith("-O")) {
                customOptimization = true;
                break;
            }
        }
        if (!customOptimization) {
            cmd.add("-O2");
        }
        cmd.add("-DANDROID");
        cmd.add("-D__ANDROID_API__=" + platformLevel);

        cmd.add("-I" + sourceDir.getAbsolutePath());
        addIncludeDirectories(sourceDir, cmd);
        for (String inc : info.includeDirs) {
            cmd.add("-I" + inc);
        }

        cmd.addAll(info.cflags);
        if (hasCpp && !hasC) {
            cmd.addAll(info.cppflags);
        }

        cmd.add("-o");
        cmd.add(tempOutFile.getAbsolutePath());

        for (File src : sourceFiles) {
            cmd.add(src.getAbsolutePath());
        }

        cmd.add("-llog");
        cmd.add("-landroid");
        cmd.add("-lm");

        for (String lib : info.linkedLibraries) {
            if (!lib.equals("log") && !lib.equals("android") && !lib.equals("m")) {
                cmd.add("-l" + lib);
            }
        }
        cmd.addAll(info.ldflags);

        if (hasCpp && !"none".equals(info.stl) && !"system".equals(info.stl)) {
            cmd.add("-lc++_static");
        }

        String output = runCommandWithEnv(cmd, ndkDir, "Clang compilation failed for " + abi);

        if (!tempOutFile.isFile() || tempOutFile.length() == 0L) {
            throw new zy("Clang compilation finished, but output shared library was not created: " + tempOutFile.getAbsolutePath() +
                    "\nCompiler: " + clangBinary.getAbsolutePath() + " (" + clangBinary.length() + " bytes)" +
                    "\nCommand: " + cmd +
                    "\nOutput:\n" + output);
        }

        String nativeLibsDir = builder.fpu.getPathNativelibs(builder.yq.sc_id) + File.separator + abi;
        FileUtil.makeDir(nativeLibsDir);
        File finalOutFile = new File(nativeLibsDir, "lib" + libName + ".so");
        FileUtil.copyFile(tempOutFile.getAbsolutePath(), finalOutFile.getAbsolutePath());

        if (!finalOutFile.isFile() || finalOutFile.length() == 0L) {
            throw new zy("Failed to copy compiled .so library to project directory: " + finalOutFile.getAbsolutePath());
        }

        LogUtil.d(TAG, "Successfully compiled " + finalOutFile.getAbsolutePath() + " (" + finalOutFile.length() + " bytes)");
    }

    public static boolean isRunnable(File binary) {
        if (binary == null || !binary.isFile() || binary.length() == 0) {
            return false;
        }
        makeExecutable(binary);
        try {
            Process process = new ProcessBuilder(binary.getAbsolutePath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }
            int code = process.waitFor();
            return code == 0;
        } catch (Throwable t) {
            LogUtil.w(TAG, "Binary " + binary.getAbsolutePath() + " is not executable on this device: " + t.getMessage());
            return false;
        }
    }

    private static File findNinja(File toolsDir) {
        File binDir = new File(SketchApplication.getContext().getFilesDir(), "bin");
        File[] candidates = {
                new File(binDir, "cmake/bin/ninja"),
                new File(binDir, "ninja"),
                new File(toolsDir, "cmake/bin/ninja"),
                new File(toolsDir, "ninja/ninja"),
                new File(toolsDir, "ninja"),
                new File(toolsDir, "bin/ninja")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                makeExecutable(candidate);
                return candidate;
            }
        }
        return null;
    }

    private static File findLlvmBinDir(File ndkDir) {
        if (ndkDir == null || !ndkDir.isDirectory()) {
            return null;
        }
        File directBin = new File(ndkDir, "bin");
        if (directBin.isDirectory() && containsClang(directBin)) {
            return directBin;
        }
        File llvmPrebuilt = new File(ndkDir, "toolchains/llvm/prebuilt");
        if (llvmPrebuilt.isDirectory()) {
            File[] hosts = llvmPrebuilt.listFiles();
            if (hosts != null) {
                for (File host : hosts) {
                    if (host.isDirectory()) {
                        File bin = new File(host, "bin");
                        if (bin.isDirectory() && containsClang(bin)) {
                            return bin;
                        }
                    }
                }
            }
        }
        File toolchains = new File(ndkDir, "toolchains");
        if (toolchains.isDirectory()) {
            File[] tcList = toolchains.listFiles();
            if (tcList != null) {
                for (File tc : tcList) {
                    File prebuilt = new File(tc, "prebuilt");
                    if (prebuilt.isDirectory()) {
                        File[] hosts = prebuilt.listFiles();
                        if (hosts != null) {
                            for (File h : hosts) {
                                File bin = new File(h, "bin");
                                if (bin.isDirectory() && containsClang(bin)) {
                                    return bin;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (directBin.isDirectory()) {
            return directBin;
        }
        return findBinDirRecursively(ndkDir, 5);
    }

    private static boolean containsClang(File binDir) {
        if (binDir == null || !binDir.isDirectory()) return false;
        if (binDir.getAbsolutePath().contains("simpleperf")) return false;
        File[] files = binDir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (name.startsWith("clang") || name.contains("-clang")) return true;
        }
        return false;
    }

    private static File findBinDirRecursively(File dir, int depth) {
        if (dir == null || !dir.isDirectory() || depth <= 0) return null;
        if (dir.getName().equals("simpleperf") || dir.getName().equals("CMakeFiles")) return null;
        File bin = new File(dir, "bin");
        if (bin.isDirectory() && containsClang(bin)) {
            return bin;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !child.getName().equals("simpleperf") && !child.getName().equals("CMakeFiles") && !child.getName().startsWith(".")) {
                    File found = findBinDirRecursively(child, depth - 1);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static void repairNdkBinaries(File binDir) {
        if (binDir == null || !binDir.isDirectory()) return;

        File[] files = binDir.listFiles();
        if (files == null) return;

        File realClang = null;
        for (File f : files) {
            if (f.isFile() && f.length() > 1024 * 1024L) {
                String name = f.getName();
                if (name.matches("^clang-[0-9]+$") || name.equals("clang-19") || name.equals("clang-20") || name.equals("clang-21")) {
                    realClang = f;
                    break;
                }
            }
        }
        if (realClang == null) {
            for (File f : files) {
                if (f.isFile() && f.length() > 1024 * 1024L && f.getName().startsWith("clang")
                        && !f.getName().contains("format") && !f.getName().contains("tidy")
                        && !f.getName().contains("check") && !f.getName().contains("scan")) {
                    realClang = f;
                    break;
                }
            }
        }

        if (realClang != null) {
            File clang = new File(binDir, "clang");
            if (!clang.exists() || clang.length() < 1024L) {
                try {
                    FileUtil.copyFile(realClang.getAbsolutePath(), clang.getAbsolutePath());
                    makeExecutable(clang);
                    LogUtil.d(TAG, "Auto-repaired 0-byte clang using " + realClang.getName());
                } catch (Exception e) {
                    LogUtil.w(TAG, "Failed to repair clang: " + e.getMessage());
                }
            }

            File clangCpp = new File(binDir, "clang++");
            if (!clangCpp.exists() || clangCpp.length() < 1024L) {
                try {
                    FileUtil.copyFile(realClang.getAbsolutePath(), clangCpp.getAbsolutePath());
                    makeExecutable(clangCpp);
                    LogUtil.d(TAG, "Auto-repaired 0-byte clang++ using " + realClang.getName());
                } catch (Exception e) {
                    LogUtil.w(TAG, "Failed to repair clang++: " + e.getMessage());
                }
            }
        }

        File realLld = null;
        for (File f : files) {
            if (f.isFile() && f.length() > 1024 * 1024L && f.getName().startsWith("lld")) {
                realLld = f;
                break;
            }
        }
        if (realLld != null) {
            File ldLld = new File(binDir, "ld.lld");
            if (!ldLld.exists() || ldLld.length() < 1024L) {
                try {
                    FileUtil.copyFile(realLld.getAbsolutePath(), ldLld.getAbsolutePath());
                    makeExecutable(ldLld);
                } catch (Exception ignored) {}
            }
            File ld = new File(binDir, "ld");
            if (!ld.exists() || ld.length() < 1024L) {
                try {
                    FileUtil.copyFile(realLld.getAbsolutePath(), ld.getAbsolutePath());
                    makeExecutable(ld);
                } catch (Exception ignored) {}
            }
        }

        for (File f : files) {
            if (f.isFile()) {
                makeExecutable(f);
            }
        }
    }

    private static boolean isValidClangExecutable(File f) {
        if (f == null || !f.isFile()) {
            return false;
        }
        if (f.length() < 1024L) {
            return false;
        }
        makeExecutable(f);
        return true;
    }

    public static File findClangBinary(File toolsDir, boolean isCpp, String abi, int platformLevel) {
        File ndkDir = new File(toolsDir, "ndk");
        return findClang(ndkDir, isCpp, abi, platformLevel);
    }

    private static File findClang(File ndkDir, boolean cpp, String abi, int platformLevel) {
        File binDir = findLlvmBinDir(ndkDir);
        if (binDir != null && binDir.isDirectory()) {
            repairNdkBinaries(binDir);

            String wrapperPrefix;
            switch (abi) {
                case "arm64-v8a":
                    wrapperPrefix = "aarch64-linux-android";
                    break;
                case "armeabi-v7a":
                    wrapperPrefix = "armv7a-linux-androideabi";
                    break;
                case "x86_64":
                    wrapperPrefix = "x86_64-linux-android";
                    break;
                case "x86":
                    wrapperPrefix = "i686-linux-android";
                    break;
                default:
                    wrapperPrefix = "aarch64-linux-android";
                    break;
            }

            File targetClang = new File(binDir, wrapperPrefix + platformLevel + (cpp ? "-clang++" : "-clang"));
            if (isValidClangExecutable(targetClang)) return targetClang;

            File genericClang = new File(binDir, wrapperPrefix + (cpp ? "-clang++" : "-clang"));
            if (isValidClangExecutable(genericClang)) return genericClang;

            File baseClang = new File(binDir, cpp ? "clang++" : "clang");
            if (isValidClangExecutable(baseClang)) return baseClang;

            File fallbackClang = new File(binDir, "clang");
            if (isValidClangExecutable(fallbackClang)) return fallbackClang;

            File[] files = binDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    if (cpp && n.matches("^clang\\+\\+-[0-9]+$") && isValidClangExecutable(f)) {
                        return f;
                    } else if (!cpp && n.matches("^clang-[0-9]+$") && isValidClangExecutable(f)) {
                        return f;
                    }
                }
                for (File f : files) {
                    String n = f.getName();
                    if (n.startsWith("clang") && isValidClangExecutable(f)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private static String getTargetTriple(String abi, int api) {
        switch (abi) {
            case "arm64-v8a":
                return "aarch64-linux-android" + api;
            case "armeabi-v7a":
                return "armv7a-linux-androideabi" + api;
            case "x86_64":
                return "x86_64-linux-android" + api;
            case "x86":
                return "i686-linux-android" + api;
            default:
                return "aarch64-linux-android" + api;
        }
    }

    private static File findSysroot(File ndkDir) {
        File llvmBin = findLlvmBinDir(ndkDir);
        if (llvmBin != null) {
            File parent = llvmBin.getParentFile();
            if (parent != null) {
                File sysroot = new File(parent, "sysroot");
                if (sysroot.isDirectory()) {
                    return sysroot;
                }
            }
        }
        File directSysroot = new File(ndkDir, "sysroot");
        if (directSysroot.isDirectory()) {
            return directSysroot;
        }
        return findSysrootRecursively(ndkDir, 5);
    }

    private static File findSysrootRecursively(File dir, int depth) {
        if (dir == null || !dir.isDirectory() || depth <= 0) return null;
        if (dir.getName().equals("simpleperf") || dir.getName().equals("CMakeFiles")) return null;
        if (new File(dir, "usr/include").isDirectory()) return dir;
        if (new File(dir, "sysroot/usr/include").isDirectory()) return new File(dir, "sysroot");
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !child.getName().equals("simpleperf") && !child.getName().equals("CMakeFiles") && !child.getName().startsWith(".")) {
                    File found = findSysrootRecursively(child, depth - 1);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static void makeExecutable(File file) {
        if (file == null || !file.exists()) return;
        file.setExecutable(true, false);
        file.setReadable(true, false);
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "755", file.getAbsolutePath()}).waitFor();
        } catch (Exception ignored) {}
    }

    private static boolean isNativeSource(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return file.isFile() && (name.endsWith(".c") || name.endsWith(".cc")
                || name.endsWith(".cpp") || name.endsWith(".cxx"));
    }

    private static void findSourceFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("build") && !f.getName().equals("CMakeFiles") && !f.getName().equals("simpleperf")) {
                findSourceFiles(f, result);
            } else if (isNativeSource(f)) {
                result.add(f);
            }
        }
    }

    private static void addIncludeDirectories(File dir, List<String> cmd) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory() && !child.getName().startsWith(".") && !child.getName().equals("build") && !child.getName().equals("CMakeFiles") && !child.getName().equals("simpleperf")) {
                cmd.add("-I" + child.getAbsolutePath());
                addIncludeDirectories(child, cmd);
            }
        }
    }

    private List<String> getAbis(File sourceDir) {
        File abisFile = new File(sourceDir, "abis.txt");
        List<String> abis = new ArrayList<>();
        if (abisFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(abisFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        abis.add(line);
                    }
                }
            } catch (IOException e) {
                LogUtil.e(TAG, "Failed to read abis.txt", e);
            }
        }
        if (abis.isEmpty() && builder != null && builder.build_settings != null) {
            String configAbis = builder.build_settings.getValue(BuildSettings.SETTING_CMAKE_ABIS, "");
            if (!configAbis.isEmpty()) {
                for (String abi : configAbis.split(",")) {
                    String trimmed = abi.trim();
                    if (!trimmed.isEmpty() && !abis.contains(trimmed)) {
                        abis.add(trimmed);
                    }
                }
            } else {
                boolean arm64 = "true".equals(builder.build_settings.getValue(BuildSettings.SETTING_CMAKE_ABI_ARM64_V8A, "true"));
                boolean armv7 = "true".equals(builder.build_settings.getValue(BuildSettings.SETTING_CMAKE_ABI_ARMEABI_V7A, "true"));
                boolean x86 = "true".equals(builder.build_settings.getValue(BuildSettings.SETTING_CMAKE_ABI_X86, "false"));
                boolean x86_64 = "true".equals(builder.build_settings.getValue(BuildSettings.SETTING_CMAKE_ABI_X86_64, "false"));

                if (arm64) abis.add(BuildSettings.ABI_ARM64_V8A);
                if (armv7) abis.add(BuildSettings.ABI_ARMEABI_V7A);
                if (x86) abis.add(BuildSettings.ABI_X86);
                if (x86_64) abis.add(BuildSettings.ABI_X86_64);
            }
        }
        if (abis.isEmpty()) {
            abis.addAll(Arrays.asList(AUTO_ABIS));
        }
        return abis;
    }

    private static void runCommand(List<String> command, String failureMessage) throws zy {
        runCommandWithEnv(command, null, failureMessage);
    }

    private static String runCommandWithEnv(List<String> command, File ndkDir, String failureMessage) throws zy {
        LogUtil.d(TAG, "Running " + command);

        StringBuilder output = new StringBuilder();
        int exitCode;
        try {
            ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
            Map<String, String> env = pb.environment();

            if (ndkDir != null) {
                File llvmBin = findLlvmBinDir(ndkDir);
                String currentPath = env.get("PATH");
                String llvmPath = llvmBin != null ? llvmBin.getAbsolutePath() : "";
                env.put("PATH", llvmPath + (currentPath != null ? File.pathSeparator + currentPath : "/system/bin:/system/xbin"));

                if (llvmBin != null) {
                    File libDir = new File(llvmBin.getParentFile(), "lib");
                    File lib64Dir = new File(llvmBin.getParentFile(), "lib64");
                    String currentLd = env.get("LD_LIBRARY_PATH");
                    env.put("LD_LIBRARY_PATH", libDir.getAbsolutePath() + File.pathSeparator + lib64Dir.getAbsolutePath() +
                            (currentLd != null ? File.pathSeparator + currentLd : "/system/lib64:/system/lib"));
                }

                env.put("ANDROID_NDK_HOME", ndkDir.getAbsolutePath());
                env.put("ANDROID_NDK_ROOT", ndkDir.getAbsolutePath());
            }

            env.put("TMPDIR", SketchApplication.getContext().getCacheDir().getAbsolutePath());
            env.put("HOME", SketchApplication.getContext().getFilesDir().getAbsolutePath());

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            exitCode = process.waitFor();
        } catch (IOException e) {
            throw new zy(failureMessage + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new zy(failureMessage + ": interrupted");
        }

        LogUtil.d(TAG, "Exit code " + exitCode + ", output:\n" + output);
        if (exitCode != 0) {
            String log = output.toString();
            if (log.length() > MAX_LOG_CHARS_IN_ERROR) {
                log = "..." + log.substring(log.length() - MAX_LOG_CHARS_IN_ERROR);
            }
            throw new zy(failureMessage + " (exit code " + exitCode + "):\n" + log);
        }
        return output.toString();
    }

    private static int searchAndCopySo(File dir, String targetDir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        int copied = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().equals("CMakeFiles") && !file.getName().equals("simpleperf")) {
                    copied += searchAndCopySo(file, targetDir);
                }
            } else if (file.getName().endsWith(".so")) {
                FileUtil.copyFile(file.getAbsolutePath(), targetDir + File.separator + file.getName());
                copied++;
            }
        }
        return copied;
    }
}