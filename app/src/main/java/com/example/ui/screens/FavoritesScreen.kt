package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.CachedVideoThumbnail
import com.example.ui.components.FazaAccentGradientButton
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaBrandLogo
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassIconBadge
import com.example.ui.components.FazaGlassPillBadge
import com.example.ui.components.FazaGlassTopBarIconButton
import com.example.ui.components.VideoListCard
import com.example.ui.viewmodel.LibraryViewModel
import com.example.util.Formatters
import com.example.util.VideoDeduplicator
import kotlinx.coroutines.launch

/**
 * Selects deduplicated, non-private favorite videos sorted with the active [SortOption].
 */
fun selectFavoriteVideos(
    allPublicVideos: List<VideoItem>,
    sortOption: SortOption
): List<VideoItem> {
    val favorites = VideoDeduplicator.deduplicate(
        allPublicVideos.filter { !it.isPrivate && it.isFavourite }
    )
    return LibraryViewModel.sortVideos(favorites, sortOption)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteVideos: List<VideoItem>,
    allPublicVideos: List<VideoItem>,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavourite: (VideoItem) -> Unit,
    onTogglePrivate: (VideoItem) -> Unit,
    onRemoveVideo: (Long) -> Unit,
    onBrowseFolders: () -> Unit,
    bottomOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showManageFavoritesSheet by rememberSaveable { mutableStateOf(false) }

    val uniqueAllVideos = remember(allPublicVideos) {
        VideoDeduplicator.deduplicate(allPublicVideos.filter { !it.isPrivate })
    }
    val totalDurationMs = remember(favoriteVideos) {
        favoriteVideos.sumOf { it.durationMs.coerceAtLeast(0L) }
    }

    val handleRemoveFavorite: (VideoItem) -> Unit = { video ->
        onToggleFavourite(video)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.favorites_removed_snackbar, video.title),
                actionLabel = context.getString(R.string.favorites_undo_action),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onToggleFavourite(video.copy(isFavourite = false))
            }
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
                        FazaBrandLogo(subtitle = stringResource(R.string.nav_favorites))
                    },
                    actions = {
                        if (favoriteVideos.isNotEmpty()) {
                            VideoSortPillButton(
                                selectedSort = sortOption,
                                onSelectSort = onSortChange,
                                testTagPrefix = "favorites",
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        if (uniqueAllVideos.isNotEmpty()) {
                            FazaGlassTopBarIconButton(
                                icon = Icons.Filled.BookmarkAdd,
                                contentDescription = stringResource(R.string.favorites_add_videos_cd),
                                onClick = { showManageFavoritesSheet = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("favorites_add_button")
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                val adaptiveSpec = calculateAdaptiveLayoutSpec(maxWidth, maxHeight)
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                if (favoriteVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = adaptiveSpec.horizontalPadding,
                                end = adaptiveSpec.horizontalPadding,
                                top = 20.dp,
                                bottom = 16.dp + bottomOverlayPadding + navBarBottom
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        FazaGlassCard(
                            accented = true,
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .testTag("favorites_empty_state")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FazaGlassIconBadge(
                                    icon = Icons.Outlined.FavoriteBorder,
                                    size = 60.dp,
                                    iconSize = 28.dp,
                                    cornerRadius = 20.dp,
                                    useBlueHighlight = false
                                )
                                Text(
                                    text = stringResource(R.string.favorites_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.favorites_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (uniqueAllVideos.isNotEmpty()) {
                                    FazaAccentGradientButton(
                                        onClick = { showManageFavoritesSheet = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("favorites_empty_add_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.BookmarkAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.favorites_add_videos),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onBrowseFolders,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("favorites_browse_folders_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.favorites_browse_folders))
                                    }
                                } else {
                                    FazaAccentGradientButton(
                                        onClick = onBrowseFolders,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("favorites_browse_folders_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.favorites_browse_folders),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(adaptiveSpec.videoGridColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = adaptiveSpec.maxContentWidth)
                            .testTag("favorites_video_list"),
                        contentPadding = PaddingValues(
                            start = adaptiveSpec.horizontalPadding,
                            end = adaptiveSpec.horizontalPadding,
                            top = 12.dp,
                            bottom = 16.dp + bottomOverlayPadding + navBarBottom
                        ),
                        horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                        verticalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                    ) {
                        item(
                            key = "favorites_summary_header",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "favorites_summary"
                        ) {
                            FavoritesSummaryCard(
                                count = favoriteVideos.size,
                                totalDurationMs = totalDurationMs,
                                onPlayFirst = { favoriteVideos.firstOrNull()?.let(onPlayVideo) }
                            )
                        }

                        items(
                            items = favoriteVideos,
                            key = { it.id },
                            contentType = { "favorite_video_card" }
                        ) { video ->
                            VideoListCard(
                                video = video,
                                isCompact = adaptiveSpec.isUltraCompactWidth,
                                onPlayClick = { onPlayVideo(video) },
                                onToggleFavourite = { handleRemoveFavorite(video) },
                                onTogglePrivate = { onTogglePrivate(video) },
                                onRemove = { onRemoveVideo(video.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showManageFavoritesSheet) {
        ManageFavoritesBottomSheet(
            allVideos = uniqueAllVideos,
            onToggleFavourite = onToggleFavourite,
            onDismiss = { showManageFavoritesSheet = false }
        )
    }
}

@Composable
private fun FavoritesSummaryCard(
    count: Int,
    totalDurationMs: Long,
    onPlayFirst: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val countLabel = "$count video${if (count == 1) "" else "s"}"
    val durationLabel = remember(totalDurationMs) {
        if (totalDurationMs > 0L) Formatters.formatDuration(totalDurationMs) else null
    }

    FazaGlassCard(
        accented = true,
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("favorites_summary_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FazaGlassIconBadge(
                icon = Icons.Filled.Favorite,
                size = 46.dp,
                iconSize = 22.dp,
                cornerRadius = 15.dp,
                useBlueHighlight = false
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stringResource(R.string.favorites_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FazaGlassPillBadge(
                        text = countLabel
                    )
                    if (durationLabel != null) {
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            FilledTonalButton(
                onClick = onPlayFirst,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorites_play_first_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.favorites_play_first),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageFavoritesBottomSheet(
    allVideos: List<VideoItem>,
    onToggleFavourite: (VideoItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterQuery by rememberSaveable { mutableStateOf("") }
    val filteredVideos = remember(allVideos, filterQuery) {
        LibraryViewModel.filterVideosByQuery(allVideos, filterQuery)
            .sortedWith(
                compareByDescending<VideoItem> { it.isFavourite }
                    .thenBy { it.title.lowercase() }
            )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("favorites_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.favorites_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.favorites_picker_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("favorites_picker_done_button")
                ) {
                    Text(text = stringResource(R.string.favorites_picker_done))
                }
            }

            if (allVideos.size > 3) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text(stringResource(R.string.favorites_picker_search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (filterQuery.isNotEmpty()) {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear filter"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorites_picker_search_input")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .testTag("favorites_picker_list"),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredVideos,
                    key = { it.id }
                ) { video ->
                    FavoritePickerRow(
                        video = video,
                        onToggleFavourite = { onToggleFavourite(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritePickerRow(
    video: VideoItem,
    onToggleFavourite: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    FazaGlassCard(
        shape = RoundedCornerShape(16.dp),
        onClick = onToggleFavourite,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("favorites_picker_row_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerHigh)
            ) {
                CachedVideoThumbnail(
                    thumbnailPath = video.thumbnailPath,
                    contentDescription = video.title,
                    decodeWidthPx = 220,
                    decodeHeightPx = 124,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.folderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorites_picker_toggle_${video.id}")
            ) {
                Icon(
                    imageVector = if (video.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (video.isFavourite) {
                        "Remove ${video.title} from favorites"
                    } else {
                        "Add ${video.title} to favorites"
                    },
                    tint = if (video.isFavourite) colors.error else colors.onSurfaceVariant
                )
            }
        }
    }
}
