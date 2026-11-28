package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.UpdateCheckState
import com.example.data.model.UpdateManifest
import com.example.data.model.UserPreferences
import com.example.data.model.VersionRelease
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassPillBadge
import com.example.ui.components.FazaGlassTopBarIconButton
import com.example.ui.components.FazaRingLoader
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UpdatesScreen(
    checkState: UpdateCheckState,
    versionHistory: List<VersionRelease>,
    manifestUrl: String,
    onBack: () -> Unit,
    onCheckUpdates: () -> Unit,
    onShowAvailableDialog: () -> Unit,
    onSaveManifestUrl: (String) -> Unit,
    onDownloadUpdate: (UpdateManifest) -> Unit = {},
    lastCheckedAtMillis: Long? = null,
    periodicIntervalMinutes: Int = 30,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showEndpointDialog by remember { mutableStateOf(false) }

    val formattedLastChecked = remember(lastCheckedAtMillis) {
        lastCheckedAtMillis?.let { ts ->
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ts))
        }
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
                    navigationIcon = {
                        FazaGlassTopBarIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            modifier = Modifier.testTag("updates_back_button")
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.update_screen_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        FazaGlassTopBarIconButton(
                            icon = Icons.Filled.Edit,
                            contentDescription = "Configure fallback update server URL",
                            onClick = { showEndpointDialog = true }
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
                val historyColumns = if (adaptiveSpec.useTwoPane) 2 else 1

                LazyVerticalGrid(
                    columns = GridCells.Fixed(historyColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = adaptiveSpec.maxContentWidth)
                        .testTag("updates_screen_grid"),
                    contentPadding = PaddingValues(
                        horizontal = adaptiveSpec.horizontalPadding,
                        vertical = adaptiveSpec.verticalPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing),
                    verticalArrangement = Arrangement.spacedBy(adaptiveSpec.contentSpacing)
                ) {
                    item(key = "updates_status_card", span = { GridItemSpan(maxLineSpan) }) {
                        FazaGlassCard(
                            shape = RoundedCornerShape(28.dp),
                            accented = true,
                            contentPadding = PaddingValues(20.dp),
                            modifier = Modifier.testTag("updates_status_card")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            when (checkState) {
                                                UpdateCheckState.Checking -> FazaRingLoader(
                                                    contentDescription = "Checking for updates",
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .testTag("updates_checking_loader")
                                                )
                                                is UpdateCheckState.UpdateAvailable -> Icon(
                                                    Icons.Filled.SystemUpdate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                is UpdateCheckState.UpToDate -> Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                is UpdateCheckState.Unavailable -> Icon(
                                                    Icons.Filled.CloudOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                UpdateCheckState.Idle -> Icon(
                                                    Icons.Filled.SystemUpdate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (checkState) {
                                                UpdateCheckState.Idle -> "FAZA Player v${BuildConfig.VERSION_NAME}"
                                                UpdateCheckState.Checking -> "Checking Firebase Remote Config..."
                                                is UpdateCheckState.UpdateAvailable ->
                                                    "Version ${checkState.manifest.latestVersion} available"
                                                is UpdateCheckState.UpToDate -> "Up to date (v${BuildConfig.VERSION_NAME})"
                                                is UpdateCheckState.Unavailable -> "Update check unavailable"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = when (checkState) {
                                                is UpdateCheckState.Unavailable -> checkState.reason
                                                is UpdateCheckState.UpdateAvailable ->
                                                    "${checkState.manifest.sourceLabel} · Released ${checkState.manifest.releaseDate}"
                                                is UpdateCheckState.UpToDate -> checkState.sourceLabel
                                                else -> stringResource(R.string.update_screen_source_remote_config)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Periodic check telemetry row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("updates_periodic_status_text")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = stringResource(
                                                R.string.update_screen_periodic_status,
                                                periodicIntervalMinutes
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (formattedLastChecked != null) {
                                            Text(
                                                text = stringResource(
                                                    R.string.update_screen_last_checked,
                                                    formattedLastChecked
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (checkState is UpdateCheckState.UpdateAvailable) {
                                        Button(
                                            onClick = { onDownloadUpdate(checkState.manifest) },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("updates_download_button")
                                        ) {
                                            Icon(
                                                Icons.Filled.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.update_screen_download_cta),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = onCheckUpdates,
                                            enabled = true,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("updates_check_now_button")
                                        ) {
                                            Icon(
                                                Icons.Filled.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Re-check")
                                        }
                                    } else {
                                        Button(
                                            onClick = onCheckUpdates,
                                            enabled = checkState != UpdateCheckState.Checking,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("updates_check_now_button")
                                        ) {
                                            Icon(
                                                Icons.Filled.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Check Now")
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Text(
                                    text = "Contact & Privacy: ${stringResource(R.string.developer_email)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("updates_contact_privacy_text")
                                )
                            }
                        }
                    }

                    // Dedicated Incoming Update Changelog & Download Card when a newer build is detected
                    if (checkState is UpdateCheckState.UpdateAvailable) {
                        item(key = "updates_available_changelog_card", span = { GridItemSpan(maxLineSpan) }) {
                            AvailableUpdateChangelogCard(
                                manifest = checkState.manifest,
                                onDownloadUpdate = { onDownloadUpdate(checkState.manifest) },
                                onOpenSummaryDialog = onShowAvailableDialog
                            )
                        }
                    }

                    item(key = "updates_history_header", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Version History",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(versionHistory, key = { "${it.version}_${it.versionCode}" }) { release ->
                        VersionHistoryCard(release = release)
                    }
                }
            }
        }
    }

    if (showEndpointDialog) {
        var editedUrl by remember { mutableStateOf(manifestUrl) }
        AlertDialog(
            onDismissRequest = { showEndpointDialog = false },
            title = { Text("Fallback Update Host URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedUrl,
                        onValueChange = { editedUrl = it },
                        label = { Text("GitHub Repo or Manifest JSON URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { editedUrl = UserPreferences.DEFAULT_UPDATE_MANIFEST_URL }
                    ) {
                        Text("Reset to Official GitHub Repo")
                    }
                }
            },
            confirmButton = {
                Button(
                    shape = CircleShape,
                    onClick = {
                        showEndpointDialog = false
                        onSaveManifestUrl(editedUrl)
                    }
                ) {
                    Text("Save & Check")
                }
            },
            dismissButton = {
                TextButton(
                    shape = CircleShape,
                    onClick = { showEndpointDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AvailableUpdateChangelogCard(
    manifest: UpdateManifest,
    onDownloadUpdate: () -> Unit,
    onOpenSummaryDialog: () -> Unit
) {
    val cleanLatest = manifest.latestVersion.removePrefix("v").removePrefix("V")
    val latestDisplay = if (manifest.latestVersionCode > 0) {
        "v$cleanLatest (#${manifest.latestVersionCode})"
    } else {
        "v$cleanLatest"
    }
    val currentDisplay = "v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})"

    FazaGlassCard(
        shape = RoundedCornerShape(28.dp),
        accented = true,
        highlightedBorder = false,
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("updates_available_changelog_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_screen_whats_new_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Released ${manifest.releaseDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FazaGlassPillBadge(
                    text = manifest.sourceLabel.ifBlank { "Remote Config" },
                    useBlueAccent = true
                )
            }

            // Version comparison bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Installed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentDisplay,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
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
                            .size(18.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "New Version",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = latestDisplay,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("updates_available_version_label")
                        )
                    }
                }
            }

            if (manifest.summary.isNotBlank()) {
                Text(
                    text = manifest.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (manifest.newFeatures.isNotEmpty()) {
                ChangelogSectionBlock(
                    heading = "New Features",
                    icon = Icons.Filled.NewReleases,
                    bullets = manifest.newFeatures
                )
            }
            if (manifest.performanceImprovements.isNotEmpty()) {
                ChangelogSectionBlock(
                    heading = "Performance Improvements",
                    icon = Icons.Filled.Speed,
                    bullets = manifest.performanceImprovements
                )
            }
            if (manifest.bugFixes.isNotEmpty()) {
                ChangelogSectionBlock(
                    heading = "Bug Fixes",
                    icon = Icons.Filled.Build,
                    bullets = manifest.bugFixes
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDownloadUpdate,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("updates_changelog_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.update_screen_download_cta),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onOpenSummaryDialog,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(0.7f)
                        .height(48.dp)
                        .testTag("updates_view_summary_button")
                ) {
                    Text(
                        text = "Summary",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangelogSectionBlock(
    heading: String,
    icon: ImageVector,
    bullets: List<String>
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    text = heading,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VersionHistoryCard(release: VersionRelease) {
    FazaGlassCard(
        shape = RoundedCornerShape(28.dp),
        accented = release.isInstalledBuild,
        highlightedBorder = false,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Version ${release.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (release.versionCode > 0) {
                            "${release.releaseDate} · Build ${release.versionCode}"
                        } else {
                            release.releaseDate
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (release.isInstalledBuild) {
                    FazaGlassPillBadge(text = "INSTALLED", useBlueAccent = true)
                }
            }

            if (release.summary.isNotBlank()) {
                Text(
                    text = release.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (release.newFeatures.isNotEmpty()) {
                ReleaseBulletGroup("New Features", release.newFeatures)
            }
            if (release.performanceImprovements.isNotEmpty()) {
                ReleaseBulletGroup("Performance", release.performanceImprovements)
            }
            if (release.bugFixes.isNotEmpty()) {
                ReleaseBulletGroup("Bug Fixes", release.bugFixes)
            }
        }
    }
}

@Composable
private fun ReleaseBulletGroup(heading: String, bullets: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(heading, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        bullets.forEach { item ->
            Text(
                text = "· $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
