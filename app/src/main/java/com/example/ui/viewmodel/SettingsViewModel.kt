package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AppColorSource
import com.example.data.model.AppIconOverlayConfig
import com.example.data.model.DecoderMode
import com.example.data.model.NowPlayingConfig
import com.example.data.model.PlayerUiConfig
import com.example.data.model.RunningTimeConfig
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isPreferencesLoaded = MutableStateFlow(false)
    val isPreferencesLoaded: StateFlow<Boolean> = _isPreferencesLoaded.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .onEach { _isPreferencesLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun toggleQuickTheme() {
        val current = preferences.value.themeMode
        val next = if (current == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColorEnabled(enabled) }
    }

    fun setAppColorSource(source: AppColorSource) {
        viewModelScope.launch { preferencesRepository.setAppColorSource(source) }
    }

    fun setCustomThemeColorHex(colorHex: Long) {
        viewModelScope.launch { preferencesRepository.setCustomThemeColorHex(colorHex) }
    }

    fun setDecoderMode(mode: DecoderMode) {
        viewModelScope.launch { preferencesRepository.setDecoderMode(mode) }
    }

    fun setDecoderFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDecoderFallbackEnabled(enabled) }
    }

    fun setTunneledPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setTunneledPlaybackEnabled(enabled) }
    }

    fun updateSubtitleStyle(style: SubtitleStyleConfig) {
        viewModelScope.launch { preferencesRepository.updateSubtitleStyle(style) }
    }

    fun updateNowPlayingConfig(config: NowPlayingConfig) {
        viewModelScope.launch { preferencesRepository.updateNowPlayingConfig(config) }
    }

    fun updateRunningTimeConfig(config: RunningTimeConfig) {
        viewModelScope.launch { preferencesRepository.updateRunningTimeConfig(config) }
    }

    fun updateAppIconOverlayConfig(config: AppIconOverlayConfig) {
        viewModelScope.launch { preferencesRepository.updateAppIconOverlayConfig(config) }
    }

    fun updatePlayerUiConfig(config: PlayerUiConfig) {
        viewModelScope.launch { preferencesRepository.updatePlayerUiConfig(config) }
    }

    fun resetPlayerUiConfig() {
        viewModelScope.launch { preferencesRepository.updatePlayerUiConfig(PlayerUiConfig()) }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBackgroundPlaybackEnabled(enabled) }
    }

    fun setContinuePlayingDuringCall(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setContinuePlayingDuringCall(enabled) }
    }

    fun setAutoPip(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoPipEnabled(enabled) }
    }

    fun setForceLandscapeOnOpen(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setForceLandscapeOnOpen(enabled) }
    }

    fun setAutoResumeLastVideoOnRestart(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoResumeLastVideoOnRestart(enabled) }
    }

    fun setMidVideoBreakEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMidVideoBreakEnabled(enabled) }
    }

    fun setPrivatePin(pin: String?) {
        viewModelScope.launch { preferencesRepository.setPrivatePin(pin) }
    }

    fun setHasPromptedMediaAccess(prompted: Boolean) {
        viewModelScope.launch { preferencesRepository.setHasPromptedMediaAccess(prompted) }
    }

    companion object {
        fun factory(preferencesRepository: UserPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(preferencesRepository) as T
                }
            }
    }
}
