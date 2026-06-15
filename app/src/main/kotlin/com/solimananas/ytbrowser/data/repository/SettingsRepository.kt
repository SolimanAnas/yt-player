package com.solimananas.ytbrowser.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.solimananas.ytbrowser.data.model.BrowserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val HOMEPAGE_URL = stringPreferencesKey("homepage_url")
        val BLOCK_ADS = booleanPreferencesKey("block_ads")
        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val DNS_SAFE_SEARCH = booleanPreferencesKey("dns_safe_search")
        val DNS_PROVIDER = stringPreferencesKey("dns_provider")
        val FINGERPRINT_PROTECTION = booleanPreferencesKey("fingerprint_protection")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val SAVE_DATA = booleanPreferencesKey("save_data")
        val CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        val POPUPS_BLOCKED = booleanPreferencesKey("popups_blocked")
        val PASSWORD_SAVE = booleanPreferencesKey("password_save")
        val AUTOFILL_ENABLED = booleanPreferencesKey("autofill_enabled")
        val TRANSLATE_ENABLED = booleanPreferencesKey("translate_enabled")
        val READER_MODE_ENABLED = booleanPreferencesKey("reader_mode_enabled")
        val SHOW_SHIELDS_BUTTON = booleanPreferencesKey("show_shields_button")
        val SHOW_HOME_BUTTON = booleanPreferencesKey("show_home_button")
        val FILTER_LIST_LAST_UPDATED = longPreferencesKey("filter_list_last_updated")
    }

    val settings: Flow<BrowserSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            BrowserSettings(
                themeMode = prefs[Keys.THEME_MODE] ?: "system",
                dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
                searchEngine = prefs[Keys.SEARCH_ENGINE] ?: "google",
                homepageUrl = prefs[Keys.HOMEPAGE_URL] ?: "ytbrowser://newtab",
                blockAds = prefs[Keys.BLOCK_ADS] ?: true,
                blockTrackers = prefs[Keys.BLOCK_TRACKERS] ?: true,
                blockThirdPartyCookies = prefs[Keys.BLOCK_THIRD_PARTY_COOKIES] ?: true,
                httpsOnly = prefs[Keys.HTTPS_ONLY] ?: true,
                dnsSafeSearch = prefs[Keys.DNS_SAFE_SEARCH] ?: false,
                dnsProvider = prefs[Keys.DNS_PROVIDER] ?: "system",
                fingerprintProtection = prefs[Keys.FINGERPRINT_PROTECTION] ?: true,
                javaScriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
                cookiesEnabled = prefs[Keys.COOKIES_ENABLED] ?: true,
                desktopMode = prefs[Keys.DESKTOP_MODE] ?: false,
                textSize = prefs[Keys.TEXT_SIZE] ?: 100,
                backgroundPlayback = prefs[Keys.BACKGROUND_PLAYBACK] ?: true,
                pipEnabled = prefs[Keys.PIP_ENABLED] ?: true,
                saveData = prefs[Keys.SAVE_DATA] ?: false,
                clearOnExit = prefs[Keys.CLEAR_ON_EXIT] ?: false,
                popupsBlocked = prefs[Keys.POPUPS_BLOCKED] ?: true,
                passwordSaveEnabled = prefs[Keys.PASSWORD_SAVE] ?: true,
                autofillEnabled = prefs[Keys.AUTOFILL_ENABLED] ?: true,
                translateEnabled = prefs[Keys.TRANSLATE_ENABLED] ?: true,
                readerModeEnabled = prefs[Keys.READER_MODE_ENABLED] ?: true,
                showShieldsButton = prefs[Keys.SHOW_SHIELDS_BUTTON] ?: true,
                showHomeButton = prefs[Keys.SHOW_HOME_BUTTON] ?: true,
                filterListLastUpdated = prefs[Keys.FILTER_LIST_LAST_UPDATED] ?: 0L
            )
        }

    suspend fun updateTheme(mode: String) = update { it[Keys.THEME_MODE] = mode }
    suspend fun updateDynamicColors(enabled: Boolean) = update { it[Keys.DYNAMIC_COLORS] = enabled }
    suspend fun updateSearchEngine(engine: String) = update { it[Keys.SEARCH_ENGINE] = engine }
    suspend fun updateHomepage(url: String) = update { it[Keys.HOMEPAGE_URL] = url }
    suspend fun updateBlockAds(enabled: Boolean) = update { it[Keys.BLOCK_ADS] = enabled }
    suspend fun updateBlockTrackers(enabled: Boolean) = update { it[Keys.BLOCK_TRACKERS] = enabled }
    suspend fun updateBlockThirdPartyCookies(enabled: Boolean) = update { it[Keys.BLOCK_THIRD_PARTY_COOKIES] = enabled }
    suspend fun updateHttpsOnly(enabled: Boolean) = update { it[Keys.HTTPS_ONLY] = enabled }
    suspend fun updateFingerprintProtection(enabled: Boolean) = update { it[Keys.FINGERPRINT_PROTECTION] = enabled }
    suspend fun updateJavaScript(enabled: Boolean) = update { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    suspend fun updateCookies(enabled: Boolean) = update { it[Keys.COOKIES_ENABLED] = enabled }
    suspend fun updateDesktopMode(enabled: Boolean) = update { it[Keys.DESKTOP_MODE] = enabled }
    suspend fun updateTextSize(size: Int) = update { it[Keys.TEXT_SIZE] = size }
    suspend fun updateBackgroundPlayback(enabled: Boolean) = update { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    suspend fun updatePiP(enabled: Boolean) = update { it[Keys.PIP_ENABLED] = enabled }
    suspend fun updateSaveData(enabled: Boolean) = update { it[Keys.SAVE_DATA] = enabled }
    suspend fun updateClearOnExit(enabled: Boolean) = update { it[Keys.CLEAR_ON_EXIT] = enabled }
    suspend fun updateFilterListLastUpdated(ts: Long) = update { it[Keys.FILTER_LIST_LAST_UPDATED] = ts }

    private suspend fun update(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit(transform)
    }
}
