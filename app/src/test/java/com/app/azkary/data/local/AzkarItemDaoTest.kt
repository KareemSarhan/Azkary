package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.model.AzkarSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarItemDaoTest : DatabaseTest() {

    private val azkarItemDao by lazy { database.azkarItemDao() }

    @Test
    fun `upsert and retrieve item by id`() = runTest {
        val item = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED,
            isInfinite = false
        )

        azkarItemDao.upsertItem(item)

        val retrieved = azkarItemDao.getItemById("item-1").first()

        assertNotNull(retrieved)
        assertEquals("item-1", retrieved?.itemId)
        assertEquals(3, retrieved?.requiredRepeats)
        assertEquals(AzkarSource.SEEDED, retrieved?.source)
    }

    @Test
    fun `upsert multiple items`() = runTest {
        val items = listOf(
            AzkarItemEntity(
                itemId = "item-1",
                requiredRepeats = 3,
                source = AzkarSource.SEEDED,
                isInfinite = false
            ),
            AzkarItemEntity(
                itemId = "item-2",
                requiredRepeats = 1,
                source = AzkarSource.USER,
                isInfinite = true
            ),
            AzkarItemEntity(
                itemId = "item-3",
                requiredRepeats = 7,
                source = AzkarSource.SEEDED,
                isInfinite = false
            )
        )

        azkarItemDao.upsertItems(items)

        val retrieved1 = azkarItemDao.getItemById("item-1").first()
        val retrieved2 = azkarItemDao.getItemById("item-2").first()
        val retrieved3 = azkarItemDao.getItemById("item-3").first()

        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertNotNull(retrieved3)
    }

    @Test
    fun `get seeded items returns only seeded items ordered by id`() = runTest {
        val items = listOf(
            AzkarItemEntity(
                itemId = "z-item",
                requiredRepeats = 1,
                source = AzkarSource.SEEDED,
                isInfinite = false
            ),
            AzkarItemEntity(
                itemId = "a-item",
                requiredRepeats = 1,
                source = AzkarSource.USER,
                isInfinite = false
            ),
            AzkarItemEntity(
                itemId = "m-item",
                requiredRepeats = 1,
                source = AzkarSource.SEEDED,
                isInfinite = false
            )
        )

        azkarItemDao.upsertItems(items)

        azkarItemDao.getAvailableItems().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            // Should be ordered by itemId: m-item, z-item
            assertEquals("m-item", result[0].azkarItem.itemId)
            assertEquals("z-item", result[1].azkarItem.itemId)
            cancel()
        }
    }

    @Test
    fun `upsert updates existing item`() = runTest {
        val originalItem = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 3,
            source = AzkarSource.SEEDED,
            isInfinite = false
        )
        azkarItemDao.upsertItem(originalItem)

        val updatedItem = AzkarItemEntity(
            itemId = "item-1",
            requiredRepeats = 10,
            source = AzkarSource.USER,
            isInfinite = true
        )
        azkarItemDao.upsertItem(updatedItem)

        val retrieved = azkarItemDao.getItemById("item-1").first()
        assertEquals(10, retrieved?.requiredRepeats)
        assertEquals(AzkarSource.USER, retrieved?.source)
        assertTrue(retrieved?.isInfinite == true)
    }

    @Test
    fun `delete custom item only deletes USER items`() = runTest {
        val items = listOf(
            AzkarItemEntity(
                itemId = "seeded-item",
                requiredRepeats = 3,
                source = AzkarSource.SEEDED,
                isInfinite = false
            ),
            AzkarItemEntity(
                itemId = "user-item",
                requiredRepeats = 5,
                source = AzkarSource.USER,
                isInfinite = false
            )
        )
        azkarItemDao.upsertItems(items)

        azkarItemDao.deleteCustomItem("seeded-item")
        val seededRetrieved = azkarItemDao.getItemById("seeded-item").first()
        assertNotNull(seededRetrieved)

        azkarItemDao.deleteCustomItem("user-item")
        val userRetrieved = azkarItemDao.getItemById("user-item").first()
        assertNull(userRetrieved)
    }

    @Test
    fun `get item by id returns null for non-existent item`() = runTest {
        val retrieved = azkarItemDao.getItemById("non-existent").first()
        assertNull(retrieved)
    }
}
