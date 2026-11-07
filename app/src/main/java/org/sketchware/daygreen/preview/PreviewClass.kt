package org.sketchware.daygreen.preview

import a.a.a.wq
import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.besome.sketch.beans.ViewBean
import dalvik.system.DexClassLoader
import dalvik.system.DexFile
import mod.jbk.build.BuiltInLibraries
import org.sketchware.daygreen.FileCheckUtils
import pro.sketchware.SketchApplication
import pro.sketchware.tools.ViewBeanFactory
import pro.sketchware.utility.FilePathUtil
import java.io.File

object PreviewClass {

    private const val TAG = "PreviewClass"

    @JvmStatic
    var currentScId: String? = null

    private var cachedScId: String? = null
    private var cachedClassLoader: ClassLoader? = null
    private var cachedDexTimestamp: Long = 0L

    // Cache of simple class name -> fully qualified class name for a project
    private val fqnMap = HashMap<String, String>()
    private var fqnMapScId: String? = null

    /**
     * Checks if project classes have been compiled by javac/kotlinc into bin/classes or build_cache/classes.
     * If so, automatically and quickly compiles them into a DEX file (preview_classes.dex) using D8.
     */
    private fun ensureDexFromClasses(scId: String, binDir: File, context: Context): File? {
        val classesDir = File(binDir, "classes")
        val cacheClassesDir = File(binDir.parentFile, "build_cache/classes")

        val sourceDir = when {
            classesDir.isDirectory && classesDir.walkTopDown().any { it.isFile && it.extension == "class" } -> classesDir
            cacheClassesDir.isDirectory && cacheClassesDir.walkTopDown().any { it.isFile && it.extension == "class" } -> cacheClassesDir
            else -> return null
        }

        val classFiles = sourceDir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()
        if (classFiles.isEmpty()) return null

        val appCodeCache = context.codeCacheDir ?: context.cacheDir
        val previewDexDir = File(appCodeCache, "preview_dex_$scId").apply { if (!exists()) mkdirs() }
        val targetDex = File(previewDexDir, "preview_classes.dex")

        val latestClassTime = classFiles.maxOf { it.lastModified() }
        if (targetDex.exists() && targetDex.lastModified() >= latestClassTime && targetDex.length() > 0) {
            return targetDex
        }

        return try {
            val tempDir = File(previewDexDir, "temp_dex").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            val builder = D8Command.builder()
                .setIntermediate(false)
                .setMode(CompilationMode.DEBUG)
                .setMinApiLevel(21)
                .setOutput(tempDir.toPath(), OutputMode.DexIndexed)

            for (cf in classFiles) {
                builder.addProgramFiles(cf.toPath())
            }

            val androidJar = FileCheckUtils.getDefaultAndroidJar(context)
            if (androidJar != null && androidJar.exists() && androidJar.length() > 0) {
                builder.addLibraryFiles(listOf(androidJar.toPath()))
            }

            D8.run(builder.build())

            val outputDex = File(tempDir, "classes.dex")
            if (outputDex.exists() && outputDex.length() > 0) {
                targetDex.setWritable(true)
                outputDex.copyTo(targetDex, overwrite = true)
                targetDex.setReadOnly()
                tempDir.deleteRecursively()
                targetDex
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to compile classes to DEX for preview: ${e.message}")
            null
        }
    }

    /**
     * Finds all compiled DEX or APK files for the given project [scId].
     */
    @JvmStatic
    fun findProjectDexFiles(scId: String): List<File> {
        val list = mutableListOf<File>()
        try {
            val context = SketchApplication.getContext()
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            // 1. Auto-dex from compiled class files (bin/classes or build_cache/classes)
            ensureDexFromClasses(scId, binDir, context)?.let {
                list.add(it)
            }

            // 2. bin/dex/*.dex (from D8 / ProjectBuilder after run/compile)
            val dexDir = File(binDir, "dex")
            if (dexDir.isDirectory) {
                dexDir.listFiles { _, name -> name.endsWith(".dex") }?.let {
                    for (f in it) {
                        if (!list.contains(f)) list.add(f)
                    }
                }
            }

            // 3. bin/classes.dex
            val rootDex = File(binDir, "classes.dex")
            if (rootDex.isFile && !list.contains(rootDex)) {
                list.add(rootDex)
            }

            // 4. Built-in library DEX files (AndroidX, Material, AppCompat, RecyclerView, etc.)
            try {
                val dexsDir = BuiltInLibraries.EXTRACTED_BUILT_IN_LIBRARY_DEX_FILES_PATH
                if (dexsDir.isDirectory) {
                    dexsDir.listFiles { _, name -> name.endsWith(".dex") }?.let {
                        for (libDex in it) {
                            if (!list.contains(libDex)) list.add(libDex)
                        }
                    }
                }
            } catch (ignored: Throwable) {
            }

            // 5. User local library DEX (.sketchware/data/<sc_id>/files/library/dex/*.dex)
            try {
                val userLibDexDir = File(FilePathUtil().getDexPathLocalLibraryUser(scId))
                if (userLibDexDir.isDirectory) {
                    userLibDexDir.listFiles { _, name -> name.endsWith(".dex") }?.let {
                        for (f in it) {
                            if (!list.contains(f)) list.add(f)
                        }
                    }
                }
            } catch (ignored: Throwable) {
            }

            // 6. Global local libraries (.sketchware/libs/local_libs/*/classes.dex)
            try {
                val globalLibs = File(Environment.getExternalStorageDirectory(), ".sketchware/libs/local_libs")
                if (globalLibs.isDirectory) {
                    globalLibs.listFiles()?.forEach { libFolder ->
                        if (libFolder.isDirectory) {
                            val libDex = File(libFolder, "classes.dex")
                            if (libDex.isFile && !list.contains(libDex)) list.add(libDex)
                        }
                    }
                }
            } catch (ignored: Throwable) {
            }

            // 7. Project APK in bin/
            if (binDir.isDirectory) {
                binDir.listFiles { _, name -> name.endsWith(".apk") || name.endsWith(".apk.unsigned") }?.let {
                    for (apk in it) {
                        if (!list.contains(apk)) list.add(apk)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error finding project dex files for $scId: ${e.message}")
        }
        return list
    }

    /**
     * Returns true if the project has been compiled at least once (class files or DEX files exist).
     */
    @JvmStatic
    fun isCompiled(scId: String?): Boolean {
        if (scId.isNullOrEmpty()) return false
        try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            val classesDir = File(binDir, "classes")
            if (classesDir.isDirectory && classesDir.walkTopDown().any { it.isFile && it.extension == "class" }) {
                return true
            }

            val cacheClasses = File(myscPath, "build_cache/classes")
            if (cacheClasses.isDirectory && cacheClasses.walkTopDown().any { it.isFile && it.extension == "class" }) {
                return true
            }

            val dexDir = File(binDir, "dex")
            if (dexDir.isDirectory && dexDir.listFiles { _, name -> name.endsWith(".dex") }?.isNotEmpty() == true) {
                return true
            }

            if (File(binDir, "classes.dex").isFile) return true

            if (binDir.listFiles { _, name -> name.endsWith(".apk") || name.endsWith(".apk.unsigned") }?.isNotEmpty() == true) {
                return true
            }
        } catch (ignored: Throwable) {
        }
        return false
    }

    /**
     * Determines whether the given class name represents a custom user class rather than
     * a standard Android/AndroidX framework view.
     */
    @JvmStatic
    fun isCustomClass(className: String?): Boolean {
        if (className.isNullOrBlank()) return false

        // Standard short tags
        when (className) {
            "View", "ViewGroup", "LinearLayout", "RelativeLayout", "FrameLayout",
            "ScrollView", "HorizontalScrollView", "HScrollView", "TextView", "Button",
            "ImageView", "ImageButton", "EditText", "CheckBox", "RadioButton",
            "Switch", "SeekBar", "ProgressBar", "Spinner", "ListView", "GridView",
            "include" -> return false
        }

        // Framework and standard library packages (handled natively by Sketchware)
        if (className.startsWith("android.") ||
            className.startsWith("androidx.") ||
            className.startsWith("com.google.android.material.") ||
            className.startsWith("com.google.android.gms.")
        ) {
            return false
        }

        return true
    }

    /**
     * Retrieves or creates a [DexClassLoader] for the project's compiled DEX files.
     */
    @JvmStatic
    @Synchronized
    fun getClassLoader(context: Context, scId: String): ClassLoader? {
        val dexFiles = findProjectDexFiles(scId)
        if (dexFiles.isEmpty()) return null

        val latestTimestamp = dexFiles.maxOfOrNull { it.lastModified() } ?: 0L

        if (cachedScId == scId && cachedClassLoader != null && cachedDexTimestamp == latestTimestamp) {
            return cachedClassLoader
        }

        return try {
            val appCodeCache = context.codeCacheDir ?: context.cacheDir
            val internalDexDir = File(appCodeCache, "preview_dex_$scId").apply {
                if (!exists()) mkdirs()
            }

            val copiedPaths = mutableListOf<String>()
            for (dex in dexFiles) {
                val target = File(internalDexDir, dex.name)
                if (!target.exists() || target.lastModified() < dex.lastModified()) {
                    target.setWritable(true)
                    dex.copyTo(target, overwrite = true)
                    target.setReadOnly()
                }
                copiedPaths.add(target.absolutePath)
            }

            val dexPath = copiedPaths.joinToString(File.pathSeparator)
            val optDir = File(appCodeCache, "preview_opt_$scId").apply {
                if (!exists()) mkdirs()
            }

            val loader = DexClassLoader(
                dexPath,
                optDir.absolutePath,
                null,
                context.classLoader
            )

            cachedScId = scId
            cachedClassLoader = loader
            cachedDexTimestamp = latestTimestamp
            loader
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create DexClassLoader for sc_id=$scId", e)
            null
        }
    }

    /**
     * Resolves a simple class name (e.g. "MyCustomView") to its fully-qualified class name (e.g. "com.my.app.MyCustomView")
     * by scanning project classes, manifest package, and DEX entries.
     */
    @JvmStatic
    fun resolveClassFqn(scId: String, simpleName: String): String? {
        if (fqnMapScId == scId && fqnMap.containsKey(simpleName)) {
            return fqnMap[simpleName]
        }

        if (fqnMapScId != scId) {
            fqnMap.clear()
            fqnMapScId = scId
        }

        try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            // 1. Scan bin/classes and build_cache/classes
            val searchDirs = listOf(File(binDir, "classes"), File(myscPath, "build_cache/classes"))
            for (dir in searchDirs) {
                if (dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
                        val rel = file.relativeTo(dir).path
                        val fqn = rel.removeSuffix(".class").replace('/', '.').replace('\\', '.')
                        val short = fqn.substringAfterLast('.')
                        fqnMap[short] = fqn
                        if (short == simpleName) {
                            return fqn
                        }
                    }
                }
            }

            // 2. Scan DEX entries in project DEX files
            val dexFiles = findProjectDexFiles(scId)
            for (dexFile in dexFiles) {
                try {
                    @Suppress("DEPRECATION")
                    val df = DexFile(dexFile)
                    val entries = df.entries()
                    while (entries.hasMoreElements()) {
                        val className = entries.nextElement()
                        val short = className.substringAfterLast('.')
                        if (!fqnMap.containsKey(short)) {
                            fqnMap[short] = className
                        }
                        if (short == simpleName) {
                            return className
                        }
                    }
                } catch (ignored: Throwable) {
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error resolving class FQN for $simpleName in $scId: ${e.message}")
        }

        return fqnMap[simpleName]
    }

    /**
     * Loads the [Class] by name. If [scId] is provided, attempts to load from the project's
     * compiled DEX first; otherwise falls back to the host app's ClassLoader.
     */
    @JvmStatic
    @JvmOverloads
    fun loadClass(context: Context, className: String, scId: String? = null): Class<*>? {
        if (className.isBlank()) return null
        val targetScId = scId ?: currentScId
        val cleanName = className.trim()

        // 1. Try project DEX if compiled
        if (!targetScId.isNullOrEmpty()) {
            try {
                val loader = getClassLoader(context, targetScId)
                if (loader != null) {
                    // Try exact name
                    try {
                        return loader.loadClass(cleanName)
                    } catch (ignored: ClassNotFoundException) {
                    } catch (ignored: NoClassDefFoundError) {
                    }

                    // If simple name without package, resolve from project
                    if (!cleanName.contains('.')) {
                        val resolvedFqn = resolveClassFqn(targetScId, cleanName)
                        if (resolvedFqn != null) {
                            try {
                                return loader.loadClass(resolvedFqn)
                            } catch (ignored: Throwable) {
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error loading class $cleanName from project $targetScId: ${e.message}")
            }
        }

        // 2. Try host app ClassLoader
        try {
            return context.classLoader.loadClass(cleanName)
        } catch (ignored: Throwable) {
        }

        // 3. Try standard Android prefixes if simple name
        if (!cleanName.contains('.')) {
            val prefixes = arrayOf(
                "android.widget.",
                "android.view.",
                "android.webkit.",
                "com.google.android.material."
            )
            for (prefix in prefixes) {
                try {
                    return context.classLoader.loadClass(prefix + cleanName)
                } catch (ignored: Throwable) {
                }
            }
        }

        return null
    }

    /**
     * Resolves the corresponding ViewBean viewType integer for a custom class.
     * If the class has been compiled, inspects its superclasses to map to the nearest
     * known ViewBean widget/layout type.
     * If not compiled or not found, falls back to [ViewBean.VIEW_TYPE_LAYOUT_LINEAR].
     */
    @JvmStatic
    @JvmOverloads
    fun resolveViewType(className: String, scId: String? = null, context: Context? = null): Int {
        val ctx = context ?: SketchApplication.getContext()
        val clazz = loadClass(ctx, className, scId)

        if (clazz == null) {
            // Belum pernah di-compile atau tidak ditemukan -> fallback ke LinearLayout
            return ViewBean.VIEW_TYPE_LAYOUT_LINEAR
        }

        if (!View::class.java.isAssignableFrom(clazz)) {
            return ViewBean.VIEW_TYPE_LAYOUT_LINEAR
        }

        var current: Class<*>? = clazz
        while (current != null && current != View::class.java && current != Any::class.java) {
            val simpleName = current.simpleName

            val registryType = PreviewViewRegistry.typeOf(simpleName, -1)
            if (registryType != -1 && registryType != ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                return registryType
            }

            val considered = ViewBeanFactory.getConsideredTypeViewByName(simpleName, -1)
            if (considered != -1 && considered != ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                return considered
            }

            val typeByName = ViewBean.getViewTypeByTypeName(simpleName)
            if (typeByName != -1 && typeByName != ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                return typeByName
            }

            current = current.superclass
        }

        return ViewBean.VIEW_TYPE_LAYOUT_LINEAR
    }

    /**
     * Instantiates a View of the given [className] using reflection.
     * Returns null if the class cannot be loaded or instantiated.
     */
    @JvmStatic
    @JvmOverloads
    fun createView(context: Context, className: String, scId: String? = null): View? {
        val clazz = loadClass(context, className, scId)
        if (clazz == null) {
            Log.d(TAG, "createView: class '$className' not loaded for scId=$scId")
            return null
        }
        if (!View::class.java.isAssignableFrom(clazz)) {
            Log.w(TAG, "createView: '$className' is not a View subclass")
            return null
        }

        // Try constructor (Context)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(context) as View
        } catch (t: Throwable) {
            val cause = if (t is java.lang.reflect.InvocationTargetException) t.targetException else t
            Log.d(TAG, "createView: (Context) failed for $className: ${cause.message}")
        }

        // Try constructor (Context, AttributeSet)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(context, null) as View
        } catch (t: Throwable) {
            val cause = if (t is java.lang.reflect.InvocationTargetException) t.targetException else t
            Log.d(TAG, "createView: (Context, AttributeSet) failed for $className: ${cause.message}")
        }

        // Try constructor (Context, AttributeSet, Int)
        try {
            val ctor = clazz.getDeclaredConstructor(
                Context::class.java,
                AttributeSet::class.java,
                Int::class.javaPrimitiveType ?: java.lang.Integer.TYPE
            )
            ctor.isAccessible = true
            return ctor.newInstance(context, null, 0) as View
        } catch (t: Throwable) {
            val cause = if (t is java.lang.reflect.InvocationTargetException) t.targetException else t
            Log.d(TAG, "createView: (Context, AttributeSet, Int) failed for $className: ${cause.message}")
        }

        return null
    }

    /**
     * Fallback view when a custom class has not been compiled yet or cannot be instantiated.
     */
    @JvmStatic
    fun fallbackView(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
    }

    /**
     * Creates an [ItemCustomView] wrapping the real compiled custom view,
     * or a fallback [ItemLinearLayout] with an Android Studio-style placeholder if not yet compiled.
     */
    @JvmStatic
    fun createItemView(context: Context, viewBean: ViewBean, scId: String?): View {
        val targetScId = scId ?: currentScId
        val realView = if (!targetScId.isNullOrEmpty() && isCompiled(targetScId)) {
            createView(context, viewBean.convert, targetScId)
        } else {
            null
        }
        return ItemCustomView(context, realView, viewBean)
    }

    @JvmStatic
    fun clearCache(scId: String? = null) {
        if (scId == null || scId == cachedScId) {
            cachedScId = null
            cachedClassLoader = null
            cachedDexTimestamp = 0L
            fqnMap.clear()
            fqnMapScId = null
        }
    }
}
