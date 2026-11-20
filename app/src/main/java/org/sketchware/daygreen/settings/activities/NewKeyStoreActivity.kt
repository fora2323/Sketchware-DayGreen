package org.sketchware.daygreen.settings.activities

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.besome.sketch.lib.base.BaseAppCompatActivity
import org.sketchware.daygreen.settings.NewKeyStoreScreen

class NewKeyStoreActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isStoragePermissionGranted) {
            finish()
            return
        }

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
            NewKeyStoreScreen(
                onBack = { finish() },
                onSuccess = { pwd ->
                    val intent = Intent()
                    intent.putExtra("pwd", pwd)
                    setResult(RESULT_OK, intent)
                    finish()
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isStoragePermissionGranted) {
            finish()
        }
    }
}
