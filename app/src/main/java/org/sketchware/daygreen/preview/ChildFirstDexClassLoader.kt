package org.sketchware.daygreen.preview

import a.a.a.wq
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.SystemClock
import android.util.Log
import android.view.ContextThemeWrapper
import com.besome.sketch.editor.manage.library.material3.Material3LibraryManager
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Custom ClassLoader untuk mengutamakan Class dari Dex project (contoh: library AndroidX, Material, dsb)
 * dibandingkan Class milik editor, mencegah error duplikasi namespace atau cast exception.
 */
class ChildFirstDexClassLoader(
    dexPath: String,
    optDir: String,
    libPath: String?,
    parent: ClassLoader
) : DexClassLoader(dexPath, optDir, libPath, parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // Sudah pernah di-load lewat loader ini (di-define di sini, atau didelegasikan ke parent
        // sebelumnya)? Pakai lagi. Tanpa ini, tiap lookup ulang selalu jalan ke findClass() yang
        // melempar ClassNotFoundException untuk semua class milik parent.
        findLoadedClass(name)?.let { return it }

        // Class platform tidak mungkin ada di dex project, langsung ke parent.
        // Urutan saja yang berubah: super.loadClass tetap jatuh ke dex project kalau parent tidak punya.
        if (name.startsWith("android.") || name.startsWith("java.") ||
            name.startsWith("javax.") || name.startsWith("dalvik.") ||
            name.startsWith("libcore.")
        ) {
            return super.loadClass(name, resolve)
        }
        try {
            return findClass(name)
        } catch (ignored: ClassNotFoundException) {
        }
        return super.loadClass(name, resolve)
    }
}

/**
 * Cache Resources (AssetManager project) dan theme overlay Material3 per scId.
 *
 * Sebelumnya tiap custom view membuat AssetManager + Resources + Material3LibraryManager baru,
 * padahal isinya sama untuk satu project. Sekarang dibuat sekali lalu dipakai bersama.
 *
 * Kunci cache : scId (+ uiMode untuk Resources, karena configuration night/day ikut tersalin).
 * Invalidate  : - Resources: otomatis kalau path/lastModified file APK project berubah
 *                 (dicek paling sering tiap [RES_RECHECK_MS]).
 *               - Theme overlay: kedaluwarsa tiap [THEME_TTL_MS].
 *               - Manual lewat [clear] (dipanggil dari PreviewClass.clearCache).
 */
object ProjectResourcesCache {
    private const val RES_RECHECK_MS = 2000L
    private const val THEME_TTL_MS = 5000L

    private class ResEntry(
        val apkPath: String?,
        val apkModified: Long,
        val uiMode: Int,
        val checkedAt: Long,
        val resources: Resources?
    )

    private class ThemeEntry(val resId: Int, val at: Long)

    private val lock = Any()
    private val resCache = HashMap<String, ResEntry>()
    private val themeCache = HashMap<String, ThemeEntry>()

    private fun findApk(scId: String): File? {
        return try {
            val binDir = File(wq.d(scId), "bin")
            binDir.listFiles { _, name ->
                name.endsWith(".apk") || name.endsWith(".apk.unsigned") || name.endsWith(".ap_")
            }?.firstOrNull()?.takeIf { it.exists() }
        } catch (e: Throwable) {
            null
        }
    }

    private fun build(base: Context, apk: File): Resources? {
        return try {
            val assetManager = AssetManager::class.java.newInstance()
            val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
            addAssetPath.invoke(assetManager, apk.absolutePath)
            val superRes = base.resources
            Resources(assetManager, superRes.displayMetrics, superRes.configuration)
        } catch (e: Throwable) {
            Log.w("PreviewClass", "Gagal membuat Resources project", e)
            null
        }
    }

    /** Resources hasil compile project, atau null kalau belum ada APK / gagal dibuat. */
    fun getResources(base: Context, scId: String): Resources? {
        val now = SystemClock.elapsedRealtime()
        val uiMode = base.resources.configuration.uiMode

        val cached = synchronized(lock) { resCache[scId] }
        if (cached != null && cached.uiMode == uiMode && now - cached.checkedAt < RES_RECHECK_MS) {
            return cached.resources
        }

        val apk = findApk(scId)
        val apkPath = apk?.absolutePath
        val apkModified = apk?.lastModified() ?: 0L

        // APK sama persis -> pakai ulang Resources lama, cukup perbarui waktu pengecekan.
        if (cached != null && cached.uiMode == uiMode && cached.apkPath == apkPath && cached.apkModified == apkModified) {
            synchronized(lock) {
                resCache[scId] = ResEntry(apkPath, apkModified, uiMode, now, cached.resources)
            }
            return cached.resources
        }

        val res = if (apk != null) build(base, apk) else null
        synchronized(lock) {
            resCache[scId] = ResEntry(apkPath, apkModified, uiMode, now, res)
        }
        return res
    }

    /** Resource id theme overlay Material3 untuk editor view project ini. */
    fun getThemeOverlay(base: Context, scId: String): Int {
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(lock) { themeCache[scId] }
        if (cached != null && now - cached.at < THEME_TTL_MS) return cached.resId

        val id: Int = Material3LibraryManager(base, scId).viewEditorThemeOverlay
        synchronized(lock) { themeCache[scId] = ThemeEntry(id, now) }
        return id
    }

    /** Hapus cache satu project, atau semua kalau [scId] null. */
    fun clear(scId: String? = null) {
        synchronized(lock) {
            if (scId == null) {
                resCache.clear()
                themeCache.clear()
            } else {
                resCache.remove(scId)
                themeCache.remove(scId)
            }
        }
    }
}

/**
 * Context Wrapper khusus yang menyajikan Resource (AssetManager) hasil aapt2 project dan Theme Material3 project
 * sehingga pemanggilan R.drawable, R.color, dll, akan ditarik dari hasil compile (APK project).
 *
 * Resources dan theme overlay diambil dari [ProjectResourcesCache], jadi membuat wrapper ini murah.
 */
class ProjectContextWrapper(base: Context, scId: String) :
    ContextThemeWrapper(base, ProjectResourcesCache.getThemeOverlay(base, scId)) {
    private var projectResources: Resources? = null
    private var projectTheme: Resources.Theme? = null

    init {
        try {
            val res = ProjectResourcesCache.getResources(base, scId)
            if (res != null) {
                projectResources = res
                val theme = res.newTheme()
                theme.setTo(super.getTheme())
                projectTheme = theme
            }
        } catch (e: Exception) {
            Log.w("PreviewClass", "Gagal inisialisasi context wrapper project", e)
        }
    }

    override fun getResources(): Resources = projectResources ?: super.getResources()
    override fun getAssets(): AssetManager = projectResources?.assets ?: super.getAssets()
    override fun getTheme(): Resources.Theme = projectTheme ?: super.getTheme()
}