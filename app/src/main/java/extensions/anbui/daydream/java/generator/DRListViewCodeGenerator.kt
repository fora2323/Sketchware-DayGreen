package extensions.anbui.daydream.java.generator

import extensions.anbui.daydream.java.generator.DRJavaCodeGenerator.castToIntIfNeeded

object DRListViewCodeGenerator {
    @JvmStatic
    fun listSmoothScrollTo(targetId: String, position: String): String {
        return "${targetId}.smoothScrollToPosition(${castToIntIfNeeded(position)});"
    }
}