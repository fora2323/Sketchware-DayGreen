package extensions.fora2323.daygreen.keyword

object IDKotlinKeyword {
    val KEYWORDS = arrayOf(
        // Kotlin keywords
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "private",
        "protected", "public", "return", "super", "this", "throw", "true", "try",
        "typealias", "typeof", "val", "var", "when", "while",

        // Android classes (same as Java)
        "Activity", "AppCompatActivity", "Fragment", "Intent", "View", "TextView",
        "Button", "RecyclerView", "LinearLayout", "RelativeLayout", "Toolbar",

        // Kotlin Android extensions
        "findViewById", "setContentView", "startActivity", "toast", "snackbar",

        // Coroutines
        "launch", "async", "runBlocking", "Dispatchers", "withContext", "coroutineScope",

        // Kotlin collections
        "listOf", "mutableListOf", "arrayListOf", "mapOf", "mutableMapOf", "hashMapOf",
        "setOf", "mutableSetOf", "hashSetOf",

        // R.java references
        "R.id.", "R.layout.", "R.string.", "R.drawable.", "@id/", "@+id/", "@string/",
        "@color/", "@drawable/"
    )
}