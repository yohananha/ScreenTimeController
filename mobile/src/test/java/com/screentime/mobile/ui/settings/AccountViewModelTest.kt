package com.screentime.mobile.ui.settings

import com.google.common.truth.Truth.assertThat
import com.screentime.mobile.MainCoroutineRule
import com.screentime.mobile.fcm.PushTokenRegistrar
import com.screentime.shared.R as SharedR
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.Family
import com.screentime.shared.model.FamilyRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule val main = MainCoroutineRule()

    private val auth: AuthRepository = mockk {
        every { currentUid } returns "uid-1"
        every { signOut() } just runs
    }
    private val firestore: FirestoreRepository = mockk(relaxed = true)
    private val registrar: PushTokenRegistrar = mockk { coEvery { unregister(any()) } just runs }
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = MutableStateFlow<String?>("fam-1")
    }

    private fun vm() = AccountViewModel(auth, firestore, registrar, familyIdProvider)

    @Test fun `deleteAccount unregisters push, deletes server-side, then signs out`() = runTest(main.dispatcher) {
        vm().deleteAccount()
        advanceUntilIdle()
        coVerifyOrder {
            registrar.unregister("uid-1")
            firestore.deleteAccount()
            auth.signOut()
        }
    }

    @Test fun `a failed server delete keeps the user signed in and shows an error`() = runTest(main.dispatcher) {
        coEvery { firestore.deleteAccount() } throws RuntimeException("offline")
        val vm = vm()
        vm.deleteAccount()
        advanceUntilIdle()
        verify(exactly = 0) { auth.signOut() }
        assertThat(vm.state.value.busy).isFalse()
        assertThat(vm.state.value.error).isEqualTo(SharedR.string.error_generic)
    }

    @Test fun `deleteFamily deletes the current family`() = runTest(main.dispatcher) {
        vm().deleteFamily()
        advanceUntilIdle()
        coVerify { firestore.deleteFamily("fam-1") }
    }

    @Test fun `isOwner reflects the family's ownerUid`() = runTest(main.dispatcher) {
        every { firestore.familyFlow("fam-1") } returns
            flowOf(Family("fam-1", "uid-1", mapOf("uid-1" to FamilyRole.ADMIN), emptyList()))
        val vm = vm()
        val job = launch { vm.isOwner.collect {} }
        advanceUntilIdle()
        assertThat(vm.isOwner.value).isTrue()
        job.cancel()
    }

    @Test fun `signOut unregisters the push token while still signed in, then signs out`() = runTest(main.dispatcher) {
        vm().signOut()
        advanceUntilIdle()
        coVerifyOrder {
            registrar.unregister("uid-1")
            auth.signOut()
        }
    }

    @Test fun `signOut with no session just signs out`() = runTest(main.dispatcher) {
        every { auth.currentUid } returns null
        vm().signOut()
        advanceUntilIdle()
        coVerify(exactly = 0) { registrar.unregister(any()) }
        verify { auth.signOut() }
    }
}
