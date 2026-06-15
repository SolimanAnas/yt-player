package com.solimananas.ytbrowser.data.model

data class BrowserSettings(
    val themeMode: String = "system",       // "light", "dark", "amoled", "system"
    val dynamicColors: Boolean = true,
    val searchEngine: String = "google",    // "google", "duckduckgo", "bing", "brave"
    val homepageUrl: String = "ytbrowser://newtab",
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val httpsOnly: Boolean = true,
    val dnsSafeSearch: Boolean = false,
    val dnsProvider: String = "system",     // "system", "cloudflare", "google", "quad9"
    val fingerprintProtection: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val locationEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val microphoneEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val popupsBlocked: Boolean = true,
    val desktopMode: Boolean = false,
    val textSize: Int = 100,               // percent
    val backgroundPlayback: Boolean = true,
    val pipEnabled: Boolean = true,
    val saveData: Boolean = false,
    val clearOnExit: Boolean = false,
    val syncEnabled: Boolean = false,
    val passwordSaveEnabled: Boolean = true,
    val autofillEnabled: Boolean = true,
    val translateEnabled: Boolean = true,
    val readerModeEnabled: Boolean = true,
    val showShieldsButton: Boolean = true,
    val showHomeButton: Boolean = true,
    val filterListLastUpdated: Long = 0L
)

data class ShieldsConfig(
    val enabled: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val fingerprintProtection: Boolean = true,
    val httpsUpgrade: Boolean = true
)
