package extensions.anbui.daydream.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

object NativeToolchainManager {
    private const val TAG = "NativeToolchainManager"

    const val NDK_DEFAULT_URL = "https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-aarch64.tar.xz"
    const val CMAKE_DEFAULT_URL = "https://github.com/Kitware/CMake/releases/download/v3.26.4/cmake-3.26.4-linux-aarch64.tar.gz"

    fun getBinDir(context: Context): File {
        val dir = File(context.filesDir, "bin")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getNdkDir(context: Context): File = File(getBinDir(context), "android-ndk")

    fun getCmakeDir(context: Context): File = File(getBinDir(context), "cmake")

    fun getNdkToolchainFile(context: Context): File {
        val primary = File(getNdkDir(context), "build/cmake/android.toolchain.cmake")
        if (primary.isFile) return primary
        val legacy = File(context.filesDir, "native/ndk/build/cmake/android.toolchain.cmake")
        if (legacy.isFile) return legacy
        return primary
    }

    fun getCmakeBinaryFile(context: Context): File {
        val primary = File(getCmakeDir(context), "bin/cmake")
        if (primary.isFile) return primary
        val alt = File(getCmakeDir(context), "cmake")
        if (alt.isFile) return alt
        val legacy = File(context.filesDir, "native/cmake/bin/cmake")
        if (legacy.isFile) return legacy
        return primary
    }

    fun isNdkInstalled(context: Context): Boolean = getNdkToolchainFile(context).isFile

    fun isCmakeInstalled(context: Context): Boolean = getCmakeBinaryFile(context).isFile

    fun getNdkSizeFormatted(context: Context): String {
        val dir = getNdkDir(context)
        val legacyDir = File(context.filesDir, "native/ndk")
        val activeDir = if (dir.exists()) dir else legacyDir
        if (!activeDir.exists()) return "0 MB"
        val bytes = getFolderSize(activeDir)
        return formatFileSize(bytes)
    }

    fun getCmakeSizeFormatted(context: Context): String {
        val dir = getCmakeDir(context)
        val legacyDir = File(context.filesDir, "native/cmake")
        val activeDir = if (dir.exists()) dir else legacyDir
        if (!activeDir.exists()) return "0 MB"
        val bytes = getFolderSize(activeDir)
        return formatFileSize(bytes)
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        val children = file.listFiles() ?: return 0L
        for (child in children) {
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        urlStr: String,
        isNdk: Boolean,
        onProgress: (progress: Int, message: String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetName = if (isNdk) "Android NDK" else "CMake"
        val ext = when {
            urlStr.endsWith(".tar.xz", ignoreCase = true) -> ".tar.xz"
            urlStr.endsWith(".tar.gz", ignoreCase = true) -> ".tar.gz"
            urlStr.endsWith(".tgz", ignoreCase = true) -> ".tgz"
            else -> ".zip"
        }
        val tempArchive = File(context.cacheDir, "temp_${if (isNdk) "ndk" else "cmake"}$ext")
        if (tempArchive.exists()) tempArchive.delete()

        try {
            onProgress(0, "Connecting to download server...")
            var currentUrl = urlStr
            var finalConnection: HttpURLConnection
            var redirects = 0
            while (true) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Sketchware-DayGreen/1.0 (Android)")
                }
                val code = conn.responseCode
                if (code in 300..399 && redirects < 7) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrEmpty()) {
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(URL(currentUrl), location).toString()
                        }
                        redirects++
                        continue
                    }
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    conn.disconnect()
                    throw java.io.IOException("Server returned HTTP $code (${conn.responseMessage})")
                }
                finalConnection = conn
                break
            }

            val totalBytes = finalConnection.contentLengthLong
            val inputStream = finalConnection.inputStream
            val outputStream = FileOutputStream(tempArchive)

            val buffer = ByteArray(64 * 1024)
            var downloadedBytes = 0L
            var read: Int
            var lastProgress = -1

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                downloadedBytes += read
                if (totalBytes > 0) {
                    val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        val dlMb = downloadedBytes / (1024 * 1024)
                        val totalMb = totalBytes / (1024 * 1024)
                        onProgress((progress * 0.85).toInt(), "Downloading $targetName: $dlMb MB / $totalMb MB ($progress%)")
                    }
                } else {
                    val dlMb = downloadedBytes / (1024 * 1024)
                    onProgress(40, "Downloading $targetName: $dlMb MB")
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            onProgress(88, "Extracting $targetName archive (Pure Java)...")
            extractAndSetup(context, tempArchive, isNdk, onProgress)
            tempArchive.delete()

            onProgress(100, "$targetName successfully installed!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and install $targetName: ${e.message}", e)
            tempArchive.delete()
            Result.failure(e)
        }
    }

    suspend fun installFromUri(
        context: Context,
        uri: Uri,
        isNdk: Boolean,
        onProgress: (progress: Int, message: String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetName = if (isNdk) "Android NDK" else "CMake"
        val tempArchive = File(context.cacheDir, "import_${if (isNdk) "ndk" else "cmake"}.tmp")
        if (tempArchive.exists()) tempArchive.delete()

        try {
            onProgress(10, "Copying archive from storage...")
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open selected archive.")
            val outputStream = FileOutputStream(tempArchive)
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            onProgress(40, "Extracting $targetName archive...")
            extractAndSetup(context, tempArchive, isNdk, onProgress)
            tempArchive.delete()

            onProgress(100, "$targetName successfully imported and installed!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import $targetName from URI: ${e.message}", e)
            tempArchive.delete()
            Result.failure(e)
        }
    }

    private fun extractAndSetup(
        context: Context,
        archiveFile: File,
        isNdk: Boolean,
        onProgress: ((progress: Int, message: String) -> Unit)? = null
    ) {
        val targetDir = if (isNdk) getNdkDir(context) else getCmakeDir(context)
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        if (isZipFile(archiveFile)) {
            onProgress?.invoke(90, "Extracting ZIP archive...")
            val zipFile = ZipFile(archiveFile)
            zipFile.extractAll(targetDir.absolutePath)
        } else if (isXzFile(archiveFile)) {
            onProgress?.invoke(90, "Extracting TAR.XZ archive (Pure Java)...")
            extractTarXz(archiveFile, targetDir, onProgress)
        } else if (isGzFile(archiveFile)) {
            onProgress?.invoke(90, "Extracting TAR.GZ archive...")
            extractTarGz(archiveFile, targetDir, onProgress)
        } else {
            // Default to TarXz or Tar
            try {
                extractTarXz(archiveFile, targetDir, onProgress)
            } catch (e: Exception) {
                Log.w(TAG, "TarXz failed, trying Zip: ${e.message}")
                val zipFile = ZipFile(archiveFile)
                zipFile.extractAll(targetDir.absolutePath)
            }
        }

        // Normalize directory structure if archive was wrapped in a root directory
        normalizeExtractedDirectory(targetDir, isNdk)

        // Make executable
        makeBinariesExecutable(targetDir)
    }

    private fun isZipFile(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                if (fis.read(header) == 4) {
                    header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                            (header[2] == 0x03.toByte() || header[2] == 0x05.toByte())
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isXzFile(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(6)
                if (fis.read(header) == 6) {
                    // XZ Magic bytes: FD 37 7A 58 5A 00
                    header[0] == 0xFD.toByte() && header[1] == 0x37.toByte() &&
                            header[2] == 0x7A.toByte() && header[3] == 0x58.toByte() &&
                            header[4] == 0x5A.toByte() && header[5] == 0x00.toByte()
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isGzFile(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(2)
                if (fis.read(header) == 2) {
                    header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractTarXz(
        archiveFile: File,
        targetDir: File,
        onProgress: ((progress: Int, message: String) -> Unit)? = null
    ) {
        val symlinkList = mutableListOf<Pair<File, String>>()
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 64 * 1024).use { bis ->
                XZInputStream(bis).use { xzIn ->
                    TarArchiveInputStream(xzIn).use { tarIn ->
                        var entry: TarArchiveEntry? = tarIn.nextEntry as? TarArchiveEntry
                        var count = 0
                        while (entry != null) {
                            val outFile = File(targetDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else if (entry.isSymbolicLink || entry.isLink) {
                                outFile.parentFile?.mkdirs()
                                val link = entry.linkName
                                if (!link.isNullOrEmpty()) {
                                    symlinkList.add(Pair(outFile, link))
                                }
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                                if ((entry.mode and 0b001_000_000) != 0 ||
                                    outFile.parentFile?.name == "bin" ||
                                    outFile.name.endsWith(".so")
                                ) {
                                    outFile.setExecutable(true, false)
                                    outFile.setReadable(true, false)
                                }
                            }
                            count++
                            if (count % 300 == 0) {
                                onProgress?.invoke(92, "Extracting files: $count items...")
                            }
                            entry = tarIn.nextEntry as? TarArchiveEntry
                        }
                    }
                }
            }
        }
        resolveSymlinks(symlinkList)
    }

    private fun extractTarGz(
        archiveFile: File,
        targetDir: File,
        onProgress: ((progress: Int, message: String) -> Unit)? = null
    ) {
        val symlinkList = mutableListOf<Pair<File, String>>()
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 64 * 1024).use { bis ->
                GZIPInputStream(bis).use { gzIn ->
                    TarArchiveInputStream(gzIn).use { tarIn ->
                        var entry: TarArchiveEntry? = tarIn.nextEntry as? TarArchiveEntry
                        var count = 0
                        while (entry != null) {
                            val outFile = File(targetDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else if (entry.isSymbolicLink || entry.isLink) {
                                outFile.parentFile?.mkdirs()
                                val link = entry.linkName
                                if (!link.isNullOrEmpty()) {
                                    symlinkList.add(Pair(outFile, link))
                                }
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                                if ((entry.mode and 0b001_000_000) != 0 ||
                                    outFile.parentFile?.name == "bin"
                                ) {
                                    outFile.setExecutable(true, false)
                                }
                            }
                            count++
                            if (count % 300 == 0) {
                                onProgress?.invoke(92, "Extracting files: $count items...")
                            }
                            entry = tarIn.nextEntry as? TarArchiveEntry
                        }
                    }
                }
            }
        }
        resolveSymlinks(symlinkList)
    }

    private fun resolveSymlinks(symlinks: List<Pair<File, String>>) {
        for ((linkFile, targetRel) in symlinks) {
            try {
                if (linkFile.exists()) linkFile.delete()
                var resolved = false
                try {
                    android.system.Os.symlink(targetRel, linkFile.absolutePath)
                    resolved = true
                } catch (ignored: Exception) {
                }
                if (!resolved) {
                    val targetFile = File(linkFile.parentFile, targetRel).canonicalFile
                    if (targetFile.exists() && targetFile.isFile) {
                        targetFile.copyTo(linkFile, overwrite = true)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Symlink resolution failed for ${linkFile.name} -> $targetRel: ${e.message}")
            }
            if (linkFile.parentFile?.name == "bin" || linkFile.name.endsWith(".so")) {
                linkFile.setExecutable(true, false)
                linkFile.setReadable(true, false)
            }
        }
    }

    private fun normalizeExtractedDirectory(targetDir: File, isNdk: Boolean) {
        val expectedCheck = if (isNdk) {
            File(targetDir, "build/cmake/android.toolchain.cmake")
        } else {
            File(targetDir, "bin/cmake")
        }

        if (expectedCheck.isFile) {
            return
        }

        // Check if there is a single top-level directory or nested directory that contains the expected layout
        val children = targetDir.listFiles() ?: return
        val matchedDir = children.firstOrNull {
            it.isDirectory && (if (isNdk) File(it, "build/cmake/android.toolchain.cmake").isFile else (File(it, "bin/cmake").isFile || File(it, "cmake").isFile))
        } ?: children.firstOrNull { it.isDirectory && children.size == 1 }

        if (matchedDir != null && matchedDir.isDirectory) {
            val nestedChildren = matchedDir.listFiles() ?: return
            for (child in nestedChildren) {
                val dest = File(targetDir, child.name)
                child.renameTo(dest)
            }
            matchedDir.delete()
        }
    }

    fun makeBinariesExecutable(dir: File) {
        if (!dir.exists()) return
        repairExtractedBinaries(dir)
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                makeBinariesExecutable(file)
            } else {
                val name = file.name
                if (name == "cmake" || name == "ninja" || name == "cpack" || name == "ctest" ||
                    name.endsWith(".so") || !name.contains(".") || file.parentFile?.name == "bin"
                ) {
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }
        }
    }

    private fun repairExtractedBinaries(dir: File) {
        val binDirs = listOf(
            File(dir, "toolchains/llvm/prebuilt/linux-x86_64/bin"),
            File(dir, "toolchains/llvm/prebuilt/linux-aarch64/bin"),
            File(dir, "bin")
        )
        for (binDir in binDirs) {
            if (binDir.isDirectory) {
                val files = binDir.listFiles() ?: continue
                val realClang = files.firstOrNull {
                    it.isFile && it.name.matches(Regex("^clang-[0-9]+$")) && it.length() > 1024 * 1024L
                } ?: files.firstOrNull {
                    it.isFile && it.name.startsWith("clang") && !it.name.contains("format") &&
                            !it.name.contains("tidy") && !it.name.contains("check") && it.length() > 1024 * 1024L
                }
                if (realClang != null) {
                    val clang = File(binDir, "clang")
                    if (!clang.exists() || clang.length() < 1024L) {
                        try {
                            realClang.copyTo(clang, overwrite = true)
                            clang.setExecutable(true, false)
                        } catch (ignored: Exception) {}
                    }
                    val clangCpp = File(binDir, "clang++")
                    if (!clangCpp.exists() || clangCpp.length() < 1024L) {
                        try {
                            realClang.copyTo(clangCpp, overwrite = true)
                            clangCpp.setExecutable(true, false)
                        } catch (ignored: Exception) {}
                    }
                }
            }
        }
    }

    fun deleteNdk(context: Context): Boolean {
        val dir = getNdkDir(context)
        val legacyDir = File(context.filesDir, "native/ndk")
        val d1 = if (dir.exists()) dir.deleteRecursively() else true
        val d2 = if (legacyDir.exists()) legacyDir.deleteRecursively() else true
        return d1 && d2
    }

    fun deleteCmake(context: Context): Boolean {
        val dir = getCmakeDir(context)
        val legacyDir = File(context.filesDir, "native/cmake")
        val d1 = if (dir.exists()) dir.deleteRecursively() else true
        val d2 = if (legacyDir.exists()) legacyDir.deleteRecursively() else true
        return d1 && d2
    }
}
