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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
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
 * The Account card's buttons: stacked full-width, delete actions in red.
 * Also saves a PNG per variant (en/he × owner/co-parent) to the app's
 * external files dir under account-buttons/, for a visual check.
 */
@RunWith(AndroidJUnit4::class)
class AccountButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

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

    private fun render(tag: String, isOwner: Boolean, onDeleteFamily: () -> Unit = {}, onDeleteAccount: () -> Unit = {}) {
        composeRule.setContent {
            ScreenTimeTheme {
                InLocale(tag) {
                    // A typical phone content width (360dp screen minus the screen's side padding).
                    Box(Modifier.testTag("card").background(PeachPlum.colors.background).padding(16.dp).width(328.dp)) {
                        AccountButtons(
                            isOwner = isOwner,
                            busy = false,
                            onSignOut = {},
                            onDeleteFamily = onDeleteFamily,
                            onDeleteAccount = onDeleteAccount,
                        )
                    }
                }
            }
        }
    }

    private fun saveScreenshot(name: String) {
        val bitmap = composeRule.onNodeWithTag("card").captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "account-buttons")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun ownerSeesSignOutAndBothDeletesStacked_en() {
        var deletedFamily = false
        render("en", isOwner = true, onDeleteFamily = { deletedFamily = true })
        composeRule.onNodeWithText("Sign out").assertExists()
        composeRule.onNodeWithText("Delete my account").assertExists()
        composeRule.onNodeWithText("Delete family").performClick()
        assertEquals(true, deletedFamily)
        saveScreenshot("owner_en")
    }

    @Test
    fun coParentSeesNoDeleteFamily_en() {
        render("en", isOwner = false)
        composeRule.onNodeWithText("Delete family").assertDoesNotExist()
        composeRule.onNodeWithText("Delete my account").assertExists()
        saveScreenshot("coparent_en")
    }

    @Test
    fun ownerLayoutInHebrew() {
        render("he", isOwner = true)
        composeRule.onNodeWithText("מחיקת החשבון שלי").assertExists()
        composeRule.onNodeWithText("מחיקת המשפחה").assertExists()
        saveScreenshot("owner_he")
    }

    @Test
    fun coParentLayoutInHebrew() {
        render("he", isOwner = false)
        composeRule.onNodeWithText("מחיקת המשפחה").assertDoesNotExist()
        saveScreenshot("coparent_he")
    }
}
