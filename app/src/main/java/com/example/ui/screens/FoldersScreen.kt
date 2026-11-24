package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ads.rememberIsInternetConnected
import com.example.data.model.FolderSummary
import com.example.data.model.SortOption
import com.example.data.model.StreamProtocol
import com.example.data.model.UpdateManifest
import com.example.data.model.VideoItem
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.CachedVideoThumbnail
import com.example.ui.components.FazaAccentGradientButton
import com.example.ui.components.FazaAdBannerCard
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaBrandLogo
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassIconBadge
import com.example.ui.components.FazaGlassPillBadge
import com.example.ui.components.FazaGlassTopBarIconButton
import com.example.ui.components.NetworkStreamDialog
import com.example.ui.components.UpdateNotificationBanner
import com.example.ui.components.VideoListCard
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.components.rememberFazaBounceScale
import com.example.ui.components.rememberFazaGlassBorderBrush
import com.example.ui.components.rememberFazaGlassFillBrush
import com.example.util.Formatters

private val FolderThumbOverlayBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.08f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.62f)
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoldersScreen(
    folders: List<FolderSummary>,
    filteredVideos: List<VideoItem> = emptyList(),
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    onSortChange: (SortOption) -> Unit = {},
    recentNetworkStreams: List<VideoItem> = emptyList(),
    isImporting: Boolean = false,
    isRefreshing: Boolean = false,
    statusMessage: String? = null,
    updateBannerManifest: UpdateManifest? = null,
    isCheckingUpdates: Boolean = false,
    onRefreshUpdate: (UpdateManifest) -> Unit = {},
    onViewUpdateDetails: () -> Unit = {},
    onRecheckUpdates: () -> Unit = {},
    onDismissUpdateBanner: () -> Unit = {},
    onAutoDetectVideos: (Boolean) -> Unit = {},
    onRefreshStorage: () -> Unit = { onAutoDetectVideos(true) },
    onPickVideosClick: () -> Unit = {},
    onOpenDocumentClick: () -> Unit = {},
    onAddStreamUrl: (String, String?, StreamProtocol) -> Unit = { _, _, _ -> },
    onPlayExistingVideo: (VideoItem) -> Unit = {},
    onToggleFavourite: (VideoItem) -> Unit = {},
    onTogglePrivate: (VideoItem) -> Unit = {},
    onRemoveVideo: (Long) -> Unit = {},
    onClearStatus: () -> Unit = {},
    onLinkFolderClick: () -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onUnlinkFolder: (String) -> Unit,
    bottomOverlayPadding: Dp = 0.dp,
    dockBottomPadding: Dp = bottomOverlayPadding,
    continueOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = searchQuery.isNotEmpty()) {
        onSearchQueryChange("")
    }
    var showStreamDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val pullToRefreshState = rememberPullToRefreshState()
    val isInternetConnected = rememberIsInternetConnected()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onAutoDetectVideos(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearStatus()
        }
    }

    FazaAmbientBackdrop(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = bottomOverlayPadding)
                )
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        FazaBrandLogo(subtitle = "Home")
                    },
                    actions = {
                        FazaGlassTopBarIconButton(
                            icon = Icons.Filled.Refresh,
                            contentDescription = "Scan device storage",
                            onClick = onRefreshStorage,
                            modifier = Modifier.testTag("auto_detect_videos_button")
                        )
                        FazaGlassTopBarIconButton(
                            icon = Icons.Filled.AddLink,
                            contentDescription = "Play stream URL",
                            onClick = { showStreamDialog = true },
                            modifier = Modifier.testTag("open_stream_chip")
                        )
                        FazaGlassTopBarIconButton(
                            icon = Icons.Filled.FileOpen,
                            contentDescription = "Open video file",
                            onClick = onOpenDocumentClick,
                            modifier = Modifier.testTag("open_document_chip")
                        )
                        FazaGlassTopBarIconButton(
                            icon = Icons.Filled.CreateNewFolder,
                            contentDescription = "Link folder",
                            onClick = onLinkFolderClick,
                            highlighted = true,
                            modifier = Modifier.testTag("add_folder_button")
                        )
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = isImporting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefreshStorage,
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("home_pull_to_refresh")
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val adaptiveSpec = calculateAdaptiveLayoutSpec(maxWidth, maxHeight)
                        val folderGridColumns = when {
                            maxWidth >= 900.dp -> 4
                            maxWidth >= 600.dp -> 3
                            maxWidth >= 340.dp -> 2
                            else -> 1
                        }

                        if (folders.isEmpty()) {
                            val emptyBottomPadding = if (isInternetConnected) {
                                12.dp + continueOverlayPadding
                            } else {
                                24.dp + bottomOverlayPadding + navBarBottom
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .widthIn(max = 840.dp)
                                    .padding(bottom = emptyBottomPadding),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                UpdateNotificationBanner(
                                    manifest = updateBannerManifest,
                                    isChecking = isCheckingUpdates,
                                    onRefreshUpdate = onRefreshUpdate,
                                    onViewDetails = onViewUpdateDetails,
                                    onRecheckVersion = onRecheckUpdates,
                                    onDismiss = onDismissUpdateBanner,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                                )
                                FazaGlassCard(
                                    accented = false,
                                    shape = RoundedCornerShape(28.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = if (adaptiveSpec.isUltraCompactWidth) 18.dp else 26.dp,
                                        vertical = if (adaptiveSpec.isShortHeight) 22.dp else 32.dp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 540.dp)
                                        .padding(
                                            horizontal = adaptiveSpec.horizontalPadding,
                                            vertical = if (adaptiveSpec.isShortHeight) 14.dp else 26.dp
                                        )
                                        .testTag("home_empty_folders_state")
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FazaGlassIconBadge(
                                            icon = Icons.Outlined.Folder,
                                            size = 68.dp,
                                            iconSize = 32.dp,
                                            cornerRadius = 34.dp,
                                            useSolidGradient = false
                                        )
                                        Text(
                                            text = "No video folders found",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "No device video folders are available yet. Pull down to scan device storage or link a folder to browse your videos.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FazaAccentGradientButton(
                                                onClick = onLinkFolderClick
                                            ) {
                                                Text(
                                                    text = "Link Folder",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = onRefreshStorage,
                                                shape = CircleShape,
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Scan Storage")
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = { showStreamDialog = true },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("empty_state_stream_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AddLink,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Play Network Stream URL")
                                        }
                                    }
                                }
                            }
                        } else {
                            val gridBottomPadding = if (isInternetConnected) {
                                12.dp + continueOverlayPadding
                            } else {
                                24.dp + bottomOverlayPadding + navBarBottom
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(folderGridColumns),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = adaptiveSpec.maxContentWidth)
                                    .testTag("home_folders_grid"),
                                contentPadding = PaddingValues(
                                    start = adaptiveSpec.horizontalPadding,
                                    end = adaptiveSpec.horizontalPadding,
                                    top = 8.dp,
                                    bottom = gridBottomPadding
                                ),
                                horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                                verticalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                            ) {
                                if (updateBannerManifest != null) {
                                    item(
                                        key = "update_banner",
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = "update_banner"
                                    ) {
                                        UpdateNotificationBanner(
                                            manifest = updateBannerManifest,
                                            isChecking = isCheckingUpdates,
                                            onRefreshUpdate = onRefreshUpdate,
                                            onViewDetails = onViewUpdateDetails,
                                            onRecheckVersion = onRecheckUpdates,
                                            onDismiss = onDismissUpdateBanner
                                        )
                                    }
                                }

                                item(
                                    key = "network_stream_card",
                                    span = { GridItemSpan(maxLineSpan) },
                                    contentType = "stream_card"
                                ) {
                                    FazaGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("folders_network_stream_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        accented = false,
                                        onClick = { showStreamDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FazaGlassIconBadge(
                                                icon = Icons.Filled.AddLink,
                                                size = 38.dp,
                                                iconSize = 19.dp,
                                                cornerRadius = 10.dp,
                                                useSolidGradient = true
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Network Stream",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (recentNetworkStreams.isNotEmpty()) {
                                                        "Open URL or replay ${recentNetworkStreams.size} saved stream${if (recentNetworkStreams.size == 1) "" else "s"}"
                                                    } else {
                                                        "HLS, DASH, MP4, MKV URLs"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Open stream dialog",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                item(
                                    key = "folders_header",
                                    span = { GridItemSpan(maxLineSpan) },
                                    contentType = "section_header"
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Folders",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.testTag("home_folders_header")
                                        )
                                        Text(
                                            text = "${folders.size} folder${if (folders.size == 1) "" else "s"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                items(
                                    items = folders,
                                    key = { it.folderUri ?: it.name },
                                    contentType = { "device_folder_card" }
                                ) { folder ->
                                    DeviceFolderCard(
                                        folder = folder,
                                        onOpenFolder = { onOpenFolder(folder) },
                                        onUnlinkFolder = onUnlinkFolder
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
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = dockBottomPadding + navBarBottom + 8.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FazaAdBannerCard(
                            testTag = "home_ad_banner",
                            dockedBottom = true
                        )
                    }
                }
            }
        }
    }

    if (showStreamDialog) {
        NetworkStreamDialog(
            recentStreams = recentNetworkStreams,
            onDismiss = { showStreamDialog = false },
            onPlayStream = { url, title, protocol ->
                showStreamDialog = false
                onAddStreamUrl(url, title, protocol)
            },
            onPlaySavedStream = { stream ->
                showStreamDialog = false
                onPlayExistingVideo(stream)
            }
        )
    }
}

@Composable
private fun DeviceFolderCard(
    folder: FolderSummary,
    onOpenFolder: () -> Unit,
    onUnlinkFolder: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val countLabel = remember(folder.videoCount) {
        "${folder.videoCount} video${if (folder.videoCount == 1) "" else "s"}"
    }
    val sizeLabel = remember(folder.totalSizeBytes) {
        if (folder.totalSizeBytes > 0L) Formatters.formatFileSize(folder.totalSizeBytes) else ""
    }

    FazaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("folder_card_${folder.name}"),
        shape = RoundedCornerShape(16.dp),
        onClick = onOpenFolder
    ) {
        // Clean edge-to-edge minimalist media thumbnail header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(colors.surfaceContainerHigh)
        ) {
            CachedVideoThumbnail(
                thumbnailPath = folder.thumbnailPath,
                contentDescription = "${folder.name} folder thumbnail",
                decodeWidthPx = 420,
                decodeHeightPx = 262,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("folder_thumb_${folder.name}")
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FolderThumbOverlayBrush)
            )

            // Optional unlink icon for SAF-linked folders
            if (folder.isSafLinked && folder.folderUri != null) {
                IconButton(
                    onClick = { onUnlinkFolder(folder.folderUri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.68f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LinkOff,
                            contentDescription = "Unlink folder",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Understated dark video count tag at bottom-end of thumbnail
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        // Minimalist folder title & size footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (sizeLabel.isNotEmpty()) sizeLabel else countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoSortPillButton(
    selectedSort: SortOption,
    onSelectSort: (SortOption) -> Unit,
    testTagPrefix: String = "home",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val pillShape = CircleShape
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        enabled = true,
        pressedScale = 0.95f
    )

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            interactionSource = interactionSource,
            shape = pillShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = bounceScale
                    scaleY = bounceScale
                }
                .minimumInteractiveComponentSize()
                .testTag("${testTagPrefix}_sort_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort videos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selectedSort.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.entries.forEach { option ->
                val isSelected = option == selectedSort
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectSort(option)
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_sort_option_${option.name}")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    videos: List<VideoItem>,
    sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    onSortChange: (SortOption) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefreshStorage: () -> Unit = {},
    onBack: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavourite: (VideoItem) -> Unit,
    onTogglePrivate: (VideoItem) -> Unit,
    onRemoveVideo: (Long) -> Unit,
    bottomOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val pullToRefreshState = rememberPullToRefreshState()
    val uniqueVideos = remember(videos) { com.example.util.VideoDeduplicator.deduplicate(videos) }

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
                    navigationIcon = {
                        FazaGlassTopBarIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to folders",
                            onClick = onBack,
                            modifier = Modifier.testTag("folder_detail_back_button")
                        )
                    },
                    title = {
                        Column {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("folder_detail_title")
                            )
                            Text(
                                text = "${uniqueVideos.size} video${if (uniqueVideos.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        VideoSortPillButton(
                            selectedSort = sortOption,
                            onSelectSort = onSortChange,
                            testTagPrefix = "folder_detail",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefreshStorage,
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("folder_detail_pull_to_refresh")
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val adaptiveSpec = calculateAdaptiveLayoutSpec(maxWidth, maxHeight)
                    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    if (uniqueVideos.isEmpty()) {
                        FazaGlassCard(
                            accented = true,
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                                .padding(horizontal = adaptiveSpec.horizontalPadding, vertical = 24.dp)
                                .testTag("folder_detail_empty_state")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FazaGlassIconBadge(
                                    icon = Icons.Outlined.VideoLibrary,
                                    size = 56.dp,
                                    iconSize = 26.dp,
                                    cornerRadius = 18.dp,
                                    useBlueHighlight = true
                                )
                                Text(
                                    text = "No videos in this folder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Pull down to rescan this folder for newly added video files.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(adaptiveSpec.videoGridColumns),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = adaptiveSpec.maxContentWidth)
                                .testTag("folder_detail_video_list"),
                            contentPadding = PaddingValues(
                                start = adaptiveSpec.horizontalPadding,
                                end = adaptiveSpec.horizontalPadding,
                                top = 12.dp,
                                bottom = 16.dp + bottomOverlayPadding + navBarBottom
                            ),
                            horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                            verticalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                        ) {
                            items(
                                items = uniqueVideos,
                                key = { it.id },
                                contentType = { "folder_video_card" }
                            ) { video ->
                                VideoListCard(
                                    video = video,
                                    isCompact = adaptiveSpec.isUltraCompactWidth,
                                    onPlayClick = { onPlayVideo(video) },
                                    onToggleFavourite = { onToggleFavourite(video) },
                                    onTogglePrivate = { onTogglePrivate(video) },
                                    onRemove = { onRemoveVideo(video.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
