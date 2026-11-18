package org.sketchware.daygreen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pro.sketchware.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(onBack: () -> Unit) {
    MaterialTheme(colorScheme = sketchwareColorScheme()) {
        val colors = MaterialTheme.colorScheme
        val context = LocalContext.current
        val kofiTitle = stringResource(R.string.donation_kofi_title)
        val kofiUrl = stringResource(R.string.donation_kofi_url)
        val saweriaTitle = stringResource(R.string.donation_saweria_title)
        val saweriaUrl = stringResource(R.string.donation_saweria_url)
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        fun openUrl(url: String) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = colors.background,
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.donation_title)) },
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
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    ) {
                        Column {
                            DonationRow(title = kofiTitle, onClick = { openUrl(kofiUrl) })
                            HorizontalDivider(color = colors.outline)
                            DonationRow(title = saweriaTitle, onClick = { openUrl(saweriaUrl) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}