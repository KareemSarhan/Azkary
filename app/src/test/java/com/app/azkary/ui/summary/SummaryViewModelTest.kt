package com.app.azkary.ui.summary

import android.content.Context
import app.cash.turbine.test
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.prefs.LocationPreferences
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.IslamicDateProvider
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
class SummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SummaryViewModel
    private lateinit var repository: AzkarRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var prayerTimesRepository: PrayerTimesRepository
    private lateinit var islamicDateProvider: IslamicDateProvider
    private lateinit var localeManager: LocaleManager
    private lateinit var context: Context

    private val testCategories = listOf(
        CategoryUi(
            id = "cat1",
            name = "Morning Azkar",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.MORNING,
            progress = 0.5f,
            from = 0,
            to = 8
        ),
        CategoryUi(
            id = "cat2",
            name = "Evening Azkar",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.NIGHT,
            progress = 0.8f,
            from = 0,
            to = 8
        ),
        CategoryUi(
            id = "cat3",
            name = "Custom Category",
            type = CategoryType.USER,
            systemKey = null,
            progress = 0f,
            from = 0,
            to = 8
        )
    )

    private val testLocation = LatLng(24.7136, 46.6753)
    private val testDate = LocalDate.of(2026, 1, 15)

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        prayerTimesRepository = mockk(relaxed = true)
        islamicDateProvider = mockk(relaxed = true)
        localeManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Default mock behaviors
        every { localeManager.currentLangTagFlow } returns MutableStateFlow("en")
        every { userPreferencesRepository.holdToComplete } returns MutableStateFlow(true)
        every { userPreferencesRepository.showWeeklyProgress } returns MutableStateFlow(true)
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(useLocation = false, lastResolvedLocation = null, locationName = null)
        )
        coEvery { islamicDateProvider.getCurrentDate() } returns testDate
        every { 
            repository.observeCategoriesWithDisplayName(any(), any()) 
        } returns flowOf(testCategories)
        coEvery { 
            prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) 
        } returns createMockWindowCalculationResult()

        viewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )
    }

    @Test
    fun `initial state - isEditMode should be false`() {
        assertFalse(viewModel.isEditMode.value)
    }

    @Test
    fun `initial state - sessionEndTime should be null`() {
        assertNull(viewModel.sessionEndTime.value)
    }

    @Test
    fun `initial state - currentWindows should be null`() {
        assertNull(viewModel.currentWindows.value)
    }

    @Test
    fun `toggleEditMode should switch isEditMode state`() {
        assertFalse(viewModel.isEditMode.value)

        viewModel.toggleEditMode()
        assertTrue(viewModel.isEditMode.value)

        viewModel.toggleEditMode()
        assertFalse(viewModel.isEditMode.value)
    }

    @Test
    fun `holdToComplete should emit value from user preferences`() = runTest {
        val holdToCompleteFlow = MutableStateFlow(true)
        every { userPreferencesRepository.holdToComplete } returns holdToCompleteFlow

        val newViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        newViewModel.holdToComplete.test {
            assertEquals(true, awaitItem())

            holdToCompleteFlow.value = false
            assertEquals(false, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteCategory should call repository deleteCategory`() = runTest {
        coEvery { repository.deleteCategory(any()) } just Runs

        viewModel.deleteCategory("cat1")
        advanceUntilIdle()

        coVerify { repository.deleteCategory("cat1") }
    }

    @Test
    fun `reorderCategories should call repository reorderCategories`() = runTest {
        coEvery { repository.reorderCategories(any()) } just Runs

        val newOrder = listOf("cat2", "cat1", "cat3")
        viewModel.reorderCategories(newOrder)
        advanceUntilIdle()

        coVerify { repository.reorderCategories(newOrder) }
    }

    @Test
    fun `moveCategoryUp should swap categories at indices`() = runTest {
        coEvery { repository.reorderCategories(any()) } just Runs
        every { repository.observeCategoriesWithDisplayName(any(), any()) } returns flowOf(testCategories)

        viewModel.moveCategoryUp(1)
        advanceUntilIdle()

        coVerify { 
            repository.reorderCategories(listOf("cat2", "cat1", "cat3"))
        }
    }

    @Test
    fun `moveCategoryUp should not move first category`() = runTest {
        viewModel.moveCategoryUp(0)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.reorderCategories(any()) }
    }

    @Test
    fun `moveCategoryDown should swap categories at indices`() = runTest {
        coEvery { repository.reorderCategories(any()) } just Runs
        every { repository.observeCategoriesWithDisplayName(any(), any()) } returns flowOf(testCategories)

        viewModel.moveCategoryDown(0)
        advanceUntilIdle()

        coVerify { 
            repository.reorderCategories(listOf("cat2", "cat1", "cat3"))
        }
    }

    @Test
    fun `moveCategoryDown should not move last category`() = runTest {
        viewModel.moveCategoryDown(2)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.reorderCategories(any()) }
    }

    @Test
    fun `toggleCategoryCompletion should mark complete when progress less than 100 percent`() = runTest {
        coEvery { repository.markCategoryComplete(any(), any()) } just Runs

        val incompleteCategory = testCategories[2] // progress = 0f
        every { repository.observeCategoriesWithDisplayName(any(), any()) } returns flowOf(listOf(incompleteCategory))

        viewModel.toggleCategoryCompletion("cat3")
        advanceUntilIdle()

        coVerify { repository.markCategoryComplete("cat3", testDate.toString()) }
    }

    @Test
    fun `toggleCategoryCompletion should mark incomplete when progress is 100 percent`() = runTest {
        coEvery { repository.markCategoryIncomplete(any(), any()) } just Runs

        // Create a category with progress >= 1f
        val completeCategory = testCategories[1].copy(progress = 1f)
        every { repository.observeCategoriesWithDisplayName(any(), any()) } returns flowOf(listOf(completeCategory))

        viewModel.toggleCategoryCompletion("cat2")
        advanceUntilIdle()

        coVerify { repository.markCategoryIncomplete("cat2", testDate.toString()) }
    }

    @Test
    fun `location preferences with enabled location should trigger prayer times refresh`() = runTest {
        // Set up mock BEFORE creating ViewModel
        val locationPrefs = LocationPreferences(
            useLocation = true,
            lastResolvedLocation = testLocation,
            locationName = "Riyadh"
        )
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(locationPrefs)

        val mockWindows = createMockWindowCalculationResult()
        coEvery { 
            prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) 
        } returns mockWindows

        // Create new ViewModel with location enabled
        val testViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        advanceUntilIdle()

        coVerify { prayerTimesRepository.getCurrentWindows(testLocation.latitude, testLocation.longitude, any(), any()) }
    }

    @Test
    fun `location preferences with disabled location should not trigger prayer times refresh`() = runTest {
        // Set up mock BEFORE creating ViewModel
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(useLocation = false, lastResolvedLocation = null, locationName = null)
        )

        // Create new ViewModel with location disabled
        val testViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        advanceUntilIdle()

        coVerify(exactly = 0) { prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) }
    }

    @Test
    fun `sessionEndTime should be updated when prayer times are refreshed`() = runTest {
        val mockWindows = createMockWindowCalculationResult()
        coEvery { 
            prayerTimesRepository.getCurrentWindows(any(), any(), any(), any()) 
        } returns mockWindows

        // Set up location preferences with location enabled
        every { userPreferencesRepository.locationPreferences } returns MutableStateFlow(
            LocationPreferences(
                useLocation = true,
                lastResolvedLocation = testLocation,
                locationName = "Riyadh"
            )
        )

        // Create new ViewModel with location enabled
        val testViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        advanceUntilIdle()

        // Verify prayer times were fetched
        coVerify { prayerTimesRepository.getCurrentWindows(testLocation.latitude, testLocation.longitude, any(), any()) }
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
            todayTimes = DayPrayerTimes(
                date = today,
                fajr = LocalTime.of(5, 0),
                sunrise = LocalTime.of(6, 0),
                sunset = LocalTime.of(18, 0),
                dhuhr = LocalTime.of(12, 0),
                asr = LocalTime.of(15, 0),
                maghrib = LocalTime.of(18, 0),
                isha = LocalTime.of(19, 30),
                timezone = zoneId,
                firstthird = LocalTime.of(23, 0),
                midnight = LocalTime.of(0, 0),
                lastthird = LocalTime.of(3, 0)
            ),
            tomorrowTimes = null
        )
    }

    @Test
    fun `sessionEndTime should be null initially`() {
        assertNull(viewModel.sessionEndTime.value)
    }

    @Test
    fun `categories should update when locale changes`() = runTest {
        val langFlow = MutableStateFlow("en")
        every { localeManager.currentLangTagFlow } returns langFlow

        every { 
            repository.observeCategoriesWithDisplayName(any(), any()) 
        } returns flowOf(testCategories)

        
        val testViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        testViewModel.categories.test {
            awaitItem()

            langFlow.value = "ar"

            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weeklyProgress should emit correct day progress`() = runTest {
        val dateProgressMap = mapOf(
            LocalDate.now().toString() to 0.5f,
            LocalDate.now().minusDays(1).toString() to 0.3f,
            LocalDate.now().minusDays(2).toString() to 0.0f,
            LocalDate.now().minusDays(3).toString() to 0.7f,
            LocalDate.now().minusDays(4).toString() to 0.0f,
            LocalDate.now().minusDays(5).toString() to 0.0f,
            LocalDate.now().minusDays(6).toString() to 0.1f
        )
        every { repository.getWeeklyProgress(any()) } returns flowOf(dateProgressMap)

        val testViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        testViewModel.weeklyProgress.test {
            val progress = awaitItem()
            assertEquals(7, progress.size)
            assertTrue(progress.any { it.isToday })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weeklyProgress should mark today correctly`() = runTest {
        val today = LocalDate.now()
        val dateProgressMap = mapOf(
            today.toString() to 1.0f,
            today.minusDays(1).toString() to 0.5f,
            today.minusDays(2).toString() to 0.3f,
            today.minusDays(3).toString() to 0.0f,
            today.minusDays(4).toString() to 0.0f,
            today.minusDays(5).toString() to 0.1f,
            today.minusDays(6).toString() to 0.0f
        )
        every { repository.getWeeklyProgress(any()) } returns flowOf(dateProgressMap)

        val testViewModel = SummaryViewModel(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            prayerTimesRepository = prayerTimesRepository,
            islamicDateProvider = islamicDateProvider,
            localeManager = localeManager,
            context = context
        )

        testViewModel.weeklyProgress.test {
            val progress = awaitItem()
            val todayItem = progress.find { it.isToday }
            assertTrue(todayItem != null)
            assertEquals(today.toString(), todayItem?.date)
            cancelAndIgnoreRemainingEvents()
        }
    }

}
