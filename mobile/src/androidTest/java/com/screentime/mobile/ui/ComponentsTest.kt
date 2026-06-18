package com.screentime.mobile.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.screentime.mobile.ui.components.ChipGroup
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.components.StatusBadge
import com.screentime.mobile.ui.components.Status
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusBadgeOnTrackIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { StatusBadge(status = Status.OnTrack) }
        }
        composeRule.onNodeWithText("On track").assertIsDisplayed()
    }

    @Test
    fun statusBadgeAlmostUpIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { StatusBadge(status = Status.AlmostUp) }
        }
        composeRule.onNodeWithText("Almost up").assertIsDisplayed()
    }

    @Test
    fun statusBadgeTimesUpIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { StatusBadge(status = Status.TimesUp) }
        }
        composeRule.onNodeWithText("Time's up").assertIsDisplayed()
    }

    @Test
    fun statusBadgePausedIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { StatusBadge(status = Status.Paused) }
        }
        composeRule.onNodeWithText("Paused").assertIsDisplayed()
    }

    @Test
    fun primaryButtonEnabledIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { SproutPrimaryButton(text = "Confirm", onClick = {}) }
        }
        composeRule.onNodeWithText("Confirm").assertIsDisplayed()
    }

    @Test
    fun primaryButtonDisabledIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { SproutPrimaryButton(text = "Disabled", onClick = {}, enabled = false) }
        }
        composeRule.onNodeWithText("Disabled").assertIsDisplayed()
    }

    @Test
    fun ghostButtonIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme { SproutGhostButton(text = "Cancel", onClick = {}) }
        }
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun chipGroupTogglesSelection() {
        var selected = 15
        composeRule.setContent {
            ScreenTimeTheme {
                var sel by remember { mutableStateOf(15) }
                ChipGroup(
                    options = listOf(15, 30, 60),
                    selected = sel,
                    onSelect = { sel = it; selected = it },
                    label = { "${it}m" },
                )
            }
        }
        composeRule.onNodeWithText("30m").performClick()
        assertEquals(30, selected)
    }

    @Test
    fun chipGroupShowsAllOptions() {
        composeRule.setContent {
            ScreenTimeTheme {
                ChipGroup(
                    options = listOf(15, 30, 60),
                    selected = 15,
                    onSelect = {},
                    label = { "${it}m" },
                )
            }
        }
        composeRule.onNodeWithText("15m").assertIsDisplayed()
        composeRule.onNodeWithText("30m").assertIsDisplayed()
        composeRule.onNodeWithText("60m").assertIsDisplayed()
    }
}
