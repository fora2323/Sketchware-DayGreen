package mod.hilal.saif.android_manifest

import android.os.Environment
import android.util.Log
import com.google.gson.JsonParseException
import mod.hey.studios.project.ProjectSettings
import mod.hey.studios.util.Helper
import pro.sketchware.utility.FileUtil
import pro.sketchware.utility.GsonUtils.getGson
import pro.sketchware.utility.SketchwareUtil
import pro.sketchware.xml.XmlBuilder
import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

object AndroidManifestInjector {
    private const val TAG = "AndroidManifestInjector"

    private fun isManualEditEnabled(scId: String): Boolean {
        return try {
            ProjectSettings(scId).getValue(
                ProjectSettings.SETTING_MANIFEST_MANUAL_EDIT_ENABLED,
                ProjectSettings.SETTING_MANIFEST_MANUAL_EDIT_ENABLED_DEFAULT
            ) == "true"
        } catch (e: Exception) {
            Log.e(TAG, "Error checking manual edit setting", e)
            false
        }
    }

    private fun injectionDirectory(scId: String): File = File(Environment.getExternalStorageDirectory(), ".sketchware${File.separator}data${File.separator}$scId${File.separator}Injection${File.separator}androidmanifest")

    @JvmStatic
    fun getPathAndroidManifestAttributeInjection(scId: String) = File(injectionDirectory(scId), "attributes.json")

    @JvmStatic
    fun getPathAndroidManifestLauncherActivity(scId: String) = File(injectionDirectory(scId), "activity_launcher.txt")

    @JvmStatic
    fun getPathAndroidManifestActivitiesComponents(scId: String) = File(injectionDirectory(scId), "activities_components.json")

    @JvmStatic
    fun getPathAndroidManifestAppComponents(scId: String) = File(injectionDirectory(scId), "app_components.txt")

    @JvmStatic
    fun getPathManualManifest(scId: String) = File(injectionDirectory(scId), "manual_manifest.xml")

    @JvmStatic
    fun hasManualManifest(scId: String): Boolean {
        if (!isManualEditEnabled(scId)) return false
        val file = getPathManualManifest(scId)
        return file.exists() && file.length() > 0
    }

    @JvmStatic
    fun getManualManifest(scId: String): String? {
        if (!isManualEditEnabled(scId)) return null
        val file = getPathManualManifest(scId)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val content = FileUtil.readFile(file.absolutePath)
            if (content != null && content.trim().startsWith("<?xml") &&
                content.contains("<manifest") && content.contains("</manifest>")) content
            else {
                Log.e(TAG, "Manual manifest is invalid or empty for project: $scId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading manual manifest for project: $scId", e)
            null
        }
    }

    private fun readAndroidManifestAttributeInjections(scId: String): ArrayList<HashMap<String, Any>> {
        return try {
            val file = getPathAndroidManifestAttributeInjection(scId)
            if (!file.exists()) return ArrayList()
            getGson().fromJson(FileUtil.readFile(file.absolutePath), Helper.TYPE_MAP_LIST)
                ?: throw IllegalStateException("result == null")
        } catch (e: JsonParseException) {
            SketchwareUtil.toastError("Failed to parse AndroidManifest attribute injections; Reason: $e")
            ArrayList()
        } catch (e: IllegalStateException) {
            SketchwareUtil.toastError("Failed to parse AndroidManifest attribute injections; Reason: ${e.message}")
            ArrayList()
        }
    }

    @JvmStatic
    fun getP(nx: XmlBuilder, id: String) {
        //bug
        //if (!isManualEditEnabled(id)) return
        for ((index, attribute) in readAndroidManifestAttributeInjections(id).withIndex()) {
            val name = attribute["name"]
            if (name is String) {
                val value = attribute["value"]
                if (value is String && name == "_application_permissions") {
                    XmlBuilder("uses-permission").also {
                        it.addAttributeValue(value)
                        nx.addChildNode(it)
                    }
                } else if (value !is String) {
                    SketchwareUtil.toastError("Invalid AndroidManifest attribute injection value in attribute #${index + 1}")
                }
            } else SketchwareUtil.toastError("Invalid AndroidManifest attribute injection name in attribute #${index + 1}")
        }
    }

    @JvmStatic
    fun getAppAttrs(nx: XmlBuilder, projectId: String) {
        //bug
        //if (isManualEditEnabled(projectId)) 
        addToApp(nx, projectId)
    }

    @JvmStatic
    fun getActivityAttrs(nx: XmlBuilder, projectId: String, actName: String): Boolean {
        //bug
        //if (!isManualEditEnabled(projectId)) return false
        val className = actName.substring(0, actName.indexOf(".java"))
        for ((index, attribute) in readAndroidManifestAttributeInjections(projectId).withIndex()) {
            val name = attribute["name"]
            if (name is String && (className == name || name == "_apply_for_all_activities")) {
                addToAct(nx, projectId, actName)
                return true
            } else if (name !is String) {
                SketchwareUtil.toastError("Invalid AndroidManifest attribute injection name in attribute #${index + 1}")
            }
        }
        return false
    }

    @JvmStatic fun isActivityThemeUsed(nx: XmlBuilder, projectId: String, actName: String) = isActivityAttributeUsed("android:theme", projectId, actName)
    @JvmStatic fun isActivityOrientationUsed(nx: XmlBuilder, projectId: String, actName: String) = isActivityAttributeUsed("android:screenOrientation", projectId, actName)
    @JvmStatic fun isActivityKeyboardUsed(nx: XmlBuilder, projectId: String, actName: String) = isActivityAttributeUsed("android:windowSoftInputMode", projectId, actName)
    @JvmStatic fun isActivityExportedUsed(scId: String, activityName: String) = isActivityAttributeUsed("android:exported", scId, activityName)

    @JvmStatic
    fun isActivityAttributeUsed(attribute: String, scId: String, activityName: String): Boolean {
        //bug
        //if (!isManualEditEnabled(scId)) return false
        val className = activityName.substring(0, activityName.indexOf(".java"))
        for ((index, map) in readAndroidManifestAttributeInjections(scId).withIndex()) {
            val name = map["name"]
            if (name is String) {
                if (className == name || name == "_apply_for_all_activities") {
                    val value = map["value"]
                    if (value is String && value.contains(attribute)) return true
                    if (value !is String) SketchwareUtil.toastError("Invalid AndroidManifest attribute injection value in attribute #${index + 1}")
                }
            } else SketchwareUtil.toastError("Invalid AndroidManifest attribute injection name in attribute #${index + 1}")
        }
        return false
    }

    @JvmStatic
    fun getLauncherActivity(projectId: String): String {
        //bug edit launcher
        //if (!isManualEditEnabled(projectId)) return "main"
        val file = getPathAndroidManifestLauncherActivity(projectId)
        if (file.exists()) {
            val activity = FileUtil.readFile(file.absolutePath)
            if (!activity.contains(" ") && !activity.contains(".")) return activity
        }
        return "main"
    }

    @JvmStatic
    fun setLauncherActivity(projectId: String, activity: String) {
        FileUtil.writeFile(getPathAndroidManifestLauncherActivity(projectId).absolutePath, activity)
    }

    @JvmStatic
    fun mHolder(manifest: String, projectId: String): String {
        //bug
        //if (!isManualEditEnabled(projectId)) return manifest
        getManualManifest(projectId)?.let { return it }
        val lines = ArrayList(Arrays.asList(*manifest.split("\n".toRegex()).toTypedArray()))
        val components = getPathAndroidManifestActivitiesComponents(projectId)
        if (FileUtil.isExistFile(components.absolutePath)) {
            val data: ArrayList<HashMap<String, Any>> = getGson().fromJson(FileUtil.readFile(components.absolutePath), Helper.TYPE_MAP_LIST)
            for (component in data) {
                val name = component["name"] as? String ?: continue
                val value = component["value"] as? String ?: continue
                if (value.trim().isEmpty()) continue
                for (k in 3 until lines.size) {
                    if (lines[k].contains("android:name=\"") && lines[k].contains(name) && lines[k - 1].contains("<activity")) {
                        for (q in k until lines.size) {
                            val line = lines[q]
                            val previous = lines[q - 1]
                            if (line.matches("^\\t\\t<[a-zA-Z_-]+[^>]".toRegex())) {
                                val shortClosing = previous.contains("\"/>") || previous.contains("\" />")
                                val spaceBeforeClosing = previous.contains("\" />")
                                if (shortClosing) {
                                    lines[q - 1] = previous.replace(if (spaceBeforeClosing) "\" />" else "\"/>", "\">") + "\r\n$value\r\n</activity>"
                                } else lines[q - 2] = lines[q - 2] + "\r\n$value"
                                break
                            }
                        }
                    }
                }
            }
        }
        val appComponents = getPathAndroidManifestAppComponents(projectId)
        if (appComponents.exists()) {
            val content = FileUtil.readFile(appComponents.absolutePath)
            if (content.trim().isNotEmpty()) {
                val closingIndex = lines.indexOfLast { it.contains("</application>") }
                if (closingIndex != -1) {
                    lines[closingIndex] = "$content\r\n" + lines[closingIndex]
                } else {
                    // fallback kalau somehow </application> gak ketemu
                    lines[lines.size - 3] = lines[lines.size - 3] + "\r\n$content"
                }
            }
        }
        return lines.joinToString("", prefix = "\n") { it + "\n" }.dropLast(1)
    }

    @JvmStatic
    fun addToApp(nx: XmlBuilder, projectId: String) {
        //bug
        //if (!isManualEditEnabled(projectId)) return
        var themeInjected = false
        for ((index, attribute) in readAndroidManifestAttributeInjections(projectId).withIndex()) {
            if (attribute["name"] == "_application_attrs") {
                val value = attribute["value"]
                if (value is String) {
                    nx.addAttributeValue(value)
                    if (value.contains("android:theme")) themeInjected = true
                } else SketchwareUtil.toastError("Invalid AndroidManifest attribute injection value in attribute #${index + 1}")
            }
        }
        if (!themeInjected) nx.addAttributeValue("android:theme=\"@style/AppTheme\"")
    }

    @JvmStatic
    fun addToAct(nx: XmlBuilder, projectId: String, actName: String) {
        //bug
        //if (!isManualEditEnabled(projectId)) return
        val className = actName.substring(0, actName.indexOf(".java"))
        for ((index, attribute) in readAndroidManifestAttributeInjections(projectId).withIndex()) {
            val name = attribute["name"]
            if (name is String && (className == name || name == "_apply_for_all_activities")) {
                val value = attribute["value"]
                if (value is String) nx.addAttributeValue(value)
                else SketchwareUtil.toastError("Invalid AndroidManifest attribute injection value in attribute #${index + 1}")
            } else if (name !is String) {
                SketchwareUtil.toastError("Invalid AndroidManifest attribute injection name in attribute #${index + 1}")
            }
        }
    }
}