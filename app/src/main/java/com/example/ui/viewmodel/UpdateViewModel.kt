package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.UpdateCheckState
import com.example.data.model.UpdateManifest
import com.example.data.model.VersionRelease
import com.example.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _checkState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val checkState: StateFlow<UpdateCheckState> = _checkState.asStateFlow()

    private val _pendingPopupManifest = MutableStateFlow<UpdateManifest?>(null)
    val pendingPopupManifest: StateFlow<UpdateManifest?> = _pendingPopupManifest.asStateFlow()

    private val _detectedManifest = MutableStateFlow<UpdateManifest?>(null)
    private val _dismissedBannerVersion = MutableStateFlow<String?>(null)

    val availableBannerManifest: StateFlow<UpdateManifest?> = combine(
        _detectedManifest,
        _dismissedBannerVersion
    ) { detected, dismissedVersion ->
        val available = detected ?: return@combine null
        val versionKey = "${available.latestVersion}_${available.latestVersionCode}"
        if (dismissedVersion == versionKey) null else available
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _hasUnseenReleaseAlert = MutableStateFlow(false)

    val unreadNotificationCount: StateFlow<Int> = combine(
        availableBannerManifest,
        _hasUnseenReleaseAlert
    ) { banner, unseenRelease ->
        (if (banner != null) 1 else 0) + (if (unseenRelease) 1 else 0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _versionHistory = MutableStateFlow<List<VersionRelease>>(
        listOf(updateRepository.currentInstalledRelease)
    )
    val versionHistory: StateFlow<List<VersionRelease>> = _versionHistory.asStateFlow()

    private val _showHomeChangelogPopup = MutableStateFlow(false)
    val showHomeChangelogPopup: StateFlow<Boolean> = _showHomeChangelogPopup.asStateFlow()

    val currentRelease: VersionRelease = updateRepository.currentInstalledRelease

    val preferences = preferencesRepository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.data.model.UserPreferences()
    )

    private var lastCheckTimestampMs: Long = 0L

    init {
        checkOnStartup()
    }

    private fun checkOnStartup() {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            if (BuildConfig.VERSION_CODE > prefs.lastSeenUpdateVersionCode) {
                _showHomeChangelogPopup.value = true
                _hasUnseenReleaseAlert.value = true
            }
            performCheck(
                manifestUrl = prefs.updateManifestUrl,
                respectDismissed = true,
                dismissedVersion = prefs.dismissedUpdateVersion
            )
        }
    }

    fun checkIfStale(minIntervalMs: Long = 5 * 60 * 1000L) {
        if (_checkState.value == UpdateCheckState.Checking) return
        val now = System.currentTimeMillis()
        if (lastCheckTimestampMs > 0L && (now - lastCheckTimestampMs) < minIntervalMs) return
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            performCheck(
                manifestUrl = prefs.updateManifestUrl,
                respectDismissed = true,
                dismissedVersion = prefs.dismissedUpdateVersion
            )
        }
    }

    fun checkManually() {
        _dismissedBannerVersion.value = null
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            performCheck(
                manifestUrl = prefs.updateManifestUrl,
                respectDismissed = false,
                dismissedVersion = prefs.dismissedUpdateVersion
            )
        }
    }

    private suspend fun performCheck(
        manifestUrl: String,
        respectDismissed: Boolean,
        dismissedVersion: String?
    ) {
        _checkState.value = UpdateCheckState.Checking
        val result = updateRepository.checkForUpdates(manifestUrl)
        lastCheckTimestampMs = System.currentTimeMillis()
        _checkState.value = result

        when (result) {
            is UpdateCheckState.UpdateAvailable -> {
                _detectedManifest.value = result.manifest
                val remoteHistory = result.manifest.versionHistory
                val merged = mergeHistory(remoteHistory)
                _versionHistory.value = merged
                if (!respectDismissed || dismissedVersion != result.manifest.latestVersion) {
                    _pendingPopupManifest.value = result.manifest
                }
            }
            is UpdateCheckState.UpToDate -> {
                _detectedManifest.value = null
                _versionHistory.value = mergeHistory(result.serverHistory)
            }
            is UpdateCheckState.Unavailable -> {
                _versionHistory.value = listOf(updateRepository.currentInstalledRelease)
            }
            UpdateCheckState.Idle, UpdateCheckState.Checking -> Unit
        }
    }

    fun dismissUpdateBanner() {
        val available = _detectedManifest.value ?: return
        _dismissedBannerVersion.value = "${available.latestVersion}_${available.latestVersionCode}"
    }

    fun dismissPopupForLater() {
        val manifest = _pendingPopupManifest.value ?: return
        _pendingPopupManifest.value = null
        viewModelScope.launch {
            preferencesRepository.setDismissedUpdateVersion(manifest.latestVersion)
        }
    }

    fun hidePopupTemporarily() {
        _pendingPopupManifest.value = null
    }

    fun showPopupIfAvailable() {
        val available = _detectedManifest.value
            ?: (_checkState.value as? UpdateCheckState.UpdateAvailable)?.manifest
            ?: return
        _pendingPopupManifest.value = available
    }

    fun openHomeUpdatePopup() {
        _showHomeChangelogPopup.value = true
    }

    fun dismissHomeChangelogPopup() {
        _showHomeChangelogPopup.value = false
        _hasUnseenReleaseAlert.value = false
        viewModelScope.launch {
            preferencesRepository.setLastSeenUpdateVersionCode(BuildConfig.VERSION_CODE)
        }
    }

    fun markReleaseAlertSeen() {
        if (!_hasUnseenReleaseAlert.value) return
        _hasUnseenReleaseAlert.value = false
        viewModelScope.launch {
            preferencesRepository.setLastSeenUpdateVersionCode(BuildConfig.VERSION_CODE)
        }
    }

    fun updateManifestEndpoint(url: String) {
        _detectedManifest.value = null
        _dismissedBannerVersion.value = null
        viewModelScope.launch {
            preferencesRepository.setUpdateManifestUrl(url)
            preferencesRepository.clearDismissedUpdateVersion()
            checkManually()
        }
    }

    private fun mergeHistory(remote: List<VersionRelease>): List<VersionRelease> {
        val installed = updateRepository.currentInstalledRelease
        val installedKey = installed.version.trim().removePrefix("v").removePrefix("V").lowercase()
        val filtered = remote.filterNot {
            val remoteKey = it.version.trim().removePrefix("v").removePrefix("V").lowercase()
            remoteKey == installedKey || (it.versionCode > 0 && it.versionCode == installed.versionCode)
        }
        return (filtered + installed).sortedWith { a, b ->
            val cmp = updateRepository.compareVersions(b.version, a.version)
            if (cmp != 0) cmp else b.versionCode.compareTo(a.versionCode)
        }
    }

    companion object {
        fun factory(
            updateRepository: UpdateRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UpdateViewModel(updateRepository, preferencesRepository) as T
            }
        }
    }
}
