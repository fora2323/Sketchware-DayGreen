package org.sketchware.daygreen.blocks

import com.besome.sketch.editor.LogicEditorActivity
import pro.sketchware.R
import pro.sketchware.utility.ThemeUtils

object IDOperator {

    /*
     * Definisi block baru (hashMap) didaftarkan di sini.
     * Contoh:
     *   val hashMap = HashMap<String, Any>()
     *   hashMap["name"] = "contohBlock"
     *   hashMap["type"] = "b"
     *   hashMap["typeName"] = ""
     *   hashMap["code"] = "%s == %s"
     *   hashMap["color"] = "#5cb722"
     *   hashMap["palette"] = "-1"
     *   hashMap["spec"] = "%s sama dengan %s"
     *   arrayList.add(hashMap)
     *
     * Saat ini kosong. Kalau mulai dipakai, panggil dari DRBlocksHandler.
     */
    @JvmStatic
    fun addBlocks(arrayList: ArrayList<HashMap<String, Any>>) {
        // kosong
    }

    /*
     * Palette Operator. Dipanggil dari BlocksHandler.primaryBlocksD
     * tepat setelah block "=".
     *
     * Format: logicEditor.a("<tipe>", "<nama block>")
     * Tipe: " " = command, "b" = boolean, "d" = number, "s" = string,
     *       "c" = C-block, "f" = return/final
     * Header baru: logicEditor.a("Judul Header", getTitleBgColor(logicEditor))
     */
    @JvmStatic
    fun addPaletteBlocks(logicEditor: LogicEditorActivity) {
        logicEditor.a("add source directly", getTitleBgColor(logicEditor))
        logicEditor.a(" ", "addSourceDirectly")
        logicEditor.a("b", "asdBoolean")
        logicEditor.a("d", "asdNumber")
        logicEditor.a("s", "asdString")
        logicEditor.a("c", "addSourceDirectlyIf")

        // Contoh block baru (aktifkan setelah definisinya ada di addBlocks):
        // logicEditor.a("b", "contohBlockBoolean")   // contoh 1
        // logicEditor.a("s", "contohBlockString")    // contoh 2
    }

    private fun getTitleBgColor(logicEditor: LogicEditorActivity): Int {
        return ThemeUtils.getColor(
            logicEditor,
            if (ThemeUtils.isDarkThemeEnabled(logicEditor)) R.attr.colorSurfaceContainerHigh
            else R.attr.colorSurfaceInverse
        )
    }
}