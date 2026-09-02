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
}
