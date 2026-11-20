package com.example.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.FazaApplication
import com.example.MainActivity
import com.example.data.model.DecoderMode

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as FazaApplication
        val player = app.playerManager.getOrCreatePlayer(this)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        app.playerManager.setOnPlayerReplacedListener { updatedPlayer ->
            mediaSession?.player = updatedPlayer
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        (application as? FazaApplication)?.playerManager?.setOnPlayerReplacedListener(null)
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        val defaultMovieAudioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        fun buildExoPlayer(
            context: android.content.Context,
            decoderMode: DecoderMode,
            decoderFallbackEnabled: Boolean = true,
            tunneledPlaybackEnabled: Boolean = false,
            handleAudioFocus: Boolean = true
        ): ExoPlayer {
            val codecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder
                )
                selectCodecInfosForMode(
                    mimeType = mimeType,
                    decoders = decoders,
                    isSoftwareDecoder = ::isSoftwareDecoder,
                    decoderMode = decoderMode,
                    decoderFallbackEnabled = decoderFallbackEnabled
                )
            }

            val renderersFactory = DefaultRenderersFactory(context).apply {
                setEnableDecoderFallback(decoderFallbackEnabled)
                setMediaCodecSelector(codecSelector)
                if (decoderMode == DecoderMode.SOFTWARE) {
                    forceDisableMediaCodecAsynchronousQueueing()
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                } else {
                    forceEnableMediaCodecAsynchronousQueueing()
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                }
            }

            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                        .setAllowAudioMixedMimeTypeAdaptiveness(true)
                        .setTunnelingEnabled(tunneledPlaybackEnabled)
                        .clearVideoSizeConstraints()
                )
            }

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 60_000, 1_500, 3_000)
                .build()

            return ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setAudioAttributes(defaultMovieAudioAttributes, handleAudioFocus)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
        }

        private fun isSoftwareDecoder(info: MediaCodecInfo): Boolean {
            return isSoftwareCodecDescriptor(
                name = info.name,
                softwareOnly = info.softwareOnly,
                hardwareAccelerated = info.hardwareAccelerated
            )
        }

        fun inspectDeviceVideoCodecs(): List<DeviceCodecCapability> {
            val watchedMimes = listOf(
                "H.264 / AVC" to "video/avc",
                "H.265 / HEVC" to "video/hevc",
                "VP9" to "video/x-vnd.on2.vp9",
                "AV1" to "video/av01"
            )
            return try {
                val infos = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos
                    .filter { !it.isEncoder }
                watchedMimes.map { (label, mime) ->
                    val matching = infos.filter { info ->
                        info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
                    }
                    val hwNames = matching.filter { info ->
                        val isSwOnly = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            info.isSoftwareOnly
                        } else {
                            false
                        }
                        val isHwAccel = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            info.isHardwareAccelerated
                        } else {
                            true
                        }
                        !isSoftwareCodecDescriptor(
                            name = info.name,
                            softwareOnly = isSwOnly,
                            hardwareAccelerated = isHwAccel
                        )
                    }.map { it.name }
                    val swNames = matching.filter { info ->
                        val isSwOnly = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            info.isSoftwareOnly
                        } else {
                            false
                        }
                        val isHwAccel = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            info.isHardwareAccelerated
                        } else {
                            true
                        }
                        isSoftwareCodecDescriptor(
                            name = info.name,
                            softwareOnly = isSwOnly,
                            hardwareAccelerated = isHwAccel
                        )
                    }.map { it.name }
                    DeviceCodecCapability(
                        formatLabel = label,
                        hasHardwareDecoder = hwNames.isNotEmpty(),
                        hasSoftwareDecoder = swNames.isNotEmpty(),
                        primaryHardwareCodec = hwNames.firstOrNull(),
                        primarySoftwareCodec = swNames.firstOrNull()
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

internal fun isSoftwareCodecDescriptor(
    name: String,
    softwareOnly: Boolean,
    hardwareAccelerated: Boolean,
    sdkInt: Int = android.os.Build.VERSION.SDK_INT
): Boolean {
    val lower = name.lowercase()
    return softwareOnly ||
        (sdkInt >= android.os.Build.VERSION_CODES.Q && !hardwareAccelerated) ||
        lower.startsWith("c2.android.") ||
        lower.startsWith("omx.google.") ||
        lower.contains("ffmpeg") ||
        lower.endsWith(".sw") ||
        lower.contains(".sw.")
}

internal fun <T> selectCodecInfosForMode(
    mimeType: String,
    decoders: List<T>,
    isSoftwareDecoder: (T) -> Boolean,
    decoderMode: DecoderMode,
    decoderFallbackEnabled: Boolean
): List<T> {
    if (!mimeType.startsWith("video/", ignoreCase = true)) {
        return decoders
    }
    return when (decoderMode) {
        DecoderMode.AUTO -> {
            val hw = decoders.filterNot(isSoftwareDecoder)
            val sw = decoders.filter(isSoftwareDecoder)
            hw + sw
        }
        DecoderMode.HARDWARE -> {
            val hw = decoders.filterNot(isSoftwareDecoder)
            if (hw.isNotEmpty() || !decoderFallbackEnabled) hw else decoders
        }
        DecoderMode.SOFTWARE -> {
            val sw = decoders.filter(isSoftwareDecoder)
            if (sw.isNotEmpty() || !decoderFallbackEnabled) sw else decoders
        }
    }
}

data class DeviceCodecCapability(
    val formatLabel: String,
    val hasHardwareDecoder: Boolean,
    val hasSoftwareDecoder: Boolean,
    val primaryHardwareCodec: String?,
    val primarySoftwareCodec: String?
)
