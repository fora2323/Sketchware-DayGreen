package org.sketchware.daygreen.settings.activities

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.besome.sketch.lib.base.BaseAppCompatActivity
import org.sketchware.daygreen.settings.SdkManagerScreen

class SdkManagerActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val statusBarStyle = if (isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }

        val navigationBarStyle = if (isDark) {
            SystemBarStyle.dark(scrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b))
        } else {
            SystemBarStyle.light(
                scrim = Color.argb(0xe6, 0xff, 0xff, 0xff),
                darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b),
            )
        }

        enableEdgeToEdge(
            statusBarStyle = statusBarStyle,
            navigationBarStyle = navigationBarStyle,
        )

        setContent {
            SdkManagerScreen(
                onBack = { finish() },
            )
        }
    }
}
