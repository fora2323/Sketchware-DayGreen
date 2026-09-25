package mod.hilal.saif.activities.tools

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.chrisbanes.insetter.Insetter
import dev.pranav.filepicker.FilePickerCallback
import dev.pranav.filepicker.FilePickerDialogFragment
import dev.pranav.filepicker.FilePickerOptions
import dev.pranav.filepicker.SelectionMode
import org.sketchware.daygreen.DownloadUtility
import org.sketchware.daygreen.FileCheckUtils
import pro.sketchware.R
import pro.sketchware.databinding.ActivityGenericListBinding
import java.io.File

class BuildToolsActivity : BaseAppCompatActivity() {
    private lateinit var binding: ActivityGenericListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)
        binding = ActivityGenericListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Insetter.builder().padding(WindowInsetsCompat.Type.statusBars()).applyToView(binding.toolbar)

        Insetter.builder().padding(WindowInsetsCompat.Type.navigationBars()).applyToView(binding.listContainer)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Build Tools"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupItems()
    }

    private fun setupItems() {
        val abi = DownloadUtility.getDeviceAbi()
        binding.listContainer.removeAllViews()

        // NDK
        val ndkAbi = when (abi) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            else -> "aarch64"
        }
        addToolCard(
            title = "Android NDK (r29)",
            description = "Required for compiling native C/C++ code",
            isInstalled = FileCheckUtils.isNdkDownloaded(this),
            size = FileCheckUtils.getNdkSize(this),
            url = "https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-$ndkAbi.tar.xz",
            destination = File(filesDir, "native/ndk.tar.xz")
        )

        // CMake
        val cmakeAbi = when (abi) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x64"
            else -> "arm64"
        }
        addToolCard(
            title = "CMake (3.25.3)",
            description = "Build tool to configure and compile native code",
            isInstalled = FileCheckUtils.isCmakeDownloaded(this),
            size = FileCheckUtils.getCmakeSize(this),
            url = "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/3.25.3/cmake-3.25.3-1-linux-$cmakeAbi.tar.gz",
            destination = File(filesDir, "native/cmake.tar.gz")
        )

        // AAPT
        addToolCard(
            title = "AAPT (Android Asset Packaging Tool)",
            description = "Tool for compiling and packaging Android resources",
            isInstalled = FileCheckUtils.isAaptDownloaded(this),
            size = FileCheckUtils.getAaptSize(this),
            url = "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/2.19/aapt2-$abi",
            destination = File(filesDir, "bin/aapt2")
        )

        // AIDL
        val aidlAbi = when (abi) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm32"
            "x86_64" -> "x86_64"
            else -> "x86"
        }
        addToolCard(
            title = "AIDL (Android Interface Definition Language)",
            description = "Tool for compiling .aidl files to Java",
            isInstalled = File(filesDir, "bin/aidl").exists(),
            size = if (File(filesDir, "bin/aidl").exists())
                "%.1f MB".format(File(filesDir, "bin/aidl").length() / 1024f / 1024f)
            else "~3 MB",
            url = "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/35/aidl-$aidlAbi",
            destination = File(filesDir, "bin/aidl")
        )
    }

    private fun addToolCard(title: String, description: String, isInstalled: Boolean, size: String, url: String, destination: File) {
        val cardView = layoutInflater.inflate(R.layout.item_download_card, binding.listContainer, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.title)
        val tvDesc = cardView.findViewById<TextView>(R.id.description)
        val tvStatusChip = cardView.findViewById<TextView>(R.id.status_chip)
        val tvStatusText = cardView.findViewById<TextView>(R.id.status_text)
        val btnImport = cardView.findViewById<View>(R.id.btn_import)
        val btnDownload = cardView.findViewById<View>(R.id.btn_download)
        val imgIcon = cardView.findViewById<ImageView>(R.id.icon)

        tvTitle.text = title
        tvDesc.text = description
        tvStatusChip.text = if (isInstalled) "Installed" else "Not Installed"
        tvStatusChip.alpha = if (isInstalled) 1.0f else 0.6f

        tvStatusText.text = "Status: ${if (isInstalled) "Installed" else "Not installed"} ($size ${if (isInstalled) "used" else "download"})"

        val downloadBtn = btnDownload as MaterialButton
        if (isInstalled) {
            downloadBtn.text = "Remove"
            downloadBtn.setIconResource(R.drawable.ic_mtrl_delete)
            downloadBtn.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Remove Tool")
                    .setMessage("Are you sure you want to remove $title?")
                    .setPositiveButton("Remove") { _, _ ->
                        if (destination.exists()) {
                            destination.delete()
                        }
                        if (title.contains("NDK")) {
                            File(filesDir, "native/ndk").deleteRecursively()
                        } else if (title.contains("CMake")) {
                            File(filesDir, "native/cmake").deleteRecursively()
                        }
                        setupItems()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            downloadBtn.text = "Download"
            downloadBtn.setIconResource(R.drawable.ic_mtrl_download)
            downloadBtn.setOnClickListener {
                DownloadUtility.downloadFile(this, url, destination) {
                    if (title.contains("AAPT") || title.contains("AIDL")) {
                        destination.setExecutable(true)
                    }
                    setupItems()
                }
            }
        }

        btnImport.setOnClickListener {
            importArchive(destination)
        }

        if (title.contains("NDK")) {
            imgIcon.setImageResource(R.drawable.ic_mtrl_cpp)
            imgIcon.setColorFilter(MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLACK))
        } else if (title.contains("CMake")) {
            imgIcon.setImageResource(R.drawable.ic_mtrl_terminal)
            imgIcon.setColorFilter(MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLACK))
        } else if (title.contains("AAPT")) {
            imgIcon.setImageResource(R.drawable.ic_mtrl_box)
            imgIcon.setColorFilter(MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLACK))
        } else if (title.contains("AIDL")) {
            imgIcon.setImageResource(R.drawable.ic_mtrl_code)
            imgIcon.setColorFilter(MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLACK))
        }

        binding.listContainer.addView(cardView)
    }

    private fun importArchive(destination: File) {
        val options = FilePickerOptions().apply {
            selectionMode = SelectionMode.FILE
            extensions = arrayOf("zip", "tar.xz", "jar", "tar.gz", "tgz")
        }

        val callback = object : FilePickerCallback() {
            override fun onFileSelected(file: File) {
                destination.parentFile?.mkdirs()
                file.copyTo(destination, overwrite = true)
                val name = destination.name.lowercase()
                if (name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tar.xz") || name.endsWith(".tgz")) {
                    DownloadUtility.extractArchive(this@BuildToolsActivity, destination) {
                        Toast.makeText(this@BuildToolsActivity, "Imported and extracted ${file.name}", Toast.LENGTH_SHORT).show()
                        setupItems()
                    }
                } else {
                    if (destination.name.contains("aapt") || destination.name.contains("aidl")) {
                        destination.setExecutable(true, false)
                    }
                    Toast.makeText(this@BuildToolsActivity, "Imported ${file.name}", Toast.LENGTH_SHORT).show()
                    setupItems()
                }
            }
        }

        FilePickerDialogFragment(options, callback).show(supportFragmentManager, "file_picker")
    }
}