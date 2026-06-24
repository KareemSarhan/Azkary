package com.app.azkary.di

import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.data.repository.SelfHostedLocationRepository
import com.app.azkary.domain.AppRatingManager
import com.app.azkary.domain.SelfHostedAppRatingManager
import com.app.azkary.util.AppUpdateManagerFactory
import com.app.azkary.util.SelfHostedAppUpdateManagerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorModule {
    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: SelfHostedLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAppRatingManager(impl: SelfHostedAppRatingManager): AppRatingManager

    @Binds
    @Singleton
    abstract fun bindAppUpdateManagerFactory(
        impl: SelfHostedAppUpdateManagerFactory
    ): AppUpdateManagerFactory
}
