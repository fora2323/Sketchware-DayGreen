package org.sketchware.daygreen.preview

import a.a.a.wq
import android.graphics.Color
import android.view.ContextThemeWrapper
import com.besome.sketch.editor.manage.library.material3.Material3LibraryManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import pro.sketchware.R
import pro.sketchware.SketchApplication
import pro.sketchware.utility.FilePathUtil
import pro.sketchware.utility.ThemeUtils
import java.io.File
import java.io.FileInputStream

class PreviewResourceResolver private constructor(
    val scId: String,
    private val nightMode: Boolean
) {
    private val resourceBasePath: String = run {
        val path = try {
            FilePathUtil().getPathResource(scId)
        } catch (e: Throwable) {
            null
        }
        if (!path.isNullOrEmpty()) path else "${wq.b(scId)}/files/resource"
    }

    private val dimenCache = HashMap<String, String>()
    private val colorCache = HashMap<String, String>()
    private val stringCache = HashMap<String, String>()

    companion object {
        @JvmStatic
        fun init(scId: String, nightMode: Boolean): PreviewResourceResolver {
            val resolver = PreviewResourceResolver(scId, nightMode)
            resolver.parseValuesDir()
            return resolver
        }

        private val ATTR_COLOR_MAP: Map<String, Int> = mapOf(
            "colorPrimary" to R.attr.colorPrimary,
            "colorOnPrimary" to R.attr.colorOnPrimary,
            "colorPrimaryContainer" to R.attr.colorPrimaryContainer,
            "colorOnPrimaryContainer" to R.attr.colorOnPrimaryContainer,
            "colorSecondary" to R.attr.colorSecondary,
            "colorOnSecondary" to R.attr.colorOnSecondary,
            "colorSecondaryContainer" to R.attr.colorSecondaryContainer,
            "colorOnSecondaryContainer" to R.attr.colorOnSecondaryContainer,
            "colorTertiary" to R.attr.colorTertiary,
            "colorOnTertiary" to R.attr.colorOnTertiary,
            "colorTertiaryContainer" to R.attr.colorTertiaryContainer,
            "colorOnTertiaryContainer" to R.attr.colorOnTertiaryContainer,
            "colorError" to R.attr.colorError,
            "colorOnError" to R.attr.colorOnError,
            "colorErrorContainer" to R.attr.colorErrorContainer,
            "colorOnErrorContainer" to R.attr.colorOnErrorContainer,
            "colorSurface" to R.attr.colorSurface,
            "colorOnSurface" to R.attr.colorOnSurface,
            "colorSurfaceVariant" to R.attr.colorSurfaceVariant,
            "colorOnSurfaceVariant" to R.attr.colorOnSurfaceVariant,
            "colorOutline" to R.attr.colorOutline,
            "colorBackground" to android.R.attr.colorBackground,
            "colorOnBackground" to R.attr.colorOnBackground,
            "colorControlNormal" to R.attr.colorControlNormal,
            "colorControlActivated" to R.attr.colorControlActivated,
            "colorControlHighlight" to R.attr.colorControlHighlight
        )
    }

    private fun parseValuesDir() {
        parseValuesSubdir("values")
        if (nightMode) {
            parseValuesSubdir("values-night", colorsOnly = true)
        }
    }

    private fun parseValuesSubdir(dirName: String, colorsOnly: Boolean = false) {
        try {
            val valuesDir = File(resourceBasePath, dirName)
            if (!valuesDir.exists() || !valuesDir.isDirectory) return

            val xmlFiles = valuesDir.listFiles { file ->
                file.isFile && file.name.endsWith(".xml", ignoreCase = true)
            } ?: return

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false

            for (file in xmlFiles) {
                try {
                    FileInputStream(file).use { inputStream ->
                        val parser = factory.newPullParser()
                        parser.setInput(inputStream, "UTF-8")

                        var eventType = parser.eventType
                        var currentTag: String? = null
                        var currentName: String? = null

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    currentTag = parser.name
                                    if (currentTag == "dimen" || currentTag == "color" || currentTag == "string") {
                                        currentName = parser.getAttributeValue(null, "name")
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    val text = parser.text?.trim()
                                    val name = currentName
                                    if (!name.isNullOrEmpty() && !text.isNullOrEmpty()) {
                                        when (currentTag) {
                                            "color" -> colorCache[name] = text
                                            "dimen" -> if (!colorsOnly) dimenCache[name] = text
                                            "string" -> if (!colorsOnly) stringCache[name] = text
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    currentTag = null
                                    currentName = null
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    fun resolveDimenToDp(value: String?): Float? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()

        val rawDimen = if (trimmed.startsWith("@dimen/")) {
            val key = trimmed.removePrefix("@dimen/")
            dimenCache[key] ?: return null
        } else {
            trimmed
        }

        val resolvedStr = if (rawDimen.startsWith("@dimen/")) {
            dimenCache[rawDimen.removePrefix("@dimen/")] ?: return null
        } else {
            rawDimen
        }

        val metrics = SketchApplication.getContext().resources.displayMetrics
        val density = if (metrics.density > 0f) metrics.density else 1f
        val scaledDensity = if (metrics.scaledDensity > 0f) metrics.scaledDensity else density

        return try {
            when {
                resolvedStr.endsWith("dip", ignoreCase = true) ->
                    resolvedStr.dropLast(3).trim().toFloat()
                resolvedStr.endsWith("dp", ignoreCase = true) ->
                    resolvedStr.dropLast(2).trim().toFloat()
                resolvedStr.endsWith("sp", ignoreCase = true) ->
                    (resolvedStr.dropLast(2).trim().toFloat() * scaledDensity) / density
                resolvedStr.endsWith("px", ignoreCase = true) ->
                    resolvedStr.dropLast(2).trim().toFloat() / density
                else -> resolvedStr.toFloatOrNull()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun resolveDimenToPx(value: String?): Int {
        if (value.isNullOrBlank()) return -1
        val trimmed = value.trim()

        val rawDimen = if (trimmed.startsWith("@dimen/")) {
            val key = trimmed.removePrefix("@dimen/")
            dimenCache[key] ?: return -1
        } else {
            trimmed
        }

        val resolvedStr = if (rawDimen.startsWith("@dimen/")) {
            dimenCache[rawDimen.removePrefix("@dimen/")] ?: return -1
        } else {
            rawDimen
        }

        val metrics = SketchApplication.getContext().resources.displayMetrics
        val density = metrics.density
        val scaledDensity = metrics.scaledDensity

        return try {
            when {
                resolvedStr.endsWith("dip", ignoreCase = true) ->
                    Math.round(resolvedStr.dropLast(3).trim().toFloat() * density)
                resolvedStr.endsWith("dp", ignoreCase = true) ->
                    Math.round(resolvedStr.dropLast(2).trim().toFloat() * density)
                resolvedStr.endsWith("sp", ignoreCase = true) ->
                    Math.round(resolvedStr.dropLast(2).trim().toFloat() * scaledDensity)
                resolvedStr.endsWith("px", ignoreCase = true) ->
                    Math.round(resolvedStr.dropLast(2).trim().toFloat())
                else -> -1
            }
        } catch (_: NumberFormatException) {
            -1
        }
    }

    fun resolveColor(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()

        try {
            if (trimmed.startsWith("#")) {
                return Color.parseColor(trimmed)
            }

            if (trimmed.startsWith("@color/")) {
                var colorRef = trimmed
                var depth = 0
                while (colorRef.startsWith("@color/") && depth < 5) {
                    val key = colorRef.removePrefix("@color/")
                    colorRef = colorCache[key] ?: return null
                    depth++
                }
                if (colorRef.startsWith("#")) {
                    return Color.parseColor(colorRef)
                }
            }

            if (trimmed.startsWith("?attr/") || trimmed.startsWith("?")) {
                val attrName = trimmed
                    .removePrefix("?attr/")
                    .removePrefix("?")
                    .substringAfterLast(":")

                val attrResId = ATTR_COLOR_MAP[attrName] ?: return null
                val context = SketchApplication.getContext()
                val m3 = Material3LibraryManager(context, scId)
                val themed = ContextThemeWrapper(context, m3.viewEditorThemeOverlay)
                return ThemeUtils.getColor(themed, attrResId)
            }
        } catch (_: Exception) {
            return null
        }

        return null
    }

    fun resolveString(value: String?): String? {
        if (value == null) return null
        if (value.startsWith("@string/")) {
            val key = value.removePrefix("@string/")
            return stringCache[key] ?: value
        }
        return value
    }

    fun resolveDrawable(name: String): File? {
        val cleanName = name.removePrefix("@drawable/").trim()
        if (cleanName.isEmpty()) return null

        val folders = mutableListOf<String>()
        if (nightMode) {
            folders.add("drawable-night")
            folders.add("drawable-night-xhdpi")
            folders.add("drawable-night-xxhdpi")
        }
        folders.add("drawable")
        folders.add("drawable-xhdpi")
        folders.add("drawable-xxhdpi")

        val extensions = listOf(".xml", ".png", ".jpg", ".webp", ".svg", ".9.png")

        for (folder in folders) {
            val dir = File(resourceBasePath, folder)
            if (!dir.exists() || !dir.isDirectory) continue
            for (ext in extensions) {
                val candidate = File(dir, "$cleanName$ext")
                if (candidate.exists() && candidate.isFile) {
                    return candidate
                }
            }
        }
        return null
    }
}