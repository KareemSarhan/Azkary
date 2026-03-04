package com.app.azkary.ui.reading

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.domain.IslamicDateProvider
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ReadingViewModel
    private lateinit var repository: AzkarRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var islamicDateProvider: IslamicDateProvider
    private lateinit var localeManager: LocaleManager
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private val testCategoryId = "test-category-123"
    private val testDate = LocalDate.of(2026, 1, 15)

    private val testItems = listOf(
        AzkarItemUi(
            id = "item1",
            title = "Test Item 1",
            arabicText = "السلام عليكم",
            transliteration = "As-salamu alaykum",
            translation = "Peace be upon you",
            reference = "Bukhari",
            requiredRepeats = 3,
            currentRepeats = 0,
            isCompleted = false,
            isInfinite = false
        ),
        AzkarItemUi(
            id = "item2",
            title = "Test Item 2",
            arabicText = "الحمد لله",
            transliteration = "Alhamdulillah",
            translation = "Praise be to Allah",
            reference = "Muslim",
            requiredRepeats = 33,
            currentRepeats = 10,
            isCompleted = false,
            isInfinite = false
        ),
        AzkarItemUi(
            id = "item3",
            title = "Infinite Item",
            arabicText = "سبحان الله",
            transliteration = "SubhanAllah",
            translation = "Glory be to Allah",
            reference = null,
            requiredRepeats = 0,
            currentRepeats = 100,
            isCompleted = false,
            isInfinite = true
        )
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        islamicDateProvider = mockk(relaxed = true)
        localeManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle().apply {
            set("categoryId", testCategoryId)
        }

        // Default mock behaviors - use MutableStateFlow for stateIn compatibility
        every { localeManager.currentLangTagFlow } returns MutableStateFlow("en")
        every { userPreferencesRepository.holdToComplete } returns MutableStateFlow(true)
        every { userPreferencesRepository.vibrationEnabled } returns MutableStateFlow(true)
        coEvery { islamicDateProvider.getCurrentDate() } returns testDate
        every { 
            repository.observeItemsForCategory(any(), any(), any()) 
        } returns flowOf(testItems)
        every { 
            repository.getWeightedProgress(any(), any(), any()) 
        } returns flowOf(0.5f)

        viewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `categoryId should be retrieved from SavedStateHandle`() {
        assertEquals(testCategoryId, viewModel.categoryId)
    }

    @Test
    fun `categoryId should be null when not in SavedStateHandle`() {
        val emptySavedStateHandle = SavedStateHandle()
        val viewModelWithoutCategory = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = emptySavedStateHandle
        )

        assertEquals(null, viewModelWithoutCategory.categoryId)
    }

    @Test
    fun `holdToComplete should emit value from user preferences`() = runTest {
        val holdToCompleteFlow = MutableStateFlow(true)
        every { userPreferencesRepository.holdToComplete } returns holdToCompleteFlow

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.holdToComplete.test {
            assertEquals(true, awaitItem())

            holdToCompleteFlow.value = false
            assertEquals(false, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `items should emit list from repository`() = runTest {
        viewModel.items.test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("item1", items[0].id)
            assertEquals("item2", items[1].id)
            assertEquals("item3", items[2].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `items should be empty when categoryId is null`() = runTest {
        val emptySavedStateHandle = SavedStateHandle()
        val viewModelWithoutCategory = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = emptySavedStateHandle
        )

        viewModelWithoutCategory.items.test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `items should update when locale changes`() = runTest {
        val langFlow = MutableStateFlow("en")
        every { localeManager.currentLangTagFlow } returns langFlow

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.items.test {
            // Initial emission
            awaitItem()

            // Change language
            langFlow.value = "ar"

            // Should trigger new emission
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weightedProgress should emit progress from repository`() = runTest {
        every { repository.getWeightedProgress(any(), any(), any()) } returns flowOf(0.75f)

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.weightedProgress.test {
            assertEquals(0.75f, awaitItem(), 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weightedProgress should be zero when categoryId is null`() = runTest {
        val emptySavedStateHandle = SavedStateHandle()
        val viewModelWithoutCategory = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = emptySavedStateHandle
        )

        viewModelWithoutCategory.weightedProgress.test {
            assertEquals(0f, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `incrementRepeat should call repository with correct parameters`() = runTest {
        coEvery { repository.incrementRepeat(any(), any(), any()) } just Runs

        viewModel.incrementRepeat("item1")
        advanceUntilIdle()

        coVerify { 
            repository.incrementRepeat(testCategoryId, "item1", testDate.toString()) 
        }
    }

    @Test
    fun `incrementRepeat should do nothing when categoryId is null`() = runTest {
        val emptySavedStateHandle = SavedStateHandle()
        val viewModelWithoutCategory = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = emptySavedStateHandle
        )

        viewModelWithoutCategory.incrementRepeat("item1")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.incrementRepeat(any(), any(), any()) }
    }

    @Test
    fun `markItemComplete should call repository with correct parameters`() = runTest {
        coEvery { repository.markItemComplete(any(), any(), any()) } just Runs

        viewModel.markItemComplete("item1")
        advanceUntilIdle()

        coVerify { 
            repository.markItemComplete(testCategoryId, "item1", testDate.toString()) 
        }
    }

    @Test
    fun `markItemComplete should do nothing when categoryId is null`() = runTest {
        val emptySavedStateHandle = SavedStateHandle()
        val viewModelWithoutCategory = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = emptySavedStateHandle
        )

        viewModelWithoutCategory.markItemComplete("item1")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.markItemComplete(any(), any(), any()) }
    }

    @Test
    fun `incrementRepeat should use current date from islamicDateProvider`() = runTest {
        val customDate = LocalDate.of(2026, 3, 15)
        coEvery { islamicDateProvider.getCurrentDate() } returns customDate
        coEvery { repository.incrementRepeat(any(), any(), any()) } just Runs

        // Recreate viewModel with custom date
        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.incrementRepeat("item1")
        advanceUntilIdle()

        coVerify { repository.incrementRepeat(testCategoryId, "item1", customDate.toString()) }
    }

    @Test
    fun `items should observe correct language tag from localeManager`() = runTest {
        val langFlow = MutableStateFlow("en")
        every { localeManager.currentLangTagFlow } returns langFlow

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.items.test {
            awaitItem()

            // Verify repository was called with correct language
            verify { repository.observeItemsForCategory(testCategoryId, "en", testDate.toString()) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `items should filter transliteration for Arabic locale`() = runTest {
        val arabicItems = listOf(
            AzkarItemUi(
                id = "item1",
                title = "Test",
                arabicText = "السلام عليكم",
                transliteration = null, // Should be null for Arabic
                translation = "Peace",
                reference = "Bukhari",
                requiredRepeats = 3,
                currentRepeats = 0,
                isCompleted = false,
                isInfinite = false
            )
        )
        every { localeManager.currentLangTagFlow } returns MutableStateFlow("ar")
        every { 
            repository.observeItemsForCategory(any(), any(), any()) 
        } returns flowOf(arabicItems)

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.items.test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(null, items[0].transliteration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `vibrationEnabled should emit value from user preferences`() = runTest {
        val vibrationEnabledFlow = MutableStateFlow(true)
        every { userPreferencesRepository.vibrationEnabled } returns vibrationEnabledFlow

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.vibrationEnabled.test {
            assertEquals(true, awaitItem())

            vibrationEnabledFlow.value = false
            assertEquals(false, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `vibrationEnabled should have default value true`() = runTest {
        viewModel.vibrationEnabled.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleVibration should call setVibrationEnabled with inverted value when currently enabled`() = runTest {
        every { userPreferencesRepository.vibrationEnabled } returns MutableStateFlow(true)
        coEvery { userPreferencesRepository.setVibrationEnabled(any()) } just Runs

        viewModel.toggleVibration()
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setVibrationEnabled(false) }
    }

    @Test
    fun `toggleVibration should call setVibrationEnabled with inverted value when currently disabled`() = runTest {
        val vibrationEnabledFlow = MutableStateFlow(false)
        every { userPreferencesRepository.vibrationEnabled } returns vibrationEnabledFlow
        coEvery { userPreferencesRepository.setVibrationEnabled(any()) } just Runs

        val newViewModel = ReadingViewModel(
            repository = repository,
            localeManager = localeManager,
            islamicDateProvider = islamicDateProvider,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        newViewModel.vibrationEnabled.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        newViewModel.toggleVibration()
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setVibrationEnabled(true) }
    }
}
