package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppColorSource
import com.example.data.model.AppCustomColorPreset
import com.example.data.model.AppIconLanguageLayout
import com.example.data.model.AppIconOverlayAnimation
import com.example.data.model.AppIconOverlayConfig
import com.example.data.model.AppIconOverlayPosition
import com.example.data.model.AppIconOverlayStyle
import com.example.data.model.AppIconOverlayTheme
import com.example.data.model.DecoderMode
import com.example.data.model.NowPlayingConfig
import com.example.data.model.NowPlayingPosition
import com.example.data.model.NowPlayingStyle
import com.example.data.model.NowPlayingTheme
import com.example.data.model.PlayerAccentPreset
import com.example.data.model.PlayerBarTheme
import com.example.data.model.PlayerControlSize
import com.example.data.model.PlayerControlStyle
import com.example.data.model.PlayerUiConfig
import com.example.data.model.PlayerUiThemeStyle
import com.example.data.model.RunningTimeConfig
import com.example.data.model.RunningTimeDisplayMode
import com.example.data.model.RunningTimePosition
import com.example.data.model.RunningTimeStyle
import com.example.data.model.RunningTimeTheme
import com.example.data.model.SeekBarStyle
import com.example.data.model.SortOption
import com.example.data.model.SubtitleEdgeStyle
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.ThemeMode
import com.example.ads.AdFreeAccessPolicy
import com.example.ads.REQUIRED_REWARDED_ADS
import com.example.ads.RewardCompletionResult
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "faza_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val APP_COLOR_SOURCE = stringPreferencesKey("app_color_source")
        val CUSTOM_THEME_COLOR_HEX = longPreferencesKey("custom_theme_color_hex")
        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val DECODER_FALLBACK = booleanPreferencesKey("decoder_fallback")
        val TUNNELED_PLAYBACK = booleanPreferencesKey("tunneled_playback")
        val SUB_ENABLED = booleanPreferencesKey("sub_enabled")
        val SUB_FONT_SIZE = floatPreferencesKey("sub_font_size")
        val SUB_TEXT_COLOR = longPreferencesKey("sub_text_color")
        val SUB_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        val SUB_EDGE_STYLE = stringPreferencesKey("sub_edge_style")
        val SUB_BOLD = booleanPreferencesKey("sub_bold")
        val SUB_BOTTOM_PADDING = floatPreferencesKey("sub_bottom_padding")

        val NP_ENABLED = booleanPreferencesKey("np_enabled")
        val NP_THEME = stringPreferencesKey("np_theme")
        val NP_STYLE = stringPreferencesKey("np_style")
        val NP_POSITION = stringPreferencesKey("np_position")
        val NP_MAX_LINES = intPreferencesKey("np_max_lines")
        val NP_LIVE_DOT = booleanPreferencesKey("np_live_dot")
        val NP_MARQUEE = booleanPreferencesKey("np_marquee")
        val NP_FONT_SCALE = floatPreferencesKey("np_font_scale")
        val NP_SHOW_UP_NEXT = booleanPreferencesKey("np_show_up_next")
        val NP_UP_NEXT_LEAD_MIN = intPreferencesKey("np_up_next_lead_min")

        val RT_ENABLED = booleanPreferencesKey("rt_enabled")
        val RT_THEME = stringPreferencesKey("rt_theme")
        val RT_STYLE = stringPreferencesKey("rt_style")
        val RT_FONT_SCALE = floatPreferencesKey("rt_font_scale")
        val RT_SHOW_TOTAL = booleanPreferencesKey("rt_show_total")
        val RT_DISPLAY_MODE = stringPreferencesKey("rt_display_mode")
        val RT_POSITION = stringPreferencesKey("rt_position")
        val RT_CUSTOM_X = floatPreferencesKey("rt_custom_x")
        val RT_CUSTOM_Y = floatPreferencesKey("rt_custom_y")

        val AI_ENABLED = booleanPreferencesKey("app_icon_overlay_enabled")
        val AI_STYLE = stringPreferencesKey("app_icon_overlay_style")
        val AI_THEME = stringPreferencesKey("app_icon_overlay_theme")
        val AI_POSITION = stringPreferencesKey("app_icon_overlay_position")
        val AI_SIZE_SCALE = floatPreferencesKey("app_icon_overlay_size_scale")
        val AI_OPACITY = floatPreferencesKey("app_icon_overlay_opacity")
        val AI_SHOW_BORDER = booleanPreferencesKey("app_icon_overlay_show_border")
        val AI_CUSTOM_X = floatPreferencesKey("app_icon_overlay_custom_x")
        val AI_CUSTOM_Y = floatPreferencesKey("app_icon_overlay_custom_y")
        val AI_ANIMATION = stringPreferencesKey("app_icon_overlay_animation")
        val AI_ANIMATION_SPEED = floatPreferencesKey("app_icon_overlay_animation_speed")
        val AI_ANIMATE_ONLY_PLAYING = booleanPreferencesKey("app_icon_overlay_animate_only_playing")
        val AI_SHOW_VIDEO_LANGUAGE = booleanPreferencesKey("app_icon_overlay_show_video_language")
        val AI_LANGUAGE_LAYOUT = stringPreferencesKey("app_icon_overlay_language_layout")

        val PUI_THEME_STYLE = stringPreferencesKey("pui_theme_style")
        val PUI_ACCENT = stringPreferencesKey("pui_accent")
        val PUI_BAR_THEME = stringPreferencesKey("pui_bar_theme")
        val PUI_CONTROL_STYLE = stringPreferencesKey("pui_control_style")
        val PUI_CONTROL_SIZE = stringPreferencesKey("pui_control_size")
        val PUI_SEEK_BAR_STYLE = stringPreferencesKey("pui_seek_bar_style")
        val PUI_HUD_OPACITY = floatPreferencesKey("pui_hud_opacity")
        val PUI_AUTO_HIDE_SEC = intPreferencesKey("pui_auto_hide_sec")
        val PUI_SEEK_STEP_SEC = intPreferencesKey("pui_seek_step_sec")
        val PUI_SHOW_CENTER_SKIP = booleanPreferencesKey("pui_show_center_skip")
        val PUI_SHOW_RES_BADGE = booleanPreferencesKey("pui_show_res_badge")
        val PUI_SHOW_FAV = booleanPreferencesKey("pui_show_fav")
        val PUI_SHOW_PIP = booleanPreferencesKey("pui_show_pip")
        val PUI_SHOW_ROTATE = booleanPreferencesKey("pui_show_rotate")
        val PUI_SHOW_LOCK = booleanPreferencesKey("pui_show_lock")
        val PUI_SHOW_DECODER = booleanPreferencesKey("pui_show_decoder")
        val PUI_SHOW_ASPECT = booleanPreferencesKey("pui_show_aspect")
        val PUI_SHOW_TRACKS = booleanPreferencesKey("pui_show_tracks")
        val PUI_SHOW_NP_BTN = booleanPreferencesKey("pui_show_np_btn")
        val PUI_SHOW_PREV_NEXT = booleanPreferencesKey("pui_show_prev_next")
        val PUI_DOUBLE_TAP_SKIP = booleanPreferencesKey("pui_double_tap_skip")
        val PUI_HOLD_SPEED = booleanPreferencesKey("pui_hold_speed")
        val PUI_HOLD_DEFAULT_SPEED = floatPreferencesKey("pui_hold_default_speed")
        val PUI_SWIPE_GESTURES = booleanPreferencesKey("pui_swipe_gestures")
        val PUI_PINCH_ZOOM = booleanPreferencesKey("pui_pinch_zoom")
        val PUI_FZ_HIDDEN_ITEMS = stringPreferencesKey("pui_fz_hidden_items")

        val BG_PLAYBACK = booleanPreferencesKey("bg_playback")
        val CONTINUE_PLAYING_DURING_CALL = booleanPreferencesKey("continue_playing_during_call")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val FORCE_LANDSCAPE_ON_OPEN = booleanPreferencesKey("force_landscape_on_open")
        val AUTO_RESUME_ON_RESTART = booleanPreferencesKey("auto_resume_on_restart")
        val MID_VIDEO_BREAK_ENABLED = booleanPreferencesKey("mid_video_break_enabled")
        val HAS_SEEN_GESTURE_HELP = booleanPreferencesKey("has_seen_gesture_help")
        val HAS_PROMPTED_MEDIA_ACCESS = booleanPreferencesKey("has_prompted_media_access")
        val VIDEO_SORT_OPTION = stringPreferencesKey("video_sort_option")
        val PRIVATE_PIN = stringPreferencesKey("private_pin")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
        val LAST_SEEN_UPDATE_VERSION_CODE = intPreferencesKey("last_seen_update_version_code")
        val UPDATE_MANIFEST_URL = stringPreferencesKey("update_manifest_url")
        val FREE_ACCESS_EXPIRES_AT_MS = longPreferencesKey("free_access_expires_at_ms")
        val REWARDED_ADS_WATCHED_COUNT = intPreferencesKey("rewarded_ads_watched_count")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]?.let { name ->
            ThemeMode.entries.find { it.name == name }
        } ?: ThemeMode.SYSTEM

        val decoderMode = prefs[Keys.DECODER_MODE]?.let { name ->
            DecoderMode.entries.find { it.name == name }
        } ?: DecoderMode.AUTO

        val edgeStyle = prefs[Keys.SUB_EDGE_STYLE]?.let { name ->
            SubtitleEdgeStyle.entries.find { it.name == name }
        } ?: SubtitleEdgeStyle.OUTLINE

        val npTheme = prefs[Keys.NP_THEME]?.let { name ->
            NowPlayingTheme.entries.find { it.name == name }
        } ?: NowPlayingTheme.MATERIAL_YOU

        val npStyle = prefs[Keys.NP_STYLE]?.let { name ->
            NowPlayingStyle.entries.find { it.name == name }
        } ?: NowPlayingStyle.COMPACT_PILL

        val npPosition = prefs[Keys.NP_POSITION]?.let { name ->
            NowPlayingPosition.entries.find { it.name == name }
        } ?: NowPlayingPosition.TOP_CENTER

        val rtTheme = prefs[Keys.RT_THEME]?.let { name ->
            RunningTimeTheme.entries.find { it.name == name }
        } ?: RunningTimeTheme.MATERIAL_YOU

        val rtStyle = prefs[Keys.RT_STYLE]?.let { name ->
            RunningTimeStyle.entries.find { it.name == name }
        } ?: RunningTimeStyle.COMPACT_PILL

        val rtPosition = prefs[Keys.RT_POSITION]?.let { name ->
            RunningTimePosition.entries.find { it.name == name }
        } ?: RunningTimePosition.TOP_END

        val aiStyle = prefs[Keys.AI_STYLE]?.let { name ->
            AppIconOverlayStyle.entries.find { it.name == name }
        } ?: AppIconOverlayStyle.ROUNDED_BADGE

        val aiTheme = prefs[Keys.AI_THEME]?.let { name ->
            AppIconOverlayTheme.entries.find { it.name == name }
        } ?: AppIconOverlayTheme.FROSTED_DARK

        val aiPosition = prefs[Keys.AI_POSITION]?.let { name ->
            AppIconOverlayPosition.entries.find { it.name == name }
        } ?: AppIconOverlayPosition.TOP_START

        val aiAnimation = prefs[Keys.AI_ANIMATION]?.let { name ->
            AppIconOverlayAnimation.entries.find { it.name == name }
        } ?: AppIconOverlayAnimation.NONE

        val aiLanguageLayout = prefs[Keys.AI_LANGUAGE_LAYOUT]?.let { name ->
            AppIconLanguageLayout.entries.find { it.name == name }
        } ?: AppIconLanguageLayout.ONE_LINE

        val puiThemeStyle = prefs[Keys.PUI_THEME_STYLE]?.let { name ->
            PlayerUiThemeStyle.entries.find { it.name == name }
        } ?: PlayerUiThemeStyle.FAZA_CLASSIC

        val puiAccent = prefs[Keys.PUI_ACCENT]?.let { name ->
            PlayerAccentPreset.entries.find { it.name == name }
        } ?: PlayerAccentPreset.DYNAMIC_PRIMARY

        val puiBarTheme = prefs[Keys.PUI_BAR_THEME]?.let { name ->
            PlayerBarTheme.entries.find { it.name == name }
        } ?: PlayerBarTheme.MATERIAL_YOU

        val puiControlStyle = prefs[Keys.PUI_CONTROL_STYLE]?.let { name ->
            PlayerControlStyle.entries.find { it.name == name }
        } ?: PlayerControlStyle.STANDARD

        val puiControlSize = prefs[Keys.PUI_CONTROL_SIZE]?.let { name ->
            PlayerControlSize.entries.find { it.name == name }
        } ?: PlayerControlSize.MEDIUM

        val puiSeekBarStyle = prefs[Keys.PUI_SEEK_BAR_STYLE]?.let { name ->
            SeekBarStyle.entries.find { it.name == name }
        } ?: SeekBarStyle.STANDARD

        val videoSortOption = prefs[Keys.VIDEO_SORT_OPTION]?.let { name ->
            SortOption.entries.find { it.name == name }
        } ?: SortOption.DATE_ADDED_DESC

        val legacyDynamic = prefs[Keys.DYNAMIC_COLOR] ?: true
        val appColorSource = prefs[Keys.APP_COLOR_SOURCE]?.let { name ->
            AppColorSource.entries.find { it.name == name }
        } ?: if (legacyDynamic) AppColorSource.WALLPAPER else AppColorSource.CUSTOM

        UserPreferences(
            themeMode = themeMode,
            dynamicColorEnabled = appColorSource == AppColorSource.WALLPAPER,
            appColorSource = appColorSource,
            customThemeColorHex = prefs[Keys.CUSTOM_THEME_COLOR_HEX] ?: AppCustomColorPreset.ELECTRIC_VIOLET.seedHex,
            decoderMode = decoderMode,
            decoderFallbackEnabled = prefs[Keys.DECODER_FALLBACK] ?: true,
            tunneledPlaybackEnabled = prefs[Keys.TUNNELED_PLAYBACK] ?: false,
            subtitleStyle = SubtitleStyleConfig(
                enabled = prefs[Keys.SUB_ENABLED] ?: true,
                fontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 18f,
                textColorHex = prefs[Keys.SUB_TEXT_COLOR] ?: 0xFFFFFFFF,
                backgroundOpacity = prefs[Keys.SUB_BG_OPACITY] ?: 0.55f,
                edgeStyle = edgeStyle,
                bold = prefs[Keys.SUB_BOLD] ?: false,
                bottomPaddingDp = prefs[Keys.SUB_BOTTOM_PADDING] ?: 44f
            ),
            nowPlayingConfig = NowPlayingConfig(
                enabled = prefs[Keys.NP_ENABLED] ?: true,
                theme = npTheme,
                style = npStyle,
                position = npPosition,
                maxLines = (prefs[Keys.NP_MAX_LINES] ?: 1).coerceIn(1, 2),
                showLiveDot = prefs[Keys.NP_LIVE_DOT] ?: true,
                marqueeScroll = prefs[Keys.NP_MARQUEE] ?: true,
                fontScale = prefs[Keys.NP_FONT_SCALE] ?: 1.0f,
                showUpNext = prefs[Keys.NP_SHOW_UP_NEXT] ?: true,
                upNextLeadMinutes = (prefs[Keys.NP_UP_NEXT_LEAD_MIN] ?: 5).coerceIn(1, 30)
            ),
            runningTimeConfig = RunningTimeConfig(
                enabled = prefs[Keys.RT_ENABLED] ?: true,
                theme = rtTheme,
                style = rtStyle,
                fontScale = prefs[Keys.RT_FONT_SCALE] ?: 1.0f,
                displayMode = resolveRunningTimeDisplayMode(
                    storedModeName = prefs[Keys.RT_DISPLAY_MODE],
                    legacyShowTotal = prefs[Keys.RT_SHOW_TOTAL]
                ),
                position = rtPosition,
                customXFraction = (prefs[Keys.RT_CUSTOM_X] ?: 0.88f).coerceIn(0f, 1f),
                customYFraction = (prefs[Keys.RT_CUSTOM_Y] ?: 0.12f).coerceIn(0f, 1f)
            ),
            appIconOverlayConfig = AppIconOverlayConfig(
                enabled = prefs[Keys.AI_ENABLED] ?: false,
                style = aiStyle,
                theme = aiTheme,
                position = aiPosition,
                sizeScale = (prefs[Keys.AI_SIZE_SCALE] ?: 1.0f).coerceIn(0.65f, 1.65f),
                opacity = (prefs[Keys.AI_OPACITY] ?: 0.85f).coerceIn(0.20f, 1.0f),
                showBorder = prefs[Keys.AI_SHOW_BORDER] ?: true,
                customXFraction = (prefs[Keys.AI_CUSTOM_X] ?: 0.08f).coerceIn(0f, 1f),
                customYFraction = (prefs[Keys.AI_CUSTOM_Y] ?: 0.12f).coerceIn(0f, 1f),
                animation = aiAnimation,
                animationSpeed = (prefs[Keys.AI_ANIMATION_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
                animateOnlyWhilePlaying = prefs[Keys.AI_ANIMATE_ONLY_PLAYING] ?: true,
                showVideoLanguage = prefs[Keys.AI_SHOW_VIDEO_LANGUAGE] ?: false,
                languageLayout = aiLanguageLayout
            ),
            playerUiConfig = PlayerUiConfig(
                themeStyle = puiThemeStyle,
                accentPreset = puiAccent,
                barTheme = puiBarTheme,
                controlStyle = puiControlStyle,
                controlSize = puiControlSize,
                seekBarStyle = puiSeekBarStyle,
                hudOpacity = (prefs[Keys.PUI_HUD_OPACITY] ?: 0.85f).coerceIn(0.4f, 1.0f),
                autoHideTimeoutSec = (prefs[Keys.PUI_AUTO_HIDE_SEC] ?: 4).coerceIn(0, 15),
                seekStepSeconds = (prefs[Keys.PUI_SEEK_STEP_SEC] ?: 10).coerceIn(5, 60),
                showCenterSkipButtons = prefs[Keys.PUI_SHOW_CENTER_SKIP] ?: true,
                showResolutionBadge = prefs[Keys.PUI_SHOW_RES_BADGE] ?: true,
                showFavouriteButton = prefs[Keys.PUI_SHOW_FAV] ?: true,
                showPipButton = prefs[Keys.PUI_SHOW_PIP] ?: true,
                showRotateButton = prefs[Keys.PUI_SHOW_ROTATE] ?: true,
                showLockButton = prefs[Keys.PUI_SHOW_LOCK] ?: true,
                showDecoderButton = prefs[Keys.PUI_SHOW_DECODER] ?: true,
                showAspectButton = prefs[Keys.PUI_SHOW_ASPECT] ?: true,
                showTracksButton = prefs[Keys.PUI_SHOW_TRACKS] ?: true,
                showNowPlayingButton = prefs[Keys.PUI_SHOW_NP_BTN] ?: true,
                showPrevNextButtons = prefs[Keys.PUI_SHOW_PREV_NEXT] ?: true,
                doubleTapSkipEnabled = prefs[Keys.PUI_DOUBLE_TAP_SKIP] ?: true,
                holdSpeedEnabled = prefs[Keys.PUI_HOLD_SPEED] ?: true,
                holdDefaultSpeed = (prefs[Keys.PUI_HOLD_DEFAULT_SPEED] ?: 2.0f).coerceIn(1.25f, 3.0f),
                swipeGesturesEnabled = prefs[Keys.PUI_SWIPE_GESTURES] ?: true,
                pinchZoomEnabled = prefs[Keys.PUI_PINCH_ZOOM] ?: true,
                fzHiddenToolbarItems = prefs[Keys.PUI_FZ_HIDDEN_ITEMS]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet()
                    ?: emptySet()
            ),
            backgroundPlaybackEnabled = prefs[Keys.BG_PLAYBACK] ?: true,
            continuePlayingDuringCall = prefs[Keys.CONTINUE_PLAYING_DURING_CALL] ?: false,
            autoPipEnabled = prefs[Keys.AUTO_PIP] ?: true,
            forceLandscapeOnOpen = prefs[Keys.FORCE_LANDSCAPE_ON_OPEN] ?: false,
            autoResumeLastVideoOnRestart = prefs[Keys.AUTO_RESUME_ON_RESTART] ?: true,
            midVideoBreakEnabled = prefs[Keys.MID_VIDEO_BREAK_ENABLED] ?: true,
            hasSeenPlayerGestureHelp = prefs[Keys.HAS_SEEN_GESTURE_HELP] ?: false,
            hasPromptedMediaAccess = prefs[Keys.HAS_PROMPTED_MEDIA_ACCESS] ?: false,
            videoSortOption = videoSortOption,
            privatePin = prefs[Keys.PRIVATE_PIN],
            dismissedUpdateVersion = prefs[Keys.DISMISSED_UPDATE_VERSION],
            lastSeenUpdateVersionCode = prefs[Keys.LAST_SEEN_UPDATE_VERSION_CODE] ?: 0,
            updateManifestUrl = prefs[Keys.UPDATE_MANIFEST_URL]
                ?.takeUnless { it.isBlank() || it == UserPreferences.LEGACY_UPDATE_MANIFEST_URL }
                ?: UserPreferences.DEFAULT_UPDATE_MANIFEST_URL,
            freeAccessExpiresAtMs = (prefs[Keys.FREE_ACCESS_EXPIRES_AT_MS] ?: 0L).coerceAtLeast(0L),
            rewardedAdsWatchedCount = (prefs[Keys.REWARDED_ADS_WATCHED_COUNT] ?: 0)
                .coerceIn(0, REQUIRED_REWARDED_ADS)
        )
    }

    private suspend fun editSetting(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        editSetting { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        editSetting {
            it[Keys.DYNAMIC_COLOR] = enabled
            it[Keys.APP_COLOR_SOURCE] = if (enabled) AppColorSource.WALLPAPER.name else AppColorSource.CUSTOM.name
        }
    }

    suspend fun setAppColorSource(source: AppColorSource) {
        editSetting {
            it[Keys.APP_COLOR_SOURCE] = source.name
            it[Keys.DYNAMIC_COLOR] = (source == AppColorSource.WALLPAPER)
        }
    }

    suspend fun setCustomThemeColorHex(colorHex: Long) {
        editSetting {
            it[Keys.CUSTOM_THEME_COLOR_HEX] = colorHex
            it[Keys.APP_COLOR_SOURCE] = AppColorSource.CUSTOM.name
            it[Keys.DYNAMIC_COLOR] = false
        }
    }

    suspend fun setDecoderMode(mode: DecoderMode) {
        editSetting { it[Keys.DECODER_MODE] = mode.name }
    }

    suspend fun setDecoderFallbackEnabled(enabled: Boolean) {
        editSetting { it[Keys.DECODER_FALLBACK] = enabled }
    }

    suspend fun setTunneledPlaybackEnabled(enabled: Boolean) {
        editSetting { it[Keys.TUNNELED_PLAYBACK] = enabled }
    }

    suspend fun updateSubtitleStyle(style: SubtitleStyleConfig) {
        editSetting {
            it[Keys.SUB_ENABLED] = style.enabled
            it[Keys.SUB_FONT_SIZE] = style.fontSizeSp
            it[Keys.SUB_TEXT_COLOR] = style.textColorHex
            it[Keys.SUB_BG_OPACITY] = style.backgroundOpacity
            it[Keys.SUB_EDGE_STYLE] = style.edgeStyle.name
            it[Keys.SUB_BOLD] = style.bold
            it[Keys.SUB_BOTTOM_PADDING] = style.bottomPaddingDp
        }
    }

    suspend fun updateNowPlayingConfig(config: NowPlayingConfig) {
        editSetting {
            it[Keys.NP_ENABLED] = config.enabled
            it[Keys.NP_THEME] = config.theme.name
            it[Keys.NP_STYLE] = config.style.name
            it[Keys.NP_POSITION] = config.position.name
            it[Keys.NP_MAX_LINES] = config.maxLines.coerceIn(1, 2)
            it[Keys.NP_LIVE_DOT] = config.showLiveDot
            it[Keys.NP_MARQUEE] = config.marqueeScroll
            it[Keys.NP_FONT_SCALE] = config.fontScale
            it[Keys.NP_SHOW_UP_NEXT] = config.showUpNext
            it[Keys.NP_UP_NEXT_LEAD_MIN] = config.upNextLeadMinutes.coerceIn(1, 30)
        }
    }

    suspend fun updateRunningTimeConfig(config: RunningTimeConfig) {
        editSetting {
            it[Keys.RT_ENABLED] = config.enabled
            it[Keys.RT_THEME] = config.theme.name
            it[Keys.RT_STYLE] = config.style.name
            it[Keys.RT_FONT_SCALE] = config.fontScale
            it[Keys.RT_DISPLAY_MODE] = config.displayMode.name
            it[Keys.RT_SHOW_TOTAL] = config.showTotalDuration
            it[Keys.RT_POSITION] = config.position.name
            it[Keys.RT_CUSTOM_X] = config.customXFraction.coerceIn(0f, 1f)
            it[Keys.RT_CUSTOM_Y] = config.customYFraction.coerceIn(0f, 1f)
        }
    }

    suspend fun updateAppIconOverlayConfig(config: AppIconOverlayConfig) {
        editSetting {
            it[Keys.AI_ENABLED] = config.enabled
            it[Keys.AI_STYLE] = config.style.name
            it[Keys.AI_THEME] = config.theme.name
            it[Keys.AI_POSITION] = config.position.name
            it[Keys.AI_SIZE_SCALE] = config.sizeScale.coerceIn(0.65f, 1.65f)
            it[Keys.AI_OPACITY] = config.opacity.coerceIn(0.20f, 1.0f)
            it[Keys.AI_SHOW_BORDER] = config.showBorder
            it[Keys.AI_CUSTOM_X] = config.customXFraction.coerceIn(0f, 1f)
            it[Keys.AI_CUSTOM_Y] = config.customYFraction.coerceIn(0f, 1f)
            it[Keys.AI_ANIMATION] = config.animation.name
            it[Keys.AI_ANIMATION_SPEED] = config.animationSpeed.coerceIn(0.5f, 2.0f)
            it[Keys.AI_ANIMATE_ONLY_PLAYING] = config.animateOnlyWhilePlaying
            it[Keys.AI_SHOW_VIDEO_LANGUAGE] = config.showVideoLanguage
            it[Keys.AI_LANGUAGE_LAYOUT] = config.languageLayout.name
        }
    }

    suspend fun updatePlayerUiConfig(config: PlayerUiConfig) {
        editSetting {
            it[Keys.PUI_THEME_STYLE] = config.themeStyle.name
            it[Keys.PUI_ACCENT] = config.accentPreset.name
            it[Keys.PUI_BAR_THEME] = config.barTheme.name
            it[Keys.PUI_CONTROL_STYLE] = config.controlStyle.name
            it[Keys.PUI_CONTROL_SIZE] = config.controlSize.name
            it[Keys.PUI_SEEK_BAR_STYLE] = config.seekBarStyle.name
            it[Keys.PUI_HUD_OPACITY] = config.hudOpacity
            it[Keys.PUI_AUTO_HIDE_SEC] = config.autoHideTimeoutSec
            it[Keys.PUI_SEEK_STEP_SEC] = config.seekStepSeconds
            it[Keys.PUI_SHOW_CENTER_SKIP] = config.showCenterSkipButtons
            it[Keys.PUI_SHOW_RES_BADGE] = config.showResolutionBadge
            it[Keys.PUI_SHOW_FAV] = config.showFavouriteButton
            it[Keys.PUI_SHOW_PIP] = config.showPipButton
            it[Keys.PUI_SHOW_ROTATE] = config.showRotateButton
            it[Keys.PUI_SHOW_LOCK] = config.showLockButton
            it[Keys.PUI_SHOW_DECODER] = config.showDecoderButton
            it[Keys.PUI_SHOW_ASPECT] = config.showAspectButton
            it[Keys.PUI_SHOW_TRACKS] = config.showTracksButton
            it[Keys.PUI_SHOW_NP_BTN] = config.showNowPlayingButton
            it[Keys.PUI_SHOW_PREV_NEXT] = config.showPrevNextButtons
            it[Keys.PUI_DOUBLE_TAP_SKIP] = config.doubleTapSkipEnabled
            it[Keys.PUI_HOLD_SPEED] = config.holdSpeedEnabled
            it[Keys.PUI_HOLD_DEFAULT_SPEED] = config.holdDefaultSpeed
            it[Keys.PUI_SWIPE_GESTURES] = config.swipeGesturesEnabled
            it[Keys.PUI_PINCH_ZOOM] = config.pinchZoomEnabled
            it[Keys.PUI_FZ_HIDDEN_ITEMS] = config.fzHiddenToolbarItems.joinToString(",")
        }
    }

    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        editSetting { it[Keys.BG_PLAYBACK] = enabled }
    }

    suspend fun setContinuePlayingDuringCall(enabled: Boolean) {
        editSetting { it[Keys.CONTINUE_PLAYING_DURING_CALL] = enabled }
    }

    suspend fun setAutoPipEnabled(enabled: Boolean) {
        editSetting { it[Keys.AUTO_PIP] = enabled }
    }

    suspend fun setForceLandscapeOnOpen(enabled: Boolean) {
        editSetting { it[Keys.FORCE_LANDSCAPE_ON_OPEN] = enabled }
    }

    suspend fun setAutoResumeLastVideoOnRestart(enabled: Boolean) {
        editSetting { it[Keys.AUTO_RESUME_ON_RESTART] = enabled }
    }

    suspend fun setMidVideoBreakEnabled(enabled: Boolean) {
        editSetting { it[Keys.MID_VIDEO_BREAK_ENABLED] = enabled }
    }

    suspend fun setHasSeenGestureHelp(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_GESTURE_HELP] = seen }
    }

    suspend fun setHasPromptedMediaAccess(prompted: Boolean) {
        context.dataStore.edit { it[Keys.HAS_PROMPTED_MEDIA_ACCESS] = prompted }
    }

    suspend fun setVideoSortOption(option: SortOption) {
        editSetting { it[Keys.VIDEO_SORT_OPTION] = option.name }
    }

    suspend fun setPrivatePin(pin: String?) {
        editSetting {
            if (pin.isNullOrBlank()) {
                it.remove(Keys.PRIVATE_PIN)
            } else {
                it[Keys.PRIVATE_PIN] = pin
            }
        }
    }

    suspend fun setDismissedUpdateVersion(version: String) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE_VERSION] = version }
    }

    suspend fun clearDismissedUpdateVersion() {
        context.dataStore.edit { it.remove(Keys.DISMISSED_UPDATE_VERSION) }
    }

    suspend fun setLastSeenUpdateVersionCode(versionCode: Int) {
        context.dataStore.edit { it[Keys.LAST_SEEN_UPDATE_VERSION_CODE] = versionCode }
    }

    suspend fun setUpdateManifestUrl(url: String) {
        editSetting {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) {
                it[Keys.UPDATE_MANIFEST_URL] = UserPreferences.DEFAULT_UPDATE_MANIFEST_URL
            } else {
                it[Keys.UPDATE_MANIFEST_URL] = trimmed
            }
        }
    }

    /**
     * Atomically records one verified rewarded-ad completion in DataStore so progress
     * and the 24-hour expiration timestamp survive app restarts and device reboots.
     */
    suspend fun recordCompletedRewardedAd(
        nowMs: Long = System.currentTimeMillis()
    ): RewardCompletionResult {
        var result: RewardCompletionResult? = null
        context.dataStore.edit { prefs ->
            val currentWatched = (prefs[Keys.REWARDED_ADS_WATCHED_COUNT] ?: 0)
                .coerceIn(0, REQUIRED_REWARDED_ADS)
            val currentExpires = (prefs[Keys.FREE_ACCESS_EXPIRES_AT_MS] ?: 0L).coerceAtLeast(0L)
            val transition = AdFreeAccessPolicy.applyCompletedReward(
                currentWatchedCount = currentWatched,
                currentExpiresAtMs = currentExpires,
                nowMs = nowMs
            )
            prefs[Keys.REWARDED_ADS_WATCHED_COUNT] = transition.newWatchedCount
            if (transition.unlocked24HourAccess) {
                prefs[Keys.FREE_ACCESS_EXPIRES_AT_MS] = transition.newExpiresAtMs
            } else if (!transition.wasAlreadyActive && currentExpires in 1L..nowMs) {
                // Previous access expired; keep clean state while progressing toward a fresh unlock
                prefs[Keys.FREE_ACCESS_EXPIRES_AT_MS] = 0L
            }
            result = transition
        }
        return requireNotNull(result)
    }

    suspend fun setFreeAccessState(watchedCount: Int, expiresAtMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REWARDED_ADS_WATCHED_COUNT] = watchedCount.coerceIn(0, REQUIRED_REWARDED_ADS)
            prefs[Keys.FREE_ACCESS_EXPIRES_AT_MS] = expiresAtMs.coerceAtLeast(0L)
        }
    }
}

internal fun resolveRunningTimeDisplayMode(
    storedModeName: String?,
    legacyShowTotal: Boolean?
): RunningTimeDisplayMode {
    storedModeName?.let { name ->
        RunningTimeDisplayMode.entries.find { it.name == name }
    }?.let { return it }

    return if (legacyShowTotal == false) {
        RunningTimeDisplayMode.ELAPSED_ONLY
    } else {
        RunningTimeDisplayMode.ELAPSED_REMAINING
    }
}
