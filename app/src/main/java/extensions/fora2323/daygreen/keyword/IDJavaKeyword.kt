package extensions.fora2323.daygreen.keyword

import kotlin.jvm.JvmField

object IDJavaKeyword {
    @JvmField
    val KEYWORDS = arrayOf(
        // Java Keywords
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "native", "new", "package", "private", "protected",
        "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "var", "record", "sealed", "permits", "yield",

        // Android Core & AndroidX Classes
        "Activity", "AppCompatActivity", "ComponentActivity", "Fragment", "FragmentActivity",
        "Service", "BroadcastReceiver", "ContentProvider", "Intent", "Bundle", "View", "TextView",
        "Button", "ImageView", "EditText", "RecyclerView", "ListView", "ScrollView",
        "RelativeLayout", "LinearLayout", "FrameLayout", "ConstraintLayout", "CardView",
        "Toolbar", "AppBarLayout", "DrawerLayout", "NavigationView",
        "Dialog", "AlertDialog", "Toast", "ProgressBar", "SeekBar", "Adapter",
        "Context", "SharedPreferences", "Lifecycle", "ViewModel", "LiveData",

        // AndroidX Material Design 3 Components
        "MaterialButton", "MaterialButtonToggleGroup", "ExtendedFloatingActionButton",
        "FloatingActionButton", "MaterialCardView", "MaterialShapeDrawable",
        "TopAppBar", "CenterAlignedTopAppBar", "MediumTopAppBar", "LargeTopAppBar",
        "NavigationBar", "NavigationRail", "NavigationDrawer", "ModalNavigationDrawer",
        "PermanentNavigationDrawer", "ModalBottomSheet", "BottomSheetDialog", "BottomSheetBehavior",
        "Chip", "ChipGroup", "FilterChip", "InputChip", "SuggestionChip",
        "SegmentedButton", "SegmentedButtonGroup",
        "Slider", "RangeSlider", "Switch", "MaterialCheckBox", "MaterialRadioButton",
        "BadgeDrawable", "BadgedBox", "LinearProgressIndicator", "CircularProgressIndicator",
        "SearchBar", "DockedSearchBar", "Snackbar", "SnackbarHost",
        "MaterialTimePicker", "MaterialDatePicker", "TextInputLayout", "TextInputEditText",
        "MaterialDivider", "MaterialAutoCompleteTextView",

        // Common Android Methods
        "findViewById", "setContentView", "onCreate", "onStart", "onResume", "onPause",
        "onStop", "onDestroy", "onClick", "setOnClickListener", "startActivity",
        "putExtra", "getIntent", "getExtras", "finish", "setText", "getText",

        // Common Java Standard Libraries
        "ArrayList", "HashMap", "HashSet", "List", "Map", "Set", "String", "Integer",
        "Boolean", "Double", "Float", "Long", "Object", "Log", "StringBuilder",
        "StringBuffer", "Math", "System", "Arrays", "Collections",

        // Retrofit / Volley
        "Retrofit", "OkHttpClient", "Gson", "Call", "Callback", "Response", "RequestQueue",
        "JsonObjectRequest", "StringRequest",

        // R.java References & Resources
        "R.id.", "R.layout.", "R.string.", "R.drawable.", "R.color.", "R.dimen.",
        "R.style.", "R.attr.", "R.mipmap.", "R.raw.",
        "@id/", "@+id/", "@string/", "@color/", "@drawable/", "@mipmap/", "@dimen/",
        "@style/", "@layout/", "@android:", "@attr/"
    )
}