package com.app.azkary.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.app.azkary.data.local.dao.PrayerDayDao
import com.app.azkary.data.local.dao.PrayerMonthDao
import com.app.azkary.data.local.entities.PrayerDayEntity
import com.app.azkary.data.local.entities.PrayerMonthEntity
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.dto.PrayerDayDto
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.domain.AzkarWindowEngine
import com.app.azkary.domain.model.DayPrayerTimes
import com.app.azkary.domain.model.WindowCalculationResult
import com.app.azkary.util.PrayerTimeParser
import com.app.azkary.util.ParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRepository: PrayerTimesNetworkRepository,
    private val prayerMonthDao: PrayerMonthDao,
    private val prayerDayDao: PrayerDayDao,
    private val windowEngine: AzkarWindowEngine,
    private val cacheRefreshInterval: Long
) : PrayerTimesRepository {

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override suspend fun getMonthlyPrayerTimes(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int,
        school: Int
    ): PrayerCalendarResponse = withContext(Dispatchers.IO) {
        
        // First, try to get from cache
        val cachedMonth = prayerMonthDao.getMonth(year, month, latitude, longitude, methodId)
        
        if (cachedMonth != null && !shouldRefreshMonth(cachedMonth)) {
            // Return cached data if it's fresh enough
            val cachedDays = prayerDayDao.getDaysForMonth(cachedMonth.id)
            return@withContext mapCachedToResponse(cachedMonth, cachedDays)
        }
        
        // If no cache or needs refresh, try network
        if (isNetworkAvailable()) {
            try {
                val response = networkRepository.fetchMonthlyPrayerTimes(
                    year, month, latitude, longitude, methodId, school
                )
                
                // Cache the response
                cacheResponse(response, year, month, latitude, longitude, methodId)
                
                return@withContext response
            } catch (e: Exception) {
                // Network failed, try to return stale cache if available
                cachedMonth?.let { month ->
                    val days = prayerDayDao.getDaysForMonth(month.id)
                    return@withContext mapCachedToResponse(month, days)
                }
                
                // No cache available, rethrow the network exception
                throw e
            }
        } else {
            // No network, return cache if available
            cachedMonth?.let { month ->
                val days = prayerDayDao.getDaysForMonth(month.id)
                return@withContext mapCachedToResponse(month, days)
            }
            
            throw ApiException("No network connection and no cached data available")
        }
    }

    override suspend fun getDayPrayerTimes(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        methodId: Int,
        school: Int
    ): DayPrayerTimes? = withContext(Dispatchers.IO) {
        
        // Try to get from cache first
        val monthEntity = prayerMonthDao.getMonth(
            date.year, date.monthValue, latitude, longitude, methodId
        )
        
        if (monthEntity != null) {
            val dayEntity = prayerDayDao.getDay(monthEntity.id, date)
            if (dayEntity != null) {
                return@withContext mapEntityToDayPrayerTimes(dayEntity, monthEntity.timezone)
            }
        }
        
        // If not in cache, fetch month and try again
        try {
            getMonthlyPrayerTimes(date.year, date.monthValue, latitude, longitude, methodId, school)
            
            // Try cache again after fetch
            val refreshedMonth = prayerMonthDao.getMonth(
                date.year, date.monthValue, latitude, longitude, methodId
            )
            
            refreshedMonth?.let { month ->
                val dayEntity = prayerDayDao.getDay(month.id, date)
                return@withContext dayEntity?.let { 
                    mapEntityToDayPrayerTimes(it, month.timezone)
                }
            }
            
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }

    override suspend fun getPrayerTimesInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        latitude: Double,
        longitude: Double,
        methodId: Int,
        school: Int
    ): List<DayPrayerTimes> = withContext(Dispatchers.IO) {
        
        if (startDate.isAfter(endDate)) {
            return@withContext emptyList()
        }
        
        val result = mutableListOf<DayPrayerTimes>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val dayTimes = getDayPrayerTimes(currentDate, latitude, longitude, methodId, school)
            dayTimes?.let { result.add(it) }
            currentDate = currentDate.plusDays(1)
        }
        
        result
    }

    override suspend fun getCurrentWindows(
        latitude: Double,
        longitude: Double,
        methodId: Int,
        school: Int
    ): WindowCalculationResult = withContext(Dispatchers.IO) {
        
        val now = Instant.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        
        val todayTimes = getDayPrayerTimes(today, latitude, longitude, methodId, school)
        val tomorrowTimes = getDayPrayerTimes(tomorrow, latitude, longitude, methodId, school)
        
        if (todayTimes == null) {
            return@withContext WindowCalculationResult(
                currentWindow = null,
                nextWindow = null,
                todayTimes = null,
                tomorrowTimes = tomorrowTimes
            )
        }
        
        windowEngine.calculateWindows(now, todayTimes, tomorrowTimes)
    }

    override suspend fun refreshMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int,
        school: Int
    ): PrayerCalendarResponse = withContext(Dispatchers.IO) {
        
        if (!isNetworkAvailable()) {
            throw ApiException("No network connection available for refresh")
        }
        
        val response = networkRepository.fetchMonthlyPrayerTimes(
            year, month, latitude, longitude, methodId, school
        )
        
        // Force cache update
        cacheResponse(response, year, month, latitude, longitude, methodId)
        
        response
    }

    override suspend fun clearOldCache(keepLastMonths: Int) = withContext(Dispatchers.IO) {
        val cutoffDate = Instant.now().minus(keepLastMonths.toLong(), ChronoUnit.MONTHS)
        prayerMonthDao.deleteOldMonths(cutoffDate)
    }

    private suspend fun cacheResponse(
        response: PrayerCalendarResponse,
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int
    ) {
        // Create or update month entity
        val monthEntity = PrayerMonthEntity(
            year = year,
            month = month,
            latitude = latitude,
            longitude = longitude,
            methodId = methodId,
            timezone = response.data.firstOrNull()?.meta?.timezone ?: "UTC"
        )
        
        val monthId = prayerMonthDao.insertMonth(monthEntity)
        
        // Convert and cache day entities
        val dayEntities = response.data.mapNotNull { dayDto ->
            try {
                mapDtoToEntity(dayDto, monthId)
            } catch (e: ParsingException) {
                // Skip invalid prayer times but continue processing others
                null
            }
        }
        
        prayerDayDao.upsertMonth(monthId, dayEntities)
    }

    private fun mapDtoToEntity(dto: PrayerDayDto, monthId: Long): PrayerDayEntity {
        val date = LocalDate.parse(dto.date.gregorian.date)
        
        return PrayerDayEntity(
            monthId = monthId,
            date = date,
            fajr = PrayerTimeParser.parseTimeString(dto.timings.Fajr),
            dhuhr = PrayerTimeParser.parseTimeString(dto.timings.Dhuhr),
            asr = PrayerTimeParser.parseTimeString(dto.timings.Asr),
            maghrib = PrayerTimeParser.parseTimeString(dto.timings.Maghrib),
            isha = PrayerTimeParser.parseTimeString(dto.timings.Isha),
            sunrise = PrayerTimeParser.parseTimeString(dto.timings.Sunrise),
            sunset = PrayerTimeParser.parseTimeString(dto.timings.Sunset)
        )
    }

    private fun mapEntityToDayPrayerTimes(entity: PrayerDayEntity, timezone: String): DayPrayerTimes {
        return DayPrayerTimes(
            date = entity.date,
            fajr = entity.fajr,
            dhuhr = entity.dhuhr,
            asr = entity.asr,
            maghrib = entity.maghrib,
            isha = entity.isha,
            timezone = ZoneId.of(timezone)
        )
    }

    private fun mapCachedToResponse(month: PrayerMonthEntity, days: List<PrayerDayEntity>): PrayerCalendarResponse {
        val dayDtos = days.map { entity ->
            PrayerDayDto(
                timings = com.app.azkary.data.network.dto.PrayerTimingsDto(
                    Fajr = entity.fajr.toString(),
                    Sunrise = entity.sunrise.toString(),
                    Dhuhr = entity.dhuhr.toString(),
                    Asr = entity.asr.toString(),
                    Sunset = entity.sunset.toString(),
                    Maghrib = entity.maghrib.toString(),
                    Isha = entity.isha.toString()
                ),
                date = com.app.azkary.data.network.dto.DateDto(
                    gregorian = com.app.azkary.data.network.dto.GregorianDateDto(
                        date = entity.date.toString(),
                        day = entity.date.dayOfMonth.toString(),
                        month = com.app.azkary.data.network.dto.MonthDto(
                            number = entity.date.monthValue,
                            en = entity.date.month.name
                        ),
                        year = entity.date.year.toString()
                    )
                ),
                meta = com.app.azkary.data.network.dto.MetaDto(
                    timezone = month.timezone
                )
            )
        }
        
        return PrayerCalendarResponse(
            code = 200,
            status = "OK",
            data = dayDtos
        )
    }

    private fun shouldRefreshMonth(month: PrayerMonthEntity): Boolean {
        val now = Instant.now()
        val ageMillis = now.toEpochMilli() - month.lastUpdated.toEpochMilli()
        // Refresh if data is older than the configured cache refresh interval
        return ageMillis > cacheRefreshInterval
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}