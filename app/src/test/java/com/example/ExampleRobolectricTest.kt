package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FAZA Player", appName)
        assertEquals("Gooin India Pvt Ltd", context.getString(R.string.company_name))
        assertEquals("Rajbir Singh", context.getString(R.string.developer_name))
    }

    @Test
    fun `main activity launches and navigates dock tabs without crash`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("floating_navigation_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNode(hasText("Home") and hasAnyAncestor(hasTestTag("floating_navigation_dock"))).assertDoesNotExist()
        composeRule.onNodeWithTag("nav_favorites").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_profile").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("floating_search_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_favorites").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("favorites_empty_state").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_hero_card").assertIsDisplayed()

        composeRule.onNodeWithTag("floating_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_results_list").assertIsDisplayed()

        composeRule.onNodeWithTag("search_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("floating_search_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_search_field").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("floating_search_button").assertIsDisplayed()
    }

    @Test
    fun `theme settings supports switching between wallpaper colour and custom colour`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_folder_list")
            .performScrollToNode(hasTestTag("settings_folder_appearance"))
        composeRule.waitForIdle()

        // Navigate into 2nd folder: Theme (Appearance)
        composeRule.onNodeWithTag("settings_folder_appearance")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        // Scroll Application Colour card to top of viewport cleanly above FloatingNavigationDock
        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(1)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("app_color_source_wallpaper").assertIsDisplayed()
        composeRule.onNodeWithTag("app_color_source_custom").assertIsDisplayed().performClick()
        repeat(20) {
            composeRule.waitForIdle()
            if (composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("custom_color_picker_section"))
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                return@repeat
            }
            Thread.sleep(50)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_detail_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_color_picker_section").assertIsDisplayed()
        composeRule.onNodeWithTag("custom_color_preset_royal_purple").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_color_source_wallpaper").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `profile tab shows Gemini AI assistant with modes and search toggle`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_tab_overview").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_tab_assistant").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_screen_list")
            .performScrollToNode(hasTestTag("profile_ai_launcher_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_ai_launcher_card").assertIsDisplayed()

        composeRule.onNodeWithTag("profile_tab_assistant").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_assistant_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("assistant_welcome_view").assertIsDisplayed()
        composeRule.onNodeWithTag("assistant_google_search_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("assistant_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("assistant_send_button").assertIsDisplayed()

        composeRule.onNodeWithTag("assistant_mode_deep_reasoning").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("assistant_google_search_toggle").assertDoesNotExist()

        composeRule.onNodeWithTag("assistant_mode_general_search").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("assistant_google_search_toggle").assertIsDisplayed()

        composeRule.onNodeWithTag("profile_tab_overview").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_hero_card").assertIsDisplayed()
    }

    @Test
    fun `settings toggles hardware acceleration and syncs with advanced decoder settings`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_quick_hardware_acceleration_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_quick_hardware_acceleration_switch")
            .assertIsDisplayed()
            .assertIsOn()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("settings_quick_hardware_acceleration_switch").assertIsOff()
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithTag("settings_folder_list")
            .performScrollToNode(hasTestTag("settings_folder_advanced"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_folder_advanced")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("advanced_hardware_acceleration_switch")
            .assertIsDisplayed()
            .assertIsOff()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("advanced_hardware_acceleration_switch").assertIsOn()
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_folder_list")
            .performScrollToNode(hasTestTag("settings_folder_playback"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_folder_playback").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("auto_resume_on_restart_switch")
            .assertIsDisplayed()
            .assertIsOn()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("auto_resume_on_restart_switch").assertIsOff()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun `display folder holds all player ui tabs and running time cycles on tap`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()

        // 1st folder is Display (Player UI, Now Playing, Running Time, Subtitles)
        composeRule.onNodeWithTag("settings_folder_list")
            .performScrollToNode(hasTestTag("settings_folder_display"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_folder_display").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("display_tab_player_ui").assertIsDisplayed()
        composeRule.onNodeWithTag("display_tab_now_playing").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("now_playing_up_next_badge", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Next in 05:00")
        composeRule.onNodeWithTag("np_preview_up_next").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("display_tab_running_time").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(1)
        composeRule.waitForIdle()

        // Default mode: Elapsed / Remaining (12:00/02:00)
        composeRule.onNodeWithTag("running_time_text", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("12:00/02:00")

        // Tap 1 -> Elapsed / Total (12:00/14:00)
        composeRule.onNodeWithTag("always_on_running_time").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("running_time_text", useUnmergedTree = true)
                    .assertTextEquals("12:00/14:00")
                true
            }.getOrDefault(false)
        }

        // Tap 2 -> Elapsed Only (12:00)
        composeRule.onNodeWithTag("always_on_running_time").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("running_time_text", useUnmergedTree = true)
                    .assertTextEquals("12:00")
                true
            }.getOrDefault(false)
        }

        // Tap 3 -> Cycles back to Elapsed / Remaining (12:00/02:00)
        composeRule.onNodeWithTag("always_on_running_time").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("running_time_text", useUnmergedTree = true)
                    .assertTextEquals("12:00/02:00")
                true
            }.getOrDefault(false)
        }

        // Customized placement chips: switch to Bottom Left, then Custom to reveal X/Y sliders & reset
        composeRule.onNodeWithTag("rt_position_bottom_start")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("rt_placement_summary", useUnmergedTree = true)
                    .assertTextEquals("Bottom Left")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("rt_position_custom")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("rt_custom_x_slider"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("rt_custom_x_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("rt_custom_y_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("rt_reset_placement_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("rt_placement_summary", useUnmergedTree = true)
                    .assertTextEquals("Top Right")
                true
            }.getOrDefault(false)
        }

        // Permanent App Icon sub-tab: optional (off by default), full customization & placement controls
        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("display_tab_app_icon").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_icon_enabled_switch")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("app_icon_position_bottom_end"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("always_on_app_icon").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_icon_position_bottom_end")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("app_icon_placement_summary", useUnmergedTree = true)
                    .assertTextEquals("Bottom Right")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("app_icon_position_custom")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("app_icon_custom_x_slider"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("app_icon_custom_x_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_icon_custom_y_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_icon_reset_placement_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("app_icon_placement_summary", useUnmergedTree = true)
                    .assertTextEquals("Top Left")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("app_icon_style_pill_brand")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("always_on_app_icon_label"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("app_icon_animation_summary", useUnmergedTree = true)
            .performScrollTo()
            .assertTextEquals("Off")
        // Verify legacy motion animations are completely removed
        composeRule.onNodeWithTag("app_icon_anim_pulse").assertDoesNotExist()
        composeRule.onNodeWithTag("app_icon_anim_float").assertDoesNotExist()
        composeRule.onNodeWithTag("app_icon_anim_spin").assertDoesNotExist()
        composeRule.onNodeWithTag("app_icon_anim_swing").assertDoesNotExist()
        composeRule.onNodeWithTag("app_icon_anim_gemini_glow")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("app_icon_animation_summary", useUnmergedTree = true)
                    .assertTextEquals("Gemini Glow")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("app_icon_anim_speed_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_icon_anim_only_playing_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_icon_language_switch")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodes(hasTestTag("always_on_app_icon_language"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("app_icon_lang_layout_summary", useUnmergedTree = true)
            .performScrollTo()
            .assertTextEquals("One Line")
        composeRule.onNodeWithTag("app_icon_lang_layout_two_lines")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("app_icon_lang_layout_summary", useUnmergedTree = true)
                    .assertTextEquals("Two Lines")
                true
            }.getOrDefault(false)
        }

        // Subtitles sub-tab is also accessible inside Display folder
        composeRule.onNodeWithTag("settings_detail_list").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("display_tab_subtitles").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subtitle_enabled_switch").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `settings tab hides search and shows updated about and updates links and Made in India`() {
        assertEquals(59, BuildConfig.VERSION_CODE)
        assertEquals("1.57 Beta", BuildConfig.VERSION_NAME)

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("floating_search_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_search_field").assertDoesNotExist()
        composeRule.onNodeWithText("01", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("02", useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithTag("settings_folder_list")
            .performScrollToNode(hasTestTag("settings_folder_about"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("me_profile_card").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_folder_about").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("about_application_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list")
            .performScrollToNode(hasTestTag("about_whats_new_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_whats_new_card").assertIsDisplayed()
        composeRule.onNode(hasText("What's New – v1.57 Beta"), useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("settings_detail_list")
            .performScrollToNode(hasTestTag("about_direct_download_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_direct_download_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_github_download_button", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("about_telegram_download_button", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("settings_detail_list")
            .performScrollToNode(hasTestTag("about_buy_me_coffee_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_buy_me_coffee_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_buy_me_coffee_title", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("\u2615 Buy Me A Coffee")
        composeRule.onNodeWithTag("about_buy_me_coffee_subtitle", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Support the Developer \u2764\uFE0F")
        composeRule.onNodeWithTag("about_buy_me_coffee_button").assertIsDisplayed()
        assertEquals(
            "https://buymeacoffee.com/FGa6Tc98UN",
            com.example.ui.screens.settings.BUY_ME_A_COFFEE_URL
        )

        composeRule.onNodeWithTag("settings_detail_list")
            .performScrollToNode(hasTestTag("about_facebook_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_facebook_button").assertIsDisplayed()
        composeRule.onNodeWithTag("about_instagram_button").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_detail_list")
            .performScrollToNode(hasTestTag("about_special_thanks_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_special_thanks_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_special_thanks_google_icon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("about_special_thanks_title", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Special Thanks")
        composeRule.onNodeWithTag("about_special_thanks_message", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals(
                "Special thanks to Google for helping us to build FAZA Player. Your technology, innovation, and support have helped make this application possible."
            )

        composeRule.onNodeWithTag("settings_detail_list")
            .performScrollToNode(hasTestTag("about_made_in_india_text"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_made_in_india_text", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Made in India \uD83C\uDDEE\uD83C\uDDF3")
    }

    @Test
    fun `test banner ads show on home and profile only when online and hide offline`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<FazaApplication>()
        kotlinx.coroutines.runBlocking {
            app.preferencesRepository.setFreeAccessState(watchedCount = 0, expiresAtMs = 0L)
        }
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val shadowCm = shadowOf(cm)

        val activeNetwork = cm.activeNetwork ?: org.robolectric.shadows.ShadowNetwork.newInstance(101)
        val onlineCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(onlineCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowCm.setNetworkCapabilities(activeNetwork, onlineCaps)
        shadowCm.networkCallbacks.forEach { callback ->
            callback.onAvailable(activeNetwork)
            callback.onCapabilitiesChanged(activeNetwork, onlineCaps)
        }
        assertTrue(com.example.ads.FazaAdsManager.isInternetConnected(context))

        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule
                .onAllNodes(hasTestTag("home_ad_banner"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("home_ad_banner").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule
                .onAllNodes(hasTestTag("profile_ad_banner"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("profile_ad_banner").assertIsDisplayed()

        // Disconnect internet -> banners hide immediately and Free Access enters offline mode without errors
        val offlineCaps = ShadowNetworkCapabilities.newInstance()
        shadowCm.setNetworkCapabilities(activeNetwork, offlineCaps)
        shadowCm.setDefaultNetworkActive(false)
        shadowCm.setActiveNetworkInfo(null)
        shadowCm.networkCallbacks.forEach { callback ->
            callback.onCapabilitiesChanged(activeNetwork, offlineCaps)
            callback.onLost(activeNetwork)
        }
        assertFalse(com.example.ads.FazaAdsManager.isInternetConnected(context))

        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule
                .onAllNodes(hasTestTag("profile_ad_banner"))
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.onNodeWithTag("profile_screen_list")
            .performScrollToNode(hasTestTag("profile_free_access_card"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("free_access_watch_ad_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("free_access_offline_note").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule
                .onAllNodes(hasTestTag("home_ad_banner"))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    @Test
    fun `rewarded ads track 0 to 8 unlock 24h ad-free access persist and restore ads on expiry`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<FazaApplication>()
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val shadowCm = shadowOf(cm)

        try {
            kotlinx.coroutines.runBlocking {
                app.preferencesRepository.setFreeAccessState(watchedCount = 0, expiresAtMs = 0L)
            }

            // Ensure online
            val activeNetwork = cm.activeNetwork ?: org.robolectric.shadows.ShadowNetwork.newInstance(102)
            val onlineCaps = ShadowNetworkCapabilities.newInstance()
            shadowOf(onlineCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowCm.setNetworkCapabilities(activeNetwork, onlineCaps)
            shadowCm.networkCallbacks.forEach { callback ->
                callback.onAvailable(activeNetwork)
                callback.onCapabilitiesChanged(activeNetwork, onlineCaps)
            }

            // Navigate to Profile & locate Free Access section
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("nav_profile").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("profile_screen_list")
                .performScrollToNode(hasTestTag("profile_free_access_card"))
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("free_access_section_title", useUnmergedTree = true)
                .assertTextEquals("Free Access")
            composeRule.onNodeWithTag("free_access_option_title", useUnmergedTree = true)
                .assertTextEquals("Get 24 Hours Access")
            composeRule.onNodeWithTag("free_access_watched_count", useUnmergedTree = true)
                .assertTextEquals("Rewarded Ads Watched: 0/8")
            composeRule.onNodeWithTag("free_access_remaining_ads", useUnmergedTree = true)
                .assertTextEquals("Remaining Ads: 8")
            composeRule.onNodeWithTag("free_access_progress_percent", useUnmergedTree = true)
                .assertTextEquals("0%")
            composeRule.onNodeWithTag("free_access_watch_ad_button").assertIsEnabled()

            // 1. Skipped / dismissed early ad must NOT increment progress
            com.example.ads.FazaAdsManager.testRewardedAdLauncher = { sessionId, callbacks ->
                callbacks.onDismissedWithoutReward(sessionId)
            }
            composeRule.onNodeWithTag("free_access_watch_ad_button")
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("free_access_watched_count", useUnmergedTree = true)
                .assertTextEquals("Rewarded Ads Watched: 0/8")
            composeRule.onNodeWithTag("free_access_feedback_message", useUnmergedTree = true)
                .assertTextEquals("Ad was closed early. Watch the full ad to count toward free access.")

            // 2. Failed ad load must NOT increment progress
            com.example.ads.FazaAdsManager.testRewardedAdLauncher = { sessionId, callbacks ->
                callbacks.onFailed(sessionId, "Test ad unavailable")
            }
            composeRule.onNodeWithTag("free_access_watch_ad_button")
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("free_access_watched_count", useUnmergedTree = true)
                .assertTextEquals("Rewarded Ads Watched: 0/8")
            composeRule.onNodeWithTag("free_access_feedback_message", useUnmergedTree = true)
                .assertTextEquals("Test ad unavailable")

            // 3. Watch 3 valid ads (fire duplicate callback twice on each to prove deduplication)
            com.example.ads.FazaAdsManager.testRewardedAdLauncher = { sessionId, callbacks ->
                callbacks.onRewardEarned(sessionId)
                callbacks.onRewardEarned(sessionId) // duplicate callback must be ignored
            }
            repeat(3) { step ->
                composeRule.onNodeWithTag("free_access_watch_ad_button")
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.waitUntil(timeoutMillis = 3_000L) {
                    runBlocking {
                        app.preferencesRepository.preferencesFlow.first().rewardedAdsWatchedCount == step + 1
                    }
                }
                composeRule.waitForIdle()
            }
            composeRule.onNodeWithTag("free_access_watched_count", useUnmergedTree = true)
                .assertTextEquals("Rewarded Ads Watched: 3/8")
            composeRule.onNodeWithTag("free_access_remaining_ads", useUnmergedTree = true)
                .assertTextEquals("Remaining Ads: 5")
            composeRule.onNodeWithTag("free_access_progress_percent", useUnmergedTree = true)
                .assertTextEquals("37.5%")

            // 4. Watch remaining 5 ads (total 8/8) -> unlocks 24h access, resets progress to 0/8, hides banners
            repeat(5) { step ->
                composeRule.onNodeWithTag("free_access_watch_ad_button")
                    .performSemanticsAction(SemanticsActions.OnClick)
                val targetStep = step + 4
                composeRule.waitUntil(timeoutMillis = 3_000L) {
                    runBlocking {
                        val prefs = app.preferencesRepository.preferencesFlow.first()
                        if (targetStep < 8) {
                            prefs.rewardedAdsWatchedCount == targetStep
                        } else {
                            prefs.freeAccessExpiresAtMs > System.currentTimeMillis() && prefs.rewardedAdsWatchedCount == 0
                        }
                    }
                }
                composeRule.waitForIdle()
            }

            composeRule.onNodeWithTag("free_access_activated_label", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals("Access Activated: 24 Hours")
            composeRule.onNodeWithTag("free_access_remaining_time", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule.onNodeWithTag("free_access_feedback_message", useUnmergedTree = true)
                .assertTextEquals(com.example.ads.FREE_ACCESS_CONGRATULATIONS_MESSAGE)

            // Banner ads are hidden on Profile and Home while 24h access is active (even online)
            composeRule.onNodeWithTag("profile_ad_banner").assertDoesNotExist()
            composeRule.onNodeWithTag("nav_home").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("home_ad_banner").assertDoesNotExist()

            // 5. Simulate expiration -> unlock option returns and banner ads resume when online
            kotlinx.coroutines.runBlocking {
                app.preferencesRepository.setFreeAccessState(
                    watchedCount = 0,
                    expiresAtMs = System.currentTimeMillis() - 60_000L
                )
            }
            composeRule.waitUntil(timeoutMillis = 3_000L) {
                composeRule
                    .onAllNodes(hasTestTag("home_ad_banner"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("home_ad_banner").assertIsDisplayed()

            composeRule.onNodeWithTag("nav_profile").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("profile_ad_banner").assertIsDisplayed()
            composeRule.onNodeWithTag("profile_screen_list")
                .performScrollToNode(hasTestTag("profile_free_access_card"))
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("free_access_expired_badge", useUnmergedTree = true).assertIsDisplayed()
            composeRule.onNodeWithTag("free_access_option_title", useUnmergedTree = true)
                .assertTextEquals("Get 24 Hours Access")
        } finally {
            com.example.ads.FazaAdsManager.testRewardedAdLauncher = null
            kotlinx.coroutines.runBlocking {
                app.preferencesRepository.setFreeAccessState(watchedCount = 0, expiresAtMs = 0L)
            }
        }
    }

    @Test
    fun `firebase app check stores registered debug token when launched without intent extra`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val persistenceKey = com.google.firebase.FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.$persistenceKey",
            Context.MODE_PRIVATE
        )
        assertEquals(
            "e55f2c3d-a060-47e7-8a66-523c0ce67774",
            prefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null)
        )
        assertEquals(
            "e55f2c3d-a060-47e7-8a66-523c0ce67774",
            com.example.ai.resolveAppCheckDebugToken(context, null)
        )

        val overrideIntent = android.content.Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", "custom-harness-token")
        }
        assertEquals(
            "custom-harness-token",
            com.example.ai.resolveAppCheckDebugToken(context, overrideIntent)
        )
    }

    @Test
    fun `home screen shows minimal floating continue playing icon button with empty state and resume`() {
        val app = ApplicationProvider.getApplicationContext<FazaApplication>()

        // Minimal floating Play icon button is visible on Home screen with no text/card metadata
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("floating_continue_play_button").assertIsDisplayed()
        composeRule.onNodeWithTag("floating_continue_play_icon", useUnmergedTree = true).assertIsDisplayed()

        // Tapping with no previous video displays the empty-state message
        composeRule.onNodeWithTag("floating_continue_play_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule
                .onAllNodes(hasTestTag("continue_playing_empty_message"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("continue_playing_empty_message").assertIsDisplayed()

        // Once a previous video exists, the floating button still shows ONLY the icon (no filename/card text)
        kotlinx.coroutines.runBlocking {
            app.database.videoDao().insertVideo(
                com.example.data.model.VideoItem(
                    uri = "https://example.com/continue-end-test.mp4",
                    title = "Evening Documentary",
                    folderName = "Movies",
                    durationMs = 120_000L,
                    lastPlayedAt = System.currentTimeMillis(),
                    lastPositionMs = 35_000L
                )
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("floating_continue_play_button").assertIsDisplayed()
        composeRule.onNodeWithTag("floating_continue_play_icon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(
            hasText("Evening Documentary") and hasAnyAncestor(hasTestTag("floating_continue_play_button")),
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `home screen displays device video folders only and opens folder videos on tap`() {
        val app = ApplicationProvider.getApplicationContext<FazaApplication>()

        // Empty state when no video folders are available
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_empty_folders_state").assertIsDisplayed()

        // Insert videos inside a device folder
        val insertedId = kotlinx.coroutines.runBlocking {
            app.database.videoDao().insertVideo(
                com.example.data.model.VideoItem(
                    uri = "content://media/external/video/media/901",
                    title = "Vacation Clip.mp4",
                    folderName = "Camera",
                    durationMs = 95_000L,
                    sizeBytes = 24L * 1024L * 1024L
                )
            )
        }

        // Home screen shows the device video folder card and does NOT show individual video cards directly
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("folder_card_Camera"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("home_folders_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_card_Camera").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_thumb_Camera", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("video_card_$insertedId").assertDoesNotExist()

        // Tapping the folder opens FolderDetailScreen and displays its videos
        composeRule.onNodeWithTag("folder_card_Camera").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("folder_detail_video_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("folder_detail_title", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Camera")
        composeRule.onNodeWithTag("video_card_$insertedId").assertIsDisplayed()

        // Navigating back returns smoothly to the Home folders screen
        composeRule.onNodeWithTag("folder_detail_back_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("folder_card_Camera"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun `favorites tab allows bookmarking managing and quickly accessing preferred videos`() {
        val app = ApplicationProvider.getApplicationContext<FazaApplication>()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_favorites").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("favorites_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_browse_folders_button").assertIsDisplayed()

        // Insert a public video and a private favorited video
        val publicVideoId = kotlinx.coroutines.runBlocking {
            app.database.videoDao().insertVideo(
                com.example.data.model.VideoItem(
                    uri = "content://media/external/video/media/950",
                    title = "Favorite Concert 4K.mp4",
                    folderName = "Concerts",
                    durationMs = 240_000L,
                    sizeBytes = 64L * 1024L * 1024L,
                    isFavourite = false,
                    isPrivate = false
                )
            )
        }
        val privateVideoId = kotlinx.coroutines.runBlocking {
            app.database.videoDao().insertVideo(
                com.example.data.model.VideoItem(
                    uri = "content://media/external/video/media/951",
                    title = "Private Clip.mp4",
                    folderName = "Vault",
                    durationMs = 60_000L,
                    isFavourite = true,
                    isPrivate = true
                )
            )
        }

        // Empty state still holds (private video excluded) and now offers quick-add CTA
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("favorites_empty_add_button"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("video_card_$privateVideoId").assertDoesNotExist()

        // Open Manage Favorites sheet and bookmark the public video
        composeRule.onNodeWithTag("favorites_empty_add_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("favorites_picker_toggle_$publicVideoId"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("favorites_picker_toggle_$publicVideoId")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("favorites_picker_done_button")
            .performSemanticsAction(SemanticsActions.OnClick)

        // Bookmarked video appears in the Favorites tab with summary card and Play action
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("video_card_$publicVideoId"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("favorites_video_list").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_play_first_button", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("video_card_$publicVideoId").assertIsDisplayed()
        composeRule.onNodeWithTag("video_card_$privateVideoId").assertDoesNotExist()

        // Unbookmarking directly from Favorites removes the item and restores empty state
        composeRule.onNodeWithTag("fav_button_$publicVideoId")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodes(hasTestTag("favorites_empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("favorites_empty_state").assertIsDisplayed()
    }
}
