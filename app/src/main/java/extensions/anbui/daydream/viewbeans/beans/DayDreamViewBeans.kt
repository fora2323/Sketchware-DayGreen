package extensions.anbui.daydream.viewbeans.beans

import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import pro.sketchware.R

object DayDreamViewBeans {
    const val VIEW_TYPE_WIDGET_LOADINGINDICATOR: Int = 80
    const val VIEW_TYPE_WIDGET_MATERIALDIVIDER: Int = 81
    const val VIEW_TYPE_WIDGET_BOTTOMSHEETDRAGHANDLEVIEW: Int = 82

    var views: BiMap<Int?, String?>? = ImmutableBiMap.Builder<Int?, String?>()
        .put(VIEW_TYPE_WIDGET_LOADINGINDICATOR, "LoadingIndicator")
        .put(VIEW_TYPE_WIDGET_MATERIALDIVIDER, "MaterialDivider")
        .put(VIEW_TYPE_WIDGET_BOTTOMSHEETDRAGHANDLEVIEW, "BottomSheetDragHandleView")
        .build()

    @JvmStatic
    fun getViewTypeResId(id: Int): Int {
        return when (id) {
            VIEW_TYPE_WIDGET_LOADINGINDICATOR -> R.drawable.eraser_size_5_24px
            VIEW_TYPE_WIDGET_MATERIALDIVIDER -> R.drawable.horizontal_rule_24px
            VIEW_TYPE_WIDGET_BOTTOMSHEETDRAGHANDLEVIEW -> R.drawable.drag_handle_24px
            else -> id
        }
    }
}
