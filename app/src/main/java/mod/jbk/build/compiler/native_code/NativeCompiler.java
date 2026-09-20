package mod.jbk.build.compiler.native_code;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import a.a.a.ProjectBuilder;
import a.a.a.zy;
import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.util.LogUtil;
import pro.sketchware.SketchApplication;
import pro.sketchware.utility.FileUtil;

/**
 * Compiles a project's native (C/C++) code with the Android NDK and CMake (or direct Clang).
 * <p>
 * If Native Tools is disabled in Library Manager, native compilation is completely bypassed.
 * No auto-generated CMakeLists.txt or .txt files are ever created.
 * <p>
 * When a custom CMakeLists.txt is provided and CMake is runnable on Android, CMake is used.
 * Otherwise, the compiler directly compiles the C/C++ sources using the NDK's static Clang.
 */
public class NativeCompiler {
    private static final String TAG = "NativeCompiler";

    private static final String[] AUTO_ABIS = {"arm64-v8a", "armeabi-v7a"};
    private static final String[] CUSTOM_ABIS = {"arm64-v8a"};

    private static final int ANDROID_PLATFORM_LEVEL = 21;
    private static final int MAX_LOG_CHARS_IN_ERROR = 4000;

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
        if (!DayDreamProjectSettings.isEnableDayDream(builder.yq.sc_id)) {
            LogUtil.d(TAG, "Native tools is disabled in Library Manager for project " + builder.yq.sc_id + "; skipping native compilation.");
            return;
        }

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

        File cmakeLists = new File(sourceDir, "CMakeLists.txt");
        if (cmakeLists.isFile()) {
            // Delete legacy auto-generated CMakeLists.txt from previous versions to keep project clean
            try (BufferedReader reader = new BufferedReader(new FileReader(cmakeLists))) {
                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.contains("auto-generated")) {
                    cmakeLists.delete();
                    cmakeLists = null;
                }
            } catch (Exception ignored) {}
        }

        boolean hasCustomCMakeLists = cmakeLists != null && cmakeLists.isFile();

        if (nativeSourceFiles.isEmpty() && !hasCustomCMakeLists) {
            LogUtil.d(TAG, "No native C/C++ source files or custom CMakeLists.txt found in " + nativeSourcePath + "; skipping native compilation.");
            return;
        }

        if (progressReceiver != null) {
            progressReceiver.onProgress("Compiling Native code...", 14);
        }

        File binDir = new File(SketchApplication.getContext().getFilesDir(), "bin");
        File toolsDir = getToolsDirectory();

        File ndkDir = new File(binDir, "android-ndk");
        if (!ndkDir.isDirectory()) {
            ndkDir = new File(toolsDir, "ndk");
        }

        File toolchainFile = new File(ndkDir, "build/cmake/android.toolchain.cmake");
        if (!toolchainFile.isFile()) {
            toolchainFile = new File(toolsDir, "ndk/build/cmake/android.toolchain.cmake");
        }

        File cmakeBinary = new File(binDir, "cmake/bin/cmake");
        if (!cmakeBinary.isFile()) {
            cmakeBinary = new File(binDir, "cmake");
        }
        if (!cmakeBinary.isFile()) {
            cmakeBinary = new File(toolsDir, "cmake/bin/cmake");
        }

        boolean cmakeSucceeded = false;
        // Only run CMake if a user-supplied custom CMakeLists.txt actually exists and CMake is runnable
        if (hasCustomCMakeLists && toolchainFile.isFile() && isRunnable(cmakeBinary)) {
            try {
                compileWithCmake(sourceDir, cmakeBinary, toolchainFile, toolsDir, nativeSourcePath);
                cmakeSucceeded = true;
            } catch (zy e) {
                LogUtil.w(TAG, "CMake compilation failed: " + e.getMessage() + ". Attempting direct Clang compilation fallback...");
            }
        }

        if (!cmakeSucceeded) {
            if (!ndkDir.isDirectory()) {
                throw new zy("Android NDK not found. Please install Android NDK in DayDream Universal Settings.");
            }
            if (nativeSourceFiles.isEmpty()) {
                throw new zy("Native build failed: No C/C++ source files (.c, .cpp, .cc) were found to compile.");
            }
            compileDirectWithClang(ndkDir, sourceDir, nativeSourceFiles, cmakeLists);
        }
    }

    private void compileWithCmake(
            File sourceDir,
            File cmakeBinary,
            File toolchainFile,
            File toolsDir,
            String nativeSourcePath
    ) throws zy {
        File ninjaBinary = findNinja(toolsDir);
        String[] abis = CUSTOM_ABIS;
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
            configure.add("-DANDROID_PLATFORM=android-" + ANDROID_PLATFORM_LEVEL);
            configure.add("-DCMAKE_BUILD_TYPE=Release");
            configure.add("-DCMAKE_TOOLCHAIN_FILE=" + toolchainFile.getAbsolutePath());
            if (ninjaBinary != null) {
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

    private void compileDirectWithClang(File ndkDir, File sourceDir, List<File> sourceFiles, File cmakeLists) throws zy {
        boolean hasCpp = false;
        for (File src : sourceFiles) {
            String name = src.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx")) {
                hasCpp = true;
                break;
            }
        }

        File clangBinary = findClang(ndkDir, hasCpp);
        if (clangBinary == null && hasCpp) {
            clangBinary = findClang(ndkDir, false);
        }
        if (clangBinary == null) {
            throw new zy("Clang compiler not found in NDK directory: " + ndkDir.getAbsolutePath() +
                    ". Please ensure Android NDK is installed correctly.");
        }

        makeExecutable(clangBinary);

        String libName = extractLibraryName(cmakeLists);
        List<String> extraLibs = extractLinkedLibraries(cmakeLists);

        int successCount = 0;
        String lastError = null;

        for (String abi : AUTO_ABIS) {
            if (progressReceiver != null) {
                progressReceiver.onProgress("Compiling Native code with Clang (" + abi + ")...", 14);
            }
            try {
                compileSingleAbiWithClang(clangBinary, ndkDir, sourceDir, sourceFiles, abi, libName, extraLibs, hasCpp);
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
            String libName,
            List<String> extraLibs,
            boolean hasCpp
    ) throws zy {
        File buildDir = new File(SketchApplication.getContext().getCacheDir(), "native_build" + File.separator + abi);
        FileUtil.makeDir(buildDir.getAbsolutePath());
        File tempOutFile = new File(buildDir, "lib" + libName + ".so");
        if (tempOutFile.exists()) {
            tempOutFile.delete();
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(clangBinary.getAbsolutePath());

        cmd.add("-target");
        if ("arm64-v8a".equals(abi)) {
            cmd.add("aarch64-linux-android" + ANDROID_PLATFORM_LEVEL);
        } else if ("armeabi-v7a".equals(abi)) {
            cmd.add("armv7a-linux-androideabi" + ANDROID_PLATFORM_LEVEL);
        } else if ("x86".equals(abi)) {
            cmd.add("i686-linux-android" + ANDROID_PLATFORM_LEVEL);
        } else if ("x86_64".equals(abi)) {
            cmd.add("x86_64-linux-android" + ANDROID_PLATFORM_LEVEL);
        } else {
            cmd.add("aarch64-linux-android" + ANDROID_PLATFORM_LEVEL);
        }

        File sysroot = findSysroot(ndkDir);
        if (sysroot != null && sysroot.isDirectory()) {
            cmd.add("--sysroot=" + sysroot.getAbsolutePath());
        }

        cmd.add("-shared");
        cmd.add("-fPIC");
        cmd.add("-O2");

        cmd.add("-I" + sourceDir.getAbsolutePath());
        addIncludeDirectories(sourceDir, cmd);

        cmd.add("-o");
        cmd.add(tempOutFile.getAbsolutePath());

        for (File src : sourceFiles) {
            cmd.add(src.getAbsolutePath());
        }

        cmd.add("-llog");
        cmd.add("-landroid");
        cmd.add("-lm");

        for (String lib : extraLibs) {
            if (!lib.equals("log") && !lib.equals("android") && !lib.equals("m")) {
                cmd.add("-l" + lib);
            }
        }

        if (hasCpp) {
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
        if (binary == null || !binary.isFile()) {
            return false;
        }
        makeExecutable(binary);
        try {
            Process process = new ProcessBuilder(binary.getAbsolutePath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            return process.exitValue() == 0;
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
                new File(toolsDir, "ninja")
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
        File llvmPrebuilt = new File(ndkDir, "toolchains/llvm/prebuilt");
        if (llvmPrebuilt.isDirectory()) {
            File[] hosts = llvmPrebuilt.listFiles();
            if (hosts != null) {
                for (File host : hosts) {
                    if (host.isDirectory()) {
                        File bin = new File(host, "bin");
                        if (bin.isDirectory() && new File(bin, "clang").isFile()) {
                            return bin;
                        }
                    }
                }
                for (File host : hosts) {
                    if (host.isDirectory()) {
                        File bin = new File(host, "bin");
                        if (bin.isDirectory()) {
                            return bin;
                        }
                    }
                }
            }
        }
        File directBin = new File(ndkDir, "bin");
        if (directBin.isDirectory()) {
            return directBin;
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
                                if (bin.isDirectory()) {
                                    return bin;
                                }
                            }
                        }
                    }
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
                if (name.matches("^clang-[0-9]+$") || name.equals("clang-21")) {
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

    private static File findClang(File ndkDir, boolean cpp) {
        File binDir = findLlvmBinDir(ndkDir);
        if (binDir != null && binDir.isDirectory()) {
            repairNdkBinaries(binDir);

            String exactName = cpp ? "clang++" : "clang";
            File exactBinary = new File(binDir, exactName);
            if (isValidClangExecutable(exactBinary)) {
                return exactBinary;
            }

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
                    if (n.matches("^clang-[0-9]+$") && isValidClangExecutable(f)) {
                        return f;
                    }
                }
            }
        }
        return null;
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
        return null;
    }

    private static void makeExecutable(File file) {
        if (!file.canExecute() && !file.setExecutable(true)) {
            LogUtil.w(TAG, "Couldn't mark " + file.getAbsolutePath() + " as executable");
        }
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
            if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("build") && !f.getName().equals("CMakeFiles")) {
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
            if (child.isDirectory() && !child.getName().startsWith(".") && !child.getName().equals("build") && !child.getName().equals("CMakeFiles")) {
                cmd.add("-I" + child.getAbsolutePath());
                addIncludeDirectories(child, cmd);
            }
        }
    }

    private static String extractLibraryName(File cmakeLists) {
        if (cmakeLists != null && cmakeLists.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cmakeLists))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase(Locale.ROOT).startsWith("add_library")) {
                        int start = line.indexOf('(');
                        if (start != -1) {
                            String rest = line.substring(start + 1).trim();
                            String[] parts = rest.split("[\\s,)]+");
                            if (parts.length > 0 && !parts[0].isEmpty()) {
                                return parts[0].replaceAll("[\"']", "");
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return "native-lib";
    }

    private static List<String> extractLinkedLibraries(File cmakeLists) {
        List<String> libs = new ArrayList<>();
        if (cmakeLists != null && cmakeLists.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cmakeLists))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase(Locale.ROOT).startsWith("target_link_libraries")) {
                        int start = line.indexOf('(');
                        int end = line.indexOf(')');
                        if (start != -1) {
                            String content = end != -1 ? line.substring(start + 1, end) : line.substring(start + 1);
                            String[] tokens = content.split("\\s+");
                            for (int i = 1; i < tokens.length; i++) {
                                String token = tokens[i].replaceAll("[\"']", "").trim();
                                if (token.isEmpty() || token.startsWith("${") || token.equals("PUBLIC") || token.equals("PRIVATE") || token.equals("INTERFACE")) {
                                    continue;
                                }
                                if (!libs.contains(token)) {
                                    libs.add(token);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return libs;
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
            if (ndkDir != null) {
                File llvmBin = findLlvmBinDir(ndkDir);
                if (llvmBin != null && llvmBin.isDirectory()) {
                    Map<String, String> env = pb.environment();
                    String path = env.get("PATH");
                    env.put("PATH", llvmBin.getAbsolutePath() + (path != null ? File.pathSeparator + path : ""));
                }
            }
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
                if (!file.getName().equals("CMakeFiles")) {
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