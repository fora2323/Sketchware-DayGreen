package org.sketchware.daygreen.blocks

import a.a.a.xB
import android.content.Context
import com.besome.sketch.editor.LogicEditorActivity
import pro.sketchware.R
import pro.sketchware.SketchApplication
import pro.sketchware.utility.ThemeUtils

object IDBlockShizuku {

    private fun getTitleBgColor(logicEditor: LogicEditorActivity): Int {
        return ThemeUtils.getColor(logicEditor, if (ThemeUtils.isDarkThemeEnabled(logicEditor)) R.attr.colorSurfaceContainerHigh else R.attr.colorSurfaceInverse)
    }

    @JvmStatic
    fun addBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        registerLabels(SketchApplication.getContext())

        var hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "shizukuPingBinder"
        hashMap["type"] = "b"
        hashMap["code"] = "%s.pingBinder()"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.shizuku pingBinder"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuCheckPermission"
        hashMap["type"] = "b"
        hashMap["code"] = "%s.checkSelfPermission()"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.shizuku checkSelfPermission"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuGetVersion"
        hashMap["type"] = "d"
        hashMap["code"] = "%s.getVersion()"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.shizuku getVersion"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuGetUid"
        hashMap["type"] = "d"
        hashMap["code"] = "%s.getUid()"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.shizuku getUid"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuRequestPermission"
        hashMap["type"] = " "
        hashMap["code"] = "%s.requestPermission((int)%s);"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.shizuku requestPermission requestCode %d"
        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addPaletteBlocks(logicEditor: LogicEditorActivity) {
        registerLabels(logicEditor)
        logicEditor.a("Shizuku", getTitleBgColor(logicEditor))
        logicEditor.a("b", "shizukuPingBinder")
        logicEditor.a("b", "shizukuCheckPermission")
        logicEditor.a("d", "shizukuGetVersion")
        logicEditor.a("d", "shizukuGetUid")        
        logicEditor.a(" ", "shizukuRequestPermission")
    }

    /**
     * Label diambil dari map xB.c berdasarkan opCode. Token %m.shizuku membuat dropdown
     * pemilih component, sama seperti %m.intent pada block Intent.
     */
    private fun registerLabels(context: Context) {
        val xb = xB.b()
        xb.a(context, "shizukuPingBinder")
        xb.c["shizukuPingBinder"] = "%m.shizuku pingBinder"
        xb.c["shizukuCheckPermission"] = "%m.shizuku checkSelfPermission"
        xb.c["shizukuGetVersion"] = "%m.shizuku getVersion"
        xb.c["shizukuGetUid"] = "%m.shizuku getUid"        
        xb.c["shizukuRequestPermission"] = "%m.shizuku requestPermission requestCode %d"
    }
}