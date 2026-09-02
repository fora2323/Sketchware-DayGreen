package extensions.anbui.daydream.activity.project.settings

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import extensions.anbui.daydream.settings.DRSettings
import pro.sketchware.databinding.ActivityDaydreamUniversalSettingsBinding

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
        DRSettings.getUseBackupTool(this) { isUse ->
            binding.swBackuptool.setChecked(isUse)
        }
        DRSettings.getAutoCleanUpAfterBuild(this) { isClean ->
            binding.swAutocleanafterbuild.setChecked(isClean);
        }

        binding.swBackuptool.setOnCheckedChangeListener { _, isChecked ->
            DRSettings.setUseBackupTool(this, isChecked)
        }
        binding.lnBackuptool.setOnClickListener { _ -> binding.swBackuptool.toggle() }

        binding.swAutocleanafterbuild.setOnCheckedChangeListener { _, isChecked ->
            DRSettings.setAutoCleanUpAfterBuild(this, isChecked)
        }
        binding.lnAutocleanafterbuild.setOnClickListener { _ -> binding.swAutocleanafterbuild.toggle() }
    }
}