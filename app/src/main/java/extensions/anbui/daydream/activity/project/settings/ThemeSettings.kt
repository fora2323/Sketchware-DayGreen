package extensions.anbui.daydream.activity.project.settings

import a.a.a.jC
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.besome.sketch.beans.ProjectLibraryBean
import com.besome.sketch.editor.manage.library.material3.Material3LibraryManager
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.project.DRProjectTracker
import extensions.anbui.daydream.settings.DayDreamProjectSettings
import pro.sketchware.databinding.ActivityDaydreamThemeSettingsBinding


class ThemeSettings : AppCompatActivity() {
    private var projectID: String? = null
    private lateinit var binding: ActivityDaydreamThemeSettingsBinding
    private lateinit var material3LibraryManager : Material3LibraryManager
    private lateinit var compatLibraryBean : ProjectLibraryBean

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
        binding = ActivityDaydreamThemeSettingsBinding.inflate(layoutInflater)
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

    private fun initialize() {
        binding.swEnabled.setChecked(DayDreamProjectSettings.isUseTheme(projectID))
        binding.swEnabled.setOnCheckedChangeListener { _, isChecked ->
            DayDreamProjectSettings.setUseTheme(projectID, isChecked)
            universalUIController(isChecked)
        }
        binding.lnEnabled.setOnClickListener { _ -> binding.swEnabled.toggle() }

        binding.swDynamiccolor.setChecked(DayDreamProjectSettings.isUseDynamicColor(projectID))
        binding.swDynamiccolor.setOnCheckedChangeListener { _, isChecked ->
            DayDreamProjectSettings.setUseDynamicColor(
                projectID,
                isChecked
            )
        }
        binding.lnDynamiccolor.setOnClickListener { _ -> binding.swDynamiccolor.toggle() }

        compatLibraryBean = jC.c(projectID).c()

        material3LibraryManager =
            Material3LibraryManager(compatLibraryBean)

        val currentTheme: Int = DayDreamProjectSettings.getTheme(projectID)
        when (currentTheme) {
            Configs.material2Theme -> {
                binding.themeSelector.check(binding.selectM2.id)
            }

            Configs.material3Theme -> {
                binding.themeSelector.check(binding.selectM3.id)
            }

            else -> {
                binding.themeSelector.check(binding.selectM3e.id)
            }
        }

        if (currentTheme == Configs.material2Theme) {
            binding.lnDynamiccolor.setAlpha(0.5f)
            binding.lnDynamiccolor.setEnabled(false)
        }

        val currentDayNight: Int = DayDreamProjectSettings.getThemeDayNight(projectID)
        when (currentDayNight) {
            Configs.DayTheme -> {
                binding.dnSelector.check(binding.selectDay.id)
            }

            Configs.NightTheme -> {
                binding.dnSelector.check(binding.selectNight.id)
            }

            else -> {
                binding.dnSelector.check(binding.selectDaynight.id)
            }
        }

        binding.themeSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.selectM2.id -> {
                        DayDreamProjectSettings.setTheme(projectID,
                            Configs.material2Theme)

                        binding.lnDynamiccolor.setAlpha(0.5f)
                        binding.lnDynamiccolor.setEnabled(false)
                        material3LibraryManager.appCombatLibraryBean.configurations["material3"] =
                            false
                    }

                    binding.selectM3.id -> {
                        DayDreamProjectSettings.setTheme(projectID,
                            Configs.material3Theme)

                        binding.lnDynamiccolor.setAlpha(1f)
                        binding.lnDynamiccolor.setEnabled(true)
                        material3LibraryManager.appCombatLibraryBean.configurations["material3"] =
                            true
                    }

                    binding.selectM3e.id -> {
                        DayDreamProjectSettings.setTheme(
                            projectID,
                            Configs.material3ExpressiveTheme
                        )

                        binding.lnDynamiccolor.setAlpha(1f)
                        binding.lnDynamiccolor.setEnabled(true)
                        material3LibraryManager.appCombatLibraryBean.configurations["material3"] =
                            true
                    }
                }
            }
        }

        binding.dnSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.selectDay.id -> {
                        DayDreamProjectSettings.setThemeDayNight(projectID, Configs.DayTheme)
                        material3LibraryManager.appCombatLibraryBean.configurations["theme"] = "Light"
                    }

                    binding.selectNight.id -> {
                        DayDreamProjectSettings.setThemeDayNight(projectID, Configs.NightTheme)
                        material3LibraryManager.appCombatLibraryBean.configurations["theme"] = "Dark"
                    }

                    binding.selectDaynight.id -> {
                        DayDreamProjectSettings.setThemeDayNight(projectID, Configs.DayNightTheme)
                        material3LibraryManager.appCombatLibraryBean.configurations["theme"] = "DayNight"
                    }
                }
            }
        }

        universalUIController(binding.swEnabled.isChecked)
    }

    private fun universalUIController(isEnable: Boolean) {
        binding.lnAllOptions.setAlpha(if (isEnable) 1f else 0.5f)

        binding.selectM2.setEnabled(isEnable)
        binding.selectM3.setEnabled(isEnable)
        binding.selectM3e.setEnabled(isEnable)
        binding.lnDynamiccolor.setEnabled(isEnable)
        binding.selectDay.setEnabled(isEnable)
        binding.selectNight.setEnabled(isEnable)
        binding.selectDaynight.setEnabled(isEnable)
    }

    private fun saveForSK() {
        material3LibraryManager.appCombatLibraryBean.configurations["dynamic_colors"] = binding.swDynamiccolor.isChecked
        jC.c(projectID).b(compatLibraryBean)
    }

    private fun onBack() {
        saveForSK()
        finish()
    }
}