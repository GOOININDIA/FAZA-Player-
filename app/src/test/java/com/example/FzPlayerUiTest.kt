package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.ui.components.FzPlayerUiOverlay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w890dp-h412dp-land")
class FzPlayerUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fzUiOverlay_rendersAllFiveSectionsAnd13ToolbarItemsInLandscape() {
        var lockClicked = false
        var rewindClicked = false
        var forwardClicked = false
        var playPauseClicked = false
        var audioEffectClicked = false

        composeRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 890.dp, height = 412.dp)
                        .background(Color.Black)
                ) {
                    FzPlayerUiOverlay(
                        videoTitle = "Dune_Part_Two_4K_HDR.mkv",
                        positionMs = 720_000L,
                        durationMs = 840_000L,
                        isPlaying = true,
                        isScrubbing = false,
                        scrubPositionMs = 0L,
                        activeDecoder = DecoderMode.HARDWARE,
                        aspectRatioMode = AspectRatioMode.FIT,
                        subtitlesEnabled = true,
                        nightModeEnabled = false,
                        shuffleEnabled = false,
                        repeatMode = Player.REPEAT_MODE_OFF,
                        isMuted = false,
                        sleepTimerRemainingSec = null,
                        sleepAtEndOfVideo = false,
                        abPointAMs = null,
                        abPointBMs = null,
                        audioEffectEnabled = true,
                        equalizerEnabled = false,
                        playbackSpeed = 1.0f,
                        backgroundPlayEnabled = true,
                        isLandscapeLocked = false,
                        hiddenToolbarItems = emptySet(),
                        onBack = {},
                        onOpenAudioTracks = {},
                        onToggleSubtitles = {},
                        onCycleDecoder = {},
                        onOpenMoreSettings = {},
                        onOpenCustomizeUi = {},
                        onSwitchToClassicUi = {},
                        onOpenGestureHelp = {},
                        onToggleNightMode = {},
                        onOpenCustomiseItems = {},
                        onToggleShuffle = {},
                        onCycleLoop = {},
                        onToggleMute = {},
                        onOpenSleepTimer = {},
                        onCycleAbRepeat = {},
                        onToggleAudioEffect = { audioEffectClicked = true },
                        onOpenEqualizer = {},
                        onOpenSpeedPicker = {},
                        onTakeScreenshot = {},
                        onToggleBackgroundPlay = {},
                        onRotateScreen = {},
                        onScrubChange = {},
                        onScrubFinish = {},
                        onLockScreen = { lockClicked = true },
                        onJumpBackward10s = { rewindClicked = true },
                        onPreviousTrack = {},
                        onTogglePlayPause = { playPauseClicked = true },
                        onNextTrack = {},
                        onJumpForward10s = { forwardClicked = true },
                        onCycleAspectRatio = {},
                        onExpandOrPopup = {},
                        onUserInteraction = {}
                    )
                }
            }
        }

        // 1. Top Action Bar
        composeRule.onNodeWithTag("fz_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_video_title").assertIsDisplayed().assertTextEquals("Dune_Part_Two_4K_HDR.mkv")
        composeRule.onNodeWithTag("fz_battery_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_clock_text").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_audio_track_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_subtitle_toggle_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_decoder_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_more_button").assertIsDisplayed()

        // 2. Scrollable Quick-Access Toolbar (13 items + Audio Effect active dot)
        composeRule.onNodeWithTag("fz_quick_toolbar").assertIsDisplayed()
        val toolTags = listOf(
            "fz_tool_night_mode",
            "fz_tool_customise_items",
            "fz_tool_shuffle",
            "fz_tool_loop",
            "fz_tool_mute",
            "fz_tool_sleep_timer",
            "fz_tool_ab_repeat",
            "fz_tool_audio_effect",
            "fz_tool_equalizer",
            "fz_tool_speed",
            "fz_tool_screenshot",
            "fz_tool_background_play",
            "fz_tool_screen_rotation"
        )
        toolTags.forEach { tag ->
            composeRule.onNodeWithTag("fz_quick_toolbar").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
            if (tag == "fz_tool_audio_effect") {
                composeRule.onNodeWithTag("fz_audio_effect_active_dot", useUnmergedTree = true).assertIsDisplayed()
                composeRule.onNodeWithTag(tag).performClick()
            }
        }
        assertTrue(audioEffectClicked)

        // 4. Timeline & Scrubber Bar
        composeRule.onNodeWithTag("fz_timeline_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_elapsed_time").assertIsDisplayed().assertTextEquals("12:00")
        composeRule.onNodeWithTag("fz_seek_slider").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_remaining_time").assertIsDisplayed().assertTextEquals("-02:00")

        // 5. Bottom Control Bar (8 controls)
        composeRule.onNodeWithTag("fz_bottom_controls").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_lock_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("fz_rewind_10_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("fz_prev_track_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_play_pause_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("fz_next_track_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_forward_10_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("fz_aspect_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fz_expand_popup_button").assertIsDisplayed()

        assertTrue(lockClicked)
        assertTrue(rewindClicked)
        assertTrue(playPauseClicked)
        assertTrue(forwardClicked)
    }
}
