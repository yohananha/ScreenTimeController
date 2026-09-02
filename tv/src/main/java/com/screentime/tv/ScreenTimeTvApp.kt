package com.screentime.tv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.screentime.shared.limits.BonusStore
import com.screentime.tv.locale.TvLocaleController
import com.screentime.tv.usage.CountablePackages
import com.screentime.tv.usage.DailyResetWorker
import com.screentime.tv.usage.PackageChangeReceiver
import com.screentime.tv.usage.UsageWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ScreenTimeTvApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var bonusStore: BonusStore

    // Field injection alone is enough to force this @Singleton to be
    // constructed at process start, which is what starts its Firestore
    // collector (see TvLocaleController's init{} block) — nothing here
    // needs to call it directly.
    @Inject lateinit var localeController: TvLocaleController

    @Inject lateinit var countablePackages: CountablePackages

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Keeps the set of apps that count as screen time fresh when the
        // child (or the parent) installs something new.
        PackageChangeReceiver(countablePackages).register(this)
        // WorkManager's minimum periodic interval is 15 min. Real-time
        // enforcement runs in the AccessibilityService.
        UsageWorker.schedule(this, intervalMinutes = 15L)
        DailyResetWorker.schedule(this)
        // If WorkManager missed yesterday's midnight (device off, app killed,
        // etc.) catch up immediately so bonus minutes don't survive into a new
        // day.
        DailyResetWorker.runIfOverdue(this, bonusStore)
    }
}
