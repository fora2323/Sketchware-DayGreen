package org.sketchware.daygreen.builds

import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import mod.jbk.util.LogUtil
import pro.sketchware.utility.FileUtil


class BuildCache(sc_id: String) {
    
    private val cacheRoot: File = File(FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/build_cache")

    init {
        cacheRoot.mkdirs()
    }

    private fun hashFile(stage: String): File {
        return File(cacheRoot, "$stage.hash")
    }

    fun stageOutputDir(stage: String): File {
        val dir = File(cacheRoot, "${stage}_output")
        dir.mkdirs()
        return dir
    }

    fun isUpToDate(stage: String, currentHash: String?): Boolean {
        if (currentHash == null) return false
        val hf = hashFile(stage)
        if (!hf.exists()) return false
        
        val stored = FileUtil.readFile(hf.absolutePath)
        val upToDate = currentHash == stored
        LogUtil.d(TAG, "Stage '$stage' up to date: $upToDate")
        
        return upToDate
    }

    fun markUpToDate(stage: String, currentHash: String?) {
        if (currentHash == null) return
        FileUtil.writeFile(hashFile(stage).absolutePath, currentHash)
    }

    fun invalidateAll() {
        FileUtil.deleteFile(cacheRoot.absolutePath)
        cacheRoot.mkdirs()
    }

    companion object {
        private const val TAG = "BuildCache"

        @JvmStatic
        fun hashDirectory(vararg paths: String?): String? {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val allFiles = mutableListOf<File>()
                
                for (path in paths) {
                    if (path == null) continue
                    collectFiles(File(path), allFiles)
                }
                
                allFiles.sortBy { 
                    it.absolutePath 
                }
                
                for (f in allFiles) {
                    digest.update(f.absolutePath.toByteArray(StandardCharsets.UTF_8))
                    hashFileContentInto(digest, f)
                }
                bytesToHex(digest.digest())
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to hash directories ${paths.toList()}", e)
                null
            }
        }

        @JvmStatic
        fun hashFiles(files: List<File>): String? {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val sorted = files.sortedBy { it.absolutePath }
                
                for (f in sorted) {
                    digest.update(f.absolutePath.toByteArray(StandardCharsets.UTF_8))
                    hashFileContentInto(digest, f)
                }
                bytesToHex(digest.digest())
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to hash file list", e)
                null
            }
        }

        @JvmStatic
        fun hashStrings(vararg extra: String?): String? {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                for (s in extra) {
                    digest.update((s ?: "null").toByteArray(StandardCharsets.UTF_8))
                    digest.update(0.toByte())
                }
                bytesToHex(digest.digest())
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to hash strings", e)
                null
            }
        }

        @JvmStatic
        fun combine(vararg hashes: String?): String? {
            for (h in hashes) {
                if (h == null) return null
            }
            val sb = java.lang.StringBuilder()
            for (h in hashes) {
                sb.append(h).append('|')
            }
            return sb.toString()
        }

        @JvmStatic
        private fun collectFiles(file: File?, out: MutableList<File>) {
            if (file == null || !file.exists()) return
            if (file.isDirectory) {
                val children = file.listFiles()
                if (children != null) {
                    for (child in children) collectFiles(child, out)
                }
            } else {
                out.add(file)
            }
        }

        @JvmStatic
        private fun hashFileContentInto(digest: MessageDigest, f: File) {
            if (!f.exists() || f.isDirectory) return
            FileInputStream(f).use { inputStream ->
                val buf = ByteArray(8192)
                var read: Int
                while (inputStream.read(buf).also { read = it } != -1) {
                    digest.update(buf, 0, read)
                }
            }
        }

        @JvmStatic
        private fun bytesToHex(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}