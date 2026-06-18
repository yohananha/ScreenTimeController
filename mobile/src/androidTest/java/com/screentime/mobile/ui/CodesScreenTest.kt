package com.screentime.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.screentime.mobile.ui.components.CodeTilesRow
import com.screentime.mobile.ui.components.HeroCard
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius
import com.screentime.mobile.ui.theme.SproutPalette
import com.screentime.mobile.ui.codes.CodesScreen
import com.screentime.mobile.ui.codes.CodesViewModel
import com.screentime.mobile.ui.codes.CodesUiState
import com.screentime.shared.model.OneTimeCode
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CodesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeState = MutableStateFlow(CodesUiState())
    private val mockViewModel = mock<CodesViewModel>()

    @Before
    fun setUp() {
        whenever(mockViewModel.state).thenReturn(fakeState)
    }

    @Test
    fun codeTilesRowRendersDigits() {
        composeRule.setContent {
            ScreenTimeTheme {
                CodeTilesRow(code = "1234")
            }
        }
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun heroCardWithCodeContentRenders() {
        composeRule.setContent {
            ScreenTimeTheme {
                HeroCard { Text("UNLOCK CODE") }
            }
        }
        composeRule.onNodeWithText("UNLOCK CODE").assertIsDisplayed()
    }

    @Test
    fun singleUsePillLabelIsVisible() {
        composeRule.setContent {
            ScreenTimeTheme {
                HeroCard {
                    Row {
                        Text("UNLOCK CODE")
                        Box(
                            modifier = Modifier
                                .background(Sprout.colors.accent, SproutRadius.pill)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "Single-use",
                                style = Sprout.typography.caption,
                                color = Sprout.colors.ink,
                            )
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("Single-use").assertIsDisplayed()
    }

    @Test
    fun noCodeStatePlaceholders() {
        fakeState.value = CodesUiState(active = null)
        composeRule.setContent {
            ScreenTimeTheme {
                CodesScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNodeWithText("Generate a code below.").assertIsDisplayed()
        composeRule.onAllNodesWithText("–").assertCountEquals(4)
    }

    @Test
    fun activeCodeDigitTilesVisible() {
        fakeState.value = CodesUiState(
            active = OneTimeCode("5678", 30, Instant.now().plusSeconds(60))
        )
        composeRule.setContent {
            ScreenTimeTheme {
                CodesScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNodeWithText("5").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun singleUsePillColor() {
        assertEquals(Color(0xFFB9A8F0), SproutPalette.accent)
    }
}
