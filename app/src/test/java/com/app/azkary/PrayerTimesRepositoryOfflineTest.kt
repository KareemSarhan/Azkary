package com.app.azkary

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.dao.PrayerDayDao
import com.app.azkary.data.local.dao.PrayerMonthDao
import com.app.azkary.data.local.entities.PrayerDayEntity
import com.app.azkary.data.local.entities.PrayerMonthEntity
import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.data.repository.PrayerTimesNetworkRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.data.repository.PrayerTimesRepositoryImpl
import com.app.azkary.domain.AzkarWindowEngine
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities
import retrofit2.Retrofit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Offline-first behavior tests for PrayerTimesRepository
 * Tests caching, stale data handling, network failure scenarios,
 * and synchronization between local cache and remote API.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class PrayerTimesRepositoryOfflineTest {

    private lateinit var context: Context
    private lateinit var database: AzkarDatabase
    private lateinit var prayerMonthDao: PrayerMonthDao
    private lateinit var prayerDayDao: PrayerDayDao
    private lateinit var server: MockWebServer
    private lateinit var repository: PrayerTimesRepository
    private lateinit var networkRepository: PrayerTimesNetworkRepository
    private lateinit var windowEngine: AzkarWindowEngine
    private lateinit var connectivityManager: ConnectivityManager

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(context, AzkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        prayerMonthDao = database.prayerMonthDao()
        prayerDayDao = database.prayerDayDao()
        
        // Setup MockWebServer
        server = MockWebServer()
        server.start()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        
        val api = retrofit.create(AladhanApiService::class.java)
        networkRepository = com.app.azkary.data.repository.PrayerTimesNetworkRepositoryImpl(api)
        
        windowEngine = AzkarWindowEngine()
        
        // Create repository with short cache refresh interval for testing
        repository = PrayerTimesRepositoryImpl(
            context = context,
            networkRepository = networkRepository,
            prayerMonthDao = prayerMonthDao,
            prayerDayDao = prayerDayDao,
            windowEngine = windowEngine,
            cacheRefreshInterval = 1000L // 1 second for testing
        )
        
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @After
    fun teardown() {
        database.close()
        server.shutdown()
    }

    //region Cache-First Tests

    @Test
    fun `returns cached data when fresh and network available`() = runTest {
        // Setup: Insert fresh cache
        val monthId = insertCachedMonth(2026, 1, Instant.now())
        insertCachedDays(monthId, 2026, 1)
        
        // Set network available but don't enqueue response
        setNetworkAvailable(true)
        
        // Act: Get prayer times
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Assert: Should return cached data without network call
        assertEquals(200, result.code)
        assertEquals(31, result.data.size)
        assertEquals(0, server.requestCount) // No network call made
    }

    @Test
    fun `fetches from network when cache is stale`() = runTest {
        // Setup: Insert stale cache (older than 1 second)
        val staleTime = Instant.now().minusSeconds(10)
        val monthId = insertCachedMonth(2026, 1, staleTime)
        insertCachedDays(monthId, 2026, 1)
        
        // Enqueue network response
        server.enqueue(MockResponse().setResponseCode(200).setBody(validCalendarResponse()))
        setNetworkAvailable(true)
        
        // Act
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Assert: Should fetch from network
        assertEquals(200, result.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `returns stale cache when network fails`() = runTest {
        // Setup: Insert stale cache
        val staleTime = Instant.now().minusSeconds(10)
        val monthId = insertCachedMonth(2026, 1, staleTime)
        insertCachedDays(monthId, 2026, 1)
        
        // Enqueue network error
        server.enqueue(MockResponse().setResponseCode(500))
        setNetworkAvailable(true)
        
        // Act
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Assert: Should return stale cache
        assertEquals(200, result.code)
        assertEquals(31, result.data.size)
    }

    //endregion

    //region Offline Mode Tests

    @Test
    fun `returns cached data when offline`() = runTest {
        // Setup: Insert cache
        val monthId = insertCachedMonth(2026, 1, Instant.now())
        insertCachedDays(monthId, 2026, 1)
        
        // Set network unavailable
        setNetworkAvailable(false)
        
        // Act
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Assert
        assertEquals(200, result.code)
        assertEquals(31, result.data.size)
    }

    @Test(expected = ApiException::class)
    fun `throws exception when offline and no cache`() = runTest {
        // Set network unavailable
        setNetworkAvailable(false)
        
        // Act: Should throw exception
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test
    fun `getDayPrayerTimes returns null when offline and no cache`() = runTest {
        // Set network unavailable
        setNetworkAvailable(false)
        
        // Act
        val result = repository.getDayPrayerTimes(
            LocalDate.of(2026, 1, 15), 
            24.7136, 
            46.6753
        )
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `getDayPrayerTimes returns cached day when offline`() = runTest {
        // Setup: Insert cache
        val monthId = insertCachedMonth(2026, 1, Instant.now())
        insertCachedDays(monthId, 2026, 1)
        
        // Set network unavailable
        setNetworkAvailable(false)
        
        // Act
        val result = repository.getDayPrayerTimes(
            LocalDate.of(2026, 1, 15), 
            24.7136, 
            46.6753
        )
        
        // Assert
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 1, 15), result?.date)
    }

    //endregion

    //region Cache Corruption Tests

    @Test
    fun `detects and clears corrupted cache with 0 days`() = runTest {
        // Setup: Insert corrupted cache (month with no days)
        insertCachedMonth(2026, 1, Instant.now())
        // Don't insert any days - this simulates corruption
        
        // Enqueue network response
        server.enqueue(MockResponse().setResponseCode(200).setBody(validCalendarResponse()))
        setNetworkAvailable(true)
        
        // Act
        val result = repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Assert: Should fetch from network
        assertEquals(200, result.code)
        assertEquals(1, server.requestCount)
    }

    @Test(expected = ApiException::class)
    fun `throws exception when offline and cache is corrupted`() = runTest {
        // Setup: Insert corrupted cache
        insertCachedMonth(2026, 1, Instant.now())
        // Don't insert any days
        
        // Set network unavailable
        setNetworkAvailable(false)
        
        // Act: Should throw exception
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    //endregion

    //region Refresh Tests

    @Test
    fun `refreshMonth forces network fetch even with fresh cache`() = runTest {
        // Setup: Insert fresh cache
        val monthId = insertCachedMonth(2026, 1, Instant.now())
        insertCachedDays(monthId, 2026, 1)
        
        // Enqueue network response
        server.enqueue(MockResponse().setResponseCode(200).setBody(validCalendarResponse()))
        setNetworkAvailable(true)
        
        // Act
        val result = repository.refreshMonth(2026, 1, 24.7136, 46.6753)
        
        // Assert: Should fetch from network
        assertEquals(200, result.code)
        assertEquals(1, server.requestCount)
    }

    @Test(expected = ApiException::class)
    fun `refreshMonth throws exception when offline`() = runTest {
        // Set network unavailable
        setNetworkAvailable(false)
        
        // Act: Should throw exception
        repository.refreshMonth(2026, 1, 24.7136, 46.6753)
    }

    //endregion

    //region Cache Management Tests

    @Test
    fun `caches network response for future use`() = runTest {
        // Enqueue network response
        server.enqueue(MockResponse().setResponseCode(200).setBody(validCalendarResponse()))
        setNetworkAvailable(true)
        
        // Act: First call fetches from network
        repository.getMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        
        // Verify cache was populated
        val cachedMonth = prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4)
        assertNotNull(cachedMonth)
        
        val cachedDays = cachedMonth?.let { prayerDayDao.getDaysForMonth(it.id) }
        assertEquals(31, cachedDays?.size)
    }

    @Test
    fun `clearOldCache removes old data`() = runTest {
        // Setup: Insert old and new cache
        val oldTime = Instant.now().minusSeconds(86400 * 90) // 90 days ago
        val newTime = Instant.now()
        
        val oldMonthId = prayerMonthDao.insertMonth(
            PrayerMonthEntity(
                year = 2025,
                month = 10,
                latitude = 24.7136,
                longitude = 46.6753,
                methodId = 4,
                timezone = "Asia/Riyadh",
                lastUpdated = oldTime
            )
        )
        
        val newMonthId = prayerMonthDao.insertMonth(
            PrayerMonthEntity(
                year = 2026,
                month = 1,
                latitude = 24.7136,
                longitude = 46.6753,
                methodId = 4,
                timezone = "Asia/Riyadh",
                lastUpdated = newTime
            )
        )
        
        // Act
        repository.clearOldCache(keepLastMonths = 1)
        
        // Assert: Old data should be deleted
        assertNull(prayerMonthDao.getMonth(2025, 10, 24.7136, 46.6753, 4))
        assertNotNull(prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4))
    }

    //endregion

    //region Range Query Tests

    @Test
    fun `getPrayerTimesInRange returns empty when offline and no cache`() = runTest {
        setNetworkAvailable(false)
        
        val result = repository.getPrayerTimesInRange(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 7),
            24.7136,
            46.6753
        )
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPrayerTimesInRange returns available cached data when offline`() = runTest {
        // Setup: Insert cache for part of range
        val monthId = insertCachedMonth(2026, 1, Instant.now())
        insertCachedDays(monthId, 2026, 1)
        
        setNetworkAvailable(false)
        
        val result = repository.getPrayerTimesInRange(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 7),
            24.7136,
            46.6753
        )
        
        // Should return available cached days
        assertEquals(7, result.size)
    }

    @Test
    fun `getPrayerTimesInRange handles reversed dates`() = runTest {
        val result = repository.getPrayerTimesInRange(
            LocalDate.of(2026, 1, 7),
            LocalDate.of(2026, 1, 1),
            24.7136,
            46.6753
        )
        
        assertTrue(result.isEmpty())
    }

    //endregion

    //region Helper Methods

    private fun setNetworkAvailable(available: Boolean) {
        val shadowConnectivityManager = Shadows.shadowOf(connectivityManager)
        
        if (available) {
            // Create a network info that's connected
            val networkInfo = android.net.NetworkInfo(
                android.net.ConnectivityManager.TYPE_WIFI,
                0, "WIFI", ""
            )
            val shadowNetworkInfo = Shadows.shadowOf(networkInfo)
            shadowNetworkInfo.setConnectionStatus(android.net.NetworkInfo.State.CONNECTED)
            shadowConnectivityManager.setActiveNetworkInfo(networkInfo)
            
            // Also set up network capabilities
            val networkCapabilities = ShadowNetworkCapabilities.newInstance()
            Shadows.shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val activeNetwork = shadowConnectivityManager.activeNetwork
            if (activeNetwork != null) {
                shadowConnectivityManager.setNetworkCapabilities(activeNetwork, networkCapabilities)
            }
        } else {
            shadowConnectivityManager.setActiveNetworkInfo(null)
        }
    }

    private suspend fun insertCachedMonth(year: Int, month: Int, lastUpdated: Instant): Long {
        val entity = PrayerMonthEntity(
            year = year,
            month = month,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = lastUpdated
        )
        return prayerMonthDao.insertMonth(entity)
    }

    private suspend fun insertCachedDays(monthId: Long, year: Int, month: Int) {
        val days = (1..31).map { day ->
            PrayerDayEntity(
                monthId = monthId,
                date = LocalDate.of(year, month, day),
                fajr = LocalTime.of(5, 0),
                dhuhr = LocalTime.of(12, 5),
                asr = LocalTime.of(15, 25),
                maghrib = LocalTime.of(18, 0),
                isha = LocalTime.of(19, 15),
                sunrise = LocalTime.of(6, 10),
                sunset = LocalTime.of(18, 2),
                firstthird = LocalTime.of(0, 45),
                midnight = LocalTime.of(1, 30),
                lastthird = LocalTime.of(2, 15)
            )
        }
        prayerDayDao.upsertMonth(monthId, days)
    }

    private fun validCalendarResponse(): String {
        return """
        {
          "code": 200,
          "status": "OK",
          "data": [
            {
              "timings": {
                "Fajr": "05:00",
                "Sunrise": "06:10",
                "Dhuhr": "12:05",
                "Asr": "15:25",
                "Maghrib": "18:00",
                "Isha": "19:15",
                "Sunset": "18:02",
                "Firstthird": "00:45",
                "Midnight": "01:30",
                "Lastthird": "02:15"
              },
              "date": { 
                "gregorian": { 
                  "date":"01-01-2026",
                  "day":"01",
                  "month":{"number":1,"en":"January"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
          ]
        }
        """.trimIndent()
    }

    //endregion
}
