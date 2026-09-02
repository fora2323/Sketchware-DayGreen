package extensions.anbui.daydream.mainifest

import extensions.anbui.daydream.project.DRProjectTracker
import extensions.anbui.daydream.project.GetProjectInfo
import pro.sketchware.xml.XmlBuilder

object DRManifestManager {
    @JvmStatic
    fun addApplicationForOneSignal(applicationTag: XmlBuilder) {
        //Gradle in Android Studio will add them automatically.
        if (DRProjectTracker.isExportForAndroidStudio) return

        val receiverTag = XmlBuilder("receiver")

        receiverTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.receivers.FCMBroadcastReceiver"
        )
        receiverTag.addAttribute("android", "exported", "true")
        receiverTag.addAttribute(
            "android",
            "permission",
            "com.google.android.c2dm.permission.SEND"
        )

        val intentFilterTag0 = XmlBuilder("intent-filter")
        intentFilterTag0.addAttribute("android", "priority", "999")
        val actionTag0 = XmlBuilder("action")
        actionTag0.addAttribute("android", "name", "com.google.android.c2dm.intent.RECEIVE")
        val categoryTag = XmlBuilder("category")
        categoryTag.addAttribute("android", "name", GetProjectInfo.getPackageName(DRProjectTracker.currentprojectID))

        intentFilterTag0.addChildNode(categoryTag)
        intentFilterTag0.addChildNode(actionTag0)
        receiverTag.addChildNode(intentFilterTag0)
        applicationTag.addChildNode(receiverTag)

        val hmsMessageServiceOneSignalTag = XmlBuilder("service")

        hmsMessageServiceOneSignalTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.services.HmsMessageServiceOneSignal"
        )
        hmsMessageServiceOneSignalTag.addAttribute("android", "exported", "false")
        hmsMessageServiceOneSignalTag.addAttribute(
            "android",
            "permission",
            "com.google.android.c2dm.permission.SEND"
        )

        val intentFilterTag = XmlBuilder("intent-filter")
        val actionTag = XmlBuilder("action")
        actionTag.addAttribute("android", "name", "com.huawei.push.action.MESSAGING_EVENT")

        intentFilterTag.addChildNode(actionTag)
        hmsMessageServiceOneSignalTag.addChildNode(intentFilterTag)
        applicationTag.addChildNode(hmsMessageServiceOneSignalTag)


        val notificationOpenedActivityHMSTag = XmlBuilder("activity")

        notificationOpenedActivityHMSTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.activities.NotificationOpenedActivityHMS"
        )
        notificationOpenedActivityHMSTag.addAttribute("android", "exported", "true")
        notificationOpenedActivityHMSTag.addAttribute("android", "noHistory", "true")
        notificationOpenedActivityHMSTag.addAttribute(
            "android",
            "theme",
            "@android:style/Theme.Translucent.NoTitleBar"
        )

        val intentFilterTag1 = XmlBuilder("intent-filter")
        val actionTag1 = XmlBuilder("action")
        actionTag1.addAttribute("android", "name", "android.intent.action.VIEW")

        intentFilterTag1.addChildNode(actionTag1)
        notificationOpenedActivityHMSTag.addChildNode(intentFilterTag1)
        applicationTag.addChildNode(notificationOpenedActivityHMSTag)


        val notificationDismissReceiverTag = XmlBuilder("receiver")

        notificationDismissReceiverTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.receivers.NotificationDismissReceiver"
        )
        notificationDismissReceiverTag.addAttribute("android", "exported", "true")

        applicationTag.addChildNode(notificationDismissReceiverTag)


        val bootUpReceiverTag = XmlBuilder("receiver")
        bootUpReceiverTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.receivers.BootUpReceiver"
        )
        bootUpReceiverTag.addAttribute("android", "exported", "false")
        val bootUpReceiverIntentFilterTag = XmlBuilder("intent-filter")
        val bootUpReceiverBootCompleteActionTag = XmlBuilder("action")
        bootUpReceiverBootCompleteActionTag.addAttribute(
            "android",
            "name",
            "android.intent.action.BOOT_COMPLETED"
        )
        val bootUpReceiverQuickBootPowerOnActionTag = XmlBuilder("action")
        bootUpReceiverQuickBootPowerOnActionTag.addAttribute(
            "android",
            "name",
            "android.intent.action.QUICKBOOT_POWERON"
        )
        bootUpReceiverIntentFilterTag.addChildNode(bootUpReceiverBootCompleteActionTag)
        bootUpReceiverIntentFilterTag.addChildNode(bootUpReceiverQuickBootPowerOnActionTag)
        bootUpReceiverTag.addChildNode(bootUpReceiverIntentFilterTag)
        applicationTag.addChildNode(bootUpReceiverTag)


        val upgradeReceiverTag = XmlBuilder("receiver")

        upgradeReceiverTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.receivers.UpgradeReceiver"
        )
        upgradeReceiverTag.addAttribute("android", "exported", "true")

        val intentFilterTag3 = XmlBuilder("intent-filter")
        val actionTag3 = XmlBuilder("action")
        actionTag3.addAttribute("android", "name", "android.intent.action.MY_PACKAGE_REPLACED")

        intentFilterTag3.addChildNode(actionTag3)
        upgradeReceiverTag.addChildNode(intentFilterTag3)
        applicationTag.addChildNode(upgradeReceiverTag)


        val notificationOpenedActivityTag = XmlBuilder("activity")

        notificationOpenedActivityTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.activities.NotificationOpenedActivity"
        )
        notificationOpenedActivityTag.addAttribute("android", "excludeFromRecents", "true")
        notificationOpenedActivityTag.addAttribute("android", "exported", "true")
        notificationOpenedActivityTag.addAttribute("android", "noHistory", "true")
        notificationOpenedActivityTag.addAttribute("android", "taskAffinity", "")
        notificationOpenedActivityTag.addAttribute(
            "android",
            "theme",
            "@android:style/Theme.Translucent.NoTitleBar"
        )

        applicationTag.addChildNode(notificationOpenedActivityTag)


        val notificationOpenedActivityAndroid22AndOlderTag = XmlBuilder("activity")

        notificationOpenedActivityAndroid22AndOlderTag.addAttribute(
            "android",
            "name",
            "com.onesignal.notifications.activities.NotificationOpenedActivityAndroid22AndOlder"
        )
        notificationOpenedActivityAndroid22AndOlderTag.addAttribute(
            "android",
            "excludeFromRecents",
            "true"
        )
        notificationOpenedActivityAndroid22AndOlderTag.addAttribute("android", "exported", "true")
        notificationOpenedActivityAndroid22AndOlderTag.addAttribute("android", "noHistory", "true")
        notificationOpenedActivityAndroid22AndOlderTag.addAttribute(
            "android",
            "theme",
            "@android:style/Theme.Translucent.NoTitleBar"
        )

        applicationTag.addChildNode(notificationOpenedActivityAndroid22AndOlderTag)


        val syncServiceTag = XmlBuilder("service")

        syncServiceTag.addAttribute("android", "name", "com.onesignal.core.services.SyncService")
        syncServiceTag.addAttribute("android", "exported", "false")
        syncServiceTag.addAttribute("android", "stopWithTask", "true")

        applicationTag.addChildNode(syncServiceTag)


        val syncJobServiceTag = XmlBuilder("service")

        syncJobServiceTag.addAttribute(
            "android",
            "name",
            "com.onesignal.core.services.SyncJobService"
        )
        syncJobServiceTag.addAttribute("android", "exported", "false")
        syncJobServiceTag.addAttribute(
            "android",
            "permission",
            "android.permission.BIND_JOB_SERVICE"
        )

        applicationTag.addChildNode(syncJobServiceTag)


        val permissionsActivityTag = XmlBuilder("activity")

        permissionsActivityTag.addAttribute(
            "android",
            "name",
            "com.onesignal.core.activities.PermissionsActivity"
        )
        permissionsActivityTag.addAttribute("android", "exported", "false")
        permissionsActivityTag.addAttribute(
            "android",
            "theme",
            "@android:style/Theme.Translucent.NoTitleBar"
        )

        applicationTag.addChildNode(permissionsActivityTag)
    }

    @JvmStatic
    fun addApplicationForShizuku(applicationTag: XmlBuilder) {
        val providerTag = XmlBuilder("provider")
        providerTag.addAttribute("android", "name", "rikka.shizuku.ShizukuProvider")
        providerTag.addAttribute("android", "authorities", "\${applicationId}.shizuku")
        providerTag.addAttribute("android", "multiprocess", "false")
        providerTag.addAttribute("android", "enabled", "true")
        providerTag.addAttribute("android", "exported", "true")
        providerTag.addAttribute(
            "android",
            "permission",
            "android.permission.INTERACT_ACROSS_USERS_FULL"
        )
        val metadataTag = XmlBuilder("meta-data")
        metadataTag.addAttribute("android", "name", "moe.shizuku.client.V3_SUPPORT")
        metadataTag.addAttribute("android", "value", "true")
        providerTag.addChildNode(metadataTag)
        applicationTag.addChildNode(providerTag)
    }
}