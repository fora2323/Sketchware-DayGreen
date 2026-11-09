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

class ItemCustomView @JvmOverloads constructor(
    context: Context,
    val realCustomView: View?,
    bean: ViewBean,
    private val creationError: Throwable? = null,
    private val scId: String? = null,
    private val isStale: Boolean = false
) : ItemLinearLayout(context) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = 0xFF4CAF50.toInt()
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var fallbackLabel: TextView? = null
    private var previewError: Throwable? = null

    init {
        this.bean = bean
        orientation = VERTICAL

        if (realCustomView != null) {
            setPadding(0, 0, 0, 0)
            background = null
            super.addView(realCustomView, -1, generateChildLayoutParams())
        } else {
            gravity = Gravity.CENTER

            val className = bean.convert.ifEmpty { "CustomView" }.substringAfterLast('.')

            // Tentukan warna & ikon berdasarkan tipe error
            val (labelColor, bgColor, icon) = when {
                creationError == null -> Triple(Color.WHITE, 0xFF4CAF50.toInt(), "")
                creationError is ClassNotFoundException ||
                creationError?.cause is ClassNotFoundException ->
                    Triple(Color.parseColor("#FFD600"), Color.parseColor("#33FFD600"), "⚠ ")
                else ->
                    Triple(Color.parseColor("#FF5252"), Color.parseColor("#33FF5252"), "✕ ")
            }

            setBackgroundColor(bgColor)

            fallbackLabel = TextView(context).apply {
                text = "$icon$className"
                setTextColor(labelColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setSingleLine(true)
                maxLines = 1
                gravity = Gravity.CENTER
            }

            val lp = LayoutParams(
                if (bean.layout.width == LayoutBean.LAYOUT_WRAP_CONTENT)
                    LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT,
                if (bean.layout.height == LayoutBean.LAYOUT_WRAP_CONTENT)
                    LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }

            super.addView(fallbackLabel, -1, lp)

            // Tampilkan pesan error singkat di bawah nama class
            if (creationError != null) {
                val isNotCompiled = creationError is ClassNotFoundException ||
                        creationError?.cause is ClassNotFoundException ||
                        creationError is IllegalStateException

                val errorMsg = if (isNotCompiled) {
                    "Belum di-compile"
                } else {
                    creationError.cause?.javaClass?.simpleName
                        ?: creationError.javaClass.simpleName
                }

                val errorLabel = TextView(context).apply {
                    text = errorMsg
                    setTextColor(labelColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    gravity = Gravity.CENTER
                    maxLines = 2
                    alpha = 0.75f
                }
                super.addView(
                    errorLabel, -1,
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        topMargin = (4 * resources.displayMetrics.density).toInt()
                    }
                )
            }
        }
    }

    private fun generateChildLayoutParams(): LayoutParams {
        val density = resources.displayMetrics.density
        fun dimenToPx(dimen: Int): Int = when (dimen) {
            LayoutBean.LAYOUT_MATCH_PARENT -> LayoutParams.MATCH_PARENT
            LayoutBean.LAYOUT_WRAP_CONTENT -> LayoutParams.WRAP_CONTENT
            else -> (dimen * density).toInt()
        }

        return LayoutParams(dimenToPx(bean.layout.width), dimenToPx(bean.layout.height)).apply {
            val grav = bean.layout.layoutGravity
            if (grav != LayoutBean.GRAVITY_NONE) gravity = grav
            weight = bean.layout.weight.toFloat()
            leftMargin = (bean.layout.marginLeft * density).toInt()
            topMargin = (bean.layout.marginTop * density).toInt()
            rightMargin = (bean.layout.marginRight * density).toInt()
            bottomMargin = (bean.layout.marginBottom * density).toInt()
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
            } catch (ignored: Throwable) {}
        }
        super.addView(child, index, params)
    }

    override fun addView(child: View?, index: Int) {
        if (realCustomView is ViewGroup && child != realCustomView) {
            try {
                realCustomView.addView(child, index)
                return
            } catch (ignored: Throwable) {}
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
        if (realCustomView is ViewGroup) realCustomView.removeAllViews()
        super.removeAllViews()
        if (realCustomView != null) {
            super.addView(realCustomView, -1, generateChildLayoutParams())
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Simpan & sembunyikan margin sebelum measure supaya tidak dihitung 2x oleh LinearLayout.
        // Pakai variabel lokal — tidak menyentuh state di luar try/finally.
        val lp = realCustomView?.layoutParams as? LayoutParams
        val savedMargins = lp?.let {
            intArrayOf(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
        }
        try {
            lp?.setMargins(0, 0, 0, 0)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } catch (t: Throwable) {
            previewError = t
            setMeasuredDimension(
                getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
            )
        } finally {
            // Restore margin SELALU, bahkan kalau ada exception
            savedMargins?.let { m ->
                lp?.setMargins(m[0], m[1], m[2], m[3])
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        try {
            super.onLayout(changed, l, t, r, b)
            // Paksa realCustomView mengisi wrapper hanya kalau ukurannya belum pas.
            // Ini mencegah override onLayout() milik custom view jadi kacau.
            realCustomView?.let { v ->
                val expectedW = r - l
                val expectedH = b - t
                if (v.width != expectedW || v.height != expectedH) {
                    v.layout(0, 0, expectedW, expectedH)
                }
            }
        } catch (e: Throwable) {
            previewError = e
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        try {
            super.dispatchDraw(canvas)
        } catch (t: Throwable) {
            previewError = t
        }

        if (previewError != null) {
            val bgPaint = Paint().apply {
                color = Color.parseColor("#44FF0000")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                isAntiAlias = true
            }
            val msg = previewError?.javaClass?.simpleName ?: "Render Error"
            canvas.drawText(msg, 10f, 40f, textPaint)
        }

        // Dot oranye kecil di sudut kanan atas kalau source lebih baru dari build
        if (isStale) {
            val p = Paint().apply {
                color = Color.parseColor("#FFA500")
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            canvas.drawCircle(width - 15f, 15f, 10f, p)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (realCustomView != null && isSelected) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        }
    }
}