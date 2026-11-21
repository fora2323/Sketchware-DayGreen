package org.sketchware.daygreen.blocks

import a.a.a.xB
import android.content.Context
import com.besome.sketch.editor.LogicEditorActivity
import pro.sketchware.R
import pro.sketchware.SketchApplication
import pro.sketchware.utility.ThemeUtils

object IDBlockFirebase {

    private fun getTitleBgColor(logicEditor: LogicEditorActivity): Int {
        return ThemeUtils.getColor(logicEditor, if (ThemeUtils.isDarkThemeEnabled(logicEditor)) R.attr.colorSurfaceContainerHigh else R.attr.colorSurfaceInverse)
    }

    @JvmStatic
    fun addBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        registerLabels(SketchApplication.getContext())

        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "firebaseSetValue"
        hashMap["type"] = " "
        hashMap["code"] = "%s.child(%s).setValue(%s);"
        hashMap["color"] = "#212121"
        hashMap["palette"] = "-1"
        hashMap["spec"] = "%m.firebase add key %s value %s"
        arrayList.add(hashMap)
    }

    /**
     * Palette Firebase DB. Urutan di sini adalah urutan tampil di logic editor.
     * Untuk menambah block baru, cukup tambah satu baris logicEditor.a(...) di sini.
     */
    @JvmStatic
    fun addPaletteBlocks(logicEditor: LogicEditorActivity) {
        registerLabels(logicEditor)
        logicEditor.a("Firebase", getTitleBgColor(logicEditor))
        logicEditor.a(" ", "firebaseAdd")
        logicEditor.a(" ", "firebaseSetValue")
        logicEditor.a(" ", "firebasePush")
        logicEditor.a("s", "firebaseGetPushKey")
        logicEditor.a(" ", "firebaseDelete")
        logicEditor.a("c", "firebaseGetChildren")
        logicEditor.a(" ", "firebaseStartListen")
        logicEditor.a(" ", "firebaseStopListen")
    }

    @JvmStatic
    fun registerLabels(context: Context) {
        val xb = xB.b()
        xb.a(context, "firebaseSetValue")
        xb.c["firebaseSetValue"] = "%m.firebase add key %s value %s"
    }
}