package com.screentime.tv.di

import android.content.Context
import com.screentime.shared.auth.DeviceFamilyIdProvider
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.limits.FirestoreLimitsProvider
import com.screentime.shared.limits.LimitsProvider
import com.screentime.shared.room.AppDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun limitsProvider(impl: FirestoreLimitsProvider): LimitsProvider

    @Binds
    @Singleton
    abstract fun familyIdProvider(impl: DeviceFamilyIdProvider): FamilyIdProvider

    companion object {
        @Provides
        @Singleton
        fun appDatabase(@ApplicationContext context: Context): AppDatabase =
            AppDatabase.get(context)
    }
}
