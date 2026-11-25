package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppIconOverlayConfig
import com.example.data.model.NowPlayingConfig
import com.example.data.model.PlayerUiConfig
import com.example.data.model.RunningTimeConfig
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.UserPreferences
import com.example.ui.components.AppIconOverlayStyleEditor
import com.example.ui.components.NowPlayingStyleEditor
import com.example.ui.components.PlayerUiCustomizerEditor
import com.example.ui.components.RunningTimeStyleEditor
import com.example.ui.components.SubtitleStyleEditor
import com.example.ui.components.rememberFazaBounceScale
import com.example.ui.theme.FazaNavActivePurpleDark
import com.example.ui.theme.FazaNavActivePurpleLight
import com.example.ui.theme.FazaNavBorderDark
import com.example.ui.theme.FazaNavBorderLight
import com.example.ui.theme.FazaNavInactiveDark
import com.example.ui.theme.FazaNavInactiveLight
import com.example.ui.theme.FazaNavOnActivePurple
import com.example.ui.theme.FazaNavSurfaceDark
import com.example.ui.theme.FazaNavSurfaceLight
import com.example.ui.theme.LocalFazaIsDark
import kotlin.math.roundToInt

@Composable
internal fun SettingsDisplayFolderContent(
    preferences: UserPreferences,
    activeSubTab: DisplaySubTab,
    onSelectSubTab: (DisplaySubTab) -> Unit,
    onUpdatePlayerUiConfig: (PlayerUiConfig) -> Unit,
    onResetPlayerUiConfig: () -> Unit,
    onUpdateNowPlayingConfig: (NowPlayingConfig) -> Unit,
    onUpdateRunningTimeConfig: (RunningTimeConfig) -> Unit,
    onUpdateAppIconOverlayConfig: (AppIconOverlayConfig) -> Unit = {},
    onUpdateSubtitleStyle: (SubtitleStyleConfig) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Modern Sub-Navigation Grid for All Player UI Settings (all 4 visible at a glance)
        item(key = "display_subtab_strip") {
            DisplayCategoryPillStrip(
                preferences = preferences,
                activeSubTab = activeSubTab,
                onSelectSubTab = onSelectSubTab
            )
        }

        // Active Player Display Section
        item(key = "display_subtab_body") {
            AnimatedContent(
                targetState = activeSubTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(130))
                },
                label = "display_subtab_content"
            ) { tab ->
                when (tab) {
                    DisplaySubTab.PLAYER_UI -> {
                        SectionCard(
                            title = "Player UI & Controls",
                            subtitle = tab.description
                        ) {
                            PlayerUiCustomizerEditor(
                                config = preferences.playerUiConfig,
                                onConfigChange = onUpdatePlayerUiConfig,
                                onReset = onResetPlayerUiConfig,
                                onOpenNowPlaying = { onSelectSubTab(DisplaySubTab.NOW_PLAYING) },
                                onOpenRunningTime = { onSelectSubTab(DisplaySubTab.RUNNING_TIME) },
                                onOpenAppIconOverlay = { onSelectSubTab(DisplaySubTab.APP_ICON) },
                                onOpenSubtitleStyle = { onSelectSubTab(DisplaySubTab.SUBTITLES) }
                            )
                        }
                    }

                    DisplaySubTab.NOW_PLAYING -> {
                        SectionCard(
                            title = "Permanent Now Playing",
                            subtitle = tab.description
                        ) {
                            NowPlayingStyleEditor(
                                config = preferences.nowPlayingConfig,
                                onConfigChange = onUpdateNowPlayingConfig
                            )
                        }
                    }

                    DisplaySubTab.RUNNING_TIME -> {
                        SectionCard(
                            title = "Running Time",
                            subtitle = tab.description
                        ) {
                            RunningTimeStyleEditor(
                                config = preferences.runningTimeConfig,
                                onConfigChange = onUpdateRunningTimeConfig
                            )
                        }
                    }

                    DisplaySubTab.APP_ICON -> {
                        SectionCard(
                            title = "Permanent App Icon",
                            subtitle = tab.description
                        ) {
                            AppIconOverlayStyleEditor(
                                config = preferences.appIconOverlayConfig,
                                onConfigChange = onUpdateAppIconOverlayConfig
                            )
                        }
                    }

                    DisplaySubTab.SUBTITLES -> {
                        SectionCard(
                            title = "Subtitles & Captions",
                            subtitle = tab.description
                        ) {
                            SubtitleStyleEditor(
                                style = preferences.subtitleStyle,
                                onStyleChange = onUpdateSubtitleStyle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayCategoryPillStrip(
    preferences: UserPreferences,
    activeSubTab: DisplaySubTab,
    onSelectSubTab: (DisplaySubTab) -> Unit
) {
    val isDark = LocalFazaIsDark.current
    val activePurple = if (isDark) FazaNavActivePurpleDark else FazaNavActivePurpleLight
    val surfaceColor = if (isDark) FazaNavSurfaceDark else FazaNavSurfaceLight
    val borderColor = if (isDark) FazaNavBorderDark else FazaNavBorderLight
    val inactiveGrey = if (isDark) FazaNavInactiveDark else FazaNavInactiveLight

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("display_subtab_strip")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player Display Studio",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = activePurple
                )
                Text(
                    text = "All Player UI settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = inactiveGrey
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 600.dp) 4 else 2
                val rows = DisplaySubTab.entries.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowTabs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTabs.forEach { tab ->
                                val selected = activeSubTab == tab
                                val interactionSource = remember { MutableInteractionSource() }
                                val bounceScale = rememberFazaBounceScale(
                                    interactionSource = interactionSource,
                                    pressedScale = 0.94f
                                )
                                val statusTag = when (tab) {
                                    DisplaySubTab.PLAYER_UI -> preferences.playerUiConfig.themeStyle.label
                                    DisplaySubTab.NOW_PLAYING -> if (preferences.nowPlayingConfig.enabled) "On" else "Off"
                                    DisplaySubTab.RUNNING_TIME -> if (preferences.runningTimeConfig.enabled) "On" else "Off"
                                    DisplaySubTab.APP_ICON -> if (preferences.appIconOverlayConfig.enabled) "On" else "Off"
                                    DisplaySubTab.SUBTITLES -> if (preferences.subtitleStyle.enabled) {
                                        "${preferences.subtitleStyle.fontSizeSp.roundToInt()}sp"
                                    } else {
                                        "Off"
                                    }
                                }

                                Surface(
                                    onClick = { onSelectSubTab(tab) },
                                    interactionSource = interactionSource,
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selected) activePurple else surfaceColor,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) activePurple else borderColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = bounceScale
                                            scaleY = bounceScale
                                        }
                                        .minimumInteractiveComponentSize()
                                        .testTag("display_tab_${tab.tagSuffix}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            tint = if (selected) FazaNavOnActivePurple else inactiveGrey,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) {
                                                    FazaNavOnActivePurple
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = statusTag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) {
                                                    FazaNavOnActivePurple.copy(alpha = 0.85f)
                                                } else {
                                                    inactiveGrey
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
                }
            }
        }
    }
}
