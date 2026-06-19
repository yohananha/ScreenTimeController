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
            coVerify { bonusStore.addBonus("com.x", 12 * 60_000L) }
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
            coVerify(exactly = 0) { bonusStore.addBonus(any(), any()) }
        }
    }
}
