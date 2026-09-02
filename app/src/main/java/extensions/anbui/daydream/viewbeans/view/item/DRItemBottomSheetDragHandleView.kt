package extensions.anbui.daydream.viewbeans.view.item

import a.a.a.wB
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.widget.LinearLayout
import com.besome.sketch.beans.ViewBean
import com.besome.sketch.editor.view.ItemView
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.divider.MaterialDivider

class DRItemBottomSheetDragHandleView(context: Context) : LinearLayout(context), ItemView {
    private var viewBean: ViewBean? = null
    private var isSelected = false
    private var isFixed = false
    private var paint: Paint? = null
    private var dip = 0f
    private var bottomSheetDragHandleView: BottomSheetDragHandleView? = null

    init {
        initialize(context)
    }

    private fun initialize(context: Context) {
        dip = wB.a(context, 1.0f)
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint!!.setColor(-0x6a662a30)
        setDrawingCacheEnabled(true)
        bottomSheetDragHandleView = BottomSheetDragHandleView(getContext())
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        bottomSheetDragHandleView!!.setLayoutParams(layoutParams)
        bottomSheetDragHandleView!!.setPadding(0, 0, 0, 0)
        addView(bottomSheetDragHandleView)
        gravity = Gravity.CENTER
    }

    override fun getBean(): ViewBean? {
        return viewBean
    }

    override fun setBean(viewBean: ViewBean?) {
        this.viewBean = viewBean
    }

    override fun getFixed(): Boolean {
        return isFixed
    }

    override fun setFixed(isFixed: Boolean) {
        this.isFixed = isFixed
    }

    fun getSelection(): Boolean {
        return isSelected
    }

    override fun setSelection(selection: Boolean) {
        isSelected = selection
        invalidate()
    }

    public override fun onDraw(var1: Canvas) {
        if (isSelected) {
            var1.drawRect(
                0f,
                0f,
                measuredWidth.toFloat(),
                measuredHeight.toFloat(),
                paint!!
            )
        }
        super.onDraw(var1)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(
            (left * dip).toInt(),
            (top * dip).toInt(),
            (right * dip).toInt(),
            (bottom * dip).toInt()
        )
    }
}