package com.screentime.tv.overlay

import com.google.common.truth.Truth.assertThat
import com.google.firebase.functions.FirebaseFunctionsException
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CodeRedeemerTest {

    private val firestore: FirestoreRepository = mockk(relaxed = true)
    private val bonusStore: BonusStore = mockk(relaxed = true)
    private val familyId = MutableStateFlow<String?>("fam-1")
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = this@CodeRedeemerTest.familyId
    }

    private fun newRedeemer(): CodeRedeemer {
        every { firestore.lockoutFlow(any()) } returns flowOf(LockoutSettings())
        return CodeRedeemer(firestore, familyIdProvider, bonusStore)
    }

    @Test fun `redeem returns true and adds bonus on success`() = runTest {
        coEvery { firestore.redeemCode("fam-1", "111111") } returns 30
        val r = newRedeemer()
        val ok = r.redeem("111111", currentlyBlocked = "com.x")
        assertThat(ok).isTrue()
        coVerify { bonusStore.addBonus("com.x", 30 * 60_000L) }
    }

    @Test fun `redeem without currentlyBlocked does not call bonusStore`() = runTest {
        coEvery { firestore.redeemCode(any(), any()) } returns 10
        val r = newRedeemer()
        val ok = r.redeem("111111", currentlyBlocked = null)
        assertThat(ok).isTrue()
        coVerify(exactly = 0) { bonusStore.addBonus(any(), any()) }
    }

    @Test fun `redeem returns false when server throws FirebaseFunctionsException`() = runTest {
        val ex = mockk<FirebaseFunctionsException>(relaxed = true)
        every { ex.code } returns FirebaseFunctionsException.Code.NOT_FOUND
        coEvery { firestore.redeemCode(any(), any()) } throws ex
        val r = newRedeemer()
        assertThat(r.redeem("999999", null)).isFalse()
    }

    @Test fun `redeem returns false when no family id available`() = runTest {
        familyId.value = null
        val r = newRedeemer()
        assertThat(r.redeem("111111", null)).isFalse()
        coVerify(exactly = 0) { firestore.redeemCode(any(), any()) }
    }

    @Test fun `clearExpiredLockout no-ops when not locked`() = runTest {
        every { firestore.lockoutFlow(any()) } returns
            flowOf(LockoutSettings(locked = false))
        val r = CodeRedeemer(firestore, familyIdProvider, bonusStore)
        r.clearExpiredLockout()
        coVerify(exactly = 0) { firestore.clearLockout(any()) }
    }

    @Test fun `clearExpiredLockout no-ops in PARENT_UNLOCK mode`() = runTest {
        every { firestore.lockoutFlow(any()) } returns
            flowOf(LockoutSettings(locked = true, mode = LockoutMode.PARENT_UNLOCK))
        val r = CodeRedeemer(firestore, familyIdProvider, bonusStore)
        r.clearExpiredLockout()
        coVerify(exactly = 0) { firestore.clearLockout(any()) }
    }

    @Test fun `clearExpiredLockout clears in TIMER mode when locked`() = runTest {
        every { firestore.lockoutFlow(any()) } returns
            flowOf(LockoutSettings(locked = true, mode = LockoutMode.TIMER))
        val r = CodeRedeemer(firestore, familyIdProvider, bonusStore)
        var attempts = 0
        while (!r.lockout.value.locked && attempts < 50) {
            kotlinx.coroutines.delay(10)
            attempts++
        }
        r.clearExpiredLockout()
        coVerify { firestore.clearLockout("fam-1") }
    }
}
