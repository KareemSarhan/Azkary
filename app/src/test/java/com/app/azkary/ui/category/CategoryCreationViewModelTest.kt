package com.app.azkary.ui.category

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.azkary.R
import com.app.azkary.data.model.AvailableZikr
import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.CategoryItemConfig
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.util.LocaleManager
import com.app.azkary.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryCreationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CategoryCreationViewModel
    private lateinit var repository: AzkarRepository
    private lateinit var localeManager: LocaleManager
    private lateinit var context: Context

    private val testAvailableItems = listOf(
        AvailableZikr(
            id = "zikr1",
            title = "Morning Zikr",
            arabicText = "السلام عليكم",
            transliteration = "As-salamu alaykum",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED
        ),
        AvailableZikr(
            id = "zikr2",
            title = "Evening Zikr",
            arabicText = "الحمد لله",
            transliteration = "Alhamdulillah",
            requiredRepeats = 33,
            source = AzkarSource.SEEDED
        ),
        AvailableZikr(
            id = "zikr3",
            title = "Custom Zikr",
            arabicText = "سبحان الله",
            transliteration = "SubhanAllah",
            requiredRepeats = 100,
            source = AzkarSource.USER
        )
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        localeManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Default mock behaviors
        every { localeManager.currentLangTagFlow } returns flowOf("en")
        every { localeManager.getCurrentLanguageTag(context) } returns "en"
        every { repository.observeAvailableItems(any()) } returns flowOf(testAvailableItems)

        viewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `initial state - uiState should have default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.categoryName)
            assertEquals("", state.searchQuery)
            assertTrue(state.selectedItems.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.isStockCategory)
            assertEquals(0, state.from)
            assertEquals(8, state.to)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `categoryId should be null for new category creation`() {
        assertNull(viewModel.categoryId)
    }

    @Test
    fun `categoryId should be retrieved from SavedStateHandle when editing`() {
        val savedStateHandle = SavedStateHandle().apply {
            set("categoryId", "existing-category-123")
        }

        val editViewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = savedStateHandle
        )

        assertEquals("existing-category-123", editViewModel.categoryId)
    }

    @Test
    fun `onCategoryNameChange should update categoryName in uiState`() = runTest {
        viewModel.onCategoryNameChange("My Custom Category")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("My Custom Category", state.categoryName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchQueryChange should update searchQuery in uiState`() = runTest {
        viewModel.onSearchQueryChange("morning")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("morning", state.searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemSelect should add item to selectedItems`() = runTest {
        val zikr = testAvailableItems[0]
        viewModel.onItemSelect(zikr)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems.size)
            assertEquals(zikr.id, state.selectedItems[0].itemId)
            assertEquals(zikr.requiredRepeats, state.selectedItems[0].requiredRepeats)
            assertFalse(state.selectedItems[0].isInfinite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemSelect should not add duplicate items`() = runTest {
        val zikr = testAvailableItems[0]
        viewModel.onItemSelect(zikr)
        viewModel.onItemSelect(zikr) // Try to add again

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemSelect should add new items at the top of the list`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0])
        viewModel.onItemSelect(testAvailableItems[1])

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.selectedItems.size)
            assertEquals("zikr2", state.selectedItems[0].itemId) // Most recently added
            assertEquals("zikr1", state.selectedItems[1].itemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemRemove should remove item from selectedItems`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0])
        viewModel.onItemSelect(testAvailableItems[1])
        viewModel.onItemRemove("zikr1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems.size)
            assertEquals("zikr2", state.selectedItems[0].itemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemCountChange should update requiredRepeats for the item`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0]) // requiredRepeats = 3
        viewModel.onItemCountChange("zikr1", 10)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(10, state.selectedItems[0].requiredRepeats)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemCountChange should coerce negative values to 1`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0])
        viewModel.onItemCountChange("zikr1", -5)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems[0].requiredRepeats)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemCountChange should coerce zero to 1`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0])
        viewModel.onItemCountChange("zikr1", 0)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems[0].requiredRepeats)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onItemInfiniteToggle should toggle isInfinite state`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0])
        
        // Initially isInfinite = false
        viewModel.uiState.test {
            var state = awaitItem()
            assertFalse(state.selectedItems[0].isInfinite)
            cancelAndIgnoreRemainingEvents()
        }

        // Toggle to true
        viewModel.onItemInfiniteToggle("zikr1")
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.selectedItems[0].isInfinite)
            cancelAndIgnoreRemainingEvents()
        }

        // Toggle back to false
        viewModel.onItemInfiniteToggle("zikr1")
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.selectedItems[0].isInfinite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moveSelectedItem should reorder items correctly`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0]) // zikr1
        viewModel.onItemSelect(testAvailableItems[1]) // zikr2 (at index 0)
        viewModel.onItemSelect(testAvailableItems[2]) // zikr3 (at index 0)

        // Order: zikr3, zikr2, zikr1
        viewModel.moveSelectedItem(0, 2) // Move zikr3 to end

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.selectedItems.size)
            assertEquals("zikr2", state.selectedItems[0].itemId)
            assertEquals("zikr1", state.selectedItems[1].itemId)
            assertEquals("zikr3", state.selectedItems[2].itemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moveSelectedItem should do nothing for invalid indices`() = runTest {
        viewModel.onItemSelect(testAvailableItems[0])
        viewModel.onItemSelect(testAvailableItems[1])

        viewModel.uiState.test {
            awaitItem() // Initial state with 2 items
            
            // Try invalid moves
            viewModel.moveSelectedItem(-1, 1)
            viewModel.moveSelectedItem(0, 5)
            viewModel.moveSelectedItem(0, 0) // Same index

            // State should remain unchanged
            val state = awaitItem()
            assertEquals(2, state.selectedItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFromChange should update from value in uiState`() = runTest {
        viewModel.onFromChange(5)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(5, state.from)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToChange should update to value in uiState`() = runTest {
        viewModel.onToChange(12)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(12, state.to)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        viewModel.onCategoryNameChange("")
        viewModel.onItemSelect(testAvailableItems[0])

        var onErrorCalled = false
        var errorMessage = ""

        viewModel.saveCategory(
            onSuccess = {},
            onError = { 
                onErrorCalled = true
                errorMessage = it
            }
        )

        assertTrue(onErrorCalled)
        assertTrue(errorMessage.isNotBlank())

        // Clear error
        viewModel.clearError()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveCategory should call onError when category name is blank`() {
        viewModel.onCategoryNameChange("")
        viewModel.onItemSelect(testAvailableItems[0])

        every { context.getString(R.string.error_category_name_required) } returns "Category name required"

        var onErrorCalled = false
        var errorMessage = ""

        viewModel.saveCategory(
            onSuccess = { throw AssertionError("Should not succeed") },
            onError = { 
                onErrorCalled = true
                errorMessage = it
            }
        )

        assertTrue(onErrorCalled)
        assertEquals("Category name required", errorMessage)
    }

    @Test
    fun `saveCategory should call onError when no items selected`() {
        viewModel.onCategoryNameChange("My Category")
        // Don't add any items

        every { context.getString(R.string.error_select_zikr) } returns "Select at least one zikr"

        var onErrorCalled = false
        var errorMessage = ""

        viewModel.saveCategory(
            onSuccess = { throw AssertionError("Should not succeed") },
            onError = { 
                onErrorCalled = true
                errorMessage = it
            }
        )

        assertTrue(onErrorCalled)
        assertEquals("Select at least one zikr", errorMessage)
    }

    @Test
    fun `saveCategory should create new category when categoryId is null`() = runTest {
        viewModel.onCategoryNameChange("My New Category")
        viewModel.onItemSelect(testAvailableItems[0])
        viewModel.onItemSelect(testAvailableItems[1])

        coEvery { 
            repository.createCustomCategory(any(), any(), any(), any(), any()) 
        } returns "new-category-id"

        var onSuccessCalled = false
        var onErrorCalled = false

        viewModel.saveCategory(
            onSuccess = { onSuccessCalled = true },
            onError = { onErrorCalled = true }
        )
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertFalse(onErrorCalled)
        coVerify { 
            repository.createCustomCategory(
                name = "My New Category",
                langTag = "en",
                itemConfigs = viewModel.uiState.value.selectedItems,
                from = 0,
                to = 8
            )
        }
    }

    @Test
    fun `saveCategory should update existing category when categoryId is not null`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set("categoryId", "existing-category-123")
        }

        val editViewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = savedStateHandle
        )

        editViewModel.onCategoryNameChange("Updated Category")
        editViewModel.onItemSelect(testAvailableItems[0])

        coEvery { 
            repository.updateCustomCategory(any(), any(), any(), any(), any()) 
        } just Runs

        var onSuccessCalled = false

        editViewModel.saveCategory(
            onSuccess = { onSuccessCalled = true },
            onError = { throw AssertionError("Should not error") }
        )
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        coVerify { 
            repository.updateCustomCategory(
                categoryId = "existing-category-123",
                name = "Updated Category",
                itemConfigs = editViewModel.uiState.value.selectedItems,
                from = 0,
                to = 8
            )
        }
    }

    @Test
    fun `saveCategory should call onError when repository throws exception`() = runTest {
        viewModel.onCategoryNameChange("My Category")
        viewModel.onItemSelect(testAvailableItems[0])

        coEvery { 
            repository.createCustomCategory(any(), any(), any(), any(), any()) 
        } throws RuntimeException("Database error")

        var onErrorCalled = false
        var errorMessage = ""

        viewModel.saveCategory(
            onSuccess = { throw AssertionError("Should not succeed") },
            onError = { 
                onErrorCalled = true
                errorMessage = it
            }
        )
        advanceUntilIdle()

        assertTrue(onErrorCalled)
        assertTrue(errorMessage.contains("Failed to save category"))
        assertTrue(errorMessage.contains("Database error"))
    }

    @Test
    fun `saveCategory should reset uiState on success`() = runTest {
        viewModel.onCategoryNameChange("My Category")
        viewModel.onItemSelect(testAvailableItems[0])

        coEvery { 
            repository.createCustomCategory(any(), any(), any(), any(), any()) 
        } returns "new-id"

        viewModel.saveCategory(
            onSuccess = {},
            onError = { throw AssertionError("Should not error") }
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.categoryName)
            assertTrue(state.selectedItems.isEmpty())
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddCustomZikr should create zikr and add to selected items`() = runTest {
        coEvery { 
            repository.createCustomZikr(any(), any(), any(), any(), any(), any(), any()) 
        } returns "new-zikr-id"

        viewModel.onAddCustomZikr(
            arabicText = "سبحان الله",
            transliteration = "SubhanAllah",
            translation = "Glory be to Allah",
            reference = "Bukhari",
            requiredRepeats = 33,
            isInfinite = false
        )
        advanceUntilIdle()

        coVerify { 
            repository.createCustomZikr(
                arabicText = "سبحان الله",
                transliteration = "SubhanAllah",
                translation = "Glory be to Allah",
                reference = "Bukhari",
                requiredRepeats = 33,
                isInfinite = false,
                langTag = "en"
            )
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems.size)
            assertEquals("new-zikr-id", state.selectedItems[0].itemId)
            assertEquals(33, state.selectedItems[0].requiredRepeats)
            assertFalse(state.selectedItems[0].isInfinite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddCustomZikr with isInfinite should set requiredRepeats to 0`() = runTest {
        coEvery { 
            repository.createCustomZikr(any(), any(), any(), any(), any(), any(), any()) 
        } returns "infinite-zikr-id"

        viewModel.onAddCustomZikr(
            arabicText = "الله أكبر",
            transliteration = "Allahu Akbar",
            translation = "Allah is Greatest",
            reference = "",
            requiredRepeats = 10,
            isInfinite = true
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.selectedItems.size)
            assertEquals(0, state.selectedItems[0].requiredRepeats) // Should be 0 for infinite
            assertTrue(state.selectedItems[0].isInfinite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddCustomZikr should set error on exception`() = runTest {
        coEvery { 
            repository.createCustomZikr(any(), any(), any(), any(), any(), any(), any()) 
        } throws RuntimeException("Failed to create")

        every { context.getString(R.string.error_failed_create_zikr) } returns "Failed to create zikr"

        viewModel.onAddCustomZikr(
            arabicText = "test",
            transliteration = "test",
            translation = "test",
            reference = "",
            requiredRepeats = 1,
            isInfinite = false
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.error?.contains("Failed to create zikr") == true)
            assertTrue(state.error?.contains("Failed to create") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `availableItems should emit from repository`() = runTest {
        viewModel.availableItems.test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("zikr1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `availableItems should filter based on language`() = runTest {
        val langFlow = MutableStateFlow("ar")
        every { localeManager.currentLangTagFlow } returns langFlow

        val arabicItems = testAvailableItems.map { 
            it.copy(transliteration = null) // No transliteration for Arabic
        }
        every { repository.observeAvailableItems("ar") } returns flowOf(arabicItems)

        val newViewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = SavedStateHandle()
        )

        newViewModel.availableItems.test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertNull(items[0].transliteration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadCategoryData should populate uiState when editing`() = runTest {
        val existingCategory = CategoryUi(
            id = "existing-cat",
            name = "Existing Category",
            type = CategoryType.USER,
            systemKey = null,
            progress = 0.5f,
            from = 6,
            to = 12
        )

        val categoryItems = listOf(
            com.app.azkary.data.model.AzkarItemUi(
                id = "item1",
                title = "Item 1",
                arabicText = "test",
                transliteration = "test",
                translation = "test",
                reference = null,
                requiredRepeats = 5,
                currentRepeats = 2,
                isCompleted = false,
                isInfinite = false
            )
        )

        every { repository.observeCategoriesWithDisplayName(any(), any()) } returns flowOf(listOf(existingCategory))
        every { repository.observeItemsForCategory(any(), any(), any()) } returns flowOf(categoryItems)

        val savedStateHandle = SavedStateHandle().apply {
            set("categoryId", "existing-cat")
        }

        val editViewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = savedStateHandle
        )

        advanceUntilIdle()

        editViewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Existing Category", state.categoryName)
            assertEquals(6, state.from)
            assertEquals(12, state.to)
            assertFalse(state.isStockCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadCategoryData should set isStockCategory for default categories`() = runTest {
        val stockCategory = CategoryUi(
            id = "stock-cat",
            name = "Morning Azkar",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.MORNING,
            progress = 0.5f,
            from = 0,
            to = 8
        )

        every { repository.observeCategoriesWithDisplayName(any(), any()) } returns flowOf(listOf(stockCategory))
        every { repository.observeItemsForCategory(any(), any(), any()) } returns flowOf(emptyList())

        val savedStateHandle = SavedStateHandle().apply {
            set("categoryId", "stock-cat")
        }

        val editViewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = savedStateHandle
        )

        advanceUntilIdle()

        editViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStockCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadCategoryData should set error on exception`() = runTest {
        every { repository.observeCategoriesWithDisplayName(any(), any()) } throws RuntimeException("Load failed")
        every { context.getString(R.string.error_failed_load_category) } returns "Failed to load category"

        val savedStateHandle = SavedStateHandle().apply {
            set("categoryId", "bad-cat")
        }

        val editViewModel = CategoryCreationViewModel(
            repository = repository,
            localeManager = localeManager,
            context = context,
            savedStateHandle = savedStateHandle
        )

        advanceUntilIdle()

        editViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.error?.contains("Failed to load category") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
