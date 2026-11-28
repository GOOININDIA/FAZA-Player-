package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.example.ads.AdFreeAccessPolicy
import com.example.ads.FreeAccessStatus
import com.example.ai.AssistantChatMode
import com.example.auth.FazaAuthState
import com.example.auth.FazaAuthUiError
import com.example.auth.FazaGoogleAccount
import com.example.auth.ProfileWelcomeGreeting
import com.example.auth.classifyAuthError
import com.example.data.model.FolderSummary
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.data.model.VideoItem
import androidx.compose.ui.graphics.Color
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.FazaAdBannerCard
import com.example.ui.components.FazaAiSignInGateCard
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaBrandLogo
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassIconBadge
import com.example.ui.components.FazaGlassPillBadge
import com.example.ui.components.FreeAccessSectionCard
import com.example.ui.components.GoogleAccountAvatar
import com.example.ui.components.ProfileAssistantPanel
import com.example.ui.components.ProfileErrorBoundaryBanner
import com.example.ui.components.ProfileErrorSnackbarVisuals
import com.example.ui.components.ProfileCompactHeaderGreeting
import com.example.ui.components.ProfileGoogleAccountCard
import com.example.ui.components.ProfileNotificationSnackbarHost
import com.example.ui.components.ProfileWelcomeGreetingHeader
import com.example.ui.components.RecentVideoChipCard
import com.example.ui.components.VideoListCard
import com.example.ui.components.isVideoEligibleForContinuePlaying
import com.example.ui.components.rememberFazaGlassBorderBrush
import com.example.ui.theme.FazaNavBorderDark
import com.example.ui.theme.FazaNavBorderLight
import com.example.ui.theme.FazaNavInactiveDark
import com.example.ui.theme.FazaNavInactiveLight
import com.example.ui.theme.FazaNavSurfaceDark
import com.example.ui.theme.FazaNavSurfaceLight
import com.example.ui.theme.LocalFazaIsDark
import com.example.ui.viewmodel.FreeAccessFeedback
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
    authenticatedAccount: FazaGoogleAccount? = null,
    authState: FazaAuthState = authenticatedAccount?.let { FazaAuthState.SignedIn(it) } ?: FazaAuthState.SignedOut,
    isAuthBusy: Boolean = false,
    authUiError: FazaAuthUiError? = null,
    authError: String? = null,
    welcomeGreeting: ProfileWelcomeGreeting? = null,
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onDismissAuthError: () -> Unit = {},
    onDismissWelcomeGreeting: () -> Unit = {},
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavourite: (VideoItem) -> Unit,
    onTogglePrivate: (VideoItem) -> Unit,
    onRemoveVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpdates: () -> Unit,
    onLinkFolderClick: () -> Unit,
    freeAccessStatus: FreeAccessStatus = AdFreeAccessPolicy.resolveStatus(
        watchedCount = preferences.rewardedAdsWatchedCount,
        expiresAtMs = preferences.freeAccessExpiresAtMs,
        nowMs = System.currentTimeMillis()
    ),
    isOnline: Boolean = false,
    isLoadingRewardedAd: Boolean = false,
    freeAccessFeedback: FreeAccessFeedback? = null,
    onWatchRewardedAd: () -> Unit = {},
    onDismissFreeAccessFeedback: () -> Unit = {},
    showBannerAd: Boolean = false,
    bottomOverlayPadding: Dp = 0.dp,
    dockBottomPadding: Dp = bottomOverlayPadding,
    continueOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val snackbarHostState = remember { SnackbarHostState() }
    val effectiveAccount = when (authState) {
        is FazaAuthState.SignedIn -> authState.account
        is FazaAuthState.SignedOut,
        is FazaAuthState.SessionExpired -> null
        is FazaAuthState.SigningIn -> authenticatedAccount
    }
    val effectiveAuthBusy = isAuthBusy || authState is FazaAuthState.SigningIn
    val resolvedAuthUiError = remember(authUiError, authError, authState) {
        authUiError ?: when {
            authState is FazaAuthState.SessionExpired ->
                classifyAuthError(
                    com.example.auth.AuthSessionExpiredException(authState.reason),
                    id = authState.reason.hashCode().toLong()
                )
            !authError.isNullOrBlank() ->
                classifyAuthError(IllegalStateException(authError), id = authError.hashCode().toLong())
            else -> null
        }
    }
    val snackbarBottomOffset = bottomOverlayPadding + if (showBannerAd && !isAssistantOpen) 68.dp else 0.dp

    LaunchedEffect(resolvedAuthUiError?.id) {
        val currentError = resolvedAuthUiError
        if (currentError == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            ProfileErrorSnackbarVisuals(
                uiError = currentError,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        )
        if (result == SnackbarResult.ActionPerformed && currentError.canRetry) {
            onDismissAuthError()
            onGoogleSignIn()
        }
    }

    val favourites = remember(videos) { videos.filter { it.isFavourite } }
    val continueWatching = remember(videos) {
        videos.filter { isVideoEligibleForContinuePlaying(it) }
            .sortedByDescending { it.lastPlayedAt ?: 0L }
            .take(8)
    }
    val totalDurationMs = remember(videos) { videos.sumOf { it.durationMs.coerceAtLeast(0L) } }

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
            snackbarHost = {
                ProfileNotificationSnackbarHost(
                    hostState = snackbarHostState,
                    bottomPadding = snackbarBottomOffset
                )
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        AnimatedContent(
                            targetState = if (isAssistantOpen && effectiveAccount != null) welcomeGreeting else null,
                            transitionSpec = {
                                (fadeIn(tween(240)) + slideInVertically(
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { -it / 2 }) togetherWith fadeOut(tween(180))
                            },
                            label = "profile_topbar_greeting_transition"
                        ) { compactGreeting ->
                            if (compactGreeting != null) {
                                ProfileCompactHeaderGreeting(
                                    greeting = compactGreeting,
                                    onDismiss = onDismissWelcomeGreeting
                                )
                            } else {
                                FazaBrandLogo(subtitle = if (isAssistantOpen) "AI Assistant" else "Profile")
                            }
                        }
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
                val isDark = LocalFazaIsDark.current
                val themeColors = MaterialTheme.colorScheme
                val segmentedColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = themeColors.primary,
                    activeContentColor = themeColors.onPrimary,
                    activeBorderColor = themeColors.primary,
                    inactiveContainerColor = if (isDark) FazaNavSurfaceDark else FazaNavSurfaceLight,
                    inactiveContentColor = if (isDark) FazaNavInactiveDark else FazaNavInactiveLight,
                    inactiveBorderColor = if (isDark) FazaNavBorderDark else FazaNavBorderLight
                )
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
                        colors = segmentedColors,
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
                        colors = segmentedColors,
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

                ProfileWelcomeGreetingHeader(
                    greeting = if (!isAssistantOpen && effectiveAccount != null) welcomeGreeting else null,
                    onDismiss = onDismissWelcomeGreeting,
                    modifier = Modifier
                        .widthIn(max = adaptiveSpec.maxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = adaptiveSpec.horizontalPadding, vertical = 4.dp)
                )

                ProfileErrorBoundaryBanner(
                    uiError = resolvedAuthUiError,
                    isBusy = effectiveAuthBusy,
                    onRetrySignIn = {
                        onDismissAuthError()
                        onGoogleSignIn()
                    },
                    onDismiss = onDismissAuthError,
                    modifier = Modifier
                        .widthIn(max = adaptiveSpec.maxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = adaptiveSpec.horizontalPadding, vertical = 4.dp)
                )

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
                        val activeAccount = effectiveAccount
                        if (activeAccount == null) {
                            FazaAiSignInGateCard(
                                isBusy = effectiveAuthBusy,
                                authError = authError,
                                onSignInClick = onGoogleSignIn,
                                onDismissError = onDismissAuthError,
                                showInlineError = false,
                                bottomOverlayPadding = bottomOverlayPadding + navBarBottom
                            )
                        } else {
                            key(activeAccount.firebaseUid) {
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
                            }
                        }
                    } else {
                        val profileGridBottomPadding = if (showBannerAd) {
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
                                        key = "profile_google_account_card_row",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ProfileGoogleAccountCard(
                                            account = effectiveAccount,
                                            isBusy = effectiveAuthBusy,
                                            authError = authError,
                                            onSignInClick = onGoogleSignIn,
                                            onSignOutClick = onGoogleSignOut,
                                            onDismissError = onDismissAuthError,
                                            showInlineError = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
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
                                                authenticatedAccount = effectiveAccount,
                                                useTwoByTwoStats = containerWidth < 900.dp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ProfileAiLauncherCard(
                                                assistantMessagesCount = if (effectiveAccount != null) assistantMessages.size else 0,
                                                isSignedIn = effectiveAccount != null,
                                                onOpenAssistant = { onAssistantOpenChange(true) },
                                                onSendTrendingPrompt = {
                                                    onAssistantOpenChange(true)
                                                    if (effectiveAccount != null) {
                                                        assistantViewModel.selectMode(AssistantChatMode.GENERAL_SEARCH)
                                                        assistantViewModel.sendMessage("Search for trending 4K HDR movies and series released recently.")
                                                    }
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
                                            authenticatedAccount = effectiveAccount,
                                            useTwoByTwoStats = adaptiveSpec.isUltraCompactWidth,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    item(
                                        key = "profile_google_account_card",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ProfileGoogleAccountCard(
                                            account = effectiveAccount,
                                            isBusy = effectiveAuthBusy,
                                            authError = authError,
                                            onSignInClick = onGoogleSignIn,
                                            onSignOutClick = onGoogleSignOut,
                                            onDismissError = onDismissAuthError,
                                            showInlineError = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    item(
                                        key = "profile_ai_assistant_card",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        ProfileAiLauncherCard(
                                            assistantMessagesCount = if (effectiveAccount != null) assistantMessages.size else 0,
                                            isSignedIn = effectiveAccount != null,
                                            onOpenAssistant = { onAssistantOpenChange(true) },
                                            onSendTrendingPrompt = {
                                                onAssistantOpenChange(true)
                                                if (effectiveAccount != null) {
                                                    assistantViewModel.selectMode(AssistantChatMode.GENERAL_SEARCH)
                                                    assistantViewModel.sendMessage("Search for trending 4K HDR movies and series released recently.")
                                                }
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

                                // Free Access — 24-Hour Ad-Free Access & 8 Rewarded Ads Progress
                                item(
                                    key = "profile_free_access",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    FreeAccessSectionCard(
                                        status = freeAccessStatus,
                                        isOnline = isOnline,
                                        isLoadingAd = isLoadingRewardedAd,
                                        feedback = freeAccessFeedback,
                                        onWatchRewardedAd = onWatchRewardedAd,
                                        onDismissFeedback = onDismissFreeAccessFeedback
                                    )
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
                                        FazaGlassCard(
                                            shape = RoundedCornerShape(24.dp),
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
                                    FazaGlassCard(
                                        shape = RoundedCornerShape(22.dp),
                                        onClick = onOpenSettings,
                                        modifier = Modifier.fillMaxWidth()
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

                            if (showBannerAd) {
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
                                    FazaAdBannerCard(testTag = "profile_ad_banner")
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
    authenticatedAccount: FazaGoogleAccount? = null,
    useTwoByTwoStats: Boolean,
    modifier: Modifier = Modifier
) {
    val runtimeLabel = if (totalDurationMs > 0L) Formatters.formatDuration(totalDurationMs) else "0:00"
    val colors = MaterialTheme.colorScheme

    FazaGlassCard(
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(if (useTwoByTwoStats) 16.dp else 18.dp),
        modifier = modifier.testTag("profile_hero_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedContent(
                    targetState = authenticatedAccount,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { it / 4 }) togetherWith (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 4 })
                    },
                    label = "profile_hero_avatar_transition"
                ) { account ->
                    if (account != null) {
                        GoogleAccountAvatar(
                            photoUrl = account.photoUrl,
                            displayName = account.resolvedDisplayName,
                            size = if (useTwoByTwoStats) 44.dp else 48.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(if (useTwoByTwoStats) 44.dp else 48.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User Profile",
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AnimatedContent(
                        targetState = authenticatedAccount?.resolvedDisplayName,
                        transitionSpec = {
                            (fadeIn(tween(260)) + slideInVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) { it / 2 }) togetherWith (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
                        },
                        label = "profile_hero_greeting_transition"
                    ) { signedInName ->
                        Text(
                            text = if (signedInName != null) {
                                stringResource(R.string.profile_welcome_greeting, signedInName)
                            } else {
                                "Personal Cinema"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("profile_hero_greeting_title")
                        )
                    }
                    Text(
                        text = "${stringResource(R.string.app_name)} · v${BuildConfig.VERSION_NAME} · ${preferences.decoderMode.badge}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.faza_icon_fg),
                        contentDescription = "FAZA Player",
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
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
    isSignedIn: Boolean = false,
    onOpenAssistant: () -> Unit,
    onSendTrendingPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    FazaGlassCard(
        shape = RoundedCornerShape(18.dp),
        accented = false,
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
                    size = 40.dp,
                    iconSize = 20.dp,
                    cornerRadius = 11.dp,
                    useSolidGradient = true
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FAZA AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isSignedIn) {
                            "Chat & search for movies, formats, and playback tips"
                        } else {
                            stringResource(R.string.google_auth_ai_launcher_signed_out_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open AI Assistant",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Trending movies",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when {
                                !isSignedIn -> "Sign in to chat"
                                assistantMessagesCount > 0 -> "Continue chat"
                                else -> "Start chatting"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
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
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
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
                fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(16.dp),
        accented = highlight,
        highlightedBorder = false,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FazaGlassIconBadge(
                icon = icon,
                size = 38.dp,
                iconSize = 19.dp,
                cornerRadius = 10.dp,
                useSolidGradient = highlight,
                useBlueHighlight = !highlight
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = countLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
