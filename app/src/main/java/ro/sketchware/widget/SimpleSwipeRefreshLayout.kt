package ro.sketchware

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ListView

import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px

import kotlin.math.min

import com.google.android.material.loadingindicator.LoadingIndicator

class SimpleSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    fun interface OnRefreshListener {
        fun onRefresh()
    }

    companion object {
        private const val DRAG_RATE = 0.45f
        private const val ANIM_DURATION = 250L
        private const val DEFAULT_REFRESH_TRIGGER_DP = 72

        const val DEFAULT = 1
        const val LARGE = 0
    }

    private var targetView: View? = null
    private val indicatorContainer: FrameLayout
    private val progressBar: LoadingIndicator

    private val touchSlop: Int
    private var initialDownY = 0f
    private var isBeingDragged = false

    var isRefreshing: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                indicatorContainer.visibility = View.VISIBLE
                indicatorContainer.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                animateIndicatorTo(refreshTrigger)
            } else {
                indicatorContainer.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(ANIM_DURATION)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            indicatorContainer.visibility = View.GONE
                            animateIndicatorTo(0)
                            indicatorContainer.animate().setListener(null)
                        }
                    }).start()
            }
        }

    private var listener: OnRefreshListener? = null
    private var refreshTrigger: Int
    private var indicatorTotalSize: Int

    var size: Int = DEFAULT
        set(value) {
            if (value != DEFAULT && value != LARGE) return
            field = value
            updatePresetSizes()
        }

    private val circleBg: GradientDrawable

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val density = resources.displayMetrics.density

        refreshTrigger = (DEFAULT_REFRESH_TRIGGER_DP * density + 0.5f).toInt()
        val circleSize = (40 * density).toInt()
        val progressSize = (24 * density).toInt()
        indicatorTotalSize = circleSize

        indicatorContainer = FrameLayout(context)
        circleBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E8DEDF"))
        }
        indicatorContainer.background = circleBg
        indicatorContainer.elevation = 6 * density

        progressBar = LoadingIndicator(context)

        val progressLp = LayoutParams(progressSize, progressSize).apply {
            gravity = Gravity.CENTER
        }
        indicatorContainer.addView(progressBar, progressLp)

        val containerLp = LayoutParams(circleSize, circleSize).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = -circleSize
        }
        addView(indicatorContainer, containerLp)

        indicatorContainer.visibility = View.GONE
        indicatorContainer.scaleX = 0f
        indicatorContainer.scaleY = 0f

        clipToPadding = false
        setWillNotDraw(false)
    }

    fun setProgressBackgroundColorSchemeColor(@ColorInt color: Int) {
        circleBg.setColor(color)
    }

    fun setColorSchemeColors(@ColorInt vararg colors: Int) {
        if (colors.isNotEmpty()) {
            progressBar.indicatorColor = colors
        }
    }

    fun setCustomSizeDp(@Dimension(unit = Dimension.DP) circleSizeDp: Int, @Dimension(unit = Dimension.DP) progressSizeDp: Int, @Dimension(unit = Dimension.DP) triggerDp: Int) {
        val density = resources.displayMetrics.density
        val circlePx = (circleSizeDp * density + 0.5f).toInt()
        val progressPx = (progressSizeDp * density + 0.5f).toInt()
        val triggerPx = (triggerDp * density + 0.5f).toInt()
        applyCustomSizes(circlePx, progressPx, triggerPx)
    }

    fun setCustomSizePx(@Px circleSizePx: Int, @Px progressSizePx: Int, @Px triggerPx: Int) {
        applyCustomSizes(circleSizePx, progressSizePx, triggerPx)
    }

    private fun updatePresetSizes() {
        val density = resources.displayMetrics.density
        val circleSize: Int
        val progressSize: Int

        if (size == LARGE) {
            circleSize = (56 * density + 0.5f).toInt()
            progressSize = (32 * density + 0.5f).toInt()
            refreshTrigger = (120 * density + 0.5f).toInt()
        } else {
            circleSize = (40 * density + 0.5f).toInt()
            progressSize = (24 * density + 0.5f).toInt()
            refreshTrigger = (DEFAULT_REFRESH_TRIGGER_DP * density + 0.5f).toInt()
        }
        applyCustomSizes(circleSize, progressSize, refreshTrigger)
    }

    private fun applyCustomSizes(@Px circlePx: Int, @Px progressPx: Int, @Px triggerPx: Int) {
        refreshTrigger = triggerPx
        indicatorTotalSize = circlePx

        indicatorContainer.layoutParams = indicatorContainer.layoutParams?.apply {
            width = circlePx
            height = circlePx
            if (this is LayoutParams) {
                topMargin = -circlePx
            }
        }

        progressBar.layoutParams = progressBar.layoutParams?.apply {
            width = progressPx
            height = progressPx
        }

        if (!isRefreshing) {
            indicatorContainer.translationY = 0f
        } else {
            animateIndicatorTo(refreshTrigger)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 1) {
            for (i in 0 until childCount) {
                val ch = getChildAt(i)
                if (ch !== indicatorContainer) {
                    targetView = ch
                    break
                }
            }
        } else if (childCount == 1 && getChildAt(0) !== indicatorContainer) {
            targetView = getChildAt(0)
        }
    }

    fun setTargetView(view: View) {
        targetView = view
    }

    fun setOnRefreshListener(listener: OnRefreshListener?) {
        this.listener = listener
    }

    private fun animateIndicatorTo(toOffset: Int) {
        ObjectAnimator.ofFloat(
            indicatorContainer,
            "translationY",
            indicatorContainer.translationY,
            toOffset.toFloat()
        ).apply {
            duration = ANIM_DURATION
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || canChildScrollUp() || isRefreshing) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialDownY = ev.y
                isBeingDragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                val y = ev.y
                val yDiff = y - initialDownY
                if (yDiff > touchSlop && !isBeingDragged) {
                    isBeingDragged = true
                    indicatorContainer.visibility = View.VISIBLE
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isBeingDragged = false
        }
        return isBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || canChildScrollUp() || isRefreshing) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialDownY = ev.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = ev.y
                var dy = (y - initialDownY) * DRAG_RATE
                if (dy < 0) return false

                val maxDrag = refreshTrigger * 1.5f
                if (dy > maxDrag) dy = maxDrag

                moveIndicator(dy.toInt())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val y = ev.y
                val dy = (y - initialDownY) * DRAG_RATE

                if (dy > refreshTrigger) {
                    isRefreshing = true
                    listener?.let {
                        Handler(Looper.getMainLooper()).postDelayed({ it.onRefresh() }, 100)
                    }
                } else {
                    indicatorContainer.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .setDuration(150)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                indicatorContainer.visibility = View.GONE
                                moveIndicator(0)
                                indicatorContainer.animate().setListener(null)
                            }
                        }).start()
                }
                isBeingDragged = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun moveIndicator(offset: Int) {
        indicatorContainer.translationY = offset.toFloat()

        if (offset > 0) {
            val progress = offset.toFloat() / refreshTrigger
            val scale = min(1.0f, progress)
            indicatorContainer.scaleX = scale
            indicatorContainer.scaleY = scale

            //progressBar.rotation = offset * 2.0f
        }
    }

    fun canChildScrollUp(): Boolean {
        targetView?.let {
            if (it is ListView) {
                if (it.childCount == 0) return false
                if (it.firstVisiblePosition > 0) return true
                val firstChild = it.getChildAt(0) ?: return false
                return firstChild.top < it.paddingTop
            } else {
                return it.canScrollVertically(-1)
            }
        }
        return true
    }
}