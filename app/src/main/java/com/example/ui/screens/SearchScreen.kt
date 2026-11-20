package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ResolutionTier
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import androidx.compose.ui.graphics.Color
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaGlassIconBadge
import com.example.ui.components.FazaGlassTopBarIconButton
import com.example.ui.components.HomeMaterialSearchBar
import com.example.ui.components.VideoListCard

enum class SearchQuickFilter(val label: String) {
    ALL("All"),
    ULTRA_HD("4K / 8K"),
    FULL_HD("1080p"),
    MKV("MKV"),
    MP4("MP4"),
    FAVOURITES("Favourites"),
    STREAMS("Streams")
}

fun applySearchQuickFilter(videos: List<VideoItem>, filter: SearchQuickFilter): List<VideoItem> =
    when (filter) {
        SearchQuickFilter.ALL -> videos
        SearchQuickFilter.ULTRA_HD -> videos.filter {
            it.resolutionTier == ResolutionTier.UHD_4K || it.resolutionTier == ResolutionTier.UHD_8K
        }
        SearchQuickFilter.FULL_HD -> videos.filter { it.resolutionTier == ResolutionTier.FHD_1080P }
        SearchQuickFilter.MKV -> videos.filter { it.formatTag.equals("MKV", ignoreCase = true) }
        SearchQuickFilter.MP4 -> videos.filter { it.formatTag.equals("MP4", ignoreCase = true) }
        SearchQuickFilter.FAVOURITES -> videos.filter { it.isFavourite }
        SearchQuickFilter.STREAMS -> videos.filter {
            !it.isDownloaded ||
                it.uri.startsWith("http://", ignoreCase = true) ||
                it.uri.startsWith("https://", ignoreCase = true) ||
                it.uri.startsWith("rtsp://", ignoreCase = true) ||
                it.uri.startsWith("rtmp://", ignoreCase = true)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    videos: List<VideoItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavourite: (VideoItem) -> Unit,
    onTogglePrivate: (VideoItem) -> Unit,
    onRemoveVideo: (Long) -> Unit,
    onPickVideosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(SearchQuickFilter.ALL) }

    BackHandler {
        if (searchQuery.isNotEmpty() || selectedFilter != SearchQuickFilter.ALL) {
            onSearchQueryChange("")
            selectedFilter = SearchQuickFilter.ALL
        } else {
            onBack()
        }
    }

    val filteredResults = remember(videos, selectedFilter) {
        applySearchQuickFilter(videos, selectedFilter)
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                            contentDescription = "Back",
                            onClick = onBack,
                            modifier = Modifier.testTag("search_back_button")
                        )
                    },
                    title = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        VideoSortPillButton(
                            selectedSort = sortOption,
                            onSelectSort = onSortChange,
                            testTagPrefix = "search",
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = adaptiveSpec.maxContentWidth)
            ) {
                HomeMaterialSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    resultCount = filteredResults.size,
                    modifier = Modifier.padding(horizontal = adaptiveSpec.horizontalPadding, vertical = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = adaptiveSpec.horizontalPadding, vertical = 6.dp)
                ) {
                    items(SearchQuickFilter.entries, key = { it.name }) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                selectedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("search_filter_${filter.name.lowercase()}")
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(adaptiveSpec.videoGridColumns),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("search_results_list"),
                    contentPadding = PaddingValues(
                        start = adaptiveSpec.horizontalPadding,
                        end = adaptiveSpec.horizontalPadding,
                        top = 6.dp,
                        bottom = 24.dp + navBarBottom
                    ),
                    horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                    verticalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                ) {
                    item(key = "search_summary", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = if (searchQuery.isBlank() && selectedFilter == SearchQuickFilter.ALL) {
                                "${filteredResults.size} video${if (filteredResults.size == 1) "" else "s"} in library"
                            } else {
                                "${filteredResults.size} matching video${if (filteredResults.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("search_result_count")
                        )
                    }

                    if (filteredResults.isEmpty()) {
                        item(key = "search_empty", span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = if (adaptiveSpec.isShortHeight) 24.dp else 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FazaGlassIconBadge(
                                    icon = if (videos.isEmpty()) Icons.Filled.Search else Icons.Outlined.SearchOff,
                                    size = 64.dp,
                                    iconSize = 30.dp,
                                    cornerRadius = 20.dp,
                                    useSolidGradient = true
                                )
                                Text(
                                    text = if (searchQuery.isBlank() && selectedFilter == SearchQuickFilter.ALL) {
                                        "No videos in your library yet"
                                    } else {
                                        "No matching videos"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (searchQuery.isBlank() && selectedFilter == SearchQuickFilter.ALL) {
                                        "Add videos or link a folder from Home to search by title, folder, format, or resolution."
                                    } else {
                                        "Try clearing filters or searching by video name, folder, format (MKV, MP4), or resolution (4K, 1080p)."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (searchQuery.isNotBlank() || selectedFilter != SearchQuickFilter.ALL) {
                                    FilledTonalButton(
                                        onClick = {
                                            onSearchQueryChange("")
                                            selectedFilter = SearchQuickFilter.ALL
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Reset Search & Filters")
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = onPickVideosClick,
                                        shape = CircleShape,
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Pick Videos")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredResults, key = { it.id }) { video ->
                            VideoListCard(
                                video = video,
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
