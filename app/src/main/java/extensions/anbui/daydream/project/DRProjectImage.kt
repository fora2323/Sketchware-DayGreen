package extensions.anbui.daydream.project

import extensions.anbui.daydream.utils.ImageUtils

object DRProjectImage {
    @JvmStatic
    fun invertColor(imageFilePath : String) {
        ImageUtils.invertColor(imageFilePath, imageFilePath)
    }

    @JvmStatic
    fun fillColor(color : Int, imageFilePath : String) {
        ImageUtils.colorizeToSingleColor(color, imageFilePath, imageFilePath)
    }
}