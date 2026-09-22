package com.screentime.mobile.ui

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.screentime.mobile.ui.theme.RubikFont
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import com.screentime.mobile.ui.theme.PeachPlumPalette
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeachPlumThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headlineFontIsRubik() {
        composeRule.setContent {
            ScreenTimeTheme {
                val style = com.screentime.mobile.ui.theme.PeachPlum.typography.display
                assertEquals(RubikFont, style.fontFamily)
                Text("Headline", style = style)
            }
        }
        composeRule.onNodeWithText("Headline").assertIsDisplayed()
    }

    @Test
    fun bodyFontIsRubik() {
        composeRule.setContent {
            ScreenTimeTheme {
                val style = com.screentime.mobile.ui.theme.PeachPlum.typography.label
                assertEquals(RubikFont, style.fontFamily)
                Text("Body", style = style)
            }
        }
        composeRule.onNodeWithText("Body").assertIsDisplayed()
    }

    @Test
    fun primaryColorIsCorrect() {
        assertEquals(Color(0xFFFFB088), PeachPlumPalette.primary)
    }

    @Test
    fun backgroundIsCreamy() {
        assertEquals(Color(0xFFFFF6EE), PeachPlumPalette.background)
    }

    @Test
    fun inkColorIsPlum() {
        assertEquals(Color(0xFF2A1E2E), PeachPlumPalette.ink)
    }

    @Test
    fun titleFontIsRubik() {
        composeRule.setContent {
            ScreenTimeTheme {
                val style = com.screentime.mobile.ui.theme.PeachPlum.typography.title
                assertEquals(RubikFont, style.fontFamily)
                Text("Title", style = style)
            }
        }
        composeRule.onNodeWithText("Title").assertIsDisplayed()
    }
}
