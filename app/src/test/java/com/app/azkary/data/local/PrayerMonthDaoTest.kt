package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.PrayerMonthEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PrayerMonthDaoTest : DatabaseTest() {

    private val prayerMonthDao by lazy { database.prayerMonthDao() }

    @Test
    fun `insert and retrieve month`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )

        val id = prayerMonthDao.insertMonth(month)

        val retrieved = prayerMonthDao.getMonth(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4
        )

        assertNotNull(retrieved)
        assertEquals(id, retrieved?.id)
        assertEquals(2026, retrieved?.year)
        assertEquals(3, retrieved?.month)
        assertEquals(24.7136, retrieved?.latitude ?: 0.0, 0.0001)
        assertEquals(46.6753, retrieved?.longitude ?: 0.0, 0.0001)
        assertEquals(4, retrieved?.methodId)
        assertEquals("Asia/Riyadh", retrieved?.timezone)
    }

    @Test
    fun `insert with replace strategy updates existing`() = runTest {
        val originalMonth = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = Instant.parse("2026-03-01T00:00:00Z")
        )
        prayerMonthDao.insertMonth(originalMonth)

        val updatedMonth = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Dubai",
            lastUpdated = Instant.parse("2026-03-02T00:00:00Z")
        )
        prayerMonthDao.insertMonth(updatedMonth)

        val retrieved = prayerMonthDao.getMonth(2026, 3, 24.7136, 46.6753, 4)
        assertEquals("Asia/Dubai", retrieved?.timezone)
    }

    @Test
    fun `observe month returns flow`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        prayerMonthDao.insertMonth(month)

        prayerMonthDao.observeMonth(2026, 3, 24.7136, 46.6753, 4).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals(2026, result?.year)
            cancel()
        }
    }

    @Test
    fun `get month returns null for non-existent`() = runTest {
        val retrieved = prayerMonthDao.getMonth(2026, 3, 24.7136, 46.6753, 4)
        assertNull(retrieved)
    }

    @Test
    fun `different location parameters return different months`() = runTest {
        val month1 = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val month2 = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 25.2048,
            longitude = 55.2708,
            methodId = 4,
            timezone = "Asia/Dubai"
        )
        prayerMonthDao.insertMonth(month1)
        prayerMonthDao.insertMonth(month2)

        val retrieved1 = prayerMonthDao.getMonth(2026, 3, 24.7136, 46.6753, 4)
        val retrieved2 = prayerMonthDao.getMonth(2026, 3, 25.2048, 55.2708, 4)

        assertEquals("Asia/Riyadh", retrieved1?.timezone)
        assertEquals("Asia/Dubai", retrieved2?.timezone)
    }

    @Test
    fun `update month modifies entity`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val id = prayerMonthDao.insertMonth(month)

        val updatedMonth = month.copy(
            id = id,
            timezone = "Asia/Dubai",
            lastUpdated = Instant.now()
        )
        prayerMonthDao.updateMonth(updatedMonth)

        val retrieved = prayerMonthDao.getMonth(2026, 3, 24.7136, 46.6753, 4)
        assertEquals("Asia/Dubai", retrieved?.timezone)
    }

    @Test
    fun `delete old months removes outdated entries`() = runTest {
        val oldInstant = Instant.parse("2026-01-01T00:00:00Z")
        val recentInstant = Instant.parse("2026-03-01T00:00:00Z")
        val cutoffInstant = Instant.parse("2026-02-01T00:00:00Z")

        val oldMonth = PrayerMonthEntity(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = oldInstant
        )
        val recentMonth = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh",
            lastUpdated = recentInstant
        )
        prayerMonthDao.insertMonth(oldMonth)
        prayerMonthDao.insertMonth(recentMonth)

        prayerMonthDao.deleteOldMonths(cutoffInstant)

        val oldRetrieved = prayerMonthDao.getMonth(2026, 1, 24.7136, 46.6753, 4)
        val recentRetrieved = prayerMonthDao.getMonth(2026, 3, 24.7136, 46.6753, 4)

        assertNull(oldRetrieved)
        assertNotNull(recentRetrieved)
    }

    @Test
    fun `get count returns number of months`() = runTest {
        val initialCount = prayerMonthDao.getCount()
        assertEquals(0, initialCount)

        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        prayerMonthDao.insertMonth(month)

        val count = prayerMonthDao.getCount()
        assertEquals(1, count)
    }
}
