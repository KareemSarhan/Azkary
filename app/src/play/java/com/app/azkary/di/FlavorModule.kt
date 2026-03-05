package com.app.azkary.di

import android.content.Context
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.data.repository.PlayLocationRepository
import com.app.azkary.domain.AppRatingManager
import com.app.azkary.domain.PlayAppRatingManager
import com.app.azkary.util.AppUpdateManagerFactory
import com.app.azkary.util.PlayAppUpdateManagerFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorModule {
    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: PlayLocationRepository): LocationRepository
    
    @Binds
    @Singleton
    abstract fun bindAppRatingManager(impl: PlayAppRatingManager): AppRatingManager
    
    @Binds
    @Singleton
    abstract fun bindAppUpdateManagerFactory(impl: PlayAppUpdateManagerFactory): AppUpdateManagerFactory
}

@Module
@InstallIn(SingletonComponent::class)
object PlayServicesModule {
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}
