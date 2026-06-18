package com.screentime.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import com.screentime.mobile.ui.theme.rememberScreenPadding
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponsiveLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenPaddingUtilityRendersWithoutError() {
        composeRule.setContent {
            ScreenTimeTheme {
                val pad = rememberScreenPadding()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = pad)
                        .testTag("padded_box"),
                ) {
                    Text("Content")
                }
            }
        }
        composeRule.onNodeWithTag("padded_box").assertIsDisplayed()
    }

    @Test
    fun screenPaddingIsAtLeast16dp() {
        var observedPadding = 0.dp
        composeRule.setContent {
            ScreenTimeTheme {
                observedPadding = rememberScreenPadding()
                Text("Pad check")
            }
        }
        composeRule.waitForIdle()
        assert(observedPadding >= 16.dp) {
            "Expected padding >= 16dp but was $observedPadding"
        }
    }

    @Test
    fun heroCardDecorativeCircleDoesNotCrash() {
        composeRule.setContent {
            ScreenTimeTheme {
                com.screentime.mobile.ui.components.HeroCard {
                    Text("Hello")
                }
            }
        }
        composeRule.onNodeWithText("Hello").assertIsDisplayed()
    }

    @Test
    fun compactWidthUses16dpPadding() {
        var observedPadding = 0.dp
        composeRule.setContent {
            val configuration = android.content.res.Configuration().apply {
                screenWidthDp = 360
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides configuration
            ) {
                ScreenTimeTheme {
                    observedPadding = rememberScreenPadding()
                }
            }
        }
        assertEquals(16.dp, observedPadding)
    }

    @Test
    fun mediumWidthUses32dpPadding() {
        var observedPadding = 0.dp
        composeRule.setContent {
            val configuration = android.content.res.Configuration().apply {
                screenWidthDp = 600
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides configuration
            ) {
                ScreenTimeTheme {
                    observedPadding = rememberScreenPadding()
                }
            }
        }
        assertEquals(32.dp, observedPadding)
    }
}
