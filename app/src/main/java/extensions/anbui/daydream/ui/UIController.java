package extensions.anbui.daydream.ui;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

public class UIController {
    public static void setLightStatusBar(boolean _on, Activity _activity) {
        Window window = _activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(_on);
    }

    public static boolean isUsingThemeNightMode() {
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES;
    }
}
