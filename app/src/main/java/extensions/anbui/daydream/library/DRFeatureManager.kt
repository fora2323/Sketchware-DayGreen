package extensions.anbui.daydream.library

import a.a.a.jC
import com.besome.sketch.beans.ComponentBean
import extensions.anbui.daydream.project.ProjectLogic
import extensions.anbui.daydream.settings.DayDreamProjectSettings

object DRFeatureManager {
    @JvmStatic
    var forMinSDK: Int = 33
    @JvmStatic
    var isAllowRelease: Boolean = false
    @JvmStatic
    var isAllowR8: Boolean = false

    @JvmStatic
    fun isThemeEnabled(projectID: String): Boolean {
        return DayDreamProjectSettings.isEnableDayDream(projectID) &&
                DayDreamProjectSettings.isUseTheme(projectID) &&
                LibraryUtils.isAllowUseTheme(projectID)
    }

    @JvmStatic
    fun isDynamicColorEnabled(projectID: String): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID) &&
            DayDreamProjectSettings.isUseTheme(projectID) &&
            LibraryUtils.isAllowUseTheme(projectID) &&
            LibraryUtils.isAllowUseDynamicColor(projectID)
        ) {

            return DayDreamProjectSettings.isUseDynamicColor(projectID)
        }

        return false
    }

    @JvmStatic
    fun isDisableAutomaticPermissionRequestsEnabled(
        projectID: String,
        activityName: String?
    ): Boolean {
        if (LibraryUtils.isAllowUseWindowInsetsHandling(projectID)) {
            return if (DayDreamProjectSettings.isEnableDayDream(projectID)
                && DayDreamProjectSettings.isUniversalDisableAutomaticPermissionRequests(projectID)
            ) {
                true
            } else if (!activityName.isNullOrEmpty()) {
                DayDreamProjectSettings.isDisableAutomaticPermissionRequests(
                    projectID,
                    activityName
                )
            } else {
                false
            }
        }

        return false
    }

    @JvmStatic
    fun isEdgeToEdgeEnabled(projectID: String, xmlName: String?): Boolean {
        if (LibraryUtils.isAllowUseEdgeToEdge(projectID)) {
            return if (DayDreamProjectSettings.isEnableDayDream(projectID) &&
                DayDreamProjectSettings.isUniversalEdgeToEdge(projectID)
            ) {
                true
            } else if (!xmlName.isNullOrEmpty()) {
                DayDreamProjectSettings.isEnableEdgeToEdge(projectID, xmlName)
            } else {
                false
            }
        }

        return false
    }

    @JvmStatic
    fun isWindowInsetsHandlingEnabled(projectID: String, xmlName: String?): Boolean {
        if (LibraryUtils.isAllowUseWindowInsetsHandling(projectID)) {
            return if (DayDreamProjectSettings.isEnableDayDream(projectID)
                && DayDreamProjectSettings.isUniversalWindowInsetsHandling(projectID)
            ) {
                true
            } else if (!xmlName.isNullOrEmpty()) {
                DayDreamProjectSettings.isEnableWindowInsetsHandling(projectID, xmlName)
            } else {
                false
            }
        }

        return false
    }

    @JvmStatic
    fun isContentProtectionEnabled(projectID: String, xmlName: String?): Boolean {
        if (LibraryUtils.isAllowUseWindowInsetsHandling(projectID)) {
            return if (DayDreamProjectSettings.isEnableDayDream(projectID)
                && DayDreamProjectSettings.isUniversalContentProtection(projectID)
            ) {
                true
            } else if (!xmlName.isNullOrEmpty()) {
                DayDreamProjectSettings.isContentProtection(projectID, xmlName)
            } else {
                false
            }
        }

        return false
    }

    @JvmStatic
    fun isOnBackInvokedCallbackEnabled(projectID: String, activityName: String?): Boolean {
        if (LibraryUtils.isAllowUseWindowInsetsHandling(projectID)) {
            return DayDreamProjectSettings.isEnableDayDream(projectID)
                    && DayDreamProjectSettings.isUninversalEnableOnBackInvokedCallback(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                ProjectLogic.isThisActivityHaveOnBackPressed(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isAndroidXWorkManagerEnabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isForceAddWorkManager(projectID)
                    && LibraryUtils.isAllowUseAndroidXWorkManager(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportWorkManager(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isAndroidXMedia3Enabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUniversalUseMedia3(projectID)
                    && LibraryUtils.isAllowUseAndroidXMedia3(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportAndroidXMedia3(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isAndroidXBrowserEnabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUniversalUseAndroidXBrowser(projectID)
                    && LibraryUtils.isAllowUseAndroidXBrowser(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportAndroidXBrowser(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isAndroidXCredentialManagerEnabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUniversalUseAndroidXCredentialManager(projectID)
                    && LibraryUtils.isAllowUseAndroidXCredentialManager(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportAndroidXCredentialManager(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isGoogleAnalyticsEnabled(projectID: String): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUseGoogleAnalytics(projectID)
                    && LibraryUtils.isAllowUseGoogleAnalytics(projectID)
        }

        return false
    }

    @JvmStatic
    fun isGlidetransformationsEnabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isGlideTransformations(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportGlideTransformations(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isOneSignalEnabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUseOneSignal(projectID)
                    && DayDreamProjectSettings.getOneSignalAppId(projectID).isNotEmpty()
                    && LibraryUtils.isAllowUseOneSignal(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isAutoInitializeOneSignal(projectID)
        }

        return false
    }

    @JvmStatic
    fun isShizukuEnabled(projectID: String, activityName: String?): Boolean {
        if (!LibraryUtils.isAllowUseShizuku(projectID)) return false
        return hasShizukuComponent(projectID, activityName)
    }

    private fun hasShizukuComponent(projectID: String, activityName: String?): Boolean {
        val files = jC.b(projectID).b()
        val targets = if (activityName.isNullOrEmpty()) files else files.filter { 
            it.fileName == activityName 
        }
        return targets.any { file ->
            jC.a(projectID).e(file.javaName).any { it.type == ComponentBean.COMPONENT_TYPE_SHIZUKU }
        }
    }

    @JvmStatic
    fun isAndroidBillingEnabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUseAndroidBilling(projectID)
                    && LibraryUtils.isAllowUseAndroidBilling(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportAndroidBilling(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isRetrofit2Enabled(projectID: String, activityName: String?): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return DayDreamProjectSettings.isUseRetrofit2(projectID)
                    && if (activityName.isNullOrEmpty())
                true
            else
                DayDreamProjectSettings.isImportRetrofit2(projectID, activityName)
        }

        return false
    }

    @JvmStatic
    fun isNeedAddNetworkPermision(projectID: String): Boolean {
        if (DayDreamProjectSettings.isEnableDayDream(projectID)) {
            return (DayDreamProjectSettings.isUniversalUseMedia3(projectID) && LibraryUtils.isAllowUseAndroidXMedia3(
                projectID
            ))
                    || (DayDreamProjectSettings.isUniversalUseAndroidXBrowser(projectID) && LibraryUtils.isAllowUseAndroidXBrowser(
                projectID
            ))
                    || (DayDreamProjectSettings.isUniversalUseAndroidXCredentialManager(projectID)) && LibraryUtils.isAllowUseAndroidXCredentialManager(
                projectID
            )
                    || DayDreamProjectSettings.isUseRetrofit2(projectID)
        }

        return false
    }
}