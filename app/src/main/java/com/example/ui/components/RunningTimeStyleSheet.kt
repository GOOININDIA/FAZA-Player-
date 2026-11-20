package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RunningTimeConfig
import com.example.data.model.RunningTimeDisplayMode
import com.example.data.model.RunningTimePosition
import com.example.data.model.RunningTimeStyle
import com.example.data.model.RunningTimeTheme
import com.example.util.Formatters
import kotlin.math.abs
import kotlin.math.roundToInt

fun resolveRunningTimeBias(config: RunningTimeConfig): Pair<Float, Float> = when (config.position) {
    RunningTimePosition.TOP_END -> 1f to -1f
    RunningTimePosition.TOP_START -> -1f to -1f
    RunningTimePosition.TOP_CENTER -> 0f to -1f
    RunningTimePosition.BOTTOM_END -> 1f to 1f
    RunningTimePosition.BOTTOM_START -> -1f to 1f
    RunningTimePosition.BOTTOM_CENTER -> 0f to 1f
    RunningTimePosition.CUSTOM -> {
        val hx = (config.customXFraction.coerceIn(0f, 1f) * 2f) - 1f
        val vy = (config.customYFraction.coerceIn(0f, 1f) * 2f) - 1f
        hx to vy
    }
}

fun resolveRunningTimeAlignment(config: RunningTimeConfig): Alignment {
    val (hx, vy) = resolveRunningTimeBias(config)
    return BiasAlignment(horizontalBias = hx, verticalBias = vy)
}

fun formatRunningTimeLabel(
    positionMs: Long,
    durationMs: Long,
    displayMode: RunningTimeDisplayMode
): String {
    val safePositionMs = positionMs.coerceAtLeast(0L)
    if (durationMs <= 0L || displayMode == RunningTimeDisplayMode.ELAPSED_ONLY) {
        return Formatters.formatDuration(safePositionMs)
    }
    val clampedPositionMs = safePositionMs.coerceIn(0L, durationMs)
    return when (displayMode) {
        RunningTimeDisplayMode.ELAPSED_REMAINING -> {
            val remainingMs = (durationMs - clampedPositionMs).coerceAtLeast(0L)
            "${Formatters.formatDuration(clampedPositionMs)}/${Formatters.formatDuration(remainingMs)}"
        }
        RunningTimeDisplayMode.ELAPSED_TOTAL -> {
            "${Formatters.formatDuration(clampedPositionMs)}/${Formatters.formatDuration(durationMs)}"
        }
        RunningTimeDisplayMode.ELAPSED_ONLY -> {
            Formatters.formatDuration(safePositionMs)
        }
    }
}

@Composable
fun AlwaysOnRunningTimeOverlay(
    positionMs: Long,
    durationMs: Long,
    config: RunningTimeConfig,
    availableWidth: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (!config.enabled) return

    val baseSp = when {
        availableWidth < 360.dp -> 11f
        availableWidth < 600.dp -> 12.5f
        availableWidth < 840.dp -> 13.5f
        else -> 14.5f
    }
    val safeScale = config.fontScale.coerceIn(0.75f, 1.6f)
    val textSize = (baseSp * safeScale).sp
    val iconSize = (baseSp * safeScale * 1.05f).dp

    val timeLabel = formatRunningTimeLabel(
        positionMs = positionMs,
        durationMs = durationMs,
        displayMode = config.displayMode
    )

    val (bgColor, textColor, accentColor, borderStroke) = when (config.theme) {
        RunningTimeTheme.MATERIAL_YOU -> TimerPalette(
            bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            fg = MaterialTheme.colorScheme.onPrimaryContainer,
            accent = MaterialTheme.colorScheme.primary,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        )
        RunningTimeTheme.FROSTED_DARK -> TimerPalette(
            bg = Color(0xA60B0D12),
            fg = Color.White.copy(alpha = 0.96f),
            accent = MaterialTheme.colorScheme.primary,
            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.20f))
        )
        RunningTimeTheme.TRANSPARENT -> TimerPalette(
            bg = Color.Transparent,
            fg = Color.White,
            accent = MaterialTheme.colorScheme.primary,
            border = null
        )
        RunningTimeTheme.HIGH_CONTRAST -> TimerPalette(
            bg = Color(0xEB000000),
            fg = Color.White,
            accent = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
        )
    }

    val shape = when (config.style) {
        RunningTimeStyle.COMPACT_PILL -> RoundedCornerShape(999.dp)
        RunningTimeStyle.ROUNDED_CARD -> RoundedCornerShape(14.dp)
        RunningTimeStyle.BADGE_OUTLINED -> RoundedCornerShape(10.dp)
        RunningTimeStyle.MINIMAL_TEXT -> RoundedCornerShape(6.dp)
    }

    val effectiveBg = when (config.style) {
        RunningTimeStyle.MINIMAL_TEXT -> Color.Transparent
        RunningTimeStyle.BADGE_OUTLINED -> if (config.theme == RunningTimeTheme.TRANSPARENT) {
            Color(0x55000000)
        } else {
            bgColor.copy(alpha = (bgColor.alpha * 0.72f).coerceAtLeast(0.45f))
        }
        else -> bgColor
    }

    val effectiveBorder = when (config.style) {
        RunningTimeStyle.MINIMAL_TEXT -> null
        RunningTimeStyle.BADGE_OUTLINED -> BorderStroke(1.2.dp, accentColor.copy(alpha = 0.85f))
        else -> borderStroke
    }

    val textShadow = if (effectiveBg == Color.Transparent) {
        Shadow(color = Color.Black.copy(alpha = 0.92f), offset = Offset(1f, 1f), blurRadius = 6f)
    } else null

    val horizontalPad = when (config.style) {
        RunningTimeStyle.COMPACT_PILL -> (11f * safeScale.coerceAtMost(1.25f)).dp
        RunningTimeStyle.ROUNDED_CARD -> (13f * safeScale.coerceAtMost(1.25f)).dp
        RunningTimeStyle.BADGE_OUTLINED -> (10f * safeScale.coerceAtMost(1.25f)).dp
        RunningTimeStyle.MINIMAL_TEXT -> 6.dp
    }
    val verticalPad = when (config.style) {
        RunningTimeStyle.COMPACT_PILL -> (5f * safeScale.coerceAtMost(1.2f)).dp
        RunningTimeStyle.ROUNDED_CARD -> (7f * safeScale.coerceAtMost(1.2f)).dp
        RunningTimeStyle.BADGE_OUTLINED -> (5f * safeScale.coerceAtMost(1.2f)).dp
        RunningTimeStyle.MINIMAL_TEXT -> 2.dp
    }

    val interactionSource = remember { MutableInteractionSource() }
    val wrapperModifier = if (onClick != null) {
        modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Switch running time format",
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "Running time $timeLabel, ${config.displayMode.label}"
            }
            .testTag("always_on_running_time")
    } else {
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Running time $timeLabel"
            }
            .testTag("always_on_running_time")
    }

    Box(
        modifier = wrapperModifier,
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            color = effectiveBg,
            shape = shape,
            border = effectiveBorder,
            modifier = if (onClick != null) {
                Modifier
                    .clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
            } else {
                Modifier
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = horizontalPad, vertical = verticalPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.style != RunningTimeStyle.MINIMAL_TEXT) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = timeLabel,
                    style = TextStyle(
                        color = textColor,
                        fontSize = textSize,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                        shadow = textShadow
                    ),
                    maxLines = 1,
                    modifier = Modifier.testTag("running_time_text")
                )
            }
        }
    }
}

private data class TimerPalette(
    val bg: Color,
    val fg: Color,
    val accent: Color,
    val border: BorderStroke?
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunningTimeStyleEditor(
    config: RunningTimeConfig,
    onConfigChange: (RunningTimeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizePresets = listOf(
        "Small" to 0.85f,
        "Medium" to 1.0f,
        "Large" to 1.2f,
        "XL" to 1.45f
    )

    val currentConfig by rememberUpdatedState(config)
    val currentOnConfigChange by rememberUpdatedState(onConfigChange)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Interactive Live Placement Theater Preview
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0E131F), Color(0xFF1A2236), Color(0xFF0E131F))
                    )
                )
                .pointerInput(config.enabled) {
                    if (!config.enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val xFrac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val yFrac = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        currentOnConfigChange(
                            currentConfig.copy(
                                position = RunningTimePosition.CUSTOM,
                                customXFraction = xFrac,
                                customYFraction = yFrac
                            )
                        )
                    }
                }
                .pointerInput(config.enabled) {
                    if (!config.enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val xFrac = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val yFrac = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        currentOnConfigChange(
                            currentConfig.copy(
                                position = RunningTimePosition.CUSTOM,
                                customXFraction = xFrac,
                                customYFraction = yFrac
                            )
                        )
                    }
                }
                .testTag("running_time_preview_canvas")
        ) {
            val previewWidth = maxWidth
            if (config.enabled) {
                val (_, verticalBias) = resolveRunningTimeBias(config)
                val hintAlignment = if (verticalBias > 0.25f) Alignment.TopStart else Alignment.BottomStart
                val hintText = if (config.position == RunningTimePosition.CUSTOM) {
                    "Custom (${(config.customXFraction * 100).roundToInt()}%, ${(config.customYFraction * 100).roundToInt()}%) · Drag to place"
                } else {
                    "${config.position.label} Preview · Tap or drag to customize"
                }

                Text(
                    text = hintText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                    modifier = Modifier
                        .align(hintAlignment)
                        .padding(12.dp)
                        .testTag("running_time_placement_hint")
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    AlwaysOnRunningTimeOverlay(
                        positionMs = 720_000L,
                        durationMs = 840_000L,
                        config = config,
                        availableWidth = previewWidth,
                        onClick = {
                            onConfigChange(config.copy(displayMode = config.displayMode.next()))
                        },
                        modifier = Modifier.align(resolveRunningTimeAlignment(config))
                    )
                }
            } else {
                Text(
                    text = "Running Time is turned OFF",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // On / Off Option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text("Always Show Running Time", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Display video running time at your chosen screen placement during playback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
                modifier = Modifier.testTag("running_time_enabled_switch")
            )
        }

        if (config.enabled) {
            // Customized Placement Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenWith,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Placement", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = if (config.position == RunningTimePosition.CUSTOM) {
                            "${(config.customXFraction * 100).roundToInt()}% · ${(config.customYFraction * 100).roundToInt()}%"
                        } else {
                            config.position.label
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("rt_placement_summary")
                    )
                }
                Text(
                    text = "Pick any corner, center edge, or choose Custom to place it anywhere on screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RunningTimePosition.entries.forEach { pos ->
                        FilterChip(
                            selected = config.position == pos,
                            onClick = { onConfigChange(config.copy(position = pos)) },
                            label = { Text(pos.label) },
                            modifier = Modifier.testTag("rt_position_${pos.name.lowercase()}")
                        )
                    }
                }

                AnimatedVisibility(visible = config.position == RunningTimePosition.CUSTOM) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("rt_custom_placement_controls")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Horizontal Position", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${(config.customXFraction * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.customXFraction.coerceIn(0f, 1f),
                                onValueChange = {
                                    onConfigChange(
                                        config.copy(
                                            position = RunningTimePosition.CUSTOM,
                                            customXFraction = it.coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.testTag("rt_custom_x_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vertical Position", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${(config.customYFraction * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.customYFraction.coerceIn(0f, 1f),
                                onValueChange = {
                                    onConfigChange(
                                        config.copy(
                                            position = RunningTimePosition.CUSTOM,
                                            customYFraction = it.coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.testTag("rt_custom_y_slider")
                            )

                            FilledTonalButton(
                                onClick = {
                                    onConfigChange(
                                        config.copy(
                                            position = RunningTimePosition.TOP_END,
                                            customXFraction = 0.88f,
                                            customYFraction = 0.12f
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("rt_reset_placement_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset to Top Right")
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Timer UI Style", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RunningTimeStyle.entries.forEach { style ->
                        FilterChip(
                            selected = config.style == style,
                            onClick = { onConfigChange(config.copy(style = style)) },
                            label = { Text(style.label) },
                            modifier = Modifier.testTag("rt_style_${style.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RunningTimeTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = config.theme == theme,
                            onClick = { onConfigChange(config.copy(theme = theme)) },
                            label = { Text(theme.label) },
                            modifier = Modifier.testTag("rt_theme_${theme.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${(config.fontScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sizePresets.forEach { (label, preset) ->
                        FilterChip(
                            selected = abs(config.fontScale - preset) < 0.05f,
                            onClick = { onConfigChange(config.copy(fontScale = preset)) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("rt_size_${label.lowercase()}")
                        )
                    }
                }
                Slider(
                    value = config.fontScale,
                    onValueChange = { onConfigChange(config.copy(fontScale = it)) },
                    valueRange = 0.75f..1.60f,
                    modifier = Modifier.testTag("rt_size_slider")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Timer Display Format", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tap the running time badge during playback to cycle modes (${config.displayMode.exampleHint})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RunningTimeDisplayMode.entries.forEach { mode ->
                        FilterChip(
                            selected = config.displayMode == mode,
                            onClick = { onConfigChange(config.copy(displayMode = mode)) },
                            label = { Text("${mode.label} (${mode.exampleHint})") },
                            modifier = Modifier.testTag("rt_display_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningTimeStyleBottomSheet(
    config: RunningTimeConfig,
    onConfigChange: (RunningTimeConfig) -> Unit,
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
                text = "Running Time",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RunningTimeStyleEditor(config = config, onConfigChange = onConfigChange)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
