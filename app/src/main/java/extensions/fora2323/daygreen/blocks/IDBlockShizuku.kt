package extensions.fora2323.daygreen.blocks

import com.besome.sketch.editor.LogicEditorActivity
import pro.sketchware.R
import pro.sketchware.utility.ThemeUtils

object IDBlockShizuku {

    private fun getTitleBgColor(logicEditor: LogicEditorActivity): Int {
        return ThemeUtils.getColor(logicEditor, if (ThemeUtils.isDarkThemeEnabled(logicEditor)) R.attr.colorSurfaceContainerHigh else R.attr.colorSurfaceInverse)
    }

    @JvmStatic
    fun addBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        var hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "shizukuPingBinder"
        hashMap["type"] = "b"
        hashMap["code"] = "Shizuku.pingBinder()"
        hashMap["color"] = "#673AB7"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku pingBinder"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuCheckPermission"
        hashMap["type"] = "b"
        hashMap["code"] = "Shizuku.checkSelfPermission()"
        hashMap["color"] = "#673AB7"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku checkSelfPermission"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuRequestPermission"
        hashMap["type"] = " "
        hashMap["code"] = "Shizuku.requestPermission((int)%s);"
        hashMap["color"] = "#673AB7"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku requestPermission requestCode %d"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuGetVersion"
        hashMap["type"] = "d"
        hashMap["code"] = "Shizuku.getVersion()"
        hashMap["color"] = "#673AB7"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku getVersion"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuGetUid"
        hashMap["type"] = "d"
        hashMap["code"] = "Shizuku.getUid()"
        hashMap["color"] = "#673AB7"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku getUid"
        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addPaletteBlocks(logicEditor: LogicEditorActivity) {
        logicEditor.a("Shizuku", getTitleBgColor(logicEditor))
        logicEditor.a("b", "shizukuPingBinder")
        logicEditor.a("b", "shizukuCheckPermission")
        logicEditor.a(" ", "shizukuRequestPermission")
        logicEditor.a("d", "shizukuGetVersion")
        logicEditor.a("d", "shizukuGetUid")
    }
}