package com.solimananas.ytbrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solimananas.ytbrowser.data.model.BrowserSettings
import com.solimananas.ytbrowser.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<BrowserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserSettings())

    fun setTheme(mode: String) = viewModelScope.launch { repository.updateTheme(mode) }
    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch { repository.updateDynamicColors(enabled) }
    fun setSearchEngine(engine: String) = viewModelScope.launch { repository.updateSearchEngine(engine) }
    fun setHomepage(url: String) = viewModelScope.launch { repository.updateHomepage(url) }
    fun setBlockAds(enabled: Boolean) = viewModelScope.launch { repository.updateBlockAds(enabled) }
    fun setBlockTrackers(enabled: Boolean) = viewModelScope.launch { repository.updateBlockTrackers(enabled) }
    fun setBlockThirdPartyCookies(enabled: Boolean) = viewModelScope.launch { repository.updateBlockThirdPartyCookies(enabled) }
    fun setHttpsOnly(enabled: Boolean) = viewModelScope.launch { repository.updateHttpsOnly(enabled) }
    fun setFingerprintProtection(enabled: Boolean) = viewModelScope.launch { repository.updateFingerprintProtection(enabled) }
    fun setJavaScript(enabled: Boolean) = viewModelScope.launch { repository.updateJavaScript(enabled) }
    fun setCookies(enabled: Boolean) = viewModelScope.launch { repository.updateCookies(enabled) }
    fun setDesktopMode(enabled: Boolean) = viewModelScope.launch { repository.updateDesktopMode(enabled) }
    fun setTextSize(size: Int) = viewModelScope.launch { repository.updateTextSize(size) }
    fun setBackgroundPlayback(enabled: Boolean) = viewModelScope.launch { repository.updateBackgroundPlayback(enabled) }
    fun setPiP(enabled: Boolean) = viewModelScope.launch { repository.updatePiP(enabled) }
    fun setSaveData(enabled: Boolean) = viewModelScope.launch { repository.updateSaveData(enabled) }
    fun setClearOnExit(enabled: Boolean) = viewModelScope.launch { repository.updateClearOnExit(enabled) }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
}
