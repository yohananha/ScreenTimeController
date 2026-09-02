package com.screentime.tv.usage

import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.room.AppDatabase
import com.screentime.shared.room.UsageDao
import com.screentime.shared.room.UsageEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class UsageRecorderTest {

    private val today = LocalDate.of(2026, 9, 2)
    private val netflix = "com.netflix.ninja"

    private val dao = mockk<UsageDao>()
    private val database = mockk<AppDatabase>()
    private val firestore = mockk<FirestoreRepository>()
    private val familyIdProvider = object : FamilyIdProvider {
        override val familyId = MutableStateFlow<String?>("fam-1")
    }

    private lateinit var recorder: UsageRecorder

    @Before
    fun setUp() {
        every { database.usageDao() } returns dao
        coEvery { dao.loadForDate(any()) } returns emptyList()
        coEvery { dao.upsertAll(any()) } just Runs
        coEvery { dao.deleteForDate(any(), any()) } just Runs
        coEvery { firestore.recordUsage(any(), any(), any(), any()) } just Runs
        recorder = UsageRecorder(database, firestore, familyIdProvider)
    }

    @Test
    fun `writes a sample`() = runTest {
        val ok = recorder.record(today, mapOf(netflix to 60_000L))

        assert(ok)
        coVerify(exactly = 1) {
            firestore.recordUsage("fam-1", today, mapOf(netflix to 60_000L), emptySet())
        }
    }

    @Test
    fun `an unchanged sample is not written again`() = runTest {
        val sample = mapOf(netflix to 60_000L)
        recorder.record(today, sample)
        recorder.record(today, sample)
        recorder.record(today, sample)

        // This is what makes a per-minute cadence affordable: an idle TV
        // produces the same map every tick and costs no Firestore write.
        coVerify(exactly = 1) { firestore.recordUsage(any(), any(), any(), any()) }
    }

    @Test
    fun `a changed sample is written`() = runTest {
        recorder.record(today, mapOf(netflix to 60_000L))
        recorder.record(today, mapOf(netflix to 120_000L))

        coVerify(exactly = 1) {
            firestore.recordUsage("fam-1", today, mapOf(netflix to 120_000L), emptySet())
        }
    }

    @Test
    fun `the same sample on a new day is written`() = runTest {
        val sample = mapOf(netflix to 60_000L)
        recorder.record(today, sample)
        recorder.record(today.plusDays(1), sample)

        coVerify(exactly = 2) { firestore.recordUsage(any(), any(), any(), any()) }
    }

    @Test
    fun `a failed upload is retried on the next identical sample`() = runTest {
        val sample = mapOf(netflix to 60_000L)
        coEvery {
            firestore.recordUsage(any(), any(), any(), any())
        } throws IllegalStateException("offline")

        assert(!recorder.record(today, sample))

        coEvery { firestore.recordUsage(any(), any(), any(), any()) } just Runs
        assert(recorder.record(today, sample))

        coVerify(exactly = 2) { firestore.recordUsage(any(), any(), any(), any()) }
    }

    @Test
    fun `packages that no longer count are deleted from the day`() = runTest {
        coEvery { dao.loadForDate(today.toString()) } returns listOf(
            UsageEntity(today.toString(), netflix, 60_000L),
            UsageEntity(today.toString(), "com.google.android.tvlauncher", 900_000L),
        )

        recorder.record(today, mapOf(netflix to 60_000L))

        coVerify {
            dao.deleteForDate(today.toString(), listOf("com.google.android.tvlauncher"))
            firestore.recordUsage(
                "fam-1",
                today,
                mapOf(netflix to 60_000L),
                setOf("com.google.android.tvlauncher"),
            )
        }
    }
}
