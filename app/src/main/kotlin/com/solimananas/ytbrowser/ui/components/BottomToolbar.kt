package com.solimananas.ytbrowser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun BottomToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    isIncognito: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        // Back
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            },
            selected = false,
            onClick = onBack,
            enabled = canGoBack,
            label = null
        )
        // Forward
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            },
            selected = false,
            onClick = onForward,
            enabled = canGoForward,
            label = null
        )
        // Home
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            selected = false,
            onClick = onHome,
            label = null
        )
        // Tabs
        NavigationBarItem(
            icon = {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (tabCount > 99) "∞" else tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isIncognito)
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            selected = false,
            onClick = onTabs,
            label = null
        )
        // Menu
        NavigationBarItem(
            icon = { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") },
            selected = false,
            onClick = onMenu,
            label = null
        )
    }
}
