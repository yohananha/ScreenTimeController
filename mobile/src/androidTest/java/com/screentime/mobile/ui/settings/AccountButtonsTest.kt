package com.screentime.mobile.ui.settings

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * The Account card: plain text rows (delete actions in red), and every
 * action — Sign out included — behind a confirm dialog. Also saves PNGs to
 * the app's external files dir under account-buttons/ for a visual check.
 */
@RunWith(AndroidJUnit4::class)
class AccountButtonsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var signedOut = 0
    private var deletedFamily = 0
    private var deletedAccount = 0

    @Composable
    private fun InLocale(tag: String, content: @Composable () -> Unit) {
        val base = LocalContext.current
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        CompositionLocalProvider(
            LocalContext provides base.createConfigurationContext(config),
            LocalConfiguration provides config,
            LocalLayoutDirection provides if (tag == "he") LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) { content() }
    }

    private fun render(tag: String, isOwner: Boolean) {
        // A dialog opens in its own window, which reads the locale from the
        // Activity's resources rather than from InLocale below — so set it
        // there too (in the app the locale is app-wide, so dialogs follow it).
        composeRule.runOnUiThread {
            val res = composeRule.activity.resources
            val config = Configuration(res.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
        }
        composeRule.setContent {
            ScreenTimeTheme {
                InLocale(tag) {
                    // A typical phone content width (360dp screen minus the screen's side padding).
                    Box(Modifier.testTag("card").background(PeachPlum.colors.background).padding(16.dp).width(328.dp)) {
                        AccountActions(
                            isOwner = isOwner,
                            busy = false,
                            error = null,
                            onSignOut = { signedOut++ },
                            onDeleteFamily = { deletedFamily++ },
                            onDeleteAccount = { deletedAccount++ },
                        )
                    }
                }
            }
        }
    }

    private fun save(node: SemanticsNodeInteraction, name: String) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "account-buttons")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun signOutAsksFirst_cancelDoesNothing_confirmSignsOut() {
        render("en", isOwner = true)
        save(composeRule.onNodeWithTag("card"), "owner_en")

        composeRule.onNodeWithText("Sign out").performClick()
        composeRule.onNodeWithText("Sign out?").assertExists()
        save(composeRule.onNode(isDialog()), "signout_dialog_en")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Sign out?").assertDoesNotExist()
        assertEquals(0, signedOut)

        composeRule.onNodeWithText("Sign out").performClick()
        // The dialog's confirm button is the second "Sign out" on screen.
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onAllNodesWithTextInDialog("Sign out").performClick()
        assertEquals(1, signedOut)
    }

    @Test
    fun deleteActionsAskFirst() {
        render("en", isOwner = true)
        composeRule.onNodeWithText("Delete family").performClick()
        composeRule.onNodeWithText("Delete this family?").assertExists()
        composeRule.onNodeWithText("Delete permanently").performClick()
        assertEquals(1, deletedFamily)
        assertEquals(0, deletedAccount)
    }

    @Test
    fun coParentHasNoDeleteFamily() {
        render("en", isOwner = false)
        composeRule.onNodeWithText("Delete family").assertDoesNotExist()
        composeRule.onNodeWithText("Delete my account").assertExists()
        save(composeRule.onNodeWithTag("card"), "coparent_en")
    }

    @Test
    fun hebrewLayoutAndSignOutDialog() {
        render("he", isOwner = true)
        composeRule.onNodeWithText("מחיקת החשבון שלי").assertExists()
        save(composeRule.onNodeWithTag("card"), "owner_he")
        composeRule.onNodeWithText("התנתקות").performClick()
        composeRule.onNodeWithText("להתנתק?").assertExists()
        save(composeRule.onNode(isDialog()), "signout_dialog_he")
    }
}

/** The dialog's own button with [text], not the row behind it that has the same label. */
private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextInDialog(text: String) =
    onNode(
        androidx.compose.ui.test.hasText(text) and
            androidx.compose.ui.test.hasAnyAncestor(isDialog()),
    )
