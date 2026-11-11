package org.sketchware.daygreen.preview

import com.besome.sketch.beans.ViewBean
import org.xmlpull.v1.XmlPullParserException
import pro.sketchware.tools.ViewBeanParser
import java.io.IOException

class PreviewLayoutParser(
    private val xml: String,
    private val resolver: PreviewResourceResolver?
) {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(): ArrayList<ViewBean> {
        val parser = ViewBeanParser(xml)
        parser.setSkipRoot(false)
        parser.setResourceResolver(resolver)
        parser.setScId(resolver?.scId)

        val views = parser.parse()
        for (bean in views) {
            resolveInjectAttributes(bean)
        }
        return views
    }

    private fun resolveInjectAttributes(bean: ViewBean) {
        val res = resolver ?: return
        val inject = bean.inject
        if (inject.isNullOrBlank()) return

        val regex = Regex("""^([^=]+)="([^"]*)"$""")
        val resolvedLines = inject.lines().map { line ->
            val trimmed = line.trim()
            val match = regex.matchEntire(trimmed)
            if (match != null) {
                val key = match.groupValues[1]
                val rawValue = match.groupValues[2]

                val newValue = when {
                    rawValue.startsWith("@dimen/") -> {
                        val px = res.resolveDimenToPx(rawValue)
                        if (px >= 0) "${px}px" else rawValue
                    }
                    rawValue.startsWith("@color/") || rawValue.startsWith("?") -> {
                        val color = res.resolveColor(rawValue)
                        if (color != null) String.format("#%08X", color) else rawValue
                    }
                    rawValue.startsWith("@string/") -> {
                        res.resolveString(rawValue) ?: rawValue
                    }
                    else -> rawValue
                }
                """$key="$newValue""""
            } else {
                line
            }
        }

        bean.inject = resolvedLines.joinToString("\n").trim()
    }
}