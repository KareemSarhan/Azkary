package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.model.SystemCategoryKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDaoTest : DatabaseTest() {

    private val categoryDao by lazy { database.categoryDao() }

    @Test
    fun `insert and retrieve category`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.MORNING,
            sortOrder = 1,
            isArchived = false,
            from = 6,
            to = 12
        )

        categoryDao.insertCategory(category)

        val retrieved = categoryDao.getCategoryById("cat-1").first()

        assertNotNull(retrieved)
        assertEquals("cat-1", retrieved?.categoryId)
        assertEquals(CategoryType.DEFAULT, retrieved?.type)
        assertEquals(SystemCategoryKey.MORNING, retrieved?.systemKey)
    }

    @Test
    fun `insert multiple categories and retrieve ordered`() = runTest {
        val categories = listOf(
            CategoryEntity(
                categoryId = "cat-3",
                type = CategoryType.USER,
                sortOrder = 3,
                isArchived = false,
                from = 0,
                to = 24
            ),
            CategoryEntity(
                categoryId = "cat-1",
                type = CategoryType.DEFAULT,
                systemKey = SystemCategoryKey.MORNING,
                sortOrder = 1,
                isArchived = false,
                from = 6,
                to = 12
            ),
            CategoryEntity(
                categoryId = "cat-2",
                type = CategoryType.DEFAULT,
                systemKey = SystemCategoryKey.NIGHT,
                sortOrder = 2,
                isArchived = false,
                from = 18,
                to = 22
            )
        )

        categoryDao.insertCategories(categories)

        categoryDao.getActiveCategoriesOrdered().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("cat-1", result[0].categoryId)
            assertEquals("cat-2", result[1].categoryId)
            assertEquals("cat-3", result[2].categoryId)
            cancel()
        }
    }

    @Test
    fun `archive category only archives USER type`() = runTest {
        val userCategory = CategoryEntity(
            categoryId = "user-cat",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        val defaultCategory = CategoryEntity(
            categoryId = "default-cat",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.MORNING,
            sortOrder = 2,
            isArchived = false,
            from = 6,
            to = 12
        )

        categoryDao.insertCategories(listOf(userCategory, defaultCategory))
        categoryDao.archiveCategory("user-cat")

        val activeCategories = categoryDao.getActiveCategoriesOrdered().first()
        assertEquals(1, activeCategories.size)
        assertEquals("default-cat", activeCategories[0].categoryId)
    }

    @Test
    fun `update category sort order`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )

        categoryDao.insertCategory(category)
        categoryDao.updateSortOrder("cat-1", 5)

        val updated = categoryDao.getCategoryById("cat-1").first()
        assertEquals(5, updated?.sortOrder)
    }

    @Test
    fun `delete category only deletes USER type`() = runTest {
        val userCategory = CategoryEntity(
            categoryId = "user-cat",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        val defaultCategory = CategoryEntity(
            categoryId = "default-cat",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.MORNING,
            sortOrder = 2,
            isArchived = false,
            from = 6,
            to = 12
        )

        categoryDao.insertCategories(listOf(userCategory, defaultCategory))
        categoryDao.deleteCategory("user-cat")

        val remaining = categoryDao.getActiveCategoriesOrdered().first()
        assertEquals(1, remaining.size)
        assertEquals("default-cat", remaining[0].categoryId)
    }

    @Test
    fun `get max sort order returns highest value`() = runTest {
        val categories = listOf(
            CategoryEntity(
                categoryId = "cat-1",
                type = CategoryType.USER,
                sortOrder = 10,
                isArchived = false,
                from = 0,
                to = 24
            ),
            CategoryEntity(
                categoryId = "cat-2",
                type = CategoryType.USER,
                sortOrder = 5,
                isArchived = false,
                from = 0,
                to = 24
            )
        )

        categoryDao.insertCategories(categories)

        val maxSortOrder = categoryDao.getMaxSortOrder()
        assertEquals(10, maxSortOrder)
    }

    @Test
    fun `get max sort order returns null for empty table`() = runTest {
        val maxSortOrder = categoryDao.getMaxSortOrder()
        assertNull(maxSortOrder)
    }

    @Test
    fun `update category modifies entity`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )

        categoryDao.insertCategory(category)

        val updatedCategory = category.copy(
            sortOrder = 10,
            from = 8,
            to = 20
        )
        categoryDao.updateCategory(updatedCategory)

        val retrieved = categoryDao.getCategoryById("cat-1").first()
        assertEquals(10, retrieved?.sortOrder)
        assertEquals(8, retrieved?.from)
        assertEquals(20, retrieved?.to)
    }
}
