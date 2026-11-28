package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.LibrarySection
import com.example.data.model.SortOption
import com.example.data.model.ThemeMode
import com.example.data.model.UpdateManifest
import com.example.data.model.VideoItem
import com.example.ui.components.FazaBrandLogo
import com.example.ui.components.FazaGlassCard
import com.example.ui.components.RecentVideoChipCard
import com.example.ui.components.TopRightSettingsButton
import com.example.ui.components.UpdateNotificationBanner
import com.example.ui.components.VideoListCard
import com.example.ui.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    themeMode: ThemeMode,
    expectedPin: String?,
    hasUpdateAvailable: Boolean = false,
    updateBannerManifest: UpdateManifest? = null,
    isCheckingUpdates: Boolean = false,
    currentVersionLabel: String = "v1.9 beta",
    onOpenUpdatePopup: () -> Unit = {},
    onRefreshUpdate: (UpdateManifest) -> Unit = {},
    onViewUpdateDetails: () -> Unit = {},
    onRecheckUpdates: () -> Unit = {},
    onDismissUpdateBanner: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectSection: (LibrarySection) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onPickVideosClick: () -> Unit,
    onOpenDocumentClick: () -> Unit,
    onLinkFolderClick: () -> Unit,
    onAutoDetectVideos: (Boolean) -> Unit,
    onAddStreamUrl: (String, String?) -> Unit,
    onUnlockPrivate: (String) -> Boolean,
    onLockPrivate: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleFavourite: (VideoItem) -> Unit,
    onTogglePrivate: (VideoItem) -> Unit,
    onRemoveVideo: (Long) -> Unit,
    onClearStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchVisible by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showStreamDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onAutoDetectVideos(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearStatus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPickVideosClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Videos") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("hero_pick_videos_button")
            )
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    FazaBrandLogo()
                },
                actions = {
                    IconButton(
                        onClick = { onAutoDetectVideos(true) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("auto_detect_videos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Scan device folders"
                        )
                    }

                    IconButton(
                        onClick = { searchVisible = !searchVisible },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search videos"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort videos"
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == state.sortOption) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (option == state.sortOption) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        sortMenuExpanded = false
                                        onSortChange(option)
                                    }
                                )
                            }
                        }
                    }

                    TopRightSettingsButton(
                        onClick = onOpenSettings,
                        hasUpdateBadge = hasUpdateAvailable
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AnimatedVisibility(visible = state.isImporting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                item {
                    UpdateNotificationBanner(
                        manifest = updateBannerManifest,
                        isChecking = isCheckingUpdates,
                        onRefreshUpdate = onRefreshUpdate,
                        onViewDetails = onViewUpdateDetails,
                        onRecheckVersion = onRecheckUpdates,
                        onDismiss = onDismissUpdateBanner,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    AnimatedVisibility(visible = searchVisible) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search videos, folders, formats...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("search_input")
                        )
                    }
                }

                // Material You Tonal Action Pills
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilledTonalButton(
                                onClick = onLinkFolderClick,
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("hero_link_folder_button")
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Link Folder")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = onOpenDocumentClick,
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("open_document_chip")
                            ) {
                                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open File")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { showStreamDialog = true },
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("open_stream_chip")
                            ) {
                                Icon(Icons.Filled.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stream URL")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = onToggleTheme,
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (themeMode == ThemeMode.DARK) "Light" else "Dark")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = onOpenUpdatePopup,
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("home_version_popup_chip")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NewReleases,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(currentVersionLabel)
                            }
                        }
                    }
                }

                // Material You Filter Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(LibrarySection.entries) { section ->
                            val selected = state.activeSection == section
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (section == LibrarySection.PRIVATE && state.hasPrivatePin && !state.isPrivateUnlocked) {
                                        showPinDialog = true
                                    } else {
                                        onSelectSection(section)
                                    }
                                },
                                label = { Text(section.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("section_chip_${section.name.lowercase()}")
                            )
                        }
                    }
                }

                // Auto-Detect Device Folders Banner (shown until at least one device folder is linked)
                if (state.linkedFolderCount == 0 && state.searchQuery.isBlank() && state.activeSection == LibrarySection.ALL) {
                    item {
                        FazaGlassCard(
                            accented = true,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Detect Device Videos",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Select a device folder (Downloads, Movies, DCIM) once to auto-detect new videos here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onLinkFolderClick,
                                    modifier = Modifier.testTag("auto_detect_grant_folder_button")
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                }

                // Recently Played Row
                if (state.recentVideos.isNotEmpty() && state.searchQuery.isBlank() && state.activeSection == LibrarySection.ALL) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Recently Played",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.recentVideos, key = { "recent_${it.id}" }) { video ->
                                    RecentVideoChipCard(
                                        video = video,
                                        onClick = { onPlayVideo(video) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Private Vault Unlocked Banner
                if (state.activeSection == LibrarySection.PRIVATE && state.hasPrivatePin && state.isPrivateUnlocked) {
                    item {
                        FazaGlassCard(
                            accented = true,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Private Vault Unlocked",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                TextButton(onClick = {
                                    onLockPrivate()
                                    onSelectSection(LibrarySection.ALL)
                                }) {
                                    Text("Lock")
                                }
                            }
                        }
                    }
                }

                // Video List or Empty State
                if (state.videos.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            section = state.activeSection,
                            hasQuery = state.searchQuery.isNotBlank(),
                            onPickVideos = onPickVideosClick,
                            onLinkFolder = onLinkFolderClick,
                            onOpenStream = { showStreamDialog = true }
                        )
                    }
                } else {
                    items(state.videos, key = { it.id }) { video ->
                        VideoListCard(
                            video = video,
                            onPlayClick = { onPlayVideo(video) },
                            onToggleFavourite = { onToggleFavourite(video) },
                            onTogglePrivate = { onTogglePrivate(video) },
                            onRemove = { onRemoveVideo(video.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showStreamDialog) {
        AddStreamUrlDialog(
            onDismiss = { showStreamDialog = false },
            onConfirm = { url, title ->
                showStreamDialog = false
                onAddStreamUrl(url, title)
            }
        )
    }

    if (showPinDialog) {
        PrivateVaultPinDialog(
            onDismiss = { showPinDialog = false },
            onVerify = { entered ->
                val ok = onUnlockPrivate(entered)
                if (ok) {
                    showPinDialog = false
                    onSelectSection(LibrarySection.PRIVATE)
                }
                ok
            }
        )
    }
}

@Composable
private fun EmptyLibraryState(
    section: LibrarySection,
    hasQuery: Boolean,
    onPickVideos: () -> Unit,
    onLinkFolder: () -> Unit,
    onOpenStream: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = if (hasQuery) {
                "No matching videos"
            } else {
                when (section) {
                    LibrarySection.ALL -> "Your Library is Empty"
                    LibrarySection.RECENT -> "No Recently Played Videos"
                    LibrarySection.DOWNLOADED -> "No Downloaded Videos"
                    LibrarySection.FAVOURITES -> "No Favourite Videos"
                    LibrarySection.PRIVATE -> "Private Vault is Empty"
                }
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (hasQuery) {
                "Try a different filename, folder, or format."
            } else {
                "Add local videos, link a folder, or open a stream URL."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (!hasQuery) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPickVideos,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Select Videos")
                }
                OutlinedButton(
                    onClick = onLinkFolder,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Link Folder")
                }
            }
            TextButton(onClick = onOpenStream) {
                Text("Open Stream URL")
            }
        }
    }
}

@Composable
private fun AddStreamUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Stream URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Supports MP4, MKV, WEBM, HLS (.m3u8), and DASH (.mpd).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/video.mp4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url, title.takeIf { it.isNotBlank() }) },
                enabled = url.startsWith("http://") || url.startsWith("https://"),
                modifier = Modifier.testTag("stream_play_confirm_button")
            ) {
                Text("Play")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PrivateVaultPinDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Private Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter your PIN to view private videos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6) {
                            pin = it
                            error = false
                        }
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    supportingText = if (error) {
                        { Text("Incorrect PIN.") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onVerify(pin)) {
                        error = true
                    }
                }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
