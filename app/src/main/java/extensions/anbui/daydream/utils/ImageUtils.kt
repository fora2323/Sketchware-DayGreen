package extensions.anbui.daydream.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.widget.ImageView
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap


object ImageUtils {
    @JvmStatic
    fun invertColor(imageView : ImageView) {
        val negativeMatrix = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f,  // Red
            0f, -1.0f, 0f, 0f, 255f,  // Green
            0f, 0f, -1.0f, 0f, 255f,  // Blue
            0f, 0f, 0f, 1.0f, 0f // Alpha
        )

        val colorMatrix = ColorMatrix(negativeMatrix)
        val filter = ColorMatrixColorFilter(colorMatrix)

        imageView.colorFilter = filter
    }

    @JvmStatic
    fun invertColor(srcFilePath: String?, destFilePath: String): Boolean {
        val originalBitmap: Bitmap = BitmapFactory.decodeFile(srcFilePath) ?: return false

        val invertedBitmap: Bitmap? = originalBitmap.getConfig()?.let {
            createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), it)
        }

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                -1.0f, 0f, 0f, 0f, 255f,  // red
                0f, -1.0f, 0f, 0f, 255f,  // green
                0f, 0f, -1.0f, 0f, 255f,  // blue
                0f, 0f, 0f, 1.0f, 0f // alpha
            )
        )

        val canvas = invertedBitmap?.let { Canvas(it) }
        val paint = Paint()
        paint.setColorFilter(ColorMatrixColorFilter(colorMatrix))
        canvas?.drawBitmap(originalBitmap, 0f, 0f, paint)

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(destFilePath)
            invertedBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            originalBitmap.recycle()
            invertedBitmap?.recycle()
        }
        return false
    }

    @JvmStatic
    fun colorizeToSingleColor(color: Int, srcFilePath: String?, destFilePath: String): Boolean {
        val originalBitmap: Bitmap = BitmapFactory.decodeFile(srcFilePath) ?: return false

        val invertedBitmap: Bitmap? = originalBitmap.getConfig()?.let {
            createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), it)
        }

        val r = Color.red(color).toFloat()
        val g = Color.green(color).toFloat()
        val b = Color.blue(color).toFloat()

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                0f, 0f, 0f, 0f, r,
                0f, 0f, 0f, 0f, g,
                0f, 0f, 0f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val canvas = invertedBitmap?.let { Canvas(it) }
        val paint = Paint()
        paint.setColorFilter(ColorMatrixColorFilter(colorMatrix))
        canvas?.drawBitmap(originalBitmap, 0f, 0f, paint)

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(destFilePath)
            invertedBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            originalBitmap.recycle()
            invertedBitmap?.recycle()
        }
        return false
    }
}