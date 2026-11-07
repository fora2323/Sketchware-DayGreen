package org.sketchware.daygreen.preview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.besome.sketch.beans.ViewBean
import com.besome.sketch.editor.view.item.ItemLinearLayout
import pro.sketchware.R

class ItemCustomView(
    context: Context,
    val realCustomView: View?,
    bean: ViewBean
) : ItemLinearLayout(context) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = 0xFF4CAF50.toInt() // Green accent
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    init {
        this.bean = bean
        orientation = VERTICAL

        if (realCustomView != null) {
            // Live rendered custom view from compiled class
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            super.addView(realCustomView, -1, lp)
        } else {
            // Android Studio style placeholder when not compiled yet
            setupAndroidStudioPlaceholder(context, bean)
        }
    }

    private fun setupAndroidStudioPlaceholder(context: Context, bean: ViewBean) {
        gravity = Gravity.CENTER
        val density = resources.displayMetrics.density
        setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
        minimumWidth = (120 * density).toInt()
        minimumHeight = (56 * density).toInt()

        // Background placeholder with dashed border and subtle blueprint tint
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6 * density
            setColor(0x0C4CAF50) // Subtle green/teal tint (5% opacity)
            setStroke((1.5f * density).toInt(), 0xFF78909C.toInt(), 8 * density, 6 * density)
        }

        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        // Custom view widget icon
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_mtrl_preview)
            setColorFilter(0xFF00897B.toInt())
            val size = (22 * density).toInt()
            layoutParams = LayoutParams(size, size).apply {
                marginEnd = (8 * density).toInt()
            }
        }
        container.addView(icon)

        // Text labels (Class name + compile status hint)
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val className = bean.convert.ifEmpty { "CustomView" }.substringAfterLast('.')
        val title = TextView(context).apply {
            text = "<$className>"
            setTextColor(0xFF37474F.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = android.graphics.Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }
        textContainer.addView(title)

        val hint = TextView(context).apply {
            text = "Compile / run project to preview"
            setTextColor(0xFF90A4AE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        }
        textContainer.addView(hint)

        container.addView(textContainer)
        super.addView(container, -1, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })
    }

    override fun addView(child: View?, index: Int) {
        if (realCustomView is ViewGroup) {
            try {
                realCustomView.addView(child, index)
                return
            } catch (ignored: Throwable) {
            }
        }
        super.addView(child, index)
    }

    override fun dispatchDraw(canvas: Canvas) {
        try {
            super.dispatchDraw(canvas)
        } catch (t: Throwable) {
            val errorPaint = Paint().apply {
                color = Color.RED
                textSize = 24f
            }
            canvas.drawText("Preview error: ${t.message}", 10f, 40f, errorPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (realCustomView != null && isSelected) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        }
    }
}
