package org.sketchware.daygreen.blocks

import a.a.a.xB
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
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku pingBinder"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuCheckPermission"
        hashMap["type"] = "b"
        hashMap["code"] = "Shizuku.checkSelfPermission()"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku checkSelfPermission"
        arrayList.add(hashMap)

        hashMap = HashMap()
        hashMap["name"] = "shizukuRequestPermission"
        hashMap["type"] = " "
        hashMap["code"] = "Shizuku.requestPermission((int)%s);"
        hashMap["color"] = "#00A9D0"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "Shizuku requestPermission requestCode %d"
        arrayList.add(hashMap)
    }

    @JvmStatic
    fun addPaletteBlocks(logicEditor: LogicEditorActivity) {
        registerLabels(logicEditor)
        logicEditor.a("Shizuku", getTitleBgColor(logicEditor))
        logicEditor.a("b", "shizukuPingBinder")
        logicEditor.a("b", "shizukuCheckPermission")
        logicEditor.a(" ", "shizukuRequestPermission")
    }

    /**
     * Label block bawaan diambil dari map xB.c berdasarkan opCode.
     * Block Shizuku tidak ada di map itu, jadi labelnya kosong.
     * Map diisi ulang setelah inisialisasi bawaan supaya tidak tertimpa.
     */
    private fun registerLabels(logicEditor: LogicEditorActivity) {
        val xb = xB.b()
        // Paksa inisialisasi tabel bawaan dulu (a(Context, String) akan mengisi map jika kosong)
        xb.a(logicEditor, "shizukuPingBinder")
        xb.c["shizukuPingBinder"] = "Shizuku pingBinder"
        xb.c["shizukuCheckPermission"] = "Shizuku checkSelfPermission"
        xb.c["shizukuRequestPermission"] = "Shizuku requestPermission requestCode %d"
    }
}