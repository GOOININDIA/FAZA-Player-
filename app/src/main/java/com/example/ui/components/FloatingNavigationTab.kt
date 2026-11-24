package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.VideoItem
import com.example.ui.theme.LocalFazaIsDark

data class FloatingNavTabItem(
    val id: String = "",
    val label: String,
    val showLabel: Boolean = true,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val selected: Boolean,
    val badgeCount: Int = 0,
    val contentDescription: String = label,
    val testTag: String,
    val onClick: () -> Unit
)

data class FloatingDockDimensions(
    val navBarWidth: Dp,
    val navBarHeight: Dp,
    val searchButtonSize: Dp,
    val searchCornerRadius: Dp,
    val horizontalGap: Dp
)

fun formatDockBadgeCount(count: Int): String = when {
    count <= 0 -> ""
    count > 99 -> "99+"
    else -> count.toString()
}

fun calculateFloatingDockDimensions(availableWidth: Dp): FloatingDockDimensions {
    val isUltraCompact = availableWidth < 350.dp
    val isWidePhone = availableWidth >= 460.dp
    val navHeight = when {
        isUltraCompact -> 56.dp
        isWidePhone -> 64.dp
        else -> 60.dp
    }
    val horizontalPadding = 32.dp
    val gap = if (isUltraCompact) 8.dp else 10.dp
    val searchSize = when {
        isUltraCompact -> 50.dp
        isWidePhone -> 62.dp
        else -> 58.dp
    }
    val searchRadius = if (isUltraCompact) 16.dp else 20.dp
    val maxUsableRow = (availableWidth - horizontalPadding).coerceAtLeast(220.dp)
    val targetNavWidth = (availableWidth * 0.66f).coerceIn(212.dp, 290.dp)
    val fittedNavWidth = targetNavWidth.coerceAtMost((maxUsableRow - searchSize - gap).coerceAtLeast(180.dp))
    return FloatingDockDimensions(
        navBarWidth = fittedNavWidth,
        navBarHeight = navHeight,
        searchButtonSize = searchSize,
        searchCornerRadius = searchRadius,
        horizontalGap = gap
    )
}

fun calculateFloatingDockWidth(availableWidth: Dp): Dp =
    calculateFloatingDockDimensions(availableWidth).navBarWidth

fun selectLastPlayedVideo(
    recentVideos: List<VideoItem>,
    allPublicVideos: List<VideoItem>
): VideoItem? {
    val recentMatch = recentVideos.firstOrNull { !it.isPrivate }
    if (recentMatch != null) return recentMatch
    return allPublicVideos
        .filter { !it.isPrivate && (it.lastPlayedAt != null || it.lastPositionMs > 0L) }
        .maxByOrNull { it.lastPlayedAt ?: 0L }
}

fun isVideoFinished(video: VideoItem): Boolean {
    if (video.durationMs <= 0L) return false
    val endThresholdMs = if (video.durationMs > 3_000L) video.durationMs - 2_500L else video.durationMs
    return video.lastPositionMs >= endThresholdMs
}

fun isVideoEligibleForAutoResume(video: VideoItem): Boolean {
    if (video.isPrivate) return false
    if (video.lastPositionMs < 1_000L) return false
    if (isVideoFinished(video)) return false
    return true
}

fun selectAutoResumeVideo(
    recentVideos: List<VideoItem>,
    allPublicVideos: List<VideoItem>
): VideoItem? {
    val candidate = selectLastPlayedVideo(recentVideos, allPublicVideos) ?: return null
    return candidate.takeIf { isVideoEligibleForAutoResume(it) }
}

fun isVideoEligibleForContinuePlaying(
    video: VideoItem,
    isCurrentlyPlaying: Boolean = false,
    endedVideoId: Long? = null
): Boolean {
    if (video.isPrivate) return false
    if (endedVideoId != null && video.id == endedVideoId) return false
    if (isVideoFinished(video)) return false
    if (isCurrentlyPlaying) return true
    return video.lastPositionMs > 0L
}

fun selectContinuePlayingVideo(
    recentVideos: List<VideoItem>,
    allPublicVideos: List<VideoItem>,
    activePlayingVideoId: Long? = null,
    endedVideoId: Long? = null
): VideoItem? {
    val candidate = selectLastPlayedVideo(recentVideos, allPublicVideos) ?: return null
    val isCurrentlyPlaying = activePlayingVideoId != null && candidate.id == activePlayingVideoId
    return candidate.takeIf {
        isVideoEligibleForContinuePlaying(
            video = it,
            isCurrentlyPlaying = isCurrentlyPlaying,
            endedVideoId = endedVideoId
        )
    }
}

@Immutable
private data class FloatingDockPalette(
    val dockBackground: Color,
    val dockSurfaceTop: Color,
    val dockSurfaceBottom: Color,
    val dockBorderStart: Color,
    val dockBorderEnd: Color,
    val activeCapsuleColor: Color,
    val activeCapsuleEnd: Color,
    val activeContentColor: Color,
    val inactiveIconColor: Color,
    val searchBackground: Color,
    val searchIconColor: Color,
    val searchBorderStart: Color,
    val searchBorderEnd: Color,
    val badgeContainerColor: Color,
    val badgeContentColor: Color
)

@Composable
private fun rememberFloatingDockPalette(): FloatingDockPalette {
    val colors = MaterialTheme.colorScheme
    val isDark = LocalFazaIsDark.current
    return remember(colors, isDark) {
        FloatingDockPalette(
            dockBackground = colors.surfaceContainerLow,
            dockSurfaceTop = colors.surfaceContainerLow,
            dockSurfaceBottom = colors.surfaceContainerLow,
            dockBorderStart = colors.outlineVariant.copy(alpha = if (isDark) 0.7f else 0.85f),
            dockBorderEnd = colors.outlineVariant.copy(alpha = if (isDark) 0.7f else 0.85f),
            activeCapsuleColor = colors.primary.copy(alpha = if (isDark) 0.16f else 0.12f),
            activeCapsuleEnd = colors.primary.copy(alpha = if (isDark) 0.16f else 0.12f),
            activeContentColor = colors.primary,
            inactiveIconColor = colors.onSurfaceVariant,
            searchBackground = colors.surfaceContainerLow,
            searchIconColor = colors.onSurface,
            searchBorderStart = colors.outlineVariant.copy(alpha = if (isDark) 0.7f else 0.85f),
            searchBorderEnd = colors.outlineVariant.copy(alpha = if (isDark) 0.7f else 0.85f),
            badgeContainerColor = colors.error,
            badgeContentColor = colors.onError
        )
    }
}

@Composable
fun FloatingNavigationDock(
    items: List<FloatingNavTabItem>,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = rememberFloatingDockPalette()
    val pillShape = CircleShape

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val dims = calculateFloatingDockDimensions(maxWidth)
        val isCompact = maxWidth < 360.dp
        val iconSize = if (isCompact) 19.dp else 21.dp
        val labelFontSize = if (isCompact) 13.sp else 14.sp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.horizontalGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean Minimalist Capsule Navigation Bar
            Surface(
                shape = pillShape,
                color = palette.dockBackground,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, palette.dockBorderStart),
                shadowElevation = 2.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .width(dims.navBarWidth)
                    .height(dims.navBarHeight)
                    .clip(pillShape)
                    .testTag("floating_navigation_dock")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("floating_home_tab_bar"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val anyNonLabeledSelected = items.any { it.selected && !it.showLabel }
                    items.forEach { item ->
                        val targetWeight = when {
                            item.showLabel && item.selected -> 2.0f
                            item.showLabel && !anyNonLabeledSelected -> 2.0f
                            item.showLabel -> 1.45f
                            item.selected -> 1.45f
                            else -> 1.0f
                        }
                        val animatedWeight by animateFloatAsState(
                            targetValue = targetWeight,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tab_weight_${item.id}"
                        )

                        FloatingDockTabButton(
                            item = item,
                            palette = palette,
                            iconSize = iconSize,
                            labelFontSize = labelFontSize,
                            modifier = Modifier
                                .weight(animatedWeight)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // Separate Compact Floating Rounded-Square Search Button
            if (onSearchClick != null) {
                FloatingSearchSquareButton(
                    palette = palette,
                    size = dims.searchButtonSize,
                    cornerRadius = dims.searchCornerRadius,
                    iconSize = if (isCompact) 21.dp else 23.dp,
                    isActive = isSearchActive,
                    onClick = onSearchClick
                )
            }
        }
    }
}

@Composable
private fun FloatingDockTabButton(
    item: FloatingNavTabItem,
    palette: FloatingDockPalette,
    iconSize: Dp,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_scale_${item.id}"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (item.selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_bg_alpha_${item.id}"
    )

    val contentTint by animateColorAsState(
        targetValue = if (item.selected) palette.activeContentColor else palette.inactiveIconColor,
        animationSpec = tween(180),
        label = "dock_tint_${item.id}"
    )

    val activePillColor = palette.activeCapsuleColor.copy(alpha = pillAlpha)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(activePillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = item.onClick
            )
            .semantics {
                contentDescription = if (item.badgeCount > 0) {
                    "${item.contentDescription}, ${item.badgeCount} unread notification${if (item.badgeCount == 1) "" else "s"}"
                } else {
                    item.contentDescription
                }
                role = Role.Tab
                selected = item.selected
            }
            .padding(horizontal = if (item.showLabel) 10.dp else 4.dp)
            .testTag(item.testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(iconSize)
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = item.badgeCount > 0,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(tween(150)),
                    exit = scaleOut(tween(120)) + fadeOut(tween(120)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-4).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.badgeContainerColor)
                            .border(1.5.dp, if (item.selected) palette.activeCapsuleColor else palette.dockBackground, CircleShape)
                            .testTag("${item.testTag}_badge")
                    )
                }
            }
            if (item.showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    color = contentTint,
                    fontSize = labelFontSize,
                    fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FloatingSearchSquareButton(
    palette: FloatingDockPalette,
    size: Dp,
    cornerRadius: Dp,
    iconSize: Dp,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_search_scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isActive) palette.activeCapsuleColor else palette.searchBackground,
        animationSpec = tween(180),
        label = "floating_search_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isActive) palette.activeContentColor else palette.searchIconColor,
        animationSpec = tween(180),
        label = "floating_search_fg"
    )

    val shape = CircleShape

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = iconColor,
        border = BorderStroke(1.dp, palette.searchBorderStart),
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .size(size)
            .minimumInteractiveComponentSize()
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .semantics {
                contentDescription = "Search videos"
                role = Role.Button
            }
            .testTag("floating_search_button")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun FloatingHomeTabBar(
    items: List<FloatingNavTabItem>,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    FloatingNavigationDock(
        items = items,
        onSearchClick = onSearchClick,
        isSearchActive = isSearchActive,
        modifier = modifier
    )
}

@Composable
fun FloatingHomeNavigationRail(
    items: List<FloatingNavTabItem>,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = rememberFloatingDockPalette()
    val railShape = RoundedCornerShape(20.dp)

    Surface(
        shape = railShape,
        color = palette.dockBackground,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, palette.dockBorderStart),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            .fillMaxHeight()
            .clip(railShape)
            .testTag("floating_home_nav_rail")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
            val isShortRail = maxHeight < 420.dp
            NavigationRail(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = if (isShortRail) 6.dp else 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(if (isShortRail) 4.dp else 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items.forEach { item ->
                            NavigationRailItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                icon = {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Icon(
                                            imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.contentDescription
                                        )
                                        if (item.badgeCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = 5.dp, y = (-4).dp)
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.badgeContainerColor)
                                            )
                                        }
                                    }
                                },
                                label = if (item.showLabel) {
                                    {
                                        Text(
                                            text = item.label,
                                            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    }
                                } else {
                                    null
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = palette.activeContentColor,
                                    selectedTextColor = palette.activeCapsuleColor,
                                    indicatorColor = palette.activeCapsuleColor,
                                    unselectedIconColor = palette.inactiveIconColor,
                                    unselectedTextColor = palette.inactiveIconColor
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }

                    if (onSearchClick != null) {
                        FloatingSearchSquareButton(
                            palette = palette,
                            size = if (isShortRail) 48.dp else 52.dp,
                            cornerRadius = 16.dp,
                            iconSize = 22.dp,
                            isActive = isSearchActive,
                            onClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingContinuePlayButton(
    video: VideoItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 56.dp
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_continue_play_scale"
    )
    val buttonElevation by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 8.dp,
        animationSpec = tween(140),
        label = "floating_continue_play_elevation"
    )

    // Decode the exact uploaded PNG at display-safe resolution without modifying color, rotation, or aspect ratio
    val playIconBitmap = remember(context) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_continue_playing_icon, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxDim / (sampleSize * 2) >= 256) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_continue_playing_icon, decodeOpts)
                ?.asImageBitmap()
        }.getOrNull()
    }

    val colors = MaterialTheme.colorScheme
    val isDark = LocalFazaIsDark.current
    val buttonShape = RoundedCornerShape(18.dp)
    val containerColor = if (isDark) colors.onPrimaryContainer else colors.surfaceContainerLowest
    val borderColor = if (isDark) {
        colors.primary.copy(alpha = 0.28f)
    } else {
        colors.outlineVariant.copy(alpha = 0.72f)
    }
    val accessibilityLabel = if (video != null) {
        stringResource(R.string.continue_playing_video_cd, video.title)
    } else {
        stringResource(R.string.continue_playing_cd)
    }

    Surface(
        shape = buttonShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = buttonElevation,
        tonalElevation = 4.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .size(buttonSize.coerceAtLeast(48.dp))
            .minimumInteractiveComponentSize()
            .clip(buttonShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primary),
                onClick = onClick
            )
            .semantics {
                contentDescription = accessibilityLabel
                role = Role.Button
            }
            .testTag("floating_continue_play_button")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (playIconBitmap != null) {
                Image(
                    bitmap = playIconBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("floating_continue_play_icon")
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_continue_playing_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("floating_continue_play_icon")
                )
            }
        }
    }
}
