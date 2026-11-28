package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * State model for live on-screen Playing UI gesture feedback indicators.
 */
sealed interface PlayerGestureHudState {
    data class Brightness(val fraction: Float) : PlayerGestureHudState
    data class Volume(val fraction: Float) : PlayerGestureHudState
    data class Zoom(val scale: Float) : PlayerGestureHudState
    data class Seek(val targetMs: Long, val deltaMs: Long) : PlayerGestureHudState
    data class DoubleTapToggle(val isNowPlaying: Boolean) : PlayerGestureHudState
}

/**
 * State model for double-tap left/right skip feedback.
 */
data class PlayerDoubleTapSkipFeedback(
    val isForward: Boolean,
    val totalSeconds: Int,
    val tick: Int
)

/**
 * Centralized design tokens for FAZA Player's completely transparent, background-free
 * minimalist gesture controls UI.
 */
object PlayerGestureHudTokens {
    val ContainerBackground: Color = Color.Transparent
    val PrimaryInk: Color = Color.White
    val SecondaryInk: Color = Color.White.copy(alpha = 0.82f)
    val TrackInactive: Color = Color.White.copy(alpha = 0.28f)
    val TrackContrastUnderlay: Color = Color.Black.copy(alpha = 0.26f)
    val IconContrastUnderlay: Color = Color.Black.copy(alpha = 0.36f)
    const val AutoHideDelayMs: Long = 850L
    const val FadeInDurationMs: Int = 150
    const val FadeOutDurationMs: Int = 260
}

fun gestureHudEnterTransition() = fadeIn(
    animationSpec = tween(durationMillis = PlayerGestureHudTokens.FadeInDurationMs)
)

fun gestureHudExitTransition() = fadeOut(
    animationSpec = tween(durationMillis = PlayerGestureHudTokens.FadeOutDurationMs)
)

fun resolveGestureHudAlignment(hud: PlayerGestureHudState?): Alignment = when (hud) {
    is PlayerGestureHudState.Brightness -> Alignment.CenterStart
    is PlayerGestureHudState.Volume -> Alignment.CenterEnd
    is PlayerGestureHudState.Seek,
    is PlayerGestureHudState.Zoom,
    is PlayerGestureHudState.DoubleTapToggle,
    null -> Alignment.Center
}

private val LegibleTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.62f),
    offset = Offset(0f, 1f),
    blurRadius = 4f
)

private fun TextStyle.withGestureLegibility(): TextStyle = copy(shadow = LegibleTextShadow)

@Composable
private fun CrispMinimalGestureIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = PlayerGestureHudTokens.IconContrastUnderlay,
            modifier = Modifier
                .size(size)
                .padding(top = 1.dp)
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Completely transparent, background-free gesture HUD rendered directly over video content.
 * Displays only while the user actively performs a brightness, volume, seek, zoom, or toggle gesture.
 */
@Composable
fun MinimalistGestureHudOverlay(
    hud: PlayerGestureHudState,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("gesture_hud_indicator"),
        contentAlignment = Alignment.Center
    ) {
        when (hud) {
            is PlayerGestureHudState.Brightness -> {
                val brightnessIcon = when {
                    hud.fraction <= 0.33f -> Icons.Filled.BrightnessLow
                    hud.fraction <= 0.68f -> Icons.Filled.BrightnessMedium
                    else -> Icons.Filled.BrightnessHigh
                }
                MinimalistVerticalLevelIndicator(
                    icon = brightnessIcon,
                    label = "Brightness",
                    valueText = "${(hud.fraction.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                    progress = hud.fraction,
                    fillColor = PlayerGestureHudTokens.PrimaryInk,
                    iconTint = PlayerGestureHudTokens.PrimaryInk,
                    modifier = Modifier.testTag("gesture_hud_brightness")
                )
            }

            is PlayerGestureHudState.Volume -> {
                val volumeIcon = when {
                    hud.fraction <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                    hud.fraction <= 0.50f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                }
                MinimalistVerticalLevelIndicator(
                    icon = volumeIcon,
                    label = "Volume",
                    valueText = "${(hud.fraction.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                    progress = hud.fraction,
                    fillColor = accentColor,
                    iconTint = PlayerGestureHudTokens.PrimaryInk,
                    modifier = Modifier.testTag("gesture_hud_volume")
                )
            }

            is PlayerGestureHudState.Seek -> {
                MinimalistSeekGestureIndicator(
                    targetMs = hud.targetMs,
                    deltaMs = hud.deltaMs,
                    accentColor = accentColor,
                    modifier = Modifier.testTag("gesture_hud_seek")
                )
            }

            is PlayerGestureHudState.Zoom -> {
                MinimalistZoomGestureIndicator(
                    scale = hud.scale,
                    accentColor = accentColor,
                    modifier = Modifier.testTag("gesture_hud_zoom")
                )
            }

            is PlayerGestureHudState.DoubleTapToggle -> {
                CrispMinimalGestureIcon(
                    imageVector = if (hud.isNowPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (hud.isNowPlaying) "Play" else "Paused",
                    tint = PlayerGestureHudTokens.PrimaryInk,
                    size = 38.dp,
                    modifier = Modifier.testTag("gesture_hud_play_pause")
                )
            }
        }
    }
}

@Composable
private fun MinimalistVerticalLevelIndicator(
    icon: ImageVector,
    label: String,
    valueText: String,
    progress: Float,
    fillColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "minimal_gesture_vertical_progress"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CrispMinimalGestureIcon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            size = 22.dp
        )

        // Slim background-free vertical hairline level bar
        Canvas(
            modifier = Modifier
                .width(12.dp)
                .height(108.dp)
        ) {
            val centerX = size.width / 2f
            val strokePx = 3.dp.toPx()
            val capInset = strokePx / 2f
            val topY = capInset
            val bottomY = size.height - capInset
            val trackHeight = (bottomY - topY).coerceAtLeast(1f)

            drawLine(
                color = PlayerGestureHudTokens.TrackContrastUnderlay,
                start = Offset(centerX, topY),
                end = Offset(centerX, bottomY),
                strokeWidth = strokePx + 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = PlayerGestureHudTokens.TrackInactive,
                start = Offset(centerX, topY),
                end = Offset(centerX, bottomY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            if (animatedProgress > 0.005f) {
                val fillTopY = bottomY - (trackHeight * animatedProgress)
                drawLine(
                    color = fillColor,
                    start = Offset(centerX, bottomY),
                    end = Offset(centerX, fillTopY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }

        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium
                .copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                .withGestureLegibility(),
            color = PlayerGestureHudTokens.PrimaryInk
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
                .copy(fontSize = 10.sp, fontWeight = FontWeight.Medium)
                .withGestureLegibility(),
            color = PlayerGestureHudTokens.SecondaryInk
        )
    }
}

@Composable
private fun MinimalistSeekGestureIndicator(
    targetMs: Long,
    deltaMs: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CrispMinimalGestureIcon(
            imageVector = if (deltaMs >= 0L) Icons.Filled.FastForward else Icons.Filled.FastRewind,
            contentDescription = if (deltaMs >= 0L) "Seek forward" else "Seek backward",
            tint = PlayerGestureHudTokens.PrimaryInk,
            size = 26.dp
        )
        Text(
            text = Formatters.formatDuration(targetMs),
            style = MaterialTheme.typography.titleMedium
                .copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                .withGestureLegibility(),
            color = PlayerGestureHudTokens.PrimaryInk
        )
        Text(
            text = Formatters.formatSignedDelta(deltaMs),
            style = MaterialTheme.typography.labelSmall
                .copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
                .withGestureLegibility(),
            color = accentColor
        )
    }
}

@Composable
private fun MinimalistZoomGestureIndicator(
    scale: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = ((scale - 0.5f) / 3.5f).coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CrispMinimalGestureIcon(
            imageVector = Icons.Filled.ZoomIn,
            contentDescription = "Zoom",
            tint = PlayerGestureHudTokens.PrimaryInk,
            size = 22.dp
        )
        Text(
            text = String.format(Locale.US, "%.2fx", scale),
            style = MaterialTheme.typography.labelLarge
                .copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                .withGestureLegibility(),
            color = PlayerGestureHudTokens.PrimaryInk
        )
        Canvas(
            modifier = Modifier
                .width(88.dp)
                .height(8.dp)
        ) {
            val centerY = size.height / 2f
            val strokePx = 3.dp.toPx()
            val inset = strokePx / 2f
            val startX = inset
            val endX = size.width - inset
            val trackWidth = (endX - startX).coerceAtLeast(1f)
            drawLine(
                color = PlayerGestureHudTokens.TrackContrastUnderlay,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx + 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = PlayerGestureHudTokens.TrackInactive,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            if (progress > 0.005f) {
                drawLine(
                    color = accentColor,
                    start = Offset(startX, centerY),
                    end = Offset(startX + trackWidth * progress, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Transparent, background-free double-tap forward/backward skip indicator.
 */
@Composable
fun MinimalistDoubleTapSkipIndicator(
    feedback: PlayerDoubleTapSkipFeedback,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag(
            if (feedback.isForward) "double_tap_forward_hud" else "double_tap_rewind_hud"
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CrispMinimalGestureIcon(
            imageVector = if (feedback.isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
            contentDescription = if (feedback.isForward) {
                "Forward ${feedback.totalSeconds} seconds"
            } else {
                "Rewind ${feedback.totalSeconds} seconds"
            },
            tint = accentColor,
            size = 28.dp
        )
        Text(
            text = if (feedback.isForward) "+${feedback.totalSeconds}s" else "-${feedback.totalSeconds}s",
            style = MaterialTheme.typography.labelLarge
                .copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                .withGestureLegibility(),
            color = PlayerGestureHudTokens.PrimaryInk
        )
    }
}

/**
 * Transparent, background-free hold-to-speed gesture indicator.
 */
@Composable
fun MinimalistHoldSpeedIndicator(
    speedLabel: String,
    hintText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("hold_speed_hud"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CrispMinimalGestureIcon(
                imageVector = Icons.Filled.Speed,
                contentDescription = "Playback speed $speedLabel",
                tint = accentColor,
                size = 18.dp
            )
            Text(
                text = speedLabel,
                style = MaterialTheme.typography.titleSmall
                    .copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    .withGestureLegibility(),
                color = PlayerGestureHudTokens.PrimaryInk
            )
        }
        Text(
            text = hintText,
            style = MaterialTheme.typography.labelSmall
                .copy(fontSize = 10.sp, fontWeight = FontWeight.Medium)
                .withGestureLegibility(),
            color = PlayerGestureHudTokens.SecondaryInk
        )
    }
}
