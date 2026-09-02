package extensions.anbui.daydream.viewbeans.view.palette

import android.content.Context
import com.besome.sketch.beans.ViewBean
import com.besome.sketch.editor.view.palette.IconBase
import extensions.anbui.daydream.viewbeans.beans.DayDreamViewBeans
import pro.sketchware.R

class DRIconMaterialDivider(context: Context?) : IconBase(context) {
    init {
        setWidgetImage(R.drawable.horizontal_rule_24px)
        widgetName = "MaterialDivider"
    }

    override fun getBean(): ViewBean {
        val viewBean = ViewBean()
        viewBean.type = DayDreamViewBeans.VIEW_TYPE_WIDGET_MATERIALDIVIDER
        val layoutBean = viewBean.layout
        layoutBean.paddingLeft = 16
        layoutBean.paddingTop = 8
        layoutBean.paddingRight = 16
        layoutBean.paddingBottom = 8
        viewBean.text.text = name
        viewBean.convert = "com.google.android.material.divider.MaterialDivider"
        viewBean.inject = "app:dividerInsetStart=\"16dp\"\napp:dividerInsetEnd=\"16dp\""
        return viewBean
    }
}