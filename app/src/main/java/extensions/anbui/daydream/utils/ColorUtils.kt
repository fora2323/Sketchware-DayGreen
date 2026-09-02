package extensions.anbui.daydream.utils

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import com.besome.sketch.lib.ui.ColorPickerDialog
import pro.sketchware.activities.resourceseditor.components.utils.ColorsEditorManager
import pro.sketchware.utility.PropertiesUtil


object ColorUtils {
    @JvmStatic
    fun colorPickerLegacy(context : Context, view : View, map : HashMap<String, Any>, onPicked : Runnable?) {
        val colorPicker = ColorPickerDialog(context as Activity?, "#FFFFFF", false, false, "0")
        colorPicker.a(object : ColorPickerDialog.b {
            override fun a(var1: Int) {
                map["selected_color"] = var1
                map["selected_color_hex"] = "#" + String.format("%06X", var1 and (0x00FFFFFF))
                onPicked?.run()
            }

            override fun a(var1: String?, var2: Int) {
                map["selected_color"] = var2
                map["selected_color_hex"] = "@color/$var1"
                onPicked?.run()
            }
        })

        colorPicker.materialColorAttr { attr: String, attrColor: Int ->
            map["selected_color"] = PropertiesUtil.parseColor(
                ColorsEditorManager().getColorValue(
                    context,
                    attr,
                    3
                )
            )
            map["selected_color_hex"] = "?attr/$attr"
            onPicked?.run()
        }

        colorPicker.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    @JvmStatic
    fun isColorLight(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 186
    }
}