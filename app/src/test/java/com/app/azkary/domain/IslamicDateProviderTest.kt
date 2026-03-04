package com.app.azkary.domain

import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for IslamicDateProvider
 *
 * Tests cover:
 * - Getting Islamic date when location is enabled
 * - Fallback to Gregorian date when location is disabled
 * - Fallback to Gregorian date when location is null
 * - Fallback to Gregorian date on API error
 * - Proper repository interaction
 */
class IslamicDateProviderTest {

    private lateinit var islamicDateProvider: IslamicDateProvider
    private lateinit var prayerTimesRepository: PrayerTimesRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        prayerTimesRepository = mockk()
        userPreferencesRepository = mockk()
        islamicDateProvider = IslamicDateProvider(prayerTimesRepository, userPreferencesRepository)
    }

    @Test
    fun `getCurrentDate returns Islamic date from API when location is enabled`() = runTest {
        // Arrange
        val mockLocation = mockk<LatLng> {
            every { latitude } returns 30.0444
            every { longitude } returns 31.2357
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        val expectedIslamicDate = LocalDate.of(1447, 8, 15) // Example Islamic date

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(30.0444, 31.2357)
        } returns expectedIslamicDate

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert
        assertEquals(expectedIslamicDate, result)
        coVerify { prayerTimesRepository.getIslamicCurrentDate(30.0444, 31.2357) }
    }

    @Test
    fun `getCurrentDate returns Gregorian date when location is disabled`() = runTest {
        // Arrange
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns false
            every { lastResolvedLocation } returns null
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert
        assertEquals(LocalDate.now(), result)
        coVerify(exactly = 0) { prayerTimesRepository.getIslamicCurrentDate(any(), any()) }
    }

    @Test
    fun `getCurrentDate returns Gregorian date when location is null`() = runTest {
        // Arrange
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns null
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert
        assertEquals(LocalDate.now(), result)
        coVerify(exactly = 0) { prayerTimesRepository.getIslamicCurrentDate(any(), any()) }
    }

    @Test
    fun `getCurrentDate returns Gregorian date on API exception`() = runTest {
        // Arrange
        val mockLocation = mockk<LatLng> {
            every { latitude } returns 30.0444
            every { longitude } returns 31.2357
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(any(), any())
        } throws RuntimeException("Network error")

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert
        assertEquals(LocalDate.now(), result)
    }

    @Test
    fun `getCurrentDate calls repository with correct coordinates`() = runTest {
        // Arrange
        val testLat = 21.3891
        val testLng = 39.8579

        val mockLocation = mockk<LatLng> {
            every { latitude } returns testLat
            every { longitude } returns testLng
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(testLat, testLng)
        } returns LocalDate.of(1447, 8, 15)

        // Act
        islamicDateProvider.getCurrentDate()

        // Assert
        coVerify { prayerTimesRepository.getIslamicCurrentDate(testLat, testLng) }
    }

    @Test
    fun `getCurrentDate handles negative coordinates`() = runTest {
        // Arrange - Test with coordinates in Western/Southern hemisphere
        val mockLocation = mockk<LatLng> {
            every { latitude } returns -33.8688
            every { longitude } returns 151.2093
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(-33.8688, 151.2093)
        } returns LocalDate.of(1447, 8, 15)

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert
        assertEquals(LocalDate.of(1447, 8, 15), result)
    }

    @Test
    fun `getCurrentDate handles edge case coordinates`() = runTest {
        // Arrange - Test with coordinates near 0,0
        val mockLocation = mockk<LatLng> {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(0.0, 0.0)
        } returns LocalDate.of(1447, 8, 15)

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert
        assertEquals(LocalDate.of(1447, 8, 15), result)
    }

    @Test
    fun `getCurrentDate handles IO exception`() = runTest {
        // Arrange
        val mockLocation = mockk<LatLng> {
            every { latitude } returns 30.0444
            every { longitude } returns 31.2357
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(any(), any())
        } throws java.io.IOException("Connection timeout")

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert - Should fallback to Gregorian date
        assertEquals(LocalDate.now(), result)
    }

    @Test
    fun `getCurrentDate handles illegal state exception`() = runTest {
        // Arrange
        val mockLocation = mockk<LatLng> {
            every { latitude } returns 30.0444
            every { longitude } returns 31.2357
        }
        val locationPrefs = mockk<com.app.azkary.data.prefs.LocationPreferences> {
            every { useLocation } returns true
            every { lastResolvedLocation } returns mockLocation
        }

        every { userPreferencesRepository.locationPreferences } returns flowOf(locationPrefs)
        coEvery {
            prayerTimesRepository.getIslamicCurrentDate(any(), any())
        } throws IllegalStateException("Invalid state")

        // Act
        val result = islamicDateProvider.getCurrentDate()

        // Assert - Should fallback to Gregorian date
        assertEquals(LocalDate.now(), result)
    }
}
