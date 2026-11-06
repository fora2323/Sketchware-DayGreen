package mod.hilal.saif.activities.tools

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowInsetsCompat
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.chrisbanes.insetter.Insetter
import org.sketchware.daygreen.DownloadUtility
import org.sketchware.daygreen.FileCheckUtils
import pro.sketchware.R
import pro.sketchware.databinding.ActivityGenericListBinding
import java.io.File

class SdkManagerActivity : BaseAppCompatActivity() {
    private lateinit var binding: ActivityGenericListBinding
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)
        binding = ActivityGenericListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Insetter.builder()
            .padding(WindowInsetsCompat.Type.navigationBars())
            .applyToView(binding.listContainer)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.collapsingToolbar.title = "SDK Manager"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupItems()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sdk_manager, menu)
        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = "Search SDK (e.g. 34)"
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                setupItems(newText.orEmpty())
                return true
            }
        })
        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                setupItems("")
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupItems(query: String = currentQuery) {
        currentQuery = query
        binding.listContainer.removeAllViews()

        val trimmed = query.trim().lowercase()

        for (api in 26..37) {
            val title = "Android SDK $api"
            val description = "Download android.jar for API $api"
            val destination = File(filesDir, "libs/android-$api.jar")
            val isInstalled = destination.exists() && destination.length() > 0

            if (trimmed.isNotEmpty()) {
                val matchesTitle = title.lowercase().contains(trimmed)
                val matchesApi = api.toString().contains(trimmed)
                val matchesDesc = description.lowercase().contains(trimmed)
                val matchesStatus = if (isInstalled) "installed".contains(trimmed) else "not installed".contains(trimmed)
                if (!matchesTitle && !matchesApi && !matchesDesc && !matchesStatus) {
                    continue
                }
            }

            addToolCard(
                title = title,
                description = description,
                isInstalled = isInstalled,
                size = FileCheckUtils.getSdkVersionSize(this, api.toString()),
                url = "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/37/android-$api.jar",
                destination = destination
            )
        }
    }

    private fun addToolCard(
        title: String,
        description: String,
        isInstalled: Boolean,
        size: String,
        url: String,
        destination: File
    ) {
        val cardView = layoutInflater.inflate(R.layout.item_download_card, binding.listContainer, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.title)
        val tvDesc = cardView.findViewById<TextView>(R.id.description)
        val tvStatusChip = cardView.findViewById<TextView>(R.id.status_chip)
        val tvStatusText = cardView.findViewById<TextView>(R.id.status_text)
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
                    .setTitle("Remove SDK")
                    .setMessage("Are you sure you want to remove $title?")
                    .setPositiveButton("Remove") { _, _ ->
                        if (destination.exists()) {
                            destination.delete()
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
                    setupItems()
                }
            }
        }

        imgIcon.setImageResource(R.drawable.ic_mtrl_android)
        imgIcon.setColorFilter(MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLACK))

        binding.listContainer.addView(cardView)
    }
}
