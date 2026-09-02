package extensions.anbui.daydream.gradle

import extensions.anbui.daydream.library.DRFeatureManager
import extensions.anbui.daydream.project.DRProjectTracker

object DRGradleManager {
    @JvmStatic
    fun addDependencies(content: StringBuilder) {
        if (DRFeatureManager.isAndroidXWorkManagerEnabled(DRProjectTracker.currentprojectID, null))
            content.append("implementation 'androidx.work:work-runtime:2.11.0'\r\n")

        if (DRFeatureManager.isAndroidXMedia3Enabled(DRProjectTracker.currentprojectID, null)) {
            content.append("implementation 'androidx.media3:media3-exoplayer:1.8.0'\r\n")
            content.append("implementation 'androidx.media3:media3-ui:1.8.0'\r\n")
            content.append("implementation 'androidx.media3:media3-exoplayer-hls:1.8.0'\r\n")
        }

        if (DRFeatureManager.isAndroidXBrowserEnabled(DRProjectTracker.currentprojectID, null))
            content.append("implementation 'androidx.browser:browser:1.9.0'\r\n")

        if (DRFeatureManager.isAndroidXCredentialManagerEnabled(DRProjectTracker.currentprojectID, null)) {
            content.append("implementation 'androidx.credentials:credentials:1.5.0'\r\n")
            content.append("implementation 'androidx.credentials:credentials-play-services-auth:1.5.0'\r\n")
        }

        if (DRFeatureManager.isGlidetransformationsEnabled(DRProjectTracker.currentprojectID, null))
            content.append("implementation 'jp.wasabeef:glide-transformations:4.3.0'\r\n")

        if (DRFeatureManager.isShizukuEnabled(DRProjectTracker.currentprojectID, null)) {
            content.append("implementation 'dev.rikka.shizuku:api:13.0.0\r\n")
            content.append("implementation 'dev.rikka.shizuku:provider:13.0.0\r\n")
        }

        if (DRFeatureManager.isAndroidBillingEnabled(DRProjectTracker.currentprojectID, null))
            content.append("implementation 'com.android.billingclient:billing:8.2.0'\r\n")

        if (DRFeatureManager.isRetrofit2Enabled(DRProjectTracker.currentprojectID, null))
            content.append("implementation 'com.squareup.retrofit2:retrofit:2.12.0'\r\n")

        if (DRFeatureManager.isOneSignalEnabled(DRProjectTracker.currentprojectID, null))
            content.append("implementation 'com.onesignal:OneSignal:5.4.1'\r\n")

        if (DRFeatureManager.isGoogleAnalyticsEnabled(DRProjectTracker.currentprojectID))
            content.append("implementation 'com.google.firebase:firebase-analytics'\r\n")
    }
}