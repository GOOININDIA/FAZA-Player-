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
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FazaGlassPillBadge(
                    text = "${SettingsFolder.entries.size} folders",
                    useBlueAccent = true
                )
            }
        }

        // Ordered Modern Folder Cards
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Quick Hardware Acceleration Card
        FazaGlassCard(
            shape = RoundedCornerShape(28.dp),
            accented = hwEnabled,
            highlightedBorder = false,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
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
                    size = 46.dp,
                    iconSize = 22.dp,
                    cornerRadius = 23.dp,
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
                            fontWeight = FontWeight.SemiBold,
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
                            "GPU/MediaCodec decoding active"
                        } else {
                            "Software CPU decoding active"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hwEnabled) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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

        // Compact Quick Theme Mode & Color Bar
        FazaGlassCard(
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onCycleThemeMode)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FazaGlassIconBadge(
                        icon = themeIcon,
                        size = 40.dp,
                        iconSize = 20.dp,
                        cornerRadius = 20.dp,
                        useBlueHighlight = true
                    )
                    Column {
                        Text(
                            text = "Quick Theme · ${preferences.themeMode.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap to cycle System / Light / Dark",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = onOpenThemeFolder,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(activeSeedColor)
                                .border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f), CircleShape)
                        )
                        Text(
                            text = if (preferences.appColorSource == AppColorSource.WALLPAPER) {
                                "Wallpaper"
                            } else {
                                "Custom"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
    val useBlueAccent = folder == SettingsFolder.THEME ||
        folder == SettingsFolder.ADVANCED ||
        folder == SettingsFolder.ABOUT

    FazaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_folder_${folder.tagSuffix}"),
        shape = RoundedCornerShape(24.dp),
        accented = isSelected,
        highlightedBorder = false,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FazaGlassIconBadge(
                icon = folder.icon,
                size = 48.dp,
                iconSize = 23.dp,
                cornerRadius = 24.dp,
                useBlueHighlight = useBlueAccent && !isSelected,
                useSolidGradient = isSelected
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = folder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FazaGlassPillBadge(
                        text = badge,
                        useBlueAccent = useBlueAccent
                    )
                    if (showAlertDot) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
                Text(
                    text = folder.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (useBlueAccent) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open ${folder.title}",
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
