package extensions.anbui.daydream.xml

import com.besome.sketch.beans.ViewBean
import pro.sketchware.utility.InjectAttributeHandler
import pro.sketchware.xml.XmlBuilder

object DRXmlLayoutCreator {
    @JvmStatic
    fun addPadding(xmlBuilder: XmlBuilder, viewBean: ViewBean, toNotAdd: MutableSet<String>) {
        val injectHandler = InjectAttributeHandler(viewBean)
        val layoutBean = viewBean.layout
        val paddingLeft = layoutBean.paddingLeft
        val paddingTop = layoutBean.paddingTop
        val paddingRight = layoutBean.paddingRight
        val paddingBottom = layoutBean.paddingBottom

        if (!isNeedWrite(paddingLeft, paddingTop, paddingRight, paddingBottom)) return

        if (isCombineAll(toNotAdd, injectHandler, true, paddingLeft, paddingTop, paddingRight, paddingBottom, "android:padding", "padding")) {
            xmlBuilder.addAttribute("android", "padding", paddingLeft.dp())
            return
        }

        var isWriteLeft = isAllowWrite(toNotAdd, injectHandler, true, paddingLeft, "android:paddingLeft", "paddingLeft")
        var isWriteTop = isAllowWrite(toNotAdd, injectHandler, true, paddingTop, "android:paddingTop", "paddingTop")
        var isWriteRight = isAllowWrite(toNotAdd, injectHandler, true, paddingRight, "android:paddingRight", "paddingRight")
        var isWriteBottom = isAllowWrite(toNotAdd, injectHandler, true, paddingBottom, "android:paddingBottom", "paddingBottom")


        if (isCombine(paddingLeft, paddingRight) && isWriteLeft && isWriteRight) {
            xmlBuilder.addAttribute("android", "paddingHorizontal", paddingLeft.dp())
            isWriteLeft = false
            isWriteRight = false
        }

        if (isCombine(paddingTop, paddingBottom) && isWriteTop && isWriteBottom) {
            xmlBuilder.addAttribute("android", "paddingVertical", paddingTop.dp())
            isWriteTop = false
            isWriteBottom = false
        }

        if (isWriteLeft) {
            xmlBuilder.addAttribute("android", if (isUseStartEnd(viewBean)) "paddingStart" else "paddingLeft", paddingLeft.dp())
        }
        if (isWriteTop) {
            xmlBuilder.addAttribute("android", "paddingTop", paddingTop.dp())
        }
        if (isWriteRight) {
            xmlBuilder.addAttribute("android", if (isUseStartEnd(viewBean)) "paddingEnd" else "paddingRight", paddingRight.dp())
        }
        if (isWriteBottom) {
            xmlBuilder.addAttribute("android", "paddingBottom", paddingBottom.dp())
        }
    }

    @JvmStatic
    fun isNeedWrite(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return listOf(left, top, right, bottom).any { it != 0 }
    }

    @JvmStatic
    fun isAllowWrite(toNotAdd: MutableSet<String>, injectHandler: InjectAttributeHandler, isForcePositiveValue: Boolean, value: Int, qualifiedAttribute: String, attributeName: String): Boolean {
        return (if (isForcePositiveValue) value > 0 else true) && !toNotAdd.contains(qualifiedAttribute) && !injectHandler.contains(attributeName)
    }

    @JvmStatic
    fun isCombineAll(toNotAdd: MutableSet<String>, injectHandler: InjectAttributeHandler, isForcePositiveValue: Boolean, left: Int, top: Int, right: Int, bottom: Int, qualifiedAttribute: String, attributeName: String): Boolean {
        return (if (isForcePositiveValue) left > 0 else true) && left == top && left == right && left == bottom && !toNotAdd.contains(qualifiedAttribute) && !injectHandler.contains(attributeName)
    }

    @JvmStatic
    fun isCombine(value1: Int, value2: Int): Boolean {
        return value1 == value2
    }

    @JvmStatic
    fun isUseStartEnd(viewBean: ViewBean): Boolean {
        return listOf(
            ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW,
            ViewBean.VIEW_TYPE_WIDGET_EDITTEXT,
            ViewBean.VIEW_TYPE_WIDGET_BUTTON,
            ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW
        ).any { it == viewBean.type }
    }

    fun Int.dp() = "${this}dp"
}