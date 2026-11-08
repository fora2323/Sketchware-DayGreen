package org.sketchware.daygreen.preview

import a.a.a.wq
import android.content.Context
import android.os.Environment
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
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

    // ---------------------------------------------------------------------------------------
    // Cache performa. Dulu semua pemindaian folder/file di bawah diulang untuk SETIAP custom
    // view yang dibuat. Sekarang hasilnya dipakai bersama dan kedaluwarsa singkat, jadi setelah
    // project di-Run ulang preview tetap memakai hasil compile terbaru.
    // Semua cache dibersihkan lewat clearCache(scId).
    // ---------------------------------------------------------------------------------------
    private const val CACHE_TTL_MS = 2000L
    private const val STALE_TTL_MS = 3000L
    private const val MISS_TTL_MS = 3000L

    // Lock khusus cache. Aturan: jangan melakukan kerja berat saat memegang lock ini.
    private val cacheLock = Any()

    private class DexListEntry(val files: List<File>, val at: Long)
    private class BoolEntry(val value: Boolean, val at: Long)

    // Kunci: scId. Isi: daftar dex/apk project. Kedaluwarsa: CACHE_TTL_MS.
    private val dexListCache = HashMap<String, DexListEntry>()

    // Kunci: scId. Isi: hasil isCompiled(). Kedaluwarsa: CACHE_TTL_MS.
    private val compiledCache = HashMap<String, BoolEntry>()

    // Kunci: scId. Isi: apakah source lebih baru dari hasil compile. Kedaluwarsa: STALE_TTL_MS.
    private val staleCache = HashMap<String, BoolEntry>()

    // Kunci: "scId:SimpleName". Isi: waktu kelas itu terakhir TIDAK ditemukan. Kedaluwarsa: MISS_TTL_MS.
    private val fqnMissCache = HashMap<String, Long>()

    // Factory parser dipakai ulang; membuatnya baru tiap view mahal.
    private val xmlFactory: XmlPullParserFactory by lazy { XmlPullParserFactory.newInstance() }

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
     * Daftar dex/apk project. Hasilnya di-cache selama [CACHE_TTL_MS] karena pemindaiannya
     * (termasuk walkTopDown ke semua file .class) mahal dan dipanggil tiap loadClass().
     */
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
                dexDir.listFiles { _, name -> name.endsWith(".dex") }?.let {
                    for (f in it) {
                        if (!list.contains(f)) list.add(f)
                    }
                }
            }

            val rootDex = File(binDir, "classes.dex")
            if (rootDex.isFile && !list.contains(rootDex)) {
                list.add(rootDex)
            }

            try {
                val dexsDir = BuiltInLibraries.EXTRACTED_BUILT_IN_LIBRARY_DEX_FILES_PATH
                if (dexsDir.isDirectory) {
                    dexsDir.listFiles { _, name -> name.endsWith(".dex") }?.let {
                        for (libDex in it) {
                            if (!list.contains(libDex)) list.add(libDex)
                        }
                    }
                }
            } catch (ignored: Throwable) {}

            try {
                val userLibDexDir = File(FilePathUtil().getDexPathLocalLibraryUser(scId))
                if (userLibDexDir.isDirectory) {
                    userLibDexDir.listFiles { _, name -> name.endsWith(".dex") }?.let {
                        for (f in it) {
                            if (!list.contains(f)) list.add(f)
                        }
                    }
                }
            } catch (ignored: Throwable) {}

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
            } catch (ignored: Throwable) {}

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
            if (classesDir.isDirectory && classesDir.walkTopDown().any { it.isFile && it.extension == "class" }) return true

            val cacheClasses = File(myscPath, "build_cache/classes")
            if (cacheClasses.isDirectory && cacheClasses.walkTopDown().any { it.isFile && it.extension == "class" }) return true

            val dexDir = File(binDir, "dex")
            if (dexDir.isDirectory && dexDir.listFiles { _, name -> name.endsWith(".dex") }?.isNotEmpty() == true) return true

            if (File(binDir, "classes.dex").isFile) return true

            if (binDir.listFiles { _, name -> name.endsWith(".apk") || name.endsWith(".apk.unsigned") }?.isNotEmpty() == true) return true
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

            // Memanggil classloader child-first untuk menghindari bentrok
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

    /**
     * Nama sederhana -> nama lengkap kelas. Hasil "tidak ditemukan" juga di-cache sebentar
     * ([MISS_TTL_MS]) supaya kelas yang memang tidak ada tidak memindai folder dan DEX berulang kali.
     */
    @JvmStatic
    @Synchronized
    fun resolveClassFqn(scId: String, simpleName: String): String? {
        val key = "$scId:$simpleName"
        val now = SystemClock.elapsedRealtime()
        val missAt = synchronized(cacheLock) { fqnMissCache[key] }
        if (missAt != null && now - missAt < MISS_TTL_MS) return null

        val result = resolveClassFqnUncached(scId, simpleName)
        synchronized(cacheLock) {
            if (result == null) fqnMissCache[key] = SystemClock.elapsedRealtime() else fqnMissCache.remove(key)
        }
        return result
    }

    private fun resolveClassFqnUncached(scId: String, simpleName: String): String? {
        if (fqnMapScId == scId && fqnMap.containsKey(simpleName)) return fqnMap[simpleName]

        if (fqnMapScId != scId) {
            fqnMap.clear()
            fqnMapScId = scId
        }

        try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")

            val searchDirs = listOf(File(binDir, "classes"), File(myscPath, "build_cache/classes"))
            for (dir in searchDirs) {
                if (dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
                        val rel = file.relativeTo(dir).path
                        val fqn = rel.removeSuffix(".class").replace('/', '.').replace('\\', '.')
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
                Log.w(TAG, "Error loading class $cleanName from project $targetScId: ${e.message}")
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
    fun resolveViewType(className: String, scId: String? = null, context: Context? = null): Int {
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

    /** Helper generasi AttributeSet secara dinamis via XML **/
    private fun createAttributeSet(bean: ViewBean): AttributeSet? {
        try {
            val xml = StringBuilder().apply {
                append("<${bean.convert} xmlns:android=\"http://schemas.android.com/apk/res/android\" ")
                val w = if (bean.layout.width == LayoutBean.LAYOUT_MATCH_PARENT) "match_parent" else if (bean.layout.width == LayoutBean.LAYOUT_WRAP_CONTENT) "wrap_content" else "${bean.layout.width}dp"
                val h = if (bean.layout.height == LayoutBean.LAYOUT_MATCH_PARENT) "match_parent" else if (bean.layout.height == LayoutBean.LAYOUT_WRAP_CONTENT) "wrap_content" else "${bean.layout.height}dp"
                append("android:layout_width=\"$w\" ")
                append("android:layout_height=\"$h\" ")
                
                val bg = bean.layout.backgroundResource
                if (!bg.isNullOrEmpty()) {
                    val bgValue = if (bg.startsWith("@") || bg.startsWith("#") || bg.startsWith("?")) bg else "@drawable/$bg"
                    append("android:background=\"$bgValue\" ")
                }
                if (bean.layout.paddingLeft > 0) append("android:paddingLeft=\"${bean.layout.paddingLeft}dp\" ")
                if (bean.layout.paddingTop > 0) append("android:paddingTop=\"${bean.layout.paddingTop}dp\" ")
                if (bean.layout.paddingRight > 0) append("android:paddingRight=\"${bean.layout.paddingRight}dp\" ")
                if (bean.layout.paddingBottom > 0) append("android:paddingBottom=\"${bean.layout.paddingBottom}dp\" ")
                append("/>")
            }.toString()

            val parser = xmlFactory.newPullParser()
            parser.setInput(StringReader(xml))
            var type = 0
            while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                type = parser.next()
            }
            return Xml.asAttributeSet(parser)
        } catch (e: Exception) {
            return null
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
        
        // Wrap Context agar menggunakan Project Resource
        val wrappedContext = if (targetScId != null) ProjectContextWrapper(context, targetScId) else context
        val clazz = loadClass(wrappedContext, className, targetScId)
        
        if (clazz == null) {
            errorHolder?.set(0, ClassNotFoundException("Class '$className' tidak ditemukan. Pastikan project sudah di-Compile (Run) baru-baru ini."))
            return null
        }
        if (!View::class.java.isAssignableFrom(clazz)) {
            errorHolder?.set(0, IllegalArgumentException("'$className' bukan merupakan turunan dari View."))
            return null
        }

        val attrSet = if (bean != null) createAttributeSet(bean) else null
        
        fun unwrap(t: Throwable): Throwable = if (t is java.lang.reflect.InvocationTargetException) t.targetException else t

        // Prioritas 1: (Context, AttributeSet)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext, attrSet) as View
        } catch (t: Throwable) {
            errorHolder?.set(0, unwrap(t))
        }

        // Prioritas 2: (Context)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext) as View
        } catch (t: Throwable) {
            if (errorHolder?.get(0) == null) errorHolder?.set(0, unwrap(t))
        }

        // Prioritas 3: (Context, AttributeSet, Int)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java, Int::class.javaPrimitiveType ?: Integer.TYPE)
            ctor.isAccessible = true
            return ctor.newInstance(wrappedContext, attrSet, 0) as View
        } catch (t: Throwable) {
            if (errorHolder?.get(0) == null) errorHolder?.set(0, unwrap(t))
        }

        // Prioritas 4: (Context, AttributeSet, Int, Int)
        try {
            val ctor = clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java, Int::class.javaPrimitiveType ?: Integer.TYPE, Int::class.javaPrimitiveType ?: Integer.TYPE)
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
     * True kalau ada source yang lebih baru dari hasil compile terakhir. Memindai semua source
     * project itu mahal, jadi hasilnya di-cache per scId selama [STALE_TTL_MS].
     */
    private fun isProjectStale(scId: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cacheLock) { staleCache[scId] }
        if (cached != null && now - cached.at < STALE_TTL_MS) return cached.value

        val value = try {
            val myscPath = wq.d(scId)
            val srcDirs = listOf(File(myscPath, "app/src/main/java"), File(myscPath, "java"))
            val latestSrc = srcDirs.flatMap { dir ->
                if (dir.exists()) dir.walkTopDown().filter { it.extension == "java" || it.extension == "kt" }.toList()
                else emptyList()
            }.maxOfOrNull { it.lastModified() } ?: 0L

            val binDir = File(myscPath, "bin")
            val buildTime = maxOf(
                binDir.listFiles { _, name -> name.endsWith(".apk") || name.endsWith(".apk.unsigned") }?.maxOfOrNull { it.lastModified() } ?: 0L,
                binDir.listFiles { _, name -> name.endsWith(".dex") }?.maxOfOrNull { it.lastModified() } ?: 0L
            )

            buildTime > 0 && latestSrc > buildTime
        } catch (ignored: Throwable) {
            false
        }

        synchronized(cacheLock) {
            staleCache[scId] = BoolEntry(value, SystemClock.elapsedRealtime())
        }
        return value
    }

    @JvmStatic
    fun createItemView(context: Context, viewBean: ViewBean, scId: String?): View {
        val targetScId = scId ?: currentScId
        val errorHolder = arrayOfNulls<Throwable>(1)
        var isStale = false

        val realView = if (!targetScId.isNullOrEmpty() && isCompiled(targetScId)) {
            // [F] Cache / Data stale logic checker (di-cache per project, bukan per view)
            isStale = isProjectStale(targetScId)

            createView(context, viewBean.convert, targetScId, viewBean, errorHolder)
        } else {
            errorHolder[0] = IllegalStateException("Project belum pernah di-Compile (Build/Run). Custom View belum tersedia.")
            null
        }
        
        return ItemCustomView(context, realView, viewBean, errorHolder[0], targetScId, isStale)
    }

    @JvmStatic
    fun clearCache(scId: String? = null) {
        // Cache performa: bersihkan untuk project ini (atau semua kalau scId null).
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
        }
    }
}