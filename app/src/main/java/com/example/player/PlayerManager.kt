package com.example.player

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.view.TextureView
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.DecoderMode
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackOption(
    val id: String,
    val label: String,
    val language: String?,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerManager(private val appContext: Context) {

    private var player: ExoPlayer? = null
    private var attachedTextureView: TextureView? = null
    private var currentDecoderMode: DecoderMode = DecoderMode.AUTO
    private var currentDecoderFallbackEnabled: Boolean = true
    private var currentTunneledPlaybackEnabled: Boolean = false
    private var currentContinuePlayingDuringCall: Boolean = false
    private var currentVideoItem: VideoItem? = null
    private var mediaController: MediaController? = null
    private var isConnectingSession = false
    private var onPlayerReplacedListener: ((ExoPlayer) -> Unit)? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _videoSize = MutableStateFlow(VideoSize.UNKNOWN)
    val videoSize: StateFlow<VideoSize> = _videoSize.asStateFlow()

    private val _cues = MutableStateFlow<CueGroup>(CueGroup.EMPTY_TIME_ZERO)
    val cues: StateFlow<CueGroup> = _cues.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val audioTracks: StateFlow<List<TrackOption>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackOption>> = _subtitleTracks.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _activeDecoder = MutableStateFlow(DecoderMode.AUTO)
    val activeDecoder: StateFlow<DecoderMode> = _activeDecoder.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isPlaybackEnded = MutableStateFlow(false)
    val isPlaybackEnded: StateFlow<Boolean> = _isPlaybackEnded.asStateFlow()

    private val _endedVideoId = MutableStateFlow<Long?>(null)
    val endedVideoId: StateFlow<Long?> = _endedVideoId.asStateFlow()

    val currentVideoId: Long?
        get() = currentVideoItem?.id

    val audioEffectsController = PlayerAudioEffectsController()

    private var cachedTracks: Tracks = Tracks.EMPTY

    private fun syncPlaybackActiveState(exo: Player) {
        val active = exo.isPlaying || (exo.playWhenReady && exo.playbackState == Player.STATE_BUFFERING)
        _isPlaying.value = active
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val exo = player
            if (exo != null) {
                syncPlaybackActiveState(exo)
            } else {
                _isPlaying.value = isPlaying
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            player?.let { syncPlaybackActiveState(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            when (playbackState) {
                Player.STATE_READY -> {
                    _playbackError.value = null
                    _isPlaybackEnded.value = false
                    if (_endedVideoId.value == currentVideoItem?.id) {
                        _endedVideoId.value = null
                    }
                }
                Player.STATE_BUFFERING -> {
                    _isPlaybackEnded.value = false
                }
                Player.STATE_ENDED -> {
                    _isPlaybackEnded.value = true
                    _endedVideoId.value = currentVideoItem?.id
                }
            }
            player?.let { syncPlaybackActiveState(it) }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _videoSize.value = videoSize
        }

        override fun onCues(cueGroup: CueGroup) {
            _cues.value = cueGroup
        }

        override fun onTracksChanged(tracks: Tracks) {
            cachedTracks = tracks
            parseTracks(tracks)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioEffectsController.attachToAudioSession(audioSessionId)
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackError.value = error.localizedMessage ?: "Playback failed (${error.errorCodeName})"
            _isPlaying.value = false
        }
    }

    @Synchronized
    fun getOrCreatePlayer(context: Context = appContext): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        val created = PlaybackService.buildExoPlayer(
            context = context.applicationContext,
            decoderMode = currentDecoderMode,
            decoderFallbackEnabled = currentDecoderFallbackEnabled,
            tunneledPlaybackEnabled = currentTunneledPlaybackEnabled,
            handleAudioFocus = !currentContinuePlayingDuringCall
        )
        created.addListener(playerListener)
        created.volume = if (_isMuted.value) 0f else 1f
        created.repeatMode = _repeatMode.value
        if (created.audioSessionId != C.AUDIO_SESSION_ID_UNSET && created.audioSessionId > 0) {
            audioEffectsController.attachToAudioSession(created.audioSessionId)
        }
        player = created
        attachedTextureView?.let { created.setVideoTextureView(it) }
        return created
    }

    fun setOnPlayerReplacedListener(listener: ((ExoPlayer) -> Unit)?) {
        onPlayerReplacedListener = listener
    }

    fun attachTextureView(textureView: TextureView?) {
        if (attachedTextureView !== textureView) {
            attachedTextureView = textureView
            player?.setVideoTextureView(textureView)
        }
    }

    fun detachTextureView(textureView: TextureView?) {
        if (attachedTextureView === textureView) {
            player?.clearVideoTextureView(textureView)
            attachedTextureView = null
        }
    }

    fun startBackgroundService() {
        if (mediaController != null || isConnectingSession) return
        isConnectingSession = true
        try {
            val token = SessionToken(
                appContext,
                ComponentName(appContext, PlaybackService::class.java)
            )
            val controllerFuture = MediaController.Builder(appContext, token).buildAsync()
            controllerFuture.addListener(
                {
                    isConnectingSession = false
                    try {
                        mediaController = controllerFuture.get()
                    } catch (_: Exception) {
                    }
                },
                ContextCompat.getMainExecutor(appContext)
            )
        } catch (_: Exception) {
            isConnectingSession = false
        }
    }

    fun loadAndPlay(
        video: VideoItem,
        decoderMode: DecoderMode,
        decoderFallbackEnabled: Boolean = currentDecoderFallbackEnabled,
        tunneledPlaybackEnabled: Boolean = currentTunneledPlaybackEnabled,
        continuePlayingDuringCall: Boolean = currentContinuePlayingDuringCall,
        resumePositionMs: Long = video.lastPositionMs,
        enableBackgroundService: Boolean = true
    ) {
        currentVideoItem = video
        _playbackError.value = null
        _isPlaybackEnded.value = false
        _endedVideoId.value = null
        setContinuePlayingDuringCall(continuePlayingDuringCall)
        if (decoderMode != currentDecoderMode ||
            decoderFallbackEnabled != currentDecoderFallbackEnabled ||
            tunneledPlaybackEnabled != currentTunneledPlaybackEnabled
        ) {
            currentDecoderFallbackEnabled = decoderFallbackEnabled
            currentTunneledPlaybackEnabled = tunneledPlaybackEnabled
            switchDecoderMode(decoderMode, autoPlay = true, overridePositionMs = resumePositionMs)
            if (enableBackgroundService) startBackgroundService()
            return
        }

        val exo = getOrCreatePlayer()
        val mediaItem = buildMediaItem(video)
        exo.setMediaItem(mediaItem)
        val startPos = calculateResumeStartPositionMs(
            resumePositionMs = resumePositionMs,
            durationMs = video.durationMs
        )
        exo.seekTo(startPos)
        exo.playbackParameters = PlaybackParameters(_playbackSpeed.value)
        exo.prepare()
        exo.playWhenReady = true
        if (enableBackgroundService) {
            startBackgroundService()
        }
    }

    fun setContinuePlayingDuringCall(enabled: Boolean) {
        if (currentContinuePlayingDuringCall == enabled && player == null) return
        currentContinuePlayingDuringCall = enabled
        player?.let { exo ->
            exo.setAudioAttributes(PlaybackService.defaultMovieAudioAttributes, !enabled)
            if (enabled) {
                exo.volume = 1.0f
            }
        }
    }

    fun updateAdvancedDecoderSettings(
        decoderMode: DecoderMode = currentDecoderMode,
        decoderFallbackEnabled: Boolean = currentDecoderFallbackEnabled,
        tunneledPlaybackEnabled: Boolean = currentTunneledPlaybackEnabled
    ) {
        if (decoderMode == currentDecoderMode &&
            decoderFallbackEnabled == currentDecoderFallbackEnabled &&
            tunneledPlaybackEnabled == currentTunneledPlaybackEnabled
        ) return
        currentDecoderFallbackEnabled = decoderFallbackEnabled
        currentTunneledPlaybackEnabled = tunneledPlaybackEnabled
        if (player != null) {
            switchDecoderMode(decoderMode)
        } else {
            currentDecoderMode = decoderMode
            _activeDecoder.value = decoderMode
        }
    }

    fun switchDecoderMode(newMode: DecoderMode, autoPlay: Boolean = true, overridePositionMs: Long? = null) {
        val prevPlayer = player
        val resumePos = overridePositionMs ?: prevPlayer?.currentPosition ?: 0L
        val wasPlaying = if (overridePositionMs != null) autoPlay else (prevPlayer?.playWhenReady ?: autoPlay)
        val speed = _playbackSpeed.value

        currentDecoderMode = newMode
        _activeDecoder.value = newMode

        prevPlayer?.removeListener(playerListener)
        attachedTextureView?.let { prevPlayer?.clearVideoTextureView(it) }
        prevPlayer?.release()
        player = null

        val fresh = getOrCreatePlayer()
        onPlayerReplacedListener?.invoke(fresh)
        currentVideoItem?.let { item ->
            fresh.setMediaItem(buildMediaItem(item))
            if (resumePos > 0L) fresh.seekTo(resumePos)
            fresh.playbackParameters = PlaybackParameters(speed)
            fresh.prepare()
            fresh.playWhenReady = wasPlaying
        }
    }

    fun reloadWithExternalSubtitle(video: VideoItem, subtitleUri: String) {
        val exo = getOrCreatePlayer()
        val pos = exo.currentPosition
        val wasPlaying = exo.playWhenReady
        val updatedVideo = video.copy(subtitleUri = subtitleUri)
        currentVideoItem = updatedVideo
        _isPlaybackEnded.value = false
        _endedVideoId.value = null
        exo.setMediaItem(buildMediaItem(updatedVideo))
        exo.seekTo(pos)
        exo.prepare()
        exo.playWhenReady = wasPlaying
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        _playbackSpeed.value = clamped
        player?.playbackParameters = PlaybackParameters(clamped)
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        player?.volume = if (muted) 0f else 1f
    }

    fun toggleMute(): Boolean {
        val next = !_isMuted.value
        setMuted(next)
        return next
    }

    fun setRepeatMode(mode: Int) {
        val normalized = when (mode) {
            Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL -> mode
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = normalized
        player?.repeatMode = normalized
    }

    fun cycleRepeatMode(): Int {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        setRepeatMode(next)
        return next
    }

    fun captureVideoScreenshotBitmap(): Bitmap? {
        return try {
            attachedTextureView?.takeIf { it.isAvailable && it.width > 0 && it.height > 0 }?.bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun togglePlayPause() {
        val exo = player ?: return
        if (exo.isPlaying) {
            exo.pause()
        } else {
            if (exo.playbackState == Player.STATE_ENDED) {
                _isPlaybackEnded.value = false
                _endedVideoId.value = null
                exo.seekTo(0L)
            }
            exo.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun play() {
        val exo = player ?: return
        if (exo.playbackState == Player.STATE_ENDED) {
            _isPlaybackEnded.value = false
            _endedVideoId.value = null
            exo.seekTo(0L)
        }
        exo.play()
    }

    fun seekTo(positionMs: Long) {
        val exo = player ?: return
        val max = exo.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, max)
        if (_isPlaybackEnded.value && max != Long.MAX_VALUE && target < max - 1_000L) {
            _isPlaybackEnded.value = false
            _endedVideoId.value = null
        }
        exo.seekTo(target)
    }

    internal fun notifyPlaybackEnded(videoId: Long) {
        _isPlaying.value = false
        _isPlaybackEnded.value = true
        _endedVideoId.value = videoId
    }

    fun seekBy(deltaMs: Long) {
        val exo = player ?: return
        seekTo(exo.currentPosition + deltaMs)
    }

    fun currentPositionMs(): Long = player?.currentPosition ?: 0L

    fun durationMs(): Long = player?.duration?.takeIf { it > 0L } ?: 0L

    fun selectAudioTrack(option: TrackOption) {
        val exo = player ?: return
        val group = cachedTracks.groups.getOrNull(option.groupIndex) ?: return
        exo.trackSelectionParameters = exo.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }

    fun selectSubtitleTrack(option: TrackOption?) {
        val exo = player ?: return
        if (option == null) {
            exo.trackSelectionParameters = exo.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _cues.value = CueGroup.EMPTY_TIME_ZERO
        } else {
            val group = cachedTracks.groups.getOrNull(option.groupIndex) ?: return
            exo.trackSelectionParameters = exo.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
                .build()
        }
    }

    private fun parseTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackOption>()
        val subList = mutableListOf<TrackOption>()

        tracks.groups.forEachIndexed { groupIdx, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase() ?: "UND"
                        val channels = formatAudioChannelLayout(format.channelCount)
                        val codec = formatAudioCodecLabel(format.sampleMimeType)
                        val title = listOfNotNull(
                            format.label?.takeIf { it.isNotBlank() } ?: lang,
                            channels.takeIf { it.isNotBlank() },
                            codec
                        ).joinToString(" • ")
                        audioList.add(
                            TrackOption(
                                id = "a_${groupIdx}_$i",
                                label = title,
                                language = format.language,
                                groupIndex = groupIdx,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase() ?: "CC"
                        val title = format.label?.takeIf { it.isNotBlank() } ?: "Subtitle ($lang)"
                        subList.add(
                            TrackOption(
                                id = "s_${groupIdx}_$i",
                                label = title,
                                language = format.language,
                                groupIndex = groupIdx,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }
        _audioTracks.value = audioList
        _subtitleTracks.value = subList
    }

    private fun buildMediaItem(video: VideoItem): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(Uri.parse(video.uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(video.title)
                    .setAlbumTitle("FAZA Player • ${video.resolutionTier.badge}")
                    .build()
            )

        val isNetworkStream = video.uri.startsWith("http://", ignoreCase = true) ||
            video.uri.startsWith("https://", ignoreCase = true)
        if (isNetworkStream) {
            when {
                video.formatTag.equals("HLS", ignoreCase = true) ||
                    video.mimeType.equals(MimeTypes.APPLICATION_M3U8, ignoreCase = true) -> {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                video.formatTag.equals("DASH", ignoreCase = true) ||
                    video.mimeType.equals(MimeTypes.APPLICATION_MPD, ignoreCase = true) -> {
                    builder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
            }
        }

        video.subtitleUri?.let { subUri ->
            val subMime = when {
                subUri.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
                subUri.endsWith(".ass", true) || subUri.endsWith(".ssa", true) -> MimeTypes.TEXT_SSA
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUri))
                        .setMimeType(subMime)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        return builder.build()
    }

    fun release() {
        try {
            mediaController?.release()
        } catch (_: Exception) {
        }
        mediaController = null
        audioEffectsController.release()
        player?.removeListener(playerListener)
        player?.release()
        player = null
        _isPlaying.value = false
        _isPlaybackEnded.value = false
        _endedVideoId.value = null
    }

    companion object {
        fun isCallAudioModeActive(context: Context): Boolean {
            return try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val mode = am?.mode ?: AudioManager.MODE_NORMAL
                mode == AudioManager.MODE_RINGTONE ||
                    mode == AudioManager.MODE_IN_CALL ||
                    mode == AudioManager.MODE_IN_COMMUNICATION
            } catch (_: Exception) {
                false
            }
        }
    }
}

internal fun calculateResumeStartPositionMs(
    resumePositionMs: Long,
    durationMs: Long
): Long {
    if (resumePositionMs < 1_000L) return 0L
    if (durationMs > 3_000L && resumePositionMs >= durationMs - 2_500L) return 0L
    return resumePositionMs
}

internal fun formatAudioCodecLabel(mimeType: String?): String {
    val normalized = mimeType?.trim()?.lowercase().orEmpty()
    return when (normalized) {
        MimeTypes.AUDIO_AC3 -> "AC3"
        MimeTypes.AUDIO_E_AC3 -> "E-AC3"
        MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3 Atmos"
        MimeTypes.AUDIO_TRUEHD -> "TrueHD"
        MimeTypes.AUDIO_DTS -> "DTS"
        MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
        MimeTypes.AUDIO_DTS_EXPRESS -> "DTS Express"
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_MPEG -> "MP3"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_VORBIS -> "Vorbis"
        MimeTypes.AUDIO_ALAC -> "ALAC"
        "" -> "AUDIO"
        else -> normalized.substringAfter('/').uppercase()
    }
}

internal fun formatAudioChannelLayout(channelCount: Int): String {
    return when {
        channelCount <= 0 -> ""
        channelCount == 1 -> "Mono"
        channelCount == 2 -> "Stereo"
        channelCount == 6 -> "5.1"
        channelCount == 8 -> "7.1"
        else -> "${channelCount}ch"
    }
}

