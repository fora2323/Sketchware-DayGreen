package dev.aldi.sayuti.editor.injection

import a.a.a.jq
import pro.sketchware.utility.FileUtil
import pro.sketchware.xml.XmlBuilder

class ManifestInjection(var jq: jq?, var arr: ArrayList<*>?) {
    var path: String? = null
    var replace: String? = null
    var value: String? = null

    fun b(nx: XmlBuilder?, str: String?, str2: String?) {
        path = "${FileUtil.getExternalStorageDir()}/.sketchware/data/${jq?.sc_id}/Injection/androidmanifest/$str"
        if (FileUtil.isExistFile(path)) {
            val content = FileUtil.readFile(path)
            if (content.trim().isNotEmpty()) {
                try {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}