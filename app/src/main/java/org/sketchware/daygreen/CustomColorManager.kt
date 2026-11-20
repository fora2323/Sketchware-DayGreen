package org.sketchware.daygreen

import a.a.a.DB
import com.google.gson.reflect.TypeToken
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.GsonUtils
import java.io.File

object CustomColorManager {
    private val colorFilePath: String
        get() = FileUtil.getExternalStorageDir() + "/.sketchware/resources/color/color.json"

    @Synchronized
    private fun loadData(): MutableMap<String, String> {
        val path = colorFilePath
        if (FileUtil.isExistFile(path)) {
            return try {
                val content = FileUtil.readFile(path)
                if (content.isNullOrBlank()) {
                    HashMap()
                } else {
                    val type = object : TypeToken<HashMap<String, String>>() {}.type
                    GsonUtils.getGson().fromJson<HashMap<String, String>>(content, type) ?: HashMap()
                }
            } catch (_: Exception) {
                HashMap()
            }
        }
        return HashMap()
    }

    @Synchronized
    private fun saveData(data: Map<String, String>) {
        val path = colorFilePath
        val file = File(path)
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        FileUtil.writeFile(path, GsonUtils.getGson().toJson(data))
    }

    @JvmStatic
    @Synchronized
    fun getCustomColors(): String {
        return loadData()["custom_colors"] ?: ""
    }

    @JvmStatic
    @Synchronized
    fun saveCustomColors(colors: String?) {
        val data = loadData()
        data["custom_colors"] = colors ?: ""
        saveData(data)
    }

    @JvmStatic
    @Synchronized
    fun getCustomAttrs(): String {
        return loadData()["custom_attrs"] ?: ""
    }

    @JvmStatic
    @Synchronized
    fun saveCustomAttrs(attrs: String?) {
        val data = loadData()
        data["custom_attrs"] = attrs ?: ""
        saveData(data)
    }

    @JvmStatic
    @Synchronized
    fun clearAll() {
        FileUtil.deleteFile(colorFilePath)
    }

    @JvmStatic
    @Synchronized
    fun migrateIfNecessary(colorPref: DB?) {
        val path = colorFilePath
        if (!FileUtil.isExistFile(path) && (colorPref != null)) {
            val colors = colorPref.f("P24I1") ?: ""
            val attrs = colorPref.f("P24I2") ?: ""
            if (colors.isNotEmpty() || attrs.isNotEmpty()) {
                val data = loadData()
                if (colors.isNotEmpty()) data["custom_colors"] = colors
                if (attrs.isNotEmpty()) data["custom_attrs"] = attrs
                saveData(data)
            }
        }
    }
}
