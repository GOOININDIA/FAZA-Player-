package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.PlayerUiConfig
import com.example.data.model.PlayerUiThemeStyle
import com.example.data.model.RunningTimeConfig
import com.example.data.model.SortOption
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.VideoItem
import com.example.data.repository.VideoLibraryRepository
import com.example.player.PlayerManager
import com.example.player.TrackOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val MIN_MID_VIDEO_BREAK_DURATION_MS = 20L * 60L * 1000L // 20 minutes minimum
const val MID_VIDEO_BREAK_DURATION_SEC = 10

fun calculateMidVideoBreakTriggerMs(durationMs: Long): Long? {
    if (durationMs < MIN_MID_VIDEO_BREAK_DURATION_MS) return null
    return durationMs / 2L
}

fun shouldTriggerMidVideoBreak(
    enabled: Boolean,
    hasShownThisSession: Boolean,
    isPlaying: Boolean,
    isInPipMode: Boolean,
    previousPositionMs: Long,
    positionMs: Long,
    durationMs: Long
): Boolean {
    if (!enabled || hasShownThisSession || !isPlaying || isInPipMode) return false
    if (previousPositionMs < 0L) return false
    val triggerMs = calculateMidVideoBreakTriggerMs(durationMs) ?: return false
    return previousPositionMs < triggerMs && positionMs in triggerMs..(triggerMs + 12_000L)
}

fun isPlaybackCompletedPosition(positionMs: Long, durationMs: Long): Boolean {
    if (durationMs <= 0L) return false
    val endThresholdMs = if (durationMs > 3_000L) durationMs - 2_500L else durationMs
    return positionMs >= endThresholdMs
}

const val DEFAULT_UP_NEXT_LEAD_MINUTES = 5

fun isSameVideoFolder(a: VideoItem, b: VideoItem): Boolean {
    if (a.isPrivate != b.isPrivate) return false
    return if (a.folderUri != null || b.folderUri != null) {
        a.folderUri == b.folderUri
    } else {
        a.folderName == b.folderName
    }
}

fun buildFolderPlaybackQueue(
    allVideos: List<VideoItem>,
    currentVideo: VideoItem,
    sortOption: SortOption = SortOption.DATE_ADDED_DESC
): List<VideoItem> {
    val sameFolder = allVideos.filter { candidate -> isSameVideoFolder(currentVideo, candidate) }
    val sorted = LibraryViewModel.sortVideos(sameFolder, sortOption)
    return if (sorted.any { it.id == currentVideo.id }) {
        sorted
    } else {
        listOf(currentVideo) + sorted
    }
}

fun resolveNextVideoInFolder(
    queue: List<VideoItem>,
    currentVideo: VideoItem?,
    repeatMode: Int = Player.REPEAT_MODE_OFF
): VideoItem? {
    if (currentVideo == null || queue.size <= 1) return null
    val folderQueue = queue.filter { candidate -> isSameVideoFolder(currentVideo, candidate) }
    if (folderQueue.size <= 1) return null
    val idx = folderQueue.indexOfFirst { it.id == currentVideo.id }
    if (idx < 0) return null
    if (idx + 1 < folderQueue.size) {
        return folderQueue[idx + 1]
    }
    if (repeatMode == Player.REPEAT_MODE_ALL) {
        return folderQueue.firstOrNull { it.id != currentVideo.id }
    }
    return null
}

fun calculateUpNextLeadWindowMs(
    durationMs: Long,
    leadMinutes: Int = DEFAULT_UP_NEXT_LEAD_MINUTES
): Long {
    if (durationMs <= 0L) return 0L
    val configuredLeadMs = leadMinutes.coerceIn(1, 30) * 60_000L
    return if (durationMs > configuredLeadMs) {
        configuredLeadMs
    } else {
        val halfDuration = durationMs / 2L
        val quarterDuration = durationMs / 4L
        quarterDuration.coerceAtLeast(minOf(15_000L, halfDuration))
    }
}

fun shouldShowUpNextInNowPlaying(
    enabled: Boolean,
    hasNextVideoInFolder: Boolean,
    positionMs: Long,
    durationMs: Long,
    leadMinutes: Int = DEFAULT_UP_NEXT_LEAD_MINUTES
): Boolean {
    if (!enabled || !hasNextVideoInFolder || durationMs <= 0L) return false
    val safePos = positionMs.coerceIn(0L, durationMs)
    val remainingMs = (durationMs - safePos).coerceAtLeast(0L)
    val leadWindowMs = calculateUpNextLeadWindowMs(durationMs, leadMinutes)
    return leadWindowMs > 0L && remainingMs <= leadWindowMs
}

class PlayerViewModel(
    private val videoRepository: VideoLibraryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _queue = MutableStateFlow<List<VideoItem>>(emptyList())
    val queue: StateFlow<List<VideoItem>> = _queue.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _isLandscapeLocked = MutableStateFlow(false)
    val isLandscapeLocked: StateFlow<Boolean> = _isLandscapeLocked.asStateFlow()

    private val _isControlsLocked = MutableStateFlow(false)
    val isControlsLocked: StateFlow<Boolean> = _isControlsLocked.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _abPointAMs = MutableStateFlow<Long?>(null)
    val abPointAMs: StateFlow<Long?> = _abPointAMs.asStateFlow()

    private val _abPointBMs = MutableStateFlow<Long?>(null)
    val abPointBMs: StateFlow<Long?> = _abPointBMs.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSec: StateFlow<Int?> = _sleepTimerRemainingSec.asStateFlow()

    private val _sleepAtEndOfVideo = MutableStateFlow(false)
    val sleepAtEndOfVideo: StateFlow<Boolean> = _sleepAtEndOfVideo.asStateFlow()

    private val _midBreakRemainingSec = MutableStateFlow<Int?>(null)
    val midBreakRemainingSec: StateFlow<Int?> = _midBreakRemainingSec.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var midBreakJob: Job? = null
    private var hasShownMidBreakThisSession: Boolean = false
    private var lastObservedMidBreakPosMs: Long = -1L

    @Volatile
    private var isPipActiveForBreak: Boolean = false

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val isBuffering: StateFlow<Boolean> = playerManager.isBuffering
    val isPlaybackEnded: StateFlow<Boolean> = playerManager.isPlaybackEnded
    val endedVideoId: StateFlow<Long?> = playerManager.endedVideoId
    val playbackError: StateFlow<String?> = playerManager.playbackError
    val videoSize = playerManager.videoSize
    val cues = playerManager.cues
    val audioTracks: StateFlow<List<TrackOption>> = playerManager.audioTracks
    val subtitleTracks: StateFlow<List<TrackOption>> = playerManager.subtitleTracks
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val activeDecoder: StateFlow<DecoderMode> = playerManager.activeDecoder
    val isMuted: StateFlow<Boolean> = playerManager.isMuted
    val repeatMode: StateFlow<Int> = playerManager.repeatMode
    val audioEffectEnabled: StateFlow<Boolean> = playerManager.audioEffectsController.audioEffectEnabled
    val equalizerEnabled: StateFlow<Boolean> = playerManager.audioEffectsController.equalizerEnabled
    val eqPresetIndex: StateFlow<Int> = playerManager.audioEffectsController.selectedPresetIndex
    val eqBandLevelsMb: StateFlow<List<Int>> = playerManager.audioEffectsController.bandLevelsMb

    val preferences = preferencesRepository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.data.model.UserPreferences()
    )

    val nextFolderVideo: StateFlow<VideoItem?> = combine(
        _queue,
        _currentVideo,
        playerManager.repeatMode
    ) { queue, current, repeat ->
        resolveNextVideoInFolder(
            queue = queue,
            currentVideo = current,
            repeatMode = repeat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val hasSeenGestureHelp: StateFlow<Boolean?> = preferencesRepository.preferencesFlow
        .map<com.example.data.model.UserPreferences, Boolean?> { it.hasSeenPlayerGestureHelp }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                val pos = playerManager.currentPositionMs()
                _positionMs.value = pos
                val dur = playerManager.durationMs()
                if (dur > 0L) _durationMs.value = dur

                // Enforce active A-B Repeat segment loop
                val pointA = _abPointAMs.value
                val pointB = _abPointBMs.value
                if (pointA != null && pointB != null && pointB > pointA + 300L && pos >= pointB) {
                    playerManager.seekTo(pointA)
                    _positionMs.value = pointA
                }

                // Trigger optional one-time 10s break at 50% midpoint for long videos (>= 20 min)
                val effectiveDur = dur.takeIf { it > 0L } ?: _durationMs.value
                val activePrefs = preferences.value
                val prevObservedPos = lastObservedMidBreakPosMs
                if (_midBreakRemainingSec.value == null && shouldTriggerMidVideoBreak(
                        enabled = activePrefs.midVideoBreakEnabled,
                        hasShownThisSession = hasShownMidBreakThisSession,
                        isPlaying = playerManager.isPlaying.value,
                        isInPipMode = isPipActiveForBreak,
                        previousPositionMs = prevObservedPos,
                        positionMs = pos,
                        durationMs = effectiveDur
                    )
                ) {
                    startMidVideoBreak()
                }
                if (pos >= 0L) {
                    lastObservedMidBreakPosMs = pos
                }

                // Pause when sleep-at-end-of-video triggers near final second
                if (_sleepAtEndOfVideo.value && dur > 2_000L && pos >= dur - 600L && playerManager.isPlaying.value) {
                    playerManager.pause()
                    _sleepAtEndOfVideo.value = false
                }

                tick++
                if (tick % 6 == 0) {
                    val active = _currentVideo.value
                    val ended = playerManager.isPlaybackEnded.value
                    if (active != null && !ended && pos > 0L && !isPlaybackCompletedPosition(pos, effectiveDur)) {
                        videoRepository.updatePlaybackProgress(active.id, pos)
                    }
                }
                delay(400L)
            }
        }

        viewModelScope.launch {
            playerManager.endedVideoId.collect { endedId ->
                if (endedId != null) {
                    handlePlaybackEnded(endedId)
                }
            }
        }
    }

    private suspend fun handlePlaybackEnded(endedVideoId: Long) {
        val current = _currentVideo.value
        val now = System.currentTimeMillis()
        if (current != null && current.id == endedVideoId) {
            _currentVideo.value = current.copy(
                lastPositionMs = 0L,
                lastPlayedAt = now
            )
            _positionMs.value = 0L
        }
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            videoRepository.updatePlaybackProgress(endedVideoId, 0L)
        }
    }

    fun loadVideo(videoId: Long) {
        viewModelScope.launch {
            val all = videoRepository.allVideos.first()
            val target = videoRepository.getVideoById(videoId) ?: return@launch
            val sortOption = preferencesRepository.preferencesFlow.first().videoSortOption
            _queue.value = buildFolderPlaybackQueue(
                allVideos = all,
                currentVideo = target,
                sortOption = sortOption
            )
            playVideoItem(target)
        }
    }

    fun playVideoItem(video: VideoItem) {
        val prefs = preferences.value
        val current = _currentVideo.value
        if (current != null &&
            current.id == video.id &&
            playerManager.durationMs() > 0L &&
            !playerManager.isPlaybackEnded.value
        ) {
            playerManager.updateAdvancedDecoderSettings(
                decoderMode = prefs.decoderMode,
                decoderFallbackEnabled = prefs.decoderFallbackEnabled,
                tunneledPlaybackEnabled = prefs.tunneledPlaybackEnabled
            )
            return
        }
        saveCurrentProgress()
        midBreakJob?.cancel()
        _midBreakRemainingSec.value = null
        hasShownMidBreakThisSession = false
        lastObservedMidBreakPosMs = -1L
        _abPointAMs.value = null
        _abPointBMs.value = null
        _currentVideo.value = video
        _durationMs.value = video.durationMs
        viewModelScope.launch {
            videoRepository.updatePlaybackProgress(video.id, video.lastPositionMs)
        }
        playerManager.loadAndPlay(
            video = video,
            decoderMode = prefs.decoderMode,
            decoderFallbackEnabled = prefs.decoderFallbackEnabled,
            tunneledPlaybackEnabled = prefs.tunneledPlaybackEnabled,
            continuePlayingDuringCall = prefs.continuePlayingDuringCall,
            resumePositionMs = video.lastPositionMs,
            enableBackgroundService = prefs.backgroundPlaybackEnabled
        )
    }

    fun retryPlayback() {
        val active = _currentVideo.value ?: return
        val prefs = preferences.value
        playerManager.loadAndPlay(
            video = active,
            decoderMode = prefs.decoderMode,
            decoderFallbackEnabled = prefs.decoderFallbackEnabled,
            tunneledPlaybackEnabled = prefs.tunneledPlaybackEnabled,
            continuePlayingDuringCall = prefs.continuePlayingDuringCall,
            resumePositionMs = playerManager.currentPositionMs(),
            enableBackgroundService = prefs.backgroundPlaybackEnabled
        )
    }

    fun playNext(): Boolean {
        val list = _queue.value
        val cur = _currentVideo.value ?: return false
        if (_isShuffleEnabled.value && list.size > 1) {
            val candidates = list.filter { it.id != cur.id }
            if (candidates.isNotEmpty()) {
                playVideoItem(candidates.random())
                return true
            }
        }
        val idx = list.indexOfFirst { it.id == cur.id }
        if (idx >= 0 && idx + 1 < list.size) {
            playVideoItem(list[idx + 1])
            return true
        }
        if (playerManager.repeatMode.value == Player.REPEAT_MODE_ALL && list.isNotEmpty()) {
            playVideoItem(list.first())
            return true
        }
        return false
    }

    fun playPrevious(): Boolean {
        val list = _queue.value
        val cur = _currentVideo.value ?: return false
        if (_isShuffleEnabled.value && list.size > 1) {
            val candidates = list.filter { it.id != cur.id }
            if (candidates.isNotEmpty()) {
                playVideoItem(candidates.random())
                return true
            }
        }
        val idx = list.indexOfFirst { it.id == cur.id }
        if (idx > 0) {
            playVideoItem(list[idx - 1])
            return true
        }
        if (playerManager.repeatMode.value == Player.REPEAT_MODE_ALL && list.isNotEmpty()) {
            playVideoItem(list.last())
            return true
        }
        return false
    }

    fun toggleShuffle(): Boolean {
        val next = !_isShuffleEnabled.value
        _isShuffleEnabled.value = next
        return next
    }

    fun toggleMute(): Boolean = playerManager.toggleMute()

    fun cycleRepeatMode(): Int = playerManager.cycleRepeatMode()

    fun cycleAbRepeat(): String {
        val pos = playerManager.currentPositionMs().coerceAtLeast(0L)
        val curA = _abPointAMs.value
        val curB = _abPointBMs.value
        return when {
            curA == null -> {
                _abPointAMs.value = pos
                _abPointBMs.value = null
                "A-B Repeat: Point A set (${com.example.util.Formatters.formatDuration(pos)})"
            }
            curB == null -> {
                if (pos > curA + 500L) {
                    _abPointBMs.value = pos
                    playerManager.seekTo(curA)
                    "A-B Repeat active"
                } else {
                    _abPointAMs.value = pos
                    "A-B Repeat: Move forward to set Point B"
                }
            }
            else -> {
                _abPointAMs.value = null
                _abPointBMs.value = null
                "A-B Repeat cleared"
            }
        }
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepAtEndOfVideo.value = false
        if (minutes == null || minutes <= 0) {
            _sleepTimerRemainingSec.value = null
            return
        }
        _sleepTimerRemainingSec.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val next = (_sleepTimerRemainingSec.value ?: 0) - 1
                if (next <= 0) {
                    _sleepTimerRemainingSec.value = null
                    playerManager.pause()
                    break
                }
                _sleepTimerRemainingSec.value = next
            }
        }
    }

    fun setSleepAfterCurrentVideo(enabled: Boolean) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSec.value = null
        _sleepAtEndOfVideo.value = enabled
    }

    fun toggleAudioEffect(): Boolean = playerManager.audioEffectsController.toggleAudioEffect()

    fun setEqualizerPreset(index: Int) = playerManager.audioEffectsController.selectPreset(index)

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Int) =
        playerManager.audioEffectsController.setBandLevel(bandIndex, levelMb)

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundPlaybackEnabled(enabled)
        }
    }

    fun setPlayerThemeStyle(style: PlayerUiThemeStyle) {
        val updated = preferences.value.playerUiConfig.copy(themeStyle = style)
        updatePlayerUiConfig(updated)
    }

    fun cycleAspectRatio(): AspectRatioMode {
        val entries = AspectRatioMode.entries
        val next = entries[(entries.indexOf(_aspectRatioMode.value) + 1) % entries.size]
        _aspectRatioMode.value = next
        return next
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun toggleOrientationLock(): Boolean {
        val next = !_isLandscapeLocked.value
        _isLandscapeLocked.value = next
        return next
    }

    fun setLandscapeLocked(locked: Boolean) {
        _isLandscapeLocked.value = locked
    }

    fun setControlsLocked(locked: Boolean) {
        _isControlsLocked.value = locked
    }

    fun setDecoderMode(mode: DecoderMode) {
        viewModelScope.launch {
            val prefs = preferences.value
            preferencesRepository.setDecoderMode(mode)
            playerManager.updateAdvancedDecoderSettings(
                decoderMode = mode,
                decoderFallbackEnabled = prefs.decoderFallbackEnabled,
                tunneledPlaybackEnabled = prefs.tunneledPlaybackEnabled
            )
        }
    }

    fun setContinuePlayingDuringCall(enabled: Boolean) {
        playerManager.setContinuePlayingDuringCall(enabled)
        viewModelScope.launch {
            preferencesRepository.setContinuePlayingDuringCall(enabled)
        }
    }

    fun setPipActive(inPip: Boolean) {
        isPipActiveForBreak = inPip
        if (inPip && _midBreakRemainingSec.value != null) {
            dismissMidVideoBreak(resumePlayback = false)
        }
    }

    fun setMidVideoBreakEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMidVideoBreakEnabled(enabled)
        }
        if (!enabled && _midBreakRemainingSec.value != null) {
            dismissMidVideoBreak(resumePlayback = true)
        }
    }

    fun dismissMidVideoBreak(resumePlayback: Boolean = true) {
        midBreakJob?.cancel()
        midBreakJob = null
        _midBreakRemainingSec.value = null
        if (resumePlayback && _currentVideo.value != null) {
            playerManager.play()
        }
    }

    private fun startMidVideoBreak() {
        hasShownMidBreakThisSession = true
        midBreakJob?.cancel()
        playerManager.pause()
        _midBreakRemainingSec.value = MID_VIDEO_BREAK_DURATION_SEC
        midBreakJob = viewModelScope.launch {
            var remaining = MID_VIDEO_BREAK_DURATION_SEC
            while (remaining > 0 && isActive) {
                delay(1_000L)
                remaining--
                if (remaining > 0) {
                    _midBreakRemainingSec.value = remaining
                }
            }
            _midBreakRemainingSec.value = null
            if (isActive && _currentVideo.value != null && !isPipActiveForBreak) {
                playerManager.play()
            }
        }
    }

    fun updateSubtitleStyle(style: SubtitleStyleConfig) {
        viewModelScope.launch {
            preferencesRepository.updateSubtitleStyle(style)
        }
    }

    fun updateNowPlayingConfig(config: com.example.data.model.NowPlayingConfig) {
        viewModelScope.launch {
            preferencesRepository.updateNowPlayingConfig(config)
        }
    }

    fun updateRunningTimeConfig(config: RunningTimeConfig) {
        viewModelScope.launch {
            preferencesRepository.updateRunningTimeConfig(config)
        }
    }

    fun updateAppIconOverlayConfig(config: com.example.data.model.AppIconOverlayConfig) {
        viewModelScope.launch {
            preferencesRepository.updateAppIconOverlayConfig(config)
        }
    }

    fun updatePlayerUiConfig(config: PlayerUiConfig) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerUiConfig(config)
        }
    }

    fun resetPlayerUiConfig() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerUiConfig(PlayerUiConfig())
        }
    }

    fun markGestureHelpSeen() {
        viewModelScope.launch {
            preferencesRepository.setHasSeenGestureHelp(true)
        }
    }

    fun attachExternalSubtitle(uri: Uri) {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            videoRepository.attachSubtitleUri(video.id, uri)
            val updated = video.copy(subtitleUri = uri.toString())
            _currentVideo.value = updated
            playerManager.reloadWithExternalSubtitle(updated, uri.toString())
        }
    }

    fun toggleFavouriteCurrent() {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            videoRepository.toggleFavourite(video)
            _currentVideo.value = video.copy(isFavourite = !video.isFavourite)
        }
    }

    fun saveCurrentProgress() {
        val video = _currentVideo.value ?: return
        val isEnded = playerManager.isPlaybackEnded.value && playerManager.currentVideoId == video.id
        val pos = playerManager.currentPositionMs()
        val dur = playerManager.durationMs().takeIf { it > 0L } ?: video.durationMs
        val effectivePos = if (isEnded || isPlaybackCompletedPosition(pos, dur)) 0L else pos
        if (isEnded || pos > 0L) {
            _currentVideo.value = video.copy(
                lastPositionMs = effectivePos,
                lastPlayedAt = System.currentTimeMillis()
            )
            viewModelScope.launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    videoRepository.updatePlaybackProgress(video.id, effectivePos)
                }
            }
        }
    }

    companion object {
        fun factory(
            videoRepository: VideoLibraryRepository,
            preferencesRepository: UserPreferencesRepository,
            playerManager: PlayerManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(videoRepository, preferencesRepository, playerManager) as T
            }
        }
    }
}
