package org.sketchware.daygreen.builds

import android.content.Context
import java.io.File
import java.util.function.BooleanSupplier
import extensions.anbui.daydream.configs.Configs
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge
import mod.hey.studios.project.proguard.ProguardHandler
import mod.hey.studios.project.stringfog.StringfogHandler
import mod.jbk.build.BuildProgressReceiver
import mod.jbk.build.BuiltInLibraries
import pro.sketchware.utility.FileUtil
import a.a.a.ProjectBuilder
import a.a.a.jC
import a.a.a.lC
import a.a.a.wq
import a.a.a.yB
import a.a.a.yq
import mod.hey.studios.build.BuildSettings
import mod.hey.studios.project.ProjectSettings

class BuildTaskNew @JvmOverloads constructor(
    private val context: Context,
    private val q: yq,
    private val scId: String,
    private val receiver: BuildProgressReceiver,
    private val listener: Listener,
    private val canceled: BooleanSupplier
) {

    enum class Result { SUCCESS, FAILED, CANCELLED }

    interface Listener {
        fun onTaskStart(taskName: String)
        fun onTaskDone(taskName: String, millis: Long)
        fun onTaskSkipped(taskName: String)
        fun onTaskFailed(taskName: String, error: Throwable)
        fun onBuildFinished(result: Result, totalMillis: Long)
    }

    private fun step(
        name: String,
        stepNo: Int,
        skipIf: () -> Boolean = { false },
        body: Step
    ) {
        if (canceled.asBoolean) throw CancelledException()

        if (skipIf()) {
            receiver.onProgress("Task: $name (SKIPPED)", stepNo)
            listener.onTaskSkipped(name)
            return
        }

        receiver.onProgress("Task: $name", stepNo)
        listener.onTaskStart(name)
        val startTime = System.currentTimeMillis()
        try {
            val isUpToDate = body.run()
            if (isUpToDate) {
                receiver.onProgress("Task: $name (UP-TO-DATE)", stepNo)
                listener.onTaskSkipped(name)
            } else {
                listener.onTaskDone(name, System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            listener.onTaskFailed(name, e)
            throw e
        }
    }

    private fun hasFiles(dir: File): Boolean =
        dir.isDirectory && dir.walkTopDown().any { it.isFile }

    private fun interface Step {
        @Throws(Exception::class)
        fun run(): Boolean
    }

    private class CancelledException : Exception()

    private lateinit var builder: ProjectBuilder

    // Silent receiver untuk menyembunyikan log dari compiler internal
    private val silentReceiver = BuildProgressReceiver { _, _ -> }

    private fun hasAidlFiles(): Boolean =
        File(builder.fpu.getPathAidl(scId)).walkTopDown().any {
            it.isFile && it.name.endsWith(".aidl")
        }

    @Throws(Exception::class)
    fun run(): Boolean {
        val totalStartTime = System.currentTimeMillis()

        val buildSettings = BuildSettings(scId)
        val isRelease = buildSettings.getValue(
            BuildSettings.SETTING_BUILD_MODE,
            BuildSettings.BUILD_MODE_DEBUG
        ) == BuildSettings.BUILD_MODE_RELEASE
        val modeName = if (isRelease) "Release" else "Debug"

        try {
            step("deleteTempFiles$modeName", 1) {
                FileUtil.deleteFile(q.projectMyscPath)
                false
            }

            step("generateSourceCode$modeName", 2) {
                q.c(context)
                q.a()
                q.a(context, wq.e("600"))

                if (yB.a(lC.b(scId), "custom_icon")) {
                    q.aa(wq.e() + File.separator + scId + File.separator + "mipmaps")
                    if (yB.a(lC.b(scId), "isIconAdaptive", false)) {
                        q.createLauncherIconXml(
                            """<?xml version="1.0" encoding="utf-8"?>
                            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                                <background android:drawable="@mipmap/ic_launcher_background"/>
                                <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                                <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                            </adaptive-icon>""".trimIndent()
                        )
                    } else {
                        q.a(wq.e() + File.separator + scId + File.separator + "icon.png")
                    }
                }

                jC.d(scId).b(q.resDirectoryPath + File.separator + "drawable-xhdpi")
                jC.d(scId).c(q.resDirectoryPath + File.separator + "raw")
                jC.d(scId).a(q.assetsPath + File.separator + "fonts")

                builder = ProjectBuilder(silentReceiver, context, q)
                false
            }

            step("prepareProject$modeName", 3) {
                val fileManager = jC.b(scId)
                val dataManager = jC.a(scId)
                val libraryManager = jC.c(scId)

                q.a(libraryManager, fileManager, dataManager)
                builder.buildBuiltInLibraryInformation()
                q.b(fileManager, dataManager, libraryManager, builder.builtInLibraryManager)
                q.f()
                q.e()
                builder.maybeExtractAapt2()
                false
            }

            step("extractBuiltInLibraries$modeName", 4) {
                BuiltInLibraries.extractCompileAssets(silentReceiver)
                false
            }

            step(
                name = "mergeAssets$modeName",
                stepNo = 5,
                skipIf = { !hasFiles(File(q.assetsPath)) }
            ) {
                false
            }

            step("compileResources$modeName", 6) {
                builder.compileResources()
            }

            step(
                name = "generateViewBinding$modeName",
                stepNo = 7,
                skipIf = {
                    builder.settings.getValue(
                        ProjectSettings.SETTING_ENABLE_VIEWBINDING,
                        ProjectSettings.SETTING_GENERIC_VALUE_FALSE
                    ) == ProjectSettings.SETTING_GENERIC_VALUE_FALSE
                }
            ) {
                builder.generateViewBinding()
                false
            }

            step("compileKotlin$modeName", 8) {
                KotlinCompilerBridge.compileKotlinCodeIfPossible(silentReceiver, builder)
                false
            }

            step(
                name = "compileAidl$modeName",
                stepNo = 9,
                skipIf = { !hasAidlFiles() }
            ) {
                builder.compileAidl()
                false
            }

            step("compileJavaCode$modeName", 10) {
                builder.compileJavaCode()
            }

            step(
                name = "compileNativeCode$modeName",
                stepNo = 11,
                skipIf = {
                    builder.settings.getValue(ProjectSettings.SETTING_ENABLE_NATIVE, "false") != "true"
                }
            ) {
                builder.compileNativeCode()
                false
            }

            step(
                name = "stringFog$modeName",
                stepNo = 12,
                skipIf = { !StringfogHandler(scId).isStringfogEnabled() }
            ) {
                StringfogHandler(scId).start(silentReceiver, builder)
                false
            }

            step(
                name = "proguard$modeName",
                stepNo = 13,
                skipIf = { !builder.proguard.isShrinkingEnabled() }
            ) {
                ProguardHandler(scId).start(silentReceiver, builder)
                false
            }

            step("dexBuilder$modeName", 14) {
                builder.createDexFilesFromClasses()
            }

            step("mergeDex$modeName", 15) {
                builder.getDexFilesReady()
            }

            step("package$modeName", 16) {
                builder.buildApk()
                false
            }

            step("sign$modeName", 17) {
                if (isRelease) {
                    builder.signReleaseApk()
                } else {
                    builder.signDebugApk()
                }
                false
            }

        } catch (e: CancelledException) {
            listener.onBuildFinished(Result.CANCELLED, System.currentTimeMillis() - totalStartTime)
            return false
        } catch (e: Exception) {
            listener.onBuildFinished(Result.FAILED, System.currentTimeMillis() - totalStartTime)
            throw e
        }

        listener.onBuildFinished(Result.SUCCESS, System.currentTimeMillis() - totalStartTime)
        return true
    }
}