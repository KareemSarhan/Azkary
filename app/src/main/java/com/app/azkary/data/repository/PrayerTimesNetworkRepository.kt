package com.app.azkary.data.repository

import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.data.network.exception.RateLimitException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

interface PrayerTimesNetworkRepository {
    /**
     * Fetch monthly prayer times from Aladhan API
     */
    suspend fun fetchMonthlyPrayerTimes(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        method: Int = 4,
        school: Int = 0
    ): PrayerCalendarResponse
}

@Singleton
class PrayerTimesNetworkRepositoryImpl @Inject constructor(
    private val apiService: AladhanApiService
) : PrayerTimesNetworkRepository {

    override suspend fun fetchMonthlyPrayerTimes(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        method: Int,
        school: Int
    ): PrayerCalendarResponse = withContext(Dispatchers.IO) {
        try {
            val response: Response<PrayerCalendarResponse> = apiService.getMonthlyCalendar(
                year = year,
                month = month,
                latitude = latitude,
                longitude = longitude,
                method = method,
                school = school
            )
            
            if (!response.isSuccessful) {
                handleHttpError(response)
            }
            
            response.body() ?: throw ApiException("Empty response body")
        } catch (e: Exception) {
            if (e is ApiException || e is RateLimitException) {
                throw e
            }
            throw ApiException("Network error: ${e.message}", e)
        }
    }
    
    private suspend fun handleHttpError(response: Response<PrayerCalendarResponse>) {
        val httpCode = response.code()
        val errorBody = response.errorBody()?.string()
        
        when (httpCode) {
            429 -> {
                val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                throw RateLimitException(
                    message = "Rate limit exceeded. Retry after: ${retryAfter ?: "unknown"} seconds",
                    retryAfterSeconds = retryAfter
                )
            }
            in 400..499 -> {
                throw ApiException(
                    message = "Client error: $httpCode - ${errorBody ?: "Unknown error"}",
                    httpCode = httpCode
                )
            }
            in 500..599 -> {
                throw ApiException(
                    message = "Server error: $httpCode - ${errorBody ?: "Unknown error"}",
                    httpCode = httpCode
                )
            }
            else -> {
                throw ApiException(
                    message = "Unexpected HTTP error: $httpCode - ${errorBody ?: "Unknown error"}",
                    httpCode = httpCode
                )
            }
        }
    }
}