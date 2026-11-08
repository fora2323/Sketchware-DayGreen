package org.sketchware.daygreen.preview

import a.a.a.wq
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
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
        if (name.startsWith("android.") || name.startsWith("java.")) {
            return super.loadClass(name, resolve)
        }
        try {
            val c = findClass(name)
            if (c != null) return c
        } catch (ignored: ClassNotFoundException) {
        }
        return super.loadClass(name, resolve)
    }
}

/**
 * Context Wrapper khusus yang menyajikan Resource (AssetManager) hasil aapt2 project dan Theme Material3 project
 * sehingga pemanggilan R.drawable, R.color, dll, akan ditarik dari hasil compile (APK project).
 */
class ProjectContextWrapper(base: Context, scId: String) : ContextThemeWrapper(base, Material3LibraryManager(base, scId).viewEditorThemeOverlay) {
    private var projectResources: Resources? = null
    private var projectTheme: Resources.Theme? = null

    init {
        try {
            val binDir = File(wq.d(scId), "bin")
            val resFile = binDir.listFiles { _, name -> name.endsWith(".apk") || name.endsWith(".apk.unsigned") || name.endsWith(".ap_") }?.firstOrNull()
            if (resFile != null && resFile.exists()) {
                val assetManager = AssetManager::class.java.newInstance()
                val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                addAssetPath.invoke(assetManager, resFile.absolutePath)
                
                val superRes = base.resources
                projectResources = Resources(assetManager, superRes.displayMetrics, superRes.configuration)
                projectTheme = projectResources?.newTheme()
                projectTheme?.setTo(super.getTheme())
            }
        } catch (e: Exception) {
            Log.w("PreviewClass", "Gagal inisialisasi context wrapper project", e)
        }
    }

    override fun getResources(): Resources = projectResources ?: super.getResources()
    override fun getAssets(): AssetManager = projectResources?.assets ?: super.getAssets()
    override fun getTheme(): Resources.Theme = projectTheme ?: super.getTheme()
}