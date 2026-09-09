package extensions.fora2323.daygreen.keyword

import kotlin.jvm.JvmField

object IDKotlinKeyword {
    @JvmField
    val KEYWORDS = arrayOf(
        // Hard Keywords
        "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "!in", "interface", "is", "!is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "val", "var", "when", "while",

        // Soft Keywords & Modifiers
        "abstract", "actual", "annotation", "by", "catch", "companion", "constructor",
        "crossinline", "data", "delegate", "dynamic", "enum", "expect", "external",
        "field", "final", "finally", "get", "import", "infix", "init", "inline", "inner",
        "internal", "lateinit", "noinline", "open", "operator", "out", "override",
        "private", "protected", "public", "reified", "sealed", "set", "suspend",
        "tailrec", "value", "vararg", "where",

        // Scope Functions & Stdlib Utilities
        "let", "run", "with", "apply", "also", "takeIf", "takeUnless", "repeat", "lazy",

        // Collections & Functional Operations
        "listOf", "mutableListOf", "arrayListOf", "mapOf", "mutableMapOf", "hashMapOf",
        "setOf", "mutableSetOf", "hashSetOf", "arrayOf", "emptyList", "emptyMap", "emptySet",
        "forEach", "map", "mapNotNull", "filter", "filterNotNull", "first", "firstOrNull",
        "last", "lastOrNull", "find", "any", "all", "none", "count", "sortedBy", "groupBy",

        // Coroutines & Flow
        "launch", "async", "runBlocking", "Dispatchers", "withContext", "coroutineScope",
        "supervisorScope", "Job", "Deferred", "Flow", "StateFlow", "SharedFlow",
        "MutableStateFlow", "MutableSharedFlow", "collect", "collectLatest", "emit",
        "lifecycleScope", "viewModelScope",

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
        "putExtra", "getIntent", "getExtras", "finish", "setText", "getText", "toast", "snackbar",

        // R.java References & Resources
        "R.id.", "R.layout.", "R.string.", "R.drawable.", "R.color.", "R.dimen.",
        "R.style.", "R.attr.", "R.mipmap.", "R.raw.",
        "@id/", "@+id/", "@string/", "@color/", "@drawable/", "@mipmap/", "@dimen/",
        "@style/", "@layout/", "@android:", "@attr/"
    )
}