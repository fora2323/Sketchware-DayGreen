package extensions.anbui.daydream.java.generator

import extensions.anbui.daydream.java.generator.DRJavaCodeGenerator.castToIntIfNeeded

object DRArrayListCodeGenerator {
    @JvmStatic
    fun getAtListMap(targetId: String, position: String, keyName: String): String {
        return "${targetId}.get(${castToIntIfNeeded(position)}).get(${keyName}).toString()"
    }
}