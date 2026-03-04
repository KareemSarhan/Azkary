package com.app.azkary.data.repository

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
        year: Int, month: Int, latitude: Double, longitude: Double, methodId: Int, school: Int
    ): PrayerCalendarResponse = withContext(Dispatchers.IO) @RequiresPermission(
        Manifest.permission.ACCESS_NETWORK_STATE
    ) {

        // First, try to get from cache
        val cachedMonth = prayerMonthDao.getMonth(year, month, latitude, longitude, methodId)

        if (cachedMonth != null && !shouldRefreshMonth(cachedMonth)) {
            // Check if cached data is corrupted (0 days)
            val cachedDays = prayerDayDao.getDaysForMonth(cachedMonth.id)
            println("DEBUG: PrayerTimesRepositoryImpl - Cached days count: ${cachedDays.size}")
            
            // If cache has 0 days, it's corrupted - clear it and force refresh
            if (cachedDays.isEmpty()) {
                println("DEBUG: PrayerTimesRepositoryImpl - Detected corrupted cache (0 days), clearing and forcing refresh")
                clearCorruptedCache(cachedMonth)
                // Don't return, continue to network fetch
            } else {
                // Return cached data if it's fresh and valid
                return@withContext mapCachedToResponse(cachedMonth, cachedDays)
            }
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
                // Check if cache is corrupted even when offline
                if (days.isEmpty()) {
                    println("DEBUG: PrayerTimesRepositoryImpl - Offline but cache is corrupted (0 days)")
                    throw ApiException("No network connection and corrupted cache data")
                }
                return@withContext mapCachedToResponse(month, days)
            }

            throw ApiException("No network connection and no cached data available")
        }
    }

    override suspend fun getDayPrayerTimes(
        date: LocalDate, latitude: Double, longitude: Double, methodId: Int, school: Int
    ): DayPrayerTimes? =
        withContext(Dispatchers.IO) @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE) {

            println("DEBUG: PrayerTimesRepositoryImpl - getDayPrayerTimes called for date=$date, lat=$latitude, lon=$longitude")

            // Try to get from cache first
            val monthEntity = prayerMonthDao.getMonth(
                date.year, date.monthValue, latitude, longitude, methodId
            )

            println("DEBUG: PrayerTimesRepositoryImpl - Cached monthEntity: $monthEntity")

            if (monthEntity != null) {
                val dayEntity = prayerDayDao.getDay(monthEntity.id, date)
                println("DEBUG: PrayerTimesRepositoryImpl - Cached dayEntity: $dayEntity")
                if (dayEntity != null) {
                    val result = mapEntityToDayPrayerTimes(dayEntity, monthEntity.timezone)
                    println("DEBUG: PrayerTimesRepositoryImpl - Returning cached prayer times: $result")
                    Log.d("PrayerTimes", "=== Prayer Times (from cache) ===")
                    Log.d("PrayerTimes", "Date: ${result.date}")
                    Log.d("PrayerTimes", "Fajr: ${result.fajr}, Dhuhr: ${result.dhuhr}, Asr: ${result.asr}")
                    Log.d("PrayerTimes", "Maghrib: ${result.maghrib}, Isha: ${result.isha}, Timezone: ${result.timezone}")
                    return@withContext result
                }
            }

            println("DEBUG: PrayerTimesRepositoryImpl - No cache found, checking network availability")

            // Check network availability
            if (!isNetworkAvailable()) {
                println("DEBUG: PrayerTimesRepositoryImpl - No network connection, returning null")
                return@withContext null
            }

            println("DEBUG: PrayerTimesRepositoryImpl - Network available, fetching from API")

            // If not in cache, fetch month and try again
            try {
                println("DEBUG: PrayerTimesRepositoryImpl - Calling getMonthlyPrayerTimes")
                val monthlyResponse = getMonthlyPrayerTimes(
                    date.year, date.monthValue, latitude, longitude, methodId, school
                )
                println("DEBUG: PrayerTimesRepositoryImpl - Monthly response received: ${monthlyResponse.data.size} days")

                // Try cache again after fetch
                val refreshedMonth = prayerMonthDao.getMonth(
                    date.year, date.monthValue, latitude, longitude, methodId
                )

                println("DEBUG: PrayerTimesRepositoryImpl - Refreshed monthEntity: $refreshedMonth")

                refreshedMonth?.let { month ->
                    val dayEntity = prayerDayDao.getDay(month.id, date)
                    println("DEBUG: PrayerTimesRepositoryImpl - Day entity after refresh: $dayEntity")
                    return@withContext dayEntity?.let {
                        val result = mapEntityToDayPrayerTimes(it, month.timezone)
                        println("DEBUG: PrayerTimesRepositoryImpl - Returning fresh prayer times: $result")
                        Log.d("PrayerTimes", "=== Prayer Times (from API) ===")
                        Log.d("PrayerTimes", "Date: ${result.date}")
                        Log.d("PrayerTimes", "Fajr: ${result.fajr}, Dhuhr: ${result.dhuhr}, Asr: ${result.asr}")
                        Log.d("PrayerTimes", "Maghrib: ${result.maghrib}, Isha: ${result.isha}, Timezone: ${result.timezone}")
                        result
                    }
                }

                println("DEBUG: PrayerTimesRepositoryImpl - No day entity found after refresh, returning null")
                return@withContext null
            } catch (e: ApiException) {
                println("DEBUG: PrayerTimesRepositoryImpl - API Exception: ${e.message}")
                return@withContext null
            } catch (e: Exception) {
                println("DEBUG: PrayerTimesRepositoryImpl - Exception during fetch: ${e.message}")
                e.printStackTrace()
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
        latitude: Double, longitude: Double, methodId: Int, school: Int
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

    override suspend fun getIslamicCurrentDate(
        latitude: Double, longitude: Double, methodId: Int, school: Int
    ): LocalDate = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val todayTimes = getDayPrayerTimes(today, latitude, longitude, methodId, school)

        if (todayTimes == null) {
            return@withContext today
        }

        val fajrInstant = ZonedDateTime.of(today, todayTimes.fajr, todayTimes.timezone).toInstant()

        if (now.isBefore(fajrInstant)) {
            val yesterdayTimes = getDayPrayerTimes(yesterday, latitude, longitude, methodId, school)
            if (yesterdayTimes != null) {
                return@withContext yesterday
            }
        }

        today
    }

    override suspend fun refreshMonth(
        year: Int, month: Int, latitude: Double, longitude: Double, methodId: Int, school: Int
    ): PrayerCalendarResponse = withContext(Dispatchers.IO) @RequiresPermission(
        Manifest.permission.ACCESS_NETWORK_STATE
    ) {

        if (!isNetworkAvailable()) {
            throw ApiException("No network connection available for refresh")
        }

        val response = networkRepository.fetchMonthlyPrayerTimes(
            year, month, latitude, longitude, methodId, school
        )

        // Log raw JSON for today's date
        val today = LocalDate.now()
        val todayRaw = response.data.find { day ->
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
            try {
                val apiDate = LocalDate.parse(day.date.gregorian.date, dateFormatter)
                apiDate == today
            } catch (e: Exception) {
                false
            }
        }

        todayRaw?.let { rawDay ->
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(rawDay)
            Log.i("RawPrayerJSON", "=== Raw JSON Response for Today ===")
            Log.i("RawPrayerJSON", jsonString)
            Log.i("RawPrayerJSON", "===================================\n")
        } ?: Log.w("RawPrayerJSON", "No data found for today's date: $today")

        // Force cache update
        cacheResponse(response, year, month, latitude, longitude, methodId)


        response
    }

    override suspend fun clearOldCache(keepLastMonths: Int) = withContext(Dispatchers.IO) {
        val cutoffDate = java.time.ZonedDateTime.now().minusMonths(keepLastMonths.toLong()).toInstant()
        prayerMonthDao.deleteOldMonths(cutoffDate)
    }

    // Enhanced mapDtoToEntity method with proper date parsing and error handling
    private fun mapDtoToEntity(dto: PrayerDayDto, monthId: Long): PrayerDayEntity {
        try {
            // Parse DD-MM-YYYY format from API response
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val date = LocalDate.parse(dto.date.gregorian.date, dateFormatter)
            println("DEBUG: PrayerTimesRepositoryImpl - Successfully parsed date: ${dto.date.gregorian.date} -> $date")
            
            return PrayerDayEntity(
                monthId = monthId,
                date = date,
                fajr = PrayerTimeParser.parseTimeString(dto.timings.Fajr),
                dhuhr = PrayerTimeParser.parseTimeString(dto.timings.Dhuhr),
                asr = PrayerTimeParser.parseTimeString(dto.timings.Asr),
                maghrib = PrayerTimeParser.parseTimeString(dto.timings.Maghrib),
                isha = PrayerTimeParser.parseTimeString(dto.timings.Isha),
                sunrise = PrayerTimeParser.parseTimeString(dto.timings.Sunrise),
                sunset = PrayerTimeParser.parseTimeString(dto.timings.Sunset),
                firstthird = PrayerTimeParser.parseTimeString(dto.timings.Firstthird),
                midnight = PrayerTimeParser.parseTimeString(dto.timings.Midnight),
                lastthird = PrayerTimeParser.parseTimeString(dto.timings.Lastthird)
            )
        } catch (e: Exception) {
            println("DEBUG: PrayerTimesRepositoryImpl - Error parsing day entity for date ${dto.date.gregorian.date}: ${e.message}")
            throw e // Re-throw to prevent silent filtering
        }
    }

    private suspend fun cacheResponse(
        response: PrayerCalendarResponse,
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int
    ) {
        println("DEBUG: PrayerTimesRepositoryImpl - Caching response with ${response.data.size} days")        // Create or update month entity
        val monthEntity = PrayerMonthEntity(
            year = year,
            month = month,
            latitude = latitude,
            longitude = longitude,
            methodId = methodId,
            timezone = response.data.firstOrNull()?.meta?.timezone ?: "UTC"
        )
        val monthId = prayerMonthDao.insertMonth(monthEntity)
        println("DEBUG: PrayerTimesRepositoryImpl - Created month entity with ID: $monthId")        // Convert and cache day entities with error tracking
        val dayEntities = mutableListOf<PrayerDayEntity>()
        val errors = mutableListOf<String>()
        response.data.forEachIndexed { index, dayDto ->
            try {
                val entity = mapDtoToEntity(dayDto, monthId)
                dayEntities.add(entity)
                println("DEBUG: PrayerTimesRepositoryImpl - Successfully processed day ${index + 1}: ${dayDto.date.gregorian.date}")
            } catch (e: Exception) {
                val errorMsg =
                    "Failed to process day ${index + 1} (${dayDto.date.gregorian.date}): ${e.message}"
                errors.add(errorMsg)
                println("DEBUG: PrayerTimesRepositoryImpl - $errorMsg")
            }
        }
        println("DEBUG: PrayerTimesRepositoryImpl - Successfully processed ${dayEntities.size} days, ${errors.size} errors")
        if (errors.isNotEmpty()) {
            println("DEBUG: PrayerTimesRepositoryImpl - Errors: ${errors.joinToString("; ")}")
        }
        if (dayEntities.isNotEmpty()) {
            prayerDayDao.upsertMonth(monthId, dayEntities)
            println("DEBUG: PrayerTimesRepositoryImpl - Cached ${dayEntities.size} day entities")
        } else {
            println("DEBUG: PrayerTimesRepositoryImpl - No valid day entities to cache!")
        }
    }

    private fun mapEntityToDayPrayerTimes(
        entity: PrayerDayEntity, timezone: String
    ): DayPrayerTimes {
        return DayPrayerTimes(
            date = entity.date,
            fajr = entity.fajr,
            sunrise = entity.sunrise,
            dhuhr = entity.dhuhr,
            asr = entity.asr,
            maghrib = entity.maghrib,
            sunset = entity.sunset,
            isha = entity.isha,
            timezone = ZoneId.of(timezone),
            firstthird = entity.firstthird,
            midnight = entity.midnight,
            lastthird = entity.lastthird
        )
    }

    private fun mapCachedToResponse(
        month: PrayerMonthEntity, days: List<PrayerDayEntity>
    ): PrayerCalendarResponse {
        val dayDtos = days.map { entity ->
            PrayerDayDto(
                timings = com.app.azkary.data.network.dto.PrayerTimingsDto(
                    Fajr = entity.fajr.toString(),
                    Sunrise = entity.sunrise.toString(),
                    Dhuhr = entity.dhuhr.toString(),
                    Asr = entity.asr.toString(),
                    Sunset = entity.sunset.toString(),
                    Maghrib = entity.maghrib.toString(),
                    Isha = entity.isha.toString(),
                    Firstthird = entity.firstthird.toString(),
                    Midnight = entity.midnight.toString(),
                    Lastthird = entity.lastthird.toString()
                ), date = com.app.azkary.data.network.dto.DateDto(
                    gregorian = com.app.azkary.data.network.dto.GregorianDateDto(
                        date = entity.date.toString(),
                        day = entity.date.dayOfMonth.toString(),
                        month = com.app.azkary.data.network.dto.MonthDto(
                            number = entity.date.monthValue, en = entity.date.month.name
                        ),
                        year = entity.date.year.toString()
                    )
                ), meta = com.app.azkary.data.network.dto.MetaDto(
                    timezone = month.timezone
                )
            )
        }

        return PrayerCalendarResponse(
            code = 200, status = "OK", data = dayDtos
        )
    }

    private fun shouldRefreshMonth(month: PrayerMonthEntity): Boolean {
        val now = Instant.now()
        val ageMillis = now.toEpochMilli() - month.lastUpdated.toEpochMilli()
        // Refresh if data is older than the configured cache refresh interval
        return ageMillis > cacheRefreshInterval
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: SecurityException) {
            // If we don't have ACCESS_NETWORK_STATE permission, assume network is available
            // This allows the app to attempt API calls even without network state checking
            println("DEBUG: PrayerTimesRepositoryImpl - No network state permission, assuming network available")
            true
        } catch (e: Exception) {
            // Any other exception, assume no network
            println("DEBUG: PrayerTimesRepositoryImpl - Error checking network: ${e.message}")
            false
        }
    }

    // Helper method to clear corrupted cache data
    private suspend fun clearCorruptedCache(monthEntity: PrayerMonthEntity) {
        println("DEBUG: PrayerTimesRepositoryImpl - Clearing corrupted cache for month: ${monthEntity.year}-${monthEntity.month}")
         
        // Delete all day entities for this month
        prayerDayDao.deleteDaysForMonth(monthEntity.id)
         
        // Note: We can't delete the month entity directly as deleteMonth method doesn't exist
        // The month will be overwritten when new data is cached
        println("DEBUG: PrayerTimesRepositoryImpl - Corrupted cache cleared successfully")
    }
}
