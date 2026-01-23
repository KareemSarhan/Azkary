package com.app.azkary.di

import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.repository.PrayerTimesNetworkRepository
import com.app.azkary.data.repository.PrayerTimesNetworkRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.aladhan.com/v1/"
    private const val TIMEOUT_SECONDS = 30L
    private const val ENABLE_LOGGING = true // Set to false for production

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                // Add logging interceptor for debugging
                if (ENABLE_LOGGING) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAladhanApiService(retrofit: Retrofit): AladhanApiService {
        return retrofit.create(AladhanApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPrayerTimesNetworkRepository(
        impl: PrayerTimesNetworkRepositoryImpl
    ): PrayerTimesNetworkRepository
}
