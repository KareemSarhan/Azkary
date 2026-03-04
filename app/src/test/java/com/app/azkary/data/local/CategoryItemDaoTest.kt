package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.dao.ItemWithTextProjection
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.AzkarTextEntity
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.CategoryItemCrossRefEntity
import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryItemDaoTest : DatabaseTest() {

    private val categoryDao by lazy { database.categoryDao() }
    private val azkarItemDao by lazy { database.azkarItemDao() }
    private val azkarTextDao by lazy { database.azkarTextDao() }
    private val categoryItemDao by lazy { database.categoryItemDao() }

    @Test
    fun `insert and retrieve cross refs for category`() = runTest {
        // Setup: Create category and items
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val items = listOf(
            AzkarItemEntity("item-1", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-2", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        // Create cross refs
        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat-1",
                itemId = "item-1",
                sortOrder = 1,
                isEnabled = true,
                requiredRepeats = 3,
                isInfinite = false
            ),
            CategoryItemCrossRefEntity(
                categoryId = "cat-1",
                itemId = "item-2",
                sortOrder = 2,
                isEnabled = true,
                requiredRepeats = 1,
                isInfinite = false
            )
        )
        categoryItemDao.insertCrossRefs(crossRefs)

        categoryItemDao.getAllCrossRefsForCategory("cat-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.categoryId == "cat-1" })
            cancel()
        }
    }

    @Test
    fun `get enabled items with text returns joined data`() = runTest {
        // Setup
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val item = AzkarItemEntity("item-1", 3, AzkarSource.SEEDED)
        azkarItemDao.upsertItem(item)

        val text = AzkarTextEntity(
            itemId = "item-1",
            langTag = "en",
            title = "Morning Dua",
            text = "Subhan Allah"
        )
        azkarTextDao.upsertText(text)

        val crossRef = CategoryItemCrossRefEntity(
            categoryId = "cat-1",
            itemId = "item-1",
            sortOrder = 1,
            isEnabled = true,
            requiredRepeats = 3,
            isInfinite = false
        )
        categoryItemDao.insertCrossRef(crossRef)

        categoryItemDao.getEnabledItemsWithText("cat-1", "en").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("item-1", result[0].item_itemId)
            assertEquals("Morning Dua", result[0].text_title)
            assertEquals("Subhan Allah", result[0].text_text)
            assertEquals(1, result[0].sortOrder)
            cancel()
        }
    }

    @Test
    fun `get enabled items only returns enabled items`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val items = listOf(
            AzkarItemEntity("item-1", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-2", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        val texts = listOf(
            AzkarTextEntity("item-1", "en", title = "Item 1"),
            AzkarTextEntity("item-2", "en", title = "Item 2")
        )
        azkarTextDao.upsertTexts(texts)

        val crossRefs = listOf(
            CategoryItemCrossRefEntity(
                categoryId = "cat-1",
                itemId = "item-1",
                sortOrder = 1,
                isEnabled = true,
                requiredRepeats = 3,
                isInfinite = false
            ),
            CategoryItemCrossRefEntity(
                categoryId = "cat-1",
                itemId = "item-2",
                sortOrder = 2,
                isEnabled = false,
                requiredRepeats = 1,
                isInfinite = false
            )
        )
        categoryItemDao.insertCrossRefs(crossRefs)

        categoryItemDao.getEnabledItemsWithText("cat-1", "en").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("item-1", result[0].item_itemId)
            cancel()
        }
    }

    @Test
    fun `get weights for category returns item weights`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val item = AzkarItemEntity("item-1", 3, AzkarSource.SEEDED)
        azkarItemDao.upsertItem(item)

        // Add Arabic text (for the JOIN)
        val arabicText = AzkarTextEntity(
            itemId = "item-1",
            langTag = "ar",
            text = "سبحان الله"
        )
        azkarTextDao.upsertText(arabicText)

        val crossRef = CategoryItemCrossRefEntity(
            categoryId = "cat-1",
            itemId = "item-1",
            sortOrder = 1,
            isEnabled = true,
            requiredRepeats = 5,
            isInfinite = true
        )
        categoryItemDao.insertCrossRef(crossRef)

        categoryItemDao.getWeightsForCategory("cat-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("item-1", result[0].itemId)
            assertEquals(5, result[0].requiredRepeats)
            assertTrue(result[0].isInfinite)
            assertEquals("سبحان الله", result[0].arabicText)
            cancel()
        }
    }

    @Test
    fun `update cross ref modifies existing`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val item = AzkarItemEntity("item-1", 3, AzkarSource.SEEDED)
        azkarItemDao.upsertItem(item)

        val crossRef = CategoryItemCrossRefEntity(
            categoryId = "cat-1",
            itemId = "item-1",
            sortOrder = 1,
            isEnabled = true,
            requiredRepeats = 3,
            isInfinite = false
        )
        categoryItemDao.insertCrossRef(crossRef)

        val updatedCrossRef = crossRef.copy(
            sortOrder = 5,
            isEnabled = false,
            requiredRepeats = 10
        )
        categoryItemDao.updateCrossRef(updatedCrossRef)

        val retrieved = categoryItemDao.getAllCrossRefsForCategory("cat-1").first()
        assertEquals(5, retrieved[0].sortOrder)
        assertTrue(!retrieved[0].isEnabled)
        assertEquals(10, retrieved[0].requiredRepeats)
    }

    @Test
    fun `delete category items removes all for category`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val items = listOf(
            AzkarItemEntity("item-1", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-2", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        val crossRefs = listOf(
            CategoryItemCrossRefEntity("cat-1", "item-1", 1, true),
            CategoryItemCrossRefEntity("cat-1", "item-2", 2, true)
        )
        categoryItemDao.insertCrossRefs(crossRefs)

        categoryItemDao.deleteCategoryItems("cat-1")

        val retrieved = categoryItemDao.getAllCrossRefsForCategory("cat-1").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `remove item from category removes specific item`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val items = listOf(
            AzkarItemEntity("item-1", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-2", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        val crossRefs = listOf(
            CategoryItemCrossRefEntity("cat-1", "item-1", 1, true),
            CategoryItemCrossRefEntity("cat-1", "item-2", 2, true)
        )
        categoryItemDao.insertCrossRefs(crossRefs)

        categoryItemDao.removeItemFromCategory("cat-1", "item-1")

        val retrieved = categoryItemDao.getAllCrossRefsForCategory("cat-1").first()
        assertEquals(1, retrieved.size)
        assertEquals("item-2", retrieved[0].itemId)
    }

    @Test
    fun `cascade delete removes cross refs when category deleted`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val item = AzkarItemEntity("item-1", 3, AzkarSource.SEEDED)
        azkarItemDao.upsertItem(item)

        val crossRef = CategoryItemCrossRefEntity("cat-1", "item-1", 1, true)
        categoryItemDao.insertCrossRef(crossRef)

        // Delete category (which should cascade to cross refs)
        categoryDao.deleteCategory("cat-1")

        val retrieved = categoryItemDao.getAllCrossRefsForCategory("cat-1").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `cascade delete removes cross refs when item deleted`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val item = AzkarItemEntity("item-1", 3, AzkarSource.USER)
        azkarItemDao.upsertItem(item)

        val crossRef = CategoryItemCrossRefEntity("cat-1", "item-1", 1, true)
        categoryItemDao.insertCrossRef(crossRef)

        // Delete item (which should cascade to cross refs)
        azkarItemDao.deleteCustomItem("item-1")

        val retrieved = categoryItemDao.getAllCrossRefsForCategory("cat-1").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `get enabled items ordered by sort order`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val items = listOf(
            AzkarItemEntity("item-c", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-a", 1, AzkarSource.SEEDED),
            AzkarItemEntity("item-b", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        val texts = listOf(
            AzkarTextEntity("item-c", "en", title = "Item C"),
            AzkarTextEntity("item-a", "en", title = "Item A"),
            AzkarTextEntity("item-b", "en", title = "Item B")
        )
        azkarTextDao.upsertTexts(texts)

        val crossRefs = listOf(
            CategoryItemCrossRefEntity("cat-1", "item-c", 3, true),
            CategoryItemCrossRefEntity("cat-1", "item-a", 1, true),
            CategoryItemCrossRefEntity("cat-1", "item-b", 2, true)
        )
        categoryItemDao.insertCrossRefs(crossRefs)

        categoryItemDao.getEnabledItemsWithText("cat-1", "en").test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("item-a", result[0].item_itemId)
            assertEquals("item-b", result[1].item_itemId)
            assertEquals("item-c", result[2].item_itemId)
            cancel()
        }
    }
}
