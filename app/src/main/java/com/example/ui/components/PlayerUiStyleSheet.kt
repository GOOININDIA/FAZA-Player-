package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PlayerAccentPreset
import com.example.data.model.PlayerBarTheme
import com.example.data.model.PlayerControlSize
import com.example.data.model.PlayerControlStyle
import com.example.data.model.PlayerUiConfig
import com.example.data.model.PlayerUiThemeStyle
import com.example.data.model.SeekBarStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun resolvePlayerAccent(
    preset: PlayerAccentPreset,
    fallback: Color = MaterialTheme.colorScheme.primary
): Color = preset.colorHex?.let { Color(it) } ?: fallback

@Composable
fun PlayerUiPreviewStage(
    config: PlayerUiConfig,
    modifier: Modifier = Modifier
) {
    val accent = resolvePlayerAccent(config.accentPreset)
    val opacity = config.hudOpacity.coerceIn(0.4f, 1.0f)

    val topBg = when (config.barTheme) {
        PlayerBarTheme.CINEMA_DARK -> Color(0xFF0A0A14).copy(alpha = 0.86f * opacity)
        PlayerBarTheme.FROSTED_GLASS -> Color(0xFF141328).copy(alpha = 0.74f * opacity)
        PlayerBarTheme.AMOLED_BLACK -> Color.Black.copy(alpha = 0.94f * opacity)
        PlayerBarTheme.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f * opacity)
        PlayerBarTheme.MINIMAL_CLEAR -> Color.Transparent
    }
    val bottomBg = when (config.barTheme) {
        PlayerBarTheme.CINEMA_DARK -> Color(0xFF0A0A14).copy(alpha = 0.91f * opacity)
        PlayerBarTheme.FROSTED_GLASS -> Color(0xFF141328).copy(alpha = 0.80f * opacity)
        PlayerBarTheme.AMOLED_BLACK -> Color.Black.copy(alpha = 0.96f * opacity)
        PlayerBarTheme.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f * opacity)
        PlayerBarTheme.MINIMAL_CLEAR -> Color.Transparent
    }
    val isMaterialYouBar = config.barTheme == PlayerBarTheme.MATERIAL_YOU
    val onBarText = if (isMaterialYouBar) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }
    val roundedChipBg = if (isMaterialYouBar) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f * opacity)
    } else {
        Color.Black.copy(alpha = 0.42f * opacity)
    }

    val controlShape = when (config.controlStyle) {
        PlayerControlStyle.STANDARD -> CircleShape
        PlayerControlStyle.PILL -> RoundedCornerShape(16.dp)
        PlayerControlStyle.OUTLINED -> CircleShape
        PlayerControlStyle.MINIMAL -> CircleShape
    }
    val primaryFill = when (config.controlStyle) {
        PlayerControlStyle.STANDARD, PlayerControlStyle.PILL -> {
            if (isMaterialYouBar) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f * opacity)
            } else {
                Color.White.copy(alpha = 0.92f * opacity)
            }
        }
        PlayerControlStyle.OUTLINED -> Color.Black.copy(alpha = 0.35f * opacity)
        PlayerControlStyle.MINIMAL -> Color.Transparent
    }
    val primaryIconTint = when (config.controlStyle) {
        PlayerControlStyle.STANDARD, PlayerControlStyle.PILL -> {
            if (isMaterialYouBar) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF0A0B0E)
        }
        PlayerControlStyle.OUTLINED, PlayerControlStyle.MINIMAL -> Color.White
    }
    val secondaryFill = when (config.controlStyle) {
        PlayerControlStyle.STANDARD, PlayerControlStyle.PILL -> {
            if (isMaterialYouBar) {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f * opacity)
            } else {
                Color.Black.copy(alpha = 0.45f * opacity)
            }
        }
        PlayerControlStyle.OUTLINED -> Color.Black.copy(alpha = 0.25f * opacity)
        PlayerControlStyle.MINIMAL -> Color.Transparent
    }
    val secondaryIconTint = if (isMaterialYouBar && (config.controlStyle == PlayerControlStyle.STANDARD || config.controlStyle == PlayerControlStyle.PILL)) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }

    val scale = config.controlSize.scale

    if (config.themeStyle == PlayerUiThemeStyle.FZ_UI) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FzPitchBlack)
                .border(1.dp, FzSoftBlue.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("player_ui_preview_stage")
        ) {
            Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FZ_Cinema_4K.mkv", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Text("88%  12:34", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = FzAccentBlue.copy(alpha = 0.25f), shape = RoundedCornerShape(5.dp)) {
                        Text("HW+", style = MaterialTheme.typography.labelSmall, color = FzSoftBlue, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Night", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                    Text("Shuffle", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                    Text("A-B", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                    Text("Audio FX •", style = MaterialTheme.typography.labelSmall, color = FzSoftBlue)
                    Text("Speed (1X)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                }
            }
            Surface(
                color = FzAccentBlue.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "FZ Ui Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = FzSoftBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("12:00", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.24f))) {
                        Box(modifier = Modifier.fillMaxWidth(0.62f).fillMaxSize().clip(RoundedCornerShape(999.dp)).background(FzSoftBlue))
                    }
                    Text("-02:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Icon(Icons.Filled.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Icon(Icons.Filled.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Icon(Icons.Filled.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Icon(Icons.Filled.PictureInPictureAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0D1322), Color(0xFF1F2B46), Color(0xFF101521))
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .testTag("player_ui_preview_stage")
    ) {
        // Top Bar Preview (rounded circular icons & full pill badge)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(topBg, Color.Transparent)))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(roundedChipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = onBarText, modifier = Modifier.size(14.dp))
            }
            if (config.showResolutionBadge) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = roundedChipBg,
                    shape = CircleShape
                ) {
                    Text(
                        text = "4K UHD • AUTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMaterialYouBar) MaterialTheme.colorScheme.onSecondaryContainer else accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (config.showFavouriteButton) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = onBarText, modifier = Modifier.size(12.dp))
                    }
                }
                if (config.showPipButton) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PictureInPictureAlt, contentDescription = null, tint = onBarText, modifier = Modifier.size(12.dp))
                    }
                }
                if (config.showRotateButton) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ScreenRotation, contentDescription = null, tint = onBarText, modifier = Modifier.size(12.dp))
                    }
                }
                if (config.showLockButton) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = onBarText, modifier = Modifier.size(12.dp))
                    }
                }
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = onBarText, modifier = Modifier.size(12.dp))
                }
            }
        }

        // Center Transport Cluster Preview
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.showCenterSkipButtons) {
                Box(
                    modifier = Modifier
                        .size((30f * scale).dp)
                        .clip(controlShape)
                        .background(secondaryFill)
                        .then(
                            if (config.controlStyle == PlayerControlStyle.OUTLINED) {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), controlShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Replay10, contentDescription = null, tint = secondaryIconTint, modifier = Modifier.size((16f * scale).dp))
                }
            }

            Box(
                modifier = Modifier
                    .size((40f * scale).dp)
                    .clip(controlShape)
                    .background(primaryFill)
                    .then(
                        if (config.controlStyle == PlayerControlStyle.OUTLINED) {
                            Modifier.border(1.5.dp, accent, controlShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = primaryIconTint, modifier = Modifier.size((22f * scale).dp))
            }

            if (config.showCenterSkipButtons) {
                Box(
                    modifier = Modifier
                        .size((30f * scale).dp)
                        .clip(controlShape)
                        .background(secondaryFill)
                        .then(
                            if (config.controlStyle == PlayerControlStyle.OUTLINED) {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), controlShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Forward10, contentDescription = null, tint = secondaryIconTint, modifier = Modifier.size((16f * scale).dp))
                }
            }
        }

        // Bottom Rounded Floating Seek + Toolbar Preview
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (config.barTheme == PlayerBarTheme.MINIMAL_CLEAR) Color.Transparent else bottomBg,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val barHeight = when (config.seekBarStyle) {
                    SeekBarStyle.STANDARD -> 4.dp
                    SeekBarStyle.THICK_BAR -> 7.dp
                    SeekBarStyle.MINIMAL_LINE -> 2.dp
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("12:34", style = MaterialTheme.typography.labelSmall, color = onBarText)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(999.dp))
                            .background(onBarText.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.58f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent)
                        )
                    }
                    Text("42:10", style = MaterialTheme.typography.labelSmall, color = onBarText.copy(alpha = 0.75f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.showPrevNextButtons) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = null, tint = onBarText, modifier = Modifier.size(13.dp))
                        }
                    }
                    if (config.showDecoderButton) {
                        Surface(color = roundedChipBg, shape = CircleShape) {
                            Icon(Icons.Filled.Memory, contentDescription = null, tint = accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).size(13.dp))
                        }
                    }
                    if (config.showAspectButton) {
                        Surface(color = roundedChipBg, shape = CircleShape) {
                            Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = onBarText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).size(13.dp))
                        }
                    }
                    if (config.showTracksButton) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = onBarText, modifier = Modifier.size(13.dp))
                        }
                    }
                    if (config.showNowPlayingButton) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.BrandingWatermark, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                        }
                    }
                    if (config.showPrevNextButtons) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(roundedChipBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, tint = onBarText, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerUiCustomizerEditor(
    config: PlayerUiConfig,
    onConfigChange: (PlayerUiConfig) -> Unit,
    onReset: () -> Unit,
    onOpenNowPlaying: (() -> Unit)? = null,
    onOpenRunningTime: (() -> Unit)? = null,
    onOpenAppIconOverlay: (() -> Unit)? = null,
    onOpenSubtitleStyle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val seekStepOptions = listOf(5, 10, 15, 30)
    val autoHideOptions = listOf(2, 4, 6, 10, 0)
    val holdSpeedOptions = listOf(1.5f, 2.0f, 2.5f, 3.0f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlayerUiPreviewStage(config = config)

        // Playing UI Screen Theme (Optional FZ Ui vs FAZA Classic)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Playing UI Theme", style = MaterialTheme.typography.titleSmall)
            Text(
                text = config.themeStyle.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerUiThemeStyle.entries.forEach { theme ->
                    FilterChip(
                        selected = config.themeStyle == theme,
                        onClick = { onConfigChange(config.copy(themeStyle = theme)) },
                        label = { Text(theme.label) },
                        modifier = Modifier.testTag("pui_theme_style_${theme.name.lowercase()}")
                    )
                }
            }
        }

        // Quick Presets
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Quick Presets", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = config == PlayerUiConfig(),
                    onClick = { onConfigChange(PlayerUiConfig()) },
                    label = { Text("Cinema Default") },
                    modifier = Modifier.testTag("pui_preset_default")
                )
                FilterChip(
                    selected = config.themeStyle == PlayerUiThemeStyle.FZ_UI,
                    onClick = { onConfigChange(config.copy(themeStyle = PlayerUiThemeStyle.FZ_UI)) },
                    label = { Text("FZ Ui (Pro Dark)") },
                    modifier = Modifier.testTag("pui_preset_fz_ui")
                )
                FilterChip(
                    selected = config.themeStyle == PlayerUiThemeStyle.FAZA_CLASSIC &&
                        config.barTheme == PlayerBarTheme.MINIMAL_CLEAR &&
                        config.controlStyle == PlayerControlStyle.MINIMAL &&
                        !config.showFavouriteButton && !config.showNowPlayingButton,
                    onClick = {
                        onConfigChange(
                            config.copy(
                                themeStyle = PlayerUiThemeStyle.FAZA_CLASSIC,
                                barTheme = PlayerBarTheme.MINIMAL_CLEAR,
                                controlStyle = PlayerControlStyle.MINIMAL,
                                seekBarStyle = SeekBarStyle.MINIMAL_LINE,
                                hudOpacity = 0.72f,
                                showResolutionBadge = false,
                                showFavouriteButton = false,
                                showNowPlayingButton = false,
                                showDecoderButton = false
                            )
                        )
                    },
                    label = { Text("Minimalist") },
                    modifier = Modifier.testTag("pui_preset_minimal")
                )
                FilterChip(
                    selected = config.themeStyle == PlayerUiThemeStyle.FAZA_CLASSIC &&
                        config.barTheme == PlayerBarTheme.AMOLED_BLACK &&
                        config.seekBarStyle == SeekBarStyle.THICK_BAR &&
                        config.controlSize == PlayerControlSize.LARGE,
                    onClick = {
                        onConfigChange(
                            config.copy(
                                themeStyle = PlayerUiThemeStyle.FAZA_CLASSIC,
                                accentPreset = PlayerAccentPreset.CYAN,
                                barTheme = PlayerBarTheme.AMOLED_BLACK,
                                controlStyle = PlayerControlStyle.PILL,
                                controlSize = PlayerControlSize.LARGE,
                                seekBarStyle = SeekBarStyle.THICK_BAR,
                                hudOpacity = 0.95f,
                                showCenterSkipButtons = true,
                                showResolutionBadge = true,
                                showFavouriteButton = true,
                                showPipButton = true,
                                showRotateButton = true,
                                showLockButton = true,
                                showDecoderButton = true,
                                showAspectButton = true,
                                showTracksButton = true,
                                showNowPlayingButton = true,
                                showPrevNextButtons = true
                            )
                        )
                    },
                    label = { Text("Pro Theater") },
                    modifier = Modifier.testTag("pui_preset_pro")
                )
            }
        }

        // Accent Color
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Accent Color", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerAccentPreset.entries.forEach { preset ->
                    val swatchColor = resolvePlayerAccent(preset)
                    FilterChip(
                        selected = config.accentPreset == preset,
                        onClick = { onConfigChange(config.copy(accentPreset = preset)) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(0.75.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        },
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("pui_accent_${preset.name.lowercase()}")
                    )
                }
            }
        }

        // Bar Backdrop Theme
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Overlay & Bar Theme", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerBarTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = config.barTheme == theme,
                        onClick = { onConfigChange(config.copy(barTheme = theme)) },
                        label = { Text(theme.label) },
                        modifier = Modifier.testTag("pui_bar_${theme.name.lowercase()}")
                    )
                }
            }
        }

        // Button Style
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Button Shape & Style", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerControlStyle.entries.forEach { style ->
                    FilterChip(
                        selected = config.controlStyle == style,
                        onClick = { onConfigChange(config.copy(controlStyle = style)) },
                        label = { Text(style.label) },
                        modifier = Modifier.testTag("pui_ctrl_style_${style.name.lowercase()}")
                    )
                }
            }
        }

        // Control Size
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Controls Size", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerControlSize.entries.forEach { sz ->
                    FilterChip(
                        selected = config.controlSize == sz,
                        onClick = { onConfigChange(config.copy(controlSize = sz)) },
                        label = { Text(sz.label) },
                        modifier = Modifier.testTag("pui_size_${sz.name.lowercase()}")
                    )
                }
            }
        }

        // Seek Bar Style
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Seek Bar Style", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SeekBarStyle.entries.forEach { style ->
                    FilterChip(
                        selected = config.seekBarStyle == style,
                        onClick = { onConfigChange(config.copy(seekBarStyle = style)) },
                        label = { Text(style.label) },
                        modifier = Modifier.testTag("pui_seekbar_${style.name.lowercase()}")
                    )
                }
            }
        }

        // Opacity Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Controls Opacity", style = MaterialTheme.typography.titleSmall)
                Text("${(config.hudOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = config.hudOpacity,
                onValueChange = { onConfigChange(config.copy(hudOpacity = it)) },
                valueRange = 0.40f..1.0f,
                modifier = Modifier.testTag("pui_opacity_slider")
            )
        }

        // Auto-hide Delay
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Controls Auto-Hide Delay", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                autoHideOptions.forEach { sec ->
                    FilterChip(
                        selected = config.autoHideTimeoutSec == sec,
                        onClick = { onConfigChange(config.copy(autoHideTimeoutSec = sec)) },
                        label = { Text(if (sec == 0) "Never" else "${sec}s") },
                        modifier = Modifier.testTag("pui_autohide_$sec")
                    )
                }
            }
        }

        HorizontalDivider()

        // Toolbar Buttons Visibility
        Text("Visible Controls & Buttons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        PlayerToggleRow(
            title = "Center ±${config.seekStepSeconds}s Skip Buttons",
            subtitle = "Rewind & forward buttons beside Play/Pause",
            checked = config.showCenterSkipButtons,
            onCheckedChange = { onConfigChange(config.copy(showCenterSkipButtons = it)) },
            tag = "pui_toggle_center_skip"
        )
        PlayerToggleRow(
            title = "Resolution & Decoder Badge",
            subtitle = "4K/8K • AUTO/HW/SW badge in top bar",
            checked = config.showResolutionBadge,
            onCheckedChange = { onConfigChange(config.copy(showResolutionBadge = it)) }
        )
        PlayerToggleRow(
            title = "Favourite Heart Button",
            subtitle = "Quick bookmark in player top bar",
            checked = config.showFavouriteButton,
            onCheckedChange = { onConfigChange(config.copy(showFavouriteButton = it)) }
        )
        PlayerToggleRow(
            title = "Picture-in-Picture Button",
            subtitle = "Floating mini-window button in top bar",
            checked = config.showPipButton,
            onCheckedChange = { onConfigChange(config.copy(showPipButton = it)) }
        )
        PlayerToggleRow(
            title = "Screen Rotation Button",
            subtitle = "Manual portrait/landscape toggle in top bar",
            checked = config.showRotateButton,
            onCheckedChange = { onConfigChange(config.copy(showRotateButton = it)) }
        )
        PlayerToggleRow(
            title = "Screen Lock Button",
            subtitle = "Prevent accidental touches while watching",
            checked = config.showLockButton,
            onCheckedChange = { onConfigChange(config.copy(showLockButton = it)) }
        )
        PlayerToggleRow(
            title = "Previous / Next Video Buttons",
            subtitle = "Playlist track navigation in bottom bar",
            checked = config.showPrevNextButtons,
            onCheckedChange = { onConfigChange(config.copy(showPrevNextButtons = it)) }
        )
        PlayerToggleRow(
            title = "Decoder Quick Switcher",
            subtitle = "HW / SW / AUTO chip in bottom bar",
            checked = config.showDecoderButton,
            onCheckedChange = { onConfigChange(config.copy(showDecoderButton = it)) }
        )
        PlayerToggleRow(
            title = "Aspect Ratio Button",
            subtitle = "Fit / Fill / Zoom / 16:9 / 21:9 cycle chip",
            checked = config.showAspectButton,
            onCheckedChange = { onConfigChange(config.copy(showAspectButton = it)) }
        )
        PlayerToggleRow(
            title = "Audio & Subtitles Button",
            subtitle = "Track & caption picker in bottom bar",
            checked = config.showTracksButton,
            onCheckedChange = { onConfigChange(config.copy(showTracksButton = it)) }
        )
        PlayerToggleRow(
            title = "Now Playing Style Shortcut",
            subtitle = "Quick Now Playing customization icon in bottom bar",
            checked = config.showNowPlayingButton,
            onCheckedChange = { onConfigChange(config.copy(showNowPlayingButton = it)) }
        )

        HorizontalDivider()

        // Gestures & Seek Behavior
        Text("Gestures & Skip Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Double-Tap & Button Skip Interval", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                seekStepOptions.forEach { sec ->
                    FilterChip(
                        selected = config.seekStepSeconds == sec,
                        onClick = { onConfigChange(config.copy(seekStepSeconds = sec)) },
                        label = { Text("${sec}s") },
                        modifier = Modifier.testTag("pui_seek_step_$sec")
                    )
                }
            }
        }

        PlayerToggleRow(
            title = "Double-Tap Left/Right Skip",
            subtitle = "Double-tap sides to rewind or forward ${config.seekStepSeconds}s",
            checked = config.doubleTapSkipEnabled,
            onCheckedChange = { onConfigChange(config.copy(doubleTapSkipEnabled = it)) },
            tag = "pui_toggle_double_tap"
        )
        PlayerToggleRow(
            title = "Press & Hold Playback Speed",
            subtitle = "Hold to boost speed and slide left/right to lock speed",
            checked = config.holdSpeedEnabled,
            onCheckedChange = { onConfigChange(config.copy(holdSpeedEnabled = it)) },
            tag = "pui_toggle_hold_speed"
        )

        if (config.holdSpeedEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Default Hold Speed", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    holdSpeedOptions.forEach { spd ->
                        FilterChip(
                            selected = abs(config.holdDefaultSpeed - spd) < 0.05f,
                            onClick = { onConfigChange(config.copy(holdDefaultSpeed = spd)) },
                            label = { Text("${spd}x") },
                            modifier = Modifier.testTag("pui_hold_speed_${spd}")
                        )
                    }
                }
            }
        }

        PlayerToggleRow(
            title = "Brightness, Volume & Seek Swipes",
            subtitle = "Vertical side swipes and horizontal drag seeking",
            checked = config.swipeGesturesEnabled,
            onCheckedChange = { onConfigChange(config.copy(swipeGesturesEnabled = it)) }
        )
        PlayerToggleRow(
            title = "Pinch-to-Zoom & Pan",
            subtitle = "Two-finger zoom from 0.5x to 4.0x",
            checked = config.pinchZoomEnabled,
            onCheckedChange = { onConfigChange(config.copy(pinchZoomEnabled = it)) }
        )

        if (onOpenNowPlaying != null || onOpenRunningTime != null || onOpenAppIconOverlay != null || onOpenSubtitleStyle != null) {
            HorizontalDivider()
            Text("Overlay Studios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onOpenNowPlaying != null) {
                    OutlinedButton(onClick = onOpenNowPlaying) {
                        Icon(Icons.AutoMirrored.Filled.BrandingWatermark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Now Playing Bar")
                    }
                }
                if (onOpenRunningTime != null) {
                    OutlinedButton(onClick = onOpenRunningTime) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Running Time")
                    }
                }
                if (onOpenAppIconOverlay != null) {
                    OutlinedButton(onClick = onOpenAppIconOverlay) {
                        Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("App Icon")
                    }
                }
                if (onOpenSubtitleStyle != null) {
                    OutlinedButton(onClick = onOpenSubtitleStyle) {
                        Icon(Icons.Filled.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subtitles Style")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pui_reset_button")
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Player UI to Defaults")
        }
    }
}

@Composable
private fun PlayerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String? = null
) {
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
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerUiCustomizerBottomSheet(
    config: PlayerUiConfig,
    onConfigChange: (PlayerUiConfig) -> Unit,
    onReset: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenRunningTime: () -> Unit,
    onOpenAppIconOverlay: (() -> Unit)? = null,
    onOpenSubtitleStyle: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customize Player UI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            PlayerUiCustomizerEditor(
                config = config,
                onConfigChange = onConfigChange,
                onReset = onReset,
                onOpenNowPlaying = onOpenNowPlaying,
                onOpenRunningTime = onOpenRunningTime,
                onOpenAppIconOverlay = onOpenAppIconOverlay,
                onOpenSubtitleStyle = onOpenSubtitleStyle
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
