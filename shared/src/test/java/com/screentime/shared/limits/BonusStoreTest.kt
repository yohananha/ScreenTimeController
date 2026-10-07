package com.screentime.shared.limits

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.room.AppDatabase
import com.screentime.shared.time.TrustedClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/**
 * Tests run against an unencrypted in-memory Room build to side-step SQLCipher's
 * native library, which isn't loadable inside the JVM-only test runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class BonusStoreTest {

    private lateinit var db: AppDatabase
    private lateinit var store: BonusStore

    /** Trusted time the test controls; the device wall clock plays no part. */
    private val clock = object : TrustedClock {
        var now: Instant = Instant.now()
        override fun now(): Instant = now
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = BonusStore(db, clock)
    }

    @After fun tearDown() = db.close()

    @Test fun `addBonus extends expiry from now`() = runTest {
        store.addBonus(60_000L)
        // Give the launched coroutine a moment to settle the in-memory state.
        delay(50)
        assertThat(store.isActive()).isTrue()
        val expiry = store.expiryFor()!!
        assertThat(expiry.toEpochMilli()).isGreaterThan(System.currentTimeMillis())
    }

    @Test fun `second addBonus stacks on top of the existing expiry`() = runTest {
        store.addBonus(60_000L)
        delay(20)
        val first = store.expiryFor()!!
        store.addBonus(60_000L)
        delay(20)
        val second = store.expiryFor()!!
        assertThat(second.toEpochMilli()).isAtLeast(first.toEpochMilli() + 60_000L - 100)
    }

    @Test fun `isActive returns false with no bonus granted`() {
        assertThat(store.isActive()).isFalse()
    }

    @Test fun `clear empties the in-memory state`() = runTest {
        store.addBonus(60_000L)
        delay(20)
        store.clear()
        assertThat(store.isActive()).isFalse()
        assertThat(store.expiryFor()).isNull()
    }

    @Test fun `expiry is measured on trusted time, not the device clock`() = runTest {
        store.addBonus(60_000L)
        delay(20)
        assertThat(store.isActive()).isTrue()
        // Trusted time moves on 61s — whatever the TV's own clock says.
        clock.now = clock.now.plusSeconds(61)
        assertThat(store.isActive()).isFalse()
    }
}
