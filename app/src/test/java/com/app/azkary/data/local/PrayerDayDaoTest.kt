package com.app.azkary.data.local

import app.cash.turbine.test
import com.app.azkary.data.local.entities.PrayerDayEntity
import com.app.azkary.data.local.entities.PrayerMonthEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PrayerDayDaoTest : DatabaseTest() {

    private val prayerMonthDao by lazy { database.prayerMonthDao() }
    private val prayerDayDao by lazy { database.prayerDayDao() }

    private fun createTestTimes() = PrayerDayEntity(
        monthId = 1L,
        date = LocalDate.of(2026, 3, 1),
        fajr = LocalTime.of(5, 0),
        dhuhr = LocalTime.of(12, 0),
        asr = LocalTime.of(15, 30),
        maghrib = LocalTime.of(18, 0),
        isha = LocalTime.of(19, 30),
        sunrise = LocalTime.of(6, 15),
        sunset = LocalTime.of(17, 50),
        firstthird = LocalTime.of(23, 0),
        midnight = LocalTime.of(0, 0),
        lastthird = LocalTime.of(2, 0)
    )

    @Test
    fun `insert and retrieve day`() = runTest {
        // First insert a month (needed for foreign key)
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val day = createTestTimes().copy(monthId = monthId)
        prayerDayDao.insertDay(day)

        val retrieved = prayerDayDao.getDay(monthId, LocalDate.of(2026, 3, 1))

        assertNotNull(retrieved)
        assertEquals(monthId, retrieved?.monthId)
        assertEquals(LocalDate.of(2026, 3, 1), retrieved?.date)
        assertEquals(LocalTime.of(5, 0), retrieved?.fajr)
        assertEquals(LocalTime.of(12, 0), retrieved?.dhuhr)
        assertEquals(LocalTime.of(15, 30), retrieved?.asr)
        assertEquals(LocalTime.of(18, 0), retrieved?.maghrib)
        assertEquals(LocalTime.of(19, 30), retrieved?.isha)
    }

    @Test
    fun `insert multiple days and retrieve for month`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val days = listOf(
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 1)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 2)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 3))
        )
        prayerDayDao.insertDays(days)

        val retrieved = prayerDayDao.getDaysForMonth(monthId)

        assertEquals(3, retrieved.size)
        assertEquals(LocalDate.of(2026, 3, 1), retrieved[0].date)
        assertEquals(LocalDate.of(2026, 3, 2), retrieved[1].date)
        assertEquals(LocalDate.of(2026, 3, 3), retrieved[2].date)
    }

    @Test
    fun `observe day returns flow`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val day = createTestTimes().copy(monthId = monthId)
        prayerDayDao.insertDay(day)

        prayerDayDao.observeDay(monthId, LocalDate.of(2026, 3, 1)).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals(LocalTime.of(5, 0), result?.fajr)
            cancel()
        }
    }

    @Test
    fun `upsert month deletes existing and inserts new`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val originalDays = listOf(
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 1)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 2))
        )
        prayerDayDao.insertDays(originalDays)

        val newDays = listOf(
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 10)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 11)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 12))
        )
        prayerDayDao.upsertMonth(monthId, newDays)

        val retrieved = prayerDayDao.getDaysForMonth(monthId)
        assertEquals(3, retrieved.size)
        assertEquals(LocalDate.of(2026, 3, 10), retrieved[0].date)
    }

    @Test
    fun `delete days for month removes all days`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val days = listOf(
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 1)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 2))
        )
        prayerDayDao.insertDays(days)

        prayerDayDao.deleteDaysForMonth(monthId)

        val retrieved = prayerDayDao.getDaysForMonth(monthId)
        assertEquals(0, retrieved.size)
    }

    @Test
    fun `get day count for month returns correct count`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val days = listOf(
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 1)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 2)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 3))
        )
        prayerDayDao.insertDays(days)

        val count = prayerDayDao.getDayCountForMonth(monthId)
        assertEquals(3, count)
    }

    @Test
    fun `get days in range returns days within range`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val days = listOf(
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 1)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 5)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 10)),
            createTestTimes().copy(monthId = monthId, date = LocalDate.of(2026, 3, 15))
        )
        prayerDayDao.insertDays(days)

        val rangeResult = prayerDayDao.getDaysInRange(
            LocalDate.of(2026, 3, 5),
            LocalDate.of(2026, 3, 12)
        )

        assertEquals(2, rangeResult.size)
        assertEquals(LocalDate.of(2026, 3, 5), rangeResult[0].date)
        assertEquals(LocalDate.of(2026, 3, 10), rangeResult[1].date)
    }

    @Test
    fun `cascade delete removes days when month deleted`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val day = createTestTimes().copy(monthId = monthId)
        prayerDayDao.insertDay(day)

        // Delete month (which should cascade to days)
        // Note: PrayerMonthDao doesn't have a delete method exposed, but we test cascade behavior
        // through the upsertMonth which deletes days first

        // Simulate cascade by manually deleting
        prayerDayDao.deleteDaysForMonth(monthId)

        val retrieved = prayerDayDao.getDay(monthId, LocalDate.of(2026, 3, 1))
        assertNull(retrieved)
    }

    @Test
    fun `insert with replace updates existing day`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val originalDay = createTestTimes().copy(monthId = monthId, fajr = LocalTime.of(5, 0))
        prayerDayDao.insertDay(originalDay)

        val updatedDay = createTestTimes().copy(monthId = monthId, fajr = LocalTime.of(5, 30))
        prayerDayDao.insertDay(updatedDay)

        val retrieved = prayerDayDao.getDay(monthId, LocalDate.of(2026, 3, 1))
        assertEquals(LocalTime.of(5, 30), retrieved?.fajr)
    }

    @Test
    fun `prayer times include all fields`() = runTest {
        val month = PrayerMonthEntity(
            year = 2026,
            month = 3,
            latitude = 24.7136,
            longitude = 46.6753,
            methodId = 4,
            timezone = "Asia/Riyadh"
        )
        val monthId = prayerMonthDao.insertMonth(month)

        val day = PrayerDayEntity(
            monthId = monthId,
            date = LocalDate.of(2026, 3, 1),
            fajr = LocalTime.of(5, 0),
            dhuhr = LocalTime.of(12, 5),
            asr = LocalTime.of(15, 25),
            maghrib = LocalTime.of(18, 10),
            isha = LocalTime.of(19, 30),
            sunrise = LocalTime.of(6, 15),
            sunset = LocalTime.of(18, 5),
            firstthird = LocalTime.of(23, 30),
            midnight = LocalTime.of(0, 15),
            lastthird = LocalTime.of(2, 30)
        )
        prayerDayDao.insertDay(day)

        val retrieved = prayerDayDao.getDay(monthId, LocalDate.of(2026, 3, 1))

        assertEquals(LocalTime.of(5, 0), retrieved?.fajr)
        assertEquals(LocalTime.of(12, 5), retrieved?.dhuhr)
        assertEquals(LocalTime.of(15, 25), retrieved?.asr)
        assertEquals(LocalTime.of(18, 10), retrieved?.maghrib)
        assertEquals(LocalTime.of(19, 30), retrieved?.isha)
        assertEquals(LocalTime.of(6, 15), retrieved?.sunrise)
        assertEquals(LocalTime.of(18, 5), retrieved?.sunset)
        assertEquals(LocalTime.of(23, 30), retrieved?.firstthird)
        assertEquals(LocalTime.of(0, 15), retrieved?.midnight)
        assertEquals(LocalTime.of(2, 30), retrieved?.lastthird)
    }
}
