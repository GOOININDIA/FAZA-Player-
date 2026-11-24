package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.model.ResolutionTier
import com.example.data.model.VideoItem
import com.example.util.Formatters
import java.io.File

private val ThumbVignetteBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.44f))
)

/**
 * Reusable cached Coil thumbnail view with zero main-thread File.exists() I/O.
 */
@Composable
fun CachedVideoThumbnail(
    thumbnailPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    decodeWidthPx: Int = 360,
    decodeHeightPx: Int = 202
) {
    val context = LocalContext.current
    val trimmedPath = thumbnailPath?.takeIf { it.isNotBlank() }

    if (trimmedPath != null) {
        val request = remember(trimmedPath, decodeWidthPx, decodeHeightPx) {
            ImageRequest.Builder(context)
                .data(File(trimmedPath))
                .size(decodeWidthPx, decodeHeightPx)
                .precision(Precision.INEXACT)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey("thumb_${trimmedPath}_${decodeWidthPx}x${decodeHeightPx}")
                .placeholder(R.drawable.img_hero_cinema)
                .error(R.drawable.img_hero_cinema)
                .fallback(R.drawable.img_hero_cinema)
                .crossfade(120)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_hero_cinema),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun ResolutionBadge(tier: ResolutionTier, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isUltra = tier == ResolutionTier.UHD_4K || tier == ResolutionTier.UHD_8K

    val containerColor = if (isUltra) {
        colors.primary.copy(alpha = 0.12f)
    } else {
        colors.surfaceContainerHigh
    }
    val textColor = if (isUltra) colors.primary else colors.onSurfaceVariant

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = tier.badge,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun VideoListCard(
    video: VideoItem,
    onPlayClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onTogglePrivate: () -> Unit,
    onRemove: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val thumbWidth = if (isCompact) 100.dp else 120.dp
    val cardPadding = if (isCompact) 10.dp else 12.dp
    val innerGap = if (isCompact) 10.dp else 14.dp
    val durationLabel = remember(video.durationMs) {
        if (video.durationMs > 0L) Formatters.formatDuration(video.durationMs) else ""
    }
    val metaLabel = remember(video.formatTag, video.sizeBytes) {
        "${video.formatTag} · ${Formatters.formatFileSize(video.sizeBytes)}"
    }
    val favInteractionSource = remember { MutableInteractionSource() }
    val favBounceScale = rememberFazaBounceScale(
        interactionSource = favInteractionSource,
        enabled = true,
        pressedScale = 0.84f
    )

    FazaGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        onClick = onPlayClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(thumbWidth)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainerHigh)
            ) {
                CachedVideoThumbnail(
                    thumbnailPath = video.thumbnailPath,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ThumbVignetteBrush)
                )

                if (durationLabel.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = durationLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (video.progressFraction > 0.02f) {
                    LinearProgressIndicator(
                        progress = { video.progressFraction },
                        color = colors.primary,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(innerGap))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResolutionBadge(tier = video.resolutionTier)
                    Text(
                        text = metaLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (video.isPrivate) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Private video",
                            tint = colors.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleFavourite,
                interactionSource = favInteractionSource,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .graphicsLayer {
                        scaleX = favBounceScale
                        scaleY = favBounceScale
                    }
                    .testTag("fav_button_${video.id}")
            ) {
                Icon(
                    imageVector = if (video.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (video.isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (video.isFavourite) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("more_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (video.isPrivate) "Remove from Private" else "Move to Private Vault") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (video.isPrivate) Icons.Outlined.LockOpen else Icons.Filled.Lock,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onTogglePrivate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Library") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = colors.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentVideoChipCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val durationLabel = remember(video.durationMs) {
        if (video.durationMs > 0L) Formatters.formatDuration(video.durationMs) else ""
    }

    FazaGlassCard(
        modifier = modifier
            .width(184.dp)
            .testTag("recent_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surfaceContainerHigh)
        ) {
            CachedVideoThumbnail(
                thumbnailPath = video.thumbnailPath,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThumbVignetteBrush)
            )
            if (durationLabel.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (video.progressFraction > 0.02f) {
                LinearProgressIndicator(
                    progress = { video.progressFraction },
                    color = colors.primary,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.5.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                text = "${video.resolutionTier.badge} · ${video.formatTag}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
