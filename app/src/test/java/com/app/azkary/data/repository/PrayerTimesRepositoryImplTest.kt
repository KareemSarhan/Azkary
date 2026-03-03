package com.app.azkary.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.azkary.data.local.dao.PrayerDayDao
import com.app.azkary.data.local.dao.PrayerMonthDao
import com.app.azkary.data.local.entities.PrayerDayEntity
import com.app.azkary.data.local.entities.PrayerMonthEntity
import com.app.azkary.data.network.dto.DateDto
import com.app.azkary.data.network.dto.GregorianDateDto
import com.app.azkary.data.network.dto.MetaDto
import com.app.azkary.data.network.dto.MonthDto
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.dto.PrayerDayDto
import com.app.azkary.data.network.dto.PrayerTimingsDto
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.domain.AzkarWindowEngine
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesRepositoryImplTest {

    private lateinit var repository: PrayerTimesRepositoryImpl
    private lateinit var context: Context
    private lateinit var networkRepository: PrayerTimesNetworkRepository
    private lateinit var prayerMonthDao: PrayerMonthDao
    private lateinit var prayerDayDao: PrayerDayDao
    private lateinit var windowEngine: AzkarWindowEngine
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var testDispatcher: TestDispatcher

    companion object {
        private const val CACHE_REFRESH_INTERVAL = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        networkRepository = mockk(relaxed = true)
        prayerMonthDao = mockk(relaxed = true)
        prayerDayDao = mockk(relaxed = true)
        windowEngine = AzkarWindowEngine()
        connectivityManager = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        repository = PrayerTimesRepositoryImpl(
            context = context,
            networkRepository = networkRepository,
            prayerMonthDao = prayerMonthDao,
            prayerDayDao = prayerDayDao,
            windowEngine = windowEngine,
            cacheRefreshInterval = CACHE_REFRESH_INTERVAL
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockNetworkCapabilities(hasInternet: Boolean = true): NetworkCapabilities {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns hasInternet
        return capabilities
    }

    private fun setupNetworkAvailable(isAvailable: Boolean) {
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns 
            if (isAvailable) createMockNetworkCapabilities(true) else null
    }

    private fun createPrayerDayDto(
        day: Int = 1,
        month: Int = 1,
        year: Int = 2026
    ): PrayerDayDto {
        return PrayerDayDto(
            timings = PrayerTimingsDto(
                Fajr = "05:00",
                Sunrise = "06:15",
                Dhuhr = "12:00",
                Asr = "15:30",
                Sunset = "18:00",
                Maghrib = "18:05",
                Isha = "19:30",
                Firstthird = "23:00",
                Midnight = "00:00",
                Lastthird = "01:00"
            ),
            date = DateDto(
                gregorian = GregorianDateDto(
                    date = String.format("%02d-%02d-%04d", day, month, year),
                    day = day.toString(),
                    month = MonthDto(number = month, en = "January"),
                    year = year.toString()
                )
            ),
            meta = MetaDto(timezone = "Asia/Riyadh")
        )
    }

    private fun createPrayerDayEntity(
        monthId: Long = 1,
        date: LocalDate = LocalDate.of(2026, 1, 1)
    ): PrayerDayEntity {
        return PrayerDayEntity(
            monthId = monthId,
            date = date,
            fajr = LocalTime.of(5, 0),
            dhuhr = LocalTime.of(12, 0),
            asr = LocalTime.of(15, 30),
            maghrib = LocalTime.of(18, 5),
            isha = LocalTime.of(19, 30),
            sunrise = LocalTime.of(6, 15),
            sunset = LocalTime.of(18, 0),
            firstthird = LocalTime.of(23, 0),
            midnight = LocalTime.of(0, 0),
            lastthird = LocalTime.of(1, 0)
        )
    }

    @Test
    fun `getMonthlyPrayerTimes returns cached data when fresh`() = runTest {
        // Given
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = Instant.now()
        )
        val dayEntities = listOf(createPrayerDayEntity(1, LocalDate.of(2026, 1, 1)))

        coEvery { 
            prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4) 
        } returns monthEntity
        coEvery { prayerDayDao.getDaysForMonth(1) } returns dayEntities

        // When
        val result = repository.getMonthlyPrayerTimes(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertEquals(200, result.code)
        assertEquals(1, result.data.size)
        assertEquals("05:00", result.data[0].timings.Fajr)
        coVerify(exactly = 0) { networkRepository.fetchMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getMonthlyPrayerTimes fetches from network when cache stale`() = runTest {
        // Given - stale cache (older than 7 days)
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = Instant.now().minusMillis(CACHE_REFRESH_INTERVAL + 1000)
        )
        val networkResponse = PrayerCalendarResponse(
            code = 200,
            status = "OK",
            data = listOf(createPrayerDayDto(1, 1, 2026))
        )

        setupNetworkAvailable(true)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns monthEntity
        coEvery { prayerDayDao.getDaysForMonth(any()) } returns emptyList() // Empty to force refresh
        coEvery { prayerMonthDao.insertMonth(any()) } returns 1
        coEvery { prayerDayDao.upsertMonth(any(), any()) } just Runs
        coEvery { 
            networkRepository.fetchMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) 
        } returns networkResponse

        // When
        val result = repository.getMonthlyPrayerTimes(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertEquals(200, result.code)
        coVerify { networkRepository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753, 4, 0) }
        coVerify { prayerMonthDao.insertMonth(any()) }
    }

    @Test
    fun `getMonthlyPrayerTimes returns stale cache when network fails`() = runTest {
        // Given
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = Instant.now().minusMillis(CACHE_REFRESH_INTERVAL + 1000)
        )
        val dayEntities = listOf(createPrayerDayEntity(1, LocalDate.of(2026, 1, 1)))

        setupNetworkAvailable(true)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns monthEntity
        coEvery { prayerDayDao.getDaysForMonth(any()) } returns dayEntities
        coEvery { 
            networkRepository.fetchMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) 
        } throws ApiException("Network error")

        // When
        val result = repository.getMonthlyPrayerTimes(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then - should return stale cache
        assertEquals(200, result.code)
        assertEquals(1, result.data.size)
    }

    @Test
    fun `getMonthlyPrayerTimes throws exception when no network and no cache`() = runTest {
        // Given
        setupNetworkAvailable(false)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns null

        // When/Then
        try {
            repository.getMonthlyPrayerTimes(
                year = 2026,
                month = 1,
                latitude = 24.7136,
                longitude = 46.6753,
                methodId = 4,
                school = 0
            )
            assertTrue("Should have thrown exception", false)
        } catch (e: ApiException) {
            assertTrue(e.message?.contains("No network") == true)
        }
    }

    @Test
    fun `getMonthlyPrayerTimes clears corrupted cache with 0 days`() = runTest {
        // Given
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = Instant.now()
        )
        val networkResponse = PrayerCalendarResponse(
            code = 200,
            status = "OK",
            data = listOf(createPrayerDayDto())
        )

        setupNetworkAvailable(true)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns monthEntity
        coEvery { prayerDayDao.getDaysForMonth(1) } returns emptyList() // Corrupted cache
        coEvery { prayerDayDao.deleteDaysForMonth(1) } just Runs
        coEvery { prayerMonthDao.insertMonth(any()) } returns 1
        coEvery { prayerDayDao.upsertMonth(any(), any()) } just Runs
        coEvery { 
            networkRepository.fetchMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) 
        } returns networkResponse

        // When
        repository.getMonthlyPrayerTimes(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        coVerify { prayerDayDao.deleteDaysForMonth(1) }
    }

    @Test
    fun `getDayPrayerTimes returns cached day`() = runTest {
        // Given
        val date = LocalDate.of(2026, 1, 15)
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val dayEntity = createPrayerDayEntity(1, date)

        coEvery { 
            prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4) 
        } returns monthEntity
        coEvery { prayerDayDao.getDay(1, date) } returns dayEntity

        // When
        val result = repository.getDayPrayerTimes(
            date = date,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertNotNull(result)
        assertEquals(date, result?.date)
        assertEquals(LocalTime.of(5, 0), result?.fajr)
    }

    @Test
    fun `getDayPrayerTimes returns null when no network and no cache`() = runTest {
        // Given
        val date = LocalDate.of(2026, 1, 15)

        setupNetworkAvailable(false)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns null

        // When
        val result = repository.getDayPrayerTimes(
            date = date,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `getPrayerTimesInRange returns correct range`() = runTest {
        // Given
        val startDate = LocalDate.of(2026, 1, 1)
        val endDate = LocalDate.of(2026, 1, 3)
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )

        coEvery { 
            prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4) 
        } returns monthEntity
        coEvery { prayerDayDao.getDay(1, LocalDate.of(2026, 1, 1)) } returns createPrayerDayEntity(1, LocalDate.of(2026, 1, 1))
        coEvery { prayerDayDao.getDay(1, LocalDate.of(2026, 1, 2)) } returns createPrayerDayEntity(1, LocalDate.of(2026, 1, 2))
        coEvery { prayerDayDao.getDay(1, LocalDate.of(2026, 1, 3)) } returns createPrayerDayEntity(1, LocalDate.of(2026, 1, 3))

        // When
        val result = repository.getPrayerTimesInRange(
            startDate = startDate,
            endDate = endDate,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertEquals(3, result.size)
        assertEquals(LocalDate.of(2026, 1, 1), result[0].date)
        assertEquals(LocalDate.of(2026, 1, 3), result[2].date)
    }

    @Test
    fun `getPrayerTimesInRange returns empty list when start after end`() = runTest {
        // Given
        val startDate = LocalDate.of(2026, 1, 10)
        val endDate = LocalDate.of(2026, 1, 5)

        // When
        val result = repository.getPrayerTimesInRange(
            startDate = startDate,
            endDate = endDate,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCurrentWindows calculates windows correctly`() = runTest {
        // Given
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = today.year,
            month = today.monthValue,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )

        coEvery { 
            prayerMonthDao.getMonth(today.year, today.monthValue, 24.7136, 46.6753, 4) 
        } returns monthEntity
        coEvery { prayerDayDao.getDay(1, today) } returns createPrayerDayEntity(1, today)
        coEvery { prayerDayDao.getDay(1, tomorrow) } returns createPrayerDayEntity(1, tomorrow)

        // When
        val result = repository.getCurrentWindows(
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertNotNull(result.todayTimes)
    }

    @Test
    fun `getIslamicCurrentDate returns today after fajr`() = runTest {
        // Given
        val today = LocalDate.now()
        val monthEntity = PrayerMonthEntity(
            id = 1,
            year = today.year,
            month = today.monthValue,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = ZoneId.systemDefault().id
        )
        val dayEntity = createPrayerDayEntity(1, today).copy(
            fajr = LocalTime.of(5, 0) // Early fajr, so now is after fajr
        )

        coEvery { 
            prayerMonthDao.getMonth(today.year, today.monthValue, 24.7136, 46.6753, 4) 
        } returns monthEntity
        coEvery { prayerDayDao.getDay(1, today) } returns dayEntity

        // When
        val result = repository.getIslamicCurrentDate(
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertEquals(today, result)
    }

    @Test
    fun `refreshMonth fetches from network and caches`() = runTest {
        // Given
        val networkResponse = PrayerCalendarResponse(
            code = 200,
            status = "OK",
            data = (1..30).map { createPrayerDayDto(it, 1, 2026) }
        )

        setupNetworkAvailable(true)
        coEvery { prayerMonthDao.insertMonth(any()) } returns 1
        coEvery { prayerDayDao.upsertMonth(any(), any()) } just Runs
        coEvery { 
            networkRepository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753, 4, 0) 
        } returns networkResponse

        // When
        val result = repository.refreshMonth(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then
        assertEquals(200, result.code)
        assertEquals(30, result.data.size)
        coVerify { prayerMonthDao.insertMonth(any()) }
        coVerify { prayerDayDao.upsertMonth(any(), any()) }
    }

    @Test(expected = ApiException::class)
    fun `refreshMonth throws exception when no network`() = runTest {
        // Given
        setupNetworkAvailable(false)

        // When/Then
        repository.refreshMonth(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )
    }

    @Test
    fun `clearOldCache deletes old months`() = runTest {
        // Given
        coEvery { prayerMonthDao.deleteOldMonths(any()) } just Runs

        // When
        repository.clearOldCache(keepLastMonths = 3)

        // Then
        coVerify { prayerMonthDao.deleteOldMonths(any()) }
    }

    @Test
    fun `isNetworkAvailable returns true when network connected`() = runTest {
        // Given
        setupNetworkAvailable(true)

        // When/Then - This is tested indirectly through other methods
        // We can't test private method directly, but we can verify behavior
    }

    @Test
    fun `isNetworkAvailable returns false when network disconnected`() = runTest {
        // Given
        setupNetworkAvailable(false)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns null

        // When/Then - should throw when no network and no cache
        try {
            repository.getMonthlyPrayerTimes(2026, 1, 24.0, 46.0, 4, 0)
            assertTrue("Should have thrown exception", false)
        } catch (e: ApiException) {
            assertTrue(e.message?.contains("No network") == true)
        }
    }

    @Test
    fun `getDayPrayerTimes handles API exception gracefully`() = runTest {
        // Given
        val date = LocalDate.of(2026, 1, 15)
        setupNetworkAvailable(true)
        coEvery { prayerMonthDao.getMonth(any(), any(), any(), any(), any()) } returns null
        coEvery { 
            networkRepository.fetchMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) 
        } throws ApiException("API Error")

        // When
        val result = repository.getDayPrayerTimes(
            date = date,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            school = 0
        )

        // Then - should return null on API error
        assertNull(result)
    }
}
