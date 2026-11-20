package org.sketchware.daygreen.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.sketchware.daygreen.DownloadUtility
import org.sketchware.daygreen.FileCheckUtils
import java.io.File

data class BuildToolItem(
    val title: String,
    val description: String,
    val isInstalled: Boolean,
    val toolId: String,
    val customUrl: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildToolsScreen(onBack: () -> Unit) {
    MaterialTheme(colorScheme = sketchwareColorScheme()) {
        val colors = MaterialTheme.colorScheme
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        var refreshTrigger by remember { mutableStateOf(0) }

        val sdkVersions = listOf("37.0.0", "36.0.0", "35.0.0", "34.0.0", "33.0.0")

        val sdkToolsItems = remember(refreshTrigger) {
            sdkVersions.map { version ->
                val isInstalled = File(context.filesDir, "bin/$version/aapt2").exists() &&
                    File(context.filesDir, "bin/$version/aidl").exists() &&
                    File(context.filesDir, "bin/$version/zipalign").exists()
                BuildToolItem(
                    title = "SDK Manager $version",
                    description = "sdk manager version $version",
                    isInstalled = isInstalled,
                    toolId = "android-sdk-tools-v$version",
                )
            }
        }

        val ndkToolsItems = remember(refreshTrigger) {
            val abi = DownloadUtility.getDeviceAbi()
            val ndkAbi = when (abi) {
                "arm64-v8a" -> "aarch64"
                "armeabi-v7a" -> "arm"
                "x86_64" -> "x86_64"
                else -> "aarch64"
            }
            val cmakeAbi = when (abi) {
                "arm64-v8a" -> "arm64"
                "armeabi-v7a" -> "arm"
                "x86_64" -> "x64"
                else -> "arm64"
            }

            listOf(
                BuildToolItem(
                    title = "Android NDK (r29)",
                    description = "Required for compiling native C/C++ code",
                    isInstalled = FileCheckUtils.isNdkDownloaded(context),
                    toolId = "ndk",
                    customUrl = "https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r29-$ndkAbi.tar.xz",
                ),
                BuildToolItem(
                    title = "CMake (3.25.3)",
                    description = "Build tool to configure and compile native code",
                    isInstalled = FileCheckUtils.isCmakeDownloaded(context),
                    toolId = "cmake",
                    customUrl = "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/3.25.3/cmake-3.25.3-1-linux-$cmakeAbi.tar.gz",
                )
            )
        }

        fun handleItemClick(item: BuildToolItem) {
            if (item.isInstalled) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Remove Tool")
                    .setMessage("Are you sure you want to remove ${item.title}?")
                    .setPositiveButton("Remove") { _, _ ->
                        if (item.toolId == "ndk") {
                            File(context.filesDir, "native/ndk").deleteRecursively()
                        } else if (item.toolId == "cmake") {
                            File(context.filesDir, "native/cmake").deleteRecursively()
                        } else if (item.toolId.startsWith("android-sdk-tools-v")) {
                            val version = item.toolId.removePrefix("android-sdk-tools-v")
                            File(context.filesDir, "bin/$version").deleteRecursively()
                        }
                        refreshTrigger++
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Download Tool")
                    .setMessage("Do you want to download ${item.title}?")
                    .setPositiveButton("Download") { _, _ ->
                        val abi = DownloadUtility.getDeviceAbi()
                        val url = item.customUrl ?: "https://github.com/gus23-okta/DayGreen-Tools/releases/download/${item.toolId}/android-sdk-tools-static-$abi.zip"
                        val destination = if (item.toolId == "ndk") {
                            File(context.filesDir, "native/ndk.tar.xz")
                        } else if (item.toolId == "cmake") {
                            File(context.filesDir, "native/cmake.tar.gz")
                        } else {
                            File(context.filesDir, "bin/${item.toolId.removePrefix("android-sdk-tools-v")}/tools.zip")
                        }
                        (context as? Activity)?.let { act ->
                            DownloadUtility.downloadFile(act, url, destination) {
                                refreshTrigger++
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = colors.background,
            topBar = {
                LargeTopAppBar(
                    title = { Text("Build Tools") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = colors.background,
                        scrolledContainerColor = colors.surfaceVariant,
                        titleContentColor = colors.onBackground,
                        navigationIconContentColor = colors.onBackground,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                item {
                    SectionTitle("Sdk Manager")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    ) {
                        Column {
                            sdkToolsItems.forEachIndexed { index, item ->
                                BuildToolRow(
                                    item = item,
                                    onClick = { handleItemClick(item) }
                                )
                                if (index < sdkToolsItems.lastIndex) {
                                    HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Ndk Manager")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    ) {
                        Column {
                            ndkToolsItems.forEachIndexed { index, item ->
                                BuildToolRow(
                                    item = item,
                                    onClick = { handleItemClick(item) }
                                )
                                if (index < ndkToolsItems.lastIndex) {
                                    HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun BuildToolRow(
    item: BuildToolItem,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val displayTitle = if (item.isInstalled) "${item.title.removePrefix("SDK Manager ")} (Installed)" else item.title.removePrefix("SDK Manager ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
}
