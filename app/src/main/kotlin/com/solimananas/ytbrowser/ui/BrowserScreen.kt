package com.solimananas.ytbrowser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solimananas.ytbrowser.App
import com.solimananas.ytbrowser.engine.YouTubeEnhancer
import com.solimananas.ytbrowser.service.MediaPlaybackService
import com.solimananas.ytbrowser.ui.components.AddressBar
import com.solimananas.ytbrowser.ui.components.BottomToolbar
import com.solimananas.ytbrowser.ui.components.LinearLoadingIndicator
import com.solimananas.ytbrowser.viewmodel.BrowserScreenState
import com.solimananas.ytbrowser.viewmodel.BrowserViewModel
import com.solimananas.ytbrowser.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserApp(
    browserViewModel: BrowserViewModel,
    settingsViewModel: SettingsViewModel,
    onEnterPiP: () -> Unit
) {
    val uiState by browserViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showShieldsPanel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (uiState.showAddressBar && uiState.screenState == BrowserScreenState.BROWSER) {
                Column {
                    AddressBar(
                        url = uiState.currentUrl,
                        isLoading = uiState.isLoading,
                        isSecure = uiState.currentUrl.startsWith("https://"),
                        isIncognito = uiState.isIncognito,
                        adsBlocked = uiState.adsBlockedCount + uiState.trackersBlockedCount,
                        shieldsEnabled = settings.showShieldsButton && uiState.shields.enabled,
                        onUrlSubmit = { browserViewModel.loadUrl(browserViewModel.searchUrl(it)) },
                        onRefresh = browserViewModel::refresh,
                        onStopLoading = browserViewModel::stopLoading,
                        onShieldsClick = { showShieldsPanel = true },
                        onShareClick = { /* share */ },
                        onBookmarkClick = browserViewModel::toggleBookmark,
                        isBookmarked = uiState.isBookmarked
                    )
                    LinearLoadingIndicator(
                        progress = uiState.loadProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        bottomBar = {
            if (uiState.showBottomBar && !uiState.isFullscreen) {
                BottomToolbar(
                    canGoBack = uiState.canGoBack,
                    canGoForward = uiState.canGoForward,
                    tabCount = uiState.tabs.size,
                    isIncognito = uiState.isIncognito,
                    onBack = browserViewModel::goBack,
                    onForward = browserViewModel::goForward,
                    onHome = browserViewModel::goHome,
                    onTabs = { browserViewModel.setScreenState(BrowserScreenState.TABS) },
                    onMenu = { showMenu = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.screenState) {
                BrowserScreenState.BROWSER -> {
                    BrowserWebView(
                        url = uiState.currentUrl,
                        settings = settings,
                        shields = uiState.shields,
                        isDesktopMode = uiState.isDesktopMode,
                        isReaderMode = uiState.isReaderMode,
                        viewModel = browserViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                BrowserScreenState.TABS -> {
                    TabsScreen(
                        tabs = uiState.tabs,
                        currentTabIndex = uiState.currentTabIndex,
                        onTabSelected = browserViewModel::switchTab,
                        onTabClosed = browserViewModel::closeTab,
                        onNewTab = { browserViewModel.newTab() },
                        onNewIncognitoTab = { browserViewModel.newTab(incognito = true) },
                        onClose = { browserViewModel.setScreenState(BrowserScreenState.BROWSER) }
                    )
                }
                BrowserScreenState.BOOKMARKS -> {
                    val bookmarks by browserViewModel.bookmarks.collectAsState(emptyList())
                    BookmarksScreen(
                        bookmarks = bookmarks,
                        onBookmarkClick = {
                            browserViewModel.loadUrl(it.url)
                            browserViewModel.setScreenState(BrowserScreenState.BROWSER)
                        },
                        onBookmarkDelete = { bookmark ->
                            browserViewModel.deleteBookmark(bookmark.url)
                        },
                        onClose = { browserViewModel.setScreenState(BrowserScreenState.BROWSER) }
                    )
                }
                BrowserScreenState.HISTORY -> {
                    val history by browserViewModel.history.collectAsState(emptyList())
                    HistoryScreen(
                        history = history,
                        onHistoryClick = {
                            browserViewModel.loadUrl(it.url)
                            browserViewModel.setScreenState(BrowserScreenState.BROWSER)
                        },
                        onClearAll = { browserViewModel.clearHistory() },
                        onClose = { browserViewModel.setScreenState(BrowserScreenState.BROWSER) }
                    )
                }
                BrowserScreenState.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onClose = { browserViewModel.setScreenState(BrowserScreenState.BROWSER) }
                    )
                }
                BrowserScreenState.DOWNLOADS -> {
                    DownloadsScreen(
                        onClose = { browserViewModel.setScreenState(BrowserScreenState.BROWSER) }
                    )
                }
            }
        }
    }

    // Browser menu dropdown
    if (showMenu) {
        BrowserMenu(
            uiState = uiState,
            onDismiss = { showMenu = false },
            onBookmarks = {
                showMenu = false
                browserViewModel.setScreenState(BrowserScreenState.BOOKMARKS)
            },
            onHistory = {
                showMenu = false
                browserViewModel.setScreenState(BrowserScreenState.HISTORY)
            },
            onDownloads = {
                showMenu = false
                browserViewModel.setScreenState(BrowserScreenState.DOWNLOADS)
            },
            onSettings = {
                showMenu = false
                browserViewModel.setScreenState(BrowserScreenState.SETTINGS)
            },
            onNewTab = {
                showMenu = false
                browserViewModel.newTab()
            },
            onIncognito = {
                showMenu = false
                browserViewModel.newTab(incognito = true)
            },
            onDesktopMode = {
                showMenu = false
                browserViewModel.toggleDesktopMode()
            },
            onReaderMode = {
                showMenu = false
                browserViewModel.toggleReaderMode()
            },
            onFindInPage = {
                showMenu = false
                browserViewModel.toggleFindInPage()
            },
            onPiP = {
                showMenu = false
                onEnterPiP()
            }
        )
    }

    // Shields panel bottom sheet
    if (showShieldsPanel) {
        ShieldsPanel(
            shields = uiState.shields,
            adsBlocked = uiState.adsBlockedCount,
            trackersBlocked = uiState.trackersBlockedCount,
            bandwidthSaved = uiState.bandwidthSaved,
            onDismiss = { showShieldsPanel = false },
            onShieldsToggle = browserViewModel::setShieldsEnabled,
            onShieldsUpdate = browserViewModel::updateShields
        )
    }

    // Find in page bar
    if (uiState.showFindInPage) {
        FindInPageBar(
            query = uiState.findInPageQuery,
            onQueryChange = browserViewModel::findInPage,
            onNext = browserViewModel::findNext,
            onPrevious = browserViewModel::findPrevious,
            onDismiss = browserViewModel::toggleFindInPage
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    url: String,
    settings: com.solimananas.ytbrowser.data.model.BrowserSettings,
    shields: com.solimananas.ytbrowser.data.model.ShieldsConfig,
    isDesktopMode: Boolean,
    isReaderMode: Boolean,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as App

    val webView = remember {
        createWebView(context, viewModel, app, shields, settings)
    }

    // Update WebView settings when they change
    LaunchedEffect(settings.javaScriptEnabled, settings.cookiesEnabled, isDesktopMode) {
        webView.settings.apply {
            javaScriptEnabled = settings.javaScriptEnabled
            userAgentString = if (isDesktopMode) YouTubeEnhancer.buildDesktopUserAgent()
                             else YouTubeEnhancer.buildMobileUserAgent()
        }
        if (!settings.cookiesEnabled) {
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    // Reader mode toggle
    LaunchedEffect(isReaderMode) {
        if (isReaderMode) YouTubeEnhancer.enableReaderMode(webView)
    }

    // Register WebView with ViewModel
    DisposableEffect(webView) {
        viewModel.setWebView(webView)
        onDispose { viewModel.setWebView(null) }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { wv ->
            if (url != "about:blank" && url != wv.url && url.isNotEmpty()) {
                wv.loadUrl(url)
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    viewModel: BrowserViewModel,
    app: App,
    shields: com.solimananas.ytbrowser.data.model.ShieldsConfig,
    settings: com.solimananas.ytbrowser.data.model.BrowserSettings
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        this.settings.apply {
            javaScriptEnabled = settings.javaScriptEnabled
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false   // Required for autoplay
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(true)
            userAgentString = YouTubeEnhancer.buildMobileUserAgent()
        }

        CookieManager.getInstance().setAcceptCookie(settings.cookiesEnabled)

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()

                // HTTPS upgrade
                if (shields.httpsUpgrade && url.startsWith("http://")) {
                    view.post { view.loadUrl(url.replace("http://", "https://")) }
                    return app.adBlockEngine.createBlockedResponse()
                }

                // Ad/tracker blocking
                if (shields.enabled && shields.blockAds) {
                    val pageUrl = view.url ?: ""
                    val tabId = viewModel.uiState.value.currentTab?.id ?: "default"
                    if (app.adBlockEngine.shouldBlock(request, pageUrl, tabId)) {
                        return app.adBlockEngine.createBlockedResponse()
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                viewModel.onPageStarted(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                viewModel.onPageFinished(url, view.title ?: "")
                viewModel.onNavigationStateChanged(view.canGoBack(), view.canGoForward())

                if (YouTubeEnhancer.isYouTubePage(url)) {
                    if (settings.backgroundPlayback) YouTubeEnhancer.injectBackgroundPlayback(view)
                    if (shields.enabled && shields.blockAds) YouTubeEnhancer.injectAdRemoval(view)
                    YouTubeEnhancer.injectMediaSession(view)
                }
                if (shields.fingerprintProtection) YouTubeEnhancer.injectFingerprintProtection(view)

                // Inject cosmetic filters
                val host = view.url?.let { Uri.parse(it).host } ?: return
                val selectors = app.adBlockEngine.getCosmeticFilters(host)
                if (selectors.isNotEmpty()) {
                    val js = selectors.joinToString(";") { "document.querySelectorAll('$it').forEach(e=>e.remove())" }
                    view.evaluateJavascript(js, null)
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (settings.httpsOnly) {
                    handler.cancel()
                } else {
                    handler.proceed()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // Handle mailto:, tel:, intent: schemes
                if (!url.startsWith("http://") && !url.startsWith("https://") &&
                    !url.startsWith("about:") && !url.startsWith("ytbrowser://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) { /* ignore */ }
                    return true
                }
                return false
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                viewModel.onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                viewModel.onReceivedTitle(title)
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                viewModel.setFullscreen(true)
            }

            override fun onHideCustomView() {
                viewModel.setFullscreen(false)
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                // Deny all permissions by default - user must grant them
                request.deny()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, false, false)
            }

            override fun onDownloadStart(
                url: String, userAgent: String, contentDisposition: String,
                mimetype: String, contentLength: Long
            ) {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val intent = Intent(context, com.solimananas.ytbrowser.service.DownloadService::class.java).apply {
                    action = com.solimananas.ytbrowser.service.DownloadService.ACTION_DOWNLOAD
                    putExtra(com.solimananas.ytbrowser.service.DownloadService.EXTRA_URL, url)
                    putExtra(com.solimananas.ytbrowser.service.DownloadService.EXTRA_FILE_NAME, fileName)
                    putExtra(com.solimananas.ytbrowser.service.DownloadService.EXTRA_MIME_TYPE, mimetype)
                }
                context.startService(intent)
            }
        }

        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val intent = Intent(context, com.solimananas.ytbrowser.service.DownloadService::class.java).apply {
                action = com.solimananas.ytbrowser.service.DownloadService.ACTION_DOWNLOAD
                putExtra(com.solimananas.ytbrowser.service.DownloadService.EXTRA_URL, url)
                putExtra(com.solimananas.ytbrowser.service.DownloadService.EXTRA_FILE_NAME, fileName)
                putExtra(com.solimananas.ytbrowser.service.DownloadService.EXTRA_MIME_TYPE, mimeType)
            }
            context.startService(intent)
        }
    }
}

@Composable
fun BrowserMenu(
    uiState: com.solimananas.ytbrowser.viewmodel.BrowserUiState,
    onDismiss: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onNewTab: () -> Unit,
    onIncognito: () -> Unit,
    onDesktopMode: () -> Unit,
    onReaderMode: () -> Unit,
    onFindInPage: () -> Unit,
    onPiP: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 200.dp)
    ) {
        DropdownMenuItem(
            text = { Text("New Tab") },
            leadingIcon = { Icon(Icons.Outlined.Add, null) },
            onClick = onNewTab
        )
        DropdownMenuItem(
            text = { Text("New Incognito Tab") },
            leadingIcon = { Icon(Icons.Outlined.PersonOff, null) },
            onClick = onIncognito
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Bookmarks") },
            leadingIcon = { Icon(Icons.Outlined.Bookmarks, null) },
            onClick = onBookmarks
        )
        DropdownMenuItem(
            text = { Text("History") },
            leadingIcon = { Icon(Icons.Outlined.History, null) },
            onClick = onHistory
        )
        DropdownMenuItem(
            text = { Text("Downloads") },
            leadingIcon = { Icon(Icons.Outlined.Download, null) },
            onClick = onDownloads
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (uiState.isDesktopMode) "Mobile Mode" else "Desktop Mode") },
            leadingIcon = { Icon(Icons.Outlined.DesktopWindows, null) },
            onClick = onDesktopMode
        )
        DropdownMenuItem(
            text = { Text("Reader Mode") },
            leadingIcon = { Icon(Icons.Outlined.MenuBook, null) },
            onClick = onReaderMode
        )
        DropdownMenuItem(
            text = { Text("Find in Page") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            onClick = onFindInPage
        )
        DropdownMenuItem(
            text = { Text("Picture in Picture") },
            leadingIcon = { Icon(Icons.Outlined.PictureInPicture, null) },
            onClick = onPiP
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Settings") },
            leadingIcon = { Icon(Icons.Outlined.Settings, null) },
            onClick = onSettings
        )
    }
}

@Composable
fun FindInPageBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find in page...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.KeyboardArrowUp, "Previous")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.KeyboardArrowDown, "Next")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, "Close")
            }
        }
    }
}

