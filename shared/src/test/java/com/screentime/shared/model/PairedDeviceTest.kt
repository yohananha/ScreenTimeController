package com.screentime.shared.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class PairedDeviceTest {
    private val now = Instant.parse("2026-10-07T18:00:00Z")

    @Test fun `heartbeat status from lastSeen`() {
        assertThat(PairedDevice("tv", "TV", null).heartbeatAt(now)).isEqualTo(PairedDevice.Heartbeat.NEVER_SEEN)
        assertThat(PairedDevice("tv", "TV", now.minusSeconds(4 * 60)).heartbeatAt(now))
            .isEqualTo(PairedDevice.Heartbeat.ACTIVE)
        assertThat(PairedDevice("tv", "TV", now.minusSeconds(15 * 60)).heartbeatAt(now))
            .isEqualTo(PairedDevice.Heartbeat.ACTIVE)
        assertThat(PairedDevice("tv", "TV", now.minusSeconds(16 * 60)).heartbeatAt(now))
            .isEqualTo(PairedDevice.Heartbeat.NOT_RESPONDING)
    }
}
