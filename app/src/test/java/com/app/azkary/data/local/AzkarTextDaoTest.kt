package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.AzkarTextEntity
import com.app.azkary.data.model.AzkarSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AzkarTextDaoTest : DatabaseTest() {

    private val azkarItemDao by lazy { database.azkarItemDao() }
    private val azkarTextDao by lazy { database.azkarTextDao() }

    @Test
    fun `upsert and retrieve texts for item`() = runTest {
        // First insert an item (needed for foreign key)
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED
        )
        azkarItemDao.upsertItem(item)

        val texts = listOf(
            AzkarTextEntity(
                itemId = "item-1",
                langTag = "en",
                title = "Morning Dua",
                text = "Subhan Allah",
                translation = "Glory be to Allah",
                referenceText = "Bukhari"
            ),
            AzkarTextEntity(
                itemId = "item-1",
                langTag = "ar",
                title = "دعاء الصباح",
                text = "سبحان الله",
                translation = null,
                referenceText = "البخاري"
            )
        )

        azkarTextDao.upsertTexts(texts)

        azkarTextDao.getTextsForItem("item-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.langTag == "en" && it.title == "Morning Dua" })
            assertTrue(result.any { it.langTag == "ar" && it.text == "سبحان الله" })
            cancel()
        }
    }

    @Test
    fun `get texts for items with specific language`() = runTest {
        // Insert items
        val items = listOf(
            AzkarItemEntity("item-1", 3, AzkarSource.SEEDED),
            AzkarItemEntity("item-2", 1, AzkarSource.SEEDED)
        )
        azkarItemDao.upsertItems(items)

        // Insert texts for both items in different languages
        val texts = listOf(
            AzkarTextEntity("item-1", "en", "Title 1 EN", "Text 1 EN"),
            AzkarTextEntity("item-1", "ar", "العنوان 1", "النص 1"),
            AzkarTextEntity("item-2", "en", "Title 2 EN", "Text 2 EN"),
            AzkarTextEntity("item-2", "fr", "Title 2 FR", "Text 2 FR")
        )
        azkarTextDao.upsertTexts(texts)

        azkarTextDao.getTextsForItems(listOf("item-1", "item-2"), "en").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.langTag == "en" })
            assertTrue(result.any { it.itemId == "item-1" })
            assertTrue(result.any { it.itemId == "item-2" })
            cancel()
        }
    }

    @Test
    fun `upsert single text`() = runTest {
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED
        )
        azkarItemDao.upsertItem(item)

        val text = AzkarTextEntity(
            itemId = "item-1",
            langTag = "en",
            title = "Morning Dua",
            text = "Subhan Allah"
        )

        azkarTextDao.upsertText(text)

        val retrieved = azkarTextDao.getTextsForItem("item-1").first()
        assertEquals(1, retrieved.size)
        assertEquals("Morning Dua", retrieved[0].title)
    }

    @Test
    fun `upsert updates existing text`() = runTest {
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED
        )
        azkarItemDao.upsertItem(item)

        val originalText = AzkarTextEntity(
            itemId = "item-1",
            langTag = "en",
            title = "Original Title",
            text = "Original Text"
        )
        azkarTextDao.upsertText(originalText)

        val updatedText = AzkarTextEntity(
            itemId = "item-1",
            langTag = "en",
            title = "Updated Title",
            text = "Updated Text"
        )
        azkarTextDao.upsertText(updatedText)

        val retrieved = azkarTextDao.getTextsForItem("item-1").first()
        assertEquals(1, retrieved.size)
        assertEquals("Updated Title", retrieved[0].title)
        assertEquals("Updated Text", retrieved[0].text)
    }

    @Test
    fun `delete texts for item removes all texts`() = runTest {
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED
        )
        azkarItemDao.upsertItem(item)

        val texts = listOf(
            AzkarTextEntity("item-1", "en", "Title EN", "Text EN"),
            AzkarTextEntity("item-1", "ar", "العنوان", "النص")
        )
        azkarTextDao.upsertTexts(texts)

        azkarTextDao.deleteTextsForItem("item-1")

        val retrieved = azkarTextDao.getTextsForItem("item-1").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `cascade delete removes texts when item deleted`() = runTest {
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.USER
        )
        azkarItemDao.upsertItem(item)

        val text = AzkarTextEntity(
            itemId = "item-1",
            langTag = "en",
            title = "Title",
            text = "Text"
        )
        azkarTextDao.upsertText(text)

        // Delete item (which should cascade to texts)
        azkarItemDao.deleteCustomItem("item-1")

        val retrieved = azkarTextDao.getTextsForItem("item-1").first()
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `get texts for items with empty list returns empty`() = runTest {
        val result = azkarTextDao.getTextsForItems(emptyList(), "en").first()
        assertEquals(0, result.size)
    }

    @Test
    fun `text entity allows nullable fields`() = runTest {
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED
        )
        azkarItemDao.upsertItem(item)

        val text = AzkarTextEntity(
            itemId = "item-1",
            langTag = "en",
            title = null,
            text = null,
            translation = null,
            referenceText = null
        )
        azkarTextDao.upsertText(text)

        val retrieved = azkarTextDao.getTextsForItem("item-1").first()
        assertEquals(1, retrieved.size)
        assertNull(retrieved[0].title)
        assertNull(retrieved[0].text)
    }
}
