package com.screentime.tv.overlay

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.model.TimeRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class RequestControllerTest {

    private val firestore: FirestoreRepository = mockk(relaxed = true)
    private val bonusStore: BonusStore = mockk(relaxed = true)
    private val familyId = MutableStateFlow<String?>("fam-1")
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = this@RequestControllerTest.familyId
    }

    @Test fun `submit returns null when not paired`() = runTest {
        familyId.value = null
        val rc = RequestController(firestore, familyIdProvider, bonusStore)
        assertThat(rc.submit("com.x", 10)).isNull()
        coVerify(exactly = 0) { firestore.createRequest(any(), any(), any()) }
    }

    @Test fun `submit sets status Pending and returns the request id`() = runTest {
        coEvery { firestore.createRequest("fam-1", "com.x", 10) } returns "req-1"
        every { firestore.requestFlow(any(), any()) } returns flowOf(null)

        val rc = RequestController(firestore, familyIdProvider, bonusStore)
        rc.requestStatus.test {
            assertThat(awaitItem()).isNull()
            val id = rc.submit("com.x", 10)
            assertThat(id).isEqualTo("req-1")
            assertThat(awaitItem()).isEqualTo(TimeRequest.Status.Pending)
        }
    }

    @Test fun `approved request applies bonus BEFORE flipping status`() = runTest {
        val now = Instant.now()
        coEvery { firestore.createRequest(any(), any(), any()) } returns "req-1"
        every { firestore.requestFlow("fam-1", "req-1") } returns flowOf(
            TimeRequest(
                "req-1", "com.x", 10,
                status = TimeRequest.Status.Approved,
                approvedMinutes = 12,
                respondedAt = now,
            ),
        )

        val rc = RequestController(firestore, familyIdProvider, bonusStore)
        rc.submit("com.x", 10)

        rc.requestStatus.test {
            // Drain until Approved arrives.
            var status = awaitItem()
            while (status != TimeRequest.Status.Approved) status = awaitItem()
            assertThat(rc.approvedMinutes.value).isEqualTo(12)
            coVerify { bonusStore.addBonus(12 * 60_000L) }
        }
    }

    @Test fun `denied request only flips status, no bonus`() = runTest {
        coEvery { firestore.createRequest(any(), any(), any()) } returns "req-1"
        every { firestore.requestFlow("fam-1", "req-1") } returns flowOf(
            TimeRequest("req-1", "com.x", 10, status = TimeRequest.Status.Denied,
                respondedAt = Instant.now()),
        )

        val rc = RequestController(firestore, familyIdProvider, bonusStore)
        rc.submit("com.x", 10)

        rc.requestStatus.test {
            var status = awaitItem()
            while (status != TimeRequest.Status.Denied) status = awaitItem()
            assertThat(rc.approvedMinutes.value).isNull()
            coVerify(exactly = 0) { bonusStore.addBonus(any()) }
        }
    }

    @Test fun `asking again while a request is pending reuses it instead of sending another`() = runTest {
        coEvery { firestore.createRequest(any(), any(), any()) } returns "req-1"
        every { firestore.requestFlow(any(), any()) } returns flowOf(null)
        val rc = RequestController(firestore, familyIdProvider, bonusStore)
        rc.elapsedRealtime = { 0L }

        assertThat(rc.submit("com.x", 10)).isEqualTo("req-1")
        assertThat(rc.submit("com.x", 30)).isEqualTo("req-1")
        coVerify(exactly = 1) { firestore.createRequest(any(), any(), any()) }
    }

    @Test fun `after an answer, a new request waits out the 60s cooldown`() = runTest {
        coEvery { firestore.createRequest(any(), any(), any()) } returnsMany listOf("req-1", "req-2")
        every { firestore.requestFlow("fam-1", "req-1") } returns flowOf(
            TimeRequest("req-1", "com.x", 10, status = TimeRequest.Status.Denied, respondedAt = Instant.now()),
        )
        every { firestore.requestFlow("fam-1", "req-2") } returns flowOf(null)
        val rc = RequestController(firestore, familyIdProvider, bonusStore)
        var now = 0L
        rc.elapsedRealtime = { now }

        rc.submit("com.x", 10)
        rc.requestStatus.test {
            while (awaitItem() != TimeRequest.Status.Denied) Unit
            cancelAndIgnoreRemainingEvents()
        }
        now = 30_000L
        assertThat(rc.submit("com.x", 10)).isNull()
        now = 61_000L
        assertThat(rc.submit("com.x", 10)).isEqualTo("req-2")
    }
}
