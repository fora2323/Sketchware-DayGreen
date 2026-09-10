package extensions.fora2323.daygreen.keyword

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object IDXmlKeyword {

    private val BASE = arrayOf(
        "xmlns:android=\"http://schemas.android.com/apk/res/android\"",
        "xmlns:app=\"http://schemas.android.com/apk/res-auto\"",
        "xmlns:tools=\"http://schemas.android.com/tools\"",
        "xml", "encoding", "utf-8", "version",
        "true", "false"
    )

    private val LAYOUT = arrayOf(
        "include", "merge", "view", "fragment", "androidx.fragment.app.FragmentContainerView",

        "LinearLayout", "RelativeLayout", "FrameLayout", "ConstraintLayout",
        "CoordinatorLayout", "DrawerLayout", "AppBarLayout", "CollapsingToolbarLayout",
        "CardView", "RecyclerView", "ListView", "GridView", "ScrollView",
        "HorizontalScrollView", "ViewPager", "ViewPager2", "TabLayout", "BottomNavigationView",

        "TextView", "Button", "ImageView", "EditText", "CheckBox", "RadioButton",
        "RadioGroup", "Spinner", "ProgressBar", "SeekBar", "Switch", "ToggleButton",
        "ImageButton", "FloatingActionButton", "Chip", "MaterialButton", "TextInputLayout",
        "TextInputEditText", "AppCompatTextView", "AppCompatButton", "AppCompatImageView",
        "ExtendedFloatingActionButton", "MaterialCardView", "MaterialDivider",
        "LinearProgressIndicator", "CircularProgressIndicator", "Slider", "RangeSlider",
        "MaterialCheckBox", "MaterialRadioButton", "SwitchMaterial", "SearchBar", "SearchView",
        "NavigationRailView", "ChipGroup", "ShapeableImageView",

        "android:id", "android:name", "android:layout_width", "android:layout_height",
        "android:layout_margin", "android:layout_marginLeft", "android:layout_marginRight",
        "android:layout_marginTop", "android:layout_marginBottom", "android:layout_marginStart",
        "android:layout_marginEnd", "android:layout_padding", "android:layout_paddingLeft",
        "android:layout_paddingRight", "android:layout_paddingTop", "android:layout_paddingBottom",
        "android:layout_gravity", "android:layout_weight", "android:text", "android:textColor",
        "android:textSize", "android:textStyle", "android:background", "android:foreground",
        "android:gravity", "android:orientation", "android:visibility", "android:src",
        "android:enabled", "android:clickable", "android:focusable", "android:inputType",
        "android:hint", "android:maxLines", "android:minLines", "android:maxLength",
        "android:imeOptions", "android:windowSoftInputMode", "android:adjustViewBounds",
        "android:scaleType", "android:contentDescription", "android:elevation", "android:alpha",
        "android:rotation", "android:clipToPadding", "android:clipChildren", "android:fitsSystemWindows",
        "android:importantForAccessibility", "android:onClick", "android:tag", "android:theme",
        "android:style",

        "app:layout_constraintStart_toStartOf", "app:layout_constraintEnd_toEndOf",
        "app:layout_constraintTop_toTopOf", "app:layout_constraintBottom_toBottomOf",
        "app:layout_constraintLeft_toLeftOf", "app:layout_constraintRight_toRightOf",
        "app:layout_constraintBaseline_toBaselineOf", "app:layout_constraintDimensionRatio", "app:layout_behavior",
        "app:srcCompat", "app:tint", "app:backgroundTint", "app:strokeColor", "app:strokeWidth",
        "app:cornerRadius", "app:cardCornerRadius", "app:cardElevation", "app:cardBackgroundColor",
        "app:rippleColor", "app:startIconDrawable", "app:endIconMode", "app:hintEnabled",
        "app:helperText", "app:errorEnabled", "app:menu", "app:itemIconTint", "app:itemTextColor",
        "app:title", "app:subtitle", "app:navigationIcon",

        "tools:context", "tools:text", "tools:ignore", "tools:targetApi", "tools:listitem",
        "tools:showIn", "tools:visibility",

        "match_parent", "wrap_content", "fill_parent", "@null", "@id/", "@+id/", "@string/",
        "@color/", "@drawable/", "@mipmap/", "@dimen/", "@style/", "@array/", "@bool/",
        "@integer/", "@attr/", "@android:color/black", "@android:color/white",
        "@android:color/transparent", "center", "center_horizontal",
        "center_vertical", "top", "bottom", "left", "right", "start", "end", "horizontal",
        "vertical", "gone", "visible", "invisible", "bold", "italic", "normal"
    )

    private val MANIFEST = arrayOf(
        "manifest", "application", "activity", "service", "receiver", "provider",
        "uses-permission", "uses-feature", "intent-filter", "action", "category", "data",
        "meta-data", "android:exported", "android:permission", "android:theme", "package"
    )

    private val DRAWABLE = arrayOf(
        "vector", "path", "group", "clip-path", "selector", "shape", "solid", "stroke",
        "gradient", "corners", "padding", "size", "ripple", "layer-list", "item", "animated-vector",
        "bitmap", "inset", "level-list", "android:width", "android:height", "android:viewportWidth",
        "android:viewportHeight", "android:fillColor", "android:pathData", "android:tint"
    )

    private val ANIM = arrayOf(
        "set", "alpha", "scale", "translate", "rotate",
        "objectAnimator", "valueAnimator", "propertyValuesHolder", "animator",
        "android:duration", "android:fromAlpha", "android:toAlpha",
        "android:fromXScale", "android:toXScale", "android:fromYScale", "android:toYScale",
        "android:pivotX", "android:pivotY", "android:fromXDelta", "android:toXDelta",
        "android:fromYDelta", "android:toYDelta", "android:fromDegrees", "android:toDegrees",
        "android:interpolator", "android:repeatCount", "android:repeatMode", "android:fillAfter",
        "android:startOffset", "android:propertyName", "android:valueType", "android:valueFrom",
        "android:valueTo", "android:ordering"
    )

    private val MENU = arrayOf(
        "menu", "item", "group",
        "android:id", "android:title", "android:icon", "android:orderInCategory",
        "android:showAsAction", "android:visible", "android:enabled", "android:checkable",
        "app:showAsAction", "app:actionViewClass", "app:actionProviderClass"
    )

    private val COLOR_STATE_LIST = arrayOf(
        "selector", "item",
        "android:color", "android:alpha", "android:state_enabled", "android:state_checked",
        "android:state_pressed", "android:state_focused", "android:state_selected",
        "android:state_activated"
    )

    private val VALUES_COLORS = arrayOf("resources", "color", "name")
    private val VALUES_STRINGS = arrayOf("resources", "string", "string-array", "plurals", "item", "name", "translatable", "formatted")
    private val VALUES_STYLES = arrayOf("resources", "style", "item", "name", "parent")
    private val VALUES_ATTRS = arrayOf("resources", "declare-styleable", "attr", "name", "format", "enum", "flag", "reference", "dimension", "boolean", "integer", "fraction")
    private val VALUES_ARRAYS = arrayOf("resources", "array", "string-array", "integer-array", "item", "name")
    private val VALUES_ALL = VALUES_STYLES + VALUES_COLORS + VALUES_STRINGS + VALUES_ATTRS + VALUES_ARRAYS

    @JvmField
    val KEYWORDS = BASE + LAYOUT + MANIFEST + DRAWABLE + ANIM + MENU + COLOR_STATE_LIST + VALUES_ALL

    private val RESOURCE_FOLDER_BASE_NAMES = setOf(
        "values", "drawable", "mipmap", "layout", "anim", "animator", "menu", "color"
    )

    @JvmStatic
    fun forFile(pathOrName: String?): Array<String> {
        if (pathOrName == null) return KEYWORDS

        val lower = pathOrName.lowercase()
        val fileName = lower.substringAfterLast('/')

        if (fileName.contains("manifest") || lower.contains("/androidmanifest/")) {
            return BASE + MANIFEST
        }

        val segments = lower.split('/')
        val baseFolder = segments
            .map { it.substringBefore('-') }
            .firstOrNull { it in RESOURCE_FOLDER_BASE_NAMES }

        return when (baseFolder) {
            "values" -> BASE + VALUES_ALL
            "drawable", "mipmap" -> BASE + DRAWABLE
            "anim", "animator" -> BASE + ANIM
            "menu" -> BASE + MENU
            "color" -> BASE + COLOR_STATE_LIST
            "layout" -> BASE + LAYOUT
            else -> {
                when (fileName) {
                    "colors.xml" -> BASE + VALUES_COLORS
                    "strings.xml" -> BASE + VALUES_STRINGS
                    "styles.xml", "themes.xml" -> BASE + VALUES_STYLES
                    "attrs.xml" -> BASE + VALUES_ATTRS
                    "arrays.xml" -> BASE + VALUES_ARRAYS
                    else -> BASE + LAYOUT
                }
            }
        }
    }
}