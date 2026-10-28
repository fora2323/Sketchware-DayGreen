package org.sketchware.daygreen.builds

import android.content.Context
import java.io.File
import java.io.FileInputStream
import a.a.a.ProjectBuilder
import mod.agus.jcoderz.dex.Dex
import mod.jbk.build.compiler.resource.ResourceCompiler
import pro.sketchware.utility.FileUtil
import mod.jbk.util.LogUtil

class ResourceShrinker(private val context: Context, private val builder: ProjectBuilder) {
    
    private val TAG = "ResourceShrinker"

    fun shrink() {
        LogUtil.d(TAG, "Starting resource shrinking process...")
        val usedNames = mutableSetOf<String>()
        
        // 1. Baca keep_resources.txt jika dibuat oleh user (Syarat B4)
        // File ditempatkan di folder data project Sketchware
        val keepFile = File(FileUtil.getExternalStorageDir(), ".sketchware/data/${builder.yq.sc_id}/keep_resources.txt")
        if (keepFile.exists()) {
            keepFile.readLines().forEach { line ->
                val name = line.trim()
                if (name.isNotEmpty() && !name.startsWith("#")) {
                    usedNames.add(name)
                }
            }
            LogUtil.d(TAG, "Loaded ${usedNames.size} protected items from keep_resources.txt")
        }

        // 2. Kumpulkan referensi pola @type/name dari XML layout, drawable, manifest, dll (Syarat B3.b)
        val resDir = File(builder.yq.resDirectoryPath)
        val xmlRegex = Regex("@\\+?(?:[\\w.]+[:])?(\\w+)/(\\w+)")
        
        resDir.walkTopDown().filter { it.isFile && it.extension == "xml" }.forEach { xmlFile ->
            val content = xmlFile.readText()
            xmlRegex.findAll(content).forEach { match ->
                usedNames.add(match.groupValues[2])
            }
        }
        
        val manifestFile = File(builder.yq.androidManifestPath)
        if (manifestFile.exists()) {
            xmlRegex.findAll(manifestFile.readText()).forEach { match ->
                usedNames.add(match.groupValues[2])
            }
        }

        // 3. Kumpulkan referensi dari file Dex hasil kompilasi (Syarat B3.a)
        val dexDir = File(builder.yq.binDirectoryPath, "dex")
        val allStrings = mutableSetOf<String>()
        
        dexDir.listFiles { _, name -> name.startsWith("classes") && name.endsWith(".dex") }?.forEach { dexFile ->
            try {
                FileInputStream(dexFile).use { fis ->
                    val dex = Dex(fis)
                    
                    // Ambil semua string literal sebagai lapisan keamanan tambahan untuk 'getIdentifier'
                    allStrings.addAll(dex.strings())
                    
                    // Ekstrak referensi R$type secara eksplisit dari field DEX
                    val typeNames = dex.typeNames()
                    val strings = dex.strings()
                    dex.fieldIds().forEach { fieldId ->
                        val clsName = typeNames[fieldId.declaringClassIndex]
                        if (clsName.contains("R$")) {
                            usedNames.add(strings[fieldId.nameIndex])
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to parse DEX: ${dexFile.name}", e)
            }
        }

        // 4. Salin dan filter folder res
        val shrunkDir = File(context.cacheDir, "shrunk_res")
        FileUtil.deleteFile(shrunkDir.absolutePath)
        FileUtil.copyDirectory(resDir, shrunkDir)

        var deletedCount = 0
        var deletedSize = 0L

        shrunkDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val parentName = file.parentFile.name
            // Hindari memfilter folder values (seperti strings.xml, colors.xml) karena berisiko merusak 
            // validitas XML secara parsial. Kita fokus pada penghapusan resource berbasis file.
            if (!parentName.startsWith("values")) {
                val resName = file.nameWithoutExtension
                
                // File dipertahankan jika terdaftar di usedNames (XML/Keep/R$) ATAU cocok dengan 
                // literal string apa pun di dalam DEX (melindungi pemanggilan reflection)
                val isUsed = usedNames.contains(resName) || allStrings.contains(resName)
                
                if (!isUsed) {
                    deletedSize += file.length()
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        }

        LogUtil.d(TAG, "Shrinker deleted $deletedCount unused resource files (Saved ${deletedSize / 1024} KB).")

        // 5. Re-link dengan AAPT2 menggunakan folder yang sudah disusutkan (Syarat B3.c & B3.d)
        val aapt2 = File(context.filesDir, "bin/aapt2")
        val compiler = ResourceCompiler(builder, aapt2, false, null)
        compiler.compileAndRelinkShrunk(shrunkDir.absolutePath)
    }
}