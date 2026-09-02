package extensions.anbui.daydream.json

import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonParser
import extensions.anbui.daydream.configs.Configs

object JsonUtils {
    var TAG: String = Configs.universalTAG + "JsonUtils"
    @JvmStatic
    fun covertoMap(json: String?): MutableMap<String?, Any?>? {
        Log.i(TAG, "covertoMap: $json")
        val gson = Gson()
        val type = object : TypeToken<MutableMap<String?, Any?>?>() {}.type
        return gson.fromJson<MutableMap<String?, Any?>?>(json, type)
    }

    fun isValidData(content: String): Boolean {
        Log.i(TAG, "isValidData: $content")
        try {
            val element = JsonParser.parseString(content)
            return element != null
        } catch (_: Exception) {
            return false
        }
    }

    fun isListMapEmpty(content: String?): Boolean {
        Log.i(TAG, "isListMapEmpty: $content")
        try {
            val gson = Gson()
            val type = object : TypeToken<MutableList<MutableMap<String?, Any?>?>?>() {}.type
            val list = gson.fromJson<MutableList<MutableMap<String?, Any?>?>?>(content, type)

            return list == null || list.isEmpty()
        } catch (_: Exception) {
            return true
        }
    }
}
