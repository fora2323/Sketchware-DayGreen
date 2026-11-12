package org.sketchware.daygreen

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import pro.sketchware.R
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

object DownloadUtility {
    private const val TAG = "DownloadUtility"
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            String.format(Locale.US, "%.1f KB", kb)
        }
    }

    fun formatFileSize(context: Context, sizeInBytes: Long): String {
        return formatSize(sizeInBytes)
    }

    @Throws(IOException::class)
    private fun openConnectionWithRedirects(initialUrl: String): Pair<HttpURLConnection, Long> {
        var currentUrl = initialUrl
        var redirects = 0
        val maxRedirects = 10

        while (redirects < maxRedirects) {
            val urlObj = URL(currentUrl)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.setRequestProperty(
                "User-Agent",
                "Sketchware-DayGreen/8.0.4 (Linux; Android " + Build.VERSION.RELEASE + ")"
            )
            connection.setRequestProperty("Accept-Encoding", "identity")

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (!location.isNullOrEmpty()) {
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(urlObj, location).toString()
                    }
                    redirects++
                    continue
                }
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = connection.responseMessage ?: "HTTP $responseCode"
                connection.disconnect()
                throw IOException("Server returned HTTP response: $responseCode ($errorMsg) for $currentUrl")
            }

            val contentLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connection.contentLengthLong
            } else {
                connection.getHeaderField("Content-Length")?.toLongOrNull()
                    ?: connection.contentLength.toLong()
            }

            return Pair(connection, contentLength)
        }
        throw IOException("Too many redirects connecting to: $initialUrl")
    }

    fun downloadFile(
        activity: Activity,
        url: String,
        destinationFile: File,
        onComplete: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_material_progress, null)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.progress_indicator)
        val tvPercentage = dialogView.findViewById<TextView>(R.id.tv_percentage)
        val tvBytes = dialogView.findViewById<TextView>(R.id.tv_bytes)

        var isCancelled = false
        var currentConnection: HttpURLConnection? = null

        progressIndicator.isIndeterminate = true
        tvPercentage.text = "Connecting..."
        tvBytes.text = "0 MB"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Downloading...")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ ->
                isCancelled = true
                executor.execute {
                    try {
                        currentConnection?.disconnect()
                    } catch (ignored: Exception) {}
                }
            }
            .create()

        dialog.show()

        executor.execute {
            try {
                val (connection, fileLength) = openConnectionWithRedirects(url)
                currentConnection = connection

                destinationFile.parentFile?.mkdirs()
                val output = BufferedOutputStream(FileOutputStream(destinationFile), 64 * 1024)
                val input = BufferedInputStream(connection.inputStream, 64 * 1024)

                val data = ByteArray(64 * 1024)
                var total: Long = 0
                var count: Int
                var lastUpdate = 0L
                var lastProgress = -1

                while (input.read(data).also { count = it } != -1) {
                    if (isCancelled) {
                        output.close()
                        input.close()
                        if (destinationFile.exists()) destinationFile.delete()
                        handler.post {
                            if (!activity.isFinishing) {
                                dialog.dismiss()
                                Toast.makeText(activity, "Download cancelled", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return@execute
                    }

                    total += count
                    output.write(data, 0, count)

                    val now = System.currentTimeMillis()
                    val progress = if (fileLength > 0) ((total * 100) / fileLength).toInt().coerceIn(0, 100) else 0
                    if (now - lastUpdate > 100 || progress != lastProgress || (fileLength > 0 && total == fileLength)) {
                        lastUpdate = now
                        lastProgress = progress
                        handler.post {
                            if (activity.isFinishing) return@post
                            if (fileLength > 0) {
                                progressIndicator.isIndeterminate = false
                                progressIndicator.progress = progress
                                tvPercentage.text = "$progress%"
                                tvBytes.text = "${formatSize(total)} / ${formatSize(fileLength)}"
                            } else {
                                progressIndicator.isIndeterminate = true
                                tvPercentage.text = "Downloading..."
                                tvBytes.text = "${formatSize(total)} downloaded"
                            }
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                handler.post {
                    if (activity.isFinishing) return@post
                    dialog.dismiss()
                    val name = destinationFile.name.lowercase(Locale.ROOT)
                    if (name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tar.xz") || name.endsWith(".tgz")) {
                        extractArchive(activity, destinationFile) {
                            Toast.makeText(activity, "Download and extraction completed", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    } else {
                        Toast.makeText(activity, "Download completed", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                handler.post {
                    if (!activity.isFinishing) {
                        dialog.dismiss()
                        if (!isCancelled) {
                            Toast.makeText(activity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } finally {
                currentConnection?.disconnect()
            }
        }
    }

    fun extractArchive(activity: Activity, archiveFile: File, onComplete: () -> Unit) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_material_progress, null)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.progress_indicator)
        val tvPercentage = dialogView.findViewById<TextView>(R.id.tv_percentage)
        val tvBytes = dialogView.findViewById<TextView>(R.id.tv_bytes)

        val totalArchiveSize = archiveFile.length()
        progressIndicator.isIndeterminate = false
        progressIndicator.progress = 0
        tvPercentage.text = "0%"
        tvBytes.text = "0 MB / ${formatSize(totalArchiveSize)}"

        val progressDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Extracting ${archiveFile.name}...")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog.show()

        executor.execute {
            try {
                val name = archiveFile.name.lowercase(Locale.ROOT)
                val parentPath = archiveFile.parentFile ?: activity.filesDir
                val tempExtractDir = File(parentPath, "temp_extract_" + System.currentTimeMillis())
                tempExtractDir.mkdirs()

                val progressCallback: (Int, Long, Long) -> Unit = { pct, readBytes, totalBytes ->
                    handler.post {
                        if (!activity.isFinishing) {
                            progressIndicator.isIndeterminate = false
                            progressIndicator.progress = pct.coerceIn(0, 100)
                            tvPercentage.text = "$pct%"
                            tvBytes.text = "${formatSize(readBytes)} / ${formatSize(totalBytes)}"
                        }
                    }
                }

                if (name.endsWith(".zip")) {
                    extractZip(archiveFile, tempExtractDir, totalArchiveSize, progressCallback)
                } else if (name.endsWith(".tar.xz")) {
                    extractTarXz(archiveFile, tempExtractDir, totalArchiveSize, progressCallback)
                } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                    extractTarGz(archiveFile, tempExtractDir, totalArchiveSize, progressCallback)
                } else {
                    extractTarGeneric(archiveFile, tempExtractDir, totalArchiveSize, progressCallback)
                }

                // Determine final target directory
                val isNdk = name.contains("ndk")
                val isCmake = name.contains("cmake")
                val isSdkTools = name.contains("sdk-tools") || name.contains("tools.zip")
                val targetDir = if (isCmake) File(parentPath, "cmake")
                                else if (isNdk) File(parentPath, "ndk")
                                else if (isSdkTools) parentPath
                                else null

                if (targetDir != null) {
                    if (targetDir.exists() && !isSdkTools) targetDir.deleteRecursively()
                    targetDir.mkdirs()

                    normalizeExtractedDirectory(tempExtractDir, targetDir)
                    repairExtractedBinaries(targetDir)
                    makeAllExecutable(targetDir)
                }

                if (tempExtractDir.exists()) tempExtractDir.deleteRecursively()
                if (archiveFile.exists()) archiveFile.delete()

                handler.post {
                    if (!activity.isFinishing) {
                        progressDialog.dismiss()
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Extraction failed", e)
                handler.post {
                    if (!activity.isFinishing) {
                        progressDialog.dismiss()
                        Toast.makeText(activity, "Extraction failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private class ProgressTrackingStream(
        source: InputStream,
        private val totalBytes: Long,
        private val onProgress: (Int, Long, Long) -> Unit
    ) : FilterInputStream(source) {
        private var bytesRead: Long = 0L
        private var lastUpdate: Long = 0L
        private var lastPct: Int = -1

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) {
                bytesRead += n
                val now = System.currentTimeMillis()
                val pct = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                if (now - lastUpdate > 120 || pct != lastPct || bytesRead == totalBytes) {
                    lastUpdate = now
                    lastPct = pct
                    onProgress(pct, bytesRead, totalBytes)
                }
            }
            return n
        }
    }

    private fun extractTarXz(
        archiveFile: File,
        targetDir: File,
        totalSize: Long,
        onProgress: (Int, Long, Long) -> Unit
    ) {
        val symlinkList = mutableListOf<Pair<File, String>>()
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 128 * 1024).use { bis ->
                val progressIn = ProgressTrackingStream(bis, totalSize, onProgress)
                XZInputStream(progressIn).use { xzIn ->
                    TarArchiveInputStream(xzIn).use { tarIn ->
                        readTarStream(tarIn, targetDir, symlinkList)
                    }
                }
            }
        }
        resolveSymlinks(symlinkList)
    }

    private fun extractTarGz(
        archiveFile: File,
        targetDir: File,
        totalSize: Long,
        onProgress: (Int, Long, Long) -> Unit
    ) {
        val symlinkList = mutableListOf<Pair<File, String>>()
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 128 * 1024).use { bis ->
                val progressIn = ProgressTrackingStream(bis, totalSize, onProgress)
                GZIPInputStream(progressIn).use { gzIn ->
                    TarArchiveInputStream(gzIn).use { tarIn ->
                        readTarStream(tarIn, targetDir, symlinkList)
                    }
                }
            }
        }
        resolveSymlinks(symlinkList)
    }

    private fun extractTarGeneric(
        archiveFile: File,
        targetDir: File,
        totalSize: Long,
        onProgress: (Int, Long, Long) -> Unit
    ) {
        val symlinkList = mutableListOf<Pair<File, String>>()
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 128 * 1024).use { bis ->
                val progressIn = ProgressTrackingStream(bis, totalSize, onProgress)
                TarArchiveInputStream(progressIn).use { tarIn ->
                    readTarStream(tarIn, targetDir, symlinkList)
                }
            }
        }
        resolveSymlinks(symlinkList)
    }

    private fun readTarStream(
        tarIn: TarArchiveInputStream,
        targetDir: File,
        symlinkList: MutableList<Pair<File, String>>
    ) {
        var entry: TarArchiveEntry? = tarIn.nextEntry as? TarArchiveEntry
        val buffer = ByteArray(64 * 1024)

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
                BufferedOutputStream(FileOutputStream(outFile), 64 * 1024).use { fos ->
                    var n: Int
                    while (tarIn.read(buffer).also { n = it } != -1) {
                        fos.write(buffer, 0, n)
                    }
                }
                if ((entry.mode and 0b001_000_000) != 0 || outFile.parentFile?.name == "bin" || outFile.name.endsWith(".so")) {
                    outFile.setExecutable(true, false)
                    outFile.setReadable(true, false)
                }
            }
            entry = tarIn.nextEntry as? TarArchiveEntry
        }
    }

    private fun extractZip(
        archiveFile: File,
        targetDir: File,
        totalSize: Long,
        onProgress: (Int, Long, Long) -> Unit
    ) {
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 128 * 1024).use { bis ->
                val progressIn = ProgressTrackingStream(bis, totalSize, onProgress)
                ZipInputStream(progressIn).use { zis ->
                    val buffer = ByteArray(64 * 1024)
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(outFile), 64 * 1024).use { fos ->
                                var n: Int
                                while (zis.read(buffer).also { n = it } != -1) {
                                    fos.write(buffer, 0, n)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        }
    }

    private fun resolveSymlinks(symlinks: List<Pair<File, String>>) {
        for ((linkFile, targetRel) in symlinks) {
            try {
                if (linkFile.exists()) linkFile.delete()
                var resolved = false
                try {
                    android.system.Os.symlink(targetRel, linkFile.absolutePath)
                    resolved = true
                } catch (ignored: Exception) {}

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

    private fun normalizeExtractedDirectory(tempExtractDir: File, targetDir: File) {
        val subDirs = tempExtractDir.listFiles { f -> f.isDirectory }
        val subFiles = tempExtractDir.listFiles { f -> f.isFile }

        val sourceToMove = if (subDirs != null && subDirs.size == 1 && (subFiles == null || subFiles.isEmpty())) {
            subDirs[0]
        } else {
            tempExtractDir
        }

        if (sourceToMove == tempExtractDir) {
            sourceToMove.listFiles()?.forEach { f ->
                val dest = File(targetDir, f.name)
                if (!f.renameTo(dest)) {
                    f.copyRecursively(dest, overwrite = true)
                }
            }
        } else {
            sourceToMove.listFiles()?.forEach { f ->
                val dest = File(targetDir, f.name)
                if (!f.renameTo(dest)) {
                    f.copyRecursively(dest, overwrite = true)
                }
            }
        }
    }

    fun repairExtractedBinaries(dir: File) {
        val binDirs = listOf(
            File(dir, "toolchains/llvm/prebuilt/linux-aarch64/bin"),
            File(dir, "toolchains/llvm/prebuilt/linux-arm/bin"),
            File(dir, "toolchains/llvm/prebuilt/linux-x86_64/bin"),
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

    private fun makeAllExecutable(dir: File) {
        if (!dir.exists()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                makeAllExecutable(file)
            } else {
                val name = file.name
                if (name == "cmake" || name == "ninja" || name == "cpack" || name == "ctest" ||
                    name.startsWith("clang") || name.startsWith("ld") || name.startsWith("llvm-") ||
                    name.endsWith(".so") || !name.contains(".") || file.parentFile?.name == "bin"
                ) {
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }
        }
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", dir.absolutePath)).waitFor()
        } catch (ignored: Exception) {}
    }

    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS[0]
    }
}