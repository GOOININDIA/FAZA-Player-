package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.R
import com.example.data.model.DecoderMode
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.player.PlaybackService
import com.example.ui.components.FazaGlassCard
import com.example.util.hasAllFilesAccessPermission
import com.example.util.openSystemAllFilesAccessSettings

@Composable
internal fun SettingsPlaybackFolderContent(
    preferences: UserPreferences,
    onSetDecoderMode: (DecoderMode) -> Unit,
    onSetBackgroundPlayback: (Boolean) -> Unit,
    onSetContinuePlayingDuringCall: (Boolean) -> Unit,
    onSetAutoPip: (Boolean) -> Unit,
    onSetForceLandscapeOnOpen: (Boolean) -> Unit,
    onSetAutoResumeOnRestart: (Boolean) -> Unit,
    onSetMidVideoBreakEnabled: (Boolean) -> Unit = {},
    bottomOverlayPadding: Dp = 0.dp
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hardwareAccelerationEnabled = preferences.decoderMode != DecoderMode.SOFTWARE

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("settings_detail_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 20.dp + bottomOverlayPadding + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "playback_startup_window") {
            SectionCard(
                title = "Startup & Window",
                subtitle = "Control resume behaviour, orientation lock & floating PiP"
            ) {
                ModernToggleRow(
                    title = "Resume Last Video on Startup",
                    subtitle = if (preferences.autoResumeLastVideoOnRestart) {
                        "On — Automatically resumes your last watched video when restarting"
                    } else {
                        "Off — Opens the Home folders list on startup"
                    },
                    checked = preferences.autoResumeLastVideoOnRestart,
                    onCheckedChange = onSetAutoResumeOnRestart,
                    switchTag = "auto_resume_on_restart_switch"
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(12.dp))

                ModernToggleRow(
                    title = "Force Landscape Mode",
                    subtitle = "Automatically open videos in landscape, overriding system rotation lock",
                    checked = preferences.forceLandscapeOnOpen,
                    onCheckedChange = onSetForceLandscapeOnOpen,
                    switchTag = "force_landscape_switch"
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(12.dp))

                ModernToggleRow(
                    title = "Auto Picture-in-Picture",
                    subtitle = "Float video window when leaving the player",
                    checked = preferences.autoPipEnabled,
                    onCheckedChange = onSetAutoPip
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(12.dp))

                ModernToggleRow(
                    title = "10s Mid-Video Break (Optional)",
                    subtitle = if (preferences.midVideoBreakEnabled) {
                        "Shows once at 50% (e.g. 00:30:00 in 01:00:00) for videos 20 min or longer"
                    } else {
                        "Off — plays straight through without any mid-video pause"
                    },
                    checked = preferences.midVideoBreakEnabled,
                    onCheckedChange = onSetMidVideoBreakEnabled,
                    switchTag = "mid_video_break_switch"
                )
            }
        }

        item(key = "playback_audio_calls") {
            SectionCard(
                title = "Background & Calls",
                subtitle = "Audio continuity while minimized or on voice calls"
            ) {
                ModernToggleRow(
                    title = "Background Playback",
                    subtitle = "Continue audio playback when minimized",
                    checked = preferences.backgroundPlaybackEnabled,
                    onCheckedChange = onSetBackgroundPlayback
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(12.dp))

                ModernToggleRow(
                    title = "Keep Playing During Calls",
                    subtitle = if (preferences.continuePlayingDuringCall) {
                        "Active — video and voice audio keep playing when someone calls"
                    } else {
                        "Off — pauses video & audio automatically during incoming or active calls"
                    },
                    checked = preferences.continuePlayingDuringCall,
                    onCheckedChange = onSetContinuePlayingDuringCall,
                    switchTag = "continue_playing_during_call_switch"
                )
            }
        }

        item(key = "playback_decoder") {
            SectionCard(
                title = "Decoder Profile",
                subtitle = "Choose hardware acceleration vs software decoding compatibility"
            ) {
                ModernToggleRow(
                    title = "Hardware-Accelerated Decoding",
                    subtitle = if (hardwareAccelerationEnabled) {
                        "Enabled — GPU/MediaCodec acceleration for smooth HD, 4K & 8K playback"
                    } else {
                        "Disabled — software decoding for better compatibility with older devices"
                    },
                    checked = hardwareAccelerationEnabled,
                    onCheckedChange = { enabled ->
                        onSetDecoderMode(if (enabled) DecoderMode.AUTO else DecoderMode.SOFTWARE)
                    },
                    switchTag = "hardware_decoding_switch"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecoderMode.entries.forEach { mode ->
                        val selected = preferences.decoderMode == mode
                        Surface(
                            onClick = { onSetDecoderMode(mode) },
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_decoder_option_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        mode.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsPrivacyFolderContent(
    preferences: UserPreferences,
    onOpenPinDialog: () -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var allFilesGranted by remember { mutableStateOf(hasAllFilesAccessPermission()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allFilesGranted = hasAllFilesAccessPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("settings_detail_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 20.dp + bottomOverlayPadding + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "privacy_file_access") {
            SectionCard(
                title = "System File Access Permission",
                subtitle = "Allow FAZA Player to discover local videos and external subtitles"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (allFilesGranted) {
                                    "All File Access Granted"
                                } else {
                                    "All File Access Not Granted"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            color = if (allFilesGranted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (allFilesGranted) "Active" else "Permission Needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (allFilesGranted) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Manage device media & subtitle access in Android System Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { openSystemAllFilesAccessSettings(context) },
                        shape = CircleShape,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("settings_system_file_access_button")
                    ) {
                        Text(if (allFilesGranted) "Manage" else "Open Settings")
                    }
                }
            }
        }

        item(key = "privacy_vault_pin") {
            val pinSet = !preferences.privatePin.isNullOrBlank()
            SectionCard(
                title = "Private Vault",
                subtitle = "Protect private videos behind a numeric PIN lock"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (pinSet) "PIN Protection Active" else "PIN Not Set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = if (pinSet) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (pinSet) "Secured" else "Unprotected",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pinSet) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Lock private videos with a 4 to 6 digit numeric PIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenPinDialog,
                        shape = CircleShape,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (pinSet) "Change" else "Set PIN")
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsAdvancedFolderContent(
    preferences: UserPreferences,
    onSetDecoderMode: (DecoderMode) -> Unit,
    onSetDecoderFallback: (Boolean) -> Unit,
    onSetTunneledPlayback: (Boolean) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hwEnabled = preferences.isHardwareAccelerationEnabled
    val codecs = remember { PlaybackService.inspectDeviceVideoCodecs() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("settings_detail_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 20.dp + bottomOverlayPadding + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "advanced_hw_accel") {
            SectionCard(
                title = "Hardware Acceleration & Video Decoding",
                subtitle = "Fine-tune codec selection, fallback safety, and A/V sync"
            ) {
                ModernToggleRow(
                    title = "Hardware Acceleration",
                    subtitle = if (hwEnabled) {
                        "On (${preferences.decoderMode.label}) — Uses GPU/MediaCodec for smooth 4K/8K & lower battery drain"
                    } else {
                        "Off (Software SW) — Forces CPU decoding to fix stutter, green frames, or corrupted colors"
                    },
                    checked = hwEnabled,
                    onCheckedChange = { enabled ->
                        onSetDecoderMode(if (enabled) DecoderMode.AUTO else DecoderMode.SOFTWARE)
                    },
                    switchTag = "advanced_hardware_acceleration_switch"
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Decoder Selection Policy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecoderMode.entries.forEach { mode ->
                        val selected = preferences.decoderMode == mode
                        Surface(
                            onClick = { onSetDecoderMode(mode) },
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("advanced_decoder_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(14.dp))

                ModernToggleRow(
                    title = "Automatic Software Fallback",
                    subtitle = "Automatically recover with software decoding if a hardware codec fails on this device",
                    checked = preferences.decoderFallbackEnabled,
                    onCheckedChange = onSetDecoderFallback,
                    switchTag = "advanced_decoder_fallback_switch"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernToggleRow(
                    title = "Tunneled Video Playback",
                    subtitle = "Hardware A/V sync path for compatible 4K/8K displays; turn off if audio drifts",
                    checked = preferences.tunneledPlaybackEnabled,
                    onCheckedChange = onSetTunneledPlayback,
                    switchTag = "advanced_tunneled_playback_switch"
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Troubleshooting Quick Actions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onSetDecoderMode(DecoderMode.AUTO)
                            onSetDecoderFallback(true)
                            onSetTunneledPlayback(false)
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("advanced_preset_recommended")
                    ) {
                        Text("Recommended HW")
                    }
                    FilledTonalButton(
                        onClick = {
                            onSetDecoderMode(DecoderMode.SOFTWARE)
                            onSetDecoderFallback(true)
                            onSetTunneledPlayback(false)
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("advanced_preset_troubleshoot_sw")
                    ) {
                        Text("Fix Stutter (SW)")
                    }
                }
            }
        }

        if (codecs.isNotEmpty()) {
            item(key = "advanced_codec_inspector") {
                SectionCard(
                    title = "Device Hardware Decoder Capabilities",
                    subtitle = "Reported MediaCodec video decoders on this device"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        codecs.forEach { cap ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cap.formatLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val codecName = cap.primaryHardwareCodec ?: cap.primarySoftwareCodec ?: "Not reported"
                                    Text(
                                        text = codecName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = if (cap.hasHardwareDecoder) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = when {
                                            cap.hasHardwareDecoder -> "HW Supported"
                                            cap.hasSoftwareDecoder -> "SW Only"
                                            else -> "Unavailable"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cap.hasHardwareDecoder) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val GITHUB_DOWNLOAD_URL = "https://github.com/GOOININDIA/FAZA-Player-"
private const val TELEGRAM_DOWNLOAD_URL = "https://telegram.me/fazaplayer"
private const val FACEBOOK_CONTACT_URL = "https://www.facebook.com/share/19yEZrtPfH/"
private const val INSTAGRAM_CONTACT_URL = "https://www.instagram.com/heyrajbir_?bqrl=cTVuYXdqc2lpenVz"

@Composable
internal fun SettingsAboutFolderContent(
    updateState: UpdateCheckState,
    onCheckUpdates: () -> Unit,
    onOpenVersionHistory: () -> Unit,
    onContactDeveloper: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenLegal: (String, String) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("settings_detail_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp + bottomOverlayPadding + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Modern App Identity & Update Status Hero Card
        item(key = "about_application_card") {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_application_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.faza_icon_fg),
                                    contentDescription = "FAZA Player App Logo",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Build ${BuildConfig.VERSION_CODE} · ${stringResource(R.string.release_date)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val (statusText, statusBg, statusFg) = when (updateState) {
                        is UpdateCheckState.UpdateAvailable -> Triple(
                            "New update v${updateState.manifest.latestVersion} available",
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        is UpdateCheckState.UpToDate -> Triple(
                            "You're running the latest build (v${BuildConfig.VERSION_NAME})",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        is UpdateCheckState.Checking -> Triple(
                            "Checking for updates...",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        is UpdateCheckState.Unavailable -> Triple(
                            "Update check: ${updateState.reason}",
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        UpdateCheckState.Idle -> Triple(
                            "Ultra HD Android media player & customizable HUD",
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCheckUpdates,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("about_check_updates_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Updates")
                        }
                        FilledTonalButton(
                            onClick = onOpenVersionHistory,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("about_version_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Changelog")
                        }
                    }
                }
            }
        }

        // 1b. What's New Release Notes for Current Version
        item(key = "about_whats_new_card") {
            Box(modifier = Modifier.testTag("about_whats_new_card")) {
                SectionCard(
                    title = "What's New – v${BuildConfig.VERSION_NAME}",
                    subtitle = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) highlights"
                ) {
                    val releaseNotes = listOf(
                        "Complete Material You Expressive redesign across Home folders, Search, Profile, Settings, Updates, Dialogs, and Player sheets.",
                        "Replaced glass overlays with clean borderless tonal containers, expressive 24–28dp shapes, and full-pill buttons & chips.",
                        "First-launch media permission screen and update dialogs now adapt automatically to your wallpaper Material You palette.",
                        "Spring bounce press physics across navigation dock, folder cards, theme tiles, and rounded FAZA UI player controls.",
                        "Preserves native Android device font, Permanent Now Playing Up Next in folder, and fast folder-only Home screen."
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        releaseNotes.forEach { note ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Direct FAZA Player Download & Updated Version (GitHub & Telegram)
        item(key = "about_direct_download_card") {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_direct_download_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Direct FAZA Player Download",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Get direct FAZA Player downloads & updated versions on GitHub or Telegram",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stacked = maxWidth < 340.dp
                        if (stacked) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BrandLinkTile(
                                    iconRes = R.drawable.ic_brand_github,
                                    iconContentDescription = "GitHub logo",
                                    title = "GitHub",
                                    subtitle = "Direct Download & Releases",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    accentColor = MaterialTheme.colorScheme.onSurface,
                                    testTag = "about_github_download_button",
                                    onClick = { onOpenUrl(GITHUB_DOWNLOAD_URL) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                BrandLinkTile(
                                    iconRes = R.drawable.ic_brand_telegram,
                                    iconContentDescription = "Telegram logo",
                                    title = "Telegram",
                                    subtitle = "t.me/fazaplayer",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    testTag = "about_telegram_download_button",
                                    onClick = { onOpenUrl(TELEGRAM_DOWNLOAD_URL) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BrandLinkTile(
                                    iconRes = R.drawable.ic_brand_github,
                                    iconContentDescription = "GitHub logo",
                                    title = "GitHub",
                                    subtitle = "Direct & Updated APK",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    accentColor = MaterialTheme.colorScheme.onSurface,
                                    testTag = "about_github_download_button",
                                    onClick = { onOpenUrl(GITHUB_DOWNLOAD_URL) },
                                    modifier = Modifier.weight(1f)
                                )
                                BrandLinkTile(
                                    iconRes = R.drawable.ic_brand_telegram,
                                    iconContentDescription = "Telegram logo",
                                    title = "Telegram",
                                    subtitle = "@fazaplayer",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    testTag = "about_telegram_download_button",
                                    onClick = { onOpenUrl(TELEGRAM_DOWNLOAD_URL) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Developer & Platform Info
        item(key = "about_developer_info_card") {
            SectionCard(
                title = "Developer Info",
                subtitle = "Built by Rajbir Singh · Gooin India Pvt Ltd"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MinimalInfoRow("Company", stringResource(R.string.company_name))
                    MinimalInfoRow("Developer", stringResource(R.string.developer_name))
                    MinimalInfoRow("Email", stringResource(R.string.developer_email))
                    MinimalInfoRow("Android Support", "Android 7.0 – Android 17 (Universal)")
                }
            }
        }

        // 4. Support the Developer — Buy Me A Coffee
        item(key = "about_buy_me_coffee_card") {
            BuyMeCoffeeSupportCard(
                onOpenSupportLink = { onOpenUrl(BUY_ME_A_COFFEE_URL) }
            )
        }

        // 5. Contact & Social (Facebook, Instagram & Email)
        item(key = "about_contact_card") {
            SectionCard(
                title = "Contact & Community",
                subtitle = "Connect on Facebook, Instagram, or by email"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContactSocialRow(
                        iconRes = R.drawable.ic_brand_facebook,
                        iconContentDescription = "Facebook logo",
                        title = "Contact on Facebook",
                        subtitle = "facebook.com/share/19yEZrtPfH",
                        testTag = "about_facebook_button",
                        onClick = { onOpenUrl(FACEBOOK_CONTACT_URL) }
                    )

                    ContactSocialRow(
                        iconRes = R.drawable.ic_brand_instagram,
                        iconContentDescription = "Instagram logo",
                        title = "Contact on Instagram",
                        subtitle = "@heyrajbir_",
                        testTag = "about_instagram_button",
                        onClick = { onOpenUrl(INSTAGRAM_CONTACT_URL) }
                    )

                    FilledTonalButton(
                        onClick = onContactDeveloper,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("contact_developer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Developer")
                    }
                }
            }
        }

        // 4. Legal & Policies
        item(key = "about_legal_card") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onOpenLegal("Privacy Policy", PRIVACY_POLICY_TEXT) },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("privacy_policy_button")
                    ) {
                        Text("Privacy Policy")
                    }
                    TextButton(
                        onClick = { onOpenLegal("Terms & Conditions", TERMS_CONDITIONS_TEXT) },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("terms_conditions_button")
                    ) {
                        Text("Terms & Conditions")
                    }
                }
            }
        }

        // 5. Last Item: Made in India signature
        item(key = "about_made_in_india_footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "Made in India \uD83C\uDDEE\uD83C\uDDF3",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                            .testTag("about_made_in_india_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandLinkTile(
    iconRes: Int,
    iconContentDescription: String,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = iconContentDescription,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContactSocialRow(
    iconRes: Int,
    iconContentDescription: String,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = iconContentDescription,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    FazaGlassCard(
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
internal fun ModernToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier
        )
    }
}

@Composable
private fun MinimalInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun SetPrivatePinDialog(
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Private Vault PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a 4 to 6 digit PIN.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(pin) },
                enabled = pin.length >= 4
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (hasExisting) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

internal const val PRIVACY_POLICY_TEXT = """
FAZA Player — Privacy Policy
Effective Date: November 2026
Company: Gooin India Pvt Ltd | Developer: Rajbir Singh

1. Local-First Media Processing
FAZA Player plays videos directly on your device using Android Photo Picker and Storage Access Framework (SAF). We never upload, scan, or collect your personal video files, filenames, or watch history onto external servers.

2. Minimal Permissions
FAZA Player does not request broad storage permissions. Access to device folders and external subtitle files is granted strictly by your explicit selection via system pickers.

3. Dynamic Update Checks
When checking for application updates, FAZA Player sends only a standard HTTPS request with the current app version to the developer-configured update endpoint. No personal identifiers or media metadata are transmitted.

4. Contact & Privacy
For support or privacy inquiries, contact heyrajbir.me@gmail.com.
"""

internal const val TERMS_CONDITIONS_TEXT = """
FAZA Player — Terms & Conditions
Company: Gooin India Pvt Ltd | Developer: Rajbir Singh

1. License
FAZA Player is licensed for personal, non-commercial media playback on Android devices.

2. User Content & Streams
Users are solely responsible for ensuring they have the legal right to access and play local video files or third-party network stream URLs entered into the player.

3. Updates & Availability
Software updates and changelog manifests are provided as-is by Gooin India Pvt Ltd. Users may choose when to install available updates or dismiss notifications.

4. Limitation of Liability
Gooin India Pvt Ltd and Rajbir Singh are not liable for third-party stream availability or hardware codec limitations on specific devices.
"""
