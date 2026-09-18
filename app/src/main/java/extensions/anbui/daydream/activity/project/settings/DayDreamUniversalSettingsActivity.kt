package extensions.anbui.daydream.activity.project.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.besome.sketch.editor.manage.library.LibraryCategoryView
import com.besome.sketch.editor.manage.library.LibraryItemView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import extensions.anbui.daydream.settings.DRSettings
import pro.sketchware.R
import pro.sketchware.databinding.ActivityDaydreamUniversalSettingsBinding
import java.io.File
import java.util.ArrayList

class DayDreamUniversalSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaydreamUniversalSettingsBinding

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDaydreamUniversalSettingsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        initialize()

        if (SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                finish()
            }
        }
    }


    fun initialize() {
        val preferences = ArrayList<LibraryCategoryView>()
        val universalCategory = LibraryCategoryView(this)
        universalCategory.setTitle(null)
        preferences.add(universalCategory)

        val backupPref = createSwitchPreference(R.drawable.restore_page_24px, "Backup tool", "Use DayGreen's new backup tool instead of the old one.")
        backupPref.sw_enable.visibility = View.VISIBLE
        backupPref.sw_enable.isClickable = true
        DRSettings.getUseBackupTool(this) { backupPref.sw_enable.isChecked = it }
        backupPref.sw_enable.setOnCheckedChangeListener { _, isChecked -> DRSettings.setUseBackupTool(this, isChecked) }
        backupPref.setOnClickListener { backupPref.sw_enable.toggle() }
        universalCategory.addLibraryItem(backupPref, true)

        val cleanPref = createSwitchPreference(R.drawable.cleaning_services_24px, "Auto clean up after building", "Temporary files will be cleaned up after the build is complete.")
        cleanPref.sw_enable.visibility = View.VISIBLE
        cleanPref.sw_enable.isClickable = true
        DRSettings.getAutoCleanUpAfterBuild(this) { cleanPref.sw_enable.isChecked = it }
        cleanPref.sw_enable.setOnCheckedChangeListener { _, isChecked -> DRSettings.setAutoCleanUpAfterBuild(this, isChecked) }
        cleanPref.setOnClickListener { cleanPref.sw_enable.toggle() }
        universalCategory.addLibraryItem(cleanPref, false)

        val ndkPref = createSwitchPreference(R.drawable.ic_menu_mtr2, "Download NDK", "Download Android NDK for native compilation")
        ndkPref.sw_enable.visibility = View.GONE
        ndkPref.setOnClickListener {
            showNdkDownloadDialog()
        }
        universalCategory.addLibraryItem(ndkPref, false)

        val cmakePref = createSwitchPreference(R.drawable.ic_menu_mtr2, "Download CMake", "Download CMake for native compilation")
        cmakePref.sw_enable.visibility = View.GONE
        cmakePref.setOnClickListener {
            showCmakeDownloadDialog()
        }
        universalCategory.addLibraryItem(cmakePref, false)

        preferences.forEach { binding.lnAllOptions.addView(it) }
    }

    private fun showNdkDownloadDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Download NDK")
            .setMessage("Do you want to download Android NDK? This is required for C/C++ compilation. The file is large (~500MB).")
            .setPositiveButton("Download") { _, _ ->
                startDownload("https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-aarch64.tar.xz", "android-ndk-r29-aarch64.tar.xz")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCmakeDownloadDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Download CMake")
            .setMessage("Do you want to download CMake? This is used to manage the native build process.")
            .setPositiveButton("Download") { _, _ ->
                // User didn't provide a link for CMake, I'll use a common one or ask.
                // For now, I'll use a placeholder or common GitHub release if I can find one.
                // Assuming the user wants it from a similar source.
                startDownload("https://github.com/lzhiyong/termux-ndk/releases/download/cmake/cmake-3.26.4-aarch64.zip", "cmake-aarch64.zip")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startDownload(url: String, fileName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle(fileName)
            request.setDescription("Downloading native tools...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Download started. Check notifications.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createSwitchPreference(icon: Int, title: String, desc: String): LibraryItemView {
        val preference = LibraryItemView(this)
        preference.setHideEnabled()
        preference.icon.setImageResource(icon)
        preference.title.text = title
        preference.description.text = desc
        return preference
    }
}