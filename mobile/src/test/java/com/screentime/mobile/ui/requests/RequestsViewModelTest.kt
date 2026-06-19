package com.screentime.mobile.ui.requests

import com.google.common.truth.Truth.assertThat
import com.screentime.mobile.MainCoroutineRule
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.TimeRequest
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModelTest {

    @get:Rule val main = MainCoroutineRule()

    private val firestore: FirestoreRepository = mockk(relaxed = true)
    private val familyId = MutableStateFlow<String?>("fam-1")
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = this@RequestsViewModelTest.familyId
    }

    @Test fun `approve calls respondToRequest with requested minutes by default`() = runTest(main.dispatcher) {
        coEvery { firestore.recentRequestsFlow("fam-1") } returns flowOf(emptyList())
        val vm = RequestsViewModel(firestore, familyIdProvider)
        val req = TimeRequest("r1", "com.x", 20)
        vm.approve(req)
        advanceUntilIdle()
        coVerify { firestore.respondToRequest("fam-1", "r1", 20) }
    }

    @Test fun `approve with override minutes uses the override`() = runTest(main.dispatcher) {
        coEvery { firestore.recentRequestsFlow("fam-1") } returns flowOf(emptyList())
        val vm = RequestsViewModel(firestore, familyIdProvider)
        vm.approve(TimeRequest("r1", "com.x", 20), minutes = 5)
        advanceUntilIdle()
        coVerify { firestore.respondToRequest("fam-1", "r1", 5) }
    }

    @Test fun `deny calls respondToRequest with null minutes`() = runTest(main.dispatcher) {
        coEvery { firestore.recentRequestsFlow("fam-1") } returns flowOf(emptyList())
        val vm = RequestsViewModel(firestore, familyIdProvider)
        vm.deny(TimeRequest("r1", "com.x", 20))
        advanceUntilIdle()
        coVerify { firestore.respondToRequest("fam-1", "r1", null) }
    }

    @Test fun `actions are no-ops without a family id`() = runTest(main.dispatcher) {
        familyId.value = null
        coEvery { firestore.recentRequestsFlow(any()) } returns flowOf(emptyList())
        val vm = RequestsViewModel(firestore, familyIdProvider)
        vm.approve(TimeRequest("r1", "com.x", 20))
        vm.deny(TimeRequest("r1", "com.x", 20))
        advanceUntilIdle()
        coVerify(exactly = 0) { firestore.respondToRequest(any(), any(), any()) }
    }
}

class TimeRequestTest {
    @Test fun `pending request is not an active grant`() {
        assertThat(TimeRequest("a", "p", 10).isActiveGrant()).isFalse()
    }

    @Test fun `approved grant within window is active`() {
        val req = TimeRequest(
            "a", "p", 10,
            status = TimeRequest.Status.Approved,
            approvedMinutes = 10,
            respondedAt = Instant.now().minusSeconds(60),
        )
        assertThat(req.isActiveGrant()).isTrue()
    }

    @Test fun `approved grant past expiry is no longer active`() {
        val req = TimeRequest(
            "a", "p", 10,
            status = TimeRequest.Status.Approved,
            approvedMinutes = 1,
            respondedAt = Instant.now().minusSeconds(120),
        )
        assertThat(req.isActiveGrant()).isFalse()
    }

    @Test fun `denied grant is never active`() {
        val req = TimeRequest(
            "a", "p", 10,
            status = TimeRequest.Status.Denied,
            respondedAt = Instant.now(),
        )
        assertThat(req.isActiveGrant()).isFalse()
    }

    @Test fun `approvedMinutes overrides requestedMinutes when computing expiry`() {
        val req = TimeRequest(
            "a", "p", 30,
            status = TimeRequest.Status.Approved,
            approvedMinutes = 5,
            respondedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        assertThat(req.grantExpiresAt()).isEqualTo(Instant.parse("2026-01-01T00:05:00Z"))
    }
}
