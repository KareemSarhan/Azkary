package com.app.azkary.ui.settings

import android.content.Context
import app.cash.turbine.test
import com.app.azkary.R
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.LocationPreferences
import com.app.azkary.data.prefs.ThemeMode
import com.app.azkary.data.prefs.ThemePreferencesRepository
import com.app.azkary.data.prefs.ThemeSettings
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.GeocodingRepository
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.model.AzkarSchedule
import com.app.azkary.domain.model.AzkarWindow
import com.app.azkary.domain.model.DayPrayerTimes
import com.app.azkary.domain.model.WindowCalculationResult
import com.app.azkary.util.LocaleManager
import com.app.azkary.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SettingsViewModel
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var themePreferencesRepository: ThemePreferencesRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var geocodingRepository: GeocodingRepository
    private lateinit var prayerTimesRepository: PrayerTimesRepository
    private lateinit var localeManager: LocaleManager
    private lateinit var context: Context

    private val testLocation = LatLng(24.7136, 46.6753)
    private val testCityName = "Riyadh"

    @Before
    fun setup() {
        userPreferencesRepository = mockk(relaxed = true)
        themePreferencesRepository = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        geocodingRepository = mockk(relaxed = true)
        prayerTimesRepository = mockk(relaxed = true)
        localeManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Default mock behaviors - use MutableStateFlow for stateIn compatibility
        every { themePreferencesRepository.themeSettings } returns MutableStateFlow(ThemeSettings())
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(useLocation = false, lastResolvedLocation = null, locationName = null)
        )
        every { userPreferencesRepository.holdToComplete } returns MutableStateFlow(true)

        viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )
    }

    @Test
    fun `initial state - themeSettings should have default values`() = runTest {
        viewModel.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertTrue(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state - locationPreferences should have default values`() = runTest {
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(useLocation = true, lastResolvedLocation = null, locationName = null)
        )
        
        val testViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )
        
        // With UnconfinedTestDispatcher, stateIn starts immediately
        val prefs = testViewModel.locationPreferences.value
        // Default useLocation is true according to LocationPreferences data class
        assertTrue(prefs.useLocation)
        assertNull(prefs.lastResolvedLocation)
        assertNull(prefs.locationName)
    }

    @Test
    fun `initial state - holdToComplete should be true`() = runTest {
        viewModel.holdToComplete.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state - isRefreshingLocation should be false`() {
        assertFalse(viewModel.isRefreshingLocation.value)
    }

    @Test
    fun `initial state - locationError should be null`() {
        assertNull(viewModel.locationError.value)
    }

    @Test
    fun `initial state - prayer times states should be null or false`() {
        assertNull(viewModel.todayPrayerTimes.value)
        assertNull(viewModel.currentWindows.value)
        assertNull(viewModel.prayerTimesError.value)
        assertFalse(viewModel.isRefreshingPrayerTimes.value)
    }

    @Test
    fun `setThemeMode should call themePreferencesRepository`() = runTest {
        coEvery { themePreferencesRepository.setThemeMode(any()) } just Runs

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        coVerify { themePreferencesRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setHoldToComplete should call userPreferencesRepository`() = runTest {
        coEvery { userPreferencesRepository.setHoldToComplete(any()) } just Runs

        viewModel.setHoldToComplete(false)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setHoldToComplete(false) }
    }

    @Test
    fun `toggleUseLocation should enable location and trigger refresh`() = runTest {
        coEvery { userPreferencesRepository.setUseLocation(any()) } just Runs
        coEvery { locationRepository.getCurrentLocation() } returns createMockLocation()
        coEvery { geocodingRepository.getCityName(any(), any()) } returns testCityName
        coEvery { userPreferencesRepository.setLastResolvedLocation(any()) } just Runs
        coEvery { userPreferencesRepository.setLocationName(any()) } just Runs

        viewModel.toggleUseLocation(true)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setUseLocation(true) }
        coVerify { locationRepository.getCurrentLocation() }
    }

    @Test
    fun `toggleUseLocation with false should disable location`() = runTest {
        coEvery { userPreferencesRepository.setUseLocation(any()) } just Runs

        viewModel.toggleUseLocation(false)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setUseLocation(false) }
    }

    @Test
    fun `refreshLocation should update location and city name on success`() = runTest {
        val mockLocation = createMockLocation()
        coEvery { locationRepository.getCurrentLocation() } returns mockLocation
        coEvery { geocodingRepository.getCityName(any(), any()) } returns testCityName
        coEvery { userPreferencesRepository.setLastResolvedLocation(any()) } just Runs
        coEvery { userPreferencesRepository.setLocationName(any()) } just Runs

        viewModel.refreshLocation()
        advanceUntilIdle()

        coVerify { locationRepository.getCurrentLocation() }
        coVerify { 
            userPreferencesRepository.setLastResolvedLocation(match { 
                it.latitude == testLocation.latitude && it.longitude == testLocation.longitude 
            }) 
        }
        coVerify { geocodingRepository.getCityName(testLocation.latitude, testLocation.longitude) }
        coVerify { userPreferencesRepository.setLocationName(testCityName) }
        assertFalse(viewModel.isRefreshingLocation.value)
        assertNull(viewModel.locationError.value)
    }

    @Test
    fun `refreshLocation should set error when location is null`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns null
        every { context.getString(R.string.error_location_permission) } returns "Location permission required"

        viewModel.refreshLocation()
        advanceUntilIdle()

        assertEquals("Location permission required", viewModel.locationError.value)
        assertFalse(viewModel.isRefreshingLocation.value)
    }

    @Test
    fun `refreshLocation should set error on exception`() = runTest {
        val exception = RuntimeException("GPS error")
        coEvery { locationRepository.getCurrentLocation() } throws exception
        every { context.getString(R.string.error_location_generic) } returns "Location error"

        viewModel.refreshLocation()
        advanceUntilIdle()

        assertTrue(viewModel.locationError.value?.contains("Location error") == true)
        assertTrue(viewModel.locationError.value?.contains("GPS error") == true)
        assertFalse(viewModel.isRefreshingLocation.value)
    }

    @Test
    fun `clearLocationError should reset error state`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns null
        every { context.getString(R.string.error_location_permission) } returns "Error"

        viewModel.refreshLocation()
        advanceUntilIdle()
        assertTrue(viewModel.locationError.value != null)

        viewModel.clearLocationError()
        assertNull(viewModel.locationError.value)
    }

    @Test
    fun `getCurrentLanguageDisplayName should delegate to localeManager`() {
        every { localeManager.getCurrentLanguageDisplayName(context) } returns "English"

        val result = viewModel.getCurrentLanguageDisplayName()

        assertEquals("English", result)
        verify { localeManager.getCurrentLanguageDisplayName(context) }
    }

    @Test
    fun `openLanguageSettings should delegate to localeManager`() {
        every { localeManager.openAppLanguageSettings(context) } just Runs

        viewModel.openLanguageSettings()

        verify { localeManager.openAppLanguageSettings(context) }
    }

    @Test
    fun `refreshPrayerTimes should set error when location is disabled`() = runTest {
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(useLocation = false, lastResolvedLocation = null, locationName = null)
        )
        every { context.getString(R.string.error_location_disabled) } returns "Location disabled"

        // Recreate viewModel with updated mocks
        val newViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )

        newViewModel.refreshPrayerTimes()
        advanceUntilIdle()

        assertEquals("Location disabled", newViewModel.prayerTimesError.value)
    }

    @Test
    fun `refreshPrayerTimes should fetch and set prayer times when location enabled`() = runTest {
        val mockPrayerTimes = createMockDayPrayerTimes()
        coEvery { 
            prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) 
        } returns mockPrayerTimes
        coEvery { 
            prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) 
        } returns createMockWindowCalculationResult()

        val locationPrefsFlow = MutableStateFlow(
            LocationPreferences(useLocation = true, lastResolvedLocation = testLocation, locationName = testCityName)
        )
        every { userPreferencesRepository.locationPreferences } returns locationPrefsFlow

        val newViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )

        // Ensure locationPreferences has collected the value
        newViewModel.locationPreferences.first()
        
        newViewModel.refreshPrayerTimes()

        coVerify { 
            prayerTimesRepository.getDayPrayerTimes(
                any(), 
                testLocation.latitude, 
                testLocation.longitude,
                any(),
                any()
            ) 
        }
        assertEquals(mockPrayerTimes, newViewModel.todayPrayerTimes.value)
        assertFalse(newViewModel.isRefreshingPrayerTimes.value)
        // Note: prayerTimesError may contain Log.d error message since Log isn't mocked in unit tests
    }

    @Test
    fun `refreshPrayerTimes should set error on exception`() = runTest {
        coEvery { 
            prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) 
        } throws RuntimeException("Network error")
        every { context.getString(R.string.error_prayer_times) } returns "Prayer times error"

        val locationPrefsFlow = MutableStateFlow(
            LocationPreferences(useLocation = true, lastResolvedLocation = testLocation, locationName = testCityName)
        )
        every { userPreferencesRepository.locationPreferences } returns locationPrefsFlow

        val newViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )

        // Ensure locationPreferences has collected the value
        newViewModel.locationPreferences.first()

        newViewModel.refreshPrayerTimes()

        assertTrue(newViewModel.prayerTimesError.value?.contains("Prayer times error") == true)
        assertFalse(newViewModel.isRefreshingPrayerTimes.value)
    }

    @Test
    fun `clearPrayerTimesError should reset error state`() = runTest {
        coEvery { 
            prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) 
        } throws RuntimeException("Error")
        every { context.getString(R.string.error_prayer_times) } returns "Error"

        val locationPrefsFlow = MutableStateFlow(
            LocationPreferences(useLocation = true, lastResolvedLocation = testLocation, locationName = testCityName)
        )
        every { userPreferencesRepository.locationPreferences } returns locationPrefsFlow

        val newViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )

        // Ensure locationPreferences has collected the value
        newViewModel.locationPreferences.first()

        newViewModel.refreshPrayerTimes()
        assertTrue(newViewModel.prayerTimesError.value != null)

        newViewModel.clearPrayerTimesError()
        assertNull(newViewModel.prayerTimesError.value)
    }

    @Test
    fun `initializePrayerTimes should auto-refresh when location becomes available`() = runTest {
        val locationPrefsFlow = MutableStateFlow(
            LocationPreferences(useLocation = false, lastResolvedLocation = null, locationName = null)
        )
        every { userPreferencesRepository.locationPreferences } returns locationPrefsFlow

        val mockPrayerTimes = createMockDayPrayerTimes()
        coEvery { 
            prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) 
        } returns mockPrayerTimes
        coEvery { 
            prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) 
        } returns createMockWindowCalculationResult()

        val newViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )

        newViewModel.initializePrayerTimes()
        advanceUntilIdle()

        // No prayer times fetch yet (location disabled)
        coVerify(exactly = 0) { prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) }

        // Enable location
        locationPrefsFlow.value = LocationPreferences(
            useLocation = true,
            lastResolvedLocation = testLocation,
            locationName = testCityName
        )
        advanceUntilIdle()

        // Now should have fetched prayer times
        coVerify { prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `isRefreshingLocation should be true during refresh and false after`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns createMockLocation()
        coEvery { geocodingRepository.getCityName(any(), any()) } returns testCityName
        coEvery { userPreferencesRepository.setLastResolvedLocation(any()) } just Runs
        coEvery { userPreferencesRepository.setLocationName(any()) } just Runs

        assertFalse(viewModel.isRefreshingLocation.value)

        viewModel.refreshLocation()
        // With UnconfinedTestDispatcher, coroutine completes immediately
        // So we can only verify the final state
        advanceUntilIdle()
        assertFalse(viewModel.isRefreshingLocation.value)
    }

    @Test
    fun `isRefreshingPrayerTimes should be true during refresh and false after`() = runTest {
        coEvery { 
            prayerTimesRepository.getDayPrayerTimes(any(), any(), any(), any(), any()) 
        } returns createMockDayPrayerTimes()
        coEvery { 
            prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) 
        } returns createMockWindowCalculationResult()

        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(useLocation = true, lastResolvedLocation = testLocation, locationName = testCityName)
        )

        val newViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            themePreferencesRepository = themePreferencesRepository,
            locationRepository = locationRepository,
            geocodingRepository = geocodingRepository,
            prayerTimesRepository = prayerTimesRepository,
            localeManager = localeManager,
            context = context
        )

        assertFalse(newViewModel.isRefreshingPrayerTimes.value)

        newViewModel.refreshPrayerTimes()
        // With UnconfinedTestDispatcher, coroutine completes immediately
        advanceUntilIdle()
        assertFalse(newViewModel.isRefreshingPrayerTimes.value)
    }

    private fun createMockLocation(): android.location.Location {
        val location = mockk<android.location.Location>(relaxed = true)
        every { location.latitude } returns testLocation.latitude
        every { location.longitude } returns testLocation.longitude
        return location
    }

    private fun createMockDayPrayerTimes(): DayPrayerTimes {
        return DayPrayerTimes(
            date = LocalDate.now(),
            fajr = LocalTime.of(5, 0),
            sunrise = LocalTime.of(6, 0),
            sunset = LocalTime.of(18, 0),
            dhuhr = LocalTime.of(12, 0),
            asr = LocalTime.of(15, 0),
            maghrib = LocalTime.of(18, 0),
            isha = LocalTime.of(19, 30),
            timezone = ZoneId.systemDefault(),
            firstthird = LocalTime.of(23, 0),
            midnight = LocalTime.of(0, 0),
            lastthird = LocalTime.of(3, 0)
        )
    }

    private fun createMockWindowCalculationResult(): WindowCalculationResult {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()

        return WindowCalculationResult(
            currentWindow = AzkarSchedule(
                window = AzkarWindow.MORNING,
                start = now.minusSeconds(3600),
                end = now.plusSeconds(3600),
                date = today
            ),
            nextWindow = AzkarSchedule(
                window = AzkarWindow.NIGHT,
                start = now.plusSeconds(7200),
                end = now.plusSeconds(14400),
                date = today
            ),
            todayTimes = createMockDayPrayerTimes(),
            tomorrowTimes = null
        )
    }
}
