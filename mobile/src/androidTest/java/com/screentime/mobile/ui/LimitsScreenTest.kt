package com.screentime.mobile.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import com.screentime.mobile.ui.limits.LimitsScreen
import com.screentime.mobile.ui.limits.LimitsViewModel
import com.screentime.mobile.ui.limits.LimitsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class LimitsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeState = MutableStateFlow(LimitsUiState())
    private val fakeWriteError = MutableStateFlow<String?>(null)
    private val mockViewModel = mock<LimitsViewModel>()

    @Before
    fun setUp() {
        whenever(mockViewModel.state).thenReturn(fakeState)
        whenever(mockViewModel.writeError).thenReturn(fakeWriteError)
    }

    @Test
    fun limitsScreenTitleIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme {
                LimitsScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNodeWithText("Limits").assertIsDisplayed()
    }

    @Test
    fun heroCardLabelIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme {
                LimitsScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNodeWithText("TODAY'S SCREEN TIME").assertIsDisplayed()
    }

    @Test
    fun fabAddLimitIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme {
                LimitsScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNodeWithText("+ Add limit").assertIsDisplayed()
    }

    @Test
    fun appLimitsSectionHeaderIsDisplayed() {
        composeRule.setContent {
            ScreenTimeTheme {
                LimitsScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("App limits"))
        composeRule.onNodeWithText("App limits").assertIsDisplayed()
    }

    @Test
    fun progressBarSemantics() {
        composeRule.setContent {
            ScreenTimeTheme {
                LimitsScreen(viewModel = mockViewModel)
            }
        }
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..1f))).assertIsDisplayed()
    }

    @Test
    fun titleIs30sp() {
        composeRule.setContent {
            ScreenTimeTheme {
                LimitsScreen(viewModel = mockViewModel)
            }
        }
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Limits")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
        val layoutResult = textLayoutResults.first()
        assertEquals(30.sp, layoutResult.layoutInput.style.fontSize)
    }
}
