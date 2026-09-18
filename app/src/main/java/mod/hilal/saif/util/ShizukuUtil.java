package mod.hilal.saif.util;

import android.content.Context;
import android.content.pm.PackageManager;
import rikka.shizuku.Shizuku;

public class ShizukuUtil {
    public static boolean isShizukuRunning() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable e) {
            return false;
        }
    }
}
