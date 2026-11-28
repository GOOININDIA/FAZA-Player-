package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.UpdateManifest
import com.example.update.AppUpdateManager

@Composable
fun UpdateNotificationBanner(
    manifest: UpdateManifest?,
    isChecking: Boolean,
    onRefreshUpdate: (UpdateManifest) -> Unit,
    onViewDetails: () -> Unit,
    onRecheckVersion: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = manifest != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        if (manifest != null) {
            val remoteVersionLabel = buildString {
                if (manifest.latestVersion.isNotBlank()) {
                    append("v${manifest.latestVersion.removePrefix("v").removePrefix("V")}")
                } else {
                    append("New Build")
                }
                if (manifest.latestVersionCode > 0) {
                    append(" (#${manifest.latestVersionCode})")
                }
            }
            val currentVersionLabel = "v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})"
            val subtitleText = when {
                manifest.sourceLabel.equals(AppUpdateManager.SOURCE_LABEL_REMOTE_CONFIG, ignoreCase = true) ->
                    stringResource(R.string.update_banner_remote_config_subtitle)
                manifest.sourceLabel.startsWith("GitHub") ->
                    "New release available · Tap to view changelog & update"
                else ->
                    "New version detected · Tap to view changelog & update"
            }

            val bannerShape = RoundedCornerShape(28.dp)

            FazaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("update_notification_banner"),
                shape = bannerShape,
                accented = true,
                highlightedBorder = false,
                onClick = onViewDetails,
                contentPadding = PaddingValues(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row: Badge + Title + Re-check & Dismiss actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FazaGlassIconBadge(
                            icon = Icons.Filled.SystemUpdate,
                            size = 42.dp,
                            iconSize = 20.dp,
                            cornerRadius = 21.dp,
                            useSolidGradient = true
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.update_banner_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FazaGlassPillBadge(
                                    text = manifest.sourceLabel.ifBlank { "Update" },
                                    useBlueAccent = true
                                )
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onRecheckVersion,
                            enabled = !isChecking,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("update_banner_recheck_button")
                        ) {
                            if (isChecking) {
                                FazaRingLoader(
                                    contentDescription = "Checking for updates",
                                    modifier = Modifier
                                        .size(22.dp)
                                        .testTag("update_banner_checking_loader")
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Re-check for updates",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (!manifest.isMandatory) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("update_banner_dismiss_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss update banner",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Version comparison card: Current vs Remote Latest
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentVersionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(16.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Remote Latest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = remoteVersionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    val highlightNote = manifest.newFeatures.firstOrNull()
                        ?: manifest.performanceImprovements.firstOrNull()
                        ?: manifest.bugFixes.firstOrNull()
                    if (!highlightNote.isNullOrBlank()) {
                        Text(
                            text = "What's new: $highlightNote",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    // Primary Actions: Download Update / View Changelog screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onRefreshUpdate(manifest) },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("update_banner_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.update_banner_download_cta),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = onViewDetails,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(0.85f)
                                .height(48.dp)
                                .testTag("update_banner_details_button")
                        ) {
                            Text(
                                text = stringResource(R.string.update_banner_changelog_cta),
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
