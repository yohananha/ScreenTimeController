package com.screentime.mobile.di

import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.auth.FirestoreFamilyIdProvider
import com.screentime.shared.time.SystemTrustedClock
import com.screentime.shared.time.TrustedClock
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
    abstract fun familyIdProvider(impl: FirestoreFamilyIdProvider): FamilyIdProvider

    /** The phone isn't the device being restricted — its own clock is fine. */
    @Binds
    @Singleton
    abstract fun trustedClock(impl: SystemTrustedClock): TrustedClock
}
