package com.screentime.shared.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeFrameScheduleTest {

    @Test fun `ALL_DAY spans the full day`() {
        assertThat(TimeFrameWindow.ALL_DAY).isEqualTo(TimeFrameWindow(startMinute = 0, endMinute = 1440))
    }

    @Test fun `overlaps detects overlapping windows`() {
        val a = TimeFrameWindow(startMinute = 480, endMinute = 600) // 8:00-10:00
        val b = TimeFrameWindow(startMinute = 540, endMinute = 660) // 9:00-11:00
        assertThat(a.overlaps(b)).isTrue()
    }

    @Test fun `overlaps treats a window fully containing another as overlapping`() {
        assertThat(TimeFrameWindow.ALL_DAY.overlaps(TimeFrameWindow(startMinute = 480, endMinute = 600))).isTrue()
    }

    @Test fun `overlaps does not flag adjacent windows`() {
        val a = TimeFrameWindow(startMinute = 480, endMinute = 600)
        val b = TimeFrameWindow(startMinute = 600, endMinute = 660)
        assertThat(a.overlaps(b)).isFalse()
    }

    @Test fun `overlaps does not flag disjoint windows`() {
        val a = TimeFrameWindow(startMinute = 480, endMinute = 600)
        val b = TimeFrameWindow(startMinute = 700, endMinute = 800)
        assertThat(a.overlaps(b)).isFalse()
    }
}
