package com.screentime.tv.di

import com.screentime.shared.auth.DeviceFamilyIdProvider
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.limits.FirestoreLimitsProvider
import com.screentime.shared.limits.LimitsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
}
