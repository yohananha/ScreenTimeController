package com.screentime.tv.usage

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.limits.BonusStore
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure-JVM test (no Robolectric) — DailyResetWorker.runIfOverdue only touches
 * a SharedPreferences instance, so we can hand it a fake Context that returns
 * an in-memory SharedPreferences. Sidesteps Robolectric's Android keystore
 * provider initialisation (which fails on the CI JVM).
 */
class DailyResetWorkerTest {

    private lateinit var ctx: Context
    private lateinit var prefs: SharedPreferences
    private val store = mutableMapOf<String, String?>()

    @Before fun setUp() {
        store.clear()
        prefs = mockk(relaxed = true)
        val editor: SharedPreferences.Editor = mockk(relaxed = true)
        val keySlot = slot<String>()
        val valSlot = slot<String?>()

        every { prefs.getString(capture(keySlot), any()) } answers {
            store[keySlot.captured] ?: secondArg<String?>()
        }
        every { editor.putString(capture(keySlot), captureNullable(valSlot)) } answers {
            store[keySlot.captured] = valSlot.captured
            editor
        }
        every { editor.apply() } returns Unit
        every { prefs.edit() } returns editor

        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx
        every { ctx.getSharedPreferences(any(), any()) } returns prefs
    }

    @Test fun `clears the bonus store on first run`() {
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify { bonus.clear() }
        assertThat(store["last_reset_date"])
            .isEqualTo(LocalDate.now(ZoneId.systemDefault()).toString())
    }

    @Test fun `no-op when already run today`() {
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        clearMocks(bonus)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify(exactly = 0) { bonus.clear() }
    }

    @Test fun `re-runs when the stamped date is before today`() {
        store["last_reset_date"] =
            LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString()
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify { bonus.clear() }
        assertThat(store["last_reset_date"])
            .isEqualTo(LocalDate.now(ZoneId.systemDefault()).toString())
    }

    @Test fun `tolerates a corrupt stamp`() {
        store["last_reset_date"] = "not-a-date"
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify { bonus.clear() }
    }
}
