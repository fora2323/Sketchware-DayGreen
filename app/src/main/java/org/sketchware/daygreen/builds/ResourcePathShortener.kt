package org.sketchware.daygreen.builds

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import a.a.a.ProjectBuilder
import mod.jbk.util.LogUtil

class ResourcePathShortener(private val context: Context, private val builder: ProjectBuilder) {

    private val TAG = "ResourcePathShortener"
    private val BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private var idCounter = 0

    fun shorten() {
        val apkFile = File(builder.yq.resourcesApkPath)
        if (!apkFile.exists()) {
            LogUtil.e(TAG, "resources.apk not found at ${apkFile.absolutePath}")
            return
        }

        val tempApk = File(apkFile.absolutePath + ".tmp")
        try {
            LogUtil.d(TAG, "Starting resource path shortening...")
            val pathMap = HashMap<String, String>()

            // First pass: Generate short names for applicable res/ entries
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("res/") && !name.startsWith("res/color")) {
                        pathMap[name] = "res/" + generateShortName() + getExtension(name)
                    }
                    entry = zis.nextEntry
                }
            }

            LogUtil.d(TAG, "Generated short names for ${pathMap.size} resources.")

            // Second pass: Rewrite APK and patch resources.arsc
            ZipOutputStream(FileOutputStream(tempApk)).use { zos ->
                ZipInputStream(FileInputStream(apkFile)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val bytes = zis.readBytes()
                        
                        if (name == "resources.arsc") {
                            val patchedArsc = patchArsc(bytes, pathMap)
                            val newEntry = ZipEntry("resources.arsc")
                            newEntry.method = ZipEntry.STORED
                            newEntry.size = patchedArsc.size.toLong()
                            newEntry.compressedSize = patchedArsc.size.toLong()
                            newEntry.crc = CRC32().apply { update(patchedArsc) }.value
                            zos.putNextEntry(newEntry)
                            zos.write(patchedArsc)
                        } else {
                            val newName = pathMap[name] ?: name
                            val newEntry = ZipEntry(newName)
                            
                            // Prevent ZipException by explicitly calculating CRC for STORED entries
                            if (entry.method == ZipEntry.STORED) {
                                newEntry.method = ZipEntry.STORED
                                newEntry.size = bytes.size.toLong()
                                newEntry.compressedSize = bytes.size.toLong()
                                newEntry.crc = CRC32().apply { update(bytes) }.value
                            } else {
                                newEntry.method = ZipEntry.DEFLATED
                            }
                            
                            zos.putNextEntry(newEntry)
                            zos.write(bytes)
                        }
                        zos.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // Verify and replace original APK
            if (tempApk.exists() && tempApk.length() > 0) {
                if (apkFile.delete()) {
                    tempApk.renameTo(apkFile)
                    LogUtil.d(TAG, "Successfully shortened resource paths and rebuilt APK.")
                } else {
                    LogUtil.e(TAG, "Failed to delete original resources.apk.")
                }
            }

        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed to shorten resource paths", e)
            if (tempApk.exists()) tempApk.delete() // Fallback to original
        }
    }

    private fun generateShortName(): String {
        var i = idCounter++
        val sb = StringBuilder("-")
        if (i == 0) {
            sb.append(BASE62[0])
            return sb.toString()
        }
        while (i > 0) {
            sb.append(BASE62[i % 62])
            i /= 62
        }
        return sb.toString()
    }

    private fun getExtension(name: String): String {
        if (name.endsWith(".9.png", ignoreCase = true)) return ".9.png"
        val dot = name.lastIndexOf('.')
        return if (dot != -1) name.substring(dot) else ""
    }

    private fun patchArsc(arscBytes: ByteArray, pathMap: Map<String, String>): ByteArray {
        val bb = ByteBuffer.wrap(arscBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read ResTable_header
        val tableType = bb.short
        val tableHeaderSize = bb.short
        val tableSize = bb.int
        
        // Read ResStringPool_header
        bb.position(tableHeaderSize.toInt())
        val poolType = bb.short
        val poolHeaderSize = bb.short
        val poolSize = bb.int
        val stringCount = bb.int
        val styleCount = bb.int
        val flags = bb.int
        val stringsStart = bb.int
        val stylesStart = bb.int

        val isUtf8 = (flags and (1 shl 8)) != 0

        // Read offsets
        val stringOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) stringOffsets[i] = bb.int

        val styleOffsets = IntArray(styleCount)
        for (i in 0 until styleCount) styleOffsets[i] = bb.int

        // Read strings
        val strings = arrayOfNulls<String>(stringCount)
        for (i in 0 until stringCount) {
            val offset = tableHeaderSize.toInt() + stringsStart + stringOffsets[i]
            bb.position(offset)
            strings[i] = readString(bb, isUtf8)
        }

        // Read styles data (if any)
        val stylesDataSize = if (styleCount > 0) poolSize - stylesStart else 0
        val stylesData = ByteArray(stylesDataSize)
        if (stylesDataSize > 0) {
            bb.position(tableHeaderSize.toInt() + stylesStart)
            bb.get(stylesData)
        }

        // Replace matched paths
        for (i in 0 until stringCount) {
            val s = strings[i]
            if (s != null && pathMap.containsKey(s)) {
                strings[i] = pathMap[s]
            }
        }

        // Build new string data
        val newStringData = ByteArrayOutputStream()
        val newStringOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            newStringOffsets[i] = newStringData.size()
            writeString(newStringData, strings[i] ?: "", isUtf8)
        }

        // Pad to 4 bytes alignment
        while (newStringData.size() % 4 != 0) {
            newStringData.write(0)
        }
        val newStringBytes = newStringData.toByteArray()

        // Calculate new pool metrics
        val newStringsStart = poolHeaderSize.toInt() + (stringCount * 4) + (styleCount * 4)
        val newStylesStart = if (styleCount > 0) newStringsStart + newStringBytes.size else 0
        val newPoolSize = newStringsStart + newStringBytes.size + stylesData.size

        val sizeDiff = newPoolSize - poolSize
        val newTableSize = tableSize + sizeDiff

        // Reconstruct ARSC
        val outArsc = ByteArrayOutputStream()
        
        // 1. Write updated ResTable_header
        bb.position(0)
        val tableHeaderBytes = ByteArray(tableHeaderSize.toInt())
        bb.get(tableHeaderBytes)
        val bbHeader = ByteBuffer.wrap(tableHeaderBytes).order(ByteOrder.LITTLE_ENDIAN)
        bbHeader.putInt(4, newTableSize)
        outArsc.write(tableHeaderBytes)

        // 2. Write updated ResStringPool_header
        bb.position(tableHeaderSize.toInt())
        val origPoolHeader = ByteArray(poolHeaderSize.toInt())
        bb.get(origPoolHeader)
        val bbPool = ByteBuffer.wrap(origPoolHeader).order(ByteOrder.LITTLE_ENDIAN)
        bbPool.putInt(4, newPoolSize)
        bbPool.putInt(20, newStringsStart)
        bbPool.putInt(24, newStylesStart)
        outArsc.write(origPoolHeader)

        // 3. Write offsets
        val bbOffsets = ByteBuffer.allocate((stringCount + styleCount) * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (off in newStringOffsets) bbOffsets.putInt(off)
        for (off in styleOffsets) bbOffsets.putInt(off)
        outArsc.write(bbOffsets.array())

        // 4. Write string & style data
        outArsc.write(newStringBytes)
        if (styleCount > 0) outArsc.write(stylesData)

        // 5. Write remaining chunks (ResTable_package, etc.)
        val remainingStart = tableHeaderSize.toInt() + poolSize
        bb.position(remainingStart)
        val remainingBytes = ByteArray(bb.remaining())
        bb.get(remainingBytes)
        outArsc.write(remainingBytes)

        return outArsc.toByteArray()
    }

    private fun readString(bb: ByteBuffer, isUtf8: Boolean): String {
        if (isUtf8) {
            val charLen = decodeLengthUtf8(bb)
            val byteLen = decodeLengthUtf8(bb)
            val bytes = ByteArray(byteLen)
            bb.get(bytes)
            bb.get() // null terminator
            return String(bytes, Charsets.UTF_8)
        } else {
            val charLen = decodeLengthUtf16(bb)
            val bytes = ByteArray(charLen * 2)
            bb.get(bytes)
            bb.short // null terminator
            return String(bytes, Charsets.UTF_16LE)
        }
    }

    private fun writeString(out: ByteArrayOutputStream, str: String, isUtf8: Boolean) {
        if (isUtf8) {
            val bytes = str.toByteArray(Charsets.UTF_8)
            encodeLengthUtf8(out, str.length)
            encodeLengthUtf8(out, bytes.size)
            out.write(bytes)
            out.write(0)
        } else {
            val charLen = str.length
            encodeLengthUtf16(out, charLen)
            val bytes = str.toByteArray(Charsets.UTF_16LE)
            out.write(bytes)
            out.write(0)
            out.write(0)
        }
    }

    private fun decodeLengthUtf8(bb: ByteBuffer): Int {
        var len = bb.get().toInt() and 0xFF
        if ((len and 0x80) != 0) {
            len = ((len and 0x7F) shl 8) or (bb.get().toInt() and 0xFF)
        }
        return len
    }

    private fun encodeLengthUtf8(out: ByteArrayOutputStream, len: Int) {
        if (len > 0x7F) {
            out.write(((len shr 8) or 0x80) and 0xFF)
            out.write(len and 0xFF)
        } else {
            out.write(len and 0xFF)
        }
    }

    private fun decodeLengthUtf16(bb: ByteBuffer): Int {
        var len = bb.short.toInt() and 0xFFFF
        if ((len and 0x8000) != 0) {
            len = ((len and 0x7FFF) shl 16) or (bb.short.toInt() and 0xFFFF)
        }
        return len
    }

    private fun encodeLengthUtf16(out: ByteArrayOutputStream, len: Int) {
        if (len > 0x7FFF) {
            val high = (len shr 16) or 0x8000
            out.write(high and 0xFF)
            out.write((high shr 8) and 0xFF)
            val low = len and 0xFFFF
            out.write(low and 0xFF)
            out.write((low shr 8) and 0xFF)
        } else {
            out.write(len and 0xFF)
            out.write((len shr 8) and 0xFF)
        }
    }
}