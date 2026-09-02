package extensions.anbui.daydream.activity.project.settings.library

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import extensions.anbui.daydream.project.DRProjectTracker
import extensions.anbui.daydream.settings.DayDreamProjectSettings
import pro.sketchware.databinding.ActivityDaydreamOneSignalSettingsBinding

class OneSignalSettingsActivity : AppCompatActivity() {
    private var projectID: String? = null
    private lateinit var binding: ActivityDaydreamOneSignalSettingsBinding

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
            DRProjectTracker.startNow(projectID)
        } else {
            finish()
            return
        }
        this.enableEdgeToEdge()
        binding = ActivityDaydreamOneSignalSettingsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBack() }
        initialize()

        if (SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                onBack()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                onBack()
            }
        }
    }

    fun initialize() {
        binding.swEnabled.setChecked(DayDreamProjectSettings.isUseOneSignal(projectID))
        binding.swEnabled.setOnCheckedChangeListener { _, isChecked ->
            DayDreamProjectSettings.setUseOneSignal(projectID, isChecked)
            universalUIController(isChecked)
        }
        binding.lnEnabled.setOnClickListener { _ -> binding.swEnabled.toggle() }

        binding.etAppid.setText(DayDreamProjectSettings.getOneSignalAppId(projectID))

        binding.swAutoinitialize.setChecked(DayDreamProjectSettings.isAutoInitializeOneSignal(projectID))
        binding.swAutoinitialize.setOnCheckedChangeListener { _, isChecked -> DayDreamProjectSettings.setAutoInitializeOneSignal(projectID, isChecked) }
        binding.lnAutoinitialize.setOnClickListener { _ -> binding.swAutoinitialize.toggle() }

        universalUIController(binding.swEnabled.isChecked)
    }

    fun universalUIController(isEnable: Boolean) {
        binding.lnAllOptions.setAlpha(if (isEnable) 1.0f else 0.5f)
        binding.etAppid.setEnabled(isEnable)
        binding.lnAutoinitialize.setEnabled(isEnable)
    }

    fun onBack() {
        DayDreamProjectSettings.setOneSignalAppId(projectID, binding.etAppid.getText().toString())
        finish()
    }
}