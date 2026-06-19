package com.screentime.shared.limits

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.room.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests run against an unencrypted in-memory Room build to side-step SQLCipher's
 * native library, which isn't loadable inside the JVM-only test runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class BonusStoreTest {

    private lateinit var db: AppDatabase
    private lateinit var store: BonusStore

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = BonusStore(db)
    }

    @After fun tearDown() = db.close()

    @Test fun `addBonus extends expiry from now`() = runTest {
        store.addBonus("com.x", 60_000L)
        // Give the launched coroutine a moment to settle the in-memory state.
        delay(50)
        assertThat(store.isActive("com.x")).isTrue()
        val expiry = store.expiryFor("com.x")!!
        assertThat(expiry.toEpochMilli()).isGreaterThan(System.currentTimeMillis())
    }

    @Test fun `second addBonus stacks on top of the existing expiry`() = runTest {
        store.addBonus("com.x", 60_000L)
        delay(20)
        val first = store.expiryFor("com.x")!!
        store.addBonus("com.x", 60_000L)
        delay(20)
        val second = store.expiryFor("com.x")!!
        assertThat(second.toEpochMilli()).isAtLeast(first.toEpochMilli() + 60_000L - 100)
    }

    @Test fun `isActive returns false for unknown package`() {
        assertThat(store.isActive("com.unknown")).isFalse()
    }

    @Test fun `clear empties the in-memory map`() = runTest {
        store.addBonus("com.x", 60_000L)
        delay(20)
        store.clear()
        assertThat(store.isActive("com.x")).isFalse()
        assertThat(store.expiryFor("com.x")).isNull()
    }
}
