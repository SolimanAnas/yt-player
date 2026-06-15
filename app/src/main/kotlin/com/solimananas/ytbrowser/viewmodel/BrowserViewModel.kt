package com.solimananas.ytbrowser.viewmodel

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solimananas.ytbrowser.data.model.*
import com.solimananas.ytbrowser.data.repository.BookmarkRepository
import com.solimananas.ytbrowser.data.repository.HistoryRepository
import com.solimananas.ytbrowser.data.repository.SettingsRepository
import com.solimananas.ytbrowser.engine.AdBlockEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BrowserScreenState {
    BROWSER, TABS, BOOKMARKS, HISTORY, DOWNLOADS, SETTINGS
}

data class BrowserUiState(
    val tabs: List<Tab> = listOf(Tab()),
    val currentTabIndex: Int = 0,
    val isLoading: Boolean = false,
    val loadProgress: Float = 0f,
    val pageTitle: String = "New Tab",
    val currentUrl: String = "about:blank",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val isFullscreen: Boolean = false,
    val isPiPMode: Boolean = false,
    val isReaderMode: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isVideoPlaying: Boolean = false,
    val shields: ShieldsConfig = ShieldsConfig(),
    val screenState: BrowserScreenState = BrowserScreenState.BROWSER,
    val showAddressBar: Boolean = true,
    val showBottomBar: Boolean = true,
    val adsBlockedCount: Int = 0,
    val trackersBlockedCount: Int = 0,
    val bandwidthSaved: Long = 0L,
    val isBookmarked: Boolean = false,
    val findInPageQuery: String = "",
    val showFindInPage: Boolean = false
) {
    val currentTab: Tab? get() = tabs.getOrNull(currentTabIndex)
}

class BrowserViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val adBlockEngine: AdBlockEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _webViewRef = MutableStateFlow<WebView?>(null)

    val bookmarks = bookmarkRepository.getAllBookmarks()
    val history = historyRepository.getAllHistory()

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                val url = state.currentUrl
                if (url.isNotBlank() && url != "about:blank") {
                    bookmarkRepository.isBookmarked(url).collect { isBookmarked ->
                        _uiState.update { it.copy(isBookmarked = isBookmarked) }
                    }
                }
            }
        }
    }

    fun setWebView(webView: WebView?) {
        _webViewRef.value = webView
    }

    fun loadUrl(url: String) {
        val normalizedUrl = normalizeUrl(url)
        _webViewRef.value?.loadUrl(normalizedUrl)
        updateCurrentTab { it.copy(url = normalizedUrl, isLoading = true) }
    }

    fun onPageStarted(url: String) {
        val tabId = _uiState.value.currentTab?.id ?: return
        adBlockEngine.clearTabStats(tabId)
        _uiState.update { state ->
            state.copy(
                currentUrl = url,
                isLoading = true,
                loadProgress = 0f,
                adsBlockedCount = 0,
                trackersBlockedCount = 0
            )
        }
    }

    fun onPageFinished(url: String, title: String) {
        val tabId = _uiState.value.currentTab?.id ?: return
        val (ads, trackers) = adBlockEngine.getTabStats(tabId)
        val bw = adBlockEngine.bandwidthSavedBytes.get()

        _uiState.update { state ->
            state.copy(
                currentUrl = url,
                pageTitle = title.ifBlank { url },
                isLoading = false,
                loadProgress = 1f,
                adsBlockedCount = ads,
                trackersBlockedCount = trackers,
                bandwidthSaved = bw
            )
        }
        updateCurrentTab { it.copy(url = url, title = title, isLoading = false) }

        if (!_uiState.value.isIncognito && url != "about:blank") {
            viewModelScope.launch {
                historyRepository.addVisit(url, title)
            }
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update { it.copy(loadProgress = progress / 100f, isLoading = progress < 100) }
    }

    fun onReceivedTitle(title: String) {
        _uiState.update { it.copy(pageTitle = title) }
        updateCurrentTab { it.copy(title = title) }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
        updateCurrentTab { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun goBack() {
        _webViewRef.value?.goBack()
    }

    fun goForward() {
        _webViewRef.value?.goForward()
    }

    fun refresh() {
        _webViewRef.value?.reload()
    }

    fun stopLoading() {
        _webViewRef.value?.stopLoading()
    }

    fun goHome() {
        loadUrl("ytbrowser://newtab")
    }

    fun newTab(url: String = "about:blank", incognito: Boolean = false) {
        val tab = Tab(url = url, isIncognito = incognito)
        _uiState.update { state ->
            val newTabs = state.tabs + tab
            state.copy(tabs = newTabs, currentTabIndex = newTabs.lastIndex)
        }
        if (url != "about:blank") loadUrl(url)
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            val tabs = state.tabs.filter { it.id != tabId }
            if (tabs.isEmpty()) {
                val newTab = Tab()
                state.copy(tabs = listOf(newTab), currentTabIndex = 0)
            } else {
                val newIndex = state.currentTabIndex.coerceIn(0, tabs.lastIndex)
                state.copy(tabs = tabs, currentTabIndex = newIndex)
            }
        }
    }

    fun switchTab(index: Int) {
        if (index in _uiState.value.tabs.indices) {
            _uiState.update { it.copy(currentTabIndex = index, screenState = BrowserScreenState.BROWSER) }
            _uiState.value.tabs.getOrNull(index)?.url?.let { url ->
                _webViewRef.value?.loadUrl(url)
            }
        }
    }

    fun setScreenState(state: BrowserScreenState) {
        _uiState.update { it.copy(screenState = state) }
    }

    fun toggleIncognito() {
        newTab(incognito = !_uiState.value.isIncognito)
    }

    fun toggleBookmark() {
        val state = _uiState.value
        val url = state.currentUrl
        val title = state.pageTitle
        if (url.isBlank() || url == "about:blank") return

        viewModelScope.launch {
            val isNowBookmarked = bookmarkRepository.toggleBookmark(url, title)
            _uiState.update { it.copy(isBookmarked = isNowBookmarked) }
        }
    }

    fun toggleDesktopMode() {
        val newMode = !_uiState.value.isDesktopMode
        _uiState.update { it.copy(isDesktopMode = newMode) }
    }

    fun toggleReaderMode() {
        _uiState.update { it.copy(isReaderMode = !it.isReaderMode) }
    }

    fun toggleFindInPage() {
        _uiState.update { it.copy(showFindInPage = !it.showFindInPage, findInPageQuery = "") }
    }

    fun findInPage(query: String) {
        _uiState.update { it.copy(findInPageQuery = query) }
        _webViewRef.value?.findAllAsync(query)
    }

    fun findNext() = _webViewRef.value?.findNext(true)
    fun findPrevious() = _webViewRef.value?.findNext(false)

    fun updateShields(shields: ShieldsConfig) {
        _uiState.update { it.copy(shields = shields) }
    }

    fun setShieldsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(shields = it.shields.copy(enabled = enabled)) }
    }

    fun onVideoPlaybackChanged(playing: Boolean) {
        _uiState.update { it.copy(isVideoPlaying = playing) }
    }

    fun onPiPModeChanged(isInPiP: Boolean) {
        _uiState.update { it.copy(
            isPiPMode = isInPiP,
            showAddressBar = !isInPiP,
            showBottomBar = !isInPiP
        ) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(
            isFullscreen = fullscreen,
            showAddressBar = !fullscreen,
            showBottomBar = !fullscreen
        ) }
    }

    fun isVideoPlaying(): Boolean = _uiState.value.isVideoPlaying

    fun deleteBookmark(url: String) {
        viewModelScope.launch { bookmarkRepository.removeBookmark(url) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    fun onBackPressed(): Boolean {
        val state = _uiState.value
        return when {
            state.showFindInPage -> {
                _uiState.update { it.copy(showFindInPage = false) }
                true
            }
            state.screenState != BrowserScreenState.BROWSER -> {
                setScreenState(BrowserScreenState.BROWSER)
                true
            }
            state.canGoBack -> {
                goBack()
                true
            }
            else -> false
        }
    }

    fun searchUrl(query: String): String {
        return if (query.startsWith("http://") || query.startsWith("https://") ||
            (query.contains(".") && !query.contains(" "))) {
            if (query.startsWith("http")) query else "https://$query"
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        }
    }

    private fun normalizeUrl(url: String): String {
        if (url.startsWith("ytbrowser://")) return url
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.contains(".") && !url.contains(" ")) return "https://$url"
        return "https://www.google.com/search?q=${java.net.URLEncoder.encode(url, "UTF-8")}"
    }

    private fun updateCurrentTab(transform: (Tab) -> Tab) {
        _uiState.update { state ->
            val index = state.currentTabIndex
            val tabs = state.tabs.toMutableList()
            if (index in tabs.indices) {
                tabs[index] = transform(tabs[index])
            }
            state.copy(tabs = tabs)
        }
    }
}

class BrowserViewModelFactory(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val adBlockEngine: AdBlockEngine,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BrowserViewModel(bookmarkRepository, historyRepository, adBlockEngine, settingsRepository) as T
    }
}
