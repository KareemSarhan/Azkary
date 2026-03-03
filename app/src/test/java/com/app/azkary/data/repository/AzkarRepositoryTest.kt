package com.app.azkary.data.repository

import android.content.Context
import com.app.azkary.data.local.dao.AzkarItemDao
import com.app.azkary.data.local.dao.AzkarTextDao
import com.app.azkary.data.local.dao.CategoryDao
import com.app.azkary.data.local.dao.CategoryItemDao
import com.app.azkary.data.local.dao.CategoryTextDao
import com.app.azkary.data.local.dao.ItemWithTextProjection
import com.app.azkary.data.local.dao.ProgressDao
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.AzkarTextEntity
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.CategoryItemCrossRefEntity
import com.app.azkary.data.local.entities.CategoryTextEntity
import com.app.azkary.data.local.entities.UserProgressEntity
import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.CategoryItemConfig
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.seed.SeedManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AzkarRepositoryTest {

    private lateinit var repository: AzkarRepository
    private lateinit var categoryDao: CategoryDao
    private lateinit var categoryTextDao: CategoryTextDao
    private lateinit var azkarItemDao: AzkarItemDao
    private lateinit var azkarTextDao: AzkarTextDao
    private lateinit var categoryItemDao: CategoryItemDao
    private lateinit var progressDao: ProgressDao
    private lateinit var seedManager: SeedManager
    private lateinit var context: Context
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        categoryDao = mockk(relaxed = true)
        categoryTextDao = mockk(relaxed = true)
        azkarItemDao = mockk(relaxed = true)
        azkarTextDao = mockk(relaxed = true)
        categoryItemDao = mockk(relaxed = true)
        progressDao = mockk(relaxed = true)
        seedManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        repository = AzkarRepository(
            categoryDao = categoryDao,
            categoryTextDao = categoryTextDao,
            itemDao = azkarItemDao,
            textDao = azkarTextDao,
            categoryItemDao = categoryItemDao,
            progressDao = progressDao,
            seedManager = seedManager,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observeCategoriesWithDisplayName returns categories with best text selection`() = runTest {
        // Given
        val categories = listOf(
            CategoryEntity(
                categoryId = "cat1",
                type = CategoryType.DEFAULT,
                systemKey = null,
                sortOrder = 0,
                isArchived = false,
                from = 0,
                to = 6
            )
        )
        val categoryTexts = listOf(
            CategoryTextEntity(categoryId = "cat1", langTag = "en", name = "Morning Azkar"),
            CategoryTextEntity(categoryId = "cat1", langTag = "ar", name = "أذكار الصباح")
        )
        val progress = 0.5f

        every { categoryDao.getActiveCategoriesOrdered() } returns flowOf(categories)
        every { categoryTextDao.getCategoryTexts("cat1") } returns flowOf(categoryTexts)
        every { categoryItemDao.getEnabledItemsWithText("cat1", "en") } returns flowOf(emptyList())
        every { progressDao.observeProgressForCategoryDate("cat1", "2026-01-01") } returns flowOf(emptyList())

        // When
        val result = repository.observeCategoriesWithDisplayName("en", "2026-01-01").first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Morning Azkar", result[0].name)
        assertEquals("cat1", result[0].id)
    }

    @Test
    fun `observeCategoriesWithDisplayName falls back to first available text when preferred not found`() = runTest {
        // Given
        val categories = listOf(
            CategoryEntity(
                categoryId = "cat1",
                type = CategoryType.DEFAULT,
                systemKey = null,
                sortOrder = 0,
                isArchived = false,
                from = 0,
                to = 6
            )
        )
        val categoryTexts = listOf(
            CategoryTextEntity(categoryId = "cat1", langTag = "fr", name = "Azkar du Matin")
        )

        every { categoryDao.getActiveCategoriesOrdered() } returns flowOf(categories)
        every { categoryTextDao.getCategoryTexts("cat1") } returns flowOf(categoryTexts)
        every { categoryItemDao.getEnabledItemsWithText("cat1", "de") } returns flowOf(emptyList())
        every { progressDao.observeProgressForCategoryDate("cat1", "2026-01-01") } returns flowOf(emptyList())

        // When - request German but only French is available
        val result = repository.observeCategoriesWithDisplayName("de", "2026-01-01").first()

        // Then - should fall back to first available (French)
        assertEquals(1, result.size)
        assertEquals("Azkar du Matin", result[0].name)
    }

    @Test
    fun `observeCategoriesWithDisplayName returns empty list when no categories`() = runTest {
        // Given
        every { categoryDao.getActiveCategoriesOrdered() } returns flowOf(emptyList())

        // When
        val result = repository.observeCategoriesWithDisplayName("en", "2026-01-01").first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeItemsForCategory returns items with correct progress`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat1",
                itemId = "item1",
                sortOrder = 0,
                isEnabled = true,
                requiredRepeats = 3,
                isInfinite = false
            )
        )
        val texts = listOf(
            AzkarTextEntity(itemId = "item1", langTag = "ar", text = "سبحان الله", title = "Tasbih"),
            AzkarTextEntity(itemId = "item1", langTag = "en", text = "Subhan Allah", title = "Tasbih")
        )
        val item = AzkarItemEntity(itemId = "item1", requiredRepeats = 3, source = AzkarSource.SEEDED)
        val progress = listOf(
            UserProgressEntity(categoryId = "cat1", itemId = "item1", date = "2026-01-01", currentRepeats = 2, isCompleted = false)
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        every { progressDao.observeProgressForCategoryDate("cat1", "2026-01-01") } returns flowOf(progress)
        every { azkarTextDao.getTextsForItem("item1") } returns flowOf(texts)
        every { azkarItemDao.getItemById("item1") } returns flowOf(item)

        // When
        val result = repository.observeItemsForCategory("cat1", "en", "2026-01-01").first()

        // Then
        assertEquals(1, result.size)
        assertEquals("item1", result[0].id)
        assertEquals(2, result[0].currentRepeats)
        assertEquals(3, result[0].requiredRepeats)
        assertFalse(result[0].isCompleted)
        assertEquals("Subhan Allah", result[0].transliteration)
        assertEquals("سبحان الله", result[0].arabicText)
    }

    @Test
    fun `observeItemsForCategory handles infinite items correctly`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat1",
                itemId = "item1",
                sortOrder = 0,
                isEnabled = true,
                requiredRepeats = 0,
                isInfinite = true
            )
        )
        val texts = listOf(
            AzkarTextEntity(itemId = "item1", langTag = "ar", text = "استغفر الله")
        )
        val item = AzkarItemEntity(itemId = "item1", requiredRepeats = 0, source = AzkarSource.SEEDED, isInfinite = true)
        val progress = listOf(
            UserProgressEntity(categoryId = "cat1", itemId = "item1", date = "2026-01-01", currentRepeats = 100, isCompleted = false)
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        every { progressDao.observeProgressForCategoryDate("cat1", "2026-01-01") } returns flowOf(progress)
        every { azkarTextDao.getTextsForItem("item1") } returns flowOf(texts)
        every { azkarItemDao.getItemById("item1") } returns flowOf(item)

        // When
        val result = repository.observeItemsForCategory("cat1", "ar", "2026-01-01").first()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isInfinite)
        assertEquals(100, result[0].currentRepeats)
        assertFalse(result[0].isCompleted) // Infinite items are never completed
    }

    @Test
    fun `incrementRepeat updates progress for finite item`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat1",
                itemId = "item1",
                sortOrder = 0,
                isEnabled = true,
                requiredRepeats = 3,
                isInfinite = false
            )
        )
        val currentProgress = UserProgressEntity(
            categoryId = "cat1",
            itemId = "item1",
            date = "2026-01-01",
            currentRepeats = 1,
            isCompleted = false
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        every { progressDao.observeProgressForItem("cat1", "item1", "2026-01-01") } returns flowOf(currentProgress)
        coEvery { progressDao.upsertProgress(any()) } just Runs

        // When
        repository.incrementRepeat("cat1", "item1", "2026-01-01")

        // Then
        coVerify { 
            progressDao.upsertProgress(
                match { 
                    it.currentRepeats == 2 && !it.isCompleted 
                }
            )
        }
    }

    @Test
    fun `incrementRepeat marks item completed when reaching required repeats`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat1",
                itemId = "item1",
                sortOrder = 0,
                isEnabled = true,
                requiredRepeats = 3,
                isInfinite = false
            )
        )
        val currentProgress = UserProgressEntity(
            categoryId = "cat1",
            itemId = "item1",
            date = "2026-01-01",
            currentRepeats = 2,
            isCompleted = false
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        every { progressDao.observeProgressForItem("cat1", "item1", "2026-01-01") } returns flowOf(currentProgress)
        coEvery { progressDao.upsertProgress(any()) } just Runs

        // When - increment from 2 to 3 (required)
        repository.incrementRepeat("cat1", "item1", "2026-01-01")

        // Then
        coVerify { 
            progressDao.upsertProgress(
                match { 
                    it.currentRepeats == 3 && it.isCompleted 
                }
            )
        }
    }

    @Test
    fun `incrementRepeat for infinite item never marks completed`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat1",
                itemId = "item1",
                sortOrder = 0,
                isEnabled = true,
                requiredRepeats = 0,
                isInfinite = true
            )
        )
        val currentProgress = UserProgressEntity(
            categoryId = "cat1",
            itemId = "item1",
            date = "2026-01-01",
            currentRepeats = 999,
            isCompleted = false
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        every { progressDao.observeProgressForItem("cat1", "item1", "2026-01-01") } returns flowOf(currentProgress)
        coEvery { progressDao.upsertProgress(any()) } just Runs

        // When
        repository.incrementRepeat("cat1", "item1", "2026-01-01")

        // Then - should increment but never complete
        coVerify { 
            progressDao.upsertProgress(
                match { 
                    it.currentRepeats == 1000 && !it.isCompleted 
                }
            )
        }
    }

    @Test
    fun `markCategoryComplete marks all finite items as completed`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity("cat1", "item1", 0, true, 3, false),
            CategoryItemCrossRefEntity("cat1", "item2", 1, true, 5, false),
            CategoryItemCrossRefEntity("cat1", "item3", 2, true, 0, true) // infinite
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        coEvery { progressDao.upsertProgress(any()) } just Runs

        // When
        repository.markCategoryComplete("cat1", "2026-01-01")

        // Then - should update finite items, skip infinite
        coVerify(exactly = 2) { progressDao.upsertProgress(any()) }
    }

    @Test
    fun `markCategoryIncomplete resets all items`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity("cat1", "item1", 0, true, 3, false),
            CategoryItemCrossRefEntity("cat1", "item2", 1, true, 5, false)
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        coEvery { progressDao.upsertProgress(any()) } just Runs

        // When
        repository.markCategoryIncomplete("cat1", "2026-01-01")

        // Then
        coVerify(exactly = 2) { 
            progressDao.upsertProgress(
                match { 
                    it.currentRepeats == 0 && !it.isCompleted 
                }
            )
        }
    }

    @Test
    fun `markItemComplete sets item to required repeats`() = runTest {
        // Given
        val crossRefs = listOf(
            CategoryItemCrossRefEntity("cat1", "item1", 0, true, 5, false)
        )

        every { categoryItemDao.getAllCrossRefsForCategory("cat1") } returns flowOf(crossRefs)
        coEvery { progressDao.upsertProgress(any()) } just Runs

        // When
        repository.markItemComplete("cat1", "item1", "2026-01-01")

        // Then
        coVerify { 
            progressDao.upsertProgress(
                match { 
                    it.currentRepeats == 5 && it.isCompleted 
                }
            )
        }
    }

    @Test
    fun `createCustomCategory creates category with items`() = runTest {
        // Given
        val itemConfigs = listOf(
            CategoryItemConfig(itemId = "item1", requiredRepeats = 3, isInfinite = false),
            CategoryItemConfig(itemId = "item2", requiredRepeats = 0, isInfinite = true)
        )

        coEvery { categoryDao.getMaxSortOrder() } returns 5
        coEvery { categoryDao.insertCategory(any()) } just Runs
        coEvery { categoryTextDao.upsertCategoryText(any()) } just Runs
        coEvery { categoryItemDao.insertCrossRef(any()) } just Runs
        every { azkarItemDao.getItemById(any()) } returns flowOf(null)
        coEvery { azkarItemDao.upsertItem(any()) } just Runs

        // When
        val categoryId = repository.createCustomCategory(
            name = "My Custom Category",
            langTag = "en",
            itemConfigs = itemConfigs
        )

        // Then
        assertTrue(categoryId.startsWith("user_"))
        coVerify { categoryDao.insertCategory(any()) }
        coVerify { categoryTextDao.upsertCategoryText(any()) }
        coVerify(exactly = 2) { categoryItemDao.insertCrossRef(any()) }
    }

    @Test
    fun `deleteCategory calls dao delete`() = runTest {
        // Given
        coEvery { categoryDao.deleteCategory("cat1") } just Runs

        // When
        repository.deleteCategory("cat1")

        // Then
        coVerify { categoryDao.deleteCategory("cat1") }
    }

    @Test
    fun `reorderCategories updates sort orders`() = runTest {
        // Given
        val categoryIds = listOf("cat3", "cat1", "cat2")
        coEvery { categoryDao.updateSortOrder(any(), any()) } just Runs

        // When
        repository.reorderCategories(categoryIds)

        // Then
        coVerify { categoryDao.updateSortOrder("cat3", 0) }
        coVerify { categoryDao.updateSortOrder("cat1", 1) }
        coVerify { categoryDao.updateSortOrder("cat2", 2) }
    }

    @Test
    fun `createCustomZikr creates item with texts`() = runTest {
        // Given
        coEvery { azkarItemDao.upsertItem(any()) } just Runs
        coEvery { azkarTextDao.upsertText(any()) } just Runs

        // When
        val itemId = repository.createCustomZikr(
            arabicText = "سبحان الله",
            transliteration = "Subhan Allah",
            translation = "Glory be to Allah",
            reference = "Bukhari",
            requiredRepeats = 33,
            isInfinite = false,
            langTag = "en"
        )

        // Then
        assertTrue(itemId.startsWith("user_"))
        coVerify { azkarItemDao.upsertItem(any()) }
        coVerify(atLeast = 2) { azkarTextDao.upsertText(any()) } // Arabic + langTag + possibly English
    }

    @Test
    fun `seedDatabase calls seed manager`() = runTest {
        // Given
        coEvery { seedManager.seedIfNeeded(context) } just Runs

        // When
        repository.seedDatabase()

        // Then
        coVerify { seedManager.seedIfNeeded(context) }
    }

    @Test
    fun `observeAvailableItems returns seeded items`() = runTest {
        // Given
        val items = listOf(
            AzkarItemEntity(itemId = "item1", requiredRepeats = 3, source = AzkarSource.SEEDED),
            AzkarItemEntity(itemId = "item2", requiredRepeats = 1, source = AzkarSource.SEEDED)
        )
        val texts1 = listOf(
            AzkarTextEntity(itemId = "item1", langTag = "ar", text = "الحمد لله"),
            AzkarTextEntity(itemId = "item1", langTag = "en", text = "Alhamdulillah")
        )
        val texts2 = listOf(
            AzkarTextEntity(itemId = "item2", langTag = "ar", text = "الله أكبر")
        )

        every { azkarItemDao.getAvailableItems() } returns flowOf(items)
        every { azkarTextDao.getTextsForItem("item1") } returns flowOf(texts1)
        every { azkarTextDao.getTextsForItem("item2") } returns flowOf(texts2)

        // When
        val result = repository.observeAvailableItems("en").first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Alhamdulillah", result[0].transliteration)
        assertNull(result[1].transliteration) // No English text for item2
    }

    @Test
    fun `updateCustomCategory updates name for user category`() = runTest {
        // Given
        val category = CategoryEntity(
            categoryId = "cat1",
            type = CategoryType.USER,
            systemKey = null,
            sortOrder = 0,
            isArchived = false,
            from = 0,
            to = 6
        )

        every { categoryDao.getCategoryById("cat1") } returns flowOf(category)
        coEvery { categoryTextDao.upsertCategoryText(any()) } just Runs
        coEvery { categoryDao.updateCategory(any()) } just Runs

        // When
        repository.updateCustomCategory(
            categoryId = "cat1",
            name = "Updated Name"
        )

        // Then
        coVerify(exactly = 2) { categoryTextDao.upsertCategoryText(any()) } // en + ar
    }

    @Test
    fun `updateCustomCategory does not update name for stock category`() = runTest {
        // Given
        val category = CategoryEntity(
            categoryId = "cat1",
            type = CategoryType.DEFAULT,
            systemKey = com.app.azkary.data.model.SystemCategoryKey.MORNING,
            sortOrder = 0,
            isArchived = false,
            from = 0,
            to = 6
        )

        every { categoryDao.getCategoryById("cat1") } returns flowOf(category)
        coEvery { categoryDao.updateCategory(any()) } just Runs

        // When
        repository.updateCustomCategory(
            categoryId = "cat1",
            name = "Updated Name" // Should be ignored for stock categories
        )

        // Then
        coVerify(exactly = 0) { categoryTextDao.upsertCategoryText(any()) }
    }

    @Test
    fun `getWeightedProgress calculates progress based on text length`() = runTest {
        // Given
        val projections = listOf(
            ItemWithTextProjection(
                item_itemId = "item1",
                item_source = AzkarSource.SEEDED,
                item_createdAt = 0,
                item_updatedAt = 0,
                text_itemId = "item1",
                text_langTag = "ar",
                text_title = null,
                text_text = "سبحان", // 5 chars
                text_translation = null,
                text_referenceText = null,
                sortOrder = 0,
                requiredRepeats = 3,
                isInfinite = false
            )
        )
        val progress = listOf(
            UserProgressEntity("cat1", "item1", "2026-01-01", 1, false) // 1 out of 3 done
        )

        every { categoryItemDao.getEnabledItemsWithText("cat1", "ar") } returns flowOf(projections)
        every { progressDao.observeProgressForCategoryDate("cat1", "2026-01-01") } returns flowOf(progress)

        // When
        val result = repository.getWeightedProgress("cat1", "2026-01-01", "ar").first()

        // Then - 1/3 = 33.3%
        assertEquals(0.33f, result, 0.01f)
    }

    @Test
    fun `getWeightedProgress returns 0 when no items`() = runTest {
        // Given
        every { categoryItemDao.getEnabledItemsWithText("cat1", "en") } returns flowOf(emptyList())

        // When
        val result = repository.getWeightedProgress("cat1", "2026-01-01", "en").first()

        // Then
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `getWeightedProgress skips infinite items`() = runTest {
        // Given
        val projections = listOf(
            ItemWithTextProjection(
                item_itemId = "item1",
                item_source = AzkarSource.SEEDED,
                item_createdAt = 0,
                item_updatedAt = 0,
                text_itemId = "item1",
                text_langTag = "ar",
                text_title = null,
                text_text = "text",
                text_translation = null,
                text_referenceText = null,
                sortOrder = 0,
                requiredRepeats = 0,
                isInfinite = true
            ),
            ItemWithTextProjection(
                item_itemId = "item2",
                item_source = AzkarSource.SEEDED,
                item_createdAt = 0,
                item_updatedAt = 0,
                text_itemId = "item2",
                text_langTag = "ar",
                text_title = null,
                text_text = "text",
                text_translation = null,
                text_referenceText = null,
                sortOrder = 1,
                requiredRepeats = 2,
                isInfinite = false
            )
        )
        val progress = emptyList<UserProgressEntity>()

        every { categoryItemDao.getEnabledItemsWithText("cat1", "ar") } returns flowOf(projections)
        every { progressDao.observeProgressForCategoryDate("cat1", "2026-01-01") } returns flowOf(progress)

        // When
        val result = repository.getWeightedProgress("cat1", "2026-01-01", "ar").first()

        // Then - infinite item skipped, 0/2 for finite = 0%
        assertEquals(0f, result, 0.01f)
    }
}
