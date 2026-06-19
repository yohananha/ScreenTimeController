package com.screentime.tv.usage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.screentime.shared.limits.BonusStore
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DailyResetWorkerTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test fun `runIfOverdue clears the bonus store on first run`() {
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify { bonus.clear() }
    }

    @Test fun `runIfOverdue is a no-op when already run today`() {
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        io.mockk.clearMocks(bonus)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify(exactly = 0) { bonus.clear() }
    }

    @Test fun `runIfOverdue re-runs when the stamped date is before today`() {
        val prefs = ctx.getSharedPreferences("daily_reset", Context.MODE_PRIVATE)
        prefs.edit().putString(
            "last_reset_date",
            LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString(),
        ).apply()

        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify { bonus.clear() }
        assertThat(prefs.getString("last_reset_date", null))
            .isEqualTo(LocalDate.now(ZoneId.systemDefault()).toString())
    }

    @Test fun `runIfOverdue tolerates a corrupt stamp`() {
        ctx.getSharedPreferences("daily_reset", Context.MODE_PRIVATE)
            .edit().putString("last_reset_date", "not-a-date").apply()
        val bonus: BonusStore = mockk(relaxed = true)
        DailyResetWorker.runIfOverdue(ctx, bonus)
        verify { bonus.clear() }
    }
}
