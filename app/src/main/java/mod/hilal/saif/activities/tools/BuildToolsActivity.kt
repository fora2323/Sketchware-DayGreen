package mod.hilal.saif.activities.tools

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.besome.sketch.editor.manage.library.LibraryCategoryView
import com.besome.sketch.editor.manage.library.LibraryItemView
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mod.hey.studios.util.Helper
import org.sketchware.daygreen.DownloadUtility
import org.sketchware.daygreen.FileCheckUtils
import pro.sketchware.databinding.ActivityGitSettingsBinding
import pro.sketchware.utility.SketchwareUtil
import java.io.File

class BuildToolsActivity : BaseAppCompatActivity() {
    private lateinit var binding: ActivityGitSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)
        binding = ActivityGitSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(
                v.paddingLeft + systemInsets.left,
                v.paddingTop,
                v.paddingRight + systemInsets.right,
                v.paddingBottom
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScroll) { v, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                v.paddingBottom + navBarInsets.bottom
            )
            insets
        }

        binding.collapsingToolbar.title = "Build Tools"
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this))

        setupItems(binding.contentContainer)
    }

    private fun setupItems(content: ViewGroup) {
        content.removeAllViews()

        // Sdk Manager
        val sdkCategory = LibraryCategoryView(this)
        sdkCategory.setTitle("Sdk Manager")
        content.addView(sdkCategory)

        sdkCategory.addLibraryItem(createPreference(
            "37.0.0",
            "sdk manager version 37.0.0",
            isSdkToolsDownloaded("37.0.0"),
            "android-sdk-tools-v37.0.0"
        ), true)

        sdkCategory.addLibraryItem(createPreference(
            "36.0.0",
            "sdk manager version 36.0.0",
            isSdkToolsDownloaded("36.0.0"),
            "android-sdk-tools-v36.0.0"
        ), false)

        // Ndk Manager
        val ndkCategory = LibraryCategoryView(this)
        ndkCategory.setTitle("Ndk Manager")
        content.addView(ndkCategory)

        val abi = DownloadUtility.getDeviceAbi()
        val ndkAbi = when (abi) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            else -> "aarch64"
        }
        ndkCategory.addLibraryItem(createPreference(
            "Android NDK (r29)",
            "Required for compiling native C/C++ code",
            FileCheckUtils.isNdkDownloaded(this),
            "ndk",
            "https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-$ndkAbi.tar.xz"
        ), true)

        val cmakeAbi = when (abi) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x64"
            else -> "arm64"
        }
        ndkCategory.addLibraryItem(createPreference(
            "CMake (3.25.3)",
            "Build tool to configure and compile native code",
            FileCheckUtils.isCmakeDownloaded(this),
            "cmake",
            "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/3.25.3/cmake-3.25.3-1-linux-$cmakeAbi.tar.gz"
        ), false)
    }

    private fun isSdkToolsDownloaded(version: String): Boolean {
        return File(filesDir, "bin/$version/aapt2").exists() &&
               File(filesDir, "bin/$version/aidl").exists() &&
               File(filesDir, "bin/$version/zipalign").exists()
    }

    private fun createPreference(
        title: String,
        desc: String,
        isInstalled: Boolean,
        toolId: String,
        customUrl: String? = null
    ): LibraryItemView {
        val preference = LibraryItemView(this)
        preference.setHideIcon(true)
        preference.setHideEnabled()
        val installedText = if (isInstalled) " (Installed)" else ""
        preference.title.text = title + installedText
        preference.description.text = desc

        preference.setOnClickListener {
            if (isInstalled) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Remove Tool")
                    .setMessage("Are you sure you want to remove $title?")
                    .setPositiveButton("Remove") { _, _ ->
                        if (toolId == "ndk") {
                            File(filesDir, "native/ndk").deleteRecursively()
                        } else if (toolId == "cmake") {
                            File(filesDir, "native/cmake").deleteRecursively()
                        } else if (toolId.startsWith("android-sdk-tools-v")) {
                            val version = toolId.removePrefix("android-sdk-tools-v")
                            File(filesDir, "bin/$version").deleteRecursively()
                        }
                        setupItems(binding.contentContainer)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Download Tool")
                    .setMessage("Do you want to download $title?")
                    .setPositiveButton("Download") { _, _ ->
                        val abi = DownloadUtility.getDeviceAbi()
                        val url = customUrl ?: "https://github.com/gus23-okta/DayGreen-Tools/releases/download/$toolId/android-sdk-tools-static-$abi.zip"
                        
                        val destination = if (toolId == "ndk") {
                            File(filesDir, "native/ndk.tar.xz")
                        } else if (toolId == "cmake") {
                            File(filesDir, "native/cmake.tar.gz")
                        } else {
                            File(filesDir, "bin/${toolId.removePrefix("android-sdk-tools-v")}/tools.zip")
                        }

                        DownloadUtility.downloadFile(this, url, destination) {
                            setupItems(binding.contentContainer)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        return preference
    }
}
