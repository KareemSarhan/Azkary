package com.app.azkary.di

import android.content.Context
import androidx.room.Room
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.MIGRATION_4_5
import com.app.azkary.data.local.dao.AzkarItemDao
import com.app.azkary.data.local.dao.AzkarTextDao
import com.app.azkary.data.local.dao.CategoryDao
import com.app.azkary.data.local.dao.CategoryItemDao
import com.app.azkary.data.local.dao.CategoryTextDao
import com.app.azkary.data.local.dao.PrayerDayDao
import com.app.azkary.data.local.dao.PrayerMonthDao
import com.app.azkary.data.local.dao.ProgressDao
import com.app.azkary.util.LocaleManager
import com.app.azkary.util.SupportHelper
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
    abstract fun bindGeocodingRepository(impl: com.app.azkary.data.repository.GeocodingRepositoryImpl): com.app.azkary.data.repository.GeocodingRepository

    @Binds
    @Singleton
    abstract fun bindPrayerTimesRepository(impl: com.app.azkary.data.repository.PrayerTimesRepositoryImpl): com.app.azkary.data.repository.PrayerTimesRepository
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
            .fallbackToDestructiveMigration(true) // Reset database on schema change (v4 → v5: per-category counts)
            .addMigrations(MIGRATION_4_5)
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
    fun providePrayerMonthDao(db: AzkarDatabase): PrayerMonthDao = db.prayerMonthDao()

    @Provides
    fun providePrayerDayDao(db: AzkarDatabase): PrayerDayDao = db.prayerDayDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun providePrayerTimesCacheRefreshInterval(): Long {
        // Cache refresh interval in milliseconds (24 hours by default)
        // This could be made configurable via preferences in the future
        return 24 * 60 * 60 * 1000L
    }

    @Provides
    @Singleton
    fun provideLocaleManager(
        @ApplicationContext context: Context
    ): LocaleManager {
        return LocaleManager(context)
    }

    @Provides
    @Singleton
    fun provideSupportHelper(
        @ApplicationContext context: Context,
        localeManager: LocaleManager
    ): SupportHelper {
        return SupportHelper(context, localeManager)
    }
}
