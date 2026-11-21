package com.example.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppColorSource(val label: String, val description: String) {
    WALLPAPER("Wallpaper Colour", "Adapt app colors dynamically from your device wallpaper"),
    CUSTOM("Custom Colour", "Pick a preset palette or craft your own custom color")
}

enum class AppCustomColorPreset(val label: String, val seedHex: Long) {
    ELECTRIC_VIOLET("Electric Violet", 0xFF8B5CF6),
    ROYAL_PURPLE("Royal Violet", 0xFF7C3AED),
    OCEAN_BLUE("Ocean Blue", 0xFF2563EB),
    CINEMA_CYAN("Cinema Cyan", 0xFF0891B2),
    CRIMSON_ROSE("Crimson Rose", 0xFFE11D48),
    EMERALD_MINT("Emerald Mint", 0xFF059669),
    SUNSET_AMBER("Sunset Amber", 0xFFD97706),
    WARM_OLIVE("Warm Olive", 0xFF71610B)
}

enum class DecoderMode(val label: String, val badge: String, val description: String) {
    AUTO("Auto (Recommended)", "AUTO", "Prefers hardware decoding with seamless software fallback"),
    HARDWARE("Hardware (HW+)", "HW", "Low-battery GPU/MediaCodec acceleration for 4K & 8K"),
    SOFTWARE("Software (SW)", "SW", "Software decoding fallback for rare MKV/AVI/10-bit profiles")
}

enum class SubtitleEdgeStyle(val label: String) {
    NONE("None"),
    OUTLINE("Outline"),
    DROP_SHADOW("Drop Shadow")
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Crop / Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    RATIO_21_9("21:9 Cinema")
}

enum class SortOption(val label: String) {
    DATE_ADDED_DESC("Newest First"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    DURATION_DESC("Longest First"),
    SIZE_DESC("Largest Size"),
    RESOLUTION_DESC("Highest Resolution")
}

enum class LibrarySection(val label: String) {
    ALL("All Videos"),
    RECENT("Recent"),
    DOWNLOADED("Downloaded"),
    FAVOURITES("Favourites"),
    PRIVATE("Private")
}

enum class NowPlayingTheme(val label: String) {
    MATERIAL_YOU("Material You"),
    FROSTED_DARK("Frosted Dark"),
    TRANSPARENT("Transparent"),
    HIGH_CONTRAST("High Contrast")
}

enum class NowPlayingStyle(val label: String) {
    COMPACT_PILL("Compact Pill"),
    ROUNDED_CARD("Rounded Card"),
    SLIM_BANNER("Slim Banner"),
    MINIMAL_TEXT("Minimal Text")
}

enum class NowPlayingPosition(val label: String) {
    TOP_CENTER("Top Center"),
    TOP_START("Top Left"),
    BOTTOM_CENTER("Bottom")
}

data class NowPlayingConfig(
    val enabled: Boolean = true,
    val theme: NowPlayingTheme = NowPlayingTheme.MATERIAL_YOU,
    val style: NowPlayingStyle = NowPlayingStyle.COMPACT_PILL,
    val position: NowPlayingPosition = NowPlayingPosition.TOP_CENTER,
    val maxLines: Int = 1,
    val showLiveDot: Boolean = true,
    val marqueeScroll: Boolean = true,
    val fontScale: Float = 1.0f,
    val showUpNext: Boolean = true,
    val upNextLeadMinutes: Int = 5
)

enum class RunningTimeTheme(val label: String) {
    MATERIAL_YOU("Material You"),
    FROSTED_DARK("Frosted Dark"),
    TRANSPARENT("Transparent"),
    HIGH_CONTRAST("High Contrast")
}

enum class RunningTimeStyle(val label: String) {
    COMPACT_PILL("Compact Pill"),
    ROUNDED_CARD("Rounded Card"),
    BADGE_OUTLINED("Outlined Badge"),
    MINIMAL_TEXT("Minimal Text")
}

enum class RunningTimeDisplayMode(
    val label: String,
    val exampleHint: String
) {
    ELAPSED_REMAINING("Elapsed / Remaining", "12:00/02:00"),
    ELAPSED_TOTAL("Elapsed / Total", "12:00/14:00"),
    ELAPSED_ONLY("Elapsed Only", "12:00");

    fun next(): RunningTimeDisplayMode = when (this) {
        ELAPSED_REMAINING -> ELAPSED_TOTAL
        ELAPSED_TOTAL -> ELAPSED_ONLY
        ELAPSED_ONLY -> ELAPSED_REMAINING
    }
}

enum class RunningTimePosition(val label: String) {
    TOP_END("Top Right"),
    TOP_START("Top Left"),
    TOP_CENTER("Top Center"),
    BOTTOM_END("Bottom Right"),
    BOTTOM_START("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    CUSTOM("Custom");

    val isTopRow: Boolean
        get() = this == TOP_END || this == TOP_START || this == TOP_CENTER

    val isBottomRow: Boolean
        get() = this == BOTTOM_END || this == BOTTOM_START || this == BOTTOM_CENTER
}

data class RunningTimeConfig(
    val enabled: Boolean = true,
    val theme: RunningTimeTheme = RunningTimeTheme.MATERIAL_YOU,
    val style: RunningTimeStyle = RunningTimeStyle.COMPACT_PILL,
    val fontScale: Float = 1.0f,
    val displayMode: RunningTimeDisplayMode = RunningTimeDisplayMode.ELAPSED_REMAINING,
    val position: RunningTimePosition = RunningTimePosition.TOP_END,
    val customXFraction: Float = 0.88f,
    val customYFraction: Float = 0.12f
) {
    val showTotalDuration: Boolean
        get() = displayMode != RunningTimeDisplayMode.ELAPSED_ONLY
}

enum class AppIconOverlayTheme(val label: String) {
    MATERIAL_YOU("Material You"),
    FROSTED_DARK("Frosted Dark"),
    TRANSPARENT("Transparent"),
    HIGH_CONTRAST("High Contrast")
}

enum class AppIconOverlayStyle(val label: String) {
    ROUNDED_BADGE("Rounded Badge"),
    CIRCLE_BADGE("Circle Badge"),
    PILL_BRAND("Icon + Name"),
    ICON_ONLY("Icon Only")
}

enum class AppIconOverlayPosition(val label: String) {
    TOP_START("Top Left"),
    TOP_END("Top Right"),
    TOP_CENTER("Top Center"),
    BOTTOM_START("Bottom Left"),
    BOTTOM_END("Bottom Right"),
    BOTTOM_CENTER("Bottom Center"),
    CUSTOM("Custom");

    val isTopRow: Boolean
        get() = this == TOP_START || this == TOP_END || this == TOP_CENTER

    val isBottomRow: Boolean
        get() = this == BOTTOM_START || this == BOTTOM_END || this == BOTTOM_CENTER
}

enum class AppIconOverlayAnimation(val label: String) {
    NONE("Off"),
    PULSE("Pulse"),
    FLOAT("Float"),
    SPIN("Slow Spin"),
    SHIMMER("Glow Pulse"),
    SWING("Gentle Sway")
}

data class AppIconOverlayConfig(
    val enabled: Boolean = false,
    val style: AppIconOverlayStyle = AppIconOverlayStyle.ROUNDED_BADGE,
    val theme: AppIconOverlayTheme = AppIconOverlayTheme.FROSTED_DARK,
    val position: AppIconOverlayPosition = AppIconOverlayPosition.TOP_START,
    val sizeScale: Float = 1.0f,
    val opacity: Float = 0.85f,
    val showBorder: Boolean = true,
    val customXFraction: Float = 0.08f,
    val customYFraction: Float = 0.12f,
    val animation: AppIconOverlayAnimation = AppIconOverlayAnimation.NONE,
    val animationSpeed: Float = 1.0f,
    val animateOnlyWhilePlaying: Boolean = true
)

enum class PlayerAccentPreset(val label: String, val colorHex: Long?) {
    DYNAMIC_PRIMARY("App Theme", null),
    CRIMSON("FAZA Rose", 0xFFFB7185),
    CYAN("Cinema Cyan", 0xFF22D3EE),
    AMBER("Gold", 0xFFFBBF24),
    EMERALD("Neon Mint", 0xFF34D399),
    PURE_WHITE("Pure White", 0xFFFFFFFF)
}

enum class PlayerBarTheme(val label: String) {
    CINEMA_DARK("Cinema Dark"),
    FROSTED_GLASS("Frosted Glass"),
    AMOLED_BLACK("AMOLED Black"),
    MATERIAL_YOU("Material You"),
    MINIMAL_CLEAR("Minimal Clear")
}

enum class PlayerControlStyle(val label: String) {
    STANDARD("Rounded Circle"),
    PILL("Soft Pill"),
    OUTLINED("Outlined"),
    MINIMAL("Icon Only")
}

enum class PlayerControlSize(val label: String, val scale: Float) {
    COMPACT("Compact", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.18f)
}

enum class SeekBarStyle(val label: String) {
    STANDARD("Classic"),
    THICK_BAR("Cinema Bar"),
    MINIMAL_LINE("Slim Line")
}

enum class PlayerUiThemeStyle(
    val label: String,
    val subtitle: String
) {
    FAZA_CLASSIC("FAZA Classic", "Adaptive FAZA Cinema HUD & customizable bars"),
    FZ_UI("FZ Ui", "Pitch-black pro player HUD with quick-access feature bar")
}

enum class FzQuickToolbarItem(val label: String) {
    NIGHT_MODE("Night Mode"),
    CUSTOMISE_ITEMS("Customise Items"),
    SHUFFLE("Shuffle"),
    LOOP("Loop"),
    MUTE("Mute"),
    SLEEP_TIMER("Sleep Timer"),
    AB_REPEAT("A-B Repeat"),
    AUDIO_EFFECT("Audio Effect"),
    EQUALIZER("Equalizer"),
    SPEED("Speed"),
    SCREENSHOT("Screenshot"),
    BACKGROUND_PLAY("Background Play"),
    SCREEN_ROTATION("Screen Rotation")
}

data class PlayerUiConfig(
    val themeStyle: PlayerUiThemeStyle = PlayerUiThemeStyle.FAZA_CLASSIC,
    val accentPreset: PlayerAccentPreset = PlayerAccentPreset.DYNAMIC_PRIMARY,
    val barTheme: PlayerBarTheme = PlayerBarTheme.MATERIAL_YOU,
    val controlStyle: PlayerControlStyle = PlayerControlStyle.STANDARD,
    val controlSize: PlayerControlSize = PlayerControlSize.MEDIUM,
    val seekBarStyle: SeekBarStyle = SeekBarStyle.STANDARD,
    val hudOpacity: Float = 0.85f,
    val autoHideTimeoutSec: Int = 4,
    val seekStepSeconds: Int = 10,
    val showCenterSkipButtons: Boolean = true,
    val showResolutionBadge: Boolean = true,
    val showFavouriteButton: Boolean = true,
    val showPipButton: Boolean = true,
    val showRotateButton: Boolean = true,
    val showLockButton: Boolean = true,
    val showDecoderButton: Boolean = true,
    val showAspectButton: Boolean = true,
    val showTracksButton: Boolean = true,
    val showNowPlayingButton: Boolean = true,
    val showPrevNextButtons: Boolean = true,
    val doubleTapSkipEnabled: Boolean = true,
    val holdSpeedEnabled: Boolean = true,
    val holdDefaultSpeed: Float = 2.0f,
    val swipeGesturesEnabled: Boolean = true,
    val pinchZoomEnabled: Boolean = true,
    val fzHiddenToolbarItems: Set<String> = emptySet()
)

data class SubtitleStyleConfig(
    val enabled: Boolean = true,
    val fontSizeSp: Float = 18f,
    val textColorHex: Long = 0xFFFFFFFF,
    val backgroundOpacity: Float = 0.55f,
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.OUTLINE,
    val bold: Boolean = false,
    val bottomPaddingDp: Float = 44f
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val appColorSource: AppColorSource = AppColorSource.WALLPAPER,
    val customThemeColorHex: Long = AppCustomColorPreset.ELECTRIC_VIOLET.seedHex,
    val decoderMode: DecoderMode = DecoderMode.AUTO,
    val decoderFallbackEnabled: Boolean = true,
    val tunneledPlaybackEnabled: Boolean = false,
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    val nowPlayingConfig: NowPlayingConfig = NowPlayingConfig(),
    val runningTimeConfig: RunningTimeConfig = RunningTimeConfig(),
    val appIconOverlayConfig: AppIconOverlayConfig = AppIconOverlayConfig(),
    val playerUiConfig: PlayerUiConfig = PlayerUiConfig(),
    val backgroundPlaybackEnabled: Boolean = true,
    val continuePlayingDuringCall: Boolean = false,
    val autoPipEnabled: Boolean = true,
    val forceLandscapeOnOpen: Boolean = false,
    val autoResumeLastVideoOnRestart: Boolean = true,
    val midVideoBreakEnabled: Boolean = true,
    val hasSeenPlayerGestureHelp: Boolean = false,
    val hasPromptedMediaAccess: Boolean = false,
    val videoSortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val privatePin: String? = null,
    val dismissedUpdateVersion: String? = null,
    val lastSeenUpdateVersionCode: Int = 0,
    val updateManifestUrl: String = DEFAULT_UPDATE_MANIFEST_URL
) {
    val isHardwareAccelerationEnabled: Boolean
        get() = decoderMode != DecoderMode.SOFTWARE

    companion object {
        const val DEFAULT_UPDATE_MANIFEST_URL = "https://github.com/GOOININDIA/FAZA-Player"
        const val LEGACY_UPDATE_MANIFEST_URL = "https://updates.gooin.in/faza-player/version.json"
    }
}
