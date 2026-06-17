package com.screentime.tv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.screentime.tv.usage.DailyResetWorker
import com.screentime.tv.usage.UsageWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ScreenTimeTvApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // WorkManager's minimum periodic interval is 15 min. Real-time
        // enforcement runs in the AccessibilityService.
        UsageWorker.schedule(this, intervalMinutes = 15L)
        DailyResetWorker.schedule(this)
    }
}
