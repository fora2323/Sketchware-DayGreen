package org.sketchware.daygreen.settings.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import org.sketchware.daygreen.settings.SettingSection
import pro.sketchware.R
import pro.sketchware.activities.editor.component.ManageCustomComponentActivity
import pro.sketchware.activities.settings.SettingsActivity

class AppSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSettingsScreen(
                sections = buildSections(),
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

    private fun buildSections(): List<SettingSection> = listOf(
        SettingSection(
            header = "Managers",
            items = listOf(
                SettingItem(R.drawable.ic_mtrl_block, "Block manager", "Manage your own blocks to use in Logic Editor") {
                    startActivity(Intent(applicationContext, BlocksManager::class.java))
                },
                SettingItem(R.drawable.ic_mtrl_pull_down, "Block selector menu manager", "Manage your own block selector menus") {
                    openSettingsFragment(SettingsActivity.BLOCK_SELECTOR_MANAGER_FRAGMENT)
                },
                SettingItem(R.drawable.ic_mtrl_component, "Component manager", "Manage your own components") {
                    startActivity(Intent(applicationContext, ManageCustomComponentActivity::class.java))
                },
                SettingItem(R.drawable.ic_mtrl_list, "Event manager", "Manage your own events") {
                    openSettingsFragment(SettingsActivity.EVENTS_MANAGER_FRAGMENT)
                },
                SettingItem(R.drawable.ic_mtrl_box, "Local library manager", "Manage and download local libraries") {
                    startActivity(
                        Intent(applicationContext, ManageLocalLibraryActivity::class.java)
                            .putExtra("sc_id", "system")
                    )
                },
                SettingItem(
                    R.drawable.ic_mtrl_article,
                    Helper.getResString(R.string.design_drawer_menu_title_logcat_reader),
                    Helper.getResString(R.string.design_drawer_menu_subtitle_logcat_reader),
                ) {
                    startActivity(Intent(applicationContext, LogReaderActivity::class.java))
                },
                SettingItem(R.drawable.ic_mtrl_settings, "AI", "Configure AI assistant") {
                    startActivity(Intent(applicationContext, AiSettingsActivity::class.java))
                },
                SettingItem(R.drawable.ic_mtrl_version_control, "Git", "Configuration system control version") {
                    startActivity(Intent(applicationContext, GitSettingsActivity::class.java))
                },
                SettingItem(R.drawable.ic_mtrl_settings, "Build Tools", "Manage and download build tools") {
                    startActivity(Intent(applicationContext, BuildToolsActivity::class.java))
                },
                SettingItem(R.drawable.ic_mtrl_folder, "SDK Manager", "Manage and download SDK platforms") {
                    startActivity(Intent(applicationContext, SdkManagerActivity::class.java))
                },
            ),
        ),
        SettingSection(
            header = "General",
            items = listOf(
                SettingItem(R.drawable.ic_mtrl_settings_applications, "App settings", "Change general app settings") {
                    startActivity(Intent(applicationContext, ConfigActivity::class.java))
                },
                SettingItem(
                    R.drawable.ic_mtrl_folder,
                    "Open working directory",
                    "Open Sketchware Pro's directory and edit files in it",
                ) {
                    AppSettingsActions.openWorkingDirectory(this@AppSettingsActivity)
                },
                SettingItem(
                    R.drawable.ic_mtrl_apk_document,
                    "Sign an APK file with testkey",
                    "Sign an already existing APK file with testkey and signature schemes up to V4",
                ) {
                    AppSettingsActions.signApkFileDialog(this@AppSettingsActivity)
                },
            ),
        ),
    )
}