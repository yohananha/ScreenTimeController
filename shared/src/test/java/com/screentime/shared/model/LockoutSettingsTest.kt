package com.screentime.shared.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LockoutSettingsTest {

    @Test fun `defaults match documented values`() {
        val s = LockoutSettings()
        assertThat(s.durationMinutes).isEqualTo(15)
        assertThat(s.mode).isEqualTo(LockoutMode.TIMER)
        assertThat(s.locked).isFalse()
        assertThat(s.lockedUntil).isNull()
    }

    @Test fun `companion constants are in sync with functions index_ts`() {
        // Mirror values in functions/src/index.ts. If any of these change, the
        // server-side enforcement must be updated in lockstep — this test is the
        // canary that protects that contract.
        assertThat(LockoutSettings.DEFAULT_DURATION_MINUTES).isEqualTo(15)
        assertThat(LockoutSettings.MAX_ATTEMPTS).isEqualTo(5)
        assertThat(LockoutSettings.ATTEMPT_WINDOW_SECONDS).isEqualTo(60L)
    }

    @Test fun `copy preserves enum mode and locked state`() {
        val s = LockoutSettings(mode = LockoutMode.PARENT_UNLOCK, locked = true)
        assertThat(s.copy(durationMinutes = 30).mode).isEqualTo(LockoutMode.PARENT_UNLOCK)
        assertThat(s.copy(durationMinutes = 30).locked).isTrue()
    }
}
