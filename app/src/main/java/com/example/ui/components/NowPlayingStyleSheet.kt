package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NowPlayingConfig
import com.example.data.model.NowPlayingPosition
import com.example.data.model.NowPlayingStyle
import com.example.data.model.NowPlayingTheme
import com.example.util.Formatters
import kotlin.math.roundToInt

fun formatNowPlayingStatus(isPlaying: Boolean): String =
    if (isPlaying) "Now Playing" else "Pause"

fun formatUpNextBadge(remainingMs: Long?): String =
    if (remainingMs != null && remainingMs > 0L) {
        "Next in ${Formatters.formatDuration(remainingMs)}"
    } else {
        "Up Next"
    }

@Composable
fun PermanentNowPlayingBanner(
    fileName: String,
    isPlaying: Boolean,
    config: NowPlayingConfig,
    availableWidth: Dp,
    modifier: Modifier = Modifier,
    upNextTitle: String? = null,
    upNextRemainingMs: Long? = null,
    onPlayNext: (() -> Unit)? = null
) {
    if (!config.enabled) return

    val showUpNextRow = config.showUpNext && !upNextTitle.isNullOrBlank()
    val isTwoLine = config.maxLines >= 2
    val baseSp = when {
        availableWidth < 360.dp -> 11f
        availableWidth < 600.dp -> 12.5f
        else -> 14f
    }
    val textSize = (baseSp * config.fontScale).sp
    val statusFontSize = (baseSp * config.fontScale * 0.82f).coerceAtLeast(9.5f).sp
    val statusIconSize = (baseSp * config.fontScale * 1.05f).coerceIn(12f, 18f).dp
    val statusText = formatNowPlayingStatus(isPlaying)

    val (bgColor, textColor, targetIconColor, borderStroke) = when (config.theme) {
        NowPlayingTheme.MATERIAL_YOU -> Quad(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        )
        NowPlayingTheme.FROSTED_DARK -> Quad(
            Color(0xA60B0D12),
            Color.White.copy(alpha = 0.96f),
            if (isPlaying) Color(0xFF4ADE80) else Color(0xFFFBBF24),
            BorderStroke(0.7.dp, Color.White.copy(alpha = 0.20f))
        )
        NowPlayingTheme.TRANSPARENT -> Quad(
            Color.Transparent,
            Color.White,
            if (isPlaying) Color(0xFF4ADE80) else Color(0xFFFBBF24),
            null
        )
        NowPlayingTheme.HIGH_CONTRAST -> Quad(
            Color(0xEB000000),
            Color(0xFFFFFFFF),
            if (isPlaying) Color(0xFF4ADE80) else Color(0xFFFBBF24),
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
        )
    }

    val statusIconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = tween(180),
        label = "now_playing_status_icon_color"
    )

    val statusPillBg by animateColorAsState(
        targetValue = when (config.theme) {
            NowPlayingTheme.MATERIAL_YOU -> if (isPlaying) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            }
            NowPlayingTheme.TRANSPARENT -> Color.Black.copy(alpha = 0.48f)
            else -> if (isPlaying) {
                Color(0xFF4ADE80).copy(alpha = 0.18f)
            } else {
                Color(0xFFFBBF24).copy(alpha = 0.20f)
            }
        },
        animationSpec = tween(180),
        label = "now_playing_status_bg"
    )

    val statusTextColor by animateColorAsState(
        targetValue = when (config.theme) {
            NowPlayingTheme.MATERIAL_YOU -> if (isPlaying) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            }
            else -> if (isPlaying) Color(0xFF86EFAC) else Color(0xFFFCD34D)
        },
        animationSpec = tween(180),
        label = "now_playing_status_fg"
    )

    val shape = when (config.style) {
        NowPlayingStyle.COMPACT_PILL -> RoundedCornerShape(if (isTwoLine || showUpNextRow) 18.dp else 999.dp)
        NowPlayingStyle.ROUNDED_CARD -> RoundedCornerShape(16.dp)
        NowPlayingStyle.SLIM_BANNER -> RoundedCornerShape(12.dp)
        NowPlayingStyle.MINIMAL_TEXT -> RoundedCornerShape(8.dp)
    }

    val effectiveBg = if (config.style == NowPlayingStyle.MINIMAL_TEXT) Color.Transparent else bgColor
    val effectiveBorder = if (config.style == NowPlayingStyle.MINIMAL_TEXT) null else borderStroke

    val textShadow = if (effectiveBg == Color.Transparent) {
        Shadow(color = Color.Black.copy(alpha = 0.9f), offset = Offset(1f, 1f), blurRadius = 6f)
    } else null

    val horizontalPad = when (config.style) {
        NowPlayingStyle.COMPACT_PILL -> 12.dp
        NowPlayingStyle.ROUNDED_CARD -> 16.dp
        NowPlayingStyle.SLIM_BANNER -> 16.dp
        NowPlayingStyle.MINIMAL_TEXT -> 8.dp
    }
    val baseVerticalPad = when (config.style) {
        NowPlayingStyle.COMPACT_PILL -> 5.dp
        NowPlayingStyle.ROUNDED_CARD -> 8.dp
        NowPlayingStyle.SLIM_BANNER -> 7.dp
        NowPlayingStyle.MINIMAL_TEXT -> 3.dp
    }
    val verticalPad = if (isTwoLine) baseVerticalPad + 2.dp else baseVerticalPad

    val widthModifier = if (config.style == NowPlayingStyle.SLIM_BANNER) {
        Modifier.fillMaxWidth(0.92f)
    } else {
        Modifier
    }

    val upNextAccentColor = when (config.theme) {
        NowPlayingTheme.MATERIAL_YOU -> MaterialTheme.colorScheme.secondary
        NowPlayingTheme.HIGH_CONTRAST -> Color(0xFF93C5FD)
        else -> Color(0xFFA78BFA)
    }
    val upNextPillBg = when (config.theme) {
        NowPlayingTheme.MATERIAL_YOU -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        NowPlayingTheme.TRANSPARENT -> Color.Black.copy(alpha = 0.50f)
        else -> upNextAccentColor.copy(alpha = 0.20f)
    }
    val upNextFontSize = (baseSp * config.fontScale * 0.88f).coerceAtLeast(10f).sp
    val upNextBadgeFontSize = (baseSp * config.fontScale * 0.78f).coerceAtLeast(9f).sp

    Surface(
        color = effectiveBg,
        shape = shape,
        border = effectiveBorder,
        modifier = modifier
            .then(widthModifier)
            .testTag("permanent_now_playing_bar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPad, vertical = verticalPad),
            verticalArrangement = Arrangement.spacedBy(if (showUpNextRow) 4.dp else 0.dp)
        ) {
            if (isTwoLine) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (config.showLiveDot) {
                            PlaybackStatusIcon(
                                isPlaying = isPlaying,
                                contentDescription = statusText,
                                tint = statusIconColor,
                                iconSize = statusIconSize
                            )
                        }
                        Text(
                            text = statusText,
                            style = TextStyle(
                                color = statusTextColor,
                                fontSize = statusFontSize,
                                fontWeight = FontWeight.Bold,
                                shadow = textShadow
                            ),
                            maxLines = 1,
                            modifier = Modifier.testTag("now_playing_status_text")
                        )
                    }
                    Text(
                        text = fileName,
                        style = TextStyle(
                            color = textColor,
                            fontSize = textSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = (baseSp * config.fontScale * 1.25f).sp,
                            shadow = textShadow
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("now_playing_text")
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMinimal = config.style == NowPlayingStyle.MINIMAL_TEXT
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = if (isMinimal) {
                            Modifier
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(statusPillBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        }
                    ) {
                        if (config.showLiveDot) {
                            PlaybackStatusIcon(
                                isPlaying = isPlaying,
                                contentDescription = statusText,
                                tint = statusIconColor,
                                iconSize = statusIconSize
                            )
                        }
                        Text(
                            text = statusText,
                            style = TextStyle(
                                color = statusTextColor,
                                fontSize = statusFontSize,
                                fontWeight = FontWeight.Bold,
                                shadow = textShadow
                            ),
                            maxLines = 1,
                            modifier = Modifier.testTag("now_playing_status_text")
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fileName,
                        style = TextStyle(
                            color = textColor,
                            fontSize = textSize,
                            fontWeight = FontWeight.SemiBold,
                            shadow = textShadow
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(
                                if (config.marqueeScroll) {
                                    Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("now_playing_text")
                    )
                }
            }

            AnimatedVisibility(
                visible = showUpNextRow,
                enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
            ) {
                val safeUpNextTitle = upNextTitle.orEmpty()
                val badgeText = formatUpNextBadge(upNextRemainingMs)
                val isMinimal = config.style == NowPlayingStyle.MINIMAL_TEXT
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (onPlayNext != null) {
                                    Modifier.clickable(
                                        onClickLabel = "Play next video: $safeUpNextTitle",
                                        onClick = onPlayNext
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(vertical = 1.dp)
                            .testTag("now_playing_up_next_row")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = if (isMinimal) {
                                Modifier
                            } else {
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(upNextPillBg)
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Up next",
                                tint = upNextAccentColor,
                                modifier = Modifier.size((statusIconSize.value * 0.92f).coerceIn(11f, 16f).dp)
                            )
                            Text(
                                text = badgeText,
                                style = TextStyle(
                                    color = upNextAccentColor,
                                    fontSize = upNextBadgeFontSize,
                                    fontWeight = FontWeight.Bold,
                                    shadow = textShadow
                                ),
                                maxLines = 1,
                                modifier = Modifier.testTag("now_playing_up_next_badge")
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = safeUpNextTitle,
                            style = TextStyle(
                                color = textColor.copy(alpha = 0.92f),
                                fontSize = upNextFontSize,
                                fontWeight = FontWeight.Medium,
                                shadow = textShadow
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .then(
                                    if (config.marqueeScroll) {
                                        Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("now_playing_up_next_title")
                        )
                    }
            }
        }
    }
}

@Composable
private fun PlaybackStatusIcon(
    isPlaying: Boolean,
    contentDescription: String,
    tint: Color,
    iconSize: Dp
) {
    Crossfade(
        targetState = isPlaying,
        animationSpec = tween(150),
        label = "now_playing_icon_swap"
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .testTag("now_playing_status_icon")
        )
    }
}

private data class Quad(
    val bg: Color,
    val fg: Color,
    val dot: Color,
    val border: BorderStroke?
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NowPlayingStyleEditor(
    config: NowPlayingConfig,
    onConfigChange: (NowPlayingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewIsPlaying by remember { mutableStateOf(true) }
    var previewShowUpNext by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Theater Preview
        val previewShowsUpNext = config.showUpNext && previewShowUpNext
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    when {
                        config.maxLines >= 2 && previewShowsUpNext -> 136.dp
                        config.maxLines >= 2 || previewShowsUpNext -> 114.dp
                        else -> 94.dp
                    }
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0E131F), Color(0xFF1A2236), Color(0xFF0E131F))
                    )
                )
        ) {
            val previewWidth = maxWidth
            val previewAlignment = when (config.position) {
                NowPlayingPosition.TOP_CENTER -> Alignment.TopCenter
                NowPlayingPosition.TOP_START -> Alignment.TopStart
                NowPlayingPosition.BOTTOM_CENTER -> Alignment.BottomCenter
            }
            if (config.enabled) {
                Box(
                    modifier = Modifier
                        .align(previewAlignment)
                        .padding(12.dp)
                ) {
                    PermanentNowPlayingBanner(
                        fileName = "FAZA_8K_Ultra_HD_Cinema_Demo.mkv",
                        isPlaying = previewIsPlaying,
                        config = config,
                        availableWidth = previewWidth,
                        upNextTitle = if (previewShowsUpNext) "FAZA_8K_Next_Episode_02.mkv" else null,
                        upNextRemainingMs = if (previewShowsUpNext) config.upNextLeadMinutes * 60_000L else null
                    )
                }
            } else {
                Text(
                    text = "Now Playing Display is turned OFF",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (config.enabled) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Preview:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                FilterChip(
                    selected = previewIsPlaying,
                    onClick = { previewIsPlaying = true },
                    label = { Text("Now Playing") },
                    modifier = Modifier.testTag("np_preview_playing")
                )
                FilterChip(
                    selected = !previewIsPlaying,
                    onClick = { previewIsPlaying = false },
                    label = { Text("Pause") },
                    modifier = Modifier.testTag("np_preview_paused")
                )
                if (config.showUpNext) {
                    FilterChip(
                        selected = previewShowUpNext,
                        onClick = { previewShowUpNext = !previewShowUpNext },
                        label = { Text("Up Next (${Formatters.formatDuration(config.upNextLeadMinutes * 60_000L)})") },
                        modifier = Modifier.testTag("np_preview_up_next")
                    )
                }
            }
        }

        // On / Off Option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Permanent Now Playing", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Display active video playback state and file name in the player",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
                modifier = Modifier.testTag("now_playing_enabled_switch")
            )
        }

        if (config.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Display Lines", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = config.maxLines == 1,
                        onClick = { onConfigChange(config.copy(maxLines = 1)) },
                        label = { Text("1 Line") },
                        modifier = Modifier.testTag("np_lines_1")
                    )
                    FilterChip(
                        selected = config.maxLines >= 2,
                        onClick = { onConfigChange(config.copy(maxLines = 2)) },
                        label = { Text("2 Lines") },
                        modifier = Modifier.testTag("np_lines_2")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Display Theme", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NowPlayingTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = config.theme == theme,
                            onClick = { onConfigChange(config.copy(theme = theme)) },
                            label = { Text(theme.label) },
                            modifier = Modifier.testTag("np_theme_${theme.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Display Style", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NowPlayingStyle.entries.forEach { style ->
                        FilterChip(
                            selected = config.style == style,
                            onClick = { onConfigChange(config.copy(style = style)) },
                            label = { Text(style.label) },
                            modifier = Modifier.testTag("np_style_${style.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Placement", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NowPlayingPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = config.position == pos,
                            onClick = { onConfigChange(config.copy(position = pos)) },
                            label = { Text(pos.label) }
                        )
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Text Scale", style = MaterialTheme.typography.titleSmall)
                    Text("${(config.fontScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = config.fontScale,
                    onValueChange = { onConfigChange(config.copy(fontScale = it)) },
                    valueRange = 0.85f..1.25f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playback State Icon (Play / Pause)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = config.showLiveDot,
                    onCheckedChange = { onConfigChange(config.copy(showLiveDot = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Show Next Video in Folder", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Preview the next video in this folder during the final ${Formatters.formatDuration(config.upNextLeadMinutes * 60_000L)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.showUpNext,
                    onCheckedChange = { onConfigChange(config.copy(showUpNext = it)) },
                    modifier = Modifier.testTag("np_show_up_next_switch")
                )
            }

            if (config.showUpNext) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Show Next Video Before End", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 5, 10).forEach { mins ->
                            FilterChip(
                                selected = config.upNextLeadMinutes == mins,
                                onClick = { onConfigChange(config.copy(upNextLeadMinutes = mins)) },
                                label = { Text("${Formatters.formatDuration(mins * 60_000L)} left") },
                                modifier = Modifier.testTag("np_up_next_lead_${mins}")
                            )
                        }
                    }
                }
            }

            if (config.maxLines == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scroll Long Filenames (Marquee)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = config.marqueeScroll,
                        onCheckedChange = { onConfigChange(config.copy(marqueeScroll = it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingStyleBottomSheet(
    config: NowPlayingConfig,
    onConfigChange: (NowPlayingConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Now Playing Display",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            NowPlayingStyleEditor(config = config, onConfigChange = onConfigChange)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
