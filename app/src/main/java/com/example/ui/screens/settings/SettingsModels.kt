package com.example.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.BuildConfig
import com.example.data.model.AppColorSource
import com.example.data.model.AppCustomColorPreset
import com.example.data.model.UpdateCheckState
import com.example.data.model.UserPreferences
import com.example.ui.theme.formatColorHexLabel
import kotlin.math.roundToInt

internal enum class SettingsFolder(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tagSuffix: String
) {
    DISPLAY(
        title = "Display",
        subtitle = "Player UI, Now Playing, Running Time, App Icon & Subtitles",
        icon = Icons.Outlined.DisplaySettings,
        tagSuffix = "display"
    ),
    THEME(
        title = "Theme",
        subtitle = "Application colour, Light/Dark mode & Material You",
        icon = Icons.Outlined.Palette,
        tagSuffix = "appearance"
    ),
    PLAYBACK(
        title = "Playback",
        subtitle = "Auto-resume, PiP, orientation & decoder profile",
        icon = Icons.Outlined.PlayCircle,
        tagSuffix = "playback"
    ),
    PRIVACY(
        title = "Privacy & Storage",
        subtitle = "Private Vault PIN & All Files Access permission",
        icon = Icons.Outlined.Lock,
        tagSuffix = "privacy"
    ),
    ADVANCED(
        title = "Hardware & Decoder",
        subtitle = "GPU acceleration, codec fallback & diagnostics",
        icon = Icons.Outlined.Memory,
        tagSuffix = "advanced"
    ),
    ABOUT(
        title = "About & Updates",
        subtitle = "Version info, changelog history, legal & support",
        icon = Icons.Outlined.Info,
        tagSuffix = "about"
    )
}

internal enum class DisplaySubTab(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val tagSuffix: String
) {
    PLAYER_UI(
        label = "Player UI",
        description = "HUD theme, presets, controls, seek bar & gestures",
        icon = Icons.Outlined.Tune,
        tagSuffix = "player_ui"
    ),
    NOW_PLAYING(
        label = "Now Playing",
        description = "Permanent playback badge, lines, placement & marquee",
        icon = Icons.AutoMirrored.Outlined.BrandingWatermark,
        tagSuffix = "now_playing"
    ),
    RUNNING_TIME(
        label = "Running Time",
        description = "Elapsed/remaining timer style, scale & placement",
        icon = Icons.Outlined.Schedule,
        tagSuffix = "running_time"
    ),
    APP_ICON(
        label = "App Icon",
        description = "Optional permanent FAZA icon watermark, style, opacity & placement",
        icon = Icons.Outlined.Apps,
        tagSuffix = "app_icon"
    ),
    SUBTITLES(
        label = "Subtitles",
        description = "Caption visibility, font size, colour, edge & offset",
        icon = Icons.Outlined.Subtitles,
        tagSuffix = "subtitles"
    )
}

internal data class SettingsSearchEntry(
    val id: String,
    val title: String,
    val breadcrumb: String,
    val keywords: List<String>,
    val folder: SettingsFolder,
    val displaySubTab: DisplaySubTab? = null
)

internal val SETTINGS_SEARCH_INDEX = listOf(
    SettingsSearchEntry(
        id = "jump_player_ui",
        title = "Player UI Customizer",
        breadcrumb = "Display · Player UI",
        keywords = listOf("player", "ui", "hud", "fz ui", "faza classic", "buttons", "controls", "seek", "gestures", "double tap", "hold speed", "opacity"),
        folder = SettingsFolder.DISPLAY,
        displaySubTab = DisplaySubTab.PLAYER_UI
    ),
    SettingsSearchEntry(
        id = "jump_now_playing",
        title = "Permanent Now Playing Bar",
        breadcrumb = "Display · Now Playing",
        keywords = listOf("now playing", "banner", "marquee", "title", "status", "overlay"),
        folder = SettingsFolder.DISPLAY,
        displaySubTab = DisplaySubTab.NOW_PLAYING
    ),
    SettingsSearchEntry(
        id = "jump_running_time",
        title = "Running Time & Placement",
        breadcrumb = "Display · Running Time",
        keywords = listOf("running time", "timer", "clock", "elapsed", "remaining", "duration", "placement", "position", "corner", "custom"),
        folder = SettingsFolder.DISPLAY,
        displaySubTab = DisplaySubTab.RUNNING_TIME
    ),
    SettingsSearchEntry(
        id = "jump_app_icon",
        title = "Permanent App Icon & Placement",
        breadcrumb = "Display · App Icon",
        keywords = listOf("app icon", "logo", "watermark", "brand", "permanent", "overlay", "placement", "position", "opacity", "custom"),
        folder = SettingsFolder.DISPLAY,
        displaySubTab = DisplaySubTab.APP_ICON
    ),
    SettingsSearchEntry(
        id = "jump_subtitles",
        title = "Subtitles & Captions Style",
        breadcrumb = "Display · Subtitles",
        keywords = listOf("subtitles", "captions", "srt", "vtt", "font", "outline", "shadow", "text color"),
        folder = SettingsFolder.DISPLAY,
        displaySubTab = DisplaySubTab.SUBTITLES
    ),
    SettingsSearchEntry(
        id = "jump_theme_mode",
        title = "Theme Brightness (System / Light / Dark)",
        breadcrumb = "Theme · Brightness",
        keywords = listOf("theme", "dark", "light", "system", "night", "brightness", "mode"),
        folder = SettingsFolder.THEME
    ),
    SettingsSearchEntry(
        id = "jump_app_colour",
        title = "Application Colour & Custom Swatches",
        breadcrumb = "Theme · Application Colour",
        keywords = listOf("colour", "color", "wallpaper", "material you", "custom", "palette", "hue", "accent"),
        folder = SettingsFolder.THEME
    ),
    SettingsSearchEntry(
        id = "jump_playback_pip",
        title = "Auto PiP, Background Play & Landscape",
        breadcrumb = "Playback",
        keywords = listOf("playback", "pip", "picture in picture", "background", "landscape", "resume", "startup", "call"),
        folder = SettingsFolder.PLAYBACK
    ),
    SettingsSearchEntry(
        id = "jump_privacy_pin",
        title = "Private Vault PIN & All Files Access",
        breadcrumb = "Privacy & Storage",
        keywords = listOf("privacy", "pin", "lock", "vault", "files", "storage", "permission"),
        folder = SettingsFolder.PRIVACY
    ),
    SettingsSearchEntry(
        id = "jump_hardware_decoder",
        title = "Hardware Acceleration & Codec Inspector",
        breadcrumb = "Hardware & Decoder",
        keywords = listOf("hardware", "software", "decoder", "hw", "sw", "codec", "tunneled", "fallback", "stutter"),
        folder = SettingsFolder.ADVANCED
    ),
    SettingsSearchEntry(
        id = "jump_about_updates",
        title = "Version History, Updates & Support",
        breadcrumb = "About & Updates",
        keywords = listOf("about", "update", "version", "changelog", "privacy policy", "terms", "contact", "developer"),
        folder = SettingsFolder.ABOUT
    )
)

internal fun buildFolderSummary(
    folder: SettingsFolder,
    preferences: UserPreferences,
    updateState: UpdateCheckState
): String = when (folder) {
    SettingsFolder.DISPLAY -> {
        val pui = preferences.playerUiConfig
        val subStatus = if (preferences.subtitleStyle.enabled) {
            "Subtitles ${preferences.subtitleStyle.fontSizeSp.roundToInt()}sp"
        } else {
            "Subtitles Off"
        }
        "${pui.themeStyle.label} · ${pui.barTheme.label} · $subStatus"
    }
    SettingsFolder.THEME -> {
        val colorLabel = when (preferences.appColorSource) {
            AppColorSource.WALLPAPER -> "Wallpaper Colour"
            AppColorSource.CUSTOM -> {
                val presetName = AppCustomColorPreset.entries
                    .find { it.seedHex == preferences.customThemeColorHex }
                    ?.label
                    ?: formatColorHexLabel(preferences.customThemeColorHex)
                "Custom ($presetName)"
            }
        }
        "${preferences.themeMode.label} Mode · $colorLabel"
    }
    SettingsFolder.PLAYBACK -> {
        val parts = buildList {
            if (preferences.autoResumeLastVideoOnRestart) add("Auto-Resume")
            if (preferences.backgroundPlaybackEnabled) add("Bg Play")
            if (preferences.autoPipEnabled) add("Auto-PiP")
            if (preferences.forceLandscapeOnOpen) add("Landscape")
            if (preferences.continuePlayingDuringCall) add("Call Play")
        }
        if (parts.isEmpty()) "All background & auto modes off" else parts.joinToString(" · ")
    }
    SettingsFolder.PRIVACY -> {
        if (preferences.privatePin.isNullOrBlank()) {
            "PIN not set · Manage file access"
        } else {
            "Private Vault PIN active · Manage file access"
        }
    }
    SettingsFolder.ADVANCED -> {
        val hwStatus = if (preferences.isHardwareAccelerationEnabled) {
            "HW Accel On (${preferences.decoderMode.badge})"
        } else {
            "Software SW"
        }
        val extra = if (preferences.decoderFallbackEnabled) "Fallback On" else "Strict"
        "$hwStatus · $extra"
    }
    SettingsFolder.ABOUT -> {
        val suffix = when (updateState) {
            is UpdateCheckState.UpdateAvailable -> "Update v${updateState.manifest.latestVersion} available"
            is UpdateCheckState.UpToDate -> "Up to date"
            else -> "Build ${BuildConfig.VERSION_CODE}"
        }
        "v${BuildConfig.VERSION_NAME} · $suffix"
    }
}

internal fun buildFolderBadge(
    folder: SettingsFolder,
    preferences: UserPreferences,
    updateState: UpdateCheckState
): String = when (folder) {
    SettingsFolder.DISPLAY -> preferences.playerUiConfig.themeStyle.label
    SettingsFolder.THEME -> preferences.themeMode.label
    SettingsFolder.PLAYBACK -> preferences.decoderMode.badge
    SettingsFolder.PRIVACY -> if (preferences.privatePin.isNullOrBlank()) "Open" else "Locked"
    SettingsFolder.ADVANCED -> if (preferences.isHardwareAccelerationEnabled) "HW+" else "SW"
    SettingsFolder.ABOUT -> if (updateState is UpdateCheckState.UpdateAvailable) "New" else "v${BuildConfig.VERSION_NAME}"
}
