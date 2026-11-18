package org.sketchware.daygreen.settings.activities

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Widgets
import dev.aldi.sayuti.editor.manage.ManageLocalLibraryActivity
import mod.hey.studios.util.Helper
import mod.hilal.saif.activities.tools.BlocksManager
import mod.hilal.saif.activities.tools.BuildToolsActivity
import mod.hilal.saif.activities.tools.ConfigActivity
import mod.hilal.saif.activities.tools.SdkManagerActivity
import mod.khaled.logcat.LogReaderActivity
import org.sketchware.daygreen.ai.activities.AiSettingsActivity
import org.sketchware.daygreen.git.activities.GitSettingsActivity
import org.sketchware.daygreen.settings.AppSettingsActions
import org.sketchware.daygreen.settings.AppSettingsScreen
import org.sketchware.daygreen.settings.SettingItem
import pro.sketchware.R
import pro.sketchware.activities.editor.component.ManageCustomComponentActivity
import pro.sketchware.activities.settings.SettingsActivity

class AppSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge. Ikon status bar dan nav bar mengikuti tema di dalam aplikasi.
        // Nav bar memakai scrim default Android (lapisan semi-transparan untuk kontras),
        // status bar transparan tanpa scrim.
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
            AppSettingsScreen(
                settings = buildSettings(),
                onBack = { finish() },
            )
        }
    }

    private fun openSettingsFragment(fragmentTag: String) {
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .putExtra(SettingsActivity.FRAGMENT_TAG_EXTRA, fragmentTag)
        )
    }

    private fun buildSettings(): List<SettingItem> = listOf(
        SettingItem(Icons.Filled.Extension, "Block manager", "Manage your own blocks to use in Logic Editor") {
            startActivity(Intent(applicationContext, BlocksManager::class.java))
        },
        SettingItem(Icons.Filled.Apps, "Application", "General applications settings") {
            startActivity(Intent(applicationContext, ConfigActivity::class.java))
        },
        SettingItem(Icons.Filled.Menu, "Block selector menu manager", "Manage your own block selector menus") {
            openSettingsFragment(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT)
        },
        SettingItem(Icons.Filled.Widgets, "Component manager", "Manage your own components") {
            startActivity(Intent(applicationContext, ManageCustomComponentActivity::class.java))
        },
        SettingItem(Icons.Filled.Bolt, "Event manager", "Manage your own events") {
            openSettingsFragment(SettingsActivity.EVENTS_MANAGER_FRAGMENT)
        },
        SettingItem(Icons.Filled.LocalLibrary, "Local library manager", "Manage and download local libraries") {
            startActivity(
                Intent(applicationContext, ManageLocalLibraryActivity::class.java)
                    .putExtra("sc_id", "system")
            )
        },
        SettingItem(
            Icons.Filled.Article,
            Helper.getResString(R.string.design_drawer_menu_title_logcat_reader),
            Helper.getResString(R.string.design_drawer_menu_subtitle_logcat_reader),
        ) {
            startActivity(Intent(applicationContext, LogReaderActivity::class.java))
        },
        SettingItem(Icons.Filled.AutoAwesome, "AI", "Configure AI assistant") {
            startActivity(Intent(applicationContext, AiSettingsActivity::class.java))
        },
        SettingItem(Icons.Filled.Commit, "Git", "Configuration system control version") {
            startActivity(Intent(applicationContext, GitSettingsActivity::class.java))
        },
        SettingItem(Icons.Filled.Construction, "Build Tools", "Manage and download build tools") {
            startActivity(Intent(applicationContext, BuildToolsActivity::class.java))
        },
        SettingItem(Icons.Filled.Android, "SDK Manager", "Manage and download SDK platforms") {
            startActivity(Intent(applicationContext, SdkManagerActivity::class.java))
        },
        SettingItem(Icons.Filled.FolderOpen, "Open working directory", "Open Sketchware Pro's directory and edit files in it") {
            AppSettingsActions.openWorkingDirectory(this@AppSettingsActivity)
        },
        SettingItem(Icons.Filled.Info, "About", "About Sketchware DayGreen") {
            // TODO: isi target About
        },
        SettingItem(Icons.Filled.Favorite, "Support", "Support developer") {
            // TODO: isi target Support
        },
    )
}