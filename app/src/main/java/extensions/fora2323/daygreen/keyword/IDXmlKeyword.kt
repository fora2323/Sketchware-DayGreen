package extensions.fora2323.daygreen.keyword

import kotlin.jvm.JvmField

object IDXmlKeyword {
    @JvmField
    val KEYWORDS = arrayOf(
        // XML Namespaces & Directives
        "xmlns:android=\"http://schemas.android.com/apk/res/android\"",
        "xmlns:app=\"http://schemas.android.com/apk/res-auto\"",
        "xmlns:tools=\"http://schemas.android.com/tools\"",
        "xml", "encoding", "utf-8", "version",

        // Manifest & Structure Tags
        "manifest", "application", "activity", "service", "receiver", "provider",
        "uses-permission", "uses-feature", "intent-filter", "action", "category", "data",
        "meta-data", "include", "merge", "view", "fragment", "androidx.fragment.app.FragmentContainerView",

        // Drawable & Vector Tags
        "vector", "path", "group", "clip-path", "selector", "shape", "solid", "stroke",
        "gradient", "corners", "padding", "size", "ripple", "layer-list", "item", "animated-vector",

        // Standard Layouts & Containers
        "LinearLayout", "RelativeLayout", "FrameLayout", "ConstraintLayout",
        "CoordinatorLayout", "DrawerLayout", "AppBarLayout", "CollapsingToolbarLayout",
        "CardView", "RecyclerView", "ListView", "GridView", "ScrollView",
        "HorizontalScrollView", "ViewPager", "ViewPager2", "TabLayout", "BottomNavigationView",

        // Standard Views & Material 3 Components
        "TextView", "Button", "ImageView", "EditText", "CheckBox", "RadioButton",
        "RadioGroup", "Spinner", "ProgressBar", "SeekBar", "Switch", "ToggleButton",
        "ImageButton", "FloatingActionButton", "Chip", "MaterialButton", "TextInputLayout",
        "TextInputEditText", "AppCompatTextView", "AppCompatButton", "AppCompatImageView",
        "ExtendedFloatingActionButton", "MaterialCardView", "MaterialDivider",
        "LinearProgressIndicator", "CircularProgressIndicator", "Slider", "RangeSlider",
        "MaterialCheckBox", "MaterialRadioButton", "SwitchMaterial", "SearchBar", "SearchView",
        "NavigationRailView", "ChipGroup", "ShapeableImageView",

        // Common Android Attributes
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
        "android:style", "android:exported", "android:permission",

        // App & ConstraintLayout Attributes
        "app:layout_constraintStart_toStartOf", "app:layout_constraintEnd_toEndOf",
        "app:layout_constraintTop_toTopOf", "app:layout_constraintBottom_toBottomOf",
        "app:layout_constraintLeft_toLeftOf", "app:layout_constraintRight_toRightOf",
        "app:layout_constraintBaseline_toBaselineOf", "app:layout_constraintDimensionRatio", "app:layout_behavior",
        "app:srcCompat", "app:tint", "app:backgroundTint", "app:strokeColor", "app:strokeWidth",
        "app:cornerRadius", "app:cardCornerRadius", "app:cardElevation", "app:cardBackgroundColor",
        "app:rippleColor", "app:startIconDrawable", "app:endIconMode", "app:hintEnabled",
        "app:helperText", "app:errorEnabled", "app:menu", "app:itemIconTint", "app:itemTextColor",
        "app:title", "app:subtitle", "app:navigationIcon",

        // Tools Attributes
        "tools:context", "tools:text", "tools:ignore", "tools:targetApi", "tools:listitem",
        "tools:showIn", "tools:visibility",

        // Values & References
        "match_parent", "wrap_content", "fill_parent", "@null", "@id/", "@+id/", "@string/",
        "@color/", "@drawable/", "@mipmap/", "@dimen/", "@style/", "@array/", "@bool/",
        "@integer/", "@attr/", "@android:color/black", "@android:color/white",
        "@android:color/transparent", "true", "false", "center", "center_horizontal",
        "center_vertical", "top", "bottom", "left", "right", "start", "end", "horizontal",
        "vertical", "gone", "visible", "invisible", "bold", "italic", "normal"
    )
}