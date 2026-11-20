package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.ThemeMode
import com.example.ui.components.AllFileAccessPermissionScreen
import com.example.ui.theme.FazaPlayerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AllFileAccessScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allFileAccessScreen_displaysRequiredElementsAndTriggersActions() {
        var allowedClicked = false
        var dismissedClicked = false

        composeRule.setContent {
            FazaPlayerTheme(themeMode = ThemeMode.DARK) {
                AllFileAccessPermissionScreen(
                    onAllowAccess = { allowedClicked = true },
                    onDismiss = { dismissedClicked = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("all_file_access_illustration").assertIsDisplayed()
        composeRule.onNodeWithTag("all_file_access_message")
            .assertIsDisplayed()
            .assertTextEquals("To find media & subtitles on your device, we require 'All File Access'.")

        composeRule.onNodeWithTag("media_access_grant_button")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("media_access_why_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("media_access_why_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("media_access_why_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("media_access_why_dialog").assertDoesNotExist()

        composeRule.onNodeWithTag("media_access_grant_button").performClick()
        assertTrue(allowedClicked)

        composeRule.onNodeWithTag("media_access_close_icon").performClick()
        assertTrue(dismissedClicked)
    }

    @Test
    fun systemFileAccess_buildsPlatformSpecificSystemSettingsIntents() {
        val android15Intents = com.example.util.buildAllFilesAccessSystemIntents(
            packageName = "apk.faza.go",
            sdkInt = android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
        )
        org.junit.Assert.assertEquals(3, android15Intents.size)
        org.junit.Assert.assertEquals(
            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            android15Intents[0].action
        )
        org.junit.Assert.assertEquals("package:apk.faza.go", android15Intents[0].dataString)
        org.junit.Assert.assertEquals(
            android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
            android15Intents[1].action
        )
        org.junit.Assert.assertEquals(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android15Intents[2].action
        )

        val legacyIntents = com.example.util.buildAllFilesAccessSystemIntents(
            packageName = "apk.faza.go",
            sdkInt = android.os.Build.VERSION_CODES.Q
        )
        org.junit.Assert.assertEquals(1, legacyIntents.size)
        org.junit.Assert.assertEquals(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            legacyIntents[0].action
        )
        org.junit.Assert.assertEquals("package:apk.faza.go", legacyIntents[0].dataString)
    }
}
