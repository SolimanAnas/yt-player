package com.solimananas.ytbrowser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solimananas.ytbrowser.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onClose: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            // Appearance
            item {
                SettingsSectionHeader("Appearance")
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "Theme",
                    subtitle = settings.themeMode.replaceFirstChar { it.uppercase() }
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(settings.themeMode.replaceFirstChar { it.uppercase() })
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("light", "dark", "amoled", "system").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                    onClick = { viewModel.setTheme(mode); expanded = false }
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.ColorLens,
                    title = "Dynamic Colors",
                    subtitle = "Use wallpaper colors (Android 12+)",
                    checked = settings.dynamicColors,
                    onCheckedChange = viewModel::setDynamicColors
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Privacy & Security
            item { SettingsSectionHeader("Privacy & Security") }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.Block,
                    title = "Block Ads",
                    subtitle = "Block advertisement networks",
                    checked = settings.blockAds,
                    onCheckedChange = viewModel::setBlockAds
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.TrackChanges,
                    title = "Block Trackers",
                    subtitle = "Prevent cross-site tracking",
                    checked = settings.blockTrackers,
                    onCheckedChange = viewModel::setBlockTrackers
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.Cookie,
                    title = "Block Third-Party Cookies",
                    subtitle = "Block cookies from other domains",
                    checked = settings.blockThirdPartyCookies,
                    onCheckedChange = viewModel::setBlockThirdPartyCookies
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.Https,
                    title = "HTTPS-Only Mode",
                    subtitle = "Upgrade insecure connections",
                    checked = settings.httpsOnly,
                    onCheckedChange = viewModel::setHttpsOnly
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Anti-Fingerprinting",
                    subtitle = "Prevent browser fingerprinting",
                    checked = settings.fingerprintProtection,
                    onCheckedChange = viewModel::setFingerprintProtection
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // YouTube & Media
            item { SettingsSectionHeader("YouTube & Media") }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.PlayCircle,
                    title = "Background Playback",
                    subtitle = "Continue playing when screen is off",
                    checked = settings.backgroundPlayback,
                    onCheckedChange = viewModel::setBackgroundPlayback
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.PictureInPicture,
                    title = "Picture in Picture",
                    subtitle = "Float video while using other apps",
                    checked = settings.pipEnabled,
                    onCheckedChange = viewModel::setPiP
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Browsing
            item { SettingsSectionHeader("Browsing") }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.Code,
                    title = "JavaScript",
                    subtitle = "Enable JavaScript on websites",
                    checked = settings.javaScriptEnabled,
                    onCheckedChange = viewModel::setJavaScript
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.Cookie,
                    title = "Cookies",
                    subtitle = "Allow websites to save cookies",
                    checked = settings.cookiesEnabled,
                    onCheckedChange = viewModel::setCookies
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.DesktopWindows,
                    title = "Desktop Mode",
                    subtitle = "Request desktop version of sites",
                    checked = settings.desktopMode,
                    onCheckedChange = viewModel::setDesktopMode
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.DataSaverOn,
                    title = "Save Data",
                    subtitle = "Reduce data usage when on mobile",
                    checked = settings.saveData,
                    onCheckedChange = viewModel::setSaveData
                )
            }
            item {
                SettingsSwitch(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Clear on Exit",
                    subtitle = "Delete browsing data when closing",
                    checked = settings.clearOnExit,
                    onCheckedChange = viewModel::setClearOnExit
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Search
            item { SettingsSectionHeader("Search") }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Search,
                    title = "Search Engine",
                    subtitle = settings.searchEngine.replaceFirstChar { it.uppercase() }
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(settings.searchEngine.replaceFirstChar { it.uppercase() })
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("google", "duckduckgo", "brave", "bing", "ecosia").forEach { engine ->
                                DropdownMenuItem(
                                    text = { Text(engine.replaceFirstChar { it.uppercase() }) },
                                    onClick = { viewModel.setSearchEngine(engine); expanded = false }
                                )
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("About") }
            item {
                ListItem(
                    headlineContent = { Text("YT Browser") },
                    supportingContent = { Text("Version 1.0.0 — Privacy-focused Chromium browser") },
                    leadingContent = { Icon(Icons.Outlined.Info, null) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailing
    )
}
