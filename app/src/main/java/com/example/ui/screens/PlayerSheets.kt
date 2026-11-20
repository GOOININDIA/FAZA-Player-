package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.SubtitleStyleConfig
import com.example.player.TrackOption
import com.example.ui.components.SubtitleStyleEditor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeedAndDecoderSheet(
    currentSpeed: Float,
    decoderMode: DecoderMode,
    onSpeedChange: (Float) -> Unit,
    onDecoderChange: (DecoderMode) -> Unit,
    onOpenGestureHelp: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format(Locale.US, "%.2fx", currentSpeed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = currentSpeed,
                onValueChange = { onSpeedChange(((it * 20f).toInt() / 20f).coerceIn(0.25f, 4.0f)) },
                valueRange = 0.25f..4.0f,
                modifier = Modifier.testTag("speed_slider")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                speedPresets.forEach { preset ->
                    FilterChip(
                        selected = kotlin.math.abs(currentSpeed - preset) < 0.03f,
                        onClick = { onSpeedChange(preset) },
                        shape = CircleShape,
                        label = { Text("${preset}x") }
                    )
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decoder Engine (HW / SW)", style = MaterialTheme.typography.titleLarge)
            }

            val hardwareEnabled = decoderMode != DecoderMode.SOFTWARE
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
                    Text("Hardware-Accelerated Decoding", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (hardwareEnabled) {
                            "On — GPU/MediaCodec acceleration enabled"
                        } else {
                            "Off — software decoding for older devices"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hardwareEnabled,
                    onCheckedChange = { enabled ->
                        onDecoderChange(if (enabled) DecoderMode.HARDWARE else DecoderMode.SOFTWARE)
                    },
                    modifier = Modifier.testTag("player_hardware_decoding_switch")
                )
            }

            DecoderMode.entries.forEach { mode ->
                val selected = mode == decoderMode
                Surface(
                    onClick = { onDecoderChange(mode) },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("decoder_option_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (onOpenGestureHelp != null) {
                OutlinedButton(
                    onClick = onOpenGestureHelp,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_gesture_help_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gesture Controls Help")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksAndSubtitlesSheet(
    audioTracks: List<TrackOption>,
    subtitleTracks: List<TrackOption>,
    subtitleStyle: SubtitleStyleConfig,
    onSelectAudio: (TrackOption) -> Unit,
    onSelectSubtitle: (TrackOption?) -> Unit,
    onLoadExternalSubtitle: () -> Unit,
    onOpenSubtitleStyling: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio Tracks", style = MaterialTheme.typography.titleLarge)
            }

            if (audioTracks.isEmpty()) {
                Text(
                    text = "Default audio stream active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                audioTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAudio(track) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = track.isSelected, onClick = { onSelectAudio(track) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(track.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subtitle Tracks", style = MaterialTheme.typography.titleLarge)
            }

            val anySelected = subtitleTracks.any { it.isSelected }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSubtitle(null) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = !anySelected || !subtitleStyle.enabled, onClick = { onSelectSubtitle(null) })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subtitles Off", style = MaterialTheme.typography.bodyLarge)
            }

            subtitleTracks.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSubtitle(track) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = track.isSelected && subtitleStyle.enabled, onClick = { onSelectSubtitle(track) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(track.label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onLoadExternalSubtitle,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load SRT/VTT")
                }
                Button(
                    onClick = onOpenSubtitleStyling,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_subtitle_style_button")
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Style")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleStyleBottomSheet(
    style: SubtitleStyleConfig,
    onStyleChange: (SubtitleStyleConfig) -> Unit,
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
                text = "Subtitle Styling Studio",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SubtitleStyleEditor(style = style, onStyleChange = onStyleChange)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoSettingsSheet(
    decoderMode: DecoderMode,
    aspectRatioMode: AspectRatioMode,
    nowPlayingEnabled: Boolean,
    runningTimeEnabled: Boolean,
    appIconOverlayEnabled: Boolean = false,
    continuePlayingDuringCall: Boolean = false,
    midVideoBreakEnabled: Boolean = true,
    fzUiEnabled: Boolean = false,
    isFavourite: Boolean = false,
    onDecoderChange: (DecoderMode) -> Unit,
    onContinuePlayingDuringCallChange: (Boolean) -> Unit = {},
    onMidVideoBreakChange: (Boolean) -> Unit = {},
    onToggleFzUi: (Boolean) -> Unit = {},
    onAspectRatioChange: (AspectRatioMode) -> Unit,
    onOpenTracksSheet: () -> Unit,
    onOpenNowPlayingSheet: () -> Unit,
    onOpenRunningTimeSheet: () -> Unit,
    onOpenAppIconOverlaySheet: (() -> Unit)? = null,
    onOpenPlayerUiCustomizer: (() -> Unit)? = null,
    onToggleFavourite: (() -> Unit)? = null,
    onOpenGestureHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    val hardwareAccelerationEnabled = decoderMode != DecoderMode.SOFTWARE

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Video Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hardware Acceleration Toggle Card (HW <-> SW)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hardware Acceleration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hardwareAccelerationEnabled) {
                                        "On (${decoderMode.badge}) — GPU decoding for smooth performance"
                                    } else {
                                        "Off (SW) — software decoding for device compatibility"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = hardwareAccelerationEnabled,
                            onCheckedChange = { enabled ->
                                onDecoderChange(if (enabled) DecoderMode.HARDWARE else DecoderMode.SOFTWARE)
                            },
                            modifier = Modifier.testTag("video_settings_hardware_acceleration_switch")
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DecoderMode.entries.forEach { mode ->
                            val chipLabel = when (mode) {
                                DecoderMode.AUTO -> "Auto"
                                DecoderMode.HARDWARE -> "Hardware (HW)"
                                DecoderMode.SOFTWARE -> "Software (SW)"
                            }
                            FilterChip(
                                selected = decoderMode == mode,
                                onClick = { onDecoderChange(mode) },
                                shape = CircleShape,
                                label = { Text(chipLabel) },
                                modifier = Modifier.testTag("video_settings_decoder_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Optional Call Playback Toggle (Keep video + voice audio playing when someone calls)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneInTalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Keep Playing During Calls",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (continuePlayingDuringCall) {
                                    "On — video and voice audio keep playing when someone calls"
                                } else {
                                    "Off — pauses video & audio automatically during calls"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = continuePlayingDuringCall,
                        onCheckedChange = onContinuePlayingDuringCallChange,
                        modifier = Modifier.testTag("video_settings_call_playback_switch")
                    )
                }
            }

            // Optional 10-Second Mid-Video Break Toggle (once at 50% for videos 20+ min)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "10s Mid-Video Break",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (midVideoBreakEnabled) {
                                    "On — shows once at 50% for videos 20+ min (optional)"
                                } else {
                                    "Off — plays uninterrupted from start to finish"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = midVideoBreakEnabled,
                        onCheckedChange = onMidVideoBreakChange,
                        modifier = Modifier.testTag("video_settings_mid_break_switch")
                    )
                }
            }

            // Aspect Ratio Quick Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AspectRatio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aspect Ratio", style = MaterialTheme.typography.titleMedium)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AspectRatioMode.entries.forEach { mode ->
                        FilterChip(
                            selected = aspectRatioMode == mode,
                            onClick = { onAspectRatioChange(mode) },
                            shape = CircleShape,
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Quick Links to Player Sub-menus
            Surface(
                onClick = onOpenTracksSheet,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio & Subtitles", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Select audio stream, captions & subtitle styling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                onClick = onOpenNowPlayingSheet,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.BrandingWatermark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Now Playing Display", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (nowPlayingEnabled) "On — customize theme & layout" else "Off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                onClick = onOpenRunningTimeSheet,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_settings_running_time_item")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Running Time", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (runningTimeEnabled) "On — customize style, placement & size" else "Off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onOpenAppIconOverlaySheet != null) {
                Surface(
                    onClick = onOpenAppIconOverlaySheet,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_settings_app_icon_item")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permanent App Icon", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (appIconOverlayEnabled) "On — customize style, opacity & placement" else "Off (Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (onOpenPlayerUiCustomizer != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text("FZ Ui Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Pitch-black pro player HUD with quick-access toolbar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = fzUiEnabled,
                            onCheckedChange = onToggleFzUi,
                            modifier = Modifier.testTag("video_settings_fz_ui_switch")
                        )
                    }
                }

                Surface(
                    onClick = onOpenPlayerUiCustomizer,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_settings_player_ui_customizer")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Customize Player UI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Accent color, bar theme, button layout, size, seek bar & gestures",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (onToggleFavourite != null) {
                Surface(
                    onClick = onToggleFavourite,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavourite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isFavourite) "Remove from Favourites" else "Add to Favourites",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenGestureHelp,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gesture Controls Help")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
