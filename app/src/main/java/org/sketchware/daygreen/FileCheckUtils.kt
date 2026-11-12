package org.sketchware.daygreen

import android.content.Context
import android.text.format.Formatter
import android.util.Log
import java.io.File
import mod.jbk.build.BuiltInLibraries

object FileCheckUtils {
    private const val TAG = "FileCheckUtils"

    @JvmStatic
    fun getLatestSdkToolsDir(): File? {
        val context = pro.sketchware.SketchApplication.getContext()
        val binDir = File(context.filesDir, "bin")
        if (!binDir.exists() || !binDir.isDirectory) return null
        
        val versions = binDir.listFiles { file -> file.isDirectory && file.name.matches(Regex("\\d+\\.\\d+\\.\\d+")) }
        if (versions == null || versions.isEmpty()) return null
        
        return versions.maxByOrNull { 
            val parts = it.name.split(".")
            (parts.getOrElse(0) { "0" }.toLong() * 1000000) + 
            (parts.getOrElse(1) { "0" }.toLong() * 1000) + 
            parts.getOrElse(2) { "0" }.toLong()
        }
    }

    @JvmStatic
    fun isAaptDownloaded(context: Context): Boolean {
        val sdkToolsDir = getLatestSdkToolsDir()
        val aapt2Binary = File(sdkToolsDir ?: File(context.filesDir, "bin"), "aapt2")
        return aapt2Binary.exists() && aapt2Binary.length() > 0
    }

    @JvmStatic
    fun isSdkDownloaded(context: Context): Boolean {
        val legacySdk = File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android.jar")
        if (legacySdk.exists() && legacySdk.length() > 0) return true
        
        val libsDir = BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH
        return libsDir.exists() && libsDir.listFiles { file -> 
            file.isFile && file.name.startsWith("android-") && file.name.endsWith(".jar") && file.length() > 0
        }?.isNotEmpty() == true
    }

    @JvmStatic
    fun isNdkDownloaded(context: Context): Boolean {
        val toolsDir = File(context.filesDir, "native")
        val toolchainFile = mod.jbk.build.compiler.native_code.NativeCompiler.findToolchainFile(toolsDir)
        if (toolchainFile != null && toolchainFile.exists() && toolchainFile.length() > 0) return true
        
        val ndkDir = File(context.filesDir, "native/ndk")
        if (ndkDir.exists() && (File(ndkDir, "source.properties").exists() || File(ndkDir, "bin").exists() || File(ndkDir, "toolchains").exists())) return true

        val binNdkDir = File(context.filesDir, "bin/android-ndk")
        return binNdkDir.exists() && (File(binNdkDir, "source.properties").exists() || File(binNdkDir, "bin").exists() || File(binNdkDir, "toolchains").exists())
    }

    @JvmStatic
    fun isCmakeDownloaded(context: Context): Boolean {
        val toolsDir = File(context.filesDir, "native")
        val cmakeBinary = mod.jbk.build.compiler.native_code.NativeCompiler.findCmakeBinary(toolsDir)
        return cmakeBinary != null && cmakeBinary.exists() && cmakeBinary.length() > 0
    }

    fun getToolStatus(isInstalled: Boolean): String {
        return if (isInstalled) "Installed" else "Not Installed"
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        var size: Long = 0
        val files = file.listFiles()
        if (files != null) {
            for (f in files) {
                size += getFolderSize(f)
            }
        }
        return size
    }

    private fun formatFileSize(context: Context, sizeInBytes: Long): String {
        return Formatter.formatShortFileSize(context, sizeInBytes)
    }

    @JvmStatic
    fun getNdkSize(context: Context): String {
        val ndkDir = File(context.filesDir, "native/ndk")
        if (ndkDir.exists()) {
            return formatFileSize(context, getFolderSize(ndkDir))
        }
        val binNdkDir = File(context.filesDir, "bin/android-ndk")
        if (binNdkDir.exists()) {
            return formatFileSize(context, getFolderSize(binNdkDir))
        }
        return "~360 MB"
    }

    @JvmStatic
    fun getCmakeSize(context: Context): String {
        val cmakeDir = File(context.filesDir, "native/cmake")
        if (cmakeDir.exists()) {
            return formatFileSize(context, getFolderSize(cmakeDir))
        }
        val binCmakeDir = File(context.filesDir, "bin/cmake")
        if (binCmakeDir.exists()) {
            return formatFileSize(context, getFolderSize(binCmakeDir))
        }
        return "~48 MB"
    }

    @JvmStatic
    fun getAaptSize(context: Context): String {
        val aaptFile = File(context.filesDir, "bin/aapt2")
        if (aaptFile.exists()) {
            return formatFileSize(context, aaptFile.length())
        }
        return "~5 MB"
    }

    @JvmStatic
    fun getSdkVersionSize(context: Context, version: String): String {
        if (version.isNotEmpty()) {
            val sdkFile = File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android-$version.jar")
            if (sdkFile.exists()) {
                return formatFileSize(context, sdkFile.length())
            }
        }
        return "~30 MB"
    }

    @JvmStatic
    fun isSdkVersionDownloaded(version: String): Boolean {
        if (version.isEmpty()) return true
        val sdkFile = File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android-$version.jar")
        return sdkFile.exists() && sdkFile.length() > 0
    }

    @JvmStatic
    fun getInstalledSdks(context: Context): List<Int> {
        val libsDir = File(context.filesDir, "libs")
        if (!libsDir.exists()) return emptyList()
        
        return libsDir.listFiles { file -> file.isFile && file.name.startsWith("android-") && file.name.endsWith(".jar") && file.length() > 0 }
            ?.mapNotNull { file ->
                file.name.removePrefix("android-").removeSuffix(".jar").toIntOrNull()
            }?.sorted() ?: emptyList()
    }

    @JvmStatic
    fun getDefaultAndroidJar(context: Context): File? {
        val legacy = File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android.jar")
        if (legacy.exists() && legacy.length() > 0) {
            return legacy
        }
        val installedSdks = getInstalledSdks(context)
        if (installedSdks.isNotEmpty()) {
            val highestSdk = installedSdks.last()
            val fallbackJar = File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android-$highestSdk.jar")
            if (fallbackJar.exists() && fallbackJar.length() > 0) {
                return fallbackJar
            }
        }
        val libsDir = BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH
        if (libsDir.exists()) {
            val anySdk = libsDir.listFiles { file ->
                file.isFile && file.name.startsWith("android-") && file.name.endsWith(".jar") && file.length() > 0
            }?.maxByOrNull { it.name }
            if (anySdk != null) {
                return anySdk
            }
        }
        return null
    }

    @JvmStatic
    fun ensureLegacyAndroidJar(context: Context) {
        val defaultJar = getDefaultAndroidJar(context) ?: return
        val legacy = File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android.jar")
        if (!legacy.exists() || legacy.length() == 0L) {
            try {
                if (legacy.exists()) legacy.delete()
                java.nio.file.Files.createSymbolicLink(legacy.toPath(), defaultJar.toPath())
            } catch (e: Exception) {
                try {
                    defaultJar.copyTo(legacy, overwrite = true)
                } catch (ignored: Exception) {
                }
            }
        }
    }
}
