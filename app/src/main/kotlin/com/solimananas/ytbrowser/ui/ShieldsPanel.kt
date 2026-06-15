package com.solimananas.ytbrowser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solimananas.ytbrowser.data.model.ShieldsConfig
import com.solimananas.ytbrowser.ui.theme.ShieldsGreen
import com.solimananas.ytbrowser.ui.theme.ShieldsOrange
import com.solimananas.ytbrowser.ui.theme.ShieldsRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldsPanel(
    shields: ShieldsConfig,
    adsBlocked: Int,
    trackersBlocked: Int,
    bandwidthSaved: Long,
    onDismiss: () -> Unit,
    onShieldsToggle: (Boolean) -> Unit,
    onShieldsUpdate: (ShieldsConfig) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Shield,
                        null,
                        tint = if (shields.enabled) ShieldsGreen else ShieldsOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Brave Shields", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (shields.enabled) "Protection active" else "Protection off",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (shields.enabled) ShieldsGreen else ShieldsOrange
                        )
                    }
                }
                Switch(
                    checked = shields.enabled,
                    onCheckedChange = onShieldsToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ShieldsGreen,
                        checkedTrackColor = ShieldsGreen.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShieldsStat(
                    value = adsBlocked.toString(),
                    label = "Ads\nBlocked",
                    color = ShieldsGreen
                )
                VerticalDivider(modifier = Modifier.height(48.dp))
                ShieldsStat(
                    value = trackersBlocked.toString(),
                    label = "Trackers\nBlocked",
                    color = ShieldsGreen
                )
                VerticalDivider(modifier = Modifier.height(48.dp))
                ShieldsStat(
                    value = formatBytes(bandwidthSaved),
                    label = "Bandwidth\nSaved",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                "Controls",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            ShieldsToggleRow(
                title = "Block Ads & Trackers",
                enabled = shields.blockAds,
                onToggle = { onShieldsUpdate(shields.copy(blockAds = it)) }
            )
            ShieldsToggleRow(
                title = "Block Third-Party Cookies",
                enabled = shields.blockThirdPartyCookies,
                onToggle = { onShieldsUpdate(shields.copy(blockThirdPartyCookies = it)) }
            )
            ShieldsToggleRow(
                title = "Fingerprint Protection",
                enabled = shields.fingerprintProtection,
                onToggle = { onShieldsUpdate(shields.copy(fingerprintProtection = it)) }
            )
            ShieldsToggleRow(
                title = "HTTPS Upgrade",
                enabled = shields.httpsUpgrade,
                onToggle = { onShieldsUpdate(shields.copy(httpsUpgrade = it)) }
            )
        }
    }
}

@Composable
private fun ShieldsStat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ShieldsToggleRow(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
fun DownloadsScreen(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Download,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No downloads yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Files you download will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
    else -> "${bytes / (1024L * 1024 * 1024)}GB"
}
