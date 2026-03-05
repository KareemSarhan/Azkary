package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.CategoryTextEntity
import com.app.azkary.data.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTextDaoTest : DatabaseTest() {

    private val categoryDao by lazy { database.categoryDao() }
    private val categoryTextDao by lazy { database.categoryTextDao() }

    @Test
    fun `upsert and retrieve category texts`() = runTest {
        // First insert a category (needed for foreign key)
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val texts = listOf(
            CategoryTextEntity(
                categoryId = "cat-1",
                langTag = "en",
                name = "Morning Azkar"
            ),
            CategoryTextEntity(
                categoryId = "cat-1",
                langTag = "ar",
                name = "أذكار الصباح"
            )
        )

        categoryTextDao.upsertCategoryTexts(texts)

        categoryTextDao.getCategoryTexts("cat-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.langTag == "en" && it.name == "Morning Azkar" })
            assertTrue(result.any { it.langTag == "ar" && it.name == "أذكار الصباح" })
            cancel()
        }
    }

    @Test
    fun `upsert single category text`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val text = CategoryTextEntity(
            categoryId = "cat-1",
            langTag = "en",
            name = "Morning Azkar"
        )

        categoryTextDao.upsertCategoryText(text)

        val retrieved = categoryTextDao.getCategoryTexts("cat-1").first()
        assertEquals(1, retrieved.size)
        assertEquals("Morning Azkar", retrieved[0].name)
    }

    @Test
    fun `upsert updates existing text`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val originalText = CategoryTextEntity(
            categoryId = "cat-1",
            langTag = "en",
            name = "Morning Azkar"
        )
        categoryTextDao.upsertCategoryText(originalText)

        val updatedText = CategoryTextEntity(
            categoryId = "cat-1",
            langTag = "en",
            name = "Updated Morning Azkar"
        )
        categoryTextDao.upsertCategoryText(updatedText)

        val retrieved = categoryTextDao.getCategoryTexts("cat-1").first()
        assertEquals(1, retrieved.size)
        assertEquals("Updated Morning Azkar", retrieved[0].name)
    }

    @Test
    fun `delete category texts removes all texts for category`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val texts = listOf(
            CategoryTextEntity(
                categoryId = "cat-1",
                langTag = "en",
                name = "Morning Azkar"
            ),
            CategoryTextEntity(
                categoryId = "cat-1",
                langTag = "ar",
                name = "أذكار الصباح"
            )
        )
        categoryTextDao.upsertCategoryTexts(texts)

        categoryTextDao.deleteCategoryTexts("cat-1")

        val retrieved = categoryTextDao.getCategoryTexts("cat-1").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `cascade delete removes texts when category deleted`() = runTest {
        val category = CategoryEntity(
            categoryId = "cat-1",
            type = CategoryType.USER,
            sortOrder = 1,
            isArchived = false,
            from = 0,
            to = 24
        )
        categoryDao.insertCategory(category)

        val text = CategoryTextEntity(
            categoryId = "cat-1",
            langTag = "en",
            name = "Morning Azkar"
        )
        categoryTextDao.upsertCategoryText(text)

        // Delete category (which should cascade to texts)
        categoryDao.deleteCategory("cat-1")

        val retrieved = categoryTextDao.getCategoryTexts("cat-1").first()
        assertEquals(0, retrieved.size)
    }
}
