package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.UserProgressEntity
import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressDaoTest : DatabaseTest() {

    private val categoryDao by lazy { database.categoryDao() }
    private val azkarItemDao by lazy { database.azkarItemDao() }
    private val progressDao by lazy { database.progressDao() }

    @Test
    fun `upsert and observe progress for item`() = runTest {
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

        val progress = UserProgressEntity(
            categoryId = "cat-1",
            itemId = "item-1",
            date = "2026-03-03",
            currentRepeats = 2,
            isCompleted = false
        )

        progressDao.upsertProgress(progress)

        progressDao.observeProgressForItem("cat-1", "item-1", "2026-03-03").test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals(2, result?.currentRepeats)
            assertTrue(result?.isCompleted == false)
            cancel()
        }
    }

    @Test
    fun `observe progress for category date returns all items`() = runTest {
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

        val progressList = listOf(
            UserProgressEntity("cat-1", "item-1", "2026-03-03", 2, false),
            UserProgressEntity("cat-1", "item-2", "2026-03-03", 1, true)
        )
        progressList.forEach { progressDao.upsertProgress(it) }

        progressDao.observeProgressForCategoryDate("cat-1", "2026-03-03").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.itemId == "item-1" && it.currentRepeats == 2 })
            assertTrue(result.any { it.itemId == "item-2" && it.isCompleted })
            cancel()
        }
    }

    @Test
    fun `upsert updates existing progress`() = runTest {
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

        val originalProgress = UserProgressEntity(
            categoryId = "cat-1",
            itemId = "item-1",
            date = "2026-03-03",
            currentRepeats = 1,
            isCompleted = false
        )
        progressDao.upsertProgress(originalProgress)

        val updatedProgress = UserProgressEntity(
            categoryId = "cat-1",
            itemId = "item-1",
            date = "2026-03-03",
            currentRepeats = 3,
            isCompleted = true
        )
        progressDao.upsertProgress(updatedProgress)

        val retrieved = progressDao.observeProgressForItem("cat-1", "item-1", "2026-03-03").first()
        assertEquals(3, retrieved?.currentRepeats)
        assertTrue(retrieved?.isCompleted == true)
    }

    @Test
    fun `delete progress for category removes all progress`() = runTest {
        val category1 = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        val category2 = CategoryEntity(
            categoryId = "cat-2",
            type = CategoryType.USER,
            sortOrder = 2,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategories(listOf(category1, category2))

        val items = listOf(
            AzkarItemEntity("item-1", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-2", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        val progressList = listOf(
            UserProgressEntity("cat-1", "item-1", "2026-03-03", 2, false),
            UserProgressEntity("cat-1", "item-2", "2026-03-03", 1, true),
            UserProgressEntity("cat-2", "item-1", "2026-03-03", 3, true)
        )
        progressList.forEach { progressDao.upsertProgress(it) }

        progressDao.deleteProgressForCategory("cat-1")

        val cat1Progress = progressDao.observeProgressForCategoryDate("cat-1", "2026-03-03").first()
        val cat2Progress = progressDao.observeProgressForCategoryDate("cat-2", "2026-03-03").first()

        assertEquals(0, cat1Progress.size)
        assertEquals(1, cat2Progress.size)
    }

    @Test
    fun `cascade delete removes progress when category deleted`() = runTest {
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

        val progress = UserProgressEntity("cat-1", "item-1", "2026-03-03", 2, false)
        progressDao.upsertProgress(progress)

        // Delete category (which should cascade to progress)
        categoryDao.deleteCategory("cat-1")

        val retrieved = progressDao.observeProgressForCategoryDate("cat-1", "2026-03-03").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `cascade delete removes progress when item deleted`() = runTest {
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

        val progress = UserProgressEntity("cat-1", "item-1", "2026-03-03", 2, false)
        progressDao.upsertProgress(progress)

        // Delete item (which should cascade to progress)
        azkarItemDao.deleteCustomItem("item-1")

        val retrieved = progressDao.observeProgressForCategoryDate("cat-1", "2026-03-03").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `progress for different dates is separate`() = runTest {
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

        val progress1 = UserProgressEntity("cat-1", "item-1", "2026-03-03", 2, false)
        val progress2 = UserProgressEntity("cat-1", "item-1", "2026-03-04", 3, true)
        progressDao.upsertProgress(progress1)
        progressDao.upsertProgress(progress2)

        val retrieved1 = progressDao.observeProgressForItem("cat-1", "item-1", "2026-03-03").first()
        val retrieved2 = progressDao.observeProgressForItem("cat-1", "item-1", "2026-03-04").first()

        assertEquals(2, retrieved1?.currentRepeats)
        assertEquals(3, retrieved2?.currentRepeats)
    }

    @Test
    fun `observe returns null for non-existent progress`() = runTest {
        val result = progressDao.observeProgressForItem("cat-1", "item-1", "2026-03-03").first()
        assertNull(result)
    }

    @Test
    fun `progress for different categories is separate`() = runTest {
        val categories = listOf(
            CategoryEntity("cat-1", CategoryType.USER, sortOrder = 1, isArchived = false, from = 0, to = 24),
            CategoryEntity("cat-2", CategoryType.USER, sortOrder = 2, isArchived = false, from = 0, to = 24)
        )
        categoryDao.insertCategories(categories)

        val item = AzkarItemEntity("item-1", 3, AzkarSource.SEEDED)
        azkarItemDao.upsertItem(item)

        val progress1 = UserProgressEntity("cat-1", "item-1", "2026-03-03", 2, false)
        val progress2 = UserProgressEntity("cat-2", "item-1", "2026-03-03", 5, true)
        progressDao.upsertProgress(progress1)
        progressDao.upsertProgress(progress2)

        val retrieved1 = progressDao.observeProgressForItem("cat-1", "item-1", "2026-03-03").first()
        val retrieved2 = progressDao.observeProgressForItem("cat-2", "item-1", "2026-03-03").first()

        assertEquals(2, retrieved1?.currentRepeats)
        assertEquals(5, retrieved2?.currentRepeats)
    }
}
