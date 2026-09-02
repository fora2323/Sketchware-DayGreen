package extensions.anbui.daydream.activity.project.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import extensions.anbui.daydream.project.DRProjectTracker.startNow
import extensions.anbui.daydream.settings.DayDreamProjectSettings
import pro.sketchware.databinding.ActivityDaydreamLayoutSettingsBinding

class LayoutSettings : AppCompatActivity() {
    private var projectID: String? = null
    private lateinit var binding: ActivityDaydreamLayoutSettingsBinding

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("sc_id")) {
            projectID = intent.getStringExtra("sc_id")
            startNow(projectID)
        } else {
            finish()
            return
        }
        this.enableEdgeToEdge()
        binding = ActivityDaydreamLayoutSettingsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { _ -> finish() }
        initialize()
    }

    private fun initialize() {
        binding.swAdvancedpadding.setChecked(DayDreamProjectSettings.getAdvancedPadding(projectID))
        binding.swAdvancedpadding.setOnCheckedChangeListener { _, isChecked ->
            DayDreamProjectSettings.setAdvancedPadding(
                projectID,
                isChecked
            )
        }

        binding.lnAdvancedpadding.setOnClickListener { _ -> binding.swAdvancedpadding.toggle() }
    }
}