package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.ads.rememberIsInternetConnected
import com.example.ai.AssistantChatMode
import com.example.data.model.FolderSummary
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.data.model.VideoItem
import androidx.compose.ui.graphics.Color
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.FazaAdBannerCard
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaBrandLogo
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassIconBadge
import com.example.ui.components.FazaGlassPillBadge
import com.example.ui.components.ProfileAssistantPanel
import com.example.ui.components.RecentVideoChipCard
import com.example.ui.components.VideoListCard
import com.example.ui.components.isVideoEligibleForContinuePlaying
import com.example.ui.components.rememberFazaGlassBorderBrush
import com.example.ui.theme.LocalFazaIsDark
import com.example.ui.viewmodel.ProfileAssistantViewModel
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    videos: List<VideoItem>,
    folders: List<FolderSummary>,
    networkStreams: List<VideoItem>,
    preferences: UserPreferences,
    updateState: UpdateCheckState,
    unreadUpdateCount: Int,
    assistantViewModel: ProfileAssistantViewModel,
    isAssistantOpen: Boolean,
    onAssistantOpenChange: (Boolean) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavourite: (VideoItem) -> Unit,
    onTogglePrivate: (VideoItem) -> Unit,
    onRemoveVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpdates: () -> Unit,
    onLinkFolderClick: () -> Unit,
    bottomOverlayPadding: Dp = 0.dp,
    dockBottomPadding: Dp = bottomOverlayPadding,
    continueOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val favourites = remember(videos) { videos.filter { it.isFavourite } }
    val continueWatching = remember(videos) {
        videos.filter { isVideoEligibleForContinuePlaying(it) }
            .sortedByDescending { it.lastPlayedAt ?: 0L }
            .take(8)
    }
    val totalDurationMs = remember(videos) { videos.sumOf { it.durationMs.coerceAtLeast(0L) } }
    val isInternetConnected = rememberIsInternetConnected()

    val assistantMessages by assistantViewModel.messages.collectAsStateWithLifecycle()
    val assistantUiState by assistantViewModel.uiState.collectAsStateWithLifecycle()
    val assistantMode by assistantViewModel.selectedMode.collectAsStateWithLifecycle()
    val searchGroundingEnabled by assistantViewModel.searchGroundingEnabled.collectAsStateWithLifecycle()
    val lastFailedPrompt by assistantViewModel.lastFailedPrompt.collectAsStateWithLifecycle()

    BackHandler(enabled = isAssistantOpen) {
        onAssistantOpenChange(false)
    }

    FazaAmbientBackdrop(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        FazaBrandLogo(subtitle = if (isAssistantOpen) "AI Assistant" else "Profile")
                    }
                )
            }
        ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val containerWidth = maxWidth
            val adaptiveSpec = calculateAdaptiveLayoutSpec(maxWidth, maxHeight)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .widthIn(max = adaptiveSpec.maxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = adaptiveSpec.horizontalPadding, vertical = 6.dp)
                ) {
                    SegmentedButton(
                        selected = !isAssistantOpen,
                        onClick = { onAssistantOpenChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("profile_tab_overview")
                    ) {
                        Text("Overview", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = isAssistantOpen,
                        onClick = { onAssistantOpenChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("profile_tab_assistant")
                    ) {
                        Text(
                            text = if (assistantMessages.isNotEmpty()) {
                                "AI Chat (${assistantMessages.size})"
                            } else {
                                "AI Chat"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AnimatedContent(
                    targetState = isAssistantOpen,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    },
                    label = "profile_subtab_transition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { showAssistant ->
                    if (showAssistant) {
                        ProfileAssistantPanel(
                            messages = assistantMessages,
                            uiState = assistantUiState,
                            selectedMode = assistantMode,
                            searchGroundingEnabled = searchGroundingEnabled,
                            canRetry = lastFailedPrompt != null,
                            onSelectMode = { assistantViewModel.selectMode(it) },
                            onToggleSearchGrounding = { assistantViewModel.setSearchGroundingEnabled(it) },
                            onSendMessage = { assistantViewModel.sendMessage(it) },
                            onStopGenerating = { assistantViewModel.stopGenerating() },
                            onRetryLast = { assistantViewModel.retryLastFailed() },
                            onClearConversation = { assistantViewModel.clearConversation() },
                            bottomOverlayPadding = bottomOverlayPadding
                        )
                    } else {
                        val profileGridBottomPadding = if (isInternetConnected) {
                            12.dp + continueOverlayPadding
                        } else {
                            24.dp + bottomOverlayPadding + navBarBottom
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(adaptiveSpec.videoGridColumns),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = adaptiveSpec.maxContentWidth)
                                        .testTag("profile_screen_list"),
                                    contentPadding = PaddingValues(
                                        start = adaptiveSpec.horizontalPadding,
                                        end = adaptiveSpec.horizontalPadding,
                                        top = if (adaptiveSpec.isShortHeight) 4.dp else 8.dp,
                                        bottom = profileGridBottomPadding
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                                    verticalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                                ) {
                                if (adaptiveSpec.useTwoPane) {
                                    item(
                                        key = "profile_hero_and_ai_row",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            ProfileHeroCard(
                                                videosCount = videos.size,
                                                foldersCount = folders.size,
                                                favouritesCount = favourites.size,
                                                totalDurationMs = totalDurationMs,
                                                preferences = preferences,
                                                useTwoByTwoStats = containerWidth < 900.dp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ProfileAiLauncherCard(
                                                assistantMessagesCount = assistantMessages.size,
                                                onOpenAssistant = { onAssistantOpenChange(true) },
                                                onSendTrendingPrompt = {
                                                    assistantViewModel.selectMode(AssistantChatMode.GENERAL_SEARCH)
                                                    onAssistantOpenChange(true)
                                                    assistantViewModel.sendMessage("Search for trending 4K HDR movies and series released recently.")
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                } else {
                                    item(key = "profile_hero", span = { GridItemSpan(maxLineSpan) }) {
                                        ProfileHeroCard(
                                            videosCount = videos.size,
                                            foldersCount = folders.size,
                                            favouritesCount = favourites.size,
                                            totalDurationMs = totalDurationMs,
                                            preferences = preferences,
                                            useTwoByTwoStats = adaptiveSpec.isUltraCompactWidth,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    item(
                                        key = "profile_ai_assistant_card",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ProfileAiLauncherCard(
                                            assistantMessagesCount = assistantMessages.size,
                                            onOpenAssistant = { onAssistantOpenChange(true) },
                                            onSendTrendingPrompt = {
                                                assistantViewModel.selectMode(AssistantChatMode.GENERAL_SEARCH)
                                                onAssistantOpenChange(true)
                                                assistantViewModel.sendMessage("Search for trending 4K HDR movies and series released recently.")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Quick Navigation & Shortcuts
                                item(key = "profile_quick_actions", span = { GridItemSpan(maxLineSpan) }) {
                                    val updatesSubtitle = when {
                                        unreadUpdateCount > 0 -> "New release ready"
                                        updateState is UpdateCheckState.UpdateAvailable -> "v${updateState.manifest.latestVersion}"
                                        else -> "Check versions"
                                    }
                                    val highlightUpdates = unreadUpdateCount > 0 || updateState is UpdateCheckState.UpdateAvailable

                                    if (containerWidth < 310.dp) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ProfileShortcutCard(
                                                icon = Icons.Filled.SystemUpdate,
                                                title = "Updates",
                                                subtitle = updatesSubtitle,
                                                highlight = highlightUpdates,
                                                onClick = onOpenUpdates,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("profile_updates_shortcut")
                                            )
                                            ProfileShortcutCard(
                                                icon = Icons.Filled.Settings,
                                                title = "Settings",
                                                subtitle = "Player & display",
                                                highlight = false,
                                                onClick = onOpenSettings,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("profile_settings_shortcut")
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                                        ) {
                                            ProfileShortcutCard(
                                                icon = Icons.Filled.SystemUpdate,
                                                title = "Updates",
                                                subtitle = updatesSubtitle,
                                                highlight = highlightUpdates,
                                                onClick = onOpenUpdates,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("profile_updates_shortcut")
                                            )
                                            ProfileShortcutCard(
                                                icon = Icons.Filled.Settings,
                                                title = "Settings",
                                                subtitle = "Player & display",
                                                highlight = false,
                                                onClick = onOpenSettings,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("profile_settings_shortcut")
                                            )
                                        }
                                    }
                                }

                                if (continueWatching.isNotEmpty()) {
                                    item(
                                        key = "continue_watching_header",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ProfileSectionHeader(
                                            icon = Icons.Filled.PlayArrow,
                                            title = "Continue Watching",
                                            countLabel = "${continueWatching.size}"
                                        )
                                    }
                                    item(
                                        key = "continue_watching_row",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            items(continueWatching, key = { it.id }) { item ->
                                                RecentVideoChipCard(
                                                    video = item,
                                                    onClick = { onPlayVideo(item) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (favourites.isNotEmpty()) {
                                    item(
                                        key = "favourites_header",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ProfileSectionHeader(
                                            icon = Icons.Filled.Favorite,
                                            title = "Favourite Videos",
                                            countLabel = "${favourites.size}"
                                        )
                                    }
                                    items(favourites, key = { "fav_${it.id}" }) { video ->
                                        VideoListCard(
                                            video = video,
                                            onPlayClick = { onPlayVideo(video) },
                                            onToggleFavourite = { onToggleFavourite(video) },
                                            onTogglePrivate = { onTogglePrivate(video) },
                                            onRemove = { onRemoveVideo(video.id) }
                                        )
                                    }
                                }

                                if (continueWatching.isEmpty() && favourites.isEmpty()) {
                                    item(
                                        key = "profile_empty_activity",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        Card(
                                            shape = MaterialTheme.shapes.large,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(if (adaptiveSpec.isShortHeight) 14.dp else 20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.VideoLibrary,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Text(
                                                    text = "Your watch history & favourites appear here",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = if (networkStreams.isNotEmpty()) {
                                                        "You have ${networkStreams.size} saved stream${if (networkStreams.size == 1) "" else "s"} ready to play."
                                                    } else {
                                                        "Link a device folder or favourite a video to access it right from your profile."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                FilledTonalButton(
                                                    onClick = onLinkFolderClick,
                                                    modifier = Modifier.height(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CreateNewFolder,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Link Video Folder")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Library Summary Footer
                                item(key = "profile_footer", span = { GridItemSpan(maxLineSpan) }) {
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = onOpenSettings)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${stringResource(R.string.developer_name)} · ${stringResource(R.string.company_name)}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${stringResource(R.string.developer_email)} · ${networkStreams.size} streams saved",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            }

                            if (isInternetConnected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 560.dp)
                                        .padding(
                                            start = adaptiveSpec.horizontalPadding,
                                            end = adaptiveSpec.horizontalPadding,
                                            top = 6.dp,
                                            bottom = dockBottomPadding + navBarBottom + 8.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FazaAdBannerCard(
                                        testTag = "profile_ad_banner",
                                        dockedBottom = true
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

@Composable
private fun ProfileHeroCard(
    videosCount: Int,
    foldersCount: Int,
    favouritesCount: Int,
    totalDurationMs: Long,
    preferences: UserPreferences,
    useTwoByTwoStats: Boolean,
    modifier: Modifier = Modifier
) {
    val runtimeLabel = if (totalDurationMs > 0L) Formatters.formatDuration(totalDurationMs) else "0:00"
    val colors = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(28.dp)

    Surface(
        shape = cardShape,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .clip(cardShape)
            .testTag("profile_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (useTwoByTwoStats) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (useTwoByTwoStats) 12.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (useTwoByTwoStats) 10.dp else 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (useTwoByTwoStats) 50.dp else 60.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User Profile",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(if (useTwoByTwoStats) 26.dp else 30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Personal Cinema",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stringResource(R.string.app_name)} · v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Decoder: ${preferences.decoderMode.badge} · ${preferences.themeMode.label} Theme",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = colors.surfaceContainerLowest,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.faza_icon_fg),
                        contentDescription = "FAZA Player",
                        modifier = Modifier
                            .padding(6.dp)
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            if (useTwoByTwoStats) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatPill(
                            value = videosCount.toString(),
                            label = "Videos",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatPill(
                            value = foldersCount.toString(),
                            label = "Folders",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatPill(
                            value = favouritesCount.toString(),
                            label = "Favourites",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatPill(
                            value = runtimeLabel,
                            label = "Runtime",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileStatPill(
                        value = videosCount.toString(),
                        label = "Videos",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatPill(
                        value = foldersCount.toString(),
                        label = "Folders",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatPill(
                        value = favouritesCount.toString(),
                        label = "Favourites",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatPill(
                        value = runtimeLabel,
                        label = "Runtime",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileAiLauncherCard(
    assistantMessagesCount: Int,
    onOpenAssistant: () -> Unit,
    onSendTrendingPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    FazaGlassCard(
        shape = RoundedCornerShape(24.dp),
        accented = true,
        onClick = onOpenAssistant,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.testTag("profile_ai_launcher_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FazaGlassIconBadge(
                    icon = Icons.Filled.AutoAwesome,
                    size = 46.dp,
                    iconSize = 22.dp,
                    cornerRadius = 15.dp,
                    useSolidGradient = true
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FAZA AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Multi-turn Gemini chat with live Google Search grounding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open AI Assistant",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onSendTrendingPrompt)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Trending movies",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenAssistant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (assistantMessagesCount > 0) "Continue chat" else "Start chatting",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerLowest.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FazaGlassCard(
        shape = RoundedCornerShape(24.dp),
        accented = highlight,
        highlightedBorder = false,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FazaGlassIconBadge(
                icon = icon,
                size = 42.dp,
                iconSize = 20.dp,
                cornerRadius = 21.dp,
                useSolidGradient = highlight,
                useBlueHighlight = !highlight
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlight) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.80f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(
    icon: ImageVector,
    title: String,
    countLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        FazaGlassPillBadge(text = countLabel, useBlueAccent = true)
    }
}
