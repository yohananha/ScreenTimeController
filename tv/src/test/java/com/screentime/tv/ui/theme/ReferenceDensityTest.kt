package com.screentime.tv.ui.theme

import androidx.compose.ui.unit.Constraints
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReferenceDensityTest {

    @Test
    fun `1080p maps one reference px to one device px`() {
        assertThat(referenceDensity(Constraints.fixed(1920, 1080), fallback = 2f)).isEqualTo(1f)
    }

    @Test
    fun `4K doubles the reference canvas`() {
        assertThat(referenceDensity(Constraints.fixed(3840, 2160), fallback = 4f)).isEqualTo(2f)
    }

    @Test
    fun `720p shrinks the reference canvas`() {
        assertThat(referenceDensity(Constraints.fixed(1280, 720), fallback = 1f)).isWithin(1e-4f).of(2f / 3f)
    }

    @Test
    fun `non 16x9 window fits the limiting side`() {
        // Shorter than 16:9 — height is the limit, so the 1080-tall canvas still fits.
        assertThat(referenceDensity(Constraints.fixed(1920, 900), fallback = 2f)).isWithin(1e-4f).of(900f / 1080f)
    }

    @Test
    fun `unbounded constraints fall back to the system density`() {
        assertThat(referenceDensity(Constraints(), fallback = 2f)).isEqualTo(2f)
    }
}
