package extensions.anbui.daydream.theme

import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.settings.DayDreamProjectSettings

object DayDreamThemeTool {
    @JvmStatic
    fun generateCodeForMaterialType(projectID : String) : String {
        var finalTheme = ""
        var finalDayNight = ""

        val currentTheme : Int = DayDreamProjectSettings.getTheme(projectID)
        val currentDayNight : Int = DayDreamProjectSettings.getThemeDayNight(projectID)

        when (currentTheme) {
            Configs.material2Theme -> {
                finalTheme = "Theme.MaterialComponents"
            }
            Configs.material3Theme -> {
                finalTheme = "Theme.Material3"
            }
            Configs.material3ExpressiveTheme -> {
                finalTheme = "Theme.Material3Expressive"
            }
        }

        when (currentDayNight) {
            Configs.DayTheme -> {
                finalDayNight = ".Light"
            }
            Configs.NightTheme -> {
                if (currentTheme > Configs.material2Theme) {
                    finalDayNight = ".Dark"
                }
            }
            Configs.DayNightTheme -> {
                finalDayNight = ".DayNight"
            }
        }

        return finalTheme + finalDayNight
    }
}