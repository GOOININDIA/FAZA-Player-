package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.FzQuickToolbarItem
import com.example.util.Formatters
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val FzPitchBlack = Color(0xFF000000)
val FzSoftBlue = Color(0xFF60A5FA)
val FzAccentBlue = Color(0xFF3B82F6)
val FzSurfaceTile = Color(0xCC0D111A)

fun formatFzTimelineRemaining(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "LIVE"
    val safePos = positionMs.coerceIn(0L, durationMs)
    val remaining = (durationMs - safePos).coerceAtLeast(0L)
    return "-${Formatters.formatDuration(remaining)}"
}

fun formatFzSpeedLabel(speed: Float): String {
    val rounded = (speed * 100f).roundToInt() / 100f
    return when {
        rounded % 1f == 0f -> "${rounded.toInt()}X"
        (rounded * 10f) % 1f == 0f -> String.format(Locale.US, "%.1fX", rounded)
        else -> String.format(Locale.US, "%.2fX", rounded)
    }
}

fun resolveVisibleFzToolbarItems(hiddenNames: Set<String>): List<FzQuickToolbarItem> {
    return FzQuickToolbarItem.entries.filter { item ->
        item == FzQuickToolbarItem.CUSTOMISE_ITEMS || item.name !in hiddenNames
    }
}

private data class DeviceBatteryStatus(
    val percentage: Int,
    val isCharging: Boolean
)

@Composable
private fun rememberLiveDeviceBatteryStatus(): DeviceBatteryStatus {
    val context = LocalContext.current
    fun readCurrentBattery(intent: Intent?): DeviceBatteryStatus {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val pctFromIntent = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else null
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = (pctFromIntent ?: bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100)
            .coerceIn(0, 100)
        return DeviceBatteryStatus(percentage = pct, isCharging = charging)
    }

    var batteryState by remember {
        mutableStateOf(
            readCurrentBattery(
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            )
        )
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                batteryState = readCurrentBattery(intent)
            }
        }
        runCatching {
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
    return batteryState
}

@Composable
private fun rememberLiveDeviceClock(): String {
    val context = LocalContext.current
    var clockText by remember {
        mutableStateOf(DateFormat.getTimeFormat(context).format(Date()))
    }
    LaunchedEffect(context) {
        while (true) {
            clockText = DateFormat.getTimeFormat(context).format(Date())
            delay(15_000L)
        }
    }
    return clockText
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FzPlayerUiOverlay(
    videoTitle: String,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    activeDecoder: DecoderMode,
    aspectRatioMode: AspectRatioMode,
    subtitlesEnabled: Boolean,
    nightModeEnabled: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isMuted: Boolean,
    sleepTimerRemainingSec: Int?,
    sleepAtEndOfVideo: Boolean,
    abPointAMs: Long?,
    abPointBMs: Long?,
    audioEffectEnabled: Boolean,
    equalizerEnabled: Boolean,
    playbackSpeed: Float,
    backgroundPlayEnabled: Boolean,
    isLandscapeLocked: Boolean,
    hiddenToolbarItems: Set<String>,
    onBack: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onCycleDecoder: () -> Unit,
    onOpenMoreSettings: () -> Unit,
    onOpenCustomizeUi: () -> Unit,
    onSwitchToClassicUi: () -> Unit,
    onOpenGestureHelp: () -> Unit,
    onToggleNightMode: () -> Unit,
    onOpenCustomiseItems: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleLoop: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onCycleAbRepeat: () -> Unit,
    onToggleAudioEffect: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSpeedPicker: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onRotateScreen: () -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubFinish: (Long) -> Unit,
    onLockScreen: () -> Unit,
    onJumpBackward10s: () -> Unit,
    onPreviousTrack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onJumpForward10s: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onExpandOrPopup: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val battery = rememberLiveDeviceBatteryStatus()
    val currentClock = rememberLiveDeviceClock()
    var showMoreMenu by remember { mutableStateOf(false) }

    val toolbarItems = remember(hiddenToolbarItems) {
        resolveVisibleFzToolbarItems(hiddenToolbarItems)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("fz_player_ui_overlay")
    ) {
        val isCompactWidth = maxWidth < 460.dp
        val isShortHeight = maxHeight < 440.dp

        // 1 + 2. Top Pitch-Black Header (Action Bar + Scrollable Quick-Access Feature Toolbar)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            FzPitchBlack.copy(alpha = 0.94f),
                            FzPitchBlack.copy(alpha = 0.80f),
                            Color.Transparent
                        )
                    )
                )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(
                    top = if (isShortHeight) 2.dp else 6.dp,
                    bottom = if (isShortHeight) 6.dp else 12.dp
                )
        ) {
            // 1. TOP ACTION BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompactWidth) 4.dp else 10.dp)
                    .testTag("fz_top_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onBack()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fz_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = videoTitle.ifBlank { "Video" },
                    color = Color.White,
                    style = if (isCompactWidth) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("fz_video_title")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 2.dp else 4.dp)
                ) {
                    // Battery percentage indicator (as per device)
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("fz_battery_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
                                contentDescription = "Battery ${battery.percentage}%",
                                tint = if (battery.percentage <= 15 && !battery.isCharging) Color(0xFFF87171) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${battery.percentage}%",
                                color = Color.White,
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // Current time (as per device)
                    Text(
                        text = currentClock,
                        color = Color.White.copy(alpha = 0.92f),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("fz_clock_text")
                    )

                    // Audio track switcher icon
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onOpenAudioTracks()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_audio_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = "Audio track switcher",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Subtitle toggle icon
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onToggleSubtitles()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_subtitle_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ClosedCaption,
                            contentDescription = "Toggle subtitles",
                            tint = if (subtitlesEnabled) FzSoftBlue else Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Hardware decoding badge (as per selected hardware)
                    val hwBadgeText = when (activeDecoder) {
                        DecoderMode.HARDWARE -> "HW+"
                        DecoderMode.AUTO -> "HW"
                        DecoderMode.SOFTWARE -> "SW"
                    }
                    Surface(
                        onClick = {
                            onUserInteraction()
                            onCycleDecoder()
                        },
                        color = if (activeDecoder != DecoderMode.SOFTWARE) {
                            FzAccentBlue.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        },
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(
                            1.dp,
                            if (activeDecoder != DecoderMode.SOFTWARE) FzSoftBlue.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("fz_decoder_badge")
                    ) {
                        Text(
                            text = hwBadgeText,
                            color = if (activeDecoder != DecoderMode.SOFTWARE) FzSoftBlue else Color.White,
                            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Three-dot more options menu
                    Box {
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                showMoreMenu = true
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fz_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Audio & Subtitles") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAudioTracks()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Video Settings") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenMoreSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Customize Player UI") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenCustomizeUi()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Switch to FAZA Classic UI") },
                                onClick = {
                                    showMoreMenu = false
                                    onSwitchToClassicUi()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gesture Controls Help") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenGestureHelp()
                                }
                            )
                        }
                    }
                }
            }

            // 2. HORIZONTAL QUICK-ACCESS FEATURE TOOLBAR
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isShortHeight) 2.dp else 4.dp)
                    .testTag("fz_quick_toolbar"),
                contentPadding = PaddingValues(horizontal = if (isCompactWidth) 8.dp else 14.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isShortHeight) 6.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(toolbarItems, key = { it.name }) { item ->
                    when (item) {
                        FzQuickToolbarItem.NIGHT_MODE -> FzToolbarActionChip(
                            icon = Icons.Filled.DarkMode,
                            label = "Night Mode",
                            isActive = nightModeEnabled,
                            tag = "fz_tool_night_mode",
                            onClick = {
                                onUserInteraction()
                                onToggleNightMode()
                            }
                        )
                        FzQuickToolbarItem.CUSTOMISE_ITEMS -> FzToolbarActionChip(
                            icon = Icons.Filled.DashboardCustomize,
                            label = "Customise Items",
                            isActive = hiddenToolbarItems.isNotEmpty(),
                            tag = "fz_tool_customise_items",
                            onClick = {
                                onUserInteraction()
                                onOpenCustomiseItems()
                            }
                        )
                        FzQuickToolbarItem.SHUFFLE -> FzToolbarActionChip(
                            icon = Icons.Filled.Shuffle,
                            label = "Shuffle",
                            isActive = shuffleEnabled,
                            tag = "fz_tool_shuffle",
                            onClick = {
                                onUserInteraction()
                                onToggleShuffle()
                            }
                        )
                        FzQuickToolbarItem.LOOP -> {
                            val loopLabel = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> "Loop One"
                                Player.REPEAT_MODE_ALL -> "Loop All"
                                else -> "Loop"
                            }
                            FzToolbarActionChip(
                                icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                label = loopLabel,
                                isActive = repeatMode != Player.REPEAT_MODE_OFF,
                                tag = "fz_tool_loop",
                                onClick = {
                                    onUserInteraction()
                                    onCycleLoop()
                                }
                            )
                        }
                        FzQuickToolbarItem.MUTE -> FzToolbarActionChip(
                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Mute",
                            isActive = isMuted,
                            tag = "fz_tool_mute",
                            onClick = {
                                onUserInteraction()
                                onToggleMute()
                            }
                        )
                        FzQuickToolbarItem.SLEEP_TIMER -> {
                            val sleepActive = sleepTimerRemainingSec != null || sleepAtEndOfVideo
                            val sleepLabel = when {
                                sleepTimerRemainingSec != null -> {
                                    val mins = (sleepTimerRemainingSec + 59) / 60
                                    "Sleep (${mins}m)"
                                }
                                sleepAtEndOfVideo -> "Sleep (End)"
                                else -> "Sleep Timer"
                            }
                            FzToolbarActionChip(
                                icon = Icons.Filled.Bedtime,
                                label = sleepLabel,
                                isActive = sleepActive,
                                tag = "fz_tool_sleep_timer",
                                onClick = {
                                    onUserInteraction()
                                    onOpenSleepTimer()
                                }
                            )
                        }
                        FzQuickToolbarItem.AB_REPEAT -> {
                            val abActive = abPointAMs != null
                            val abLabel = when {
                                abPointAMs != null && abPointBMs != null -> "A-B Active"
                                abPointAMs != null -> "Set B"
                                else -> "A-B Repeat"
                            }
                            FzToolbarActionChip(
                                icon = Icons.Filled.Loop,
                                label = abLabel,
                                isActive = abActive,
                                tag = "fz_tool_ab_repeat",
                                onClick = {
                                    onUserInteraction()
                                    onCycleAbRepeat()
                                }
                            )
                        }
                        FzQuickToolbarItem.AUDIO_EFFECT -> FzToolbarActionChip(
                            icon = Icons.Filled.GraphicEq,
                            label = "Audio Effect",
                            isActive = audioEffectEnabled,
                            showIndicatorDot = audioEffectEnabled,
                            indicatorDotTag = "fz_audio_effect_active_dot",
                            tag = "fz_tool_audio_effect",
                            onClick = {
                                onUserInteraction()
                                onToggleAudioEffect()
                            }
                        )
                        FzQuickToolbarItem.EQUALIZER -> FzToolbarActionChip(
                            icon = Icons.Filled.Equalizer,
                            label = "Equalizer",
                            isActive = equalizerEnabled,
                            tag = "fz_tool_equalizer",
                            onClick = {
                                onUserInteraction()
                                onOpenEqualizer()
                            }
                        )
                        FzQuickToolbarItem.SPEED -> FzToolbarActionChip(
                            icon = Icons.Filled.Speed,
                            label = "Speed (${formatFzSpeedLabel(playbackSpeed)})",
                            isActive = kotlin.math.abs(playbackSpeed - 1.0f) > 0.01f,
                            tag = "fz_tool_speed",
                            onClick = {
                                onUserInteraction()
                                onOpenSpeedPicker()
                            }
                        )
                        FzQuickToolbarItem.SCREENSHOT -> FzToolbarActionChip(
                            icon = Icons.Filled.CameraAlt,
                            label = "Screenshot",
                            isActive = false,
                            tag = "fz_tool_screenshot",
                            onClick = {
                                onUserInteraction()
                                onTakeScreenshot()
                            }
                        )
                        FzQuickToolbarItem.BACKGROUND_PLAY -> FzToolbarActionChip(
                            icon = Icons.Filled.Headphones,
                            label = "Background Play",
                            isActive = backgroundPlayEnabled,
                            tag = "fz_tool_background_play",
                            onClick = {
                                onUserInteraction()
                                onToggleBackgroundPlay()
                            }
                        )
                        FzQuickToolbarItem.SCREEN_ROTATION -> FzToolbarActionChip(
                            icon = Icons.Filled.ScreenRotation,
                            label = "Screen Rotation",
                            isActive = isLandscapeLocked,
                            tag = "fz_tool_screen_rotation",
                            onClick = {
                                onUserInteraction()
                                onRotateScreen()
                            }
                        )
                    }
                }
            }
        }

        // 3. CENTER AREA: Clean, minimalist dark video playback stage (unobstructed!)

        // 4 + 5. BOTTOM PITCH-BLACK HUD (Timeline & Scrubber Bar + 8-Button Control Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            FzPitchBlack.copy(alpha = 0.86f),
                            FzPitchBlack.copy(alpha = 0.98f)
                        )
                    )
                )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .padding(
                    start = if (isCompactWidth) 10.dp else 18.dp,
                    end = if (isCompactWidth) 10.dp else 18.dp,
                    top = if (isShortHeight) 8.dp else 14.dp,
                    bottom = if (isShortHeight) 6.dp else 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (isShortHeight) 2.dp else 6.dp)
        ) {
            // 4. TIMELINE & SCRUBBER BAR
            val hasKnownDuration = durationMs > 0L
            val activePos = if (isScrubbing) scrubPositionMs else positionMs
            val safeDuration = durationMs.coerceAtLeast(1L)
            val sliderFraction = if (hasKnownDuration) {
                (activePos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            } else 1f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fz_timeline_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Current playback time
                Text(
                    text = Formatters.formatDuration(activePos),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.testTag("fz_elapsed_time")
                )

                // Center: Interactive progress track with smooth soft blue scrubber handle
                val sliderInteraction = remember { MutableInteractionSource() }
                Slider(
                    value = sliderFraction,
                    enabled = hasKnownDuration,
                    interactionSource = sliderInteraction,
                    onValueChange = { fraction ->
                        if (hasKnownDuration) {
                            onUserInteraction()
                            onScrubChange((fraction * safeDuration).toLong())
                        }
                    },
                    onValueChangeFinished = {
                        if (hasKnownDuration) {
                            onScrubFinish(scrubPositionMs)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = FzSoftBlue,
                        activeTrackColor = FzAccentBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(FzSoftBlue)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.24f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sliderState.value.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(FzAccentBlue, FzSoftBlue)
                                        )
                                    )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = if (isCompactWidth) 8.dp else 12.dp)
                        .testTag("fz_seek_slider")
                )

                // Right: Remaining time display
                Text(
                    text = formatFzTimelineRemaining(activePos, durationMs),
                    color = if (hasKnownDuration) Color.White.copy(alpha = 0.88f) else FzSoftBlue,
                    style = TextStyle(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.testTag("fz_remaining_time")
                )
            }

            // 5. BOTTOM CONTROL BAR (Exact left-to-right order of 8 controls)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fz_bottom_controls")
            ) {
                val barMinWidth = maxWidth
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .widthIn(min = barMinWidth),
                    horizontalArrangement = if (barMinWidth >= 420.dp) {
                        Arrangement.SpaceBetween
                    } else {
                        Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Lock screen button
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onLockScreen()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Lock screen",
                            tint = Color.White
                        )
                    }

                    // 2. Jump back 10 seconds icon
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onJumpBackward10s()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_rewind_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Jump back 10 seconds",
                            tint = Color.White
                        )
                    }

                    // 3. Previous track button
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onPreviousTrack()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_prev_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = Color.White
                        )
                    }

                    // 4. Play/Pause toggle button (prominent center)
                    Surface(
                        onClick = {
                            onUserInteraction()
                            onTogglePlayPause()
                        },
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(if (isShortHeight) 48.dp else 54.dp)
                            .testTag("fz_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = FzPitchBlack,
                                modifier = Modifier.size(if (isShortHeight) 28.dp else 32.dp)
                            )
                        }
                    }

                    // 5. Next track button
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onNextTrack()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_next_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next track",
                            tint = Color.White
                        )
                    }

                    // 6. Jump forward 10 seconds icon
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onJumpForward10s()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_forward_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Jump forward 10 seconds",
                            tint = Color.White
                        )
                    }

                    // 7. Aspect ratio/fit button
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onCycleAspectRatio()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_aspect_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AspectRatio,
                            contentDescription = "Aspect ratio: ${aspectRatioMode.label}",
                            tint = if (aspectRatioMode != AspectRatioMode.FIT) FzSoftBlue else Color.White
                        )
                    }

                    // 8. Screen expand/pop-up window button
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onExpandOrPopup()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fz_expand_popup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Expand or pop-up window",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FzToolbarActionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    tag: String,
    onClick: () -> Unit,
    showIndicatorDot: Boolean = false,
    indicatorDotTag: String? = null
) {
    val contentTint = if (isActive) FzSoftBlue else Color.White
    val tileBg = if (isActive) FzAccentBlue.copy(alpha = 0.18f) else FzSurfaceTile
    val tileBorder = if (isActive) BorderStroke(0.8.dp, FzSoftBlue.copy(alpha = 0.55f)) else null
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(tileBg)
            .then(if (tileBorder != null) Modifier.border(tileBorder, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = FzSoftBlue),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentTint,
                modifier = Modifier.size(19.dp)
            )
            if (showIndicatorDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(FzSoftBlue)
                        .border(1.dp, FzPitchBlack, CircleShape)
                        .then(if (indicatorDotTag != null) Modifier.testTag(indicatorDotTag) else Modifier)
                )
            }
        }
        Text(
            text = label,
            color = contentTint.copy(alpha = 0.94f),
            style = TextStyle(fontSize = 10.5.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
