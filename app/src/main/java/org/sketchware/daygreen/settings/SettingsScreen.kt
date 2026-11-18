package org.sketchware.daygreen.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import pro.sketchware.R

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/**
 * Tema gelap/terang mengikuti pengaturan di dalam aplikasi (AppCompatDelegate),
 * bukan pengaturan sistem. Nilainya dibaca dari konfigurasi yang sudah dioverride
 * oleh AppCompat.
 */
@Composable
internal fun isAppDarkTheme(): Boolean {
    val uiMode = LocalConfiguration.current.uiMode
    return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

@Composable
internal fun sketchwareColorScheme(): ColorScheme =
    if (isAppDarkTheme()) {
        darkColorScheme(
            primary = colorResource(R.color.md_theme_dark_primary),
            onPrimary = colorResource(R.color.md_theme_dark_on_primary),
            primaryContainer = colorResource(R.color.md_theme_dark_primary_container),
            onPrimaryContainer = colorResource(R.color.md_theme_dark_on_primary_container),
            secondary = colorResource(R.color.md_theme_dark_secondary),
            onSecondary = colorResource(R.color.md_theme_dark_on_secondary),
            background = colorResource(R.color.md_theme_dark_background),
            onBackground = colorResource(R.color.md_theme_dark_on_background),
            surface = colorResource(R.color.md_theme_dark_surface),
            onSurface = colorResource(R.color.md_theme_dark_on_surface),
            surfaceVariant = colorResource(R.color.md_theme_dark_surface_variant),
            onSurfaceVariant = colorResource(R.color.md_theme_dark_on_surface_variant),
            outline = colorResource(R.color.md_theme_dark_outline),
            error = colorResource(R.color.md_theme_dark_error),
            onError = colorResource(R.color.md_theme_dark_on_error),
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.md_theme_light_primary),
            onPrimary = colorResource(R.color.md_theme_light_on_primary),
            primaryContainer = colorResource(R.color.md_theme_light_primary_container),
            onPrimaryContainer = colorResource(R.color.md_theme_light_on_primary_container),
            secondary = colorResource(R.color.md_theme_light_secondary),
            onSecondary = colorResource(R.color.md_theme_light_on_secondary),
            background = colorResource(R.color.md_theme_light_background),
            onBackground = colorResource(R.color.md_theme_light_on_background),
            surface = colorResource(R.color.md_theme_light_surface),
            onSurface = colorResource(R.color.md_theme_light_on_surface),
            surfaceVariant = colorResource(R.color.md_theme_light_surface_variant),
            onSurfaceVariant = colorResource(R.color.md_theme_light_on_surface_variant),
            outline = colorResource(R.color.md_theme_light_outline),
            error = colorResource(R.color.md_theme_light_error),
            onError = colorResource(R.color.md_theme_light_on_error),
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    settings: List<SettingItem>,
    onBack: () -> Unit,
) {
    MaterialTheme(colorScheme = sketchwareColorScheme()) {
        val colors = MaterialTheme.colorScheme
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = colors.background,
            topBar = {
                LargeTopAppBar(
                    title = { Text("Settings") },
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
            LazyColumn(contentPadding = padding) {
                items(settings) { setting ->
                    ListItem(
                        headlineContent = { Text(setting.title) },
                        supportingContent = { Text(setting.subtitle) },
                        leadingContent = {
                            Icon(
                                imageVector = setting.icon,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = colors.background,
                            headlineColor = colors.onBackground,
                            supportingColor = colors.onSurfaceVariant,
                        ),
                        modifier = Modifier.clickable(onClick = setting.onClick),
                    )
                }
            }
        }
    }
}