package com.example.ui.screens

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon as AndroidIcon
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.data.model.AppIconOverlayPosition
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.NowPlayingPosition
import com.example.data.model.PlayerBarTheme
import com.example.data.model.PlayerControlStyle
import com.example.data.model.PlayerUiThemeStyle
import com.example.data.model.RunningTimeDisplayMode
import com.example.data.model.RunningTimePosition
import com.example.data.model.SeekBarStyle
import com.example.player.PlayerManager
import com.example.ui.components.AlwaysOnAppIconOverlay
import com.example.ui.components.AlwaysOnRunningTimeOverlay
import com.example.ui.components.AppIconOverlayBottomSheet
import com.example.ui.components.FzCustomiseItemsBottomSheet
import com.example.ui.components.FzEqualizerBottomSheet
import com.example.ui.components.FzPlayerUiOverlay
import com.example.ui.components.FzSleepTimerBottomSheet
import com.example.ui.components.FzSpeedPickerBottomSheet
import com.example.ui.components.MidVideoBreakOverlay
import com.example.ui.components.NowPlayingStyleBottomSheet
import com.example.ui.components.PermanentNowPlayingBanner
import com.example.ui.components.PlayerGestureHelpOverlay
import com.example.ui.components.PlayerUiCustomizerBottomSheet
import com.example.ui.components.RunningTimeStyleBottomSheet
import com.example.ui.components.rememberFazaBounceScale
import com.example.ui.components.buildSubtitleTextStyle
import com.example.ui.components.formatFzSpeedLabel
import com.example.ui.components.resolveAppIconOverlayAlignment
import com.example.ui.components.resolvePlayerAccent
import com.example.ui.components.resolveRunningTimeAlignment
import com.example.ui.components.saveBitmapScreenshotToGallery
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.shouldShowUpNextInNowPlaying
import com.example.util.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface GestureHudState {
    data class Brightness(val fraction: Float) : GestureHudState
    data class Volume(val fraction: Float) : GestureHudState
    data class Zoom(val scale: Float) : GestureHudState
    data class Seek(val targetMs: Long, val deltaMs: Long) : GestureHudState
    data class DoubleTapToggle(val isNowPlaying: Boolean) : GestureHudState
}

private data class DoubleTapSkipFeedback(
    val isForward: Boolean,
    val totalSeconds: Int,
    val tick: Int
)

private enum class ActiveDragAxis { NONE, SEEK, BRIGHTNESS, VOLUME }

private val HOLD_SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

private fun formatSpeedLabel(speed: Float): String {
    val rounded = (speed * 100f).roundToInt() / 100f
    return when {
        rounded % 1f == 0f -> "${rounded.toInt()}.0x"
        (rounded * 10f) % 1f == 0f -> String.format(Locale.US, "%.1fx", rounded)
        else -> String.format(Locale.US, "%.2fx", rounded)
    }
}

private const val ACTION_PIP_CONTROL = "com.example.faza.PIP_CONTROL"
private const val EXTRA_PIP_CMD = "pip_cmd"
private const val CMD_TOGGLE_PLAY = 1
private const val CMD_SEEK_FORWARD = 2

@Composable
fun PlayerScreen(
    videoId: Long,
    isInPipMode: Boolean,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val video by viewModel.currentVideo.collectAsStateWithLifecycle()
    val nextFolderVideo by viewModel.nextFolderVideo.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val videoSize by viewModel.videoSize.collectAsStateWithLifecycle()
    val cues by viewModel.cues.collectAsStateWithLifecycle()
    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val subtitleTracks by viewModel.subtitleTracks.collectAsStateWithLifecycle()
    val activeDecoder by viewModel.activeDecoder.collectAsStateWithLifecycle()
    val aspectRatioMode by viewModel.aspectRatioMode.collectAsStateWithLifecycle()
    val isLandscapeLocked by viewModel.isLandscapeLocked.collectAsStateWithLifecycle()
    val isControlsLocked by viewModel.isControlsLocked.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val audioEffectEnabled by viewModel.audioEffectEnabled.collectAsStateWithLifecycle()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
    val eqPresetIndex by viewModel.eqPresetIndex.collectAsStateWithLifecycle()
    val eqBandLevelsMb by viewModel.eqBandLevelsMb.collectAsStateWithLifecycle()
    val abPointAMs by viewModel.abPointAMs.collectAsStateWithLifecycle()
    val abPointBMs by viewModel.abPointBMs.collectAsStateWithLifecycle()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()
    val sleepAtEndOfVideo by viewModel.sleepAtEndOfVideo.collectAsStateWithLifecycle()
    val midBreakRemainingSec by viewModel.midBreakRemainingSec.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val hasSeenGestureHelp by viewModel.hasSeenGestureHelp.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionCounter by remember { mutableIntStateOf(0) }
    var gestureHud by remember { mutableStateOf<GestureHudState?>(null) }
    var gestureTick by remember { mutableIntStateOf(0) }
    var doubleTapSkipFeedback by remember { mutableStateOf<DoubleTapSkipFeedback?>(null) }
    var isHoldSpeedActive by remember { mutableStateOf(false) }
    var holdSpeedValue by remember { mutableFloatStateOf(2.0f) }
    var hasAdjustedHoldSpeed by remember { mutableStateOf(false) }
    var preHoldSpeed by remember { mutableFloatStateOf(1.0f) }
    var holdOriginX by remember { mutableFloatStateOf(0f) }
    var transientBanner by remember { mutableStateOf<String?>(null) }
    var nightModeEnabled by remember { mutableStateOf(false) }

    var showVideoSettingsSheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showSubtitleStyleSheet by remember { mutableStateOf(false) }
    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var showRunningTimeSheet by remember { mutableStateOf(false) }
    var showAppIconOverlaySheet by remember { mutableStateOf(false) }
    var showPlayerUiSheet by remember { mutableStateOf(false) }
    var showManualGestureHelp by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showFzEqualizerSheet by remember { mutableStateOf(false) }
    var showFzSleepTimerSheet by remember { mutableStateOf(false) }
    var showFzSpeedPickerSheet by remember { mutableStateOf(false) }
    var showFzCustomiseSheet by remember { mutableStateOf(false) }

    // Pinch-to-zoom & 2-finger pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Seek scrubbing state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachExternalSubtitle(uri)
            transientBanner = "External subtitle attached"
        }
    }

    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }

    LaunchedEffect(isInPipMode) {
        viewModel.setPipActive(isInPipMode)
    }

    // Automatically force landscape mode on compact single-window phones; preserve adaptive windowing on tablets/foldables/multi-window (Android 7–17)
    LaunchedEffect(preferences.forceLandscapeOnOpen, isInPipMode) {
        if (preferences.forceLandscapeOnOpen && !isInPipMode && activity != null) {
            val isAdaptiveWindow = context.resources.configuration.smallestScreenWidthDp >= 600 ||
                activity.isInMultiWindowMode
            if (!isAdaptiveWindow) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                viewModel.setLandscapeLocked(true)
            }
        }
    }

    // Auto-hide controls after configured inactivity delay (0 = keep visible until tapped)
    val autoHideSec = preferences.playerUiConfig.autoHideTimeoutSec
    LaunchedEffect(controlsVisible, interactionCounter, isPlaying, isControlsLocked, autoHideSec) {
        if (controlsVisible && isPlaying && !isControlsLocked && autoHideSec > 0) {
            delay(autoHideSec * 1000L)
            controlsVisible = false
        }
    }

    // Auto-fade gesture HUD
    LaunchedEffect(gestureTick) {
        if (gestureHud != null) {
            delay(950L)
            gestureHud = null
        }
    }

    // Auto-fade double-tap skip ripple HUD
    LaunchedEffect(doubleTapSkipFeedback?.tick) {
        if (doubleTapSkipFeedback != null) {
            delay(950L)
            doubleTapSkipFeedback = null
        }
    }

    LaunchedEffect(transientBanner) {
        if (transientBanner != null) {
            delay(1800L)
            transientBanner = null
        }
    }

    val currentBackgroundPlayback by rememberUpdatedState(preferences.backgroundPlaybackEnabled)

    // Keep device display awake whenever video playback or buffering is active
    val shouldKeepScreenOn = isPlaying || isBuffering
    DisposableEffect(view, activity, shouldKeepScreenOn) {
        view.keepScreenOn = shouldKeepScreenOn
        val window = activity?.window
        if (shouldKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            view.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Immersive full-screen system bars & restore on exit
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            viewModel.saveCurrentProgress()
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = -1f
                window.attributes = lp
            }
            val isAdaptiveWindow = context.resources.configuration.smallestScreenWidthDp >= 600 ||
                activity?.isInMultiWindowMode == true
            if (!isAdaptiveWindow || viewModel.isLandscapeLocked.value) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            viewModel.setLandscapeLocked(false)
            if (!currentBackgroundPlayback) {
                viewModel.playerManager.pause()
            }
        }
    }

    // Always persist playback position on pause/stop; pause player if background playback is disabled
    DisposableEffect(lifecycleOwner, preferences.backgroundPlaybackEnabled, preferences.continuePlayingDuringCall, isInPipMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.saveCurrentProgress()
            }
            if (event == Lifecycle.Event.ON_STOP &&
                !isInPipMode &&
                !preferences.backgroundPlaybackEnabled &&
                !(preferences.continuePlayingDuringCall && PlayerManager.isCallAudioModeActive(context))
            ) {
                viewModel.playerManager.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // PiP RemoteAction receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.getIntExtra(EXTRA_PIP_CMD, -1)) {
                    CMD_TOGGLE_PLAY -> viewModel.playerManager.togglePlayPause()
                    CMD_SEEK_FORWARD -> viewModel.playerManager.seekBy(10_000L)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    BackHandler {
        if (showManualGestureHelp) {
            showManualGestureHelp = false
        } else if (midBreakRemainingSec != null) {
            viewModel.dismissMidVideoBreak(resumePlayback = true)
        } else {
            viewModel.saveCurrentProgress()
            onBack()
        }
    }

    var lastPipSignature by remember { mutableStateOf<String?>(null) }
    val pipModifier = Modifier.onGloballyPositioned { coords ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            val w = videoSize.width.takeIf { it > 0 } ?: 16
            val h = videoSize.height.takeIf { it > 0 } ?: 9
            val sourceRect = coords.boundsInWindow().toAndroidRectF().toRect()
            val signature = "$w:$h:$isPlaying:${preferences.autoPipEnabled}:${sourceRect.flattenToString()}"
            if (signature == lastPipSignature) return@onGloballyPositioned
            lastPipSignature = signature

            val builder = PictureInPictureParams.Builder()
            val ratio = (w.toFloat() / h.toFloat()).coerceIn(1f / 2.35f, 2.35f)
            builder.setAspectRatio(Rational((ratio * 100).roundToInt(), 100))
            if (!sourceRect.isEmpty) {
                builder.setSourceRectHint(sourceRect)
            }
            builder.setActions(buildPipActions(context, isPlaying))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(preferences.autoPipEnabled && isPlaying)
            }
            try {
                activity.setPictureInPictureParams(builder.build())
            } catch (_: Exception) {
            }
        }
    }

    val viewConfig = LocalViewConfiguration.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(pipModifier)
    ) {
        val screenWidthDp = maxWidth
        val screenHeightDp = maxHeight

        // Clamp/reset pan offset smoothly across screen rotation or multi-window resize
        LaunchedEffect(screenWidthDp, screenHeightDp) {
            if (zoomScale <= 1.02f) {
                panOffset = Offset.Zero
            } else {
                val maxPanX = with(density) { (screenWidthDp * 0.6f).toPx() }
                val maxPanY = with(density) { (screenHeightDp * 0.6f).toPx() }
                panOffset = Offset(
                    x = panOffset.x.coerceIn(-maxPanX, maxPanX),
                    y = panOffset.y.coerceIn(-maxPanY, maxPanY)
                )
            }
        }

        val pui = preferences.playerUiConfig
        val isFzUiActive = pui.themeStyle == PlayerUiThemeStyle.FZ_UI
        val currentPui by rememberUpdatedState(pui)
        val playerAccent = resolvePlayerAccent(pui.accentPreset)
        val nowPlayingConfig = preferences.nowPlayingConfig
        val runningTimeConfig = preferences.runningTimeConfig
        val appIconOverlayConfig = preferences.appIconOverlayConfig
        val effectivePositionMs = if (isScrubbing) scrubPositionMs else positionMs
        val effectiveDurationMs = durationMs.takeIf { it > 0L } ?: (video?.durationMs ?: 0L)
        val isShowingUpNext = shouldShowUpNextInNowPlaying(
            enabled = nowPlayingConfig.enabled && nowPlayingConfig.showUpNext,
            hasNextVideoInFolder = nextFolderVideo != null,
            positionMs = effectivePositionMs,
            durationMs = effectiveDurationMs,
            leadMinutes = nowPlayingConfig.upNextLeadMinutes
        )
        val upNextTitle = if (isShowingUpNext) nextFolderVideo?.title else null
        val upNextRemainingMs = if (isShowingUpNext) {
            (effectiveDurationMs - effectivePositionMs.coerceIn(0L, effectiveDurationMs)).coerceAtLeast(0L)
        } else null
        val playNextFromBanner: (() -> Unit)? = if (isShowingUpNext && nextFolderVideo != null) {
            {
                val upcomingTitle = nextFolderVideo?.title.orEmpty()
                if (viewModel.playNext()) {
                    transientBanner = "Playing next: $upcomingTitle"
                }
            }
        } else null

        val topBannerActive = nowPlayingConfig.enabled && nowPlayingConfig.position != NowPlayingPosition.BOTTOM_CENTER
        val bottomBannerEnabled = nowPlayingConfig.enabled && nowPlayingConfig.position == NowPlayingPosition.BOTTOM_CENTER
        val runningTimeAtTop = runningTimeConfig.enabled && runningTimeConfig.position.isTopRow
        val runningTimeAtBottom = runningTimeConfig.enabled && runningTimeConfig.position.isBottomRow
        val runningTimeCustom = runningTimeConfig.enabled && runningTimeConfig.position == RunningTimePosition.CUSTOM
        val isTwoLineTopBanner = topBannerActive && (nowPlayingConfig.maxLines >= 2 || isShowingUpNext)
        val topOverlayActive = topBannerActive || runningTimeAtTop
        val hasAnyBottomButton = pui.showPrevNextButtons || pui.showDecoderButton ||
            pui.showAspectButton || pui.showTracksButton || pui.showNowPlayingButton

        val layoutMetrics = remember(
            screenWidthDp,
            screenHeightDp,
            controlsVisible,
            isControlsLocked,
            topBannerActive,
            nowPlayingConfig.enabled,
            nowPlayingConfig.maxLines,
            isShowingUpNext,
            runningTimeAtTop,
            bottomBannerEnabled,
            hasAnyBottomButton
        ) {
            calculatePlayerAdaptiveMetrics(
                screenWidthDp = screenWidthDp.value,
                screenHeightDp = screenHeightDp.value,
                isControlsVisible = controlsVisible,
                isControlsLocked = isControlsLocked,
                hasTopBanner = topBannerActive,
                isTwoLineBanner = nowPlayingConfig.enabled && (nowPlayingConfig.maxLines >= 2 || isShowingUpNext),
                hasRunningTime = runningTimeAtTop,
                isBottomCenterBanner = bottomBannerEnabled,
                hasAnyBottomButton = hasAnyBottomButton
            )
        }
        val isCompactWidth = layoutMetrics.isCompactWidth
        val isShortHeight = layoutMetrics.isShortHeight
        val isTabletOrExpanded = layoutMetrics.isTabletOrExpanded

        // Exact Screen-Aware Video Surface Dimensions (prevents ZOOM stretching & adapts to any aspect ratio/screen size)
        val surfaceDims = remember(screenWidthDp, screenHeightDp, videoSize, aspectRatioMode) {
            calculateVideoSurfaceDimensions(
                containerWidthDp = screenWidthDp.value,
                containerHeightDp = screenHeightDp.value,
                naturalWidth = videoSize.width,
                naturalHeight = videoSize.height,
                aspectRatioMode = aspectRatioMode
            )
        }

        // Video TextureView
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).also { tv ->
                        viewModel.playerManager.attachTextureView(tv)
                    }
                },
                update = { tv ->
                    viewModel.playerManager.attachTextureView(tv)
                },
                onRelease = { tv ->
                    viewModel.playerManager.detachTextureView(tv)
                },
                modifier = Modifier
                    .requiredSize(
                        width = surfaceDims.widthDp.dp,
                        height = surfaceDims.heightDp.dp
                    )
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            )
            if (nightModeEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x45000000))
                        .background(Color(0x16FF9800))
                        .testTag("fz_night_mode_filter")
                )
            }
        }

        // Gesture Detection Layer (disabled in PiP)
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isControlsLocked) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interactionCounter++
                            },
                            onDoubleTap = { offset ->
                                if (!isControlsLocked) {
                                    val xRatio = offset.x / size.width.coerceAtLeast(1)
                                    val skipSec = currentPui.seekStepSeconds
                                    val skipMs = skipSec * 1000L
                                    when {
                                        currentPui.doubleTapSkipEnabled && xRatio < 0.36f -> {
                                            viewModel.playerManager.seekBy(-skipMs)
                                            val prevSecs = doubleTapSkipFeedback?.takeIf { !it.isForward }?.totalSeconds ?: 0
                                            doubleTapSkipFeedback = DoubleTapSkipFeedback(
                                                isForward = false,
                                                totalSeconds = prevSecs + skipSec,
                                                tick = (doubleTapSkipFeedback?.tick ?: 0) + 1
                                            )
                                        }
                                        currentPui.doubleTapSkipEnabled && xRatio > 0.64f -> {
                                            viewModel.playerManager.seekBy(skipMs)
                                            val prevSecs = doubleTapSkipFeedback?.takeIf { it.isForward }?.totalSeconds ?: 0
                                            doubleTapSkipFeedback = DoubleTapSkipFeedback(
                                                isForward = true,
                                                totalSeconds = prevSecs + skipSec,
                                                tick = (doubleTapSkipFeedback?.tick ?: 0) + 1
                                            )
                                        }
                                        else -> {
                                            val willPlay = !isPlaying
                                            viewModel.playerManager.togglePlayPause()
                                            gestureHud = GestureHudState.DoubleTapToggle(willPlay)
                                            gestureTick++
                                        }
                                    }
                                }
                            },
                            onLongPress = { offset ->
                                if (!isControlsLocked && currentPui.holdSpeedEnabled) {
                                    val initialHoldSpeed = currentPui.holdDefaultSpeed
                                    preHoldSpeed = viewModel.playbackSpeed.value
                                    holdOriginX = offset.x
                                    holdSpeedValue = initialHoldSpeed
                                    hasAdjustedHoldSpeed = false
                                    isHoldSpeedActive = true
                                    viewModel.playerManager.setPlaybackSpeed(initialHoldSpeed)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
                    .pointerInput(isControlsLocked, videoId) {
                        if (isControlsLocked) return@pointerInput
                        val touchSlop = viewConfig.touchSlop
                        val holdStepPx = with(density) { 42.dp.toPx() }.coerceAtLeast(24f)
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val startPos = firstDown.position
                            var dragAxis = ActiveDragAxis.NONE
                            var isMultiTouch = false

                            // Initial values for continuous gestures
                            val initBrightness = activity?.window?.attributes?.screenBrightness
                                ?.takeIf { it in 0.01f..1f }
                                ?: run {
                                    try {
                                        Settings.System.getInt(
                                            context.contentResolver,
                                            Settings.System.SCREEN_BRIGHTNESS
                                        ) / 255f
                                    } catch (_: Exception) {
                                        0.5f
                                    }
                                }.coerceIn(0.05f, 1f)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val initVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
                            val initSeekPos = viewModel.playerManager.currentPositionMs()
                            val totalDuration = viewModel.playerManager.durationMs()
                            var pendingSeekTarget = initSeekPos
                            var lastAppliedVolStep = -1

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount == 0) {
                                    if (isHoldSpeedActive) {
                                        if (hasAdjustedHoldSpeed) {
                                            viewModel.playerManager.setPlaybackSpeed(holdSpeedValue)
                                            transientBanner = "Playback speed: ${formatSpeedLabel(holdSpeedValue)}"
                                        } else {
                                            viewModel.playerManager.setPlaybackSpeed(preHoldSpeed)
                                        }
                                        isHoldSpeedActive = false
                                        hasAdjustedHoldSpeed = false
                                    } else if (dragAxis == ActiveDragAxis.SEEK && totalDuration > 0L) {
                                        viewModel.playerManager.seekTo(pendingSeekTarget)
                                    }
                                    break
                                }

                                if (pressedCount >= 2) {
                                    if (isHoldSpeedActive) {
                                        viewModel.playerManager.setPlaybackSpeed(preHoldSpeed)
                                        isHoldSpeedActive = false
                                        hasAdjustedHoldSpeed = false
                                    }
                                    if (!currentPui.pinchZoomEnabled) continue
                                    isMultiTouch = true
                                    dragAxis = ActiveDragAxis.NONE
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val newZoom = (zoomScale * zoomChange).coerceIn(0.5f, 4.0f)
                                    zoomScale = newZoom
                                    panOffset = if (newZoom > 1.02f) {
                                        Offset(
                                            x = (panOffset.x + panChange.x).coerceIn(-size.width * 0.6f, size.width * 0.6f),
                                            y = (panOffset.y + panChange.y).coerceIn(-size.height * 0.6f, size.height * 0.6f)
                                        )
                                    } else {
                                        Offset.Zero
                                    }
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    gestureHud = GestureHudState.Zoom(newZoom)
                                    gestureTick++
                                } else if (!isMultiTouch) {
                                    val change = event.changes.firstOrNull() ?: continue

                                    if (isHoldSpeedActive) {
                                        change.consume()
                                        val holdDx = change.position.x - holdOriginX
                                        if (abs(holdDx) > touchSlop * 1.5f || hasAdjustedHoldSpeed) {
                                            hasAdjustedHoldSpeed = true
                                            val defaultHold = currentPui.holdDefaultSpeed
                                            val baseIndex = HOLD_SPEED_OPTIONS.indexOfFirst { abs(it - defaultHold) < 0.05f }
                                                .takeIf { it >= 0 } ?: HOLD_SPEED_OPTIONS.indexOf(2.0f)
                                            val stepDelta = (holdDx / holdStepPx).roundToInt()
                                            val targetIndex = (baseIndex + stepDelta).coerceIn(0, HOLD_SPEED_OPTIONS.lastIndex)
                                            val nextSpeed = HOLD_SPEED_OPTIONS[targetIndex]
                                            if (nextSpeed != holdSpeedValue) {
                                                holdSpeedValue = nextSpeed
                                                viewModel.playerManager.setPlaybackSpeed(nextSpeed)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                        continue
                                    }

                                    if (!currentPui.swipeGesturesEnabled) continue

                                    val dx = change.position.x - startPos.x
                                    val dy = change.position.y - startPos.y

                                    if (dragAxis == ActiveDragAxis.NONE) {
                                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                                            dragAxis = if (abs(dx) > abs(dy)) {
                                                ActiveDragAxis.SEEK
                                            } else {
                                                val xRatio = startPos.x / size.width.coerceAtLeast(1)
                                                if (xRatio < 0.5f) {
                                                    ActiveDragAxis.BRIGHTNESS
                                                } else {
                                                    ActiveDragAxis.VOLUME
                                                }
                                            }
                                        }
                                    }

                                    when (dragAxis) {
                                        ActiveDragAxis.SEEK -> {
                                            change.consume()
                                            if (totalDuration > 0L) {
                                                val deltaMs = ((dx / size.width.coerceAtLeast(1)) * 90_000f).toLong()
                                                pendingSeekTarget = (initSeekPos + deltaMs).coerceIn(0L, totalDuration)
                                                gestureHud = GestureHudState.Seek(
                                                    targetMs = pendingSeekTarget,
                                                    deltaMs = pendingSeekTarget - initSeekPos
                                                )
                                                gestureTick++
                                            }
                                        }
                                        ActiveDragAxis.BRIGHTNESS -> {
                                            change.consume()
                                            val delta = -dy / (size.height.coerceAtLeast(1) * 0.65f)
                                            val newBrightness = (initBrightness + delta).coerceIn(0.01f, 1.0f)
                                            activity?.window?.let { win ->
                                                val lp = win.attributes
                                                lp.screenBrightness = newBrightness
                                                win.attributes = lp
                                            }
                                            gestureHud = GestureHudState.Brightness(newBrightness)
                                            gestureTick++
                                        }
                                        ActiveDragAxis.VOLUME -> {
                                            change.consume()
                                            val delta = -dy / (size.height.coerceAtLeast(1) * 0.65f)
                                            val newVolFraction = (initVol + delta).coerceIn(0f, 1f)
                                            val targetStep = (newVolFraction * maxVol).roundToInt().coerceIn(0, maxVol)
                                            if (targetStep != lastAppliedVolStep) {
                                                lastAppliedVolStep = targetStep
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
                                            }
                                            gestureHud = GestureHudState.Volume(newVolFraction)
                                            gestureTick++
                                        }
                                        ActiveDragAxis.NONE -> Unit
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Crisp Styled Subtitles Layer (un-zoomed so subtitles stay readable & respect safe insets + bottom bars)
        if (preferences.subtitleStyle.enabled && cues.cues.isNotEmpty()) {
            val combinedCues = cues.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
            if (combinedCues.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(
                            if (isInPipMode) Modifier else Modifier.windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            )
                        )
                        .widthIn(max = 760.dp)
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = if (isInPipMode) 8.dp else {
                                preferences.subtitleStyle.bottomPaddingDp.dp +
                                    layoutMetrics.subtitleControlsExtraBottomDp.dp
                            }
                        )
                ) {
                    Text(
                        text = combinedCues,
                        textAlign = TextAlign.Center,
                        style = buildSubtitleTextStyle(preferences.subtitleStyle).let {
                            if (isInPipMode) it.copy(fontSize = 11.sp) else it
                        },
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = preferences.subtitleStyle.backgroundOpacity),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // BOTTOM-CENTER PERMANENT NOW PLAYING DISPLAY
        if (!isInPipMode && bottomBannerEnabled) {
            PermanentNowPlayingBanner(
                fileName = video?.title ?: "Loading Video...",
                isPlaying = isPlaying,
                config = nowPlayingConfig,
                availableWidth = screenWidthDp,
                upNextTitle = upNextTitle,
                upNextRemainingMs = upNextRemainingMs,
                onPlayNext = playNextFromBanner,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = layoutMetrics.bottomBannerExtraDp.dp
                    )
            )
        }

        val showAlwaysOnOverlay = !isInPipMode && (!isFzUiActive || !controlsVisible || isControlsLocked)
        val cycleRunningTimeMode: () -> Unit = {
            val nextMode = runningTimeConfig.displayMode.next()
            viewModel.updateRunningTimeConfig(runningTimeConfig.copy(displayMode = nextMode))
            transientBanner = "Timer: ${nextMode.label} (${nextMode.exampleHint})"
        }

        // ALWAYS-ON TOP OVERLAYS (hidden while FZ Ui top bar is open to prevent HUD collision)
        if (showAlwaysOnOverlay && (topBannerActive || runningTimeAtTop)) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(
                        top = 6.dp,
                        start = if (isCompactWidth) 8.dp else 14.dp,
                        end = if (isCompactWidth) 8.dp else 14.dp
                    )
            ) {
                val overlayInnerWidth = maxWidth
                val fileName = video?.title ?: "Loading Video..."
                val timerPosition = runningTimeConfig.position
                val timerReserved = if (runningTimeAtTop && timerPosition != RunningTimePosition.TOP_CENTER) {
                    val base = when {
                        runningTimeConfig.displayMode == RunningTimeDisplayMode.ELAPSED_ONLY -> 82f
                        durationMs >= 3_600_000L -> 148f
                        else -> 124f
                    }
                    (base * runningTimeConfig.fontScale.coerceIn(0.8f, 1.4f)).dp
                } else 0.dp
                val canTrueCenter = nowPlayingConfig.position == NowPlayingPosition.TOP_CENTER &&
                    (!runningTimeAtTop || timerPosition == RunningTimePosition.TOP_CENTER ||
                        (overlayInnerWidth / 2f - timerReserved - 12.dp) >= 75.dp)

                if (runningTimeAtTop && timerPosition == RunningTimePosition.TOP_CENTER) {
                    if (topBannerActive && nowPlayingConfig.position == NowPlayingPosition.TOP_CENTER) {
                        Column(
                            modifier = Modifier.align(Alignment.TopCenter),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PermanentNowPlayingBanner(
                                fileName = fileName,
                                isPlaying = isPlaying,
                                config = nowPlayingConfig,
                                availableWidth = screenWidthDp,
                                upNextTitle = upNextTitle,
                                upNextRemainingMs = upNextRemainingMs,
                                onPlayNext = playNextFromBanner
                            )
                            AlwaysOnRunningTimeOverlay(
                                positionMs = effectivePositionMs,
                                durationMs = durationMs,
                                config = runningTimeConfig,
                                availableWidth = screenWidthDp,
                                onClick = cycleRunningTimeMode
                            )
                        }
                    } else {
                        if (topBannerActive) {
                            PermanentNowPlayingBanner(
                                fileName = fileName,
                                isPlaying = isPlaying,
                                config = nowPlayingConfig,
                                availableWidth = screenWidthDp,
                                upNextTitle = upNextTitle,
                                upNextRemainingMs = upNextRemainingMs,
                                onPlayNext = playNextFromBanner,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .widthIn(max = (overlayInnerWidth / 2f - 64.dp).coerceAtLeast(110.dp))
                            )
                        }
                        AlwaysOnRunningTimeOverlay(
                            positionMs = effectivePositionMs,
                            durationMs = durationMs,
                            config = runningTimeConfig,
                            availableWidth = screenWidthDp,
                            onClick = cycleRunningTimeMode,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                } else if (topBannerActive && canTrueCenter) {
                    val centerMax = if (runningTimeAtTop) {
                        ((overlayInnerWidth / 2f - timerReserved - 12.dp) * 2f).coerceAtLeast(140.dp)
                    } else {
                        overlayInnerWidth
                    }
                    PermanentNowPlayingBanner(
                        fileName = fileName,
                        isPlaying = isPlaying,
                        config = nowPlayingConfig,
                        availableWidth = screenWidthDp,
                        upNextTitle = upNextTitle,
                        upNextRemainingMs = upNextRemainingMs,
                        onPlayNext = playNextFromBanner,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = centerMax)
                    )
                    if (runningTimeAtTop) {
                        AlwaysOnRunningTimeOverlay(
                            positionMs = effectivePositionMs,
                            durationMs = durationMs,
                            config = runningTimeConfig,
                            availableWidth = screenWidthDp,
                            onClick = cycleRunningTimeMode,
                            modifier = Modifier.align(
                                if (timerPosition == RunningTimePosition.TOP_START) {
                                    Alignment.TopStart
                                } else {
                                    Alignment.TopEnd
                                }
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (runningTimeAtTop && timerPosition == RunningTimePosition.TOP_START) {
                            AlwaysOnRunningTimeOverlay(
                                positionMs = effectivePositionMs,
                                durationMs = durationMs,
                                config = runningTimeConfig,
                                availableWidth = screenWidthDp,
                                onClick = cycleRunningTimeMode
                            )
                        }
                        if (topBannerActive) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = if (nowPlayingConfig.position == NowPlayingPosition.TOP_CENTER) {
                                    Alignment.TopCenter
                                } else {
                                    Alignment.TopStart
                                }
                            ) {
                                PermanentNowPlayingBanner(
                                    fileName = fileName,
                                    isPlaying = isPlaying,
                                    config = nowPlayingConfig,
                                    availableWidth = screenWidthDp,
                                    upNextTitle = upNextTitle,
                                    upNextRemainingMs = upNextRemainingMs,
                                    onPlayNext = playNextFromBanner
                                )
                            }
                        } else if (runningTimeAtTop && timerPosition == RunningTimePosition.TOP_END) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        if (runningTimeAtTop && timerPosition == RunningTimePosition.TOP_END) {
                            AlwaysOnRunningTimeOverlay(
                                positionMs = effectivePositionMs,
                                durationMs = durationMs,
                                config = runningTimeConfig,
                                availableWidth = screenWidthDp,
                                onClick = cycleRunningTimeMode
                            )
                        }
                    }
                }
            }
        }

        // ALWAYS-ON BOTTOM RUNNING TIME PLACEMENT (Bottom Left / Bottom Center / Bottom Right)
        if (showAlwaysOnOverlay && runningTimeAtBottom) {
            val bottomBannerCollisionOffset = if (
                bottomBannerEnabled && runningTimeConfig.position == RunningTimePosition.BOTTOM_CENTER
            ) {
                if (nowPlayingConfig.maxLines >= 2 || isShowingUpNext) 56.dp else 42.dp
            } else {
                0.dp
            }
            val bottomTimerAlignment = when (runningTimeConfig.position) {
                RunningTimePosition.BOTTOM_START -> Alignment.BottomStart
                RunningTimePosition.BOTTOM_CENTER -> Alignment.BottomCenter
                else -> Alignment.BottomEnd
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(
                        start = if (isCompactWidth) 8.dp else 14.dp,
                        end = if (isCompactWidth) 8.dp else 14.dp,
                        bottom = layoutMetrics.bottomBannerExtraDp.dp + bottomBannerCollisionOffset
                    )
            ) {
                AlwaysOnRunningTimeOverlay(
                    positionMs = effectivePositionMs,
                    durationMs = durationMs,
                    config = runningTimeConfig,
                    availableWidth = screenWidthDp,
                    onClick = cycleRunningTimeMode,
                    modifier = Modifier.align(bottomTimerAlignment)
                )
            }
        }

        // ALWAYS-ON CUSTOM FREE-PLACEMENT RUNNING TIME
        if (showAlwaysOnOverlay && runningTimeCustom) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        horizontal = if (isCompactWidth) 8.dp else 14.dp,
                        vertical = 10.dp
                    )
            ) {
                AlwaysOnRunningTimeOverlay(
                    positionMs = effectivePositionMs,
                    durationMs = durationMs,
                    config = runningTimeConfig,
                    availableWidth = screenWidthDp,
                    onClick = cycleRunningTimeMode,
                    modifier = Modifier.align(resolveRunningTimeAlignment(runningTimeConfig))
                )
            }
        }

        // OPTIONAL PERMANENT APP ICON OVERLAY (Preset corners/edges or Custom free placement)
        if (showAlwaysOnOverlay && appIconOverlayConfig.enabled) {
            val iconPos = appIconOverlayConfig.position
            when {
                iconPos.isTopRow -> {
                    val sharesTopStart = iconPos == AppIconOverlayPosition.TOP_START && (
                        (topBannerActive && nowPlayingConfig.position == NowPlayingPosition.TOP_START) ||
                            (runningTimeAtTop && runningTimeConfig.position == RunningTimePosition.TOP_START)
                        )
                    val sharesTopCenter = iconPos == AppIconOverlayPosition.TOP_CENTER && (
                        (topBannerActive && nowPlayingConfig.position == NowPlayingPosition.TOP_CENTER) ||
                            (runningTimeAtTop && runningTimeConfig.position == RunningTimePosition.TOP_CENTER)
                        )
                    val sharesTopEnd = iconPos == AppIconOverlayPosition.TOP_END &&
                        (runningTimeAtTop && runningTimeConfig.position == RunningTimePosition.TOP_END)
                    val topCollisionOffset = when {
                        sharesTopCenter && topBannerActive && runningTimeAtTop &&
                            runningTimeConfig.position == RunningTimePosition.TOP_CENTER -> 78.dp
                        sharesTopStart || sharesTopCenter || sharesTopEnd -> 42.dp
                        else -> 0.dp
                    }
                    val topIconAlignment = when (iconPos) {
                        AppIconOverlayPosition.TOP_START -> Alignment.TopStart
                        AppIconOverlayPosition.TOP_CENTER -> Alignment.TopCenter
                        else -> Alignment.TopEnd
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                            )
                            .padding(
                                top = 6.dp + topCollisionOffset,
                                start = if (isCompactWidth) 8.dp else 14.dp,
                                end = if (isCompactWidth) 8.dp else 14.dp
                            )
                    ) {
                        AlwaysOnAppIconOverlay(
                            config = appIconOverlayConfig,
                            availableWidth = screenWidthDp,
                            isPlaying = isPlaying,
                            modifier = Modifier.align(topIconAlignment)
                        )
                    }
                }

                iconPos.isBottomRow -> {
                    val sharesBottomCenter = iconPos == AppIconOverlayPosition.BOTTOM_CENTER && (
                        bottomBannerEnabled ||
                            (runningTimeAtBottom && runningTimeConfig.position == RunningTimePosition.BOTTOM_CENTER)
                        )
                    val sharesBottomEdge = (iconPos == AppIconOverlayPosition.BOTTOM_START &&
                        runningTimeAtBottom && runningTimeConfig.position == RunningTimePosition.BOTTOM_START) ||
                        (iconPos == AppIconOverlayPosition.BOTTOM_END &&
                            runningTimeAtBottom && runningTimeConfig.position == RunningTimePosition.BOTTOM_END)
                    val bottomCollisionOffset = when {
                        sharesBottomCenter && bottomBannerEnabled && runningTimeAtBottom &&
                            runningTimeConfig.position == RunningTimePosition.BOTTOM_CENTER -> 86.dp
                        sharesBottomCenter -> if (nowPlayingConfig.maxLines >= 2 || isShowingUpNext) 56.dp else 44.dp
                        sharesBottomEdge -> 40.dp
                        else -> 0.dp
                    }
                    val bottomIconAlignment = when (iconPos) {
                        AppIconOverlayPosition.BOTTOM_START -> Alignment.BottomStart
                        AppIconOverlayPosition.BOTTOM_CENTER -> Alignment.BottomCenter
                        else -> Alignment.BottomEnd
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            )
                            .padding(
                                start = if (isCompactWidth) 8.dp else 14.dp,
                                end = if (isCompactWidth) 8.dp else 14.dp,
                                bottom = layoutMetrics.bottomBannerExtraDp.dp + bottomCollisionOffset
                            )
                    ) {
                        AlwaysOnAppIconOverlay(
                            config = appIconOverlayConfig,
                            availableWidth = screenWidthDp,
                            isPlaying = isPlaying,
                            modifier = Modifier.align(bottomIconAlignment)
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(
                                horizontal = if (isCompactWidth) 8.dp else 14.dp,
                                vertical = 10.dp
                            )
                    ) {
                        AlwaysOnAppIconOverlay(
                            config = appIconOverlayConfig,
                            availableWidth = screenWidthDp,
                            isPlaying = isPlaying,
                            modifier = Modifier.align(resolveAppIconOverlayAlignment(appIconOverlayConfig))
                        )
                    }
                }
            }
        }

        // Buffering & Error States
        if (isBuffering) {
            CircularProgressIndicator(
                color = playerAccent,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (playbackError != null) {
            @OptIn(ExperimentalLayoutApi::class)
            Surface(
                color = Color(0xE6141822),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Playback Issue",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = playbackError ?: "Unable to decode video stream.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { viewModel.retryPlayback() }) {
                            Text("Retry")
                        }
                        Button(onClick = {
                            viewModel.setDecoderMode(com.example.data.model.DecoderMode.SOFTWARE)
                        }) {
                            Text("Try SW")
                        }
                        Button(onClick = onBack) {
                            Text("Back")
                        }
                    }
                }
            }
        }

        // Double-Tap Left/Right 10s Skip Ripple Overlay
        AnimatedVisibility(
            visible = doubleTapSkipFeedback != null && !isInPipMode,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(
                    if (doubleTapSkipFeedback?.isForward == true) Alignment.CenterEnd else Alignment.CenterStart
                )
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = if (isCompactWidth) 18.dp else 36.dp)
        ) {
            doubleTapSkipFeedback?.let { feedback ->
                Surface(
                    color = Color(0xC40A0B0E),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                        .testTag(if (feedback.isForward) "double_tap_forward_hud" else "double_tap_rewind_hud")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (feedback.isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                            contentDescription = if (feedback.isForward) "Forward ${feedback.totalSeconds} seconds" else "Rewind ${feedback.totalSeconds} seconds",
                            tint = playerAccent,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = if (feedback.isForward) "+${feedback.totalSeconds}s" else "-${feedback.totalSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Hold-to-Set Playback Speed Live HUD Pill
        AnimatedVisibility(
            visible = isHoldSpeedActive && !isInPipMode,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(
                    top = when {
                        isTwoLineTopBanner -> if (isShortHeight) 52.dp else 64.dp
                        topOverlayActive -> if (isShortHeight) 40.dp else 52.dp
                        else -> 16.dp
                    }
                )
        ) {
            Surface(
                color = Color(0xD90A0B0E),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.dp, playerAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .testTag("hold_speed_hud")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = playerAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = formatSpeedLabel(holdSpeedValue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (hasAdjustedHoldSpeed) {
                            "Speed set · Release to keep"
                        } else {
                            "${formatSpeedLabel(pui.holdDefaultSpeed)} Hold · Slide ← → to set speed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Animated Gesture HUD Indicator
        AnimatedVisibility(
            visible = gestureHud != null && !isInPipMode,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            gestureHud?.let { GestureHudOverlay(hud = it, accentColor = playerAccent) }
        }

        // Transient Toast Banner (Aspect Ratio / Subtitle attached)
        AnimatedVisibility(
            visible = transientBanner != null && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(
                    top = when {
                        isTwoLineTopBanner -> if (isShortHeight) 48.dp else 60.dp
                        topOverlayActive -> if (isShortHeight) 38.dp else 48.dp
                        else -> 12.dp
                    }
                )
        ) {
            transientBanner?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Locked Controls Unlock Pill
        if (isControlsLocked && controlsVisible && !isInPipMode) {
            Surface(
                color = Color(0xCC090C12),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(bottom = 28.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setControlsLocked(false) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("unlock_controls_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = "Unlock screen controls",
                        tint = playerAccent
                    )
                }
            }
        }

        // Main Player Controls Overlay (fully customizable via PlayerUiConfig)
        AnimatedVisibility(
            visible = controlsVisible && !isControlsLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val hudAlpha = pui.hudOpacity.coerceIn(0.4f, 1.0f)
                val topBarBg = when (pui.barTheme) {
                    PlayerBarTheme.CINEMA_DARK -> Color(0xFF0A0A14).copy(alpha = 0.86f * hudAlpha)
                    PlayerBarTheme.FROSTED_GLASS -> Color(0xFF141328).copy(alpha = 0.74f * hudAlpha)
                    PlayerBarTheme.AMOLED_BLACK -> Color.Black.copy(alpha = 0.94f * hudAlpha)
                    PlayerBarTheme.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f * hudAlpha)
                    PlayerBarTheme.MINIMAL_CLEAR -> Color.Transparent
                }
                val bottomBarBg = when (pui.barTheme) {
                    PlayerBarTheme.CINEMA_DARK -> Color(0xFF0A0A14).copy(alpha = 0.91f * hudAlpha)
                    PlayerBarTheme.FROSTED_GLASS -> Color(0xFF141328).copy(alpha = 0.80f * hudAlpha)
                    PlayerBarTheme.AMOLED_BLACK -> Color.Black.copy(alpha = 0.96f * hudAlpha)
                    PlayerBarTheme.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f * hudAlpha)
                    PlayerBarTheme.MINIMAL_CLEAR -> Color.Transparent
                }
                val isMaterialYouBar = pui.barTheme == PlayerBarTheme.MATERIAL_YOU
                val barContentTint = if (isMaterialYouBar) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                }
                val chipBgColor = if (isMaterialYouBar) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f * hudAlpha)
                } else {
                    Color.Black.copy(alpha = 0.42f * hudAlpha)
                }
                val chipContentTint = if (isMaterialYouBar) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    Color.White
                }
                val roundedBtnBg = if (isMaterialYouBar) {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f * hudAlpha)
                } else {
                    Color.Black.copy(alpha = 0.40f * hudAlpha)
                }
                val roundedBtnBorder = if (isMaterialYouBar) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                } else {
                    Color.White.copy(alpha = 0.16f)
                }

                val enabledTopActions = remember(pui, activity) {
                    buildList {
                        if (pui.showFavouriteButton) add(PlayerTopAction.FAVOURITE)
                        if (pui.showPipButton && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                            add(PlayerTopAction.PIP)
                        }
                        if (pui.showRotateButton) add(PlayerTopAction.ROTATE)
                        if (pui.showLockButton) add(PlayerTopAction.LOCK)
                        add(PlayerTopAction.CUSTOMIZE_UI)
                        add(PlayerTopAction.SETTINGS)
                    }
                }
                val actionPartition = remember(enabledTopActions, layoutMetrics.maxTopRightSlots) {
                    partitionTopBarActions(enabledTopActions, layoutMetrics.maxTopRightSlots)
                }

                val triggerPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                        val w = videoSize.width.takeIf { it > 0 } ?: 16
                        val h = videoSize.height.takeIf { it > 0 } ?: 9
                        val ratio = (w.toFloat() / h.toFloat()).coerceIn(1f / 2.35f, 2.35f)
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational((ratio * 100).roundToInt(), 100))
                            .setActions(buildPipActions(context, isPlaying))
                            .build()
                        activity.enterPictureInPictureMode(params)
                    }
                }
                val triggerRotate = {
                    val isAdaptiveWindow = context.resources.configuration.smallestScreenWidthDp >= 600 ||
                        activity?.isInMultiWindowMode == true
                    if (isAdaptiveWindow) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        transientBanner = "Adaptive layout active"
                    } else {
                        val landscape = viewModel.toggleOrientationLock()
                        activity?.requestedOrientation = if (landscape) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                        transientBanner = if (landscape) "Landscape" else "Portrait"
                    }
                }

                val triggerExpandOrPopup = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                        triggerPip()
                    } else {
                        val nextAspect = if (aspectRatioMode == AspectRatioMode.ZOOM) {
                            AspectRatioMode.FIT
                        } else {
                            AspectRatioMode.ZOOM
                        }
                        viewModel.setAspectRatio(nextAspect)
                        transientBanner = "Screen: ${nextAspect.label}"
                    }
                }

                if (isFzUiActive) {
                    FzPlayerUiOverlay(
                        videoTitle = video?.title ?: "Video",
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        isScrubbing = isScrubbing,
                        scrubPositionMs = scrubPositionMs,
                        activeDecoder = activeDecoder,
                        aspectRatioMode = aspectRatioMode,
                        subtitlesEnabled = preferences.subtitleStyle.enabled,
                        nightModeEnabled = nightModeEnabled,
                        shuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        isMuted = isMuted,
                        sleepTimerRemainingSec = sleepTimerRemainingSec,
                        sleepAtEndOfVideo = sleepAtEndOfVideo,
                        abPointAMs = abPointAMs,
                        abPointBMs = abPointBMs,
                        audioEffectEnabled = audioEffectEnabled,
                        equalizerEnabled = equalizerEnabled,
                        playbackSpeed = playbackSpeed,
                        backgroundPlayEnabled = preferences.backgroundPlaybackEnabled,
                        isLandscapeLocked = isLandscapeLocked,
                        hiddenToolbarItems = pui.fzHiddenToolbarItems,
                        onBack = {
                            viewModel.saveCurrentProgress()
                            onBack()
                        },
                        onOpenAudioTracks = { showTracksSheet = true },
                        onToggleSubtitles = {
                            val nextSub = !preferences.subtitleStyle.enabled
                            viewModel.updateSubtitleStyle(preferences.subtitleStyle.copy(enabled = nextSub))
                            if (!nextSub) {
                                viewModel.playerManager.selectSubtitleTrack(null)
                            }
                            transientBanner = if (nextSub) "Subtitles enabled" else "Subtitles hidden"
                        },
                        onCycleDecoder = {
                            val entries = DecoderMode.entries
                            val next = entries[(entries.indexOf(activeDecoder) + 1) % entries.size]
                            viewModel.setDecoderMode(next)
                            transientBanner = "Decoder: ${next.label}"
                        },
                        onOpenMoreSettings = { showVideoSettingsSheet = true },
                        onOpenCustomizeUi = { showPlayerUiSheet = true },
                        onSwitchToClassicUi = {
                            viewModel.setPlayerThemeStyle(PlayerUiThemeStyle.FAZA_CLASSIC)
                            transientBanner = "Switched to FAZA Classic UI"
                        },
                        onOpenGestureHelp = { showManualGestureHelp = true },
                        onToggleNightMode = {
                            nightModeEnabled = !nightModeEnabled
                            transientBanner = if (nightModeEnabled) "Night Mode on" else "Night Mode off"
                        },
                        onOpenCustomiseItems = { showFzCustomiseSheet = true },
                        onToggleShuffle = {
                            val on = viewModel.toggleShuffle()
                            transientBanner = if (on) "Shuffle on" else "Shuffle off"
                        },
                        onCycleLoop = {
                            val mode = viewModel.cycleRepeatMode()
                            transientBanner = when (mode) {
                                Player.REPEAT_MODE_ONE -> "Loop: Current video"
                                Player.REPEAT_MODE_ALL -> "Loop: All videos"
                                else -> "Loop: Off"
                            }
                        },
                        onToggleMute = {
                            val muted = viewModel.toggleMute()
                            transientBanner = if (muted) "Muted" else "Unmuted"
                        },
                        onOpenSleepTimer = { showFzSleepTimerSheet = true },
                        onCycleAbRepeat = {
                            transientBanner = viewModel.cycleAbRepeat()
                        },
                        onToggleAudioEffect = {
                            val enabled = viewModel.toggleAudioEffect()
                            transientBanner = if (enabled) "Audio Effect on" else "Audio Effect off"
                        },
                        onOpenEqualizer = { showFzEqualizerSheet = true },
                        onOpenSpeedPicker = { showFzSpeedPickerSheet = true },
                        onTakeScreenshot = {
                            val bmp = viewModel.playerManager.captureVideoScreenshotBitmap()
                            if (bmp == null) {
                                transientBanner = "Play video frame before capturing screenshot"
                            } else {
                                val title = video?.title ?: "FAZA_Frame"
                                coroutineScope.launch {
                                    val saved = saveBitmapScreenshotToGallery(context, bmp, title)
                                    transientBanner = if (saved) {
                                        "Screenshot saved to Pictures/FAZA Player"
                                    } else {
                                        "Unable to save screenshot"
                                    }
                                }
                            }
                        },
                        onToggleBackgroundPlay = {
                            val next = !preferences.backgroundPlaybackEnabled
                            viewModel.setBackgroundPlaybackEnabled(next)
                            transientBanner = if (next) "Background Play enabled" else "Background Play disabled"
                        },
                        onRotateScreen = triggerRotate,
                        onScrubChange = { targetMs ->
                            isScrubbing = true
                            scrubPositionMs = targetMs
                        },
                        onScrubFinish = { targetMs ->
                            viewModel.playerManager.seekTo(targetMs)
                            isScrubbing = false
                        },
                        onLockScreen = { viewModel.setControlsLocked(true) },
                        onJumpBackward10s = { viewModel.playerManager.seekBy(-10_000L) },
                        onPreviousTrack = { viewModel.playPrevious() },
                        onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                        onNextTrack = { viewModel.playNext() },
                        onJumpForward10s = { viewModel.playerManager.seekBy(10_000L) },
                        onCycleAspectRatio = {
                            val next = viewModel.cycleAspectRatio()
                            zoomScale = 1f
                            panOffset = Offset.Zero
                            transientBanner = "Aspect: ${next.label}"
                        },
                        onExpandOrPopup = triggerExpandOrPopup,
                        onUserInteraction = { interactionCounter++ }
                    )
                } else {
                // Top Action Bar (rounded Material You circular controls & pills with bounce)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(topBarBg, Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        )
                        .padding(
                            top = layoutMetrics.topBarTopOffsetDp.dp,
                            start = layoutMetrics.horizontalEdgePaddingDp.dp,
                            end = layoutMetrics.horizontalEdgePaddingDp.dp,
                            bottom = layoutMetrics.topBarBottomPaddingDp.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FazaRoundedPlayerIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = barContentTint,
                        containerColor = roundedBtnBg,
                        borderColor = roundedBtnBorder,
                        onClick = {
                            viewModel.saveCurrentProgress()
                            onBack()
                        },
                        modifier = Modifier.testTag("player_back_button")
                    )

                    // Left Technical Badges (rounded full-capsule pills)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (pui.showResolutionBadge) {
                                video?.let { v ->
                                    Surface(
                                        color = chipBgColor,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, roundedBtnBorder)
                                    ) {
                                        Text(
                                            text = "${v.resolutionTier.badge} • ${activeDecoder.badge}",
                                            color = if (isMaterialYouBar) chipContentTint else playerAccent,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(
                                                horizontal = if (isCompactWidth) 9.dp else 11.dp,
                                                vertical = 5.dp
                                            )
                                        )
                                    }
                                }
                            }

                            if (abs(playbackSpeed - 1.0f) > 0.01f) {
                                FazaRoundedPlayerPillButton(
                                    containerColor = if (isMaterialYouBar) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                                    } else {
                                        playerAccent.copy(alpha = 0.28f)
                                    },
                                    borderColor = playerAccent.copy(alpha = 0.65f),
                                    onClick = {
                                        viewModel.playerManager.setPlaybackSpeed(1.0f)
                                        transientBanner = "Playback speed: 1.0x"
                                        interactionCounter++
                                    },
                                    modifier = Modifier.testTag("active_speed_reset_badge")
                                ) {
                                    Text(
                                        text = "${formatSpeedLabel(playbackSpeed)} ✕",
                                        color = if (isMaterialYouBar) MaterialTheme.colorScheme.onPrimaryContainer else barContentTint,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    actionPartition.inlineActions.forEach { action ->
                        when (action) {
                            PlayerTopAction.FAVOURITE -> {
                                FazaRoundedPlayerIconButton(
                                    icon = if (video?.isFavourite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Toggle favourite",
                                    tint = if (video?.isFavourite == true) Color(0xFFFF4D6D) else barContentTint,
                                    containerColor = roundedBtnBg,
                                    borderColor = roundedBtnBorder,
                                    onClick = { viewModel.toggleFavouriteCurrent() },
                                    modifier = Modifier.testTag("player_favourite_button")
                                )
                            }
                            PlayerTopAction.PIP -> {
                                FazaRoundedPlayerIconButton(
                                    icon = Icons.Filled.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = barContentTint,
                                    containerColor = roundedBtnBg,
                                    borderColor = roundedBtnBorder,
                                    onClick = triggerPip,
                                    modifier = Modifier.testTag("player_pip_button")
                                )
                            }
                            PlayerTopAction.ROTATE -> {
                                FazaRoundedPlayerIconButton(
                                    icon = Icons.Filled.ScreenRotation,
                                    contentDescription = "Rotate screen",
                                    tint = if (isLandscapeLocked) playerAccent else barContentTint,
                                    containerColor = if (isLandscapeLocked && isMaterialYouBar) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f * hudAlpha)
                                    } else {
                                        roundedBtnBg
                                    },
                                    borderColor = roundedBtnBorder,
                                    onClick = triggerRotate,
                                    modifier = Modifier.testTag("player_rotate_button")
                                )
                            }
                            PlayerTopAction.LOCK -> {
                                FazaRoundedPlayerIconButton(
                                    icon = Icons.Filled.Lock,
                                    contentDescription = "Lock controls",
                                    tint = barContentTint,
                                    containerColor = roundedBtnBg,
                                    borderColor = roundedBtnBorder,
                                    onClick = { viewModel.setControlsLocked(true) },
                                    modifier = Modifier.testTag("player_lock_button")
                                )
                            }
                            PlayerTopAction.CUSTOMIZE_UI -> {
                                FazaRoundedPlayerIconButton(
                                    icon = Icons.Filled.Tune,
                                    contentDescription = "Customize Player UI",
                                    tint = if (isMaterialYouBar) MaterialTheme.colorScheme.onPrimaryContainer else playerAccent,
                                    containerColor = if (isMaterialYouBar) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f * hudAlpha)
                                    } else {
                                        roundedBtnBg
                                    },
                                    borderColor = roundedBtnBorder,
                                    onClick = {
                                        interactionCounter++
                                        showPlayerUiSheet = true
                                    },
                                    modifier = Modifier.testTag("player_customize_ui_button")
                                )
                            }
                            PlayerTopAction.SETTINGS -> {
                                FazaRoundedPlayerIconButton(
                                    icon = Icons.Filled.Settings,
                                    contentDescription = "Video settings",
                                    tint = barContentTint,
                                    containerColor = roundedBtnBg,
                                    borderColor = roundedBtnBorder,
                                    onClick = {
                                        interactionCounter++
                                        showVideoSettingsSheet = true
                                    },
                                    modifier = Modifier.testTag("player_video_settings_button")
                                )
                            }
                        }
                    }

                    if (actionPartition.overflowActions.isNotEmpty()) {
                        Box {
                            FazaRoundedPlayerIconButton(
                                icon = Icons.Filled.MoreVert,
                                contentDescription = "More player actions",
                                tint = barContentTint,
                                containerColor = roundedBtnBg,
                                borderColor = roundedBtnBorder,
                                onClick = {
                                    interactionCounter++
                                    showTopOverflowMenu = true
                                },
                                modifier = Modifier.testTag("player_top_more_button")
                            )
                            DropdownMenu(
                                expanded = showTopOverflowMenu,
                                onDismissRequest = { showTopOverflowMenu = false }
                            ) {
                                actionPartition.overflowActions.forEach { action ->
                                    when (action) {
                                        PlayerTopAction.FAVOURITE -> {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(if (video?.isFavourite == true) "Remove Favourite" else "Add to Favourites")
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (video?.isFavourite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                        contentDescription = null,
                                                        tint = if (video?.isFavourite == true) Color(0xFFFF4D6D) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    viewModel.toggleFavouriteCurrent()
                                                }
                                            )
                                        }
                                        PlayerTopAction.PIP -> {
                                            DropdownMenuItem(
                                                text = { Text("Picture in Picture") },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.PictureInPictureAlt, contentDescription = null)
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    triggerPip()
                                                }
                                            )
                                        }
                                        PlayerTopAction.ROTATE -> {
                                            DropdownMenuItem(
                                                text = { Text(if (isLandscapeLocked) "Portrait Mode" else "Rotate Screen") },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.ScreenRotation, contentDescription = null)
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    triggerRotate()
                                                }
                                            )
                                        }
                                        PlayerTopAction.LOCK -> {
                                            DropdownMenuItem(
                                                text = { Text("Lock Controls") },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.Lock, contentDescription = null)
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    viewModel.setControlsLocked(true)
                                                }
                                            )
                                        }
                                        PlayerTopAction.CUSTOMIZE_UI -> {
                                            DropdownMenuItem(
                                                text = { Text("Customize Player UI") },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.Tune, contentDescription = null)
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    interactionCounter++
                                                    showPlayerUiSheet = true
                                                }
                                            )
                                        }
                                        PlayerTopAction.SETTINGS -> {
                                            DropdownMenuItem(
                                                text = { Text("Video Settings") },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.Settings, contentDescription = null)
                                                },
                                                onClick = {
                                                    showTopOverflowMenu = false
                                                    interactionCounter++
                                                    showVideoSettingsSheet = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Center Quick Transport Cluster (rounded Material You circles/pills with spring bounce)
                val ctrlScale = pui.controlSize.scale
                val secondaryBtnSize = (layoutMetrics.secondaryButtonBaseDp * ctrlScale).coerceAtLeast(48f).dp
                val secondaryIconSize = (layoutMetrics.secondaryIconBaseDp * ctrlScale).dp
                val primaryBtnSize = (layoutMetrics.primaryButtonBaseDp * ctrlScale).coerceAtLeast(52f).dp
                val primaryIconSize = (layoutMetrics.primaryIconBaseDp * ctrlScale).dp
                val clusterSpacing = layoutMetrics.clusterSpacingDp.dp

                val controlShape = when (pui.controlStyle) {
                    PlayerControlStyle.STANDARD,
                    PlayerControlStyle.OUTLINED,
                    PlayerControlStyle.MINIMAL -> CircleShape
                    PlayerControlStyle.PILL -> RoundedCornerShape(24.dp)
                }
                val primaryBtnBg = when (pui.controlStyle) {
                    PlayerControlStyle.STANDARD, PlayerControlStyle.PILL -> {
                        if (isMaterialYouBar) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f * hudAlpha)
                        } else {
                            Color.White.copy(alpha = 0.94f * hudAlpha)
                        }
                    }
                    PlayerControlStyle.OUTLINED -> Color.Black.copy(alpha = 0.36f * hudAlpha)
                    PlayerControlStyle.MINIMAL -> Color.Transparent
                }
                val primaryBtnIconTint = when (pui.controlStyle) {
                    PlayerControlStyle.STANDARD, PlayerControlStyle.PILL -> {
                        if (isMaterialYouBar) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            Color(0xFF0A0B0E)
                        }
                    }
                    PlayerControlStyle.OUTLINED, PlayerControlStyle.MINIMAL -> Color.White
                }
                val secondaryBtnBg = when (pui.controlStyle) {
                    PlayerControlStyle.STANDARD, PlayerControlStyle.PILL -> {
                        if (isMaterialYouBar) {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f * hudAlpha)
                        } else {
                            Color.Black.copy(alpha = 0.48f * hudAlpha)
                        }
                    }
                    PlayerControlStyle.OUTLINED -> Color.Black.copy(alpha = 0.30f * hudAlpha)
                    PlayerControlStyle.MINIMAL -> Color.Transparent
                }
                val secondaryBtnIconTint = if (isMaterialYouBar && (pui.controlStyle == PlayerControlStyle.STANDARD || pui.controlStyle == PlayerControlStyle.PILL)) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                }
                val skipStepMs = pui.seekStepSeconds * 1000L

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(clusterSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pui.showCenterSkipButtons) {
                        FazaRoundedCenterControlButton(
                            icon = Icons.Filled.Replay10,
                            contentDescription = "Rewind ${pui.seekStepSeconds} seconds",
                            buttonSize = secondaryBtnSize,
                            iconSize = secondaryIconSize,
                            shape = controlShape,
                            containerColor = secondaryBtnBg,
                            iconTint = secondaryBtnIconTint,
                            borderColor = when {
                                pui.controlStyle == PlayerControlStyle.OUTLINED -> Color.White.copy(alpha = 0.55f)
                                pui.controlStyle == PlayerControlStyle.MINIMAL -> Color.Transparent
                                else -> roundedBtnBorder
                            },
                            borderWidth = 1.dp,
                            onClick = {
                                viewModel.playerManager.seekBy(-skipStepMs)
                                interactionCounter++
                            },
                            modifier = Modifier.testTag("player_rewind_button")
                        )
                    }

                    FazaRoundedCenterControlButton(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        buttonSize = primaryBtnSize,
                        iconSize = primaryIconSize,
                        shape = controlShape,
                        containerColor = primaryBtnBg,
                        iconTint = primaryBtnIconTint,
                        borderColor = when {
                            pui.controlStyle == PlayerControlStyle.OUTLINED -> playerAccent
                            pui.controlStyle == PlayerControlStyle.MINIMAL -> Color.Transparent
                            isMaterialYouBar -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            else -> Color.White.copy(alpha = 0.25f)
                        },
                        borderWidth = if (pui.controlStyle == PlayerControlStyle.OUTLINED) 1.5.dp else 1.dp,
                        onClick = {
                            viewModel.playerManager.togglePlayPause()
                            interactionCounter++
                        },
                        modifier = Modifier.testTag("player_play_pause_button")
                    )

                    if (pui.showCenterSkipButtons) {
                        FazaRoundedCenterControlButton(
                            icon = Icons.Filled.Forward10,
                            contentDescription = "Forward ${pui.seekStepSeconds} seconds",
                            buttonSize = secondaryBtnSize,
                            iconSize = secondaryIconSize,
                            shape = controlShape,
                            containerColor = secondaryBtnBg,
                            iconTint = secondaryBtnIconTint,
                            borderColor = when {
                                pui.controlStyle == PlayerControlStyle.OUTLINED -> Color.White.copy(alpha = 0.55f)
                                pui.controlStyle == PlayerControlStyle.MINIMAL -> Color.Transparent
                                else -> roundedBtnBorder
                            },
                            borderWidth = 1.dp,
                            onClick = {
                                viewModel.playerManager.seekBy(skipStepMs)
                                interactionCounter++
                            },
                            modifier = Modifier.testTag("player_forward_button")
                        )
                    }
                }

                // Bottom Timeline & Rounded Material You Feature Toolbar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, bottomBarBg)
                            )
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                        .padding(
                            horizontal = if (isCompactWidth) 10.dp else 16.dp,
                            vertical = layoutMetrics.bottomBarVerticalPaddingDp.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val bottomContainerShape = RoundedCornerShape(28.dp)
                    Surface(
                        shape = bottomContainerShape,
                        color = if (pui.barTheme == PlayerBarTheme.MINIMAL_CLEAR) {
                            Color.Transparent
                        } else if (isMaterialYouBar) {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f * hudAlpha)
                        } else {
                            bottomBarBg.copy(alpha = 0.72f * hudAlpha)
                        },
                        border = if (pui.barTheme == PlayerBarTheme.MINIMAL_CLEAR) {
                            null
                        } else {
                            BorderStroke(1.dp, roundedBtnBorder)
                        },
                        modifier = layoutMetrics.bottomBarMaxWidthDp?.let { maxW ->
                            Modifier.widthIn(max = maxW.dp).fillMaxWidth()
                        } ?: Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompactWidth) 10.dp else 14.dp,
                                    vertical = 8.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val hasKnownDuration = durationMs > 0L
                            val activePos = if (isScrubbing) scrubPositionMs else positionMs
                            val safeDuration = durationMs.coerceAtLeast(1L)
                            val sliderFraction = if (hasKnownDuration) {
                                (activePos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                            } else 1f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.formatDuration(activePos),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = barContentTint
                                )
                                @OptIn(ExperimentalMaterial3Api::class)
                                Slider(
                                    value = sliderFraction,
                                    enabled = hasKnownDuration,
                                    onValueChange = { fraction ->
                                        if (hasKnownDuration) {
                                            isScrubbing = true
                                            scrubPositionMs = (fraction * safeDuration).toLong()
                                            interactionCounter++
                                        }
                                    },
                                    onValueChangeFinished = {
                                        if (hasKnownDuration) {
                                            viewModel.playerManager.seekTo(scrubPositionMs)
                                            isScrubbing = false
                                        }
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = playerAccent,
                                        activeTrackColor = playerAccent,
                                        inactiveTrackColor = barContentTint.copy(alpha = 0.28f)
                                    ),
                                    track = { sliderState ->
                                        when (pui.seekBarStyle) {
                                            SeekBarStyle.STANDARD -> {
                                                SliderDefaults.Track(
                                                    sliderState = sliderState,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = playerAccent,
                                                        activeTrackColor = playerAccent,
                                                        inactiveTrackColor = barContentTint.copy(alpha = 0.28f)
                                                    )
                                                )
                                            }
                                            SeekBarStyle.THICK_BAR, SeekBarStyle.MINIMAL_LINE -> {
                                                val trackH = if (pui.seekBarStyle == SeekBarStyle.THICK_BAR) 9.dp else 2.5.dp
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(trackH)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(barContentTint.copy(alpha = 0.26f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(sliderState.value.coerceIn(0f, 1f))
                                                            .fillMaxHeight()
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(playerAccent)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = if (isCompactWidth) 6.dp else 10.dp)
                                        .testTag("player_seek_slider")
                                )
                                if (hasKnownDuration) {
                                    Text(
                                        text = Formatters.formatDuration(durationMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = barContentTint.copy(alpha = 0.78f)
                                    )
                                } else {
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.90f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (hasAnyBottomButton) {
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val rowMinW = maxWidth
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .widthIn(min = rowMinW),
                                        horizontalArrangement = if (rowMinW >= 372.dp) {
                                            Arrangement.SpaceEvenly
                                        } else {
                                            Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (pui.showPrevNextButtons) {
                                            FazaRoundedPlayerIconButton(
                                                icon = Icons.Filled.SkipPrevious,
                                                contentDescription = "Previous video",
                                                tint = barContentTint,
                                                containerColor = roundedBtnBg,
                                                borderColor = roundedBtnBorder,
                                                onClick = {
                                                    viewModel.playPrevious()
                                                    interactionCounter++
                                                }
                                            )
                                        }

                                        if (pui.showDecoderButton) {
                                            FazaRoundedPlayerPillButton(
                                                containerColor = chipBgColor,
                                                borderColor = roundedBtnBorder,
                                                onClick = {
                                                    interactionCounter++
                                                    showVideoSettingsSheet = true
                                                },
                                                modifier = Modifier.testTag("player_decoder_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = if (isCompactWidth) 10.dp else 13.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Memory,
                                                        contentDescription = null,
                                                        tint = if (isMaterialYouBar) chipContentTint else playerAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = activeDecoder.badge,
                                                        color = chipContentTint,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }

                                        if (pui.showAspectButton) {
                                            FazaRoundedPlayerPillButton(
                                                containerColor = chipBgColor,
                                                borderColor = roundedBtnBorder,
                                                onClick = {
                                                    val next = viewModel.cycleAspectRatio()
                                                    zoomScale = 1f
                                                    panOffset = Offset.Zero
                                                    transientBanner = "Aspect: ${next.label}"
                                                    interactionCounter++
                                                },
                                                modifier = Modifier.testTag("player_aspect_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = if (isCompactWidth) 10.dp else 13.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.AspectRatio,
                                                        contentDescription = null,
                                                        tint = chipContentTint,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = aspectRatioMode.label,
                                                        color = chipContentTint,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }

                                        if (pui.showTracksButton) {
                                            FazaRoundedPlayerIconButton(
                                                icon = Icons.Filled.ClosedCaption,
                                                contentDescription = "Audio & Subtitles",
                                                tint = barContentTint,
                                                containerColor = roundedBtnBg,
                                                borderColor = roundedBtnBorder,
                                                onClick = { showTracksSheet = true },
                                                modifier = Modifier.testTag("player_tracks_button")
                                            )
                                        }

                                        if (pui.showNowPlayingButton) {
                                            FazaRoundedPlayerIconButton(
                                                icon = Icons.AutoMirrored.Filled.BrandingWatermark,
                                                contentDescription = "Now Playing Display Style",
                                                tint = if (nowPlayingConfig.enabled) {
                                                    if (isMaterialYouBar) MaterialTheme.colorScheme.onPrimaryContainer else playerAccent
                                                } else {
                                                    barContentTint.copy(alpha = 0.6f)
                                                },
                                                containerColor = if (nowPlayingConfig.enabled && isMaterialYouBar) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f * hudAlpha)
                                                } else {
                                                    roundedBtnBg
                                                },
                                                borderColor = roundedBtnBorder,
                                                onClick = { showNowPlayingSheet = true },
                                                modifier = Modifier.testTag("player_now_playing_style_button")
                                            )
                                        }

                                        if (pui.showPrevNextButtons) {
                                            FazaRoundedPlayerIconButton(
                                                icon = Icons.Filled.SkipNext,
                                                contentDescription = "Next video",
                                                tint = barContentTint,
                                                containerColor = roundedBtnBg,
                                                borderColor = roundedBtnBorder,
                                                onClick = {
                                                    viewModel.playNext()
                                                    interactionCounter++
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        // First-launch (or manually triggered) Interactive Gesture Controls Help Overlay
        val showGestureHelp = !isInPipMode && (hasSeenGestureHelp == false || showManualGestureHelp)
        AnimatedVisibility(
            visible = showGestureHelp,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerGestureHelpOverlay(
                onDismiss = {
                    showManualGestureHelp = false
                    if (hasSeenGestureHelp == false) {
                        viewModel.markGestureHelpSeen()
                    }
                }
            )
        }

        val activeMidBreakRemaining = midBreakRemainingSec
        AnimatedVisibility(
            visible = !isInPipMode && activeMidBreakRemaining != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (activeMidBreakRemaining != null) {
                MidVideoBreakOverlay(
                    remainingSec = activeMidBreakRemaining,
                    triggerPositionMs = (durationMs / 2L).coerceAtLeast(0L),
                    totalDurationMs = durationMs.coerceAtLeast(0L),
                    onSkipBreak = {
                        viewModel.dismissMidVideoBreak(resumePlayback = true)
                        transientBanner = "Resuming video"
                    },
                    onDisableAndSkip = {
                        viewModel.setMidVideoBreakEnabled(false)
                        transientBanner = "10s mid-video break disabled"
                    }
                )
            }
        }
    }

    if (showVideoSettingsSheet) {
        VideoSettingsSheet(
            decoderMode = activeDecoder,
            aspectRatioMode = aspectRatioMode,
            nowPlayingEnabled = preferences.nowPlayingConfig.enabled,
            runningTimeEnabled = preferences.runningTimeConfig.enabled,
            appIconOverlayEnabled = preferences.appIconOverlayConfig.enabled,
            continuePlayingDuringCall = preferences.continuePlayingDuringCall,
            midVideoBreakEnabled = preferences.midVideoBreakEnabled,
            fzUiEnabled = preferences.playerUiConfig.themeStyle == PlayerUiThemeStyle.FZ_UI,
            isFavourite = video?.isFavourite == true,
            onDecoderChange = { mode ->
                viewModel.setDecoderMode(mode)
                transientBanner = "Decoder: ${mode.label}"
            },
            onContinuePlayingDuringCallChange = { enabled ->
                viewModel.setContinuePlayingDuringCall(enabled)
                transientBanner = if (enabled) {
                    "Video & voice stay playing during calls"
                } else {
                    "Video pauses during calls"
                }
            },
            onMidVideoBreakChange = { enabled ->
                viewModel.setMidVideoBreakEnabled(enabled)
                transientBanner = if (enabled) {
                    "10s mid-video break enabled (20+ min videos)"
                } else {
                    "10s mid-video break disabled"
                }
            },
            onToggleFzUi = { enabled ->
                val target = if (enabled) PlayerUiThemeStyle.FZ_UI else PlayerUiThemeStyle.FAZA_CLASSIC
                viewModel.setPlayerThemeStyle(target)
                transientBanner = "Player UI: ${target.label}"
            },
            onAspectRatioChange = { mode ->
                viewModel.setAspectRatio(mode)
                zoomScale = 1f
                panOffset = Offset.Zero
                transientBanner = "Aspect: ${mode.label}"
            },
            onOpenTracksSheet = {
                showVideoSettingsSheet = false
                showTracksSheet = true
            },
            onOpenNowPlayingSheet = {
                showVideoSettingsSheet = false
                showNowPlayingSheet = true
            },
            onOpenRunningTimeSheet = {
                showVideoSettingsSheet = false
                showRunningTimeSheet = true
            },
            onOpenAppIconOverlaySheet = {
                showVideoSettingsSheet = false
                showAppIconOverlaySheet = true
            },
            onOpenPlayerUiCustomizer = {
                showVideoSettingsSheet = false
                showPlayerUiSheet = true
            },
            onToggleFavourite = {
                viewModel.toggleFavouriteCurrent()
                transientBanner = if (video?.isFavourite == true) {
                    "Removed from Favourites"
                } else {
                    "Added to Favourites"
                }
            },
            onOpenGestureHelp = {
                showVideoSettingsSheet = false
                showManualGestureHelp = true
            },
            onDismiss = { showVideoSettingsSheet = false }
        )
    }

    if (showTracksSheet) {
        TracksAndSubtitlesSheet(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            subtitleStyle = preferences.subtitleStyle,
            onSelectAudio = { viewModel.playerManager.selectAudioTrack(it) },
            onSelectSubtitle = { option ->
                if (option == null) {
                    viewModel.updateSubtitleStyle(preferences.subtitleStyle.copy(enabled = false))
                    viewModel.playerManager.selectSubtitleTrack(null)
                } else {
                    viewModel.updateSubtitleStyle(preferences.subtitleStyle.copy(enabled = true))
                    viewModel.playerManager.selectSubtitleTrack(option)
                }
            },
            onLoadExternalSubtitle = {
                showTracksSheet = false
                subtitlePicker.launch(arrayOf("*/*"))
            },
            onOpenSubtitleStyling = {
                showTracksSheet = false
                showSubtitleStyleSheet = true
            },
            onDismiss = { showTracksSheet = false }
        )
    }

    if (showSubtitleStyleSheet) {
        SubtitleStyleBottomSheet(
            style = preferences.subtitleStyle,
            onStyleChange = { viewModel.updateSubtitleStyle(it) },
            onDismiss = { showSubtitleStyleSheet = false }
        )
    }

    if (showNowPlayingSheet) {
        NowPlayingStyleBottomSheet(
            config = preferences.nowPlayingConfig,
            onConfigChange = { viewModel.updateNowPlayingConfig(it) },
            onDismiss = { showNowPlayingSheet = false }
        )
    }

    if (showRunningTimeSheet) {
        RunningTimeStyleBottomSheet(
            config = preferences.runningTimeConfig,
            onConfigChange = { viewModel.updateRunningTimeConfig(it) },
            onDismiss = { showRunningTimeSheet = false }
        )
    }

    if (showAppIconOverlaySheet) {
        AppIconOverlayBottomSheet(
            config = preferences.appIconOverlayConfig,
            onConfigChange = { viewModel.updateAppIconOverlayConfig(it) },
            onDismiss = { showAppIconOverlaySheet = false }
        )
    }

    if (showPlayerUiSheet) {
        PlayerUiCustomizerBottomSheet(
            config = preferences.playerUiConfig,
            onConfigChange = { viewModel.updatePlayerUiConfig(it) },
            onReset = {
                viewModel.resetPlayerUiConfig()
                transientBanner = "Player UI reset to defaults"
            },
            onOpenNowPlaying = {
                showPlayerUiSheet = false
                showNowPlayingSheet = true
            },
            onOpenRunningTime = {
                showPlayerUiSheet = false
                showRunningTimeSheet = true
            },
            onOpenAppIconOverlay = {
                showPlayerUiSheet = false
                showAppIconOverlaySheet = true
            },
            onOpenSubtitleStyle = {
                showPlayerUiSheet = false
                showSubtitleStyleSheet = true
            },
            onDismiss = { showPlayerUiSheet = false }
        )
    }

    if (showFzEqualizerSheet) {
        FzEqualizerBottomSheet(
            equalizerEnabled = equalizerEnabled,
            selectedPresetIndex = eqPresetIndex,
            bandLevelsMb = eqBandLevelsMb,
            onSelectPreset = { viewModel.setEqualizerPreset(it) },
            onBandLevelChange = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
            onDismiss = { showFzEqualizerSheet = false }
        )
    }

    if (showFzSleepTimerSheet) {
        FzSleepTimerBottomSheet(
            remainingSec = sleepTimerRemainingSec,
            sleepAtEndOfVideo = sleepAtEndOfVideo,
            onSelectMinutes = { mins ->
                viewModel.setSleepTimerMinutes(mins)
                transientBanner = if (mins == null) "Sleep Timer off" else "Sleep Timer set for $mins min"
            },
            onToggleEndOfVideo = { end ->
                viewModel.setSleepAfterCurrentVideo(end)
                transientBanner = if (end) "Sleep Timer: End of video" else "Sleep Timer off"
            },
            onDismiss = { showFzSleepTimerSheet = false }
        )
    }

    if (showFzSpeedPickerSheet) {
        FzSpeedPickerBottomSheet(
            currentSpeed = playbackSpeed,
            onSelectSpeed = { spd ->
                viewModel.playerManager.setPlaybackSpeed(spd)
                transientBanner = "Playback speed: ${formatFzSpeedLabel(spd)}"
            },
            onDismiss = { showFzSpeedPickerSheet = false }
        )
    }

    if (showFzCustomiseSheet) {
        FzCustomiseItemsBottomSheet(
            themeStyle = preferences.playerUiConfig.themeStyle,
            hiddenItems = preferences.playerUiConfig.fzHiddenToolbarItems,
            onThemeStyleChange = { style ->
                viewModel.setPlayerThemeStyle(style)
                transientBanner = "Player UI: ${style.label}"
            },
            onHiddenItemsChange = { nextHidden ->
                viewModel.updatePlayerUiConfig(
                    preferences.playerUiConfig.copy(fzHiddenToolbarItems = nextHidden)
                )
            },
            onDismiss = { showFzCustomiseSheet = false }
        )
    }
}

@Composable
private fun GestureHudOverlay(hud: GestureHudState, accentColor: Color) {
    Surface(
        color = Color(0xB80A0B0E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .testTag("gesture_hud_indicator")
    ) {
        when (hud) {
            is GestureHudState.Brightness -> {
                HudMeter(
                    icon = Icons.Filled.Brightness6,
                    label = "BRIGHTNESS",
                    valueText = "${(hud.fraction * 100).roundToInt()}%",
                    progress = hud.fraction,
                    accent = Color.White
                )
            }
            is GestureHudState.Volume -> {
                HudMeter(
                    icon = if (hud.fraction <= 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    label = "VOLUME",
                    valueText = "${(hud.fraction * 100).roundToInt()}%",
                    progress = hud.fraction,
                    accent = accentColor
                )
            }
            is GestureHudState.Zoom -> {
                HudMeter(
                    icon = Icons.Filled.ZoomIn,
                    label = "ZOOM",
                    valueText = String.format(Locale.US, "%.2fx", hud.scale),
                    progress = ((hud.scale - 0.5f) / 3.5f).coerceIn(0f, 1f),
                    accent = Color.White
                )
            }
            is GestureHudState.Seek -> {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (hud.deltaMs >= 0) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = Formatters.formatDuration(hud.targetMs),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = Formatters.formatSignedDelta(hud.deltaMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
            }
            is GestureHudState.DoubleTapToggle -> {
                Box(
                    modifier = Modifier.padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hud.isNowPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (hud.isNowPlaying) "Play" else "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HudMeter(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    valueText: String,
    progress: Float,
    accent: Color
) {
    Column(
        modifier = Modifier
            .width(176.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(28.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA7BD))
        Text(text = valueText, style = MaterialTheme.typography.titleLarge, color = Color.White)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = accent,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
        )
    }
}

private fun buildPipActions(context: Context, isPlaying: Boolean): List<RemoteAction> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
    val playPauseIntent = PendingIntent.getBroadcast(
        context,
        CMD_TOGGLE_PLAY,
        Intent(ACTION_PIP_CONTROL).setPackage(context.packageName).putExtra(EXTRA_PIP_CMD, CMD_TOGGLE_PLAY),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val seekIntent = PendingIntent.getBroadcast(
        context,
        CMD_SEEK_FORWARD,
        Intent(ACTION_PIP_CONTROL).setPackage(context.packageName).putExtra(EXTRA_PIP_CMD, CMD_SEEK_FORWARD),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val playIcon = AndroidIcon.createWithResource(
        "android",
        if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
    )
    val ffIcon = AndroidIcon.createWithResource("android", android.R.drawable.ic_media_ff)
    return listOf(
        RemoteAction(
            playIcon,
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) "Pause playback" else "Resume playback",
            playPauseIntent
        ),
        RemoteAction(ffIcon, "Forward", "Forward 10 seconds", seekIntent)
    )
}

@Composable
private fun FazaRoundedPlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        enabled = true,
        pressedScale = 0.86f
    )
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FazaRoundedPlayerPillButton(
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        enabled = true,
        pressedScale = 0.90f
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = containerColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
    ) {
        content()
    }
}

@Composable
private fun FazaRoundedCenterControlButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Dp,
    iconSize: Dp,
    shape: Shape,
    containerColor: Color,
    iconTint: Color,
    borderColor: Color,
    borderWidth: Dp = 1.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        enabled = true,
        pressedScale = 0.88f
    )
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
            .size(buttonSize)
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
