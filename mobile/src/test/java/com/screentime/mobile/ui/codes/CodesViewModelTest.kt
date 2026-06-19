package com.screentime.mobile.ui.codes

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.screentime.mobile.MainCoroutineRule
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.OneTimeCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CodesViewModelTest {

    @get:Rule val main = MainCoroutineRule()

    private val firestore: FirestoreRepository = mockk()
    private val familyId = MutableStateFlow<String?>("fam-1")
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = this@CodesViewModelTest.familyId
    }

    @Test fun `generate emits isGenerating then active code on success`() = runTest(main.dispatcher) {
        val code = OneTimeCode("123456", 30, Instant.now().plusSeconds(3600))
        coEvery { firestore.createCode("fam-1", 30) } returns code

        val vm = CodesViewModel(firestore, familyIdProvider)
        vm.state.test {
            assertThat(awaitItem()).isEqualTo(CodesUiState())  // initial
            vm.generate(30)
            assertThat(awaitItem().isGenerating).isTrue()
            advanceUntilIdle()
            val final = awaitItem()
            assertThat(final.active).isEqualTo(code)
            assertThat(final.isGenerating).isFalse()
            assertThat(final.error).isNull()
        }
    }

    @Test fun `generate exposes error message on failure`() = runTest(main.dispatcher) {
        coEvery { firestore.createCode(any(), any()) } throws RuntimeException("boom")
        val vm = CodesViewModel(firestore, familyIdProvider)
        vm.generate(15)
        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo("boom")
        assertThat(vm.state.value.active).isNull()
    }

    @Test fun `generate without familyId reports error and does not call firestore`() = runTest(main.dispatcher) {
        familyId.value = null
        val vm = CodesViewModel(firestore, familyIdProvider)
        vm.generate(15)
        advanceUntilIdle()
        assertThat(vm.state.value.error).contains("No family")
    }

    @Test fun `dismiss resets the state`() = runTest(main.dispatcher) {
        coEvery { firestore.createCode(any(), any()) } returns
            OneTimeCode("000000", 5, Instant.now().plusSeconds(60))
        val vm = CodesViewModel(firestore, familyIdProvider)
        vm.generate(5)
        advanceUntilIdle()
        vm.dismiss()
        assertThat(vm.state.value).isEqualTo(CodesUiState())
    }
}
