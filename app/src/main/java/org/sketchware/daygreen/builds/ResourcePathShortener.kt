package org.sketchware.daygreen.builds

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
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

    private class CountingOutputStream(private val out: OutputStream) : OutputStream() {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray) {
            out.write(b)
            count += b.size
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }

        override fun flush() = out.flush()
        override fun close() = out.close()
    }

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
            var origArsc: ByteArray? = null

            // Pass 1: Generate short names & extract original resources.arsc
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.endsWith("/")) {
                        entry = zis.nextEntry
                        continue
                    }
                    if (name == "resources.arsc") {
                        origArsc = zis.readBytes()
                    } else if (name.startsWith("res/") && !name.startsWith("res/color/") && !name.startsWith("res/color-")) {
                        pathMap[name] = "res/" + generateShortName() + getExtension(name)
                    }
                    entry = zis.nextEntry
                }
            }

            if (origArsc == null) {
                throw Exception("resources.arsc not found in APK.")
            }

            LogUtil.d(TAG, "Generated short names for ${pathMap.size} resources.")

            // Patch ARSC
            val patchedArsc = patchArsc(origArsc!!, pathMap)
            val writtenEntries = mutableSetOf<String>()

            // Pass 2: Rewrite APK (Writing ARSC First for alignment)
            val cos = CountingOutputStream(FileOutputStream(tempApk))
            ZipOutputStream(cos).use { zos ->
                
                // Write resources.arsc FIRST to easily calculate 4-byte alignment
                zos.flush()
                val arscName = "resources.arsc"
                val offset = cos.count
                val headerSize = 30 + arscName.length
                val alignment = (offset + headerSize) % 4
                val pad = if (alignment == 0L) 0 else (4 - alignment).toInt()
                
                val arscEntry = ZipEntry(arscName)
                arscEntry.method = ZipEntry.STORED
                arscEntry.size = patchedArsc.size.toLong()
                arscEntry.compressedSize = patchedArsc.size.toLong()
                arscEntry.crc = CRC32().apply { update(patchedArsc) }.value
                
                if (pad > 0) {
                    // Inject Android Zipalign Extra Field to pad local file header
                    val extra = ByteArray(4 + pad)
                    val bbExtra = ByteBuffer.wrap(extra).order(ByteOrder.LITTLE_ENDIAN)
                    bbExtra.putShort(0xD935.toShort()) // ID
                    bbExtra.putShort(pad.toShort()) // Size of data
                    arscEntry.extra = extra
                } else {
                    arscEntry.extra = ByteArray(0)
                }

                zos.putNextEntry(arscEntry)
                zos.write(patchedArsc)
                zos.closeEntry()
                writtenEntries.add(arscName)

                // Write the rest of the files
                ZipInputStream(FileInputStream(apkFile)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name.endsWith("/") || name == "resources.arsc") {
                            entry = zis.nextEntry
                            continue
                        }
                        
                        val newName = pathMap[name] ?: name
                        val newEntry = ZipEntry(newName)
                        val bytes = zis.readBytes()
                        
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
                        zos.closeEntry()
                        writtenEntries.add(newName)
                        
                        entry = zis.nextEntry
                    }
                }
            }

            // Post-Patch Verification
            val parsedStrings = parseArscStrings(patchedArsc)
            val arscPaths = parsedStrings.filter { it.startsWith("res/") }
            for (path in arscPaths) {
                if (!writtenEntries.contains(path)) {
                    throw IllegalStateException("Verification failed: '$path' is mapped in ARSC but missing from ZIP.")
                }
            }

            // Safe Backup & Rename
            val bakApk = File(apkFile.absolutePath + ".bak")
            if (apkFile.renameTo(bakApk)) {
                if (tempApk.renameTo(apkFile)) {
                    bakApk.delete()
                    LogUtil.d(TAG, "Successfully shortened resource paths and rebuilt APK.")
                } else {
                    bakApk.renameTo(apkFile)
                    tempApk.delete()
                    throw Exception("Failed to rename temp APK to original APK. Restored original.")
                }
            } else {
                tempApk.delete()
                throw Exception("Failed to create backup of original APK. Aborted.")
            }

        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed to shorten resource paths", e)
            if (tempApk.exists()) tempApk.delete()
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

    private fun parseArscStrings(arscBytes: ByteArray): List<String> {
        val bb = ByteBuffer.wrap(arscBytes).order(ByteOrder.LITTLE_ENDIAN)
        bb.short // tableType
        val tableHeaderSize = bb.short
        bb.int // tableSize
        
        bb.position(tableHeaderSize.toInt())
        bb.short // poolType
        bb.short // poolHeaderSize
        bb.int // poolSize
        val stringCount = bb.int
        bb.int // styleCount
        val flags = bb.int
        val stringsStart = bb.int
        
        val isUtf8 = (flags and (1 shl 8)) != 0
        val stringOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) stringOffsets[i] = bb.int
        
        val strings = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val offset = tableHeaderSize.toInt() + stringsStart + stringOffsets[i]
            bb.position(offset)
            strings.add(readString(bb, isUtf8))
        }
        return strings
    }

    private fun patchArsc(arscBytes: ByteArray, pathMap: Map<String, String>): ByteArray {
        val bb = ByteBuffer.wrap(arscBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read ResTable_header
        bb.short // tableType
        val tableHeaderSize = bb.short
        val tableSize = bb.int
        
        // Read ResStringPool_header
        bb.position(tableHeaderSize.toInt())
        bb.short // poolType
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
            decodeLengthUtf8(bb) // charLen
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