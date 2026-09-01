package com.screentime.tv.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeRequest
import com.screentime.tv.R
import com.screentime.tv.service.BlockReason
import com.screentime.tv.ui.theme.ScreenTimeTvTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the kid-facing block overlay directly (not through
 * BlockOverlayController's WindowManager path, which instrumentation tests
 * can't easily attach to). Covers a representative sample of the 11 view
 * states rather than all of them exhaustively — this is the first test file
 * in tv/src/androidTest (the release workflow's `android-ui-tv` job had
 * nothing to run before this), so the goal is establishing real coverage of
 * the mechanism, not exhaustiveness in one pass.
 *
 * The Hebrew/RTL test below wraps content with `createConfigurationContext`
 * + `LocalLayoutDirection` — deliberately the same mechanism
 * TvLocaleController.wrap() uses in production, so a green test here is
 * evidence that mechanism actually works, not just that the strings exist.
 */
@RunWith(AndroidJUnit4::class)
class BlockOverlayContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun defaultLockout() = LockoutSettings()
    private fun defaultLimits() = Limits()

    @Test
    fun mainView_dailyLimitReached_showsWrapUpMessage() {
        composeRule.setContent {
            ScreenTimeTvTheme {
                BlockOverlayContent(
                    blockedPackage = "com.example.videos",
                    blockReason = BlockReason.DailyLimitReached,
                    lockout = defaultLockout(),
                    requestStatus = null,
                    approvedMinutes = null,
                    limits = defaultLimits(),
                    usedMillis = 0L,
                    backPressHandler = BackPressHandler(),
                    onSubmitCode = { true },
                    onSubmitRequest = { true },
                    onLockoutTick = {},
                )
            }
        }
        composeRule.onNodeWithText(targetContext.getString(R.string.overlay_main_title)).assertIsDisplayed()
    }

    @Test
    fun mainView_instantLocked_showsLockedMessage() {
        composeRule.setContent {
            ScreenTimeTvTheme {
                BlockOverlayContent(
                    blockedPackage = "com.example.videos",
                    blockReason = BlockReason.InstantLocked,
                    lockout = defaultLockout(),
                    requestStatus = null,
                    approvedMinutes = null,
                    limits = defaultLimits(),
                    usedMillis = 0L,
                    backPressHandler = BackPressHandler(),
                    onSubmitCode = { true },
                    onSubmitRequest = { true },
                    onLockoutTick = {},
                )
            }
        }
        composeRule.onNodeWithText(targetContext.getString(R.string.overlay_instant_locked_title)).assertIsDisplayed()
    }

    @Test
    fun lockedView_parentUnlockMode_showsAskParentMessage() {
        composeRule.setContent {
            ScreenTimeTvTheme {
                BlockOverlayContent(
                    blockedPackage = "com.example.videos",
                    blockReason = BlockReason.DailyLimitReached,
                    lockout = LockoutSettings(locked = true, mode = LockoutMode.PARENT_UNLOCK),
                    requestStatus = null,
                    approvedMinutes = null,
                    limits = defaultLimits(),
                    usedMillis = 0L,
                    backPressHandler = BackPressHandler(),
                    onSubmitCode = { true },
                    onSubmitRequest = { true },
                    onLockoutTick = {},
                )
            }
        }
        composeRule.onNodeWithText(targetContext.getString(R.string.overlay_locked_parent_title)).assertIsDisplayed()
    }

    @Test
    fun approvedView_withMinutes_showsApprovedHeadline() {
        composeRule.setContent {
            ScreenTimeTvTheme {
                BlockOverlayContent(
                    blockedPackage = "com.example.videos",
                    blockReason = BlockReason.DailyLimitReached,
                    lockout = defaultLockout(),
                    requestStatus = TimeRequest.Status.Approved,
                    approvedMinutes = 15,
                    limits = defaultLimits(),
                    usedMillis = 0L,
                    backPressHandler = BackPressHandler(),
                    onSubmitCode = { true },
                    onSubmitRequest = { true },
                    onLockoutTick = {},
                )
            }
        }
        val expected = targetContext.getString(R.string.overlay_approved_headline, 15)
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun mainView_hebrewLocale_showsHebrewWrapUpMessage() {
        composeRule.setContent {
            val base = LocalContext.current
            val hebrewContext = remember(base) {
                val config = Configuration(base.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag("he"))
                    setLayoutDirection(Locale.forLanguageTag("he"))
                }
                base.createConfigurationContext(config)
            }
            CompositionLocalProvider(
                LocalContext provides hebrewContext,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                ScreenTimeTvTheme {
                    BlockOverlayContent(
                        blockedPackage = "com.example.videos",
                        blockReason = BlockReason.DailyLimitReached,
                        lockout = defaultLockout(),
                        requestStatus = null,
                        approvedMinutes = null,
                        limits = defaultLimits(),
                        usedMillis = 0L,
                        backPressHandler = BackPressHandler(),
                        onSubmitCode = { true },
                        onSubmitRequest = { true },
                        onLockoutTick = {},
                    )
                }
            }
        }
        // Hardcoded literal, not re-derived via getString(R.string.overlay_main_title,
        // locale = he): that lookup goes through the exact same resource-resolution
        // path this test exists to catch, so a regression there would silently
        // make both sides of the comparison fall back to the English string and
        // the assertion would still pass. See values-iw/ vs values-he/ note on
        // that resource directory for why "he" alone doesn't resolve on this
        // toolchain — Android's runtime locale matching for Hebrew keys off the
        // legacy ISO-639 code "iw", not the modern "he".
        composeRule.onNodeWithText("זהו, סיימנו להיום!").assertIsDisplayed()
    }
}
