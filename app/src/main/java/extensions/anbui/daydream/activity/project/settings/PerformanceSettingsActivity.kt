package extensions.anbui.daydream.activity.project.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import extensions.anbui.daydream.project.DRProjectTracker.startNow
import extensions.anbui.daydream.project.ProjectLibrary
import extensions.anbui.daydream.settings.DayDreamProjectSettings
import pro.sketchware.databinding.ActivityPerformanceSettingsBinding

class PerformanceSettingsActivity : AppCompatActivity() {
    private var projectID: String? = null
    private lateinit var binding: ActivityPerformanceSettingsBinding

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
        binding = ActivityPerformanceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { _ -> finish() }
        initialize()
    }

    private fun initialize() {
        if (ProjectLibrary.isEnabledAdmob(projectID)) {
            binding.swImproveAdmobPerformance.setChecked(
                DayDreamProjectSettings.getImproveAdMobPerformance(
                    projectID
                )
            )
            binding.swImproveAdmobPerformance.setOnCheckedChangeListener { _, isChecked ->
                DayDreamProjectSettings.setImproveAdMobPerformance(
                    projectID,
                    isChecked
                )
            }

            binding.lnImproveAdmobPerformance.setOnClickListener { _ -> binding.swImproveAdmobPerformance.toggle() }
        } else {
            binding.tvImproveAdmobPerformanceNote.text = "Not available because this project is not using AdMob."
            binding.lnImproveAdmobPerformance.alpha = 0.5f
            binding.lnImproveAdmobPerformance.isEnabled = false
        }
    }
}