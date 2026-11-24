package com.example

import com.example.data.model.DecoderMode
import com.example.data.model.NetworkStreamValidator
import com.example.data.model.NowPlayingConfig
import com.example.data.model.NowPlayingPosition
import com.example.data.model.NowPlayingStyle
import com.example.data.model.NowPlayingTheme
import com.example.data.model.PlayerAccentPreset
import com.example.data.model.PlayerBarTheme
import com.example.data.model.PlayerControlSize
import com.example.data.model.PlayerControlStyle
import com.example.data.model.PlayerUiConfig
import com.example.data.model.ResolutionTier
import com.example.data.model.RunningTimeConfig
import com.example.data.model.RunningTimeDisplayMode
import com.example.data.model.RunningTimeStyle
import com.example.data.model.RunningTimeTheme
import com.example.data.model.SeekBarStyle
import com.example.data.model.SortOption
import com.example.data.model.StreamProtocol
import com.example.data.model.StreamUrlValidation
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import com.example.data.model.VideoItem
import com.example.data.repository.UpdateRepository
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FazaPlayerUnitTest {

    @Test
    fun resolutionTier_classifies8k4k1080p720pAccurately() {
        val video8k = VideoItem(uri = "file://8k.mkv", title = "8K.mkv", folderName = "Movies", width = 7680, height = 4320)
        val video4k = VideoItem(uri = "file://4k.mp4", title = "4K.mp4", folderName = "Movies", width = 3840, height = 2160)
        val video1080 = VideoItem(uri = "file://1080.mp4", title = "1080.mp4", folderName = "Movies", width = 1920, height = 1080)
        val video720 = VideoItem(uri = "file://720.mp4", title = "720.mp4", folderName = "Movies", width = 1280, height = 720)
        val videoSd = VideoItem(uri = "file://sd.avi", title = "sd.avi", folderName = "Movies", width = 640, height = 480)

        assertEquals(ResolutionTier.UHD_8K, video8k.resolutionTier)
        assertEquals("8K UHD", video8k.resolutionTier.badge)
        assertEquals(ResolutionTier.UHD_4K, video4k.resolutionTier)
        assertEquals(ResolutionTier.FHD_1080P, video1080.resolutionTier)
        assertEquals(ResolutionTier.HD_720P, video720.resolutionTier)
        assertEquals(ResolutionTier.SD, videoSd.resolutionTier)
    }

    @Test
    fun updateRepository_semverComparisonWorksCorrectly() {
        val repo = UpdateRepository()
        assertTrue(repo.isVersionNewer("2.0.0", "1.0.0"))
        assertTrue(repo.isVersionNewer("1.0.1", "1.0.0"))
        assertTrue(repo.isVersionNewer("v1.2.0", "1.1.9"))
        assertTrue(repo.isVersionNewer("1.1 beta", "1.0 beta"))
        assertFalse(repo.isVersionNewer("1.1 beta", "1.1 beta"))
        assertFalse(repo.isVersionNewer("1.0.0", "1.0.0"))
        assertFalse(repo.isVersionNewer("0.9.9", "1.0.0"))
    }

    @Test
    fun formatters_durationAndSizeAndSignedDelta() {
        assertEquals("00:45", Formatters.formatDuration(45_000L))
        assertEquals("1:02:05", Formatters.formatDuration(3_725_000L))
        assertEquals("+00:15", Formatters.formatSignedDelta(15_000L))
        assertEquals("-00:10", Formatters.formatSignedDelta(-10_000L))
        assertEquals("Stream", Formatters.formatFileSize(0L))
        assertEquals("1.00 GB", Formatters.formatFileSize(1024L * 1024L * 1024L))
    }

    @Test
    fun userPreferences_materialYouAndNowPlayingDefaults() {
        val defaults = UserPreferences()
        assertTrue(defaults.dynamicColorEnabled)
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertEquals(com.example.data.model.AppColorSource.WALLPAPER, defaults.appColorSource)
        assertEquals(com.example.data.model.AppCustomColorPreset.ELECTRIC_VIOLET.seedHex, defaults.customThemeColorHex)
        assertEquals(androidx.compose.ui.text.font.FontFamily.Default, com.example.ui.theme.FazaTypography.bodyMedium.fontFamily)
        assertEquals(androidx.compose.ui.text.font.FontFamily.Default, com.example.ui.theme.FazaTypography.titleLarge.fontFamily)
        assertEquals(DecoderMode.AUTO, defaults.decoderMode)
        assertTrue(defaults.decoderMode != DecoderMode.SOFTWARE)
        assertFalse(defaults.forceLandscapeOnOpen)
        assertTrue(defaults.copy(forceLandscapeOnOpen = true).forceLandscapeOnOpen)
        assertFalse(defaults.hasSeenPlayerGestureHelp)
        assertTrue(defaults.copy(hasSeenPlayerGestureHelp = true).hasSeenPlayerGestureHelp)
        assertTrue(defaults.nowPlayingConfig.enabled)
        assertEquals(NowPlayingTheme.MATERIAL_YOU, defaults.nowPlayingConfig.theme)
        assertEquals(NowPlayingStyle.COMPACT_PILL, defaults.nowPlayingConfig.style)
        assertEquals(NowPlayingPosition.TOP_CENTER, defaults.nowPlayingConfig.position)
        assertEquals(1, defaults.nowPlayingConfig.maxLines)
        assertTrue(defaults.nowPlayingConfig.showLiveDot)

        val disabled = NowPlayingConfig(
            enabled = false,
            theme = NowPlayingTheme.HIGH_CONTRAST,
            style = NowPlayingStyle.SLIM_BANNER,
            position = NowPlayingPosition.BOTTOM_CENTER,
            maxLines = 2,
            showLiveDot = false
        )
        assertFalse(disabled.enabled)
        assertEquals(NowPlayingTheme.HIGH_CONTRAST, disabled.theme)
        assertEquals(NowPlayingStyle.SLIM_BANNER, disabled.style)
        assertEquals(NowPlayingPosition.BOTTOM_CENTER, disabled.position)
        assertEquals(2, disabled.maxLines)
        assertFalse(disabled.showLiveDot)
    }

    @Test
    fun runningTimeConfig_defaultsFormatRemainingAndCycleModesWorkCorrectly() {
        val defaults = UserPreferences().runningTimeConfig
        assertTrue(defaults.enabled)
        assertEquals(RunningTimeTheme.MATERIAL_YOU, defaults.theme)
        assertEquals(RunningTimeStyle.COMPACT_PILL, defaults.style)
        assertEquals(1.0f, defaults.fontScale, 0.001f)
        assertEquals(RunningTimeDisplayMode.ELAPSED_REMAINING, defaults.displayMode)
        assertTrue(defaults.showTotalDuration)

        // Exact user specification: 12:00 elapsed out of 14:00 total -> 12:00/02:00
        assertEquals(
            "12:00/02:00",
            com.example.ui.components.formatRunningTimeLabel(
                positionMs = 720_000L,
                durationMs = 840_000L,
                displayMode = RunningTimeDisplayMode.ELAPSED_REMAINING
            )
        )
        assertEquals(
            "12:00/14:00",
            com.example.ui.components.formatRunningTimeLabel(
                positionMs = 720_000L,
                durationMs = 840_000L,
                displayMode = RunningTimeDisplayMode.ELAPSED_TOTAL
            )
        )
        assertEquals(
            "12:00",
            com.example.ui.components.formatRunningTimeLabel(
                positionMs = 720_000L,
                durationMs = 840_000L,
                displayMode = RunningTimeDisplayMode.ELAPSED_ONLY
            )
        )

        // Boundary & live-stream checks
        assertEquals(
            "14:00/00:00",
            com.example.ui.components.formatRunningTimeLabel(900_000L, 840_000L, RunningTimeDisplayMode.ELAPSED_REMAINING)
        )
        assertEquals(
            "12:00",
            com.example.ui.components.formatRunningTimeLabel(720_000L, 0L, RunningTimeDisplayMode.ELAPSED_REMAINING)
        )
        assertEquals(
            "1:02:05/27:55",
            com.example.ui.components.formatRunningTimeLabel(3_725_000L, 5_400_000L, RunningTimeDisplayMode.ELAPSED_REMAINING)
        )

        // Tap-to-cycle deterministic sequence
        assertEquals(RunningTimeDisplayMode.ELAPSED_TOTAL, RunningTimeDisplayMode.ELAPSED_REMAINING.next())
        assertEquals(RunningTimeDisplayMode.ELAPSED_ONLY, RunningTimeDisplayMode.ELAPSED_TOTAL.next())
        assertEquals(RunningTimeDisplayMode.ELAPSED_REMAINING, RunningTimeDisplayMode.ELAPSED_ONLY.next())

        // Legacy DataStore migration fallback
        assertEquals(RunningTimeDisplayMode.ELAPSED_REMAINING, com.example.data.local.resolveRunningTimeDisplayMode(null, null))
        assertEquals(RunningTimeDisplayMode.ELAPSED_REMAINING, com.example.data.local.resolveRunningTimeDisplayMode(null, true))
        assertEquals(RunningTimeDisplayMode.ELAPSED_ONLY, com.example.data.local.resolveRunningTimeDisplayMode(null, false))
        assertEquals(RunningTimeDisplayMode.ELAPSED_TOTAL, com.example.data.local.resolveRunningTimeDisplayMode("ELAPSED_TOTAL", false))

        val customized = RunningTimeConfig(
            enabled = false,
            theme = RunningTimeTheme.HIGH_CONTRAST,
            style = RunningTimeStyle.ROUNDED_CARD,
            fontScale = 1.45f,
            displayMode = RunningTimeDisplayMode.ELAPSED_ONLY,
            position = com.example.data.model.RunningTimePosition.CUSTOM,
            customXFraction = 0.25f,
            customYFraction = 0.75f
        )
        assertFalse(customized.enabled)
        assertEquals(RunningTimeTheme.HIGH_CONTRAST, customized.theme)
        assertEquals(RunningTimeStyle.ROUNDED_CARD, customized.style)
        assertEquals(1.45f, customized.fontScale, 0.001f)
        assertFalse(customized.showTotalDuration)

        // Customized placement defaults & bias resolution across presets and custom coordinates
        assertEquals(com.example.data.model.RunningTimePosition.TOP_END, defaults.position)
        assertTrue(com.example.data.model.RunningTimePosition.TOP_START.isTopRow)
        assertTrue(com.example.data.model.RunningTimePosition.BOTTOM_CENTER.isBottomRow)
        assertFalse(com.example.data.model.RunningTimePosition.CUSTOM.isTopRow)
        assertEquals(1f to -1f, com.example.ui.components.resolveRunningTimeBias(defaults))
        assertEquals(
            -1f to 1f,
            com.example.ui.components.resolveRunningTimeBias(
                defaults.copy(position = com.example.data.model.RunningTimePosition.BOTTOM_START)
            )
        )
        val (customHx, customVy) = com.example.ui.components.resolveRunningTimeBias(customized)
        assertEquals(-0.5f, customHx, 0.001f)
        assertEquals(0.5f, customVy, 0.001f)

        // Clamping out-of-bounds custom fractions keeps badge inside screen bounds
        val (clampedHx, clampedVy) = com.example.ui.components.resolveRunningTimeBias(
            defaults.copy(
                position = com.example.data.model.RunningTimePosition.CUSTOM,
                customXFraction = 1.5f,
                customYFraction = -0.3f
            )
        )
        assertEquals(1f, clampedHx, 0.001f)
        assertEquals(-1f, clampedVy, 0.001f)
    }

    @Test
    fun appIconOverlayConfig_defaultsCustomizationAndPlacementBiasWorkCorrectly() {
        val defaults = UserPreferences().appIconOverlayConfig
        // Optional by default
        assertFalse(defaults.enabled)
        assertEquals(com.example.data.model.AppIconOverlayStyle.ROUNDED_BADGE, defaults.style)
        assertEquals(com.example.data.model.AppIconOverlayTheme.FROSTED_DARK, defaults.theme)
        assertEquals(com.example.data.model.AppIconOverlayPosition.TOP_START, defaults.position)
        assertEquals(1.0f, defaults.sizeScale, 0.001f)
        assertEquals(0.85f, defaults.opacity, 0.001f)
        assertTrue(defaults.showBorder)

        // Preset placement bias resolution
        assertTrue(com.example.data.model.AppIconOverlayPosition.TOP_START.isTopRow)
        assertTrue(com.example.data.model.AppIconOverlayPosition.BOTTOM_END.isBottomRow)
        assertFalse(com.example.data.model.AppIconOverlayPosition.CUSTOM.isTopRow)
        assertEquals(-1f to -1f, com.example.ui.components.resolveAppIconOverlayBias(defaults))
        assertEquals(
            1f to 1f,
            com.example.ui.components.resolveAppIconOverlayBias(
                defaults.copy(position = com.example.data.model.AppIconOverlayPosition.BOTTOM_END)
            )
        )

        // Custom free-placement coordinates & clamping
        val custom = defaults.copy(
            enabled = true,
            style = com.example.data.model.AppIconOverlayStyle.PILL_BRAND,
            theme = com.example.data.model.AppIconOverlayTheme.MATERIAL_YOU,
            position = com.example.data.model.AppIconOverlayPosition.CUSTOM,
            sizeScale = 1.25f,
            opacity = 0.70f,
            showBorder = false,
            customXFraction = 0.75f,
            customYFraction = 0.25f
        )
        assertTrue(custom.enabled)
        assertEquals(com.example.data.model.AppIconOverlayStyle.PILL_BRAND, custom.style)
        assertEquals(com.example.data.model.AppIconOverlayTheme.MATERIAL_YOU, custom.theme)
        assertFalse(custom.showBorder)
        val (hx, vy) = com.example.ui.components.resolveAppIconOverlayBias(custom)
        assertEquals(0.5f, hx, 0.001f)
        assertEquals(-0.5f, vy, 0.001f)

        val (clampedHx, clampedVy) = com.example.ui.components.resolveAppIconOverlayBias(
            custom.copy(customXFraction = -0.4f, customYFraction = 1.9f)
        )
        assertEquals(-1f, clampedHx, 0.001f)
        assertEquals(1f, clampedVy, 0.001f)

        // Optional animation defaults: static by default, only Gemini Glow animation available
        assertEquals(com.example.data.model.AppIconOverlayAnimation.NONE, defaults.animation)
        assertEquals(
            listOf(
                com.example.data.model.AppIconOverlayAnimation.NONE,
                com.example.data.model.AppIconOverlayAnimation.GEMINI_GLOW
            ),
            com.example.data.model.AppIconOverlayAnimation.entries
        )
        assertEquals(1.0f, defaults.animationSpeed, 0.001f)
        assertTrue(defaults.animateOnlyWhilePlaying)
        assertEquals(1400, com.example.ui.components.resolveAppIconAnimationDurationMs(1400, 1.0f))
        assertEquals(700, com.example.ui.components.resolveAppIconAnimationDurationMs(1400, 2.0f))
        assertEquals(2800, com.example.ui.components.resolveAppIconAnimationDurationMs(1400, 0.5f))
        assertEquals(700, com.example.ui.components.resolveAppIconAnimationDurationMs(1400, 9.0f))
        assertEquals(
            com.example.ui.components.sampleGeminiGlowSpectrum(0f),
            com.example.ui.components.sampleGeminiGlowSpectrum(1f)
        )
        assertNotEquals(
            com.example.ui.components.sampleGeminiGlowSpectrum(0f),
            com.example.ui.components.sampleGeminiGlowSpectrum(0.4f)
        )

        // Optional video language & One Line / Two Lines defaults + native script resolver
        assertFalse(defaults.showVideoLanguage)
        assertEquals(com.example.data.model.AppIconLanguageLayout.ONE_LINE, defaults.languageLayout)
        assertEquals("हिंदी", com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("hi"))
        assertEquals("हिंदी", com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("hin"))
        assertEquals("English", com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("en-US"))
        assertEquals("বাংলা", com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("bn"))
        assertEquals("বাংলা", com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("ben"))
        assertEquals("தமிழ்", com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("ta"))
        org.junit.Assert.assertNull(com.example.util.VideoLanguageResolver.resolveLanguageCodeOrName("und"))

        val selectedBengali = listOf(
            com.example.player.TrackOption("a_0_0", "English • 2ch • AAC", "en", 0, 0, isSelected = false),
            com.example.player.TrackOption("a_0_1", "Bengali • 6ch • EAC3", "bn", 0, 1, isSelected = true)
        )
        assertEquals(
            "বাংলা",
            com.example.util.VideoLanguageResolver.resolveFromAudioTracks(selectedBengali, "Movie.mkv")
        )
        assertEquals(
            "हिंदी",
            com.example.util.VideoLanguageResolver.resolveFromAudioTracks(
                emptyList(),
                "Action.Movie.2026.Hindi.1080p.mkv"
            )
        )
    }

    @Test
    fun videoDeduplicator_collapsesCrossSourceDuplicatesAndMergesUserState() {
        val mkvDurationMs = 6_362_100L // 1:46:02
        val mp4DurationMs = 5_020L     // 00:05

        val rawScanned = listOf(
            // Same 1:46:02 MKV from file://, MediaStore, and SAF Download tree
            VideoItem(
                id = 1L,
                uri = "file:///storage/emulated/0/Download/Cinema_Movie.mkv",
                title = "Cinema_Movie.mkv",
                folderName = "Download",
                durationMs = mkvDurationMs,
                sizeBytes = 2_450_000_000L,
                width = 1920,
                height = 1080,
                formatTag = "MKV",
                isFavourite = false
            ),
            VideoItem(
                id = 2L,
                uri = "content://media/external/video/media/4021",
                title = "Cinema_Movie.mkv",
                folderName = "Download",
                durationMs = 6_362_450L, // slight retriever/MediaStore ms delta
                sizeBytes = 2_450_000_000L,
                width = 1920,
                height = 1080,
                formatTag = "MKV",
                isFavourite = true,
                lastPlayedAt = 1_700_000_000L,
                lastPositionMs = 920_000L
            ),
            VideoItem(
                id = 3L,
                uri = "content://com.android.externalstorage.documents/tree/primary%3ADownload/document/primary%3ADownload%2FCinema_Movie.mkv",
                title = "Cinema_Movie.mkv",
                folderName = "Download",
                durationMs = mkvDurationMs,
                sizeBytes = 2_450_000_000L,
                width = 1920,
                height = 1080,
                formatTag = "MKV",
                subtitleUri = "file:///storage/emulated/0/Download/Cinema_Movie.srt"
            ),
            // Same 00:05 MP4 from /sdcard alias, file://, and MediaStore
            VideoItem(
                id = 4L,
                uri = "file:///sdcard/Download/Short%20Clip.mp4",
                title = "Short Clip.mp4",
                folderName = "Download",
                durationMs = mp4DurationMs,
                sizeBytes = 4_200_000L,
                width = 1280,
                height = 720,
                formatTag = "MP4"
            ),
            VideoItem(
                id = 5L,
                uri = "content://media/external/video/media/4022",
                title = "Short Clip.mp4",
                folderName = "Download",
                durationMs = 4_980L, // rounds to same 5s
                sizeBytes = 4_200_000L,
                width = 1280,
                height = 720,
                formatTag = "MP4"
            ),
            // Distinct file in Download that must remain separate
            VideoItem(
                id = 6L,
                uri = "content://media/external/video/media/4023",
                title = "Other_Clip.mp4",
                folderName = "Download",
                durationMs = 5_000L,
                sizeBytes = 4_200_000L,
                width = 1280,
                height = 720,
                formatTag = "MP4"
            )
        )

        val outcome = com.example.util.VideoDeduplicator.deduplicateWithRemovals(rawScanned)
        assertEquals(6, outcome.rawCount)
        assertEquals(3, outcome.deduplicatedCount)
        assertEquals(3, outcome.duplicatesRemovedCount)
        assertEquals(3, outcome.duplicateIdsToDelete.size)

        val mkv = outcome.uniqueVideos.first { it.title == "Cinema_Movie.mkv" }
        assertTrue(mkv.isFavourite)
        assertEquals(920_000L, mkv.lastPositionMs)
        assertEquals("file:///storage/emulated/0/Download/Cinema_Movie.srt", mkv.subtitleUri)

        // VideoDeduplicationIndex rejects re-scans across SAF, /sdcard, and MediaStore
        val index = com.example.util.VideoDeduplicationIndex(outcome.uniqueVideos)
        assertTrue(
            index.contains(
                uri = "content://media/external/video/media/99999",
                title = "Cinema_Movie.mkv",
                folderName = "Download",
                sizeBytes = 2_450_000_000L,
                durationMs = 6_362_000L,
                knownFilePath = "/storage/emulated/0/Download/Cinema_Movie.mkv"
            )
        )
        assertTrue(
            index.contains(
                uri = "file:///storage/emulated/0/Download/Short%20Clip.mp4",
                title = "Short Clip.mp4",
                folderName = "Download",
                sizeBytes = 4_200_000L,
                durationMs = 5_000L
            )
        )
        assertFalse(
            index.contains(
                uri = "file:///storage/emulated/0/Download/Brand_New.mp4",
                title = "Brand_New.mp4",
                folderName = "Download",
                sizeBytes = 12_000_000L,
                durationMs = 18_000L
            )
        )
    }

    @Test
    fun updateRepository_parsesVersionJsonAndComparesVersions() {
        val repo = UpdateRepository()

        val standardJson = """
            {
              "applicationName": "FAZA Player",
              "latestVersion": "2.0 beta",
              "latestVersionCode": 12,
              "releaseDate": "December 2026",
              "downloadUrl": "https://updates.gooin.in/faza-player/faza-2.0.apk",
              "newFeatures": ["Remote banner notification"]
            }
        """.trimIndent()
        val parsedStandard = repo.parseManifest(standardJson)
        assertEquals("2.0 beta", parsedStandard.latestVersion)
        assertEquals(12, parsedStandard.latestVersionCode)
        assertTrue(repo.isUpdateAvailable(parsedStandard, currentVersionCode = 9, currentVersionName = "1.8 beta"))

        val compactAliasJson = """
            {
              "version": "1.9 beta",
              "versionCode": 9,
              "url": "https://updates.gooin.in/faza-player/latest.apk"
            }
        """.trimIndent()
        val parsedCompact = repo.parseManifest(compactAliasJson)
        assertEquals("1.9 beta", parsedCompact.latestVersion)
        assertEquals(9, parsedCompact.latestVersionCode)
        assertEquals("https://updates.gooin.in/faza-player/latest.apk", parsedCompact.downloadUrl)
        assertTrue(repo.isUpdateAvailable(parsedCompact, currentVersionCode = 9, currentVersionName = "1.8 beta"))

        val sameOrOlder = parsedCompact.copy(latestVersion = "1.8 beta", latestVersionCode = 9)
        assertFalse(repo.isUpdateAvailable(sameOrOlder, currentVersionCode = 9, currentVersionName = "1.8 beta"))

        val codeBumpOnly = parsedCompact.copy(latestVersion = "1.8 beta", latestVersionCode = 10)
        assertTrue(repo.isUpdateAvailable(codeBumpOnly, currentVersionCode = 9, currentVersionName = "1.8 beta"))
    }

    @Test
    fun playerUiConfig_defaultsAndFullCustomizationOptionsWork() {
        val defaults = UserPreferences().playerUiConfig
        assertEquals(PlayerAccentPreset.DYNAMIC_PRIMARY, defaults.accentPreset)
        assertEquals(PlayerBarTheme.MATERIAL_YOU, defaults.barTheme)
        assertEquals(PlayerControlStyle.STANDARD, defaults.controlStyle)
        assertEquals(PlayerControlSize.MEDIUM, defaults.controlSize)
        assertEquals(SeekBarStyle.STANDARD, defaults.seekBarStyle)
        assertEquals(10, defaults.seekStepSeconds)
        assertEquals(4, defaults.autoHideTimeoutSec)
        assertTrue(defaults.doubleTapSkipEnabled)
        assertTrue(defaults.holdSpeedEnabled)
        assertEquals(2.0f, defaults.holdDefaultSpeed, 0.001f)
        assertTrue(defaults.showCenterSkipButtons)
        assertTrue(defaults.showLockButton)

        val custom = PlayerUiConfig(
            accentPreset = PlayerAccentPreset.CYAN,
            barTheme = PlayerBarTheme.AMOLED_BLACK,
            controlStyle = PlayerControlStyle.PILL,
            controlSize = PlayerControlSize.LARGE,
            seekBarStyle = SeekBarStyle.THICK_BAR,
            hudOpacity = 0.92f,
            autoHideTimeoutSec = 0,
            seekStepSeconds = 15,
            showCenterSkipButtons = false,
            showFavouriteButton = false,
            doubleTapSkipEnabled = true,
            holdSpeedEnabled = true,
            holdDefaultSpeed = 2.5f,
            swipeGesturesEnabled = false,
            pinchZoomEnabled = false
        )
        assertEquals(PlayerAccentPreset.CYAN, custom.accentPreset)
        assertEquals(0xFF22D3EE, custom.accentPreset.colorHex)
        assertEquals(PlayerBarTheme.AMOLED_BLACK, custom.barTheme)
        assertEquals(PlayerControlStyle.PILL, custom.controlStyle)
        assertEquals(PlayerControlSize.LARGE, custom.controlSize)
        assertEquals(SeekBarStyle.THICK_BAR, custom.seekBarStyle)
        assertEquals(15, custom.seekStepSeconds)
        assertEquals(0, custom.autoHideTimeoutSec)
        assertEquals(2.5f, custom.holdDefaultSpeed, 0.001f)
        assertFalse(custom.showCenterSkipButtons)
        assertFalse(custom.swipeGesturesEnabled)
        assertFalse(custom.pinchZoomEnabled)
    }

    @Test
    fun advancedSettings_hardwareAccelerationAndTroubleshootingFlagsWork() {
        val defaults = UserPreferences()
        assertTrue(defaults.isHardwareAccelerationEnabled)
        assertEquals(DecoderMode.AUTO, defaults.decoderMode)
        assertTrue(defaults.decoderFallbackEnabled)
        assertFalse(defaults.tunneledPlaybackEnabled)

        val hwStrict = defaults.copy(
            decoderMode = DecoderMode.HARDWARE,
            decoderFallbackEnabled = false,
            tunneledPlaybackEnabled = true
        )
        assertTrue(hwStrict.isHardwareAccelerationEnabled)
        assertFalse(hwStrict.decoderFallbackEnabled)
        assertTrue(hwStrict.tunneledPlaybackEnabled)

        val swTroubleshoot = defaults.copy(
            decoderMode = DecoderMode.SOFTWARE,
            decoderFallbackEnabled = true,
            tunneledPlaybackEnabled = false
        )
        assertFalse(swTroubleshoot.isHardwareAccelerationEnabled)
        assertTrue(swTroubleshoot.decoderFallbackEnabled)
        assertFalse(swTroubleshoot.tunneledPlaybackEnabled)
    }

    @Test
    fun networkStreamValidator_validatesUrlsAndDetectsProtocols() {
        assertEquals(StreamUrlValidation.Empty, NetworkStreamValidator.validateUrl("   "))

        val missingScheme = NetworkStreamValidator.validateUrl("cdn.example.com/live/stream.m3u8")
        assertTrue(missingScheme is StreamUrlValidation.Invalid)
        assertTrue((missingScheme as StreamUrlValidation.Invalid).canPrependHttps)

        val unsupportedScheme = NetworkStreamValidator.validateUrl("ftp://example.com/video.mp4")
        assertTrue(unsupportedScheme is StreamUrlValidation.Invalid)
        assertFalse((unsupportedScheme as StreamUrlValidation.Invalid).canPrependHttps)

        val hlsValid = NetworkStreamValidator.validateUrl("  <https://stream.mux.dev/test.m3u8?token=abc> ")
        assertTrue(hlsValid is StreamUrlValidation.Valid)
        val parsedHls = hlsValid as StreamUrlValidation.Valid
        assertEquals("https://stream.mux.dev/test.m3u8?token=abc", parsedHls.normalizedUrl)
        assertEquals(StreamProtocol.HLS, parsedHls.detectedProtocol)
        assertEquals("stream.mux.dev", parsedHls.hostLabel)

        val dashValid = NetworkStreamValidator.validateUrl("http://media.example.org/manifest.mpd") as StreamUrlValidation.Valid
        assertEquals(StreamProtocol.DASH, dashValid.detectedProtocol)

        val forcedHlsMeta = NetworkStreamValidator.resolveStreamMetadata(
            url = "https://edge.example.com/live/channel42",
            selectedProtocol = StreamProtocol.HLS,
            customTitle = "Live Sports"
        )
        assertEquals("Live Sports", forcedHlsMeta.title)
        assertEquals("HLS", forcedHlsMeta.formatTag)
        assertEquals("application/x-mpegURL", forcedHlsMeta.mimeType)
    }

    @Test
    fun callPlaybackPreference_defaultsOffAndTogglesOptionalVoicePlayback() {
        val defaults = UserPreferences()
        assertFalse(defaults.continuePlayingDuringCall)

        val enabled = defaults.copy(continuePlayingDuringCall = true)
        assertTrue(enabled.continuePlayingDuringCall)
    }

    @Test
    fun floatingDockBadge_formatsDynamicUnreadCountAccurately() {
        assertEquals("", com.example.ui.components.formatDockBadgeCount(0))
        assertEquals("", com.example.ui.components.formatDockBadgeCount(-2))
        assertEquals("1", com.example.ui.components.formatDockBadgeCount(1))
        assertEquals("2", com.example.ui.components.formatDockBadgeCount(2))
        assertEquals("99", com.example.ui.components.formatDockBadgeCount(99))
        assertEquals("99+", com.example.ui.components.formatDockBadgeCount(100))
    }

    @Test
    fun homeSearchFilter_matchesTitleFolderFormatAndResolution() {
        val sampleVideos = listOf(
            VideoItem(id = 1L, uri = "file://1.mkv", title = "Dune Part Two", folderName = "Movies", mimeType = "video/x-matroska", formatTag = "MKV", width = 3840, height = 2160),
            VideoItem(id = 2L, uri = "file://2.mp4", title = "Vacation Highlights", folderName = "Camera", mimeType = "video/mp4", formatTag = "MP4", width = 1920, height = 1080),
            VideoItem(id = 3L, uri = "https://cdn.example.com/live.m3u8", title = "News Live", folderName = "Network Streams", mimeType = "application/x-mpegURL", formatTag = "HLS", width = 1280, height = 720)
        )

        assertEquals(3, com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "   ").size)
        assertEquals(listOf(1L), com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "dune").map { it.id })
        assertEquals(listOf(2L), com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "camera").map { it.id })
        assertEquals(listOf(1L), com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "mkv").map { it.id })
        assertEquals(listOf(1L), com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "4k").map { it.id })
        assertEquals(listOf(3L), com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "hls").map { it.id })
        assertTrue(com.example.ui.viewmodel.LibraryViewModel.filterVideosByQuery(sampleVideos, "nonexistent").isEmpty())
    }

    @Test
    fun homeVideoSorting_ordersVideosAcrossAllSortOptions() {
        val alphaSmallShort = VideoItem(
            id = 1L,
            uri = "file://a.mp4",
            title = "Alpha Clip",
            folderName = "Camera",
            dateAdded = 100L,
            durationMs = 15_000L,
            sizeBytes = 5_000_000L,
            width = 1280,
            height = 720
        )
        val betaLargeLong = VideoItem(
            id = 2L,
            uri = "file://b.mkv",
            title = "Beta Movie",
            folderName = "Movies",
            dateAdded = 200L,
            durationMs = 120_000L,
            sizeBytes = 250_000_000L,
            width = 1920,
            height = 1080
        )
        val gammaNewest4K = VideoItem(
            id = 3L,
            uri = "file://c.mkv",
            title = "Gamma Drone",
            folderName = "Drones",
            dateAdded = 300L,
            durationMs = 45_000L,
            sizeBytes = 120_000_000L,
            width = 3840,
            height = 2160
        )
        val items = listOf(alphaSmallShort, betaLargeLong, gammaNewest4K)

        assertEquals(SortOption.DATE_ADDED_DESC, UserPreferences().videoSortOption)
        assertEquals(
            listOf(3L, 2L, 1L),
            com.example.ui.viewmodel.LibraryViewModel.sortVideos(items, SortOption.DATE_ADDED_DESC).map { it.id }
        )
        assertEquals(
            listOf(1L, 2L, 3L),
            com.example.ui.viewmodel.LibraryViewModel.sortVideos(items, SortOption.NAME_ASC).map { it.id }
        )
        assertEquals(
            listOf(3L, 2L, 1L),
            com.example.ui.viewmodel.LibraryViewModel.sortVideos(items, SortOption.NAME_DESC).map { it.id }
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            com.example.ui.viewmodel.LibraryViewModel.sortVideos(items, SortOption.DURATION_DESC).map { it.id }
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            com.example.ui.viewmodel.LibraryViewModel.sortVideos(items, SortOption.SIZE_DESC).map { it.id }
        )
        assertEquals(
            listOf(3L, 2L, 1L),
            com.example.ui.viewmodel.LibraryViewModel.sortVideos(items, SortOption.RESOLUTION_DESC).map { it.id }
        )
    }

    @Test
    fun githubUpdateHost_parsesRepoUrlsAndDetectsNewReleasesAndBetaTags() {
        val repo = UpdateRepository()
        assertEquals("https://github.com/GOOININDIA/FAZA-Player", UserPreferences().updateManifestUrl)

        val cleanRef = repo.parseGitHubRepoUrl("https://github.com/GOOININDIA/FAZA-Player")
        assertEquals("GOOININDIA", cleanRef?.owner)
        assertEquals("FAZA-Player", cleanRef?.repo)

        val trailingHyphenRef = repo.parseGitHubRepoUrl("https://github.com/GOOININDIA/FAZA-Player-")
        assertEquals("GOOININDIA", trailingHyphenRef?.owner)
        assertEquals("FAZA-Player", trailingHyphenRef?.repo)

        assertEquals(null, repo.parseGitHubRepoUrl("https://updates.example.com/version.json"))

        // Releases API with beta/prerelease + APK asset
        val releasesJson = """
            [
              {
                "tag_name": "v1.23-beta",
                "name": "FAZA Player 1.23 Beta",
                "body": "- Added new audio equalizer\n- Improved 8K buffering",
                "draft": false,
                "prerelease": true,
                "html_url": "https://github.com/GOOININDIA/FAZA-Player/releases/tag/v1.23-beta",
                "published_at": "2026-11-16T10:00:00Z",
                "assets": [
                  {
                    "name": "FAZA-Player-v1.23-beta.apk",
                    "browser_download_url": "https://github.com/GOOININDIA/FAZA-Player/releases/download/v1.23-beta/FAZA-Player-v1.23-beta.apk"
                  }
                ]
              }
            ]
        """.trimIndent()

        val releaseState = repo.resolveGitHubUpdateState(
            repo = cleanRef!!,
            releasesJson = releasesJson,
            tagsJson = "[]",
            currentVersionCode = 23,
            currentVersionName = "1.22 beta"
        )
        assertTrue(releaseState is com.example.data.model.UpdateCheckState.UpdateAvailable)
        val releaseManifest = (releaseState as com.example.data.model.UpdateCheckState.UpdateAvailable).manifest
        assertEquals("1.23-beta", releaseManifest.latestVersion)
        assertEquals("GitHub Beta", releaseManifest.sourceLabel)
        assertEquals(
            "https://github.com/GOOININDIA/FAZA-Player/releases/download/v1.23-beta/FAZA-Player-v1.23-beta.apk",
            releaseManifest.downloadUrl
        )

        // Tags API fallback when a bare tag is pushed without creating a Release
        val tagOnlyState = repo.resolveGitHubUpdateState(
            repo = cleanRef,
            releasesJson = "[]",
            tagsJson = """[{"name": "v1.24-beta.2"}, {"name": "v1.24-beta.1"}]""",
            currentVersionCode = 23,
            currentVersionName = "1.22 beta"
        )
        assertTrue(tagOnlyState is com.example.data.model.UpdateCheckState.UpdateAvailable)
        val tagManifest = (tagOnlyState as com.example.data.model.UpdateCheckState.UpdateAvailable).manifest
        assertEquals("1.24-beta.2", tagManifest.latestVersion)
        assertEquals("GitHub Tag", tagManifest.sourceLabel)
        assertEquals(
            "https://github.com/GOOININDIA/FAZA-Player/releases/tag/v1.24-beta.2",
            tagManifest.downloadUrl
        )

        // Up-to-date when newest tag matches installed version
        val upToDateState = repo.resolveGitHubUpdateState(
            repo = cleanRef,
            releasesJson = "[]",
            tagsJson = """[{"name": "v1.22-beta"}]""",
            currentVersionCode = 23,
            currentVersionName = "1.22 beta"
        )
        assertTrue(upToDateState is com.example.data.model.UpdateCheckState.UpToDate)

        // Semver + beta progression checks
        assertTrue(repo.isVersionNewer("v1.23", "1.22 beta"))
        assertTrue(repo.isVersionNewer("v1.22", "1.22 beta"))
        assertTrue(repo.isVersionNewer("v1.23-beta.2", "1.23-beta.1"))
        assertFalse(repo.isVersionNewer("1.22-beta", "1.22 beta"))
    }

    @Test
    fun firstLaunchMediaAccess_defaultsAndFolderScanFilteringRules() {
        val defaults = UserPreferences()
        assertFalse(defaults.hasPromptedMediaAccess)
        assertTrue(defaults.copy(hasPromptedMediaAccess = true).hasPromptedMediaAccess)

        assertTrue(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory("Movies", depth = 0, isRoot = true))
        assertTrue(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory("4K Clips", depth = 4, isRoot = false))
        assertTrue(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory("Subfolder", depth = 6, isRoot = false))
        assertFalse(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory("TooDeep", depth = 7, isRoot = false))
        assertFalse(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory(".thumbnails", depth = 1, isRoot = false))
        assertFalse(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory(".cache", depth = 2, isRoot = false))
        assertFalse(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory("LOST.DIR", depth = 1, isRoot = false))
        assertFalse(com.example.data.repository.VideoLibraryRepository.shouldScanDirectory("Android", depth = 1, isRoot = false))
    }

    @Test
    fun permanentNowPlayingStatus_andFloatingDockSizing() {
        assertEquals("Now Playing", com.example.ui.components.formatNowPlayingStatus(isPlaying = true))
        assertEquals("Pause", com.example.ui.components.formatNowPlayingStatus(isPlaying = false))

        val ultraCompact = com.example.ui.components.calculateFloatingDockDimensions(androidx.compose.ui.unit.Dp(320f))
        assertEquals(56f, ultraCompact.navBarHeight.value, 0.01f)
        assertEquals(50f, ultraCompact.searchButtonSize.value, 0.01f)
        assertTrue(ultraCompact.navBarWidth.value >= 212f)
        assertTrue(ultraCompact.navBarWidth.value + ultraCompact.horizontalGap.value + ultraCompact.searchButtonSize.value <= 320f - 32f + 0.1f)

        val phoneDims = com.example.ui.components.calculateFloatingDockDimensions(androidx.compose.ui.unit.Dp(412f))
        assertEquals(271.92f, phoneDims.navBarWidth.value, 0.2f)
        assertEquals(60f, phoneDims.navBarHeight.value, 0.01f)
        assertEquals(58f, phoneDims.searchButtonSize.value, 0.01f)
        assertTrue(phoneDims.navBarWidth.value / 412f in 0.63f..0.70f)
        assertTrue(phoneDims.navBarWidth.value + phoneDims.horizontalGap.value + phoneDims.searchButtonSize.value <= 412f - 32f + 0.1f)

        val widePhone = com.example.ui.components.calculateFloatingDockDimensions(androidx.compose.ui.unit.Dp(500f))
        assertEquals(290f, widePhone.navBarWidth.value, 0.2f)
        assertEquals(64f, widePhone.navBarHeight.value, 0.01f)
        assertEquals(62f, widePhone.searchButtonSize.value, 0.01f)

        assertEquals(phoneDims.navBarWidth.value, com.example.ui.components.calculateFloatingDockWidth(androidx.compose.ui.unit.Dp(412f)).value, 0.01f)
        assertEquals("", com.example.ui.components.formatDockBadgeCount(0))
        assertEquals("5", com.example.ui.components.formatDockBadgeCount(5))
        assertEquals("99+", com.example.ui.components.formatDockBadgeCount(120))
    }

    @Test
    fun selectLastPlayedVideo_returnsMostRecentNonPrivatePlayableVideo() {
        val unplayed = VideoItem(id = 1L, uri = "file://1.mp4", title = "Unplayed", folderName = "Camera")
        val privateVideo = VideoItem(id = 2L, uri = "file://2.mp4", title = "Secret", folderName = "Vault", isPrivate = true, lastPlayedAt = 999L, lastPositionMs = 45_000L)
        val olderPlayed = VideoItem(id = 3L, uri = "file://3.mp4", title = "Older", folderName = "Movies", lastPlayedAt = 500L, lastPositionMs = 12_000L)
        val newestPlayed = VideoItem(id = 4L, uri = "file://4.mp4", title = "Newest", folderName = "Movies", lastPlayedAt = 800L, lastPositionMs = 30_000L)

        assertEquals(null, com.example.ui.components.selectLastPlayedVideo(emptyList(), listOf(unplayed, privateVideo)))
        assertEquals(4L, com.example.ui.components.selectLastPlayedVideo(listOf(privateVideo, newestPlayed, olderPlayed), listOf(unplayed, olderPlayed, newestPlayed))?.id)
        assertEquals(4L, com.example.ui.components.selectLastPlayedVideo(emptyList(), listOf(unplayed, privateVideo, olderPlayed, newestPlayed))?.id)
    }

    @Test
    fun searchQuickFilter_filtersByResolutionFormatFavouriteAndStream() {
        val sampleVideos = listOf(
            VideoItem(id = 1L, uri = "file://1.mkv", title = "Dune 4K", folderName = "Movies", formatTag = "MKV", width = 3840, height = 2160, isFavourite = true),
            VideoItem(id = 2L, uri = "file://2.mp4", title = "Trip 1080p", folderName = "Camera", formatTag = "MP4", width = 1920, height = 1080, isFavourite = false),
            VideoItem(id = 3L, uri = "https://cdn.example.com/live.m3u8", title = "Live Stream", folderName = "Streams", formatTag = "HLS", width = 1280, height = 720, isDownloaded = false)
        )

        assertEquals(listOf(1L, 2L, 3L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.ALL).map { it.id })
        assertEquals(listOf(1L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.ULTRA_HD).map { it.id })
        assertEquals(listOf(2L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.FULL_HD).map { it.id })
        assertEquals(listOf(1L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.MKV).map { it.id })
        assertEquals(listOf(2L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.MP4).map { it.id })
        assertEquals(listOf(1L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.FAVOURITES).map { it.id })
        assertEquals(listOf(3L), com.example.ui.screens.applySearchQuickFilter(sampleVideos, com.example.ui.screens.SearchQuickFilter.STREAMS).map { it.id })
    }

    @Test
    fun playerVideoSurfaceDimensions_fitsAndCropsWithoutDistortionAcrossDeviceScreens() {
        // Portrait phone (412x915) playing 16:9 (1920x1080) video
        val portraitFit = com.example.ui.screens.calculateVideoSurfaceDimensions(
            containerWidthDp = 412f,
            containerHeightDp = 915f,
            naturalWidth = 1920,
            naturalHeight = 1080,
            aspectRatioMode = com.example.data.model.AspectRatioMode.FIT
        )
        assertEquals(412f, portraitFit.widthDp, 0.1f)
        assertEquals(231.75f, portraitFit.heightDp, 0.2f)
        assertTrue(portraitFit.heightDp <= 915f)

        // ZOOM on portrait phone must preserve 16:9 ratio (height fills 915dp, width expands & clips)
        val portraitZoom = com.example.ui.screens.calculateVideoSurfaceDimensions(
            containerWidthDp = 412f,
            containerHeightDp = 915f,
            naturalWidth = 1920,
            naturalHeight = 1080,
            aspectRatioMode = com.example.data.model.AspectRatioMode.ZOOM
        )
        assertEquals(915f, portraitZoom.heightDp, 0.1f)
        assertEquals(915f * (16f / 9f), portraitZoom.widthDp, 0.5f)
        assertTrue(portraitZoom.needsClipToBounds)

        // Landscape ultrawide phone (915x412) playing 16:9 video -> pillarboxed symmetrically
        val landscapeFit = com.example.ui.screens.calculateVideoSurfaceDimensions(
            containerWidthDp = 915f,
            containerHeightDp = 412f,
            naturalWidth = 1920,
            naturalHeight = 1080,
            aspectRatioMode = com.example.data.model.AspectRatioMode.FIT
        )
        assertEquals(412f, landscapeFit.heightDp, 0.1f)
        assertEquals(412f * (16f / 9f), landscapeFit.widthDp, 0.5f)
        assertTrue(landscapeFit.widthDp <= 915f)

        // FILL mode stretches cleanly to container
        val fill = com.example.ui.screens.calculateVideoSurfaceDimensions(
            containerWidthDp = 1024f,
            containerHeightDp = 768f,
            naturalWidth = 1920,
            naturalHeight = 1080,
            aspectRatioMode = com.example.data.model.AspectRatioMode.FILL
        )
        assertEquals(1024f, fill.widthDp, 0.1f)
        assertEquals(768f, fill.heightDp, 0.1f)
    }

    @Test
    fun playerAdaptiveMetrics_andTopBarPartitionAdaptPerDeviceScreen() {
        val compactPortrait = com.example.ui.screens.calculatePlayerAdaptiveMetrics(
            screenWidthDp = 360f,
            screenHeightDp = 780f,
            isControlsVisible = true,
            isControlsLocked = false,
            hasTopBanner = true,
            isTwoLineBanner = false,
            hasRunningTime = true,
            isBottomCenterBanner = false,
            hasAnyBottomButton = true
        )
        assertEquals(com.example.ui.screens.PlayerScreenClass.COMPACT_PORTRAIT, compactPortrait.screenClass)
        assertEquals(4, compactPortrait.maxTopRightSlots)
        assertEquals(null, compactPortrait.bottomBarMaxWidthDp)

        val tablet = com.example.ui.screens.calculatePlayerAdaptiveMetrics(
            screenWidthDp = 1024f,
            screenHeightDp = 768f,
            isControlsVisible = true,
            isControlsLocked = false,
            hasTopBanner = true,
            isTwoLineBanner = true,
            hasRunningTime = true,
            isBottomCenterBanner = true,
            hasAnyBottomButton = true
        )
        assertEquals(com.example.ui.screens.PlayerScreenClass.EXPANDED_TABLET, tablet.screenClass)
        assertEquals(6, tablet.maxTopRightSlots)
        assertEquals(820f, tablet.bottomBarMaxWidthDp ?: 0f, 0.1f)
        assertTrue(tablet.subtitleControlsExtraBottomDp > tablet.bottomBannerExtraDp)

        val allTopActions = com.example.ui.screens.PlayerTopAction.entries
        val compactPartition = com.example.ui.screens.partitionTopBarActions(allTopActions, compactPortrait.maxTopRightSlots)
        assertEquals(3, compactPartition.inlineActions.size)
        assertEquals(3, compactPartition.overflowActions.size)
        assertTrue(com.example.ui.screens.PlayerTopAction.SETTINGS in compactPartition.inlineActions)
        assertTrue(com.example.ui.screens.PlayerTopAction.LOCK in compactPartition.inlineActions)

        val tabletPartition = com.example.ui.screens.partitionTopBarActions(allTopActions, tablet.maxTopRightSlots)
        assertEquals(6, tabletPartition.inlineActions.size)
        assertTrue(tabletPartition.overflowActions.isEmpty())
    }

    @Test
    fun appColorSource_defaultsAndCustomThemePaletteGenerationWork() {
        val defaults = UserPreferences()
        assertEquals(com.example.data.model.AppColorSource.WALLPAPER, defaults.appColorSource)
        assertEquals(com.example.data.model.AppCustomColorPreset.ELECTRIC_VIOLET.seedHex, defaults.customThemeColorHex)

        // HSL round-trip check across presets & AMOLED black dark background verification
        com.example.data.model.AppCustomColorPreset.entries.forEach { preset ->
            val (h, s, l) = com.example.ui.theme.rgbHexToHsl(preset.seedHex)
            val roundTrip = com.example.ui.theme.hslToRgbHex(h, s, l)
            val (h2, _, _) = com.example.ui.theme.rgbHexToHsl(roundTrip)
            assertEquals(h, h2, 2.0f)

            val lightScheme = com.example.ui.theme.buildCustomAppColorScheme(preset.seedHex, isDark = false)
            val darkScheme = com.example.ui.theme.buildCustomAppColorScheme(preset.seedHex, isDark = true)
            assertTrue(lightScheme.background.red > 0.85f)
            assertEquals(com.example.ui.theme.FazaAmoledBlack, darkScheme.background)
            assertTrue(darkScheme.background.red < 0.06f && darkScheme.background.green < 0.06f && darkScheme.background.blue < 0.06f)
            assertTrue(lightScheme.primary != darkScheme.primary)
        }

        assertEquals("#2563EB", com.example.ui.theme.formatColorHexLabel(0xFF2563EB))
    }

    @Test
    fun profileAssistant_modesSystemInstructionsAndGroundingExtractionWork() {
        assertEquals("gemini-4-hydrogen", com.example.ai.AssistantChatMode.GENERAL_SEARCH.modelName)
        assertEquals("gemini-3.1-flash-lite", com.example.ai.AssistantChatMode.FAST.modelName)
        assertEquals("gemini-4-carbon", com.example.ai.AssistantChatMode.DEEP_REASONING.modelName)
        assertEquals("gemini-4-argon", com.example.ai.AssistantChatMode.COMPLEX_INTERACTIVE.modelName)

        assertTrue(com.example.ai.AssistantChatMode.GENERAL_SEARCH.supportsGoogleSearch)
        assertTrue(com.example.ai.AssistantChatMode.GENERAL_SEARCH.defaultGoogleSearchEnabled)

        val summary = com.example.ai.LibraryContextSummary(
            totalPublicVideos = 12,
            folderCount = 3,
            favouriteCount = 2,
            streamCount = 1,
            recentTitles = listOf("Interstellar 4K"),
            folderNames = listOf("Movies", "Camera")
        )
        val instruction = com.example.ai.buildAssistantSystemInstruction(
            mode = com.example.ai.AssistantChatMode.GENERAL_SEARCH,
            searchGroundingEnabled = true,
            librarySummary = summary
        )
        assertTrue(instruction.contains("FAZA Cinema Assistant"))
        assertTrue(instruction.contains("Google Search grounding"))
        assertTrue(instruction.contains("12 videos across 3 folders"))

        val metadata = com.google.firebase.ai.type.GroundingMetadata(
            webSearchQueries = listOf("latest 4K movies", " ", "latest 4K movies"),
            searchEntryPoint = null,
            retrievalQueries = emptyList(),
            groundingAttribution = emptyList(),
            groundingChunks = listOf(
                com.google.firebase.ai.type.GroundingChunk(
                    web = com.google.firebase.ai.type.WebGroundingChunk(
                        uri = "https://example.com/4k-releases",
                        title = "Top 4K Releases",
                        domain = "example.com"
                    )
                ),
                com.google.firebase.ai.type.GroundingChunk(
                    web = com.google.firebase.ai.type.WebGroundingChunk(
                        uri = "https://example.com/4k-releases",
                        title = "Duplicate",
                        domain = "example.com"
                    )
                )
            ),
            groundingSupports = emptyList()
        )
        val (queries, sources) = com.example.ai.extractGroundingDetails(metadata)
        assertEquals(listOf("latest 4K movies"), queries)
        assertEquals(1, sources.size)
        assertEquals("Top 4K Releases", sources.first().title)
        assertEquals("example.com", sources.first().domain)
    }

    @Test
    fun videoCodecSelector_filtersOnlyVideoStreamsAndRespectsHardwareAccelerationAndFallback() {
        data class FakeCodec(
            val name: String,
            val softwareOnly: Boolean,
            val hardwareAccelerated: Boolean
        )

        val hwCodec = FakeCodec("c2.qti.avc.decoder", softwareOnly = false, hardwareAccelerated = true)
        val swCodec = FakeCodec("c2.android.avc.decoder", softwareOnly = true, hardwareAccelerated = false)
        val audioSwCodec = FakeCodec("c2.android.aac.decoder", softwareOnly = true, hardwareAccelerated = false)
        val mixedVideoList = listOf(swCodec, hwCodec)
        val isSw: (FakeCodec) -> Boolean = {
            com.example.player.isSoftwareCodecDescriptor(
                name = it.name,
                softwareOnly = it.softwareOnly,
                hardwareAccelerated = it.hardwareAccelerated,
                sdkInt = 30
            )
        }

        // AUTO prioritizes HW ahead of SW for smoother older-device playback
        val autoVideo = com.example.player.selectCodecInfosForMode(
            mimeType = "video/avc",
            decoders = mixedVideoList,
            isSoftwareDecoder = isSw,
            decoderMode = DecoderMode.AUTO,
            decoderFallbackEnabled = true
        )
        assertEquals(listOf(hwCodec, swCodec), autoVideo)

        // HARDWARE filters to hardware decoders, falling back if none exist and fallback is enabled
        val strictHwVideo = com.example.player.selectCodecInfosForMode(
            mimeType = "video/avc",
            decoders = mixedVideoList,
            isSoftwareDecoder = isSw,
            decoderMode = DecoderMode.HARDWARE,
            decoderFallbackEnabled = false
        )
        assertEquals(listOf(hwCodec), strictHwVideo)

        val hwFallbackWhenMissing = com.example.player.selectCodecInfosForMode(
            mimeType = "video/hevc",
            decoders = listOf(swCodec),
            isSoftwareDecoder = isSw,
            decoderMode = DecoderMode.HARDWARE,
            decoderFallbackEnabled = true
        )
        assertEquals(listOf(swCodec), hwFallbackWhenMissing)

        // SOFTWARE forces CPU decoders to help older devices with broken/stuttering HW codecs
        val swVideo = com.example.player.selectCodecInfosForMode(
            mimeType = "video/avc",
            decoders = mixedVideoList,
            isSoftwareDecoder = isSw,
            decoderMode = DecoderMode.SOFTWARE,
            decoderFallbackEnabled = false
        )
        assertEquals(listOf(swCodec), swVideo)

        // Audio decoders must never be stripped even in strict HARDWARE video mode
        val audioResult = com.example.player.selectCodecInfosForMode(
            mimeType = "audio/mp4a-latm",
            decoders = listOf(audioSwCodec),
            isSoftwareDecoder = isSw,
            decoderMode = DecoderMode.HARDWARE,
            decoderFallbackEnabled = false
        )
        assertEquals(listOf(audioSwCodec), audioResult)

        assertTrue(com.example.player.isSoftwareCodecDescriptor("OMX.google.h264.decoder", false, true, sdkInt = 26))
        assertTrue(com.example.player.isSoftwareCodecDescriptor("omx.ffmpeg.h264.decoder", false, true, sdkInt = 26))
        assertFalse(com.example.player.isSoftwareCodecDescriptor("OMX.qcom.video.decoder.avc", false, true, sdkInt = 26))
    }

    @Test
    fun autoResumeOnRestart_selectsEligibleUnfinishedVideoAndComputesStartPosition() {
        assertTrue(UserPreferences().autoResumeLastVideoOnRestart)

        val unplayed = VideoItem(id = 1L, uri = "file://unplayed.mp4", title = "Unplayed", folderName = "Camera", durationMs = 60_000L, lastPositionMs = 0L)
        val finished = VideoItem(id = 2L, uri = "file://done.mp4", title = "Finished", folderName = "Movies", durationMs = 60_000L, lastPlayedAt = 999L, lastPositionMs = 58_500L)
        val privateClip = VideoItem(id = 3L, uri = "file://vault.mp4", title = "Secret", folderName = "Vault", isPrivate = true, durationMs = 90_000L, lastPlayedAt = 1200L, lastPositionMs = 20_000L)
        val activeResume = VideoItem(id = 4L, uri = "file://resume.mkv", title = "Inception", folderName = "Movies", durationMs = 180_000L, lastPlayedAt = 800L, lastPositionMs = 42_500L)

        // Most recent non-private finished video is rejected by auto-resume
        assertEquals(null, com.example.ui.components.selectAutoResumeVideo(listOf(privateClip, finished, unplayed), listOf(unplayed, finished, privateClip)))

        // Mid-playback non-private video is selected for automatic startup resume
        val picked = com.example.ui.components.selectAutoResumeVideo(
            recentVideos = listOf(privateClip, activeResume),
            allPublicVideos = listOf(unplayed, activeResume)
        )
        assertEquals(4L, picked?.id)

        // Start position helper preserves saved position mid-video or for unknown-duration streams, resets near end or <1s
        assertEquals(0L, com.example.player.calculateResumeStartPositionMs(resumePositionMs = 500L, durationMs = 60_000L))
        assertEquals(0L, com.example.player.calculateResumeStartPositionMs(resumePositionMs = 58_000L, durationMs = 60_000L))
        assertEquals(42_500L, com.example.player.calculateResumeStartPositionMs(resumePositionMs = 42_500L, durationMs = 180_000L))
        assertEquals(15_000L, com.example.player.calculateResumeStartPositionMs(resumePositionMs = 15_000L, durationMs = 0L))
    }

    @Test
    fun fzUiTheme_defaultsToolbarItemsAndFormattersWorkCorrectly() {
        val defaults = UserPreferences().playerUiConfig
        assertEquals(com.example.data.model.PlayerUiThemeStyle.FAZA_CLASSIC, defaults.themeStyle)
        assertTrue(defaults.fzHiddenToolbarItems.isEmpty())

        val fzConfig = defaults.copy(themeStyle = com.example.data.model.PlayerUiThemeStyle.FZ_UI)
        assertEquals("FZ Ui", fzConfig.themeStyle.label)

        val expectedLabels = listOf(
            "Night Mode",
            "Customise Items",
            "Shuffle",
            "Loop",
            "Mute",
            "Sleep Timer",
            "A-B Repeat",
            "Audio Effect",
            "Equalizer",
            "Speed",
            "Screenshot",
            "Background Play",
            "Screen Rotation"
        )
        assertEquals(
            expectedLabels,
            com.example.data.model.FzQuickToolbarItem.entries.map { it.label }
        )

        // Customise Items stays pinned even if all other items are hidden
        val filtered = com.example.ui.components.resolveVisibleFzToolbarItems(
            setOf("SHUFFLE", "MUTE", "CUSTOMISE_ITEMS")
        )
        assertFalse(filtered.any { it == com.example.data.model.FzQuickToolbarItem.SHUFFLE })
        assertFalse(filtered.any { it == com.example.data.model.FzQuickToolbarItem.MUTE })
        assertTrue(filtered.any { it == com.example.data.model.FzQuickToolbarItem.CUSTOMISE_ITEMS })

        assertEquals("-02:00", com.example.ui.components.formatFzTimelineRemaining(720_000L, 840_000L))
        assertEquals("-00:00", com.example.ui.components.formatFzTimelineRemaining(900_000L, 840_000L))
        assertEquals("LIVE", com.example.ui.components.formatFzTimelineRemaining(15_000L, 0L))

        assertEquals("1X", com.example.ui.components.formatFzSpeedLabel(1.0f))
        assertEquals("1.5X", com.example.ui.components.formatFzSpeedLabel(1.5f))
        assertEquals("0.75X", com.example.ui.components.formatFzSpeedLabel(0.75f))
        assertEquals("2X", com.example.ui.components.formatFzSpeedLabel(2.0f))
    }

    @Test
    fun assistantChatHistoryStore_persistsMergesAndClearsUnifiedHistory() {
        val tempDir = java.nio.file.Files.createTempDirectory("faza_chat_history_test").toFile()
        try {
            val store = com.example.ai.AssistantChatHistoryStore(java.io.File(tempDir, "history.json"))
            kotlinx.coroutines.runBlocking {
                assertTrue(store.loadMessages().isEmpty())

                val m1 = com.example.ai.AssistantChatMessage(
                    id = "m1",
                    role = com.example.ai.ChatRole.USER,
                    text = "Suggest 4K HDR movies",
                    timestampMs = 1_000L
                )
                val m2 = com.example.ai.AssistantChatMessage(
                    id = "m2",
                    role = com.example.ai.ChatRole.ASSISTANT,
                    text = "Try Blade Runner 2049 and Dune.",
                    modeLabel = "Search AI",
                    usedGoogleSearch = true,
                    sources = listOf(
                        com.example.ai.GroundingSource("IMDb", "https://www.imdb.com", "imdb.com")
                    ),
                    timestampMs = 2_000L
                )
                val m3 = com.example.ai.AssistantChatMessage(
                    id = "m3",
                    role = com.example.ai.ChatRole.ASSISTANT,
                    text = "AV1 saves bandwidth over HEVC.",
                    modeLabel = "Deep",
                    isStreaming = true,
                    timestampMs = 3_000L
                )
                val m4 = com.example.ai.AssistantChatMessage(
                    id = "m4",
                    role = com.example.ai.ChatRole.USER,
                    text = "Compare AV1 and HEVC",
                    timestampMs = 2_500L
                )

                // Streaming message is excluded while in-flight, remaining are sorted & deduplicated
                store.saveMessages(listOf(m4, m1, m2, m1, m3))
                val loadedFirst = store.loadMessages()
                assertEquals(listOf("m1", "m2", "m4"), loadedFirst.map { it.id })
                assertEquals(1, loadedFirst[1].sources.size)

                // Completed m3 merges into the same chronological history
                store.saveMessages(loadedFirst + m3.copy(isStreaming = false))
                val loadedSecond = store.loadMessages()
                assertEquals(listOf("m1", "m2", "m4", "m3"), loadedSecond.map { it.id })
                assertFalse(loadedSecond.last().isStreaming)

                store.clear()
                assertTrue(store.loadMessages().isEmpty())
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun fazaAdsManager_resolvesConfiguredProductionAdMobIds() {
        assertEquals(
            "ca-app-pub-6572024129453689~5808434174",
            com.example.ads.FazaAdsManager.resolveAppId()
        )
        assertEquals(
            "ca-app-pub-6572024129453689/1854846916",
            com.example.ads.FazaAdsManager.resolveBannerAdUnitId()
        )
        assertFalse(com.example.ads.FazaAdsManager.isUsingTestAdIds())
    }

    @Test
    fun fazaAdsManager_requiresValidatedInternetOnDevicesAndHidesOffline() {
        assertTrue(
            com.example.ads.FazaAdsManager.hasValidatedInternetCapability(
                hasInternet = true,
                hasValidated = true,
                isRobolectric = false
            )
        )
        // Offline -> never show ads
        assertFalse(
            com.example.ads.FazaAdsManager.hasValidatedInternetCapability(
                hasInternet = false,
                hasValidated = false,
                isRobolectric = false
            )
        )
        // Captive portal / unvalidated connection on real Android device -> hide ads
        assertFalse(
            com.example.ads.FazaAdsManager.hasValidatedInternetCapability(
                hasInternet = true,
                hasValidated = false,
                isRobolectric = false
            )
        )
    }

    @Test
    fun adaptiveLayoutSpec_adjustsBreakpointsColumnsAndPaneModePerDevice() {
        // Ultra-compact portrait phone (320 x 568 dp)
        val ultraCompact = com.example.ui.adaptive.calculateAdaptiveLayoutSpec(
            availableWidth = androidx.compose.ui.unit.Dp(320f),
            availableHeight = androidx.compose.ui.unit.Dp(568f)
        )
        assertEquals(com.example.ui.adaptive.DeviceWidthClass.COMPACT, ultraCompact.widthClass)
        assertTrue(ultraCompact.isUltraCompactWidth)
        assertFalse(ultraCompact.isShortHeight)
        assertFalse(ultraCompact.useNavigationRail)
        assertFalse(ultraCompact.useTwoPane)
        assertEquals(1, ultraCompact.videoGridColumns)
        assertEquals(12f, ultraCompact.horizontalPadding.value, 0.01f)

        // Standard phone portrait (412 x 915 dp)
        val phonePortrait = com.example.ui.adaptive.calculateAdaptiveLayoutSpec(
            availableWidth = androidx.compose.ui.unit.Dp(412f),
            availableHeight = androidx.compose.ui.unit.Dp(915f)
        )
        assertEquals(com.example.ui.adaptive.DeviceWidthClass.COMPACT, phonePortrait.widthClass)
        assertEquals(com.example.ui.adaptive.DeviceHeightClass.EXPANDED, phonePortrait.heightClass)
        assertFalse(phonePortrait.isUltraCompactWidth)
        assertFalse(phonePortrait.useNavigationRail)
        assertFalse(phonePortrait.useTwoPane)
        assertEquals(1, phonePortrait.videoGridColumns)
        assertEquals(16f, phonePortrait.horizontalPadding.value, 0.01f)

        // Short landscape phone (667 x 360 dp) -> switches to nav rail & 2 columns, but avoids cramped two-pane
        val landscapePhone = com.example.ui.adaptive.calculateAdaptiveLayoutSpec(
            availableWidth = androidx.compose.ui.unit.Dp(667f),
            availableHeight = androidx.compose.ui.unit.Dp(360f)
        )
        assertEquals(com.example.ui.adaptive.DeviceWidthClass.MEDIUM, landscapePhone.widthClass)
        assertEquals(com.example.ui.adaptive.DeviceHeightClass.COMPACT, landscapePhone.heightClass)
        assertTrue(landscapePhone.isShortHeight)
        assertTrue(landscapePhone.useNavigationRail)
        assertFalse(landscapePhone.useTwoPane)
        assertEquals(2, landscapePhone.videoGridColumns)
        assertEquals(8f, landscapePhone.verticalPadding.value, 0.01f)

        // Unfolded foldable / medium tablet (760 x 820 dp) -> nav rail, two-pane hero/settings, 2 columns
        val mediumTablet = com.example.ui.adaptive.calculateAdaptiveLayoutSpec(
            availableWidth = androidx.compose.ui.unit.Dp(760f),
            availableHeight = androidx.compose.ui.unit.Dp(820f)
        )
        assertEquals(com.example.ui.adaptive.DeviceWidthClass.MEDIUM, mediumTablet.widthClass)
        assertTrue(mediumTablet.useNavigationRail)
        assertTrue(mediumTablet.useTwoPane)
        assertEquals(2, mediumTablet.videoGridColumns)
        assertEquals(24f, mediumTablet.horizontalPadding.value, 0.01f)

        // Large expanded tablet / desktop window (1100 x 800 dp) -> 3 columns & two-pane enabled
        val expandedTablet = com.example.ui.adaptive.calculateAdaptiveLayoutSpec(
            availableWidth = androidx.compose.ui.unit.Dp(1100f),
            availableHeight = androidx.compose.ui.unit.Dp(800f)
        )
        assertEquals(com.example.ui.adaptive.DeviceWidthClass.EXPANDED, expandedTablet.widthClass)
        assertTrue(expandedTablet.useNavigationRail)
        assertTrue(expandedTablet.useTwoPane)
        assertEquals(3, expandedTablet.videoGridColumns)
        assertEquals(32f, expandedTablet.horizontalPadding.value, 0.01f)
        assertEquals(1200f, expandedTablet.maxContentWidth.value, 0.01f)
        assertEquals(400f, expandedTablet.twoPaneMasterWidth.value, 0.01f)
    }

    @Test
    fun midVideoBreak_triggersOnceAtMidpointOnlyForLongVideosAndWhenEnabled() {
        val oneHourMs = 60L * 60L * 1000L // 01:00:00
        val thirtyMinMs = 30L * 60L * 1000L // 00:30:00
        val tenMinMs = 10L * 60L * 1000L // 00:10:00 (smaller than 20 min cutoff)

        // Default preference is enabled (optional to disable or skip)
        assertTrue(UserPreferences().midVideoBreakEnabled)
        assertFalse(UserPreferences().copy(midVideoBreakEnabled = false).midVideoBreakEnabled)

        // 01:00:00 -> triggers at 00:30:00
        assertEquals(thirtyMinMs, com.example.ui.viewmodel.calculateMidVideoBreakTriggerMs(oneHourMs))
        // Smaller videos (< 20 min) or unknown duration return null
        assertEquals(null, com.example.ui.viewmodel.calculateMidVideoBreakTriggerMs(tenMinMs))
        assertEquals(null, com.example.ui.viewmodel.calculateMidVideoBreakTriggerMs(0L))

        // Crossing 00:30:00 during active 1-hour playback triggers once
        assertTrue(
            com.example.ui.viewmodel.shouldTriggerMidVideoBreak(
                enabled = true,
                hasShownThisSession = false,
                isPlaying = true,
                isInPipMode = false,
                previousPositionMs = thirtyMinMs - 400L,
                positionMs = thirtyMinMs + 100L,
                durationMs = oneHourMs
            )
        )

        // Never triggers a second time in the same playback session
        assertFalse(
            com.example.ui.viewmodel.shouldTriggerMidVideoBreak(
                enabled = true,
                hasShownThisSession = true,
                isPlaying = true,
                isInPipMode = false,
                previousPositionMs = thirtyMinMs - 400L,
                positionMs = thirtyMinMs + 100L,
                durationMs = oneHourMs
            )
        )

        // Disabled setting skips break
        assertFalse(
            com.example.ui.viewmodel.shouldTriggerMidVideoBreak(
                enabled = false,
                hasShownThisSession = false,
                isPlaying = true,
                isInPipMode = false,
                previousPositionMs = thirtyMinMs - 400L,
                positionMs = thirtyMinMs + 100L,
                durationMs = oneHourMs
            )
        )

        // Smaller video (10 min crossing 5 min mark) never triggers
        assertFalse(
            com.example.ui.viewmodel.shouldTriggerMidVideoBreak(
                enabled = true,
                hasShownThisSession = false,
                isPlaying = true,
                isInPipMode = false,
                previousPositionMs = (tenMinMs / 2L) - 300L,
                positionMs = (tenMinMs / 2L) + 200L,
                durationMs = tenMinMs
            )
        )

        // PiP mode or initial resume past midpoint does not ambush viewer
        assertFalse(
            com.example.ui.viewmodel.shouldTriggerMidVideoBreak(
                enabled = true,
                hasShownThisSession = false,
                isPlaying = true,
                isInPipMode = true,
                previousPositionMs = thirtyMinMs - 400L,
                positionMs = thirtyMinMs + 100L,
                durationMs = oneHourMs
            )
        )
        assertFalse(
            com.example.ui.viewmodel.shouldTriggerMidVideoBreak(
                enabled = true,
                hasShownThisSession = false,
                isPlaying = true,
                isInPipMode = false,
                previousPositionMs = -1L,
                positionMs = thirtyMinMs + 500L,
                durationMs = oneHourMs
            )
        )
    }

    @Test
    fun continuePlaying_automaticallyRemovesEndedAndFinishedVideos() {
        val olderUnfinished = VideoItem(
            id = 10L,
            uri = "file://older.mp4",
            title = "Older Movie",
            folderName = "Movies",
            durationMs = 120_000L,
            lastPlayedAt = 500L,
            lastPositionMs = 40_000L
        )
        val inProgress = VideoItem(
            id = 11L,
            uri = "file://current.mp4",
            title = "Current Movie",
            folderName = "Movies",
            durationMs = 180_000L,
            lastPlayedAt = 1_000L,
            lastPositionMs = 45_000L
        )
        val finishedAtEnd = inProgress.copy(lastPositionMs = 178_000L)
        val resetAfterEnded = inProgress.copy(lastPositionMs = 0L)

        // Mid-playback video appears in Home Screen Continue Playing tab
        assertEquals(
            11L,
            com.example.ui.components.selectContinuePlayingVideo(
                recentVideos = listOf(inProgress, olderUnfinished),
                allPublicVideos = listOf(olderUnfinished, inProgress)
            )?.id
        )

        // When playback finishes via STATE_ENDED, Continue Playing tab is removed immediately
        assertEquals(
            null,
            com.example.ui.components.selectContinuePlayingVideo(
                recentVideos = listOf(inProgress, olderUnfinished),
                allPublicVideos = listOf(olderUnfinished, inProgress),
                activePlayingVideoId = null,
                endedVideoId = 11L
            )
        )

        // When progress reaches the end threshold or resets to 0 after finishing, tab stays removed
        // and does NOT resurface an older unfinished video
        assertEquals(
            null,
            com.example.ui.components.selectContinuePlayingVideo(
                recentVideos = listOf(finishedAtEnd, olderUnfinished),
                allPublicVideos = listOf(olderUnfinished, finishedAtEnd)
            )
        )
        assertEquals(
            null,
            com.example.ui.components.selectContinuePlayingVideo(
                recentVideos = listOf(resetAfterEnded, olderUnfinished),
                allPublicVideos = listOf(olderUnfinished, resetAfterEnded)
            )
        )

        // Active playback starting at 00:00 still shows Continue Playing tab while playing
        assertEquals(
            11L,
            com.example.ui.components.selectContinuePlayingVideo(
                recentVideos = listOf(resetAfterEnded, olderUnfinished),
                allPublicVideos = listOf(olderUnfinished, resetAfterEnded),
                activePlayingVideoId = 11L,
                endedVideoId = null
            )?.id
        )

        // Playback completion threshold helper matches end-of-video behavior
        assertTrue(com.example.ui.viewmodel.isPlaybackCompletedPosition(178_000L, 180_000L))
        assertFalse(com.example.ui.viewmodel.isPlaybackCompletedPosition(45_000L, 180_000L))
    }

    @Test
    fun permanentNowPlaying_upNextShowsInLast5MinutesOnlyWhenFolderHasNextVideo() {
        val defaults = NowPlayingConfig()
        assertTrue(defaults.showUpNext)
        assertEquals(5, defaults.upNextLeadMinutes)

        // Exact user example: 45:00 video -> shows when 05:00 remain (at 40:00 elapsed)
        val duration45Min = 45L * 60_000L
        assertEquals(5L * 60_000L, com.example.ui.viewmodel.calculateUpNextLeadWindowMs(duration45Min, 5))
        assertFalse(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = true,
                hasNextVideoInFolder = true,
                positionMs = 39L * 60_000L + 59_000L,
                durationMs = duration45Min,
                leadMinutes = 5
            )
        )
        assertTrue(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = true,
                hasNextVideoInFolder = true,
                positionMs = 40L * 60_000L,
                durationMs = duration45Min,
                leadMinutes = 5
            )
        )
        assertTrue(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = true,
                hasNextVideoInFolder = true,
                positionMs = 44L * 60_000L + 50_000L,
                durationMs = duration45Min,
                leadMinutes = 5
            )
        )
        // Hidden when no next video exists in the specific folder or feature is disabled
        assertFalse(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = true,
                hasNextVideoInFolder = false,
                positionMs = 42L * 60_000L,
                durationMs = duration45Min,
                leadMinutes = 5
            )
        )
        assertFalse(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = false,
                hasNextVideoInFolder = true,
                positionMs = 42L * 60_000L,
                durationMs = duration45Min,
                leadMinutes = 5
            )
        )

        // Short 4:00 video scales lead window to final 25% (01:00) instead of showing immediately
        val duration4Min = 4L * 60_000L
        assertEquals(60_000L, com.example.ui.viewmodel.calculateUpNextLeadWindowMs(duration4Min, 5))
        assertFalse(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = true,
                hasNextVideoInFolder = true,
                positionMs = 2L * 60_000L + 59_000L,
                durationMs = duration4Min,
                leadMinutes = 5
            )
        )
        assertTrue(
            com.example.ui.viewmodel.shouldShowUpNextInNowPlaying(
                enabled = true,
                hasNextVideoInFolder = true,
                positionMs = 3L * 60_000L,
                durationMs = duration4Min,
                leadMinutes = 5
            )
        )

        // Folder-scoped queue and next video resolution
        val ep1 = VideoItem(id = 1L, uri = "file://series/ep1.mkv", title = "Episode 01", folderName = "Series", dateAdded = 300L)
        val ep2 = VideoItem(id = 2L, uri = "file://series/ep2.mkv", title = "Episode 02", folderName = "Series", dateAdded = 200L)
        val otherFolder = VideoItem(id = 3L, uri = "file://movies/m1.mkv", title = "Other Folder Movie", folderName = "Movies", dateAdded = 100L)
        val privateInSameName = VideoItem(id = 4L, uri = "file://vault/ep3.mkv", title = "Private Clip", folderName = "Series", isPrivate = true, dateAdded = 50L)

        val seriesQueue = com.example.ui.viewmodel.buildFolderPlaybackQueue(
            allVideos = listOf(ep1, ep2, otherFolder, privateInSameName),
            currentVideo = ep1,
            sortOption = com.example.data.model.SortOption.NAME_ASC
        )
        assertEquals(listOf(1L, 2L), seriesQueue.map { it.id })
        assertEquals(2L, com.example.ui.viewmodel.resolveNextVideoInFolder(seriesQueue, ep1)?.id)
        assertEquals(null, com.example.ui.viewmodel.resolveNextVideoInFolder(seriesQueue, ep2))
        assertEquals(
            1L,
            com.example.ui.viewmodel.resolveNextVideoInFolder(
                queue = seriesQueue,
                currentVideo = ep2,
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            )?.id
        )

        // Single-video folder never shows a video from another folder
        val moviesQueue = com.example.ui.viewmodel.buildFolderPlaybackQueue(
            allVideos = listOf(ep1, ep2, otherFolder),
            currentVideo = otherFolder
        )
        assertEquals(listOf(3L), moviesQueue.map { it.id })
        assertEquals(null, com.example.ui.viewmodel.resolveNextVideoInFolder(moviesQueue, otherFolder))

        // Badge formatting matches 05:00 countdown
        assertEquals("Next in 05:00", com.example.ui.components.formatUpNextBadge(5L * 60_000L))
        assertEquals("Up Next", com.example.ui.components.formatUpNextBadge(null))
    }

    @Test
    fun ac3AudioSupport_routesFfmpegDecodersAndFormatsTrackLabels() {
        // AC-3 / E-AC-3 / TrueHD / DTS MIME types are recognized for FFmpeg decoding
        assertTrue(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_AC3))
        assertTrue(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_E_AC3))
        assertTrue(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_E_AC3_JOC))
        assertTrue(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_TRUEHD))
        assertTrue(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_DTS))
        assertTrue(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_DTS_HD))
        assertFalse(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.AUDIO_AAC))
        assertFalse(com.example.player.isFfmpegAudioMime(androidx.media3.common.MimeTypes.VIDEO_H264))

        val platformAc3Decoders = listOf("OMX.vendor.ac3.decoder")

        // AUTO & SOFTWARE modes route AC-3 to FFmpeg (empty platform list) when FFmpeg is available
        assertTrue(
            com.example.player.selectCodecInfosForMode(
                mimeType = androidx.media3.common.MimeTypes.AUDIO_AC3,
                decoders = platformAc3Decoders,
                isSoftwareDecoder = { false },
                decoderMode = DecoderMode.AUTO,
                decoderFallbackEnabled = true,
                ffmpegAudioAvailable = { true }
            ).isEmpty()
        )
        assertTrue(
            com.example.player.selectCodecInfosForMode(
                mimeType = androidx.media3.common.MimeTypes.AUDIO_E_AC3,
                decoders = platformAc3Decoders,
                isSoftwareDecoder = { false },
                decoderMode = DecoderMode.SOFTWARE,
                decoderFallbackEnabled = true,
                ffmpegAudioAvailable = { true }
            ).isEmpty()
        )

        // HARDWARE mode tries platform AC-3 decoders first (and falls back to FFmpeg if absent)
        assertEquals(
            platformAc3Decoders,
            com.example.player.selectCodecInfosForMode(
                mimeType = androidx.media3.common.MimeTypes.AUDIO_AC3,
                decoders = platformAc3Decoders,
                isSoftwareDecoder = { false },
                decoderMode = DecoderMode.HARDWARE,
                decoderFallbackEnabled = true,
                ffmpegAudioAvailable = { true }
            )
        )

        // If FFmpeg isn't available on the current runtime (e.g. JVM), platform decoders remain intact
        assertEquals(
            platformAc3Decoders,
            com.example.player.selectCodecInfosForMode(
                mimeType = androidx.media3.common.MimeTypes.AUDIO_AC3,
                decoders = platformAc3Decoders,
                isSoftwareDecoder = { false },
                decoderMode = DecoderMode.AUTO,
                decoderFallbackEnabled = true,
                ffmpegAudioAvailable = { false }
            )
        )

        // Clean human-readable AC-3 / surround track labels
        assertEquals("AC3", com.example.player.formatAudioCodecLabel(androidx.media3.common.MimeTypes.AUDIO_AC3))
        assertEquals("E-AC3", com.example.player.formatAudioCodecLabel(androidx.media3.common.MimeTypes.AUDIO_E_AC3))
        assertEquals("E-AC3 Atmos", com.example.player.formatAudioCodecLabel(androidx.media3.common.MimeTypes.AUDIO_E_AC3_JOC))
        assertEquals("TrueHD", com.example.player.formatAudioCodecLabel(androidx.media3.common.MimeTypes.AUDIO_TRUEHD))
        assertEquals("DTS", com.example.player.formatAudioCodecLabel(androidx.media3.common.MimeTypes.AUDIO_DTS))
        assertEquals("AAC", com.example.player.formatAudioCodecLabel(androidx.media3.common.MimeTypes.AUDIO_AAC))
        assertEquals("AUDIO", com.example.player.formatAudioCodecLabel(null))

        assertEquals("", com.example.player.formatAudioChannelLayout(0))
        assertEquals("Mono", com.example.player.formatAudioChannelLayout(1))
        assertEquals("Stereo", com.example.player.formatAudioChannelLayout(2))
        assertEquals("5.1", com.example.player.formatAudioChannelLayout(6))
        assertEquals("7.1", com.example.player.formatAudioChannelLayout(8))
        assertEquals("4ch", com.example.player.formatAudioChannelLayout(4))
    }
}

