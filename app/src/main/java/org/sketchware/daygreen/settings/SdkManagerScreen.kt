package org.sketchware.daygreen.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.sketchware.daygreen.DownloadUtility
import org.sketchware.daygreen.FileCheckUtils
import java.io.File

data class SdkItemState(
    val api: Int,
    val title: String,
    val description: String,
    val destination: File,
    val isInstalled: Boolean,
    val size: String,
    val url: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdkManagerScreen(onBack: () -> Unit) {
    MaterialTheme(colorScheme = sketchwareColorScheme()) {
        val colors = MaterialTheme.colorScheme
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }

        val sdkItems = remember(refreshTrigger) {
            val list = mutableListOf<SdkItemState>()
            for (api in 26..37) {
                val title = "Android SDK $api"
                val description = "Download android.jar for API $api"
                val destination = File(context.filesDir, "libs/android-$api.jar")
                val isInstalled = destination.exists() && destination.length() > 0
                val size = FileCheckUtils.getSdkVersionSize(context, api.toString())
                val url = "https://github.com/gus23-okta/sketchware-daygreen-build-tools/releases/download/37/android-$api.jar"

                list.add(
                    SdkItemState(
                        api = api,
                        title = title,
                        description = description,
                        destination = destination,
                        isInstalled = isInstalled,
                        size = size,
                        url = url,
                    )
                )
            }
            list
        }

        val filteredItems = remember(sdkItems, searchQuery) {
            val trimmed = searchQuery.trim().lowercase()
            if (trimmed.isEmpty()) {
                sdkItems
            } else {
                sdkItems.filter { item ->
                    item.title.lowercase().contains(trimmed) ||
                        item.api.toString().contains(trimmed) ||
                        item.description.lowercase().contains(trimmed) ||
                        (if (item.isInstalled) "installed".contains(trimmed) else "not installed".contains(trimmed))
                }
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = colors.background,
            topBar = {
                if (isSearching) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search SDK (e.g. 34)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                            scrolledContainerColor = colors.surfaceVariant,
                            titleContentColor = colors.onBackground,
                            navigationIconContentColor = colors.onBackground,
                        ),
                    )
                } else {
                    LargeTopAppBar(
                        title = { Text("SDK Manager") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
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
                }
            },
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                }
                items(filteredItems, key = { it.api }) { item ->
                    SdkCard(
                        item = item,
                        onDownload = {
                            (context as? Activity)?.let { act ->
                                DownloadUtility.downloadFile(act, item.url, item.destination) {
                                    FileCheckUtils.ensureLegacyAndroidJar(context)
                                    refreshTrigger++
                                }
                            }
                        },
                        onRemove = {
                            MaterialAlertDialogBuilder(context)
                                .setTitle("Remove SDK")
                                .setMessage("Are you sure you want to remove ${item.title}?")
                                .setPositiveButton("Remove") { _, _ ->
                                    if (item.destination.exists()) {
                                        item.destination.delete()
                                    }
                                    FileCheckUtils.ensureLegacyAndroidJar(context)
                                    refreshTrigger++
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SdkCard(
    item: SdkItemState,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.isInstalled) colors.primaryContainer else colors.surface,
                ) {
                    Text(
                        text = if (item.isInstalled) "Installed" else "Not Installed",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isInstalled) colors.onPrimaryContainer else colors.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val statusDetail = if (item.isInstalled) {
                "Status: Installed (${item.size} used)"
            } else {
                "Status: Not installed (~30 MB download)"
            }

            Text(
                text = statusDetail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            if (item.isInstalled) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Remove")
                }
            } else {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Download")
                }
            }
        }
    }
}
