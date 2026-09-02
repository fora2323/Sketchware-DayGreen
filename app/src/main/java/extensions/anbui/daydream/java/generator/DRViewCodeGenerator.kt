package extensions.anbui.daydream.java.generator

object DRViewCodeGenerator {
    @JvmStatic
    fun setAlpha(viewId: String, value: String): String {
        return "$viewId.setAlpha(${DRJavaCodeGenerator.castToFloatIfNeeded(value)});"
    }

    @JvmStatic
    fun seekBarSetProgress(viewId: String, value: String): String {
        return "$viewId.setProgress(${DRJavaCodeGenerator.castToIntIfNeeded(value)});"
    }
}