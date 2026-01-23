package com.app.azkary.data.network

import com.app.azkary.data.network.dto.PrayerCalendarResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApiService {

    /**
     * Fetch prayer times calendar for a specific month
     * 
     * @param year Gregorian year (e.g., 2026)
     * @param month Month number (1-12)
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param method Calculation method ID (default: 4 = Umm Al-Qura, Makkah)
     * @param school Juristic school for Asr calculation (0 = Shafi, 1 = Hanafi)
     * @return Calendar response with daily prayer times for the entire month
     */
    @GET("calendar/{year}/{month}")
    suspend fun getMonthlyCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 4, // Umm Al-Qura
        @Query("school") school: Int = 0  // Shafi
    ): PrayerCalendarResponse
}
