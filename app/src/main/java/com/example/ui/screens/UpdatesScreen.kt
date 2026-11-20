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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.data.model.VersionRelease
import androidx.compose.ui.graphics.Color
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.FazaGlassPillBadge
import com.example.ui.components.FazaGlassTopBarIconButton

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
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showEndpointDialog by remember { mutableStateOf(false) }

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
                            onClick = onBack
                        )
                    },
                    title = { Text("Updates & History", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        FazaGlassTopBarIconButton(
                            icon = Icons.Filled.Edit,
                            contentDescription = "Configure update server URL",
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
                    .widthIn(max = adaptiveSpec.maxContentWidth),
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
                        contentPadding = PaddingValues(20.dp)
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
                                            UpdateCheckState.Checking -> CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp
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
                                            UpdateCheckState.Checking -> "Checking for updates..."
                                            is UpdateCheckState.UpdateAvailable ->
                                                "Version ${checkState.manifest.latestVersion} available"
                                            is UpdateCheckState.UpToDate -> "Up to date (v${BuildConfig.VERSION_NAME})"
                                            is UpdateCheckState.Unavailable -> "Update server unavailable"
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = when (checkState) {
                                            is UpdateCheckState.Unavailable -> checkState.reason
                                            is UpdateCheckState.UpdateAvailable -> "Released ${checkState.manifest.releaseDate}"
                                            else -> manifestUrl
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCheckUpdates,
                                    enabled = checkState != UpdateCheckState.Checking,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("updates_check_now_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check Now")
                                }
                                if (checkState is UpdateCheckState.UpdateAvailable) {
                                    FilledTonalButton(
                                        onClick = onShowAvailableDialog,
                                        shape = CircleShape,
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("View Update")
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
            title = { Text("Update Host URL") },
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
