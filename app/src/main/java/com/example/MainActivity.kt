package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ads.AdFreeAccessPolicy
import com.example.ads.rememberIsInternetConnected
import com.example.ai.LibraryContextSummary
import com.example.ai.configureAppCheck
import com.example.data.model.UpdateCheckState
import com.example.ui.components.FirstLaunchMediaAccessDialog
import com.example.ui.components.FloatingContinuePlayButton
import com.example.ui.components.FloatingHomeNavigationRail
import com.example.ui.components.FloatingNavTabItem
import com.example.ui.components.FloatingNavigationDock
import com.example.ui.components.HomeUpdateInfoDialog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.selectAutoResumeVideo
import com.example.ui.components.selectContinuePlayingVideo
import com.example.ui.components.selectLastPlayedVideo
import com.example.ui.navigation.FazaRoute
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsAndAboutScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.screens.selectFavoriteVideos
import com.example.ui.theme.FazaPlayerTheme
import com.example.ui.viewmodel.AdFreeAccessViewModel
import com.example.ui.viewmodel.FazaAuthViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.ProfileAssistantViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.UpdateViewModel
import com.example.update.AppUpdateManager
import com.example.util.hasAllFilesAccessPermission
import com.example.util.openSystemAllFilesAccessSettings

class MainActivity : ComponentActivity() {

    private var isInPipModeState by mutableStateOf(false)
    private var incomingVideoUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        isInPipModeState = isInPictureInPictureMode
        handleIncomingIntent(intent)

        val app = application as FazaApplication

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(app.preferencesRepository)
            )
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.factory(app.videoRepository, app.preferencesRepository)
            )
            val playerViewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(
                    app.videoRepository,
                    app.preferencesRepository,
                    app.playerManager
                )
            )
            val updateViewModel: UpdateViewModel = viewModel(
                factory = UpdateViewModel.factory(
                    updateRepository = app.updateRepository,
                    preferencesRepository = app.preferencesRepository,
                    appUpdateManager = app.appUpdateManager
                )
            )
            val fazaAuthViewModel: FazaAuthViewModel = viewModel(
                factory = FazaAuthViewModel.factory(
                    authGateway = app.activeAuthGateway(),
                    accountStore = app.activeAiCloudStore()
                )
            )
            val profileAssistantViewModel: ProfileAssistantViewModel = viewModel(
                factory = ProfileAssistantViewModel.factory(
                    historyStore = app.assistantHistoryStore,
                    cloudStore = app.activeAiCloudStore()
                )
            )
            val adFreeAccessViewModel: AdFreeAccessViewModel = viewModel(
                factory = AdFreeAccessViewModel.factory(app.preferencesRepository)
            )

            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()

            FazaPlayerTheme(
                themeMode = preferences.themeMode,
                dynamicColor = preferences.dynamicColorEnabled,
                appColorSource = preferences.appColorSource,
                customColorHex = preferences.customThemeColorHex
            ) {
                FazaAppShell(
                    isInPipMode = isInPipModeState,
                    incomingVideoUri = incomingVideoUri,
                    onConsumeIncomingUri = { incomingVideoUri = null },
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    updateViewModel = updateViewModel,
                    settingsViewModel = settingsViewModel,
                    fazaAuthViewModel = fazaAuthViewModel,
                    profileAssistantViewModel = profileAssistantViewModel,
                    adFreeAccessViewModel = adFreeAccessViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")) {
            configureAppCheck(this, intent)
        }
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                incomingVideoUri = uri
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }
}

@Composable
private fun FazaAppShell(
    isInPipMode: Boolean,
    incomingVideoUri: Uri?,
    onConsumeIncomingUri: () -> Unit,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    updateViewModel: UpdateViewModel,
    settingsViewModel: SettingsViewModel,
    fazaAuthViewModel: FazaAuthViewModel,
    profileAssistantViewModel: ProfileAssistantViewModel,
    adFreeAccessViewModel: AdFreeAccessViewModel
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val isPreferencesLoaded by settingsViewModel.isPreferencesLoaded.collectAsStateWithLifecycle()
    val updateState by updateViewModel.checkState.collectAsStateWithLifecycle()
    val lastCheckedAtMillis by updateViewModel.lastCheckedAtMillis.collectAsStateWithLifecycle()
    val availableBannerManifest by updateViewModel.availableBannerManifest.collectAsStateWithLifecycle()
    val pendingPopup by updateViewModel.pendingPopupManifest.collectAsStateWithLifecycle()
    val showHomeChangelogPopup by updateViewModel.showHomeChangelogPopup.collectAsStateWithLifecycle()
    val versionHistory by updateViewModel.versionHistory.collectAsStateWithLifecycle()
    val unreadNotificationCount by updateViewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val activePlayerVideo by playerViewModel.currentVideo.collectAsStateWithLifecycle()
    val isPlayerPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val endedVideoId by playerViewModel.endedVideoId.collectAsStateWithLifecycle()
    val freeAccessStatus by adFreeAccessViewModel.freeAccessStatus.collectAsStateWithLifecycle()
    val isLoadingRewardedAd by adFreeAccessViewModel.isLoadingRewardedAd.collectAsStateWithLifecycle()
    val freeAccessFeedback by adFreeAccessViewModel.feedback.collectAsStateWithLifecycle()
    val authState by fazaAuthViewModel.authState.collectAsStateWithLifecycle()
    val authenticatedAccount by fazaAuthViewModel.currentAccount.collectAsStateWithLifecycle()
    val isAuthBusy by fazaAuthViewModel.isBusy.collectAsStateWithLifecycle()
    val authUiError by fazaAuthViewModel.authUiError.collectAsStateWithLifecycle()
    val authError by fazaAuthViewModel.authError.collectAsStateWithLifecycle()
    val welcomeGreeting by fazaAuthViewModel.welcomeGreeting.collectAsStateWithLifecycle()
    val isInternetConnected = rememberIsInternetConnected()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isProfileAssistantOpen by rememberSaveable { mutableStateOf(false) }
    var hasAttemptedStartupAutoResume by rememberSaveable { mutableStateOf(false) }
    var hasAllFilesAccess by remember { mutableStateOf(hasAllFilesAccessPermission()) }
    var awaitingSystemFileAccessReturn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fazaAuthViewModel.attemptSilentSignInIfNeeded(context)
    }

    LaunchedEffect(authState, authenticatedAccount) {
        val activeAccount = (authState as? com.example.auth.FazaAuthState.SignedIn)?.account
            ?: authenticatedAccount
        profileAssistantViewModel.bindAccount(activeAccount)
    }

    LaunchedEffect(isInternetConnected) {
        adFreeAccessViewModel.onConnectivityChanged(isInternetConnected)
    }

    LaunchedEffect(preferences.continuePlayingDuringCall) {
        playerViewModel.playerManager.setContinuePlayingDuringCall(preferences.continuePlayingDuringCall)
    }

    LaunchedEffect(
        isPreferencesLoaded,
        preferences.decoderMode,
        preferences.decoderFallbackEnabled,
        preferences.tunneledPlaybackEnabled
    ) {
        if (isPreferencesLoaded) {
            playerViewModel.playerManager.updateAdvancedDecoderSettings(
                decoderMode = preferences.decoderMode,
                decoderFallbackEnabled = preferences.decoderFallbackEnabled,
                tunneledPlaybackEnabled = preferences.tunneledPlaybackEnabled
            )
        }
    }

    LaunchedEffect(
        libraryState.allPublicVideos,
        libraryState.folders,
        libraryState.networkStreams,
        libraryState.recentVideos
    ) {
        profileAssistantViewModel.updateLibraryContext(
            LibraryContextSummary(
                totalPublicVideos = libraryState.allPublicVideos.size,
                folderCount = libraryState.folders.size,
                favouriteCount = libraryState.allPublicVideos.count { it.isFavourite },
                streamCount = libraryState.networkStreams.size,
                recentTitles = libraryState.recentVideos.filter { !it.isPrivate }.map { it.title },
                folderNames = libraryState.folders.map { it.name }
            )
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fazaAuthViewModel.refreshSessionStatusIfSignedIn()
                adFreeAccessViewModel.refreshTimer()
                updateViewModel.checkIfStale()
                val granted = hasAllFilesAccessPermission()
                hasAllFilesAccess = granted
                if (awaitingSystemFileAccessReturn) {
                    awaitingSystemFileAccessReturn = false
                    if (granted) {
                        settingsViewModel.setHasPromptedMediaAccess(true)
                        libraryViewModel.refreshDeviceStorage()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isMainTabRoute = currentDestination?.hasRoute<FazaRoute.Folders>() == true ||
        currentDestination?.hasRoute<FazaRoute.FolderDetail>() == true ||
        currentDestination?.hasRoute<FazaRoute.Favorites>() == true ||
        currentDestination?.hasRoute<FazaRoute.Profile>() == true ||
        currentDestination?.hasRoute<FazaRoute.Settings>() == true ||
        currentDestination == null
    val isSearchSelected = currentDestination?.hasRoute<FazaRoute.Search>() == true
    val isCheckingUpdates = updateState == UpdateCheckState.Checking
    val openRemoteUpdate: (com.example.data.model.UpdateManifest) -> Unit = { manifest ->
        val launched = AppUpdateManager.triggerUpdateDownload(context, manifest)
        if (!launched) {
            navController.navigate(FazaRoute.Updates) { launchSingleTop = true }
        }
    }

    // System Zero-Permission Media Pickers & SAF Folder Picker
    val pickMultipleVideos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(25)
    ) { uris ->
        libraryViewModel.importVideoUris(uris)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            libraryViewModel.importVideoUris(listOf(uri))
        }
    }

    val openFolderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            libraryViewModel.linkFolder(treeUri)
        }
    }

    LaunchedEffect(incomingVideoUri) {
        val uri = incomingVideoUri ?: return@LaunchedEffect
        hasAttemptedStartupAutoResume = true
        onConsumeIncomingUri()
        libraryViewModel.openExternalUri(uri) { videoId ->
            navController.navigate(FazaRoute.Player(videoId)) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(
        isPreferencesLoaded,
        libraryState.hasCompletedInitialScan,
        incomingVideoUri
    ) {
        if (!isPreferencesLoaded || !libraryState.hasCompletedInitialScan || hasAttemptedStartupAutoResume) {
            return@LaunchedEffect
        }
        hasAttemptedStartupAutoResume = true
        if (incomingVideoUri != null) {
            return@LaunchedEffect
        }
        if (preferences.autoResumeLastVideoOnRestart) {
            val resumeTarget = selectAutoResumeVideo(
                recentVideos = libraryState.recentVideos,
                allPublicVideos = libraryState.allPublicVideos
            )
            if (resumeTarget != null &&
                (currentDestination?.hasRoute<FazaRoute.Folders>() == true || currentDestination == null)
            ) {
                navController.navigate(FazaRoute.Player(resumeTarget.id)) {
                    launchSingleTop = true
                }
            }
        }
    }

    val isHomeSelected = currentDestination?.hasRoute<FazaRoute.Folders>() == true ||
        currentDestination?.hasRoute<FazaRoute.FolderDetail>() == true ||
        currentDestination == null
    val isFavoritesSelected = currentDestination?.hasRoute<FazaRoute.Favorites>() == true
    val isProfileSelected = currentDestination?.hasRoute<FazaRoute.Profile>() == true
    val isSettingsSelected = currentDestination?.hasRoute<FazaRoute.Settings>() == true

    val navigateTopLevel: (FazaRoute) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val floatingTabs = listOf(
        FloatingNavTabItem(
            id = "home_tab",
            label = "Home",
            showLabel = false,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            selected = isHomeSelected,
            badgeCount = 0,
            contentDescription = "Home",
            testTag = "nav_home",
            onClick = { navigateTopLevel(FazaRoute.Folders) }
        ),
        FloatingNavTabItem(
            id = "favorites_tab",
            label = "Favorites",
            showLabel = false,
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            selected = isFavoritesSelected,
            badgeCount = 0,
            contentDescription = "Favorites",
            testTag = "nav_favorites",
            onClick = { navigateTopLevel(FazaRoute.Favorites) }
        ),
        FloatingNavTabItem(
            id = "profile_tab",
            label = "Profile",
            showLabel = false,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            selected = isProfileSelected,
            badgeCount = 0,
            contentDescription = "User Profile",
            testTag = "nav_profile",
            onClick = { navigateTopLevel(FazaRoute.Profile) }
        ),
        FloatingNavTabItem(
            id = "settings_tab",
            label = "Settings",
            showLabel = false,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            selected = isSettingsSelected,
            badgeCount = unreadNotificationCount,
            contentDescription = "Settings",
            testTag = "nav_settings",
            onClick = {
                updateViewModel.markReleaseAlertSeen()
                navigateTopLevel(FazaRoute.Settings)
            }
        )
    )

    val showChrome = isMainTabRoute && !isInPipMode
    val isHomeRoot = currentDestination?.hasRoute<FazaRoute.Folders>() == true || currentDestination == null
    val activePlayingVideoId = if (isPlayerPlaying) activePlayerVideo?.id else null
    val continuePlayingCandidate = selectContinuePlayingVideo(
        recentVideos = libraryState.recentVideos,
        allPublicVideos = libraryState.allPublicVideos,
        activePlayingVideoId = activePlayingVideoId,
        endedVideoId = endedVideoId
    )
    val lastPlayedVideo = selectLastPlayedVideo(
        recentVideos = libraryState.recentVideos,
        allPublicVideos = libraryState.allPublicVideos
    )
    val continueResumeTarget = continuePlayingCandidate ?: lastPlayedVideo
    val showContinuePlayButton = showChrome && (
        isHomeRoot || (
            !isSettingsSelected &&
                !(isProfileSelected && isProfileAssistantOpen) &&
                continuePlayingCandidate != null
            )
        )
    var continueEmptyMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(continueEmptyMessage) {
        if (continueEmptyMessage != null) {
            delay(2_600L)
            continueEmptyMessage = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val adaptiveSpec = calculateAdaptiveLayoutSpec(maxWidth, maxHeight)
        val useRail = adaptiveSpec.useNavigationRail
        val shouldShowBannerAds = AdFreeAccessPolicy.shouldShowBannerAds(
            isOnline = isInternetConnected,
            expiresAtMs = freeAccessStatus.expiresAtMs,
            nowMs = freeAccessStatus.evaluatedAtMs
        )
        val hasBottomBannerAd = shouldShowBannerAds &&
            (isHomeRoot || isFavoritesSelected || (isProfileSelected && !isProfileAssistantOpen))
        val dockOnlyClearance = if (showChrome && !useRail) 78.dp else 0.dp
        val continueButtonSize = when {
            maxWidth < 360.dp -> 52.dp
            maxWidth >= 600.dp -> 60.dp
            else -> 56.dp
        }
        val continueClearance = if (showContinuePlayButton) continueButtonSize + 12.dp else 0.dp
        val dockBottomClearance = dockOnlyClearance + continueClearance

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showChrome && useRail) {
                    FloatingHomeNavigationRail(
                        items = floatingTabs,
                        onSearchClick = {
                            navController.navigate(FazaRoute.Search) { launchSingleTop = true }
                        },
                        isSearchActive = isSearchSelected
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = FazaRoute.Folders,
                    enterTransition = {
                        androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(180)
                        )
                    },
                    exitTransition = {
                        androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(140)
                        )
                    },
                    popEnterTransition = {
                        androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(180)
                        )
                    },
                    popExitTransition = {
                        androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(140)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    composable<FazaRoute.Folders> {
                        FoldersScreen(
                            folders = libraryState.folders,
                            recentNetworkStreams = libraryState.networkStreams,
                            isImporting = libraryState.isImporting,
                            isRefreshing = libraryState.isRefreshing,
                            statusMessage = libraryState.statusMessage,
                            updateBannerManifest = availableBannerManifest,
                            isCheckingUpdates = isCheckingUpdates,
                            onRefreshUpdate = openRemoteUpdate,
                            onViewUpdateDetails = {
                                navController.navigate(FazaRoute.Updates) { launchSingleTop = true }
                            },
                            onRecheckUpdates = { updateViewModel.checkManually() },
                            onDismissUpdateBanner = { updateViewModel.dismissUpdateBanner() },
                            onAutoDetectVideos = { showStatus ->
                                libraryViewModel.autoDetectDeviceVideos(showStatus)
                            },
                            onRefreshStorage = { libraryViewModel.refreshDeviceStorage() },
                            onPickVideosClick = {
                                pickMultipleVideos.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onOpenDocumentClick = {
                                openDocumentLauncher.launch(arrayOf("video/*", "*/*"))
                            },
                            onAddStreamUrl = { url, title, protocol ->
                                libraryViewModel.addNetworkStream(url, title, protocol) { id ->
                                    navController.navigate(FazaRoute.Player(id)) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onPlayExistingVideo = { video ->
                                navController.navigate(FazaRoute.Player(video.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onToggleFavourite = { libraryViewModel.toggleFavourite(it) },
                            onTogglePrivate = { libraryViewModel.togglePrivate(it) },
                            onRemoveVideo = { libraryViewModel.removeVideo(it) },
                            onClearStatus = { libraryViewModel.clearStatusMessage() },
                            onLinkFolderClick = { openFolderTreeLauncher.launch(null) },
                            onOpenFolder = { summary ->
                                navController.navigate(
                                    FazaRoute.FolderDetail(
                                        folderName = summary.name,
                                        folderUri = summary.folderUri
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onUnlinkFolder = { libraryViewModel.removeFolder(it) },
                            showBannerAd = shouldShowBannerAds,
                            bottomOverlayPadding = dockBottomClearance,
                            dockBottomPadding = dockOnlyClearance,
                            continueOverlayPadding = continueClearance
                        )
                    }

                    composable<FazaRoute.FolderDetail> { entry ->
                        val route = entry.toRoute<FazaRoute.FolderDetail>()
                        val folderVideos = remember(
                            libraryState.allPublicVideos,
                            libraryState.sortOption,
                            route.folderUri,
                            route.folderName
                        ) {
                            LibraryViewModel.sortVideos(
                                videos = com.example.util.VideoDeduplicator.deduplicate(
                                    libraryState.allPublicVideos.filter {
                                        if (route.folderUri != null) {
                                            it.folderUri == route.folderUri
                                        } else {
                                            it.folderName == route.folderName
                                        }
                                    }
                                ),
                                option = libraryState.sortOption
                            )
                        }
                        FolderDetailScreen(
                            folderName = route.folderName,
                            videos = folderVideos,
                            sortOption = libraryState.sortOption,
                            onSortChange = { libraryViewModel.setSortOption(it) },
                            isRefreshing = libraryState.isRefreshing,
                            onRefreshStorage = { libraryViewModel.refreshDeviceStorage() },
                            onBack = { navController.popBackStack() },
                            onPlayVideo = { video ->
                                navController.navigate(FazaRoute.Player(video.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onToggleFavourite = { libraryViewModel.toggleFavourite(it) },
                            onTogglePrivate = { libraryViewModel.togglePrivate(it) },
                            onRemoveVideo = { libraryViewModel.removeVideo(it) },
                            bottomOverlayPadding = dockBottomClearance
                        )
                    }

                    composable<FazaRoute.Favorites> {
                        val favoriteVideos = remember(
                            libraryState.allPublicVideos,
                            libraryState.sortOption
                        ) {
                            selectFavoriteVideos(
                                allPublicVideos = libraryState.allPublicVideos,
                                sortOption = libraryState.sortOption
                            )
                        }
                        FavoritesScreen(
                            favoriteVideos = favoriteVideos,
                            allPublicVideos = libraryState.allPublicVideos,
                            sortOption = libraryState.sortOption,
                            onSortChange = { libraryViewModel.setSortOption(it) },
                            onPlayVideo = { video ->
                                navController.navigate(FazaRoute.Player(video.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onToggleFavourite = { libraryViewModel.toggleFavourite(it) },
                            onTogglePrivate = { libraryViewModel.togglePrivate(it) },
                            onRemoveVideo = { libraryViewModel.removeVideo(it) },
                            onBrowseFolders = { navigateTopLevel(FazaRoute.Folders) },
                            showBannerAd = shouldShowBannerAds,
                            bottomOverlayPadding = dockBottomClearance,
                            dockBottomPadding = dockOnlyClearance,
                            continueOverlayPadding = continueClearance
                        )
                    }

                    composable<FazaRoute.Profile> {
                        ProfileScreen(
                            videos = libraryState.allPublicVideos,
                            folders = libraryState.folders,
                            networkStreams = libraryState.networkStreams,
                            preferences = preferences,
                            updateState = updateState,
                            unreadUpdateCount = unreadNotificationCount,
                            assistantViewModel = profileAssistantViewModel,
                            isAssistantOpen = isProfileAssistantOpen,
                            onAssistantOpenChange = { isProfileAssistantOpen = it },
                            authState = authState,
                            authenticatedAccount = authenticatedAccount,
                            isAuthBusy = isAuthBusy,
                            authUiError = authUiError,
                            authError = authError,
                            welcomeGreeting = welcomeGreeting,
                            onGoogleSignIn = {
                                hostActivity?.let { activity ->
                                    fazaAuthViewModel.signInWithGoogle(activity)
                                }
                            },
                            onGoogleSignOut = {
                                fazaAuthViewModel.signOut(
                                    context = context,
                                    onBeforeSignOut = {
                                        profileAssistantViewModel.detachBeforeSignOut()
                                    }
                                )
                            },
                            onDismissAuthError = { fazaAuthViewModel.clearError() },
                            onDismissWelcomeGreeting = { fazaAuthViewModel.dismissWelcomeGreeting() },
                            onPlayVideo = { video ->
                                navController.navigate(FazaRoute.Player(video.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onToggleFavourite = { libraryViewModel.toggleFavourite(it) },
                            onTogglePrivate = { libraryViewModel.togglePrivate(it) },
                            onRemoveVideo = { libraryViewModel.removeVideo(it) },
                            onOpenSettings = { navigateTopLevel(FazaRoute.Settings) },
                            onOpenUpdates = {
                                updateViewModel.markReleaseAlertSeen()
                                navController.navigate(FazaRoute.Updates) { launchSingleTop = true }
                            },
                            onLinkFolderClick = { openFolderTreeLauncher.launch(null) },
                            freeAccessStatus = freeAccessStatus,
                            isOnline = isInternetConnected,
                            isLoadingRewardedAd = isLoadingRewardedAd,
                            freeAccessFeedback = freeAccessFeedback,
                            onWatchRewardedAd = {
                                hostActivity?.let { activity ->
                                    adFreeAccessViewModel.requestRewardedAd(activity, isInternetConnected)
                                }
                            },
                            onDismissFreeAccessFeedback = { adFreeAccessViewModel.clearFeedback() },
                            showBannerAd = shouldShowBannerAds,
                            bottomOverlayPadding = dockBottomClearance,
                            dockBottomPadding = dockOnlyClearance,
                            continueOverlayPadding = continueClearance
                        )
                    }

                    composable<FazaRoute.Search> {
                        SearchScreen(
                            videos = libraryState.videos,
                            searchQuery = libraryState.searchQuery,
                            onSearchQueryChange = { libraryViewModel.setSearchQuery(it) },
                            sortOption = libraryState.sortOption,
                            onSortChange = { libraryViewModel.setSortOption(it) },
                            onBack = { navController.popBackStack() },
                            onPlayVideo = { video ->
                                navController.navigate(FazaRoute.Player(video.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onToggleFavourite = { libraryViewModel.toggleFavourite(it) },
                            onTogglePrivate = { libraryViewModel.togglePrivate(it) },
                            onRemoveVideo = { libraryViewModel.removeVideo(it) },
                            onPickVideosClick = {
                                pickMultipleVideos.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        )
                    }

                    composable<FazaRoute.Updates> {
                        UpdatesScreen(
                            checkState = updateState,
                            versionHistory = versionHistory,
                            manifestUrl = preferences.updateManifestUrl,
                            onBack = { navController.popBackStack() },
                            onCheckUpdates = { updateViewModel.checkManually() },
                            onShowAvailableDialog = { updateViewModel.showPopupIfAvailable() },
                            onSaveManifestUrl = { updateViewModel.updateManifestEndpoint(it) },
                            onDownloadUpdate = openRemoteUpdate,
                            lastCheckedAtMillis = lastCheckedAtMillis,
                            periodicIntervalMinutes = updateViewModel.periodicIntervalMinutes
                        )
                    }

                    composable<FazaRoute.Settings> {
                        SettingsAndAboutScreen(
                            preferences = preferences,
                            updateState = updateState,
                            isTopLevelTab = true,
                            onBack = { navController.popBackStack() },
                            onOpenVersionHistory = {
                                navController.navigate(FazaRoute.Updates) { launchSingleTop = true }
                            },
                            onSetThemeMode = { settingsViewModel.setThemeMode(it) },
                            onSetDynamicColor = { settingsViewModel.setDynamicColorEnabled(it) },
                            onSetAppColorSource = { settingsViewModel.setAppColorSource(it) },
                            onSetCustomThemeColorHex = { settingsViewModel.setCustomThemeColorHex(it) },
                            onSetDecoderMode = { settingsViewModel.setDecoderMode(it) },
                            onSetDecoderFallback = { settingsViewModel.setDecoderFallbackEnabled(it) },
                            onSetTunneledPlayback = { settingsViewModel.setTunneledPlaybackEnabled(it) },
                            onUpdateSubtitleStyle = { settingsViewModel.updateSubtitleStyle(it) },
                            onUpdateNowPlayingConfig = { settingsViewModel.updateNowPlayingConfig(it) },
                            onUpdateRunningTimeConfig = { settingsViewModel.updateRunningTimeConfig(it) },
                            onUpdateAppIconOverlayConfig = { settingsViewModel.updateAppIconOverlayConfig(it) },
                            onUpdatePlayerUiConfig = { settingsViewModel.updatePlayerUiConfig(it) },
                            onResetPlayerUiConfig = { settingsViewModel.resetPlayerUiConfig() },
                            onSetBackgroundPlayback = { settingsViewModel.setBackgroundPlayback(it) },
                            onSetContinuePlayingDuringCall = { settingsViewModel.setContinuePlayingDuringCall(it) },
                            onSetAutoPip = { settingsViewModel.setAutoPip(it) },
                            onSetForceLandscapeOnOpen = { settingsViewModel.setForceLandscapeOnOpen(it) },
                            onSetAutoResumeOnRestart = { settingsViewModel.setAutoResumeLastVideoOnRestart(it) },
                            onSetMidVideoBreakEnabled = { settingsViewModel.setMidVideoBreakEnabled(it) },
                            onSetPrivatePin = { settingsViewModel.setPrivatePin(it) },
                            onCheckUpdates = { updateViewModel.checkManually() },
                            bottomOverlayPadding = dockBottomClearance
                        )
                    }

                    composable<FazaRoute.Player> { entry ->
                        val route = entry.toRoute<FazaRoute.Player>()
                        PlayerScreen(
                            videoId = route.videoId,
                            isInPipMode = isInPipMode,
                            viewModel = playerViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = continueEmptyMessage != null && showContinuePlayButton,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(180)
                ) + androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it / 3 }
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(160)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .padding(
                        start = adaptiveSpec.horizontalPadding,
                        end = adaptiveSpec.horizontalPadding + continueButtonSize + 12.dp,
                        bottom = (if (useRail) 20.dp else 90.dp) + (if (hasBottomBannerAd) 76.dp else 0.dp)
                    )
            ) {
                continueEmptyMessage?.let { message ->
                    Surface(
                        onClick = { continueEmptyMessage = null },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.testTag("continue_playing_empty_message")
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showContinuePlayButton,
                enter = androidx.compose.animation.scaleIn(
                    initialScale = 0.82f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.scaleOut(targetScale = 0.85f) + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .padding(
                        end = adaptiveSpec.horizontalPadding,
                        bottom = (if (useRail) 16.dp else 86.dp) + (if (hasBottomBannerAd) 76.dp else 0.dp)
                    )
            ) {
                FloatingContinuePlayButton(
                    video = continueResumeTarget,
                    buttonSize = continueButtonSize,
                    onClick = {
                        val target = continueResumeTarget
                        if (target != null) {
                            continueEmptyMessage = null
                            navController.navigate(FazaRoute.Player(target.id)) {
                                launchSingleTop = true
                            }
                        } else {
                            continueEmptyMessage = context.getString(R.string.continue_playing_empty_message)
                        }
                    }
                )
            }

            if (showChrome && !useRail) {
                FloatingNavigationDock(
                    items = floatingTabs,
                    onSearchClick = if (isSettingsSelected) {
                        null
                    } else {
                        { navController.navigate(FazaRoute.Search) { launchSingleTop = true } }
                    },
                    isSearchActive = isSearchSelected,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // First-Launch All File Access & Update Popups on primary Folder screen
    val isOnPrimaryScreen = currentDestination?.hasRoute<FazaRoute.Folders>() == true || currentDestination == null
    if (isOnPrimaryScreen) {
        val shouldShowFirstLaunchMediaPrompt = isPreferencesLoaded &&
            libraryState.hasCompletedInitialScan &&
            !hasAllFilesAccess &&
            !preferences.hasPromptedMediaAccess &&
            libraryState.linkedFolderCount == 0 &&
            libraryState.allPublicVideos.isEmpty()

        val remoteManifest = pendingPopup
        if (shouldShowFirstLaunchMediaPrompt) {
            FirstLaunchMediaAccessDialog(
                onGrantAccess = {
                    awaitingSystemFileAccessReturn = true
                    val opened = openSystemAllFilesAccessSettings(context)
                    if (!opened) {
                        awaitingSystemFileAccessReturn = false
                        settingsViewModel.setHasPromptedMediaAccess(true)
                        openFolderTreeLauncher.launch(null)
                    }
                },
                onSkip = {
                    awaitingSystemFileAccessReturn = false
                    settingsViewModel.setHasPromptedMediaAccess(true)
                }
            )
        } else if (remoteManifest != null) {
            UpdateAvailableDialog(
                manifest = remoteManifest,
                onUpdateNow = {
                    updateViewModel.hidePopupTemporarily()
                    openRemoteUpdate(remoteManifest)
                },
                onLater = {
                    updateViewModel.dismissPopupForLater()
                },
                onVersionHistory = {
                    updateViewModel.hidePopupTemporarily()
                    navController.navigate(FazaRoute.Updates) {
                        launchSingleTop = true
                    }
                }
            )
        } else if (showHomeChangelogPopup) {
            HomeUpdateInfoDialog(
                release = updateViewModel.currentRelease,
                onClose = {
                    updateViewModel.dismissHomeChangelogPopup()
                },
                onVersionHistory = {
                    updateViewModel.dismissHomeChangelogPopup()
                    navController.navigate(FazaRoute.Updates) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
