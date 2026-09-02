package extensions.anbui.daydream.java.generator

import extensions.anbui.daydream.java.generator.DRJavaCodeGenerator.castToIntIfNeeded

object DRObjectAnimatorCodeGenerator {
    @JvmStatic
    fun objectanimatorSetDuration(targetId: String, duration: String): String {
        return "${targetId}.setDuration(${castToIntIfNeeded(duration)});"
    }
}