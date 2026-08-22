package extensions.anbui.daydream.configs;

import android.app.Activity;

import pro.sketchware.SketchApplication;

public class Configs {
    public static boolean isBuilding = false;
    public static final String universalTAG = "DayGreen";
    public static final String mainDataDir = "/.sketchware/";
    public static final String gitFolderDir = mainDataDir + "git/";
    public static final String gitProjectFolderName = "/project/";
    public static final String gitSourceFolderName = "/source/";
    public static final String gitLocalLibraryFolderName = "/local_library/";
    public static final String projectUnsavedDataFolderDir = mainDataDir + "/bak/";
    public static final String projectMySourceFolderDir = mainDataDir + "mysc/";
    public static final String projectInfoFolderDir = mainDataDir + "mysc/list/";
    public static final String projectDataFolderDir = mainDataDir + "data/";
    public static final String customActivityLauncherConfigDataPath = "/Injection/androidmanifest/activity_launcher.txt";
    public static final String projectLocalLibFolderDir = mainDataDir + "libs/local_libs/";
    public static final String resFolderDir = mainDataDir + "resources/";
    public static final String resBlocksFolderDir = resFolderDir + "block/";
    public static final String resFontsFolderDir = resFolderDir + "fonts/";
    public static final String resIconsFolderDir = resFolderDir + "icons/";
    public static final String resImagesFolderDir = resFolderDir + "images/";
    public static final String resSoundsFolderDir = resFolderDir + "sounds/";
    public static final String resWidgetsFolderDir = resFolderDir + "widgets/";
    public static final String appDataFolderDir = SketchApplication.getContext().getFilesDir().toString();
    public static final String jarBuiltInLibFolderDir = appDataFolderDir + "/libs/libs/";
    public static final String dexBuiltInLibFolderDir = appDataFolderDir + "/libs/dexs/";
    public static final String tempDayDreamFolderDir = mainDataDir + "daydreamtemp/";
    public static final String tempUnsavedDataDayDreamFolderDir = mainDataDir + "daydreamtemp/bak/";
    public static final String tempBackupDayDreamFolderDir = mainDataDir + "daydreamtemp/backup/";
    public static final String tempImageDayDreamFolderDir = mainDataDir + "daydreamtemp/image/";
    public static final String recycleBinDayDreamFolderDir = mainDataDir + "recyclebin/";
    public static final String encryptionKey = "sketchwaresecure";
    public static final String defaultQuickLookProjectID = "000";
    public static String currentProjectID = "";
    public static Activity mainActivity;

    public static final int material2Theme = 0;
    public static final int material3Theme = 1;
    public static final int material3ExpressiveTheme = 2;
    public static final int DayNightTheme = 0;
    public static final int DayTheme = 1;
    public static final int NightTheme = 2;
}
