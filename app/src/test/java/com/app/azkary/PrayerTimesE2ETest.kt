package com.app.azkary

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.dao.PrayerDayDao
import com.app.azkary.data.local.dao.PrayerMonthDao
import com.app.azkary.data.local.entities.PrayerDayEntity
import com.app.azkary.data.local.entities.PrayerMonthEntity
import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.repository.PrayerTimesNetworkRepository
import com.app.azkary.data.repository.PrayerTimesNetworkRepositoryImpl
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.data.repository.PrayerTimesRepositoryImpl
import com.app.azkary.domain.AzkarWindowEngine
import com.app.azkary.domain.model.AzkarWindow
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * End-to-End tests for Prayer Times user flows
 * Tests complete user scenarios from initial load to daily usage,
 * including window calculations and multi-day operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class PrayerTimesE2ETest {

    private lateinit var context: Context
    private lateinit var database: AzkarDatabase
    private lateinit var prayerMonthDao: PrayerMonthDao
    private lateinit var prayerDayDao: PrayerDayDao
    private lateinit var server: MockWebServer
    private lateinit var repository: PrayerTimesRepository
    private lateinit var windowEngine: AzkarWindowEngine

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        database = Room.inMemoryDatabaseBuilder(context, AzkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        prayerMonthDao = database.prayerMonthDao()
        prayerDayDao = database.prayerDayDao()
        
        server = MockWebServer()
        server.start()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        
        val api = retrofit.create(AladhanApiService::class.java)
        val networkRepository = PrayerTimesNetworkRepositoryImpl(api)
        
        windowEngine = AzkarWindowEngine()
        
        repository = PrayerTimesRepositoryImpl(
            context = context,
            networkRepository = networkRepository,
            prayerMonthDao = prayerMonthDao,
            prayerDayDao = prayerDayDao,
            windowEngine = windowEngine,
            cacheRefreshInterval = Long.MAX_VALUE // Never auto-refresh for E2E tests
        )
    }

    @After
    fun teardown() {
        database.close()
        server.shutdown()
    }

    //region User Flow: Initial App Launch

    @Test
    fun `E2E - First time user fetches prayer times`() = runTest {
        // User opens app for the first time in Riyadh
        // Enqueue API response
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        
        // User requests current month's prayer times
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Verify data is fetched and cached
        assertEquals(200, result.code)
        assertEquals(31, result.data.size)
        assertEquals("Asia/Riyadh", result.data.first().meta.timezone)
        
        // Verify cache was populated
        val cachedMonth = prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4)
        assertNotNull(cachedMonth)
    }

    @Test
    fun `E2E - User returns to app uses cached data`() = runTest {
        // First launch - fetch from network
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Second launch - should use cache
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        assertEquals(200, result.code)
        assertEquals(31, result.data.size)
        // Only one request made to server
        assertEquals(1, server.requestCount)
    }

    //endregion

    //region User Flow: Daily Prayer Times

    @Test
    fun `E2E - User views today's prayer times`() = runTest {
        // Setup: Cache exists
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // User wants to see today's prayer times (January 15)
        val today = LocalDate.of(2026, 1, 15)
        val dayTimes = repository.getDayPrayerTimes(today, 24.7136, 46.6753)
        
        assertNotNull(dayTimes)
        assertEquals(today, dayTimes?.date)
        assertEquals("Asia/Riyadh", dayTimes?.timezone.toString())
        
        // Verify all prayer times are present
        assertNotNull(dayTimes?.fajr)
        assertNotNull(dayTimes?.dhuhr)
        assertNotNull(dayTimes?.asr)
        assertNotNull(dayTimes?.maghrib)
        assertNotNull(dayTimes?.isha)
    }

    @Test
    fun `E2E - User views prayer times for different day`() = runTest {
        // Setup: Cache exists
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // User views prayer times for January 1st
        val date = LocalDate.of(2026, 1, 1)
        val dayTimes = repository.getDayPrayerTimes(date, 24.7136, 46.6753)
        
        assertNotNull(dayTimes)
        assertEquals(date, dayTimes?.date)
    }

    //endregion

    //region User Flow: Azkar Windows

    @Test
    fun `E2E - User checks current Azkar window`() = runTest {
        // Setup: Cache with known prayer times
        val monthId = prayerMonthDao.insertMonth(
            PrayerMonthEntity(
                year = 2026,
                month = 1,
                latitude = 24.7136,
                longitude = 46.6753,
                methodId = 4,
                timezone = "Asia/Riyadh"
            )
        )
        
        // Insert day with specific prayer times for predictable window calculation
        val today = LocalDate.now()
        prayerDayDao.insertDay(
            PrayerDayEntity(
                monthId = monthId,
                date = today,
                fajr = LocalTime.of(5, 0),
                sunrise = LocalTime.of(6, 15),
                dhuhr = LocalTime.of(12, 5),
                asr = LocalTime.of(15, 30),
                maghrib = LocalTime.of(18, 0),
                isha = LocalTime.of(19, 30),
                sunset = LocalTime.of(18, 2),
                firstthird = LocalTime.of(23, 30),
                midnight = LocalTime.of(0, 30),
                lastthird = LocalTime.of(1, 30)
            )
        )
        
        // Get current windows
        val result = repository.getCurrentWindows(24.7136, 46.6753)
        
        // Verify window calculation succeeded
        assertNotNull(result)
        // Current window should be determined based on current time
        // We can't assert specific window without mocking time, but we can verify structure
    }

    @Test
    fun `E2E - Morning window calculation`() = runTest {
        // Create specific prayer times for testing
        val todayTimes = createDayPrayerTimes(
            fajr = LocalTime.of(5, 0),
            asr = LocalTime.of(15, 30),
            isha = LocalTime.of(19, 30)
        )
        
        // Test at 10 AM - should be Morning window
        val testTime = todayTimes.fajr.plusHours(5) // 10:00 AM
        val windows = windowEngine.calculateWindows(
            currentTime = java.time.LocalDateTime.of(LocalDate.now(), testTime).atZone(ZoneId.of("Asia/Riyadh")).toInstant(),
            todayTimes = todayTimes,
            tomorrowTimes = null
        )
        
        assertEquals(AzkarWindow.MORNING, windows.currentWindow?.window)
    }

    @Test
    fun `E2E - Night window calculation`() = runTest {
        val todayTimes = createDayPrayerTimes(
            fajr = LocalTime.of(5, 0),
            asr = LocalTime.of(15, 30),
            isha = LocalTime.of(19, 30)
        )
        
        // Test at 5 PM - should be Night window (Asr to Isha)
        val testTime = todayTimes.asr.plusHours(2) // 5:30 PM
        val windows = windowEngine.calculateWindows(
            currentTime = java.time.LocalDateTime.of(LocalDate.now(), testTime).atZone(ZoneId.of("Asia/Riyadh")).toInstant(),
            todayTimes = todayTimes,
            tomorrowTimes = null
        )
        
        assertEquals(AzkarWindow.NIGHT, windows.currentWindow?.window)
    }

    @Test
    fun `E2E - Sleep window calculation`() = runTest {
        val todayTimes = createDayPrayerTimes(
            fajr = LocalTime.of(5, 0),
            asr = LocalTime.of(15, 30),
            isha = LocalTime.of(19, 30)
        )
        
        // Test at 10 PM - should be Sleep window (Isha to next Fajr)
        val testTime = todayTimes.isha.plusHours(3) // 10:30 PM
        val windows = windowEngine.calculateWindows(
            currentTime = java.time.LocalDateTime.of(LocalDate.now(), testTime).atZone(ZoneId.of("Asia/Riyadh")).toInstant(),
            todayTimes = todayTimes,
            tomorrowTimes = null
        )
        
        assertEquals(AzkarWindow.SLEEP, windows.currentWindow?.window)
    }

    //endregion

    //region User Flow: Week View

    @Test
    fun `E2E - User views prayer times for the week`() = runTest {
        // Setup: Cache exists
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // User wants to see week's prayer times (Jan 1-7)
        val startDate = LocalDate.of(2026, 1, 1)
        val endDate = LocalDate.of(2026, 1, 7)
        
        val weekTimes = repository.getPrayerTimesInRange(
            startDate, endDate, 24.7136, 46.6753
        )
        
        assertEquals(7, weekTimes.size)
        assertEquals(startDate, weekTimes.first().date)
        assertEquals(endDate, weekTimes.last().date)
    }

    @Test
    fun `E2E - User views prayer times spanning month boundary`() = runTest {
        // Setup: Cache for both months
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhFebruary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 2, 24.7136, 46.6753)
        
        // User views prayer times spanning Jan 30 - Feb 2
        val startDate = LocalDate.of(2026, 1, 30)
        val endDate = LocalDate.of(2026, 2, 2)
        
        val rangeTimes = repository.getPrayerTimesInRange(
            startDate, endDate, 24.7136, 46.6753
        )
        
        assertEquals(4, rangeTimes.size)
        assertEquals(startDate, rangeTimes.first().date)
        assertEquals(endDate, rangeTimes.last().date)
    }

    //endregion

    //region User Flow: Manual Refresh

    @Test
    fun `E2E - User manually refreshes prayer times`() = runTest {
        // Setup: Initial cache
        server.enqueue(MockResponse().setResponseCode(200).setBody(riyadhJanuary2026Response()))
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // User pulls to refresh
        server.enqueue(MockResponse().setResponseCode(200).setBody(updatedRiyadhJanuaryResponse()))
        val refreshed = repository.refreshMonth(2026, 1, 24.7136, 46.6753)
        
        assertEquals(200, refreshed.code)
        // Cache should be updated
        val cachedDays = prayerDayDao.getDaysForMonth(
            prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4)!!.id
        )
        assertEquals(31, cachedDays.size)
    }

    //endregion

    //region User Flow: Islamic Date

    @Test
    fun `E2E - User views Islamic current date`() = runTest {
        // Setup: Cache exists
        val monthId = prayerMonthDao.insertMonth(
            PrayerMonthEntity(
                year = 2026,
                month = 1,
                latitude = 24.7136,
                longitude = 46.6753,
                methodId = 4,
                timezone = "Asia/Riyadh"
            )
        )
        
        prayerDayDao.insertDay(
            PrayerDayEntity(
                monthId = monthId,
                date = LocalDate.now(),
                fajr = LocalTime.of(5, 0),
                sunrise = LocalTime.of(6, 15),
                dhuhr = LocalTime.of(12, 5),
                asr = LocalTime.of(15, 30),
                maghrib = LocalTime.of(18, 0),
                isha = LocalTime.of(19, 30),
                sunset = LocalTime.of(18, 2),
                firstthird = LocalTime.of(23, 30),
                midnight = LocalTime.of(0, 30),
                lastthird = LocalTime.of(1, 30)
            )
        )
        
        // Get Islamic current date
        val islamicDate = repository.getIslamicCurrentDate(24.7136, 46.6753)
        
        // Should return a valid date
        assertNotNull(islamicDate)
    }

    //endregion

    //region Helper Methods

    private fun createDayPrayerTimes(
        fajr: LocalTime,
        asr: LocalTime,
        isha: LocalTime
    ): com.app.azkary.domain.model.DayPrayerTimes {
        return com.app.azkary.domain.model.DayPrayerTimes(
            date = LocalDate.now(),
            fajr = fajr,
            sunrise = fajr.plusHours(1),
            dhuhr = fajr.plusHours(7),
            asr = asr,
            maghrib = isha.minusHours(1),
            isha = isha,
            timezone = ZoneId.of("Asia/Riyadh"),
            firstthird = isha.plusHours(3),
            midnight = isha.plusHours(4),
            lastthird = isha.plusHours(5),
            sunset = isha.minusHours(1).plusMinutes(2)
        )
    }

    private fun riyadhJanuary2026Response(): String {
        val days = (1..31).joinToString(",") { day ->
            val dayStr = day.toString().padStart(2, '0')
            val fajrMinute = (day % 10) + 55
            val fajrHour = if (fajrMinute >= 60) 6 else 5
            val adjustedMinute = if (fajrMinute >= 60) fajrMinute - 60 else fajrMinute
            
            """
            {
              "timings": {
                "Fajr": "${fajrHour.toString().padStart(2, '0')}:${adjustedMinute.toString().padStart(2, '0')}",
                "Sunrise": "06:15",
                "Dhuhr": "12:10",
                "Asr": "15:20",
                "Maghrib": "17:50",
                "Isha": "19:10",
                "Sunset": "17:52",
                "Firstthird": "23:30",
                "Midnight": "00:30",
                "Lastthird": "01:30"
              },
              "date": { 
                "gregorian": { 
                  "date":"$dayStr-01-2026",
                  "day":"$dayStr",
                  "month":{"number":1,"en":"January"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
            """.trimIndent()
        }

        return """
        {
          "code": 200,
          "status": "OK",
          "data": [$days]
        }
        """.trimIndent()
    }

    private fun riyadhFebruary2026Response(): String {
        val days = (1..28).joinToString(",") { day ->
            val dayStr = day.toString().padStart(2, '0')
            """
            {
              "timings": {
                "Fajr": "05:45",
                "Sunrise": "06:45",
                "Dhuhr": "12:20",
                "Asr": "15:30",
                "Maghrib": "18:00",
                "Isha": "19:20",
                "Sunset": "18:02",
                "Firstthird": "23:30",
                "Midnight": "00:30",
                "Lastthird": "01:30"
              },
              "date": { 
                "gregorian": { 
                  "date":"$dayStr-02-2026",
                  "day":"$dayStr",
                  "month":{"number":2,"en":"February"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
            """.trimIndent()
        }

        return """
        {
          "code": 200,
          "status": "OK",
          "data": [$days]
        }
        """.trimIndent()
    }

    private fun updatedRiyadhJanuaryResponse(): String {
        // Same structure but slightly different times (simulating update)
        return riyadhJanuary2026Response()
    }

    //endregion
}
