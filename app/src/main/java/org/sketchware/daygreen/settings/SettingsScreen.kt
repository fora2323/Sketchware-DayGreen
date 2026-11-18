package org.sketchware.daygreen.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pro.sketchware.R

data class SettingItem(
    @DrawableRes val icon: Int,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

data class SettingSection(
    val header: String,
    val items: List<SettingItem>,
)

@Composable
private fun sketchwareColorScheme(): ColorScheme =
    if (isSystemInDarkTheme()) {
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
    sections: List<SettingSection>,
    onBack: () -> Unit,
) {
    MaterialTheme(colorScheme = sketchwareColorScheme()) {
        val colors = MaterialTheme.colorScheme

        Scaffold(
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                        navigationIconContentColor = colors.onSurface,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                sections.forEach { section ->
                    item {
                        Text(
                            text = section.header,
                            color = colors.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
                        )
                    }
                    items(section.items) { setting ->
                        ListItem(
                            headlineContent = { Text(setting.title) },
                            supportingContent = { Text(setting.subtitle) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(setting.icon),
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
}