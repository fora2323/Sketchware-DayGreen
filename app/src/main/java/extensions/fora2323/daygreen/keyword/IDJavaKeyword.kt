package extensions.fora2323.daygreen.keyword

object IDJavaKeyword {
    val KEYWORDS = arrayOf(
        // Java keywords
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "native", "new", "package", "private", "protected",
        "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try", "void", "volatile", "while",

        // Android classes
        "Activity", "AppCompatActivity", "Fragment", "FragmentActivity", "Service",
        "BroadcastReceiver", "ContentProvider", "Intent", "Bundle", "View", "TextView",
        "Button", "ImageView", "EditText", "RecyclerView", "ListView", "ScrollView",
        "RelativeLayout", "LinearLayout", "FrameLayout", "ConstraintLayout", "CardView",
        "Toolbar", "AppBarLayout", "DrawerLayout", "NavigationView", "BottomNavigationView",
        "Dialog", "AlertDialog", "Toast", "Snackbar", "ProgressBar", "SeekBar",
        "Switch", "CheckBox", "RadioButton", "RadioGroup", "Spinner", "Adapter",

        // Common Android methods
        "findViewById", "setContentView", "onCreate", "onStart", "onResume", "onPause",
        "onStop", "onDestroy", "onClick", "setOnClickListener", "startActivity",
        "putExtra", "getIntent", "getExtras", "finish", "setText", "getText",

        // R.java references
        "R.id.", "R.layout.", "R.string.", "R.drawable.", "R.color.", "R.dimen.",
        "R.style.", "R.attr.", "R.mipmap.", "R.raw.",

        // Android resources
        "@id/", "@+id/", "@string/", "@color/", "@drawable/", "@mipmap/", "@dimen/",
        "@style/", "@layout/", "@android:",

        // Common Java/Android libraries
        "ArrayList", "HashMap", "HashSet", "List", "Map", "Set", "String", "Integer",
        "Boolean", "Double", "Float", "Long", "Object", "Log", "Context", "SharedPreferences",

        // Retrofit/Volley
        "Retrofit", "OkHttpClient", "Gson", "Call", "Callback", "Response", "RequestQueue",
        "JsonObjectRequest", "StringRequest",

        // Room Database
        "Room", "Database", "Dao", "Entity", "LiveData", "ViewModel",

        // Firebase
        "FirebaseDatabase", "FirebaseAuth", "FirebaseStorage", "DatabaseReference",
        "Query", "ValueEventListener", "ChildEventListener"
    )
}