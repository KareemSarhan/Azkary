package com.app.azkary.data.repository

import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.domain.model.DayPrayerTimes
import com.app.azkary.domain.model.WindowCalculationResult
import com.app.azkary.util.ParsingException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Main repository for prayer times with offline-first approach
 */
interface PrayerTimesRepository {
    
    /**
     * Gets prayer times for a specific month with offline-first strategy
     * 
     * @param year Year to fetch
     * @param month Month to fetch (1-12)
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param methodId Calculation method ID (default: 4)
     * @param school Juristic school (default: 0)
     * @return Prayer calendar response
     */
    suspend fun getMonthlyPrayerTimes(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int = 4,
        school: Int = 0
    ): PrayerCalendarResponse
    
    /**
     * Gets prayer times for a specific day
     */
    suspend fun getDayPrayerTimes(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        methodId: Int = 4,
        school: Int = 0
    ): DayPrayerTimes?
    
    /**
     * Gets prayer times for a date range
     */
    suspend fun getPrayerTimesInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        latitude: Double,
        longitude: Double,
        methodId: Int = 4,
        school: Int = 0
    ): List<DayPrayerTimes>
    
    /**
     * Calculates current Azkar windows
     */
    suspend fun getCurrentWindows(
        latitude: Double,
        longitude: Double,
        methodId: Int = 4,
        school: Int = 0
    ): WindowCalculationResult
    
    /**
     * Forces refresh of prayer times for a month
     */
    suspend fun refreshMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int = 4,
        school: Int = 0
    ): PrayerCalendarResponse
    
    /**
     * Clears old cached data
     */
    suspend fun clearOldCache(keepLastMonths: Int = 2)
}