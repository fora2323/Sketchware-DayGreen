package org.sketchware.daygreen

import a.a.a.DB
import com.google.gson.reflect.TypeToken
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.GsonUtils
import java.io.File

object CustomColorManager {
    private val colorFilePath: String
        get() = FileUtil.getExternalStorageDir() + "/.sketchware/resources/color/color.json"

    /**
     * Membaca isi color.json.
     * Mengembalikan map kosong kalau file belum ada.
     * Mengembalikan null kalau file ada tapi tidak bisa dibaca (rusak),
     * supaya pemanggil tidak menganggap datanya kosong lalu menimpanya.
     */
    @Synchronized
    private fun loadDataOrNull(): MutableMap<String, String>? {
        val path = colorFilePath
        if (!FileUtil.isExistFile(path)) return HashMap()
        return try {
            val content = FileUtil.readFile(path)
            if (content.isNullOrBlank()) {
                HashMap()
            } else {
                val type = object : TypeToken<HashMap<String, String>>() {}.type
                GsonUtils.getGson().fromJson<HashMap<String, String>>(content, type) ?: HashMap()
            }
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    private fun loadData(): MutableMap<String, String> = loadDataOrNull() ?: HashMap()

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

    /**
     * Mengubah satu key lalu menyimpan.
     * Kalau file rusak, salinannya dibuat dulu supaya data lama tidak hilang.
     */
    @Synchronized
    private fun updateData(key: String, value: String) {
        val path = colorFilePath
        val data = loadDataOrNull()
        if (data == null) {
            FileUtil.copyFile(path, "$path.rusak-${System.currentTimeMillis()}")
            saveData(mapOf(key to value))
        } else {
            data[key] = value
            saveData(data)
        }
    }

    @JvmStatic
    @Synchronized
    fun getCustomColors(): String {
        return try {
            loadData()["custom_colors"].orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    @JvmStatic
    @Synchronized
    fun saveCustomColors(colors: String?) {
        updateData("custom_colors", colors ?: "")
    }

    @JvmStatic
    @Synchronized
    fun getCustomAttrs(): String {
        return try {
            loadData()["custom_attrs"].orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    @JvmStatic
    @Synchronized
    fun saveCustomAttrs(attrs: String?) {
        updateData("custom_attrs", attrs ?: "")
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
                val data = HashMap<String, String>()
                if (colors.isNotEmpty()) data["custom_colors"] = colors
                if (attrs.isNotEmpty()) data["custom_attrs"] = attrs
                saveData(data)
            }
        }
    }
}