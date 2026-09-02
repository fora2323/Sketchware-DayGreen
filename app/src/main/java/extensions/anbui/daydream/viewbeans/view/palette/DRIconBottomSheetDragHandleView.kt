package extensions.anbui.daydream.viewbeans.view.palette

import android.content.Context
import com.besome.sketch.beans.ViewBean
import com.besome.sketch.editor.view.palette.IconBase
import extensions.anbui.daydream.viewbeans.beans.DayDreamViewBeans
import pro.sketchware.R

class DRIconBottomSheetDragHandleView(context: Context?) : IconBase(context) {
    init {
        setWidgetImage(R.drawable.drag_handle_24px)
        widgetName = "BottomSheetDragHandleView"
    }

    override fun getBean(): ViewBean {
        val viewBean = ViewBean()
        viewBean.type = DayDreamViewBeans.VIEW_TYPE_WIDGET_BOTTOMSHEETDRAGHANDLEVIEW
        val layoutBean = viewBean.layout
        layoutBean.paddingLeft = 0
        layoutBean.paddingTop = 0
        layoutBean.paddingRight = 0
        layoutBean.paddingBottom = 0
        layoutBean.width = LayoutParams.MATCH_PARENT;
        viewBean.text.text = name
        viewBean.convert = "com.google.android.material.bottomsheet.BottomSheetDragHandleView"
        return viewBean
    }
}