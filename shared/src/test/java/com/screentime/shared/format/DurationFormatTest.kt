package com.screentime.shared.format

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.model.Limits
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Assertions use `.contains(...)` rather than `.isEqualTo(...)` wherever the
 * value passes through [String.bidiWrap] — BidiFormatter may insert
 * invisible FSI/PDI isolation marks around the text depending on the
 * surrounding/default directionality, and the test shouldn't be sensitive to
 * exactly when that happens.
 */
@RunWith(RobolectricTestRunner::class)
class DurationFormatTest {

    private val format = DurationFormat()
    private val resources: Resources
        get() = ApplicationProvider.getApplicationContext<Context>().resources
    private val hebrewResources: Resources
        get() {
            val base = ApplicationProvider.getApplicationContext<Context>()
            val config = Configuration(base.resources.configuration).apply { setLocale(Locale("he")) }
            return base.createConfigurationContext(config).resources
        }

    @Test
    fun `minutes formats hours and minutes together`() {
        assertThat(format.minutes(resources, 90)).contains("1h")
        assertThat(format.minutes(resources, 90)).contains("30m")
    }

    @Test
    fun `minutes formats whole hours without a minutes component`() {
        assertThat(format.minutes(resources, 120)).contains("2h")
        assertThat(format.minutes(resources, 120)).doesNotContain("0m")
    }

    @Test
    fun `minutes formats under an hour as minutes only`() {
        assertThat(format.minutes(resources, 45)).contains("45m")
    }

    @Test
    fun `minutes renders UNLIMITED as the no-limit label`() {
        assertThat(format.minutes(resources, Limits.UNLIMITED)).contains("No limit")
    }

    @Test
    fun `minutes renders zero and negative values as 0m`() {
        assertThat(format.minutes(resources, 0)).contains("0m")
        assertThat(format.minutes(resources, -5)).contains("0m")
    }

    // Earlier note here blamed a "Robolectric + AGP resource-qualifier-switching
    // limitation" and pointed to tv's BlockOverlayContentTest as proof Hebrew
    // rendering worked on a real device. Both were wrong: the actual bug was
    // that Android's runtime locale matching for Hebrew keys off the legacy
    // ISO-639 code "iw", not the modern "he" — a resource folder named
    // values-he/ is silently invisible to it (confirmed with a real
    // createConfigurationContext(), not just Robolectric's qualifier switch).
    // The TV test "passed" only because it derived its expected string via
    // the exact same broken getString(..., locale=he) call it was meant to
    // exercise, so both sides silently fell back to English. Fixed by
    // renaming every values-he/ directory to values-iw/ (mobile, tv, shared)
    // and switching that TV test to a hardcoded literal. See git history for
    // the diagnostic trail (Robolectric qualifiers="he" reproduced it too).

    @Test
    fun `minutes formats hours and minutes in Hebrew`() {
        assertThat(format.minutes(hebrewResources, 90)).contains("1 שע׳")
        assertThat(format.minutes(hebrewResources, 90)).contains("30 דק׳")
    }

    @Test
    fun `quantityMinutes uses the Hebrew CLDR two-bucket`() {
        assertThat(format.quantityMinutes(hebrewResources, 1)).contains("דקה אחת")
        assertThat(format.quantityMinutes(hebrewResources, 2)).contains("שתי דקות")
        assertThat(format.quantityMinutes(hebrewResources, 5)).contains("5 דקות")
    }

    @Test
    fun `countdown pads seconds to two digits`() {
        assertThat(format.countdown(resources, 65)).contains("1:05")
    }

    @Test
    fun `countdown clamps negative durations to zero`() {
        assertThat(format.countdown(resources, -10)).contains("0:00")
    }

    @Test
    fun `quantityMinutes uses English CLDR plural rules`() {
        assertThat(format.quantityMinutes(resources, 1)).contains("1 minute")
        assertThat(format.quantityMinutes(resources, 5)).contains("5 minutes")
    }
}
