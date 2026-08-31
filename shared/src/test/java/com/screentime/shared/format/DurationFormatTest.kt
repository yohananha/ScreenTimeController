package com.screentime.shared.format

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.model.Limits
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

    // Hebrew-content assertions for this class are NOT covered here. Confirmed
    // against a real build (not just read) that neither
    // @Config(qualifiers = "he") nor RuntimeEnvironment.setQualifiers("he")
    // switches this :shared library module's test target onto
    // src/main/res/values-he — both return the English string unchanged,
    // even though the merged values-he.xml is present and correct in
    // shared/build/intermediates/.../mergeDebugUnitTestResources/. This is a
    // Robolectric 4.13 + AGP 9.2.1 resource-qualifier-switching limitation
    // specific to library-module test targets, not a bug in DurationFormat.
    // Real Hebrew rendering is exercised on an actual Android runtime by
    // tv's BlockOverlayContentTest.mainView_hebrewLocale_showsHebrewWrapUpMessage,
    // which drives the same values-he resources through a real
    // createConfigurationContext() instead of Robolectric's qualifier switch.

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

    // The Hebrew "two" CLDR bucket (שתי דקות vs the "other" %d דקות form) is
    // exercised the same way — see the note above `countdown pads seconds`.
}
