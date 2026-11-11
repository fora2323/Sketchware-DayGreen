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

    /**
     * true  = dex library (built-in / local libs) baru ikut di-load kalau memang dibutuhkan
     *         (class tidak ketemu di dex project dan tidak ada di editor).
     * false = perilaku lama: semua dex library selalu di-load.
     * Kalau preview jadi aneh setelah update ini, set false untuk membandingkan.
     */
    private const val LAZY_LIBS = true

    @JvmStatic
    var currentScId: String? = null

    private var cachedScId: String? = null
    private var cachedWithLibs: Boolean = false
    private var cachedClassLoader: ClassLoader? = null
    private var cachedDexTimestamp: Long = 0L

    // Index nama pendek -> FQN. Dibangun sekali per project (bukan tiap lookup).
    private val fqnMap = HashMap<String, String>()
    private var fqnMapScId: String? = null
    private var fqnClassesIndexed = false
    private var fqnDexIndexed = false

    // Project yang terbukti butuh dex library (di-guard lock object ini).
    private val needsLibs = HashSet<String>()

    private const val CACHE_TTL_MS = 2000L
    private const val STALE_TTL_MS = 10_000L
    private const val MISS_TTL_MS = 3000L
    private const val DEX_FULL_RECHECK_MS = 30_000L

    private val cacheLock = Any()

    // Lock per-scId supaya ensureDexFromClasses tidak dijalankan paralel untuk proyek yang sama
    private val dexBuildLocks = HashMap<String, Any>()

    private fun getDexBuildLock(scId: String): Any {
        return synchronized(dexBuildLocks) {
            dexBuildLocks.getOrPut(scId) { Any() }
        }
    }

    private class DexListEntry(val files: List<File>, val at: Long)
    private class BoolEntry(val value: Boolean, val at: Long)
    private class DexCheck(val sig: Long, val at: Long, val dex: File?)

    /** Dipakai supaya UI bisa membedakan "class tidak ada" dari error saat instansiasi. */
    private class PreviewClassNotFound(message: String) : ClassNotFoundException(message)

    private val dexListCache = HashMap<String, DexListEntry>()
    private val compiledCache = HashMap<String, BoolEntry>()
    private val staleCache = HashMap<String, BoolEntry>()
    private val fqnMissCache = HashMap<String, Long>()
    private val loadMissCache = HashMap<String, Long>()
    private val dexCheckCache = HashMap<String, DexCheck>()
    private val lastBuildSig = HashMap<String, Long>()

    private val xmlFactory: XmlPullParserFactory by lazy { XmlPullParserFactory.newInstance() }

    private val STANDARD_SIMPLE_NAMES = setOf(
        "View", "ViewGroup", "LinearLayout", "RelativeLayout", "FrameLayout",
        "ScrollView", "HorizontalScrollView", "HScrollView", "TextView", "Button",
        "ImageView", "ImageButton", "EditText", "CheckBox", "RadioButton",
        "Switch", "SeekBar", "ProgressBar", "Spinner", "ListView", "GridView",
        "include"
    )

    // ---------------------------------------------------------------------------------------
    // Build signature: murah (tanpa walk). Berubah kalau ada build baru (apk/dex/classes berubah).
    // ---------------------------------------------------------------------------------------

    private fun computeBuildSignature(scId: String): Long {
        return try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")
            var h = 17L
            h = h * 31 + File(binDir, "classes").lastModified()
            h = h * 31 + File(myscPath, "build_cache/classes").lastModified()

            binDir.listFiles()?.sortedBy { it.name }?.forEach { f ->
                val n = f.name
                if (f.isFile && (n.endsWith(".apk") || n.endsWith(".apk.unsigned") ||
                            n.endsWith(".ap_") || n.endsWith(".dex"))
                ) {
                    h = h * 31 + n.hashCode()
                    h = h * 31 + f.lastModified()
                    h = h * 31 + f.length()
                }
            }
            File(binDir, "dex").listFiles { _, name -> name.endsWith(".dex") }
                ?.sortedBy { it.name }?.forEach { f ->
                    h = h * 31 + f.name.hashCode()
                    h = h * 31 + f.lastModified()
                    h = h * 31 + f.length()
                }
            h
        } catch (e: Throwable) {
            0L
        }
    }

    // ---------------------------------------------------------------------------------------
    // DEX dari .class
    // ---------------------------------------------------------------------------------------

    private fun collectClassFiles(dir: File): List<File> {
        return if (dir.isDirectory) {
            dir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()
        } else emptyList()
    }

    private fun ensureDexFromClasses(scId: String, binDir: File, context: Context): File? {
        // Lock per-scId: thread kedua menunggu, bukan compile ulang.
        return synchronized(getDexBuildLock(scId)) {
            val sig = computeBuildSignature(scId)
            val now = SystemClock.elapsedRealtime()
            val prev = synchronized(cacheLock) { dexCheckCache[scId] }

            // Tidak ada build baru -> tidak perlu scan .class lagi.
            if (prev != null && prev.sig == sig && now - prev.at < DEX_FULL_RECHECK_MS) {
                val d = prev.dex
                if (d == null || (d.exists() && d.length() > 0)) return@synchronized d
            }

            val result = buildPreviewDex(scId, binDir, context)
            synchronized(cacheLock) {
                dexCheckCache[scId] = DexCheck(sig, SystemClock.elapsedRealtime(), result)
            }
            result
        }
    }

    private fun buildPreviewDex(scId: String, binDir: File, context: Context): File? {
        // Walk satu kali per sumber (sebelumnya sampai 3x).
        val classFiles = collectClassFiles(File(binDir, "classes")).ifEmpty {
            collectClassFiles(File(binDir.parentFile, "build_cache/classes"))
        }
        if (classFiles.isEmpty()) return null

        val appCodeCache = context.codeCacheDir ?: context.cacheDir
        val previewDexDir =
            File(appCodeCache, "preview_dex_$scId").apply { if (!exists()) mkdirs() }
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

    // ---------------------------------------------------------------------------------------
    // Daftar dex project
    // ---------------------------------------------------------------------------------------

    /** Semua dex (project + library). API lama, tetap dipertahankan. */
    @JvmStatic
    fun findProjectDexFiles(scId: String): List<File> = findProjectDexFiles(scId, true)

    private fun findProjectDexFiles(scId: String, includeLibs: Boolean): List<File> {
        val key = "$scId|$includeLibs"
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cacheLock) { dexListCache[key] }
        if (cached != null && now - cached.at < CACHE_TTL_MS && cached.files.all { it.exists() }) {
            return cached.files
        }
        val fresh = scanProjectDexFiles(scId, includeLibs)
        synchronized(cacheLock) {
            dexListCache[key] = DexListEntry(fresh, SystemClock.elapsedRealtime())
        }
        return fresh
    }

    private fun scanProjectDexFiles(scId: String, includeLibs: Boolean): List<File> {
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

            if (includeLibs) {
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
            }

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
        if (className in STANDARD_SIMPLE_NAMES) return false
        if (className.startsWith("android.") ||
            className.startsWith("androidx.") ||
            className.startsWith("com.google.android.material.") ||
            className.startsWith("com.google.android.gms.")
        ) return false
        return true
    }

    /** Class platform / widget standar: tidak perlu dicari di dex project. */
    private fun isPlatformName(name: String): Boolean {
        return name in STANDARD_SIMPLE_NAMES ||
                name.startsWith("android.") ||
                name.startsWith("java.") ||
                name.startsWith("javax.") ||
                name.startsWith("kotlin.")
    }

    // ---------------------------------------------------------------------------------------
    // ClassLoader
    // ---------------------------------------------------------------------------------------

    /** Satu-satunya tempat loader dibuat. Kalau ChildFirstDexClassLoader dihapus, ganti di sini. */
    private fun newLoader(dexPath: String, optDir: String, parent: ClassLoader): ClassLoader {
        return ChildFirstDexClassLoader(dexPath, optDir, null, parent)
    }

    @Synchronized
    private fun hasLibs(scId: String): Boolean = !LAZY_LIBS || needsLibs.contains(scId)

    /** @return true kalau project ini baru saja ditandai butuh dex library (sebelumnya belum). */
    @Synchronized
    private fun markNeedsLibs(scId: String): Boolean {
        if (!LAZY_LIBS) return false
        return needsLibs.add(scId)
    }

    @JvmStatic
    @Synchronized
    fun getClassLoader(context: Context, scId: String): ClassLoader? {
        return getClassLoaderInternal(context, scId, hasLibs(scId))
    }

    @Synchronized
    private fun getClassLoaderInternal(
        context: Context,
        scId: String,
        withLibs: Boolean
    ): ClassLoader? {
        val dexFiles = findProjectDexFiles(scId, withLibs)
        if (dexFiles.isEmpty()) return null

        val latest = dexFiles.maxOfOrNull { it.lastModified() } ?: 0L
        val stamp = latest * 31 + dexFiles.size

        if (cachedScId == scId && cachedWithLibs == withLibs &&
            cachedClassLoader != null && cachedDexTimestamp == stamp
        ) {
            return cachedClassLoader
        }

        return try {
            val appCodeCache = context.codeCacheDir ?: context.cacheDir
            val internalDexDir = File(appCodeCache, "preview_dex_$scId").apply {
                if (!exists()) mkdirs()
            }

            val copiedPaths = LinkedHashSet<String>()
            for (dex in dexFiles) {
                // Nama target unik per path asal: beberapa dex bernama sama (classes.dex)
                // sebelumnya saling menimpa di folder ini.
                val inPlace = dex.parentFile?.absolutePath == internalDexDir.absolutePath
                val target = if (inPlace) dex else File(
                    internalDexDir,
                    "${Integer.toHexString(dex.absolutePath.hashCode())}_${dex.name}"
                )
                if (!inPlace && (!target.exists() ||
                            target.lastModified() < dex.lastModified() ||
                            target.length() != dex.length())
                ) {
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

            val loader = newLoader(dexPath, optDir.absolutePath, context.classLoader)

            cachedScId = scId
            cachedWithLibs = withLibs
            cachedClassLoader = loader
            cachedDexTimestamp = stamp
            loader
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create DexClassLoader for sc_id=$scId", e)
            null
        }
    }

    // ---------------------------------------------------------------------------------------
    // Resolve nama class (index dibangun sekali, bukan walk tiap lookup)
    // ---------------------------------------------------------------------------------------

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
            fqnClassesIndexed = false
            fqnDexIndexed = false
        }
        fqnMap[simpleName]?.let { return it }

        try {
            if (!fqnClassesIndexed) {
                fqnClassesIndexed = true
                indexClassDirs(scId)
                fqnMap[simpleName]?.let { return it }
            }
            if (!fqnDexIndexed) {
                fqnDexIndexed = true
                indexDexEntries(scId)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error resolving class FQN for $simpleName in $scId: ${e.message}")
        }

        return fqnMap[simpleName]
    }

    private fun indexClassDirs(scId: String) {
        val myscPath = wq.d(scId)
        val binDir = File(myscPath, "bin")
        for (dir in listOf(File(binDir, "classes"), File(myscPath, "build_cache/classes"))) {
            if (!dir.isDirectory) continue
            dir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
                val fqn = file.relativeTo(dir).path
                    .removeSuffix(".class").replace('/', '.').replace('\\', '.')
                val short = fqn.substringAfterLast('.')
                if (!fqnMap.containsKey(short)) fqnMap[short] = fqn
            }
        }
    }

    private fun indexDexEntries(scId: String) {
        for (dexFile in findProjectDexFiles(scId, true)) {
            try {
                @Suppress("DEPRECATION")
                val df = DexFile(dexFile)
                try {
                    val entries = df.entries()
                    while (entries.hasMoreElements()) {
                        val className = entries.nextElement()
                        val short = className.substringAfterLast('.')
                        if (!fqnMap.containsKey(short)) fqnMap[short] = className
                    }
                } finally {
                    try {
                        df.close()
                    } catch (ignored: Throwable) {}
                }
            } catch (ignored: Throwable) {}
        }
    }

    // ---------------------------------------------------------------------------------------
    // Load class
    // ---------------------------------------------------------------------------------------

    private fun loadFromApp(context: Context, name: String): Class<*>? {
        try {
            return context.classLoader.loadClass(name)
        } catch (ignored: Throwable) {}

        if (!name.contains('.')) {
            val prefixes = arrayOf(
                "android.widget.",
                "android.view.",
                "android.webkit.",
                "com.google.android.material."
            )
            for (prefix in prefixes) {
                try {
                    return context.classLoader.loadClass(prefix + name)
                } catch (ignored: Throwable) {}
            }
        }
        return null
    }

    private fun tryLoadWith(
        context: Context,
        scId: String,
        name: String,
        withLibs: Boolean
    ): Class<*>? {
        try {
            val loader = getClassLoaderInternal(context, scId, withLibs) ?: return null
            try {
                return loader.loadClass(name)
            } catch (ignored: ClassNotFoundException) {
            } catch (ignored: NoClassDefFoundError) {}

            if (!name.contains('.')) {
                val resolvedFqn = resolveClassFqn(scId, name)
                if (resolvedFqn != null) {
                    try {
                        return loader.loadClass(resolvedFqn)
                    } catch (ignored: Throwable) {}
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error loading class $name from project $scId: ${e.message}")
        }
        return null
    }

    private fun loadFromProject(context: Context, scId: String, name: String): Class<*>? {
        val missKey = "$scId:$name"
        val now = SystemClock.elapsedRealtime()
        val missAt = synchronized(cacheLock) { loadMissCache[missKey] }
        if (missAt != null && now - missAt < MISS_TTL_MS) return null

        var found = tryLoadWith(context, scId, name, hasLibs(scId))

        if (found == null && LAZY_LIBS && !hasLibs(scId)) {
            // Tidak ada di dex project. Kalau editor sendiri juga tidak punya,
            // baru coba lagi dengan dex library (CircleImageView, dll).
            if (loadFromApp(context, name) == null) {
                found = tryLoadWith(context, scId, name, true)
                if (found != null) markNeedsLibs(scId)
            }
        }

        if (found == null) {
            synchronized(cacheLock) { loadMissCache[missKey] = SystemClock.elapsedRealtime() }
        }
        return found
    }

    @JvmStatic
    @JvmOverloads
    fun loadClass(context: Context, className: String, scId: String? = null): Class<*>? {
        if (className.isBlank()) return null
        val targetScId = scId ?: currentScId
        val cleanName = className.trim()

        if (!targetScId.isNullOrEmpty() && !isPlatformName(cleanName)) {
            loadFromProject(context, targetScId, cleanName)?.let { return it }
        }

        return loadFromApp(context, cleanName)
    }

    @JvmStatic
    @JvmOverloads
    fun resolveViewType(
        className: String,
        scId: String? = null,
        context: Context? = null
    ): Int {
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

    // ---------------------------------------------------------------------------------------
    // AttributeSet
    // ---------------------------------------------------------------------------------------

    /** Konversi flag gravity Android (3, 5, 17, 48, 80, ...) ke string XML. null kalau kosong. */
    private fun gravityToString(gravity: Int): String? {
        if (gravity == LayoutBean.GRAVITY_NONE) return null
        val h = gravity and Gravity.HORIZONTAL_GRAVITY_MASK
        val v = gravity and Gravity.VERTICAL_GRAVITY_MASK
        if (h == Gravity.CENTER_HORIZONTAL && v == Gravity.CENTER_VERTICAL) return "center"

        val parts = ArrayList<String>(2)
        when (h) {
            Gravity.LEFT -> parts.add("left")
            Gravity.RIGHT -> parts.add("right")
            Gravity.CENTER_HORIZONTAL -> parts.add("center_horizontal")
        }
        when (v) {
            Gravity.TOP -> parts.add("top")
            Gravity.BOTTOM -> parts.add("bottom")
            Gravity.CENTER_VERTICAL -> parts.add("center_vertical")
        }
        return if (parts.isEmpty()) null else parts.joinToString("|")
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

            // android:gravity = gravity ISI view (layout.gravity), bukan layoutGravity (posisi di parent)
            val grav = gravityToString(bean.layout.gravity)
            if (grav != null) sb.append("android:gravity=\"$grav\" ")

            // Text: nilai default TextBean (kosong / 0xffffff / 12) tidak ikut dikirim,
            // karena XML runtime juga tidak punya atribut itu kalau user tidak mengisinya.
            try {
                val tb = bean.text
                if (tb != null) {
                    val textVal = tb.text
                    if (!textVal.isNullOrEmpty()) sb.append("android:text=\"${textVal.xmlEscape()}\" ")
                    if ((tb.textColor ushr 24) != 0) {
                        sb.append("android:textColor=\"#${"%08x".format(tb.textColor)}\" ")
                    }
                    if (tb.textSize > 0 && tb.textSize != 12) {
                        sb.append("android:textSize=\"${tb.textSize}sp\" ")
                    }
                }
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

    // ---------------------------------------------------------------------------------------
    // Create view
    // ---------------------------------------------------------------------------------------

    private fun isLinkageError(t: Throwable?): Boolean {
        var cur = t
        var depth = 0
        while (cur != null && depth < 8) {
            if (cur is NoClassDefFoundError || cur is ClassNotFoundException) return true
            cur = cur.cause
            depth++
        }
        return false
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

        val linkage = BooleanArray(1)
        val view = createViewOnce(context, className, targetScId, bean, errorHolder, linkage)
        if (view != null) return view

        // Gagal karena class lain yang dirujuk tidak ada -> kemungkinan butuh dex library.
        // Tandai project ini lalu coba sekali lagi dengan loader yang memuat library.
        if (!targetScId.isNullOrEmpty() && linkage[0] && markNeedsLibs(targetScId)) {
            errorHolder?.set(0, null)
            return createViewOnce(context, className, targetScId, bean, errorHolder, BooleanArray(1))
        }
        return null
    }

    private fun createViewOnce(
        context: Context,
        className: String,
        targetScId: String?,
        bean: ViewBean?,
        errorHolder: Array<Throwable?>?,
        linkage: BooleanArray
    ): View? {
        val wrappedContext =
            if (targetScId != null) ProjectContextWrapper(context, targetScId) else context
        val clazz = loadClass(wrappedContext, className, targetScId)

        if (clazz == null) {
            errorHolder?.set(
                0, PreviewClassNotFound(
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

        val intType: Class<*> = Int::class.javaPrimitiveType ?: Integer.TYPE
        val attempts = listOf<Pair<Array<Class<*>>, Array<Any?>>>(
            arrayOf<Class<*>>(Context::class.java, AttributeSet::class.java) to
                    arrayOf<Any?>(wrappedContext, attrSet),
            arrayOf<Class<*>>(Context::class.java) to
                    arrayOf<Any?>(wrappedContext),
            arrayOf<Class<*>>(Context::class.java, AttributeSet::class.java, intType) to
                    arrayOf<Any?>(wrappedContext, attrSet, 0),
            arrayOf<Class<*>>(Context::class.java, AttributeSet::class.java, intType, intType) to
                    arrayOf<Any?>(wrappedContext, attrSet, 0, 0)
        )

        // Error yang ditampilkan: error pertama yang BUKAN "constructor tidak ada".
        var best: Throwable? = null
        for ((types, args) in attempts) {
            try {
                val ctor = clazz.getDeclaredConstructor(*types)
                ctor.isAccessible = true
                return ctor.newInstance(*args) as View
            } catch (t: Throwable) {
                val e = unwrap(t)
                if (isLinkageError(e)) linkage[0] = true
                if (e is NoSuchMethodException) {
                    if (best == null) best = e
                } else if (best == null || best is NoSuchMethodException) {
                    best = e
                }
            }
        }
        errorHolder?.set(0, best)
        return null
    }

    @JvmStatic
    fun fallbackView(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
    }

    // ---------------------------------------------------------------------------------------
    // Stale check
    // ---------------------------------------------------------------------------------------

    private fun isProjectStale(scId: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cacheLock) { staleCache[scId] }
        if (cached != null && now - cached.at < STALE_TTL_MS) return cached.value

        val value = try {
            val myscPath = wq.d(scId)
            val binDir = File(myscPath, "bin")
            val buildTime = maxOf(
                binDir.listFiles { _, name -> name.endsWith(".apk") || name.endsWith(".apk.unsigned") }
                    ?.maxOfOrNull { it.lastModified() } ?: 0L,
                binDir.listFiles { _, name -> name.endsWith(".dex") }
                    ?.maxOfOrNull { it.lastModified() } ?: 0L
            )

            if (buildTime <= 0L) {
                false
            } else {
                // any{} berhenti di file pertama yang lebih baru dari build.
                listOf(File(myscPath, "app/src/main/java"), File(myscPath, "java")).any { dir ->
                    dir.isDirectory && dir.walkTopDown().any { f ->
                        f.isFile && (f.extension == "java" || f.extension == "kt") &&
                                f.lastModified() > buildTime
                    }
                }
            }
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
            isStale = isProjectStale(targetScId)
            createView(context, viewBean.convert, targetScId, viewBean, errorHolder)
        } else {
            errorHolder[0] = IllegalStateException(
                "Project belum di-Compile. Custom View belum tersedia."
            )
            null
        }

        return ItemCustomView(context, realView, viewBean, errorHolder[0], targetScId, isStale)
    }

    // ---------------------------------------------------------------------------------------
    // Cache
    // ---------------------------------------------------------------------------------------

    /**
     * Aman dipanggil tiap refresh: kalau tidak ada build baru sejak panggilan terakhir
     * (apk/dex/classes sama), tidak ada yang dibuang, jadi loader, resources, dan dex
     * tidak dibuat ulang. Cache baru benar-benar dibersihkan kalau project habis di-build,
     * atau kalau scId == null.
     */
    @JvmStatic
    @Synchronized
    fun clearCache(scId: String? = null) {
        if (scId != null) {
            val sig = computeBuildSignature(scId)
            val unchanged = synchronized(cacheLock) {
                val prev = lastBuildSig[scId]
                lastBuildSig[scId] = sig
                prev != null && prev == sig
            }
            if (unchanged) return
        } else {
            synchronized(cacheLock) { lastBuildSig.clear() }
        }

        synchronized(cacheLock) {
            if (scId == null) {
                dexListCache.clear()
                compiledCache.clear()
                staleCache.clear()
                fqnMissCache.clear()
                loadMissCache.clear()
                dexCheckCache.clear()
            } else {
                dexListCache.remove("$scId|true")
                dexListCache.remove("$scId|false")
                compiledCache.remove(scId)
                staleCache.remove(scId)
                dexCheckCache.remove(scId)
                fqnMissCache.keys.removeAll { it.startsWith("$scId:") }
                loadMissCache.keys.removeAll { it.startsWith("$scId:") }
            }
        }
        ProjectResourcesCache.clear(scId)

        if (scId == null || scId == fqnMapScId) {
            fqnMap.clear()
            fqnMapScId = null
            fqnClassesIndexed = false
            fqnDexIndexed = false
        }

        if (scId == null || scId == cachedScId) {
            cachedScId = null
            cachedWithLibs = false
            cachedClassLoader = null
            cachedDexTimestamp = 0L
        }

        if (scId == null) needsLibs.clear()
    }

    /**
     * Bersihkan paksa (abaikan pengecekan build). Opsional: panggil dari DesignActivity.finish()
     * kalau mau memori loader langsung dilepas saat project ditutup.
     */
    @JvmStatic
    @Synchronized
    fun clearCacheForce(scId: String?) {
        if (scId != null) synchronized(cacheLock) { lastBuildSig.remove(scId) }
        clearCache(scId)
    }
}