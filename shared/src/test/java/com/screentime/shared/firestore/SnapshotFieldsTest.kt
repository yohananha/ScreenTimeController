package com.screentime.shared.firestore

import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.Instant

/**
 * The real DocumentSnapshot getters throw on a type mismatch, which inside a
 * snapshot listener crashed the TV. These readers must never throw.
 */
class SnapshotFieldsTest {

    private fun snapWith(value: Any?): DocumentSnapshot {
        val snap = mockk<DocumentSnapshot>()
        every { snap.get("f") } returns value
        return snap
    }

    @Test fun `numbers are read whether stored as Long or Double`() {
        assertThat(snapWith(45L).intOrNull("f")).isEqualTo(45)
        assertThat(snapWith(45.0).intOrNull("f")).isEqualTo(45)
        assertThat(snapWith(45L).longOrNull("f")).isEqualTo(45L)
    }

    @Test fun `out-of-range longs clamp instead of overflowing`() {
        assertThat(snapWith(Long.MAX_VALUE).intOrNull("f")).isEqualTo(Int.MAX_VALUE)
    }

    @Test fun `wrong types read as null instead of throwing`() {
        assertThat(snapWith("60").intOrNull("f")).isNull()
        assertThat(snapWith("yes").boolOrNull("f")).isNull()
        assertThat(snapWith(42L).stringOrNull("f")).isNull()
        assertThat(snapWith("2026-10-07").instantOrNull("f")).isNull()
        assertThat(snapWith(mapOf("a" to 1)).intOrNull("f")).isNull()
    }

    @Test fun `missing fields read as null`() {
        assertThat(snapWith(null).intOrNull("f")).isNull()
        assertThat(snapWith(null).boolOrNull("f")).isNull()
    }

    @Test fun `well-typed values pass through`() {
        val instant = Instant.parse("2026-10-07T12:00:00Z")
        assertThat(snapWith(true).boolOrNull("f")).isTrue()
        assertThat(snapWith("he").stringOrNull("f")).isEqualTo("he")
        assertThat(snapWith(Timestamp(instant.epochSecond, 0)).instantOrNull("f")).isEqualTo(instant)
    }
}
