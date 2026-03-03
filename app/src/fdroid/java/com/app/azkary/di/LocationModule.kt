package com.app.azkary.di

import com.app.azkary.data.location.FdroidLocationProvider
import com.app.azkary.data.location.LocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * F-Droid flavor DI module - provides FOSS implementations.
 * Uses Android's standard LocationManager instead of Google Play Services.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        impl: FdroidLocationProvider
    ): LocationProvider
}
