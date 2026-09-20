package extensions.anbui.daydream.activity.project.settings

import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import extensions.anbui.daydream.settings.DRSettings
import extensions.anbui.daydream.tools.NativeToolchainManager
import kotlinx.coroutines.launch
import pro.sketchware.R
import pro.sketchware.databinding.ActivityDaydreamUniversalSettingsBinding

class DayDreamUniversalSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaydreamUniversalSettingsBinding

    private var isImportingNdk = true

    private val pickArchiveLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            startInstallFromUri(uri, isImportingNdk)
        }
    }

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
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        initializeGeneralSettings()
        initializeNativeTools()

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

    private fun initializeGeneralSettings() {
        DRSettings.getUseBackupTool(this) { binding.swBackup.isChecked = it }
        binding.swBackup.setOnCheckedChangeListener { _, isChecked ->
            DRSettings.setUseBackupTool(this, isChecked)
        }
        binding.lnBackup.setOnClickListener { binding.swBackup.toggle() }

        DRSettings.getAutoCleanUpAfterBuild(this) { binding.swClean.isChecked = it }
        binding.swClean.setOnCheckedChangeListener { _, isChecked ->
            DRSettings.setAutoCleanUpAfterBuild(this, isChecked)
        }
        binding.lnClean.setOnClickListener { binding.swClean.toggle() }
    }

    private fun initializeNativeTools() {
        refreshNativeToolStatus()

        // NDK Actions
        binding.btnDownloadNdk.setOnClickListener {
            showDownloadConfirmation(isNdk = true)
        }
        binding.btnImportNdk.setOnClickListener {
            isImportingNdk = true
            pickArchiveLauncher.launch("*/*")
        }
        binding.btnDeleteNdk.setOnClickListener {
            showDeleteConfirmation(isNdk = true)
        }

        // CMake Actions
        binding.btnDownloadCmake.setOnClickListener {
            showDownloadConfirmation(isNdk = false)
        }
        binding.btnImportCmake.setOnClickListener {
            isImportingNdk = false
            pickArchiveLauncher.launch("*/*")
        }
        binding.btnDeleteCmake.setOnClickListener {
            showDeleteConfirmation(isNdk = false)
        }
    }

    private fun refreshNativeToolStatus() {
        // NDK Status
        val ndkInstalled = NativeToolchainManager.isNdkInstalled(this)
        if (ndkInstalled) {
            binding.tvNdkBadge.text = "Installed"
            binding.tvNdkBadge.setBackgroundResource(R.drawable.bg_round_green)
            binding.tvNdkBadge.setTextColor(getColor(android.R.color.white))
            binding.tvNdkInfo.text = "Installed • Space used: ${NativeToolchainManager.getNdkSizeFormatted(this)}"
            binding.btnDownloadNdk.visibility = View.GONE
            binding.btnImportNdk.text = "Replace"
            binding.btnImportNdk.visibility = View.VISIBLE
            binding.btnDeleteNdk.visibility = View.VISIBLE
        } else {
            binding.tvNdkBadge.text = "Not Installed"
            binding.tvNdkBadge.setBackgroundResource(R.drawable.bg_round_gray)
            binding.tvNdkBadge.setTextColor(getColor(R.color.onSurfaceVariant))
            binding.tvNdkInfo.text = "Status: Not installed (~360 MB download)"
            binding.btnDownloadNdk.text = "Download"
            binding.btnDownloadNdk.visibility = View.VISIBLE
            binding.btnImportNdk.text = "Import Archive"
            binding.btnImportNdk.visibility = View.VISIBLE
            binding.btnDeleteNdk.visibility = View.GONE
        }

        // CMake Status
        val cmakeInstalled = NativeToolchainManager.isCmakeInstalled(this)
        if (cmakeInstalled) {
            binding.tvCmakeBadge.text = "Installed"
            binding.tvCmakeBadge.setBackgroundResource(R.drawable.bg_round_green)
            binding.tvCmakeBadge.setTextColor(getColor(android.R.color.white))
            binding.tvCmakeInfo.text = "Installed • Space used: ${NativeToolchainManager.getCmakeSizeFormatted(this)}"
            binding.btnDownloadCmake.visibility = View.GONE
            binding.btnImportCmake.text = "Replace"
            binding.btnImportCmake.visibility = View.VISIBLE
            binding.btnDeleteCmake.visibility = View.VISIBLE
        } else {
            binding.tvCmakeBadge.text = "Not Installed"
            binding.tvCmakeBadge.setBackgroundResource(R.drawable.bg_round_gray)
            binding.tvCmakeBadge.setTextColor(getColor(R.color.onSurfaceVariant))
            binding.tvCmakeInfo.text = "Status: Not installed (~48 MB download)"
            binding.btnDownloadCmake.text = "Download"
            binding.btnDownloadCmake.visibility = View.VISIBLE
            binding.btnImportCmake.text = "Import Archive"
            binding.btnImportCmake.visibility = View.VISIBLE
            binding.btnDeleteCmake.visibility = View.GONE
        }

        val ndkPath = NativeToolchainManager.getNdkDir(this).absolutePath
        val cmakePath = NativeToolchainManager.getCmakeDir(this).absolutePath
        binding.tvStorageInfo.text = "Download location: $ndkPath and $cmakePath"
    }

    private fun showDownloadConfirmation(isNdk: Boolean) {
        val title = if (isNdk) "Download Android NDK" else "Download CMake"
        val message = if (isNdk) {
            "Download Android NDK (r29)? Required for compiling C/C++ native code. Archive size is ~360MB."
        } else {
            "Download CMake (3.26.4)? Used to manage native compilation. Archive size is ~48MB."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Download") { _, _ ->
                val url = if (isNdk) NativeToolchainManager.NDK_DEFAULT_URL else NativeToolchainManager.CMAKE_DEFAULT_URL
                startDownloadAndInstall(url, isNdk)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(isNdk: Boolean) {
        val toolName = if (isNdk) "Android NDK" else "CMake"
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete $toolName?")
            .setMessage("Are you sure you want to remove $toolName from app storage? You will not be able to compile native C/C++ code until re-downloaded.")
            .setPositiveButton("Delete") { _, _ ->
                if (isNdk) {
                    NativeToolchainManager.deleteNdk(this)
                } else {
                    NativeToolchainManager.deleteCmake(this)
                }
                refreshNativeToolStatus()
                Toast.makeText(this, "$toolName deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startDownloadAndInstall(url: String, isNdk: Boolean) {
        val toolName = if (isNdk) "NDK" else "CMake"
        binding.cardProgress.visibility = View.VISIBLE
        binding.tvProgressTitle.text = "Downloading $toolName"
        binding.progressIndicator.isIndeterminate = false
        binding.progressIndicator.progress = 0

        setButtonsEnabled(false)

        lifecycleScope.launch {
            val result = NativeToolchainManager.downloadAndInstall(
                context = this@DayDreamUniversalSettingsActivity,
                urlStr = url,
                isNdk = isNdk
            ) { progress, message ->
                runOnUiThread {
                    binding.progressIndicator.progress = progress
                    binding.tvProgressStatus.text = message
                }
            }

            binding.cardProgress.visibility = View.GONE
            setButtonsEnabled(true)
            refreshNativeToolStatus()

            if (result.isSuccess) {
                Toast.makeText(this@DayDreamUniversalSettingsActivity, "$toolName installed successfully!", Toast.LENGTH_LONG).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                MaterialAlertDialogBuilder(this@DayDreamUniversalSettingsActivity)
                    .setTitle("Installation Failed")
                    .setMessage("Failed to download or install $toolName:\n$error\n\nYou can also download the archive manually and use 'Import Archive'.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun startInstallFromUri(uri: Uri, isNdk: Boolean) {
        val toolName = if (isNdk) "NDK" else "CMake"
        binding.cardProgress.visibility = View.VISIBLE
        binding.tvProgressTitle.text = "Importing $toolName Archive"
        binding.progressIndicator.isIndeterminate = true
        binding.tvProgressStatus.text = "Extracting and configuring files..."

        setButtonsEnabled(false)

        lifecycleScope.launch {
            val result = NativeToolchainManager.installFromUri(
                context = this@DayDreamUniversalSettingsActivity,
                uri = uri,
                isNdk = isNdk
            ) { progress, message ->
                runOnUiThread {
                    binding.progressIndicator.isIndeterminate = false
                    binding.progressIndicator.progress = progress
                    binding.tvProgressStatus.text = message
                }
            }

            binding.cardProgress.visibility = View.GONE
            setButtonsEnabled(true)
            refreshNativeToolStatus()

            if (result.isSuccess) {
                Toast.makeText(this@DayDreamUniversalSettingsActivity, "$toolName imported and installed!", Toast.LENGTH_LONG).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                MaterialAlertDialogBuilder(this@DayDreamUniversalSettingsActivity)
                    .setTitle("Import Failed")
                    .setMessage("Failed to import $toolName archive:\n$error")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnDownloadNdk.isEnabled = enabled
        binding.btnImportNdk.isEnabled = enabled
        binding.btnDeleteNdk.isEnabled = enabled

        binding.btnDownloadCmake.isEnabled = enabled
        binding.btnImportCmake.isEnabled = enabled
        binding.btnDeleteCmake.isEnabled = enabled
    }
}