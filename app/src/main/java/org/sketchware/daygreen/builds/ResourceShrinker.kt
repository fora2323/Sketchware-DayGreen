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
        val allStrings = mutableSetOf<String>()
        val codeIdentifiers = mutableSetOf<String>()

        val stringLiteralRegex = Regex(""""([a-zA-Z0-9_]{2,})"""")
        val wordRegex = Regex("""\b[a-zA-Z0-9_]{3,}\b""")

        // 1. Baca keep_resources.txt jika dibuat oleh user
        val scId = builder.yq.sc_id
        val keepFile = File(FileUtil.getExternalStorageDir(), ".sketchware/data/$scId/keep_resources.txt")
        if (keepFile.exists()) {
            keepFile.readLines().forEach { line ->
                val name = line.trim()
                if (name.isNotEmpty() && !name.startsWith("#")) {
                    usedNames.add(name)
                }
            }
            LogUtil.d(TAG, "Loaded ${usedNames.size} protected items from keep_resources.txt")
        }

        // 2. Baca keep.xml jika didefinisikan dengan tools:keep
        val resDir = File(builder.yq.resDirectoryPath)
        val keepXmlRegex = Regex("""tools:keep\s*=\s*["']([^"']+)["']""")
        if (resDir.exists()) {
            resDir.walkTopDown().filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }.forEach { xmlFile ->
                try {
                    val content = xmlFile.readText()
                    keepXmlRegex.findAll(content).forEach { match ->
                        val targets = match.groupValues[1].split(",")
                        targets.forEach { target ->
                            val trimmed = target.trim()
                            val cleanName = if (trimmed.contains("/")) trimmed.substringAfterLast("/") else trimmed
                            if (cleanName.isNotEmpty()) {
                                usedNames.add(cleanName)
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Failed reading keep rules from ${xmlFile.name}", e)
                }
            }
        }

        // 3. Kumpulkan referensi dari AndroidManifest.xml
        val manifestFile = File(builder.yq.androidManifestPath)
        val xmlRefRegex = Regex("""@\+?(?:[\w.]+[:])?(\w+)/(\w+)""")
        val xmlAttrRegex = Regex("""\?(?:[\w.]+[:])?(\w+)""")
        if (manifestFile.exists()) {
            try {
                val manifestContent = manifestFile.readText()
                xmlRefRegex.findAll(manifestContent).forEach { match ->
                    usedNames.add(match.groupValues[2])
                }
                xmlAttrRegex.findAll(manifestContent).forEach { match ->
                    usedNames.add(match.groupValues[1])
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to parse AndroidManifest.xml", e)
            }
        }

        // 4. Kumpulkan referensi dari seluruh XML di res project, imported resources, dan local libraries
        val xmlDirs = mutableListOf<File>()
        if (resDir.exists()) xmlDirs.add(resDir)

        val importedResPath = builder.fpu.getPathResource(scId)
        if (!importedResPath.isNullOrEmpty()) {
            val importedResDir = File(importedResPath)
            if (importedResDir.exists() && importedResDir.isDirectory) {
                xmlDirs.add(importedResDir)
            }
        }

        try {
            val localLibResList = builder.mll?.resLocalLibrary
            if (localLibResList != null) {
                for (libResPath in localLibResList) {
                    if (!libResPath.isNullOrEmpty()) {
                        val libResDir = File(libResPath)
                        if (libResDir.exists() && libResDir.isDirectory) {
                            xmlDirs.add(libResDir)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed scanning local library resources", e)
        }

        for (dir in xmlDirs) {
            dir.walkTopDown().filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }.forEach { xmlFile ->
                try {
                    val content = xmlFile.readText()
                    xmlRefRegex.findAll(content).forEach { match ->
                        usedNames.add(match.groupValues[2])
                    }
                    xmlAttrRegex.findAll(content).forEach { match ->
                        usedNames.add(match.groupValues[1])
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Failed to parse XML: ${xmlFile.name}", e)
                }
            }
        }

        // 5. Scan project data Sketchware (view, logic, file) agar resource yang diset di block/designer tidak hilang
        val sketchwareDataDir = File(FileUtil.getExternalStorageDir(), ".sketchware/data/$scId")
        if (sketchwareDataDir.exists()) {
            listOf("view", "logic", "file", "resource_map", "data").forEach { dataName ->
                val dataFile = File(sketchwareDataDir, dataName)
                if (dataFile.exists()) {
                    try {
                        val content = dataFile.readText()
                        stringLiteralRegex.findAll(content).forEach { match ->
                            allStrings.add(match.groupValues[1])
                        }
                        wordRegex.findAll(content).forEach { match ->
                            codeIdentifiers.add(match.value)
                        }
                    } catch (e: Exception) {
                        LogUtil.e(TAG, "Failed reading project data: $dataName", e)
                    }
                }
            }
        }

        // 6. Scan source code Java dan Kotlin (menemukan R.type.name, nama string, dan identifier)
        val sourceDirs = mutableListOf<File>()
        sourceDirs.add(File(builder.yq.javaFilesPath))
        val pathJava = builder.fpu.getPathJava(scId)
        if (!pathJava.isNullOrEmpty()) sourceDirs.add(File(pathJava))
        val pathBroadcast = builder.fpu.getPathBroadcast(scId)
        if (!pathBroadcast.isNullOrEmpty()) sourceDirs.add(File(pathBroadcast))
        val pathService = builder.fpu.getPathService(scId)
        if (!pathService.isNullOrEmpty()) sourceDirs.add(File(pathService))

        val rRefRegex = Regex("""R\.(?:drawable|layout|string|color|anim|animator|raw|mipmap|style|dimen|menu|xml|id|font|array|attr|plurals|interpolator|transition)\.([a-zA-Z0-9_]+)""")

        for (srcDir in sourceDirs) {
            if (!srcDir.exists()) continue
            srcDir.walkTopDown().filter { it.isFile && (it.extension == "java" || it.extension == "kt") }.forEach { srcFile ->
                try {
                    val text = srcFile.readText()
                    rRefRegex.findAll(text).forEach { match ->
                        usedNames.add(match.groupValues[1])
                    }
                    stringLiteralRegex.findAll(text).forEach { match ->
                        allStrings.add(match.groupValues[1])
                    }
                    wordRegex.findAll(text).forEach { match ->
                        codeIdentifiers.add(match.value)
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Failed to scan source file: ${srcFile.name}", e)
                }
            }
        }

        // 7. Parse R.java untuk memetakan resourceId -> resourceName
        val idToName = HashMap<Int, String>()
        val rJavaRegex = Regex("""public\s+static\s+final\s+int\s+([a-zA-Z0-9_]+)\s*=\s*(0x[0-9a-fA-F]+|\d+);""")
        val rJavaDir = File(builder.yq.rJavaDirectoryPath)
        if (rJavaDir.exists()) {
            rJavaDir.walkTopDown().filter { it.isFile && it.name == "R.java" }.forEach { rJavaFile ->
                try {
                    rJavaFile.useLines { lines ->
                        lines.forEach { line ->
                            val match = rJavaRegex.find(line)
                            if (match != null) {
                                val name = match.groupValues[1]
                                val idStr = match.groupValues[2]
                                try {
                                    val id = java.lang.Long.decode(idStr).toInt()
                                    idToName[id] = name
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Failed to parse R.java: ${rJavaFile.name}", e)
                }
            }
            LogUtil.d(TAG, "Mapped ${idToName.size} resource IDs from R.java")
        }

        // 8. Kumpulkan referensi dari SEMUA file DEX (dex output project + library dexes)
        val dexFilesToScan = mutableSetOf<File>()
        val dexDir = File(builder.yq.binDirectoryPath, "dex")
        if (dexDir.exists()) {
            dexDir.listFiles { _, name -> name.startsWith("classes") && name.endsWith(".dex") }?.forEach {
                dexFilesToScan.add(it)
            }
        }

        try {
            val dexesToAdd = builder.dexesToAddButNotMerge
            if (dexesToAdd != null) {
                for (dexFile in dexesToAdd) {
                    if (dexFile.exists() && dexFile.isFile && dexFile.name.endsWith(".dex")) {
                        dexFilesToScan.add(dexFile)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed collecting extra dex files", e)
        }

        dexFilesToScan.forEach { dexFile ->
            try {
                // a. Dex string pool & type names
                FileInputStream(dexFile).use { fis ->
                    val dex = Dex(fis)
                    allStrings.addAll(dex.strings())

                    val typeNames = dex.typeNames()
                    val strings = dex.strings()
                    dex.fieldIds().forEach { fieldId ->
                        val clsName = typeNames[fieldId.declaringClassIndex]
                        if (clsName.contains("R$")) {
                            usedNames.add(strings[fieldId.nameIndex])
                        }
                    }
                }

                // b. Scan DEX byte array untuk 4-byte little endian resource IDs
                if (idToName.isNotEmpty()) {
                    val bytes = dexFile.readBytes()
                    val limit = bytes.size - 4
                    for (i in 0..limit) {
                        if (bytes[i + 3] == 0x7f.toByte()) {
                            val b0 = bytes[i].toInt() and 0xFF
                            val b1 = bytes[i + 1].toInt() and 0xFF
                            val b2 = bytes[i + 2].toInt() and 0xFF
                            val resId = (0x7f shl 24) or (b2 shl 16) or (b1 shl 8) or b0
                            val name = idToName[resId]
                            if (name != null) {
                                usedNames.add(name)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "Failed to parse DEX: ${dexFile.name}", e)
            }
        }

        LogUtil.d(TAG, "Protection index: ${usedNames.size} usedNames, ${allStrings.size} strings, ${codeIdentifiers.size} code tokens")

        // 9. Salin folder res ke shrunk_res dan lakukan penyusutan aman
        val shrunkDir = File(context.cacheDir, "shrunk_res")
        FileUtil.deleteFile(shrunkDir.absolutePath)
        FileUtil.copyDirectory(resDir, shrunkDir)

        var deletedCount = 0
        var deletedSize = 0L

        shrunkDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val resName = getResourceNameFromFile(file)

            if (shouldNeverDelete(file, resName)) {
                return@forEach
            }

            // Safe shrink mode: Dipertahankan jika terdaftar di usedNames (XML/Keep/R$/DEX IDs) ATAU
            // cocok dengan literal string apa pun ATAU muncul sebagai token kata ATAU substring nama cocok
            val isUsed = usedNames.contains(resName) ||
                    allStrings.contains(resName) ||
                    codeIdentifiers.contains(resName) ||
                    allStrings.any { it.contains(resName, ignoreCase = true) }

            if (!isUsed) {
                deletedSize += file.length()
                if (file.delete()) {
                    deletedCount++
                    LogUtil.d(TAG, "Shrunk unused resource: ${file.parentFile?.name ?: ""}/${file.name}")
                }
            }
        }

        LogUtil.d(TAG, "Shrinker deleted $deletedCount unused resource files (Saved ${deletedSize / 1024} KB).")

        // 10. Re-link dengan AAPT2 menggunakan folder yang sudah disusutkan
        val aapt2 = File(context.filesDir, "bin/aapt2")
        val compiler = ResourceCompiler(builder, aapt2, false, null)
        compiler.compileAndRelinkShrunk(shrunkDir.absolutePath)
        
        // 11. Pendekkan dan ratakan (flatten) path resource di dalam APK
        ResourcePathShortener(context, builder).shorten()
    }

    private fun getResourceNameFromFile(file: File): String {
        val fileName = file.name
        if (fileName.endsWith(".9.png", ignoreCase = true)) {
            return fileName.substring(0, fileName.length - ".9.png".length)
        }
        val dot = fileName.lastIndexOf('.')
        return if (dot != -1) fileName.substring(0, dot) else fileName
    }

    private fun shouldNeverDelete(file: File, resName: String): Boolean {
        val parentName = file.parentFile?.name ?: ""

        // 1. Folder values (strings.xml, colors.xml, styles.xml, dimens.xml, dll)
        if (parentName.startsWith("values")) return true

        // 2. Folder xml (file_paths.xml, network_security_config.xml, preferences, dll)
        if (parentName.startsWith("xml")) return true

        // 3. Layouts (main.xml, custom views, fragment layouts, dll) tidak boleh dihapus
        if (parentName.startsWith("layout")) return true

        // 4. Menu XMLs
        if (parentName.startsWith("menu")) return true

        // 5. Raw resources (suara, musik, sertifikat, file json)
        if (parentName.startsWith("raw")) return true

        // 6. Fonts
        if (parentName.startsWith("font")) return true

        // 7. Animasi dan transisi
        if (parentName.startsWith("anim") || parentName.startsWith("transition") || parentName.startsWith("interpolator")) return true

        // 8. Color state lists
        if (parentName.startsWith("color")) return true

        // 9. XML Drawables (vector drawables, shape, selector, ripple, layer-list)
        if (file.extension.equals("xml", ignoreCase = true)) return true

        // 10. Nine-patch drawables
        if (file.name.endsWith(".9.png", ignoreCase = true)) return true

        // 11. Icon launcher / aplikasi dan default assets
        val lower = resName.lowercase()
        if (lower.startsWith("ic_") ||
            lower.startsWith("app_icon") ||
            lower == "icon" ||
            lower.contains("launcher") ||
            lower == "default_image" ||
            lower.startsWith("default_")
        ) {
            return true
        }

        // 12. File keep
        if (resName == "keep" || resName.startsWith("keep_")) return true

        return false
    }
}