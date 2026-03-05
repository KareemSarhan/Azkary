package com.app.azkary.di

import com.app.azkary.data.repository.FdroidLocationRepository
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.domain.AppRatingManager
import com.app.azkary.domain.NoOpAppRatingManager
import com.app.azkary.util.AppUpdateManagerFactory
import com.app.azkary.util.FdroidAppUpdateManagerFactory
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
    abstract fun bindLocationRepository(impl: FdroidLocationRepository): LocationRepository
    
    @Binds
    @Singleton
    abstract fun bindAppRatingManager(impl: NoOpAppRatingManager): AppRatingManager
    
    @Binds
    @Singleton
    abstract fun bindAppUpdateManagerFactory(impl: FdroidAppUpdateManagerFactory): AppUpdateManagerFactory
}
