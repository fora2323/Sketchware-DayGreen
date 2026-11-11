package org.sketchware.daygreen.preview

import a.a.a.wq
import android.content.Context
import android.os.Environment
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.besome.sketch.beans.LayoutBean
import com.besome.sketch.beans.ViewBean
import dalvik.system.DexFile
import mod.jbk.build.BuiltInLibraries
import org.sketchware.daygreen.FileCheckUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import pro.sketchware.SketchApplication
import pro.sketchware.tools.ViewBeanFactory
import pro.sketchware.utility.FilePathUtil
import java.io.File
import java.io.StringReader

object PreviewClass {

    private const val TAG = "PreviewClass"

    @JvmStatic
    var currentScId: String? = null

    private var cachedScId: String? = null
    private var cachedClassLoader: ClassLoader? = null
    private var cachedDexTimestamp: Long = 0L

    private val fqnMap = HashMap<String, String>()
    private var fqnMapScId: String? = null
    private var fqnIndexScId: String? = null

    private const val CACHE_TTL_MS = 2000L
    private const val STALE_TTL_MS = 3000L
    private const val MISS_TTL_MS = 3000L

    private val cacheLock = Any()

    // Lock per-scId supaya ensureDexFromClasses tidak dijalankan paralel untuk proyek yang sama
    private val dexBuildLocks = HashMap<String, Any>()

    // Pemetaan custom view ke widget standar Sketchware (urutan penting: ImageButton sebelum Button)
    private val FALLBACK_BY_SUFFIX = listOf(
        "Switch" to "android.widget.Switch",
        "CheckBox" to "android.widget.CheckBox",
        "RadioButton" to "android.widget.RadioButton",
        "ImageButton" to "android.widget.ImageButton",
        "ImageView" to "android.widget.ImageView",
        "EditText" to "android.widget.EditText",
        "SeekBar" to "android.widget.SeekBar",
        "ProgressBar" to "android.widget.ProgressBar",
        "Spinner" to "android.widget.Spinner",
        "TextView" to "android.widget.TextView",
        "Button" to "android.widget.Button",
        "WebView" to "android.webkit.WebView",
        "ListView" to "android.widget.ListView"
    )

    private val defaultHandlers by lazy { PreviewWidgetHandler.createDefaultHandlers() }

    private fun getDexBuildLock(scId: String): Any {
        return synchronized(dexBuildLocks) {
            dexBuildLocks.getOrPut(scId) { Any() }
        }
    }

    private class DexListEntry(val files: List<File>, val at: Long)
    private class BoolEntry(val value: Boolean, val at: Long)

    private val dexListCache = HashMap<String, DexListEntry>()
    private val compiledCache = HashMap<String, BoolEntry>()
    private val staleCache = HashMap<String, BoolEntry>()
    private val fqnMissCache = HashMap<String, Long>()

    private val xmlFactory: XmlPullParserFactory by lazy { XmlPullParserFactory.newInstance() }

    private fun ensureDexFromClasses(scId: String, binDir: File, context: Context): File? {
        // Lock per-scId: kalau dua thread masuk bersamaan untuk scId yang sama, yang kedua tunggu
        // bukan langsung compile ulang
        return synchronized(getDexBuildLock(scId)) {
            val classesDir = File(binDir, "classes")
            val cacheClassesDir = File(binDir.parentFile, "build_cache/classes")

            val sourceDir = when {
                classesDir.isDirectory && classesDir.walkTopDown()
                    .any { it.isFile && it.extension == "class" } -> classesDir
                cacheClassesDir.isDirectory && cacheClassesDir.walkTopDown()
                    .any { it.isFile && it.extension == "class" } -> cacheClassesDir
                else -> return@synchronized null
            }

            val classFiles = sourceDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }.toList()
            if (classFiles.isEmpty()) return@synchronized null

            val appCodeCache = context.codeCacheDir ?: context.cacheDir
            val previewDexDir =
                File(appCodeCache, "preview_dex_$scId").apply { if (!exists()) mkdirs() }
            val targetDex = File(previewDexDir, "preview_classes.dex")

            val latestClassTime = classFiles.maxOf { it.lastModified() }
            if (targetDex.exists() && targetDex.lastModified() >= latestClassTime && targetDex.length() > 0) {
                return@synchronized targetDex
            }

            try {
                val tempDir = File(previewDexDir, "temp_dex").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }

                val builder = D8Command.builder()
                    .setIntermediate(false)
                    .setMode(CompilationMode.DEBUG)
                    .setMinApiLevel(21)
                    .setOutput(tempDir.toPath(), OutputMode.DexIndexed)

                for (cf in classFiles) builder.addProgramFiles(cf.toPath())

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
                } else null
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to compile classes to DEX for preview: ${e.message}")
                null
            }
        }
    }

    @JvmStatic
    fun findProjectDexFiles(scId: String): List<File> {
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cacheLock) { dexListCache[scId] }
        if (cached != null && now - cached.at < CACHE_TTL_MS && cached.files.all { it.exists() }) {
            return cached.files
        }
        val fresh = scanProjectDexFiles(scId)
        synchronized(cacheLock) {
            dexListCache[scId] = DexListEntry(fresh, SystemClock.elapsedRealtime())
        }
        return fresh
    }

    private fun scanProjectDexFiles(scId: String): List<File> {
        val list = mutableListOf<File>()
        try {
            val context = SketchApplication.getContext()
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            ensureDexFromClasses(scId, binDir, context)?.let { list.add(it) }

            val dexDir = File(binDir, "dex")
            if (dexDir.isDirectory) {
                dexDir.listFiles { _, name -> name.endsWith(".dex") }?.forEach {
                    if (!list.contains(it)) list.add(it)
                }
            }

            val rootDex = File(binDir, "classes.dex")
            if (rootDex.isFile && !list.contains(rootDex)) list.add(rootDex)

            try {
                val dexsDir = BuiltInLibraries.EXTRACTED_BUILT_IN_LIBRARY_DEX_FILES_PATH
                if (dexsDir.isDirectory) {
                    dexsDir.listFiles { _, name -> name.endsWith(".dex") }?.forEach {
                        if (!list.contains(it)) list.add(it)
                    }
                }
            } catch (ignored: Throwable) {}

            try {
                val userLibDexDir = File(FilePathUtil().getDexPathLocalLibraryUser(scId))
                if (userLibDexDir.isDirectory) {
                    userLibDexDir.listFiles { _, name -> name.endsWith(".dex") }?.forEach {
                        if (!list.contains(it)) list.add(it)
                    }
                }
            } catch (ignored: Throwable) {}

            try {
                val globalLibs = File(
                    Environment.getExternalStorageDirectory(),
                    ".sketchware/libs/local_libs"
                )
                if (globalLibs.isDirectory) {
                    globalLibs.listFiles()?.forEach { libFolder ->
                        if (libFolder.isDirectory) {
                            val libDex = File(libFolder, "classes.dex")
                            if (libDex.isFile && !list.contains(libDex)) list.add(libDex)
                        }
                    }
                }
            } catch (ignored: Throwable) {}

            if (binDir.isDirectory) {
                binDir.listFiles { _, name ->
                    name.endsWith(".apk") || name.endsWith(".apk.unsigned")
                }?.forEach { if (!list.contains(it)) list.add(it) }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error finding project dex files for $scId: ${e.message}")
        }
        return list
    }

    @JvmStatic
    fun isCompiled(scId: String?): Boolean {
        if (scId.isNullOrEmpty()) return false
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cacheLock) { compiledCache[scId] }
        if (cached != null && now - cached.at < CACHE_TTL_MS) return cached.value

        val value = computeIsCompiled(scId)
        synchronized(cacheLock) {
            compiledCache[scId] = BoolEntry(value, SystemClock.elapsedRealtime())
        }
        return value
    }

    private fun computeIsCompiled(scId: String): Boolean {
        try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            val classesDir = File(binDir, "classes")
            if (classesDir.isDirectory &&
                classesDir.walkTopDown().any { it.isFile && it.extension == "class" }
            ) return true

            val cacheClasses = File(myscPath, "build_cache/classes")
            if (cacheClasses.isDirectory &&
                cacheClasses.walkTopDown().any { it.isFile && it.extension == "class" }
            ) return true

            val dexDir = File(binDir, "dex")
            if (dexDir.isDirectory &&
                dexDir.listFiles { _, name -> name.endsWith(".dex") }?.isNotEmpty() == true
            ) return true

            if (File(binDir, "classes.dex").isFile) return true

            if (binDir.listFiles { _, name ->
                    name.endsWith(".apk") || name.endsWith(".apk.unsigned")
                }?.isNotEmpty() == true
            ) return true
        } catch (ignored: Throwable) {}
        return false
    }

    @JvmStatic
    fun isCustomClass(className: String?): Boolean {
        if (className.isNullOrBlank()) return false
        when (className) {
            "View", "ViewGroup", "LinearLayout", "RelativeLayout", "FrameLayout",
            "ScrollView", "HorizontalScrollView", "HScrollView", "TextView", "Button",
            "ImageView", "ImageButton", "EditText", "CheckBox", "RadioButton",
            "Switch", "SeekBar", "ProgressBar", "Spinner", "ListView", "GridView",
            "include" -> return false
        }
        if (className.startsWith("android.") ||
            className.startsWith("androidx.") ||
            className.startsWith("com.google.android.material.") ||
            className.startsWith("com.google.android.gms.")
        ) return false
        return true
    }

    /**
     * Mengembalikan nama class widget standar untuk custom view yang namanya diakhiri
     * nama widget standar (misal CustomSwitch -> android.widget.Switch).
     * Mengembalikan null kalau bukan custom view atau tidak ada padanannya.
     */
    @JvmStatic
    fun standardFallbackClass(className: String?): String? {
        if (className.isNullOrBlank()) return null
        val simple = className.substringAfterLast('.')
        if (!isCustomClass(simple)) return null
        return FALLBACK_BY_SUFFIX.firstOrNull { simple.endsWith(it.first) }?.second
    }

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

            val loader = ChildFirstDexClassLoader(
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

    @JvmStatic
    @Synchronized
    fun resolveClassFqn(scId: String, simpleName: String): String? {
        val key = "$scId:$simpleName"
        val now = SystemClock.elapsedRealtime()
        val missAt = synchronized(cacheLock) { fqnMissCache[key] }
        if (missAt != null && now - missAt < MISS_TTL_MS) return null

        val result = resolveClassFqnUncached(scId, simpleName)
        synchronized(cacheLock) {
            if (result == null) fqnMissCache[key] =
                SystemClock.elapsedRealtime() else fqnMissCache.remove(key)
        }
        return result
    }

    private fun resolveClassFqnUncached(scId: String, simpleName: String): String? {
        if (fqnMapScId != scId) {
            fqnMap.clear()
            fqnMapScId = scId
            fqnIndexScId = null
        }

        fqnMap[simpleName]?.let { return it }
        if (fqnIndexScId == scId) return null

        try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            val searchDirs =
                listOf(File(binDir, "classes"), File(myscPath, "build_cache/classes"))
            for (dir in searchDirs) {
                if (dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile && it.extension == "class" }
                        .forEach { file ->
                            val rel = file.relativeTo(dir).path
                            val fqn =
                                rel.removeSuffix(".class").replace('/', '.').replace('\\', '.')
                            val short = fqn.substringAfterLast('.')
                            fqnMap[short] = fqn
                            if (short == simpleName) return fqn
                        }
                }
            }

            val dexFiles = findProjectDexFiles(scId)
            for (dexFile in dexFiles) {
                try {
                    @Suppress("DEPRECATION")
                    val df = DexFile(dexFile)
                    val entries = df.entries()
                    while (entries.hasMoreElements()) {
                        val className = entries.nextElement()
                        val short = className.substringAfterLast('.')
                        if (!fqnMap.containsKey(short)) fqnMap[short] = className
                        if (short == simpleName) return className
                    }
                } catch (ignored: Throwable) {}
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error resolving class FQN for $simpleName in $scId: ${e.message}")
        }

        fqnIndexScId = scId
        return fqnMap[simpleName]
    }

    @JvmStatic
    @JvmOverloads
    fun loadClass(context: Context, className: String, scId: String? = null): Class<*>? {
        if (className.isBlank()) return null
        val targetScId = scId ?: currentScId
        val cleanName = className.trim()

        if (!targetScId.isNullOrEmpty()) {
            try {
                val loader = getClassLoader(context, targetScId)
                if (loader != null) {
                    try {
                        return loader.loadClass(cleanName)
                    } catch (ignored: ClassNotFoundException) {
                    } catch (ignored: NoClassDefFoundError) {}

                    if (!cleanName.contains('.')) {
                        val resolvedFqn = resolveClassFqn(targetScId, cleanName)
                        if (resolvedFqn != null) {
                            try {
                                return loader.loadClass(resolvedFqn)
                            } catch (ignored: Throwable) {}
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(
                    TAG,
                    "Error loading class $cleanName from project $targetScId: ${e.message}"
                )
            }
        }

        try {
            return context.classLoader.loadClass(cleanName)
        } catch (ignored: Throwable) {}

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
                } catch (ignored: Throwable) {}
            }
        }

        return null
    }

    @JvmStatic
    @JvmOverloads
    fun resolveViewType(
        className: String,
        scId: String? = null,
        context: Context? = null
    ): Int {
        // Custom view yang punya padanan widget standar langsung pakai tipe widget standar
        val fallback = standardFallbackClass(className)
        if (fallback != null) {
            val simple = fallback.substringAfterLast('.')
            return defaultHandlers.firstOrNull { it.matchesExact(simple) }?.viewType
                ?: ViewBean.VIEW_TYPE_LAYOUT_LINEAR
        }

        val ctx = context ?: SketchApplication.getContext()
        val clazz = loadClass(ctx, className, scId)
        if (clazz == null) return ViewBean.VIEW_TYPE_LAYOUT_LINEAR
        if (!View::class.java.isAssignableFrom(clazz)) return ViewBean.VIEW_TYPE_LAYOUT_LINEAR

        var current: Class<*>? = clazz
        while (current != null && current != View::class.java && current != Any::class.java) {
            val simpleName = current.simpleName
            val registryType = PreviewViewRegistry.typeOf(simpleName, -1)
            if (registryType != -1 && registryType != ViewBean.VIEW_TYPE_LAYOUT_LINEAR) return registryType
            val considered = ViewBeanFactory.getConsideredTypeViewByName(simpleName, -1)
            if (considered != -1 && considered != ViewBean.VIEW_TYPE_LAYOUT_LINEAR) return considered
            val typeByName = ViewBean.getViewTypeByTypeName(simpleName)
            if (typeByName != -1 && typeByName != ViewBean.VIEW_TYPE_LAYOUT_LINEAR) return typeByName
            current = current.superclass
        }
        return ViewBean.VIEW_TYPE_LAYOUT_LINEAR
    }

    private fun gravityToString(gravity: Int): String {
        return when (gravity) {
            Gravity.CENTER -> "center"
            Gravity.CENTER_HORIZONTAL -> "center_horizontal"
            Gravity.CENTER_VERTICAL -> "center_vertical"
            Gravity.START -> "start"
            Gravity.END -> "end"
            Gravity.TOP -> "top"
            Gravity.BOTTOM -> "bottom"
            Gravity.START or Gravity.TOP -> "start|top"
            Gravity.START or Gravity.BOTTOM -> "start|bottom"
            Gravity.END or Gravity.TOP -> "end|top"
            Gravity.END or Gravity.BOTTOM -> "end|bottom"
            else -> "start"
        }
    }

    private fun String.xmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun createAttributeSet(bean: ViewBean): AttributeSet? {
        return try {
            val sb = StringBuilder()
            sb.append("<${bean.convert.substringAfterLast('.')} ")
            sb.append("xmlns:android=\"http://schemas.android.com/apk/res/android\" ")

            val w = when (bean.layout.width) {
                LayoutBean.LAYOUT_MATCH_PARENT -> "match_parent"
                LayoutBean.LAYOUT_WRAP_CONTENT -> "wrap_content"
                else -> "${bean.layout.width}dp"
            }
            val h = when (bean.layout.height) {
                LayoutBean.LAYOUT_MATCH_PARENT -> "match_parent"
                LayoutBean.LAYOUT_WRAP_CONTENT -> "wrap_content"
                else -> "${bean.layout.height}dp"
            }
            sb.append("android:layout_width=\"$w\" ")
            sb.append("android:layout_height=\"$h\" ")

            // Background
            val bg = bean.layout.backgroundResource
            if (!bg.isNullOrEmpty()) {
                val bgValue = if (bg.startsWith("@") || bg.startsWith("#") || bg.startsWith("?")) bg
                else "@drawable/$bg"
                sb.append("android:background=\"$bgValue\" ")
            }

            // Padding
            if (bean.layout.paddingLeft > 0) sb.append("android:paddingLeft=\"${bean.layout.paddingLeft}dp\" ")
            if (bean.layout.paddingTop > 0) sb.append("android:paddingTop=\"${bean.layout.paddingTop}dp\" ")
            if (bean.layout.paddingRight > 0) sb.append("android:paddingRight=\"${bean.layout.paddingRight}dp\" ")
            if (bean.layout.paddingBottom > 0) sb.append("android:paddingBottom=\"${bean.layout.paddingBottom}dp\" ")

            // Gravity — sering dipakai custom view buat posisi konten internal
            val grav = bean.layout.layoutGravity
            if (grav != LayoutBean.GRAVITY_NONE) {
                sb.append("android:gravity=\"${gravityToString(grav)}\" ")
            }

            // Text properties — custom view yng extend TextView atau baca attrs text
            try {
                val textVal = bean.text?.text
                if (!textVal.isNullOrEmpty()) sb.append("android:text=\"${textVal.xmlEscape()}\" ")

                val textColor = bean.text?.textColor ?: 0
                if (textColor != 0) sb.append("android:textColor=\"#${Integer.toHexString(textColor)}\" ")

                val textSize = bean.text?.textSize ?: 0
                if (textSize > 0) sb.append("android:textSize=\"${textSize}sp\" ")
            } catch (ignored: Throwable) {
                // text bean mungkin null
            }

            sb.append("/>")

            val parser = xmlFactory.newPullParser()
            parser.setInput(StringReader(sb.toString()))
            var type = 0
            while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                type = parser.next()
            }
            Xml.asAttributeSet(parser)
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun createView(
        context: Context,
        className: String,
        scId: String? = null,
        bean: ViewBean? = null,
        errorHolder: Array<Throwable?>? = null
    ): View? {
        if (className.isBlank()) return null
        val targetScId = scId ?: currentScId

        val wrappedContext =
            if (targetScId != null) ProjectContextWrapper(context, targetScId) else context
        val clazz = loadClass(wrappedContext, className, targetScId)

        if (clazz == null) {
            errorHolder?.set(
                0, ClassNotFoundException(
                    "Class '$className' tidak ditemukan. Pastikan project sudah di-Compile."
                )
            )
            return null
        }
        if (!View::class.java.isAssignableFrom(clazz)) {
            errorHolder?.set(
                0, IllegalArgumentException("'$className' bukan turunan dari View.")
            )
            return null
        }

        val attrSet = if (bean != null) createAttributeSet(bean) else null

        fun unwrap(t: Throwable): Throwable =
            if (t is java.lang.reflect.InvocationTargetException) t.targetException else t

        // (Context, AttributeSet)
        try {
            val ctor =
                clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext, attrSet) as View
        } catch (t: Throwable) {
            errorHolder?.set(0, unwrap(t))
        }

        // (Context)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext) as View
        } catch (t: Throwable) {
            if (errorHolder?.get(0) == null) errorHolder?.set(0, unwrap(t))
        }

        // (Context, AttributeSet, Int)
        try {
            val ctor = clazz.getDeclaredConstructor(
                Context::class.java,
                AttributeSet::class.java,
                Int::class.javaPrimitiveType ?: Integer.TYPE
            )
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext, attrSet, 0) as View
        } catch (t: Throwable) {
            if (errorHolder?.get(0) == null) errorHolder?.set(0, unwrap(t))
        }

        // (Context, AttributeSet, Int, Int)
        try {
            val ctor = clazz.getDeclaredConstructor(
                Context::class.java,
                AttributeSet::class.java,
                Int::class.javaPrimitiveType ?: Integer.TYPE,
                Int::class.javaPrimitiveType ?: Integer.TYPE
            )
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext, attrSet, 0, 0) as View
        } catch (t: Throwable) {
            if (errorHolder?.get(0) == null) errorHolder?.set(0, unwrap(t))
        }

        return null
    }

    @JvmStatic
    fun fallbackView(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
    }

    /**
     * Membuat view untuk item preview.
     * Custom view yang punya padanan widget standar ditampilkan sebagai widget standar,
     * tanpa memuat class hasil compile, sehingga tidak perlu Compile dulu.
     */
    @JvmStatic
    fun createItemView(context: Context, viewBean: ViewBean, scId: String?): View {
        val targetScId = scId ?: currentScId
        val errorHolder = arrayOfNulls<Throwable>(1)

        val viewName = standardFallbackClass(viewBean.convert) ?: viewBean.convert
        val realView = createView(context, viewName, null, viewBean, errorHolder)

        return ItemCustomView(context, realView, viewBean, errorHolder[0], targetScId, false)
    }

    /**
     * Hanya dipanggil saat build selesai (finishUI / onPostExecute BuildTask),
     * BUKAN saat pindah tab atau pindah XML file.
     */
    @JvmStatic
    fun clearCache(scId: String? = null) {
        synchronized(cacheLock) {
            if (scId == null) {
                dexListCache.clear()
                compiledCache.clear()
                staleCache.clear()
                fqnMissCache.clear()
            } else {
                dexListCache.remove(scId)
                compiledCache.remove(scId)
                staleCache.remove(scId)
                fqnMissCache.keys.removeAll { it.startsWith("$scId:") }
            }
        }
        ProjectResourcesCache.clear(scId)

        if (scId == null || scId == cachedScId) {
            cachedScId = null
            cachedClassLoader = null
            cachedDexTimestamp = 0L
            fqnMap.clear()
            fqnMapScId = null
            fqnIndexScId = null
        }
    }
}