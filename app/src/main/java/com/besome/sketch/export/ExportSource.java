package com.besome.sketch.export;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;

import a.a.a.ProjectBuilder;
import a.a.a.eC;
import a.a.a.hC;
import a.a.a.iC;
import a.a.a.kC;
import a.a.a.lC;
import a.a.a.wq;
import a.a.a.xq;
import a.a.a.yB;
import a.a.a.yq;
import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.settings.DRSettings;
import extensions.anbui.daydream.tools.project.CleanUpCore;
import pro.sketchware.utility.FileUtil;

public class ExportSource {
    private static final String TAG = "ExportSource";
    private static HashMap<String, Object> sc_metadata = null;
    private static yq project_metadata = null;

    public static boolean startExport(Activity activity, String sc_id, TextView statusTextView) {
        DRProjectTracker.startNowForAndroidStudio(sc_id);

        try {

            sc_metadata = lC.b(sc_id);
            project_metadata = new yq(activity, wq.d(sc_id), sc_metadata);
            FileUtil.deleteFile(project_metadata.projectMyscPath);

            hC hCVar = new hC(sc_id);
            kC kCVar = new kC(sc_id);
            eC eCVar = new eC(sc_id);
            iC iCVar = new iC(sc_id);
            hCVar.i();
            kCVar.s();
            eCVar.g();
            eCVar.e();
            iCVar.i();

            /* Extract project type template */
            project_metadata.a(activity, wq.e(xq.a(sc_id) ? "600" : sc_id));

            /* Start generating project files */

            updateStatus(statusTextView, "Generating project files...");

            ProjectBuilder builder = new ProjectBuilder(activity, project_metadata);
            project_metadata.a(iCVar, hCVar, eCVar, yq.ExportType.ANDROID_STUDIO);
            builder.buildBuiltInLibraryInformation();
            project_metadata.b(hCVar, eCVar, iCVar, builder.getBuiltInLibraryManager());
            if (yB.a(lC.b(sc_id), "custom_icon")) {
                project_metadata.aa(wq.e() + File.separator + sc_id + File.separator + "mipmaps");
                if (yB.a(lC.b(sc_id), "isIconAdaptive", false)) {
                    project_metadata.createLauncherIconXml("""
                            <?xml version="1.0" encoding="utf-8"?>
                            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                            <background android:drawable="@mipmap/ic_launcher_background"/>
                            <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                            <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                            </adaptive-icon>""");
                }
            }
            project_metadata.a();
            kCVar.b(project_metadata.resDirectoryPath + File.separator + "drawable-xhdpi");
            kCVar.c(project_metadata.resDirectoryPath + File.separator + "raw");
            kCVar.a(project_metadata.assetsPath + File.separator + "fonts");

            pro.sketchware.utility.FilePathUtil util = new pro.sketchware.utility.FilePathUtil();
            File pathJava = new File(util.getPathJava(sc_id));
            File pathResources = new File(util.getPathResource(sc_id));
            File pathAssets = new File(util.getPathAssets(sc_id));
            File pathNativeLibraries = new File(util.getPathNativelibs(sc_id));
            File pathNativeSources = new File(util.getPathNative(sc_id));

            if (pathJava.exists()) {
                FileUtil.copyDirectory(pathJava, new File(project_metadata.javaFilesPath + File.separator + project_metadata.packageNameAsFolders));
            }
            if (pathResources.exists()) {
                FileUtil.copyDirectory(pathResources, new File(project_metadata.resDirectoryPath));
            }
            String pathProguard = util.getPathProguard(sc_id);
            if (FileUtil.isExistFile(pathProguard)) {
                FileUtil.copyFile(pathProguard, project_metadata.proguardFilePath);
            }
            if (pathAssets.exists()) {
                FileUtil.copyDirectory(pathAssets, new File(project_metadata.assetsPath));
            }
            if (pathNativeLibraries.exists()) {
                FileUtil.copyDirectory(pathNativeLibraries, new File(project_metadata.generatedFilesPath, "jniLibs"));
            }
            if (pathNativeSources.exists()) {
                File cppDest = new File(project_metadata.generatedFilesPath, "cpp");
                FileUtil.copyDirectory(pathNativeSources, cppDest);
                ensureCMakeLists(cppDest);
            }

            /* Salin gradle wrapper dari assets/gradle_file ke project export */
            copyAssetFolder(activity, "gradle_file", new File(project_metadata.projectMyscPath));

            project_metadata.f();

            /* It makes no sense that those methods aren't static */

            updateStatus(statusTextView, "Exported source code for Android Studio.");

            DRSettings.getAutoCleanUpAfterBuild(activity, isClean -> {
                if (isClean) {
                    new Thread(() -> CleanUpCore.cleanUpAfterBuildInDesign(sc_id)).start();
                }
            });

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
            return false;
        }
    }

    private static void updateStatus(TextView statusTextView, String msg) {
        if (statusTextView == null || statusTextView.getContext() == null) return;
        ((Activity) statusTextView.getContext()).runOnUiThread(() -> statusTextView.setText(msg));
    }
        
    private static void copyAssetFolder(Activity activity, String assetPath, File destDir) throws java.io.IOException {
        String[] children = activity.getAssets().list(assetPath);
        if (children == null || children.length == 0) {
            if (destDir.getParentFile() != null) destDir.getParentFile().mkdirs();
            try (java.io.InputStream in = activity.getAssets().open(assetPath);
                 java.io.OutputStream out = new java.io.FileOutputStream(destDir)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            destDir.setExecutable(true);
            return;
        }
        destDir.mkdirs();
        for (String child : children) {
            copyAssetFolder(activity, assetPath + "/" + child, new File(destDir, child));
        }
    }

    public static void ensureCMakeLists(File cppDir) {
        if (!cppDir.exists()) return;
        File cmakeFile = new File(cppDir, "CMakeLists.txt");
        if (!cmakeFile.exists()) {
            StringBuilder sb = new StringBuilder();
            sb.append("cmake_minimum_required(VERSION 3.22.1)\r\n\r\n");
            sb.append("project(\"native-lib\")\r\n\r\n");
            sb.append("add_library(\r\n");
            sb.append("    native-lib\r\n");
            sb.append("    SHARED\r\n");
            File[] files = cppDir.listFiles();
            boolean hasSources = false;
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".cc")) {
                        sb.append("    ").append(name).append("\r\n");
                        hasSources = true;
                    }
                }
            }
            if (!hasSources) {
                sb.append("    native-lib.cpp\r\n");
            }
            sb.append(")\r\n\r\n");
            sb.append("find_library(\r\n");
            sb.append("    log-lib\r\n");
            sb.append("    log\r\n");
            sb.append(")\r\n\r\n");
            sb.append("target_link_libraries(\r\n");
            sb.append("    native-lib\r\n");
            sb.append("    ${log-lib}\r\n");
            sb.append(")\r\n");
            FileUtil.writeFile(cmakeFile.getAbsolutePath(), sb.toString());
        }
    }
}