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
            // Tampilan fallback jika belum dicompile atau gagal instansiasi
            gravity = Gravity.CENTER
            val className = bean.convert.ifEmpty { "CustomView" }.substringAfterLast('.')
            fallbackLabel = TextView(context).apply {
                text = className
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setSingleLine(true)
                maxLines = 1
                gravity = Gravity.CENTER
            }
            val lp = LayoutParams(
                if (bean.layout.width == LayoutBean.LAYOUT_WRAP_CONTENT) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT,
                if (bean.layout.height == LayoutBean.LAYOUT_WRAP_CONTENT) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
            super.addView(fallbackLabel, -1, lp)

            // Tampilkan status/error di bawah label fallback dengan indikasi warna sesuai jenis error
            if (creationError != null) {
                val isNotFound = creationError is ClassNotFoundException ||
                        creationError.cause is ClassNotFoundException ||
                        (creationError is IllegalStateException && creationError.message?.contains("Compile", ignoreCase = true) == true)

                val errorMsg = if (isNotFound) {
                    "⚠ Belum di-compile"
                } else {
                    creationError.cause?.message ?: creationError.message ?: creationError.javaClass.simpleName
                }

                val errorColor = if (isNotFound) Color.parseColor("#FFA000") else Color.parseColor("#FF5252")

                val errorLabel = TextView(context).apply {
                    text = errorMsg
                    setTextColor(errorColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    gravity = Gravity.CENTER
                    maxLines = 3
                }
                super.addView(errorLabel, -1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (4 * resources.displayMetrics.density).toInt()
                })
            }
        }
    }

    private fun generateChildLayoutParams(): LayoutParams {
        val density = resources.displayMetrics.density
        fun dimenToPx(dimen: Int): Int {
            return when (dimen) {
                LayoutBean.LAYOUT_MATCH_PARENT -> LayoutParams.MATCH_PARENT
                LayoutBean.LAYOUT_WRAP_CONTENT -> LayoutParams.WRAP_CONTENT
                else -> (dimen * density).toInt()
            }
        }

        val w = dimenToPx(bean.layout.width)
        val h = dimenToPx(bean.layout.height)

        return LayoutParams(w, h).apply {
            val grav = bean.layout.layoutGravity
            if (grav != LayoutBean.GRAVITY_NONE) {
                gravity = grav
            }
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Guard: jika child memiliki ukuran dimensi tetap (> 0), gunakan langsung dan hindari pengukuran ganda
        if (realCustomView != null) {
            val childLp = realCustomView.layoutParams
            if (childLp != null && childLp.width > 0 && childLp.height > 0) {
                try {
                    realCustomView.measure(
                        MeasureSpec.makeMeasureSpec(childLp.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(childLp.height, MeasureSpec.EXACTLY)
                    )
                    setMeasuredDimension(
                        resolveSize(childLp.width + paddingLeft + paddingRight, widthMeasureSpec),
                        resolveSize(childLp.height + paddingTop + paddingBottom, heightMeasureSpec)
                    )
                    return
                } catch (t: Throwable) {
                    previewError = t
                    setMeasuredDimension(
                        getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                        getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
                    )
                    return
                }
            }
        }

        var lp: LayoutParams? = null
        var lm = 0
        var tm = 0
        var rm = 0
        var bm = 0
        if (realCustomView != null) {
            lp = realCustomView.layoutParams as? LayoutParams
            if (lp != null) {
                lm = lp.leftMargin
                tm = lp.topMargin
                rm = lp.rightMargin
                bm = lp.bottomMargin
                // Sembunyikan margin dari LinearLayout (ItemCustomView) agar ukurannya tidak menggelembung 2x lipat
                lp.setMargins(0, 0, 0, 0)
            }
        }
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } catch (t: Throwable) {
            previewError = t
            setMeasuredDimension(
                getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
            )
        } finally {
            // Selalu kembalikan margin asli agar custom view tetap dapat membacanya dari layout params
            lp?.setMargins(lm, tm, rm, bm)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        try {
            super.onLayout(changed, l, t, r, b)
            // Hanya paksa fill jika view belum mengisi penuh wrapper agar tidak mengganggu layout internal child
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
            val p = Paint().apply { color = Color.parseColor("#44FF0000"); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
            val textPaint = Paint().apply { color = Color.WHITE; textSize = 24f; isAntiAlias = true }
            val errorMsg = previewError?.message ?: previewError?.javaClass?.simpleName ?: "Render Error"
            canvas.drawText(errorMsg, 10f, 40f, textPaint)
        }

        if (isStale) {
            val p = Paint().apply { color = Color.parseColor("#FFA500"); isAntiAlias = true; style = Paint.Style.FILL }
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