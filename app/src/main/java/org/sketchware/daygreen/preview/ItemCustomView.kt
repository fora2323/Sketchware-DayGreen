package org.sketchware.daygreen.preview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.besome.sketch.beans.LayoutBean
import com.besome.sketch.beans.ViewBean
import com.besome.sketch.editor.view.item.ItemLinearLayout

class ItemCustomView(
    context: Context,
    val realCustomView: View?,
    bean: ViewBean
) : ItemLinearLayout(context) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = 0xFF4CAF50.toInt()
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var fallbackLabel: TextView? = null

    init {
        this.bean = bean
        orientation = VERTICAL

        if (realCustomView != null) {
            setPadding(0, 0, 0, 0)
            background = null
            super.addView(realCustomView, -1, generateChildLayoutParams())
        } else {
            // Cukup satu baris text nampilin nama class nya aja, tanpa icon
            val className = bean.convert.ifEmpty { "CustomView" }.substringAfterLast('.')
            fallbackLabel = TextView(context).apply {
                text = className
                setTextColor(0xFF78909C.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setSingleLine(true)
                maxLines = 1
                gravity = Gravity.CENTER
            }
            super.addView(fallbackLabel, -1, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            })
        }
    }

    private fun generateChildLayoutParams(): LayoutParams {
        val w = if (bean.layout.width == LayoutBean.LAYOUT_WRAP_CONTENT) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
        val h = if (bean.layout.height == LayoutBean.LAYOUT_WRAP_CONTENT) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
        return LayoutParams(w, h).apply {
            val grav = bean.layout.gravity
            if (grav != LayoutBean.GRAVITY_NONE) {
                gravity = grav
            }
        }
    }

    fun updateFallbackText(className: String) {
        fallbackLabel?.text = className.ifEmpty { "CustomView" }.substringAfterLast('.')
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (realCustomView is ViewGroup && child != realCustomView) {
            try {
                val childParams = params ?: realCustomView.generateLayoutParams(null)
                realCustomView.addView(child, index, childParams)
                return
            } catch (ignored: Throwable) {
            }
        }
        super.addView(child, index, params)
    }

    override fun addView(child: View?, index: Int) {
        if (realCustomView is ViewGroup && child != realCustomView) {
            try {
                realCustomView.addView(child, index)
                return
            } catch (ignored: Throwable) {
            }
        }
        super.addView(child, index)
    }

    override fun removeView(view: View?) {
        if (realCustomView is ViewGroup && view != realCustomView) {
            realCustomView.removeView(view)
            return
        }
        super.removeView(view)
    }

    override fun removeAllViews() {
        if (realCustomView is ViewGroup) {
            realCustomView.removeAllViews()
        }
        super.removeAllViews()
        if (realCustomView != null) {
            super.addView(realCustomView, -1, generateChildLayoutParams())
        }
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
