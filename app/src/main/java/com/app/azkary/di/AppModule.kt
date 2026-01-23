package com.app.azkary.di

import android.content.Context
import androidx.room.Room
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.dao.*
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.data.repository.LocationRepositoryImpl
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AzkarDatabase {
        return Room.databaseBuilder(
            context,
            AzkarDatabase::class.java,
            "azkar_db"
        )
            .fallbackToDestructiveMigration(false) // Resetting for the final schema version
        .build()
    }

    @Provides
    fun provideCategoryDao(db: AzkarDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideCategoryTextDao(db: AzkarDatabase): CategoryTextDao = db.categoryTextDao()

    @Provides
    fun provideAzkarItemDao(db: AzkarDatabase): AzkarItemDao = db.azkarItemDao()

    @Provides
    fun provideAzkarTextDao(db: AzkarDatabase): AzkarTextDao = db.azkarTextDao()

    @Provides
    fun provideCategoryItemDao(db: AzkarDatabase): CategoryItemDao = db.categoryItemDao()

    @Provides
    fun provideProgressDao(db: AzkarDatabase): ProgressDao = db.progressDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}
