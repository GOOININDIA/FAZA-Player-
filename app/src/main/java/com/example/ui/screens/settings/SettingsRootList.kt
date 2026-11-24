package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppColorSource
import com.example.data.model.ThemeMode
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassIconBadge
import com.example.ui.components.FazaGlassPillBadge

@Composable
internal fun SettingsRootList(
    preferences: UserPreferences,
    updateState: UpdateCheckState,
    selectedFolder: SettingsFolder?,
    bottomOverlayPadding: Dp = 0.dp,
    onCycleThemeMode: () -> Unit,
    onToggleHardwareAcceleration: (Boolean) -> Unit,
    onSelectFolder: (SettingsFolder, DisplaySubTab?) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("settings_folder_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 20.dp + bottomOverlayPadding + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Controls Strip
        item(key = "settings_quick_controls_strip") {
            QuickControlsBentoRow(
                preferences = preferences,
                onCycleThemeMode = onCycleThemeMode,
                onOpenThemeFolder = { onSelectFolder(SettingsFolder.THEME, null) },
                onToggleHardwareAcceleration = onToggleHardwareAcceleration
            )
        }

        item(key = "settings_folders_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${SettingsFolder.entries.size} sections",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Ordered Minimalist Settings Category Cards
        items(SettingsFolder.entries, key = { it.name }) { folder ->
            val isSelected = selectedFolder == folder
            val summary = buildFolderSummary(folder, preferences, updateState)
            val badge = buildFolderBadge(folder, preferences, updateState)
            val showUpdateDot = folder == SettingsFolder.ABOUT &&
                updateState is UpdateCheckState.UpdateAvailable

            ModernSettingsFolderCard(
                folder = folder,
                summary = summary,
                badge = badge,
                isSelected = isSelected,
                showAlertDot = showUpdateDot,
                onClick = { onSelectFolder(folder, null) }
            )
        }
    }
}

@Composable
private fun QuickControlsBentoRow(
    preferences: UserPreferences,
    onCycleThemeMode: () -> Unit,
    onOpenThemeFolder: () -> Unit,
    onToggleHardwareAcceleration: (Boolean) -> Unit
) {
    val hwEnabled = preferences.isHardwareAccelerationEnabled
    val themeIcon = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Filled.LightMode
        ThemeMode.DARK -> Icons.Filled.DarkMode
    }
    val activeSeedColor = if (preferences.appColorSource == AppColorSource.CUSTOM) {
        Color(preferences.customThemeColorHex)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Minimalist Hardware Acceleration Row
        FazaGlassCard(
            shape = RoundedCornerShape(16.dp),
            accented = false,
            highlightedBorder = false,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_quick_hardware_acceleration_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FazaGlassIconBadge(
                    icon = Icons.Outlined.Memory,
                    size = 40.dp,
                    iconSize = 20.dp,
                    cornerRadius = 11.dp,
                    useSolidGradient = hwEnabled
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Hardware Acceleration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FazaGlassPillBadge(
                            text = if (hwEnabled) preferences.decoderMode.badge else "SW",
                            useBlueAccent = !hwEnabled
                        )
                    }
                    Text(
                        text = if (hwEnabled) {
                            "GPU/MediaCodec active"
                        } else {
                            "Software CPU decoding"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = hwEnabled,
                    onCheckedChange = onToggleHardwareAcceleration,
                    modifier = Modifier.testTag("settings_quick_hardware_acceleration_switch")
                )
            }
        }

        // Compact Minimalist Quick Theme Bar
        FazaGlassCard(
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_quick_theme_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCycleThemeMode)
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FazaGlassIconBadge(
                        icon = themeIcon,
                        size = 38.dp,
                        iconSize = 19.dp,
                        cornerRadius = 10.dp,
                        useBlueHighlight = true
                    )
                    Column {
                        Text(
                            text = "Theme · ${preferences.themeMode.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap to switch System / Light / Dark",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = onOpenThemeFolder,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(activeSeedColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Text(
                            text = if (preferences.appColorSource == AppColorSource.WALLPAPER) {
                                "Wallpaper"
                            } else {
                                "Custom"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSettingsFolderCard(
    folder: SettingsFolder,
    summary: String,
    badge: String,
    isSelected: Boolean,
    showAlertDot: Boolean,
    onClick: () -> Unit
) {
    FazaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_folder_${folder.tagSuffix}"),
        shape = RoundedCornerShape(16.dp),
        accented = isSelected,
        highlightedBorder = isSelected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FazaGlassIconBadge(
                icon = folder.icon,
                size = 40.dp,
                iconSize = 20.dp,
                cornerRadius = 11.dp,
                useBlueHighlight = !isSelected,
                useSolidGradient = isSelected
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = folder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FazaGlassPillBadge(
                        text = badge,
                        useBlueAccent = !isSelected
                    )
                    if (showAlertDot) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${folder.title}",
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
