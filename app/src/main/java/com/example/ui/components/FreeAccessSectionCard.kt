package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.FreeAccessStatus
import com.example.ui.theme.FazaNavActivePurpleDark
import com.example.ui.theme.FazaNavActivePurpleLight
import com.example.ui.theme.FazaNavOnActivePurple
import com.example.ui.theme.LocalFazaIsDark
import com.example.ui.viewmodel.FreeAccessFeedback
import com.example.ui.viewmodel.FreeAccessFeedbackType

@Composable
fun FreeAccessSectionCard(
    status: FreeAccessStatus,
    isOnline: Boolean,
    isLoadingAd: Boolean,
    feedback: FreeAccessFeedback?,
    onWatchRewardedAd: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDark = LocalFazaIsDark.current
    val accentPurple = if (isDark) FazaNavActivePurpleDark else FazaNavActivePurpleLight
    val animatedAdProgress by animateFloatAsState(
        targetValue = status.progressFraction,
        animationSpec = tween(durationMillis = 280),
        label = "free_access_reward_progress"
    )
    val animatedTimeProgress by animateFloatAsState(
        targetValue = status.remainingTimeFraction,
        animationSpec = tween(durationMillis = 280),
        label = "free_access_time_progress"
    )

    FazaGlassCard(
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_free_access_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FazaGlassIconBadge(
                    icon = if (status.isActive) Icons.Filled.VerifiedUser else Icons.Filled.WorkspacePremium,
                    size = 42.dp,
                    iconSize = 21.dp,
                    cornerRadius = 12.dp,
                    useSolidGradient = status.isActive
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.free_access_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("free_access_section_title")
                    )
                    Text(
                        text = if (status.isActive) {
                            stringResource(R.string.free_access_activated_label)
                        } else {
                            stringResource(R.string.free_access_option_title)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(
                            if (status.isActive) "free_access_activated_label" else "free_access_option_title"
                        )
                    )
                }

                when {
                    status.isActive -> {
                        Surface(
                            shape = CircleShape,
                            color = colors.primaryContainer,
                            modifier = Modifier.testTag("free_access_active_badge")
                        ) {
                            Text(
                                text = stringResource(R.string.free_access_status_active),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    status.isExpired -> {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceContainerHighest,
                            modifier = Modifier.testTag("free_access_expired_badge")
                        ) {
                            Text(
                                text = stringResource(R.string.free_access_status_expired),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (status.isActive) {
                // Active 24-Hour Countdown Panel
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerHigh,
                    border = BorderStroke(1.dp, accentPurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = accentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.free_access_remaining_time,
                                        status.remainingTimeText
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    modifier = Modifier.testTag("free_access_remaining_time")
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { animatedTimeProgress },
                            color = accentPurple,
                            trackColor = colors.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .testTag("free_access_countdown_bar")
                        )

                        Text(
                            text = stringResource(R.string.free_access_active_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Rewarded Ads 0/8 Progress Panel
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.free_access_progress_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = accentPurple.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = status.progressPercentText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentPurple,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("free_access_progress_percent")
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { animatedAdProgress },
                            color = accentPurple,
                            trackColor = colors.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .testTag("free_access_progress_bar")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.free_access_watched_count,
                                    status.clampedWatchedCount,
                                    status.requiredCount
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface,
                                modifier = Modifier.testTag("free_access_watched_count")
                            )
                            Text(
                                text = stringResource(
                                    R.string.free_access_remaining_ads,
                                    status.remainingAds
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.testTag("free_access_remaining_ads")
                            )
                        }
                    }
                }

                // Primary Action Button — Get 24 Hours Access / Watch Rewarded Ad
                Button(
                    onClick = onWatchRewardedAd,
                    enabled = isOnline && !isLoadingAd,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentPurple,
                        contentColor = FazaNavOnActivePurple,
                        disabledContainerColor = colors.surfaceContainerHighest,
                        disabledContentColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("free_access_watch_ad_button")
                ) {
                    when {
                        isLoadingAd -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.free_access_loading_ad),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        !isOnline -> {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.free_access_offline_button),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (status.clampedWatchedCount == 0) {
                                    stringResource(R.string.free_access_option_title)
                                } else {
                                    stringResource(
                                        R.string.free_access_watch_next_ad,
                                        status.clampedWatchedCount,
                                        status.requiredCount
                                    )
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isOnline) {
                    Text(
                        text = stringResource(R.string.free_access_offline_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.testTag("free_access_offline_note")
                    )
                }
            }

            // Inline Feedback Banner (confirmation / progress / retry note; hidden for errors while offline)
            val visibleFeedback = feedback?.takeIf {
                isOnline || it.type == FreeAccessFeedbackType.SUCCESS
            }
            AnimatedVisibility(
                visible = visibleFeedback != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                visibleFeedback?.let { item ->
                    val containerColor = when (item.type) {
                        FreeAccessFeedbackType.SUCCESS -> colors.primaryContainer
                        FreeAccessFeedbackType.INFO -> colors.secondaryContainer
                        FreeAccessFeedbackType.WARNING -> colors.surfaceContainerHighest
                    }
                    val contentColor = when (item.type) {
                        FreeAccessFeedbackType.SUCCESS -> colors.onPrimaryContainer
                        FreeAccessFeedbackType.INFO -> colors.onSecondaryContainer
                        FreeAccessFeedbackType.WARNING -> colors.onSurface
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("free_access_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (item.type == FreeAccessFeedbackType.SUCCESS) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Filled.Info
                                },
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = item.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("free_access_feedback_message")
                            )
                            IconButton(
                                onClick = onDismissFeedback,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.free_access_dismiss_feedback_cd),
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
