package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VideoItem
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartupAutoResumeTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun appRestart_automaticallyResumesLastPlayedVideoFromSavedPosition() {
        val app = ApplicationProvider.getApplicationContext<FazaApplication>()
        runBlocking {
            app.database.videoDao().insertVideo(
                VideoItem(
                    uri = "https://example.com/sample-4k.mp4",
                    title = "Sample 4K Movie",
                    folderName = "Movies",
                    durationMs = 180_000L,
                    lastPlayedAt = System.currentTimeMillis(),
                    lastPositionMs = 45_000L
                )
            )
            app.preferencesRepository.setAutoResumeLastVideoOnRestart(true)
            app.preferencesRepository.setBackgroundPlaybackEnabled(false)
            app.preferencesRepository.setHasSeenGestureHelp(true)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 10_000L) {
                composeRule.onAllNodesWithTag("player_back_button").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("player_back_button").assertIsDisplayed()

            // Backing out returns cleanly to Home and must not re-trigger startup auto-resume
            composeRule.onNodeWithTag("player_back_button").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                composeRule.onAllNodes(hasTestTag("floating_navigation_dock")).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("floating_navigation_dock").assertIsDisplayed()
        }
    }
}
