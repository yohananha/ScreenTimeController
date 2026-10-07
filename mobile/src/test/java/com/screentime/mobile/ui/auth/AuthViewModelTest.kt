package com.screentime.mobile.ui.auth

import com.screentime.mobile.MainCoroutineRule
import com.screentime.mobile.fcm.PushTokenRegistrar
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule val main = MainCoroutineRule()

    private val session = MutableStateFlow<AuthRepository.Session?>(null)
    private val auth: AuthRepository = mockk {
        every { currentSession } returns session
    }
    private val familyId = MutableStateFlow<String?>(null)
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = this@AuthViewModelTest.familyId
    }
    private val firestore: FirestoreRepository = mockk(relaxed = true)
    private val registrar: PushTokenRegistrar = mockk {
        coEvery { registerCurrentToken(any()) } just runs
        coEvery { unregister(any()) } just runs
    }

    @Test fun `registers the push token for each newly signed-in account`() = runTest(main.dispatcher) {
        AuthViewModel(auth, familyIdProvider, registrar, firestore)
        session.value = AuthRepository.Session("uid-1", null)
        advanceUntilIdle()
        session.value = AuthRepository.Session("uid-2", null)
        advanceUntilIdle()

        coVerify(exactly = 1) { registrar.registerCurrentToken("uid-1") }
        coVerify(exactly = 1) { registrar.registerCurrentToken("uid-2") }
    }


    @Test fun `seeds the family time zone from this phone once a family is known`() = runTest(main.dispatcher) {
        AuthViewModel(auth, familyIdProvider, registrar, firestore)
        familyId.value = "fam-1"
        advanceUntilIdle()
        coVerify { firestore.ensureFamilyTimezone("fam-1", java.time.ZoneId.systemDefault().id) }
    }
}
