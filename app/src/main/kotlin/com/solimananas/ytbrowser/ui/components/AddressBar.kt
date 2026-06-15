package com.solimananas.ytbrowser.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solimananas.ytbrowser.ui.theme.ShieldsGreen
import com.solimananas.ytbrowser.ui.theme.ShieldsOrange

@Composable
fun AddressBar(
    url: String,
    isLoading: Boolean,
    isSecure: Boolean,
    isIncognito: Boolean,
    adsBlocked: Int,
    shieldsEnabled: Boolean,
    onUrlSubmit: (String) -> Unit,
    onRefresh: () -> Unit,
    onStopLoading: () -> Unit,
    onShieldsClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(url) {
        mutableStateOf(TextFieldValue(displayUrl(url)))
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Shields button
        if (shieldsEnabled) {
            val color = if (adsBlocked > 0) ShieldsGreen else ShieldsOrange
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onShieldsClick)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Shields",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                if (adsBlocked > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp),
                        containerColor = color
                    ) {
                        Text(
                            if (adsBlocked > 99) "99+" else adsBlocked.toString(),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }

        // Address input field
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            tonalElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Lock / incognito icon
                Icon(
                    imageVector = when {
                        isIncognito -> Icons.Filled.PersonOff
                        isSecure -> Icons.Filled.Lock
                        else -> Icons.Outlined.LockOpen
                    },
                    contentDescription = null,
                    tint = when {
                        isIncognito -> MaterialTheme.colorScheme.tertiary
                        isSecure -> ShieldsGreen
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))

                if (isFocused) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused && !isFocused) {
                                    textFieldValue = TextFieldValue(
                                        text = url,
                                        selection = TextRange(0, url.length)
                                    )
                                }
                                isFocused = state.isFocused
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { onUrlSubmit(textFieldValue.text) }
                        ),
                        decorationBox = { inner ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    "Search or enter URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    )
                } else {
                    Text(
                        text = displayUrl(url),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isFocused = true
                                focusRequester.requestFocus()
                            }
                    )
                }

                // Loading indicator / refresh
                AnimatedContent(targetState = isLoading) { loading ->
                    if (loading) {
                        IconButton(onClick = onStopLoading, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Bookmark button
        IconButton(
            onClick = onBookmarkClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun displayUrl(url: String): String {
    if (url == "about:blank" || url.startsWith("ytbrowser://")) return ""
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
}

@Composable
fun LinearLoadingIndicator(progress: Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = progress in 0.01f..0.99f,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}
