package com.example.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppColorSource
import com.example.data.model.AppCustomColorPreset
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import com.example.ui.components.rememberFazaBounceScale
import com.example.ui.theme.formatColorHexLabel
import com.example.ui.theme.hslToRgbHex
import com.example.ui.theme.rgbHexToHsl
import kotlin.math.roundToInt

@Composable
internal fun SettingsThemeFolderContent(
    preferences: UserPreferences,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAppColorSource: (AppColorSource) -> Unit,
    onSetCustomThemeColorHex: (Long) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("settings_detail_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 20.dp + bottomOverlayPadding + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Index 0: Live Theme Preview + Theme Brightness Selector
        item(key = "theme_brightness_section") {
            SectionCard(
                title = "Theme Brightness",
                subtitle = "Switch between System adaptive, crisp Light, or deep Dark"
            ) {
                ThemeLiveMiniPreview(preferences = preferences)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = preferences.themeMode == mode
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Filled.LightMode
                            ThemeMode.DARK -> Icons.Filled.DarkMode
                        }
                        val interactionSource = remember { MutableInteractionSource() }
                        val bounceScale = rememberFazaBounceScale(
                            interactionSource = interactionSource,
                            pressedScale = 0.93f
                        )
                        Surface(
                            onClick = { onSetThemeMode(mode) },
                            interactionSource = interactionSource,
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = bounceScale
                                    scaleY = bounceScale
                                }
                                .minimumInteractiveComponentSize()
                                .testTag("theme_chip_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Index 1: Application Colour (Wallpaper Material You vs Custom Swatches)
        item(key = "app_colour_section") {
            SectionCard(
                title = "Application Colour",
                subtitle = "Colors buttons, highlights, navigation dock & cards across FAZA Player"
            ) {
                AppColorChoiceCard(
                    title = "1. Wallpaper Colour",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Adapts Material You colors automatically from your device wallpaper"
                    } else {
                        "Uses adaptive default theme palette on Android versions below 12"
                    },
                    icon = Icons.Outlined.Wallpaper,
                    swatchColor = null,
                    selected = preferences.appColorSource == AppColorSource.WALLPAPER,
                    onClick = { onSetAppColorSource(AppColorSource.WALLPAPER) },
                    modifier = Modifier.testTag("app_color_source_wallpaper")
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppColorChoiceCard(
                    title = "2. Custom Colour",
                    subtitle = "Pick from curated palettes or craft any custom shade below",
                    icon = Icons.Outlined.ColorLens,
                    swatchColor = Color(preferences.customThemeColorHex),
                    selected = preferences.appColorSource == AppColorSource.CUSTOM,
                    onClick = { onSetAppColorSource(AppColorSource.CUSTOM) },
                    modifier = Modifier.testTag("app_color_source_custom")
                )

                AnimatedVisibility(visible = preferences.appColorSource == AppColorSource.CUSTOM) {
                    CustomAppColorPickerSection(
                        selectedColorHex = preferences.customThemeColorHex,
                        onSelectColorHex = onSetCustomThemeColorHex,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeLiveMiniPreview(preferences: UserPreferences) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = colors.primary,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Live Theme Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "${preferences.themeMode.label} · ${preferences.appColorSource.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(colors.primary, colors.secondary, colors.tertiary).forEach { dotColor ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
            Surface(
                color = colors.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = "4K HDR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AppColorChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    swatchColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        pressedScale = 0.96f
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            colors.primaryContainer
        } else {
            colors.surfaceContainerHigh
        },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = swatchColor ?: if (selected) colors.primary else colors.primaryContainer,
                border = if (swatchColor != null) {
                    BorderStroke(1.dp, colors.outline.copy(alpha = 0.28f))
                } else null,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (swatchColor != null) {
                            if (swatchColor.luminance() > 0.45f) Color(0xFF111827) else Color.White
                        } else if (selected) {
                            colors.onPrimary
                        } else {
                            colors.onPrimaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        colors.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        colors.onSurfaceVariant
                    }
                )
            }

            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CustomAppColorPickerSection(
    selectedColorHex: Long,
    onSelectColorHex: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val (initHue, initSat, _) = remember(selectedColorHex) {
        rgbHexToHsl(selectedColorHex)
    }
    var hue by remember { mutableFloatStateOf(initHue) }
    var saturation by remember { mutableFloatStateOf(initSat.coerceIn(0.28f, 0.95f)) }

    LaunchedEffect(selectedColorHex) {
        val (h, s, _) = rgbHexToHsl(selectedColorHex)
        hue = h
        saturation = s.coerceIn(0.28f, 0.95f)
    }

    val livePreviewHex = hslToRgbHex(hue, saturation, 0.46f)
    val activePreset = AppCustomColorPreset.entries.find { it.seedHex == selectedColorHex }
    val spectrumColors = remember {
        listOf(
            Color(0xFFEF4444),
            Color(0xFFF59E0B),
            Color(0xFFEAB308),
            Color(0xFF22C55E),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFFEF4444)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_color_picker_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(livePreviewHex))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePreset?.label ?: "Custom Shade",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatColorHexLabel(livePreviewHex),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Active Theme",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Text(
            text = "Preset Swatches",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppCustomColorPreset.entries.forEach { preset ->
                val isSelected = selectedColorHex == preset.seedHex
                val swatchColor = Color(preset.seedHex)
                IconButton(
                    onClick = { onSelectColorHex(preset.seedHex) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("custom_color_preset_${preset.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "${preset.label} selected",
                                tint = if (swatchColor.luminance() > 0.45f) Color(0xFF111827) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Hue",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${hue.roundToInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(spectrumColors))
                )
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    onValueChangeFinished = {
                        onSelectColorHex(hslToRgbHex(hue, saturation, 0.46f))
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(livePreviewHex),
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_color_hue_slider")
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vibrancy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(saturation * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = saturation,
                onValueChange = { saturation = it },
                onValueChangeFinished = {
                    onSelectColorHex(hslToRgbHex(hue, saturation, 0.46f))
                },
                valueRange = 0.28f..0.95f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(livePreviewHex),
                    activeTrackColor = Color(livePreviewHex)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_color_saturation_slider")
            )
        }
    }
}
