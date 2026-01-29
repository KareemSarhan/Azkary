package com.app.azkary.data.repository

import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            apiService.getMonthlyCalendar(
                year = year,
                month = month,
                latitude = latitude,
                longitude = longitude,
                method = method,
                school = school
            )
        } catch (e: Exception) {
            throw e;
        }
    }
}