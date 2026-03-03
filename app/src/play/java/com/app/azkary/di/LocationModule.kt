package com.app.azkary.di

import com.app.azkary.data.location.LocationProvider
import com.app.azkary.data.location.PlayLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Play flavor DI module - provides Google Play Services implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        impl: PlayLocationProvider
    ): LocationProvider
}
