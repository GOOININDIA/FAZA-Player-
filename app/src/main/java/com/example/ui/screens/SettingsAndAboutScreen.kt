package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.model.AppColorSource
import com.example.data.model.AppIconOverlayConfig
import com.example.data.model.DecoderMode
import com.example.data.model.NowPlayingConfig
import com.example.data.model.PlayerUiConfig
import com.example.data.model.RunningTimeConfig
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.ThemeMode
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.ui.adaptive.calculateAdaptiveLayoutSpec
import com.example.ui.components.FazaAmbientBackdrop
import com.example.ui.components.FazaBrandLogo
import com.example.ui.components.FazaGlassTopBarIconButton
import com.example.ui.screens.settings.DisplaySubTab
import com.example.ui.screens.settings.SetPrivatePinDialog
import com.example.ui.screens.settings.SettingsAboutFolderContent
import com.example.ui.screens.settings.SettingsAdvancedFolderContent
import com.example.ui.screens.settings.SettingsDisplayFolderContent
import com.example.ui.screens.settings.SettingsFolder
import com.example.ui.screens.settings.SettingsPlaybackFolderContent
import com.example.ui.screens.settings.SettingsPrivacyFolderContent
import com.example.ui.screens.settings.SettingsRootList
import com.example.ui.screens.settings.SettingsThemeFolderContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndAboutScreen(
    preferences: UserPreferences,
    updateState: UpdateCheckState,
    isTopLevelTab: Boolean = true,
    onBack: () -> Unit,
    onOpenVersionHistory: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAppColorSource: (AppColorSource) -> Unit = { onSetDynamicColor(it == AppColorSource.WALLPAPER) },
    onSetCustomThemeColorHex: (Long) -> Unit = {},
    onSetDecoderMode: (DecoderMode) -> Unit,
    onSetDecoderFallback: (Boolean) -> Unit,
    onSetTunneledPlayback: (Boolean) -> Unit,
    onUpdateSubtitleStyle: (SubtitleStyleConfig) -> Unit,
    onUpdateNowPlayingConfig: (NowPlayingConfig) -> Unit,
    onUpdateRunningTimeConfig: (RunningTimeConfig) -> Unit,
    onUpdateAppIconOverlayConfig: (AppIconOverlayConfig) -> Unit = {},
    onUpdatePlayerUiConfig: (PlayerUiConfig) -> Unit,
    onResetPlayerUiConfig: () -> Unit,
    onSetBackgroundPlayback: (Boolean) -> Unit,
    onSetContinuePlayingDuringCall: (Boolean) -> Unit,
    onSetAutoPip: (Boolean) -> Unit,
    onSetForceLandscapeOnOpen: (Boolean) -> Unit,
    onSetAutoResumeOnRestart: (Boolean) -> Unit = {},
    onSetMidVideoBreakEnabled: (Boolean) -> Unit = {},
    onSetPrivatePin: (String?) -> Unit,
    onCheckUpdates: () -> Unit,
    bottomOverlayPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeFolder by rememberSaveable { mutableStateOf<SettingsFolder?>(null) }
    var activeDisplaySubTab by rememberSaveable { mutableStateOf(DisplaySubTab.PLAYER_UI) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var legalDialogTitle by remember { mutableStateOf<String?>(null) }
    var legalDialogBody by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetBackgroundPlayback(true)
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Background playback enabled (notifications disabled)")
            }
        }
    }

    val handleBackgroundPlaybackChange: (Boolean) -> Unit = { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onSetBackgroundPlayback(true)
            }
        } else {
            onSetBackgroundPlayback(enabled)
        }
    }

    val handleContactDeveloper: () -> Unit = {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:heyrajbir.me@gmail.com")
            putExtra(
                Intent.EXTRA_SUBJECT,
                "FAZA Player v${BuildConfig.VERSION_NAME} - Developer Support"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Hello Rajbir Singh (Gooin India Pvt Ltd),\n\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\n\nMessage:\n"
            )
        }
        try {
            context.startActivity(emailIntent)
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Contact email: heyrajbir.me@gmail.com")
            }
        }
    }

    val handleOpenUrl: (String) -> Unit = { url ->
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Link: $url")
            }
        }
    }

    val handleSelectFolder: (SettingsFolder, DisplaySubTab?) -> Unit = { folder, subTab ->
        activeFolder = folder
        if (subTab != null) {
            activeDisplaySubTab = subTab
        }
    }

    val handleCycleThemeMode: () -> Unit = {
        val next = when (preferences.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        onSetThemeMode(next)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val adaptiveSpec = calculateAdaptiveLayoutSpec(maxWidth, maxHeight)
        val isTwoPane = adaptiveSpec.useTwoPane
        val selectedFolderForDetail = if (isTwoPane) {
            activeFolder ?: SettingsFolder.DISPLAY
        } else {
            activeFolder
        }

        val isInSubFolder = !isTwoPane && activeFolder != null
        BackHandler(enabled = isInSubFolder || !isTopLevelTab) {
            if (isInSubFolder) {
                activeFolder = null
            } else {
                onBack()
            }
        }

        FazaAmbientBackdrop {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        navigationIcon = {
                            if (isInSubFolder || !isTopLevelTab) {
                                FazaGlassTopBarIconButton(
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isInSubFolder) {
                                        "Back to Settings"
                                    } else {
                                        "Back"
                                    },
                                    onClick = {
                                        if (isInSubFolder) {
                                            activeFolder = null
                                        } else {
                                            onBack()
                                        }
                                    },
                                    modifier = Modifier.testTag("settings_back_button")
                                )
                            }
                        },
                    title = {
                        if (isInSubFolder) {
                            val current = activeFolder!!
                            Column {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Settings · ${current.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            FazaBrandLogo(subtitle = "Settings")
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (isTwoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SettingsRootList(
                        preferences = preferences,
                        updateState = updateState,
                        selectedFolder = selectedFolderForDetail,
                        bottomOverlayPadding = bottomOverlayPadding,
                        onCycleThemeMode = handleCycleThemeMode,
                        onToggleHardwareAcceleration = { enabled ->
                            onSetDecoderMode(if (enabled) DecoderMode.AUTO else DecoderMode.SOFTWARE)
                        },
                        onSelectFolder = handleSelectFolder,
                        modifier = Modifier
                            .width(adaptiveSpec.twoPaneMasterWidth)
                            .fillMaxHeight()
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        SettingsFolderRouter(
                            folder = selectedFolderForDetail ?: SettingsFolder.DISPLAY,
                            activeDisplaySubTab = activeDisplaySubTab,
                            onSelectDisplaySubTab = { activeDisplaySubTab = it },
                            preferences = preferences,
                            updateState = updateState,
                            bottomOverlayPadding = bottomOverlayPadding,
                            onSetThemeMode = onSetThemeMode,
                            onSetAppColorSource = onSetAppColorSource,
                            onSetCustomThemeColorHex = onSetCustomThemeColorHex,
                            onUpdatePlayerUiConfig = onUpdatePlayerUiConfig,
                            onResetPlayerUiConfig = onResetPlayerUiConfig,
                            onUpdateNowPlayingConfig = onUpdateNowPlayingConfig,
                            onUpdateRunningTimeConfig = onUpdateRunningTimeConfig,
                            onUpdateAppIconOverlayConfig = onUpdateAppIconOverlayConfig,
                            onSetDecoderMode = onSetDecoderMode,
                            onSetDecoderFallback = onSetDecoderFallback,
                            onSetTunneledPlayback = onSetTunneledPlayback,
                            onSetBackgroundPlayback = handleBackgroundPlaybackChange,
                            onSetContinuePlayingDuringCall = onSetContinuePlayingDuringCall,
                            onSetAutoPip = onSetAutoPip,
                            onSetForceLandscapeOnOpen = onSetForceLandscapeOnOpen,
                            onSetAutoResumeOnRestart = onSetAutoResumeOnRestart,
                            onSetMidVideoBreakEnabled = onSetMidVideoBreakEnabled,
                            onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                            onOpenPinDialog = { showPinSetupDialog = true },
                            onCheckUpdates = onCheckUpdates,
                            onOpenVersionHistory = onOpenVersionHistory,
                            onContactDeveloper = handleContactDeveloper,
                            onOpenUrl = handleOpenUrl,
                            onOpenLegal = { title, body ->
                                legalDialogTitle = title
                                legalDialogBody = body
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AnimatedContent(
                        targetState = activeFolder,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                fadeOut(animationSpec = tween(140))
                        },
                        label = "settings_folder_navigation"
                    ) { folder ->
                        if (folder == null) {
                            SettingsRootList(
                                preferences = preferences,
                                updateState = updateState,
                                selectedFolder = null,
                                bottomOverlayPadding = bottomOverlayPadding,
                                onCycleThemeMode = handleCycleThemeMode,
                                onToggleHardwareAcceleration = { enabled ->
                                    onSetDecoderMode(if (enabled) DecoderMode.AUTO else DecoderMode.SOFTWARE)
                                },
                                onSelectFolder = handleSelectFolder
                            )
                        } else {
                            SettingsFolderRouter(
                                folder = folder,
                                activeDisplaySubTab = activeDisplaySubTab,
                                onSelectDisplaySubTab = { activeDisplaySubTab = it },
                                preferences = preferences,
                                updateState = updateState,
                                bottomOverlayPadding = bottomOverlayPadding,
                                onSetThemeMode = onSetThemeMode,
                                onSetAppColorSource = onSetAppColorSource,
                                onSetCustomThemeColorHex = onSetCustomThemeColorHex,
                                onUpdatePlayerUiConfig = onUpdatePlayerUiConfig,
                                onResetPlayerUiConfig = onResetPlayerUiConfig,
                                onUpdateNowPlayingConfig = onUpdateNowPlayingConfig,
                                onUpdateRunningTimeConfig = onUpdateRunningTimeConfig,
                                onUpdateAppIconOverlayConfig = onUpdateAppIconOverlayConfig,
                                onSetDecoderMode = onSetDecoderMode,
                                onSetDecoderFallback = onSetDecoderFallback,
                                onSetTunneledPlayback = onSetTunneledPlayback,
                                onSetBackgroundPlayback = handleBackgroundPlaybackChange,
                                onSetContinuePlayingDuringCall = onSetContinuePlayingDuringCall,
                                onSetAutoPip = onSetAutoPip,
                                onSetForceLandscapeOnOpen = onSetForceLandscapeOnOpen,
                                onSetAutoResumeOnRestart = onSetAutoResumeOnRestart,
                                onSetMidVideoBreakEnabled = onSetMidVideoBreakEnabled,
                                onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                                onOpenPinDialog = { showPinSetupDialog = true },
                                onCheckUpdates = onCheckUpdates,
                                onOpenVersionHistory = onOpenVersionHistory,
                                onContactDeveloper = handleContactDeveloper,
                                onOpenUrl = handleOpenUrl,
                                onOpenLegal = { title, body ->
                                    legalDialogTitle = title
                                    legalDialogBody = body
                                }
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showPinSetupDialog) {
        SetPrivatePinDialog(
            hasExisting = !preferences.privatePin.isNullOrBlank(),
            onDismiss = { showPinSetupDialog = false },
            onSave = { pin ->
                showPinSetupDialog = false
                onSetPrivatePin(pin)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (pin == null) "Private Vault PIN removed" else "Private Vault PIN updated"
                    )
                }
            }
        )
    }

    legalDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { legalDialogTitle = null },
            title = { Text(title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = legalDialogBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { legalDialogTitle = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsFolderRouter(
    folder: SettingsFolder,
    activeDisplaySubTab: DisplaySubTab,
    onSelectDisplaySubTab: (DisplaySubTab) -> Unit,
    preferences: UserPreferences,
    updateState: UpdateCheckState,
    bottomOverlayPadding: Dp,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAppColorSource: (AppColorSource) -> Unit,
    onSetCustomThemeColorHex: (Long) -> Unit,
    onUpdatePlayerUiConfig: (PlayerUiConfig) -> Unit,
    onResetPlayerUiConfig: () -> Unit,
    onUpdateNowPlayingConfig: (NowPlayingConfig) -> Unit,
    onUpdateRunningTimeConfig: (RunningTimeConfig) -> Unit,
    onUpdateAppIconOverlayConfig: (AppIconOverlayConfig) -> Unit,
    onSetDecoderMode: (DecoderMode) -> Unit,
    onSetDecoderFallback: (Boolean) -> Unit,
    onSetTunneledPlayback: (Boolean) -> Unit,
    onSetBackgroundPlayback: (Boolean) -> Unit,
    onSetContinuePlayingDuringCall: (Boolean) -> Unit,
    onSetAutoPip: (Boolean) -> Unit,
    onSetForceLandscapeOnOpen: (Boolean) -> Unit,
    onSetAutoResumeOnRestart: (Boolean) -> Unit,
    onSetMidVideoBreakEnabled: (Boolean) -> Unit,
    onUpdateSubtitleStyle: (SubtitleStyleConfig) -> Unit,
    onOpenPinDialog: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenVersionHistory: () -> Unit,
    onContactDeveloper: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenLegal: (String, String) -> Unit
) {
    when (folder) {
        SettingsFolder.DISPLAY -> {
            SettingsDisplayFolderContent(
                preferences = preferences,
                activeSubTab = activeDisplaySubTab,
                onSelectSubTab = onSelectDisplaySubTab,
                onUpdatePlayerUiConfig = onUpdatePlayerUiConfig,
                onResetPlayerUiConfig = onResetPlayerUiConfig,
                onUpdateNowPlayingConfig = onUpdateNowPlayingConfig,
                onUpdateRunningTimeConfig = onUpdateRunningTimeConfig,
                onUpdateAppIconOverlayConfig = onUpdateAppIconOverlayConfig,
                onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                bottomOverlayPadding = bottomOverlayPadding
            )
        }
        SettingsFolder.THEME -> {
            SettingsThemeFolderContent(
                preferences = preferences,
                onSetThemeMode = onSetThemeMode,
                onSetAppColorSource = onSetAppColorSource,
                onSetCustomThemeColorHex = onSetCustomThemeColorHex,
                bottomOverlayPadding = bottomOverlayPadding
            )
        }
        SettingsFolder.PLAYBACK -> {
            SettingsPlaybackFolderContent(
                preferences = preferences,
                onSetDecoderMode = onSetDecoderMode,
                onSetBackgroundPlayback = onSetBackgroundPlayback,
                onSetContinuePlayingDuringCall = onSetContinuePlayingDuringCall,
                onSetAutoPip = onSetAutoPip,
                onSetForceLandscapeOnOpen = onSetForceLandscapeOnOpen,
                onSetAutoResumeOnRestart = onSetAutoResumeOnRestart,
                onSetMidVideoBreakEnabled = onSetMidVideoBreakEnabled,
                bottomOverlayPadding = bottomOverlayPadding
            )
        }
        SettingsFolder.PRIVACY -> {
            SettingsPrivacyFolderContent(
                preferences = preferences,
                onOpenPinDialog = onOpenPinDialog,
                bottomOverlayPadding = bottomOverlayPadding
            )
        }
        SettingsFolder.ADVANCED -> {
            SettingsAdvancedFolderContent(
                preferences = preferences,
                onSetDecoderMode = onSetDecoderMode,
                onSetDecoderFallback = onSetDecoderFallback,
                onSetTunneledPlayback = onSetTunneledPlayback,
                bottomOverlayPadding = bottomOverlayPadding
            )
        }
        SettingsFolder.ABOUT -> {
            SettingsAboutFolderContent(
                updateState = updateState,
                onCheckUpdates = onCheckUpdates,
                onOpenVersionHistory = onOpenVersionHistory,
                onContactDeveloper = onContactDeveloper,
                onOpenUrl = onOpenUrl,
                onOpenLegal = onOpenLegal,
                bottomOverlayPadding = bottomOverlayPadding
            )
        }
    }
}
