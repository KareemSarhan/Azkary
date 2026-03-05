package com.app.azkary.data.local

import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.model.SystemCategoryKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    // ==================== CategoryType Converters ====================

    @Test
    fun `fromCategoryType returns DEFAULT name`() {
        val result = converters.fromCategoryType(CategoryType.DEFAULT)
        assertEquals("DEFAULT", result)
    }

    @Test
    fun `fromCategoryType returns USER name`() {
        val result = converters.fromCategoryType(CategoryType.USER)
        assertEquals("USER", result)
    }

    @Test
    fun `toCategoryType returns DEFAULT for DEFAULT string`() {
        val result = converters.toCategoryType("DEFAULT")
        assertEquals(CategoryType.DEFAULT, result)
    }

    @Test
    fun `toCategoryType returns USER for USER string`() {
        val result = converters.toCategoryType("USER")
        assertEquals(CategoryType.USER, result)
    }

    @Test
    fun `CategoryType round trip preserves value`() {
        CategoryType.entries.forEach { type ->
            val converted = converters.fromCategoryType(type)
            val restored = converters.toCategoryType(converted)
            assertEquals(type, restored)
        }
    }

    // ==================== AzkarSource Converters ====================

    @Test
    fun `fromAzkarSource returns SEEDED name`() {
        val result = converters.fromAzkarSource(AzkarSource.SEEDED)
        assertEquals("SEEDED", result)
    }

    @Test
    fun `fromAzkarSource returns USER name`() {
        val result = converters.fromAzkarSource(AzkarSource.USER)
        assertEquals("USER", result)
    }

    @Test
    fun `toAzkarSource returns SEEDED for SEEDED string`() {
        val result = converters.toAzkarSource("SEEDED")
        assertEquals(AzkarSource.SEEDED, result)
    }

    @Test
    fun `toAzkarSource returns USER for USER string`() {
        val result = converters.toAzkarSource("USER")
        assertEquals(AzkarSource.USER, result)
    }

    @Test
    fun `AzkarSource round trip preserves value`() {
        AzkarSource.entries.forEach { source ->
            val converted = converters.fromAzkarSource(source)
            val restored = converters.toAzkarSource(converted)
            assertEquals(source, restored)
        }
    }

    // ==================== SystemCategoryKey Converters ====================

    @Test
    fun `fromSystemCategoryKey returns MORNING name`() {
        val result = converters.fromSystemCategoryKey(SystemCategoryKey.MORNING)
        assertEquals("MORNING", result)
    }

    @Test
    fun `fromSystemCategoryKey returns EVENING name`() {
        val result = converters.fromSystemCategoryKey(SystemCategoryKey.EVENING)
        assertEquals("EVENING", result)
    }

    @Test
    fun `fromSystemCategoryKey returns NIGHT name`() {
        val result = converters.fromSystemCategoryKey(SystemCategoryKey.NIGHT)
        assertEquals("NIGHT", result)
    }

    @Test
    fun `fromSystemCategoryKey returns SLEEP name`() {
        val result = converters.fromSystemCategoryKey(SystemCategoryKey.SLEEP)
        assertEquals("SLEEP", result)
    }

    @Test
    fun `fromSystemCategoryKey returns null for null input`() {
        val result = converters.fromSystemCategoryKey(null)
        assertNull(result)
    }

    @Test
    fun `toSystemCategoryKey returns MORNING for MORNING string`() {
        val result = converters.toSystemCategoryKey("MORNING")
        assertEquals(SystemCategoryKey.MORNING, result)
    }

    @Test
    fun `toSystemCategoryKey returns EVENING for EVENING string`() {
        val result = converters.toSystemCategoryKey("EVENING")
        assertEquals(SystemCategoryKey.EVENING, result)
    }

    @Test
    fun `toSystemCategoryKey returns NIGHT for NIGHT string`() {
        val result = converters.toSystemCategoryKey("NIGHT")
        assertEquals(SystemCategoryKey.NIGHT, result)
    }

    @Test
    fun `toSystemCategoryKey returns SLEEP for SLEEP string`() {
        val result = converters.toSystemCategoryKey("SLEEP")
        assertEquals(SystemCategoryKey.SLEEP, result)
    }

    @Test
    fun `toSystemCategoryKey returns null for null input`() {
        val result = converters.toSystemCategoryKey(null)
        assertNull(result)
    }

    @Test
    fun `SystemCategoryKey round trip preserves value`() {
        SystemCategoryKey.entries.forEach { key ->
            val converted = converters.fromSystemCategoryKey(key)
            val restored = converters.toSystemCategoryKey(converted)
            assertEquals(key, restored)
        }
    }

    @Test
    fun `SystemCategoryKey round trip preserves null`() {
        val converted = converters.fromSystemCategoryKey(null)
        val restored = converters.toSystemCategoryKey(converted)
        assertNull(restored)
    }

    // ==================== Instant Converters ====================

    @Test
    fun `fromInstant returns epoch millis for specific instant`() {
        val instant = Instant.ofEpochMilli(1706659200000L)
        val result = converters.fromInstant(instant)
        assertEquals(1706659200000L, result)
    }

    @Test
    fun `fromInstant returns zero for epoch instant`() {
        val instant = Instant.EPOCH
        val result = converters.fromInstant(instant)
        assertEquals(0L, result)
    }

    @Test
    fun `toInstant returns correct instant for epoch millis`() {
        val result = converters.toInstant(1706659200000L)
        assertEquals(Instant.ofEpochMilli(1706659200000L), result)
    }

    @Test
    fun `toInstant returns epoch for zero`() {
        val result = converters.toInstant(0L)
        assertEquals(Instant.EPOCH, result)
    }

    @Test
    fun `Instant round trip preserves value`() {
        val instants = listOf(
            Instant.EPOCH,
            Instant.ofEpochMilli(1706659200000L),
            Instant.ofEpochMilli(Long.MAX_VALUE),
            Instant.ofEpochMilli(1L)
        )
        instants.forEach { instant ->
            val converted = converters.fromInstant(instant)
            val restored = converters.toInstant(converted)
            assertEquals(instant, restored)
        }
    }

    @Test
    fun `fromInstant handles large timestamp`() {
        val instant = Instant.ofEpochMilli(Long.MAX_VALUE)
        val result = converters.fromInstant(instant)
        assertEquals(Long.MAX_VALUE, result)
    }

    @Test
    fun `toInstant handles max long value`() {
        val result = converters.toInstant(Long.MAX_VALUE)
        assertEquals(Instant.ofEpochMilli(Long.MAX_VALUE), result)
    }

    // ==================== LocalDate Converters ====================

    @Test
    fun `fromLocalDate returns epoch day for specific date`() {
        val date = LocalDate.of(2024, 1, 31)
        val result = converters.fromLocalDate(date)
        assertEquals(date.toEpochDay(), result)
    }

    @Test
    fun `fromLocalDate returns zero for epoch date`() {
        val date = LocalDate.of(1970, 1, 1)
        val result = converters.fromLocalDate(date)
        assertEquals(0L, result)
    }

    @Test
    fun `toLocalDate returns correct date for epoch day`() {
        val date = LocalDate.of(2024, 1, 31)
        val result = converters.toLocalDate(date.toEpochDay())
        assertEquals(date, result)
    }

    @Test
    fun `toLocalDate returns epoch date for zero`() {
        val result = converters.toLocalDate(0L)
        assertEquals(LocalDate.of(1970, 1, 1), result)
    }

    @Test
    fun `LocalDate round trip preserves value`() {
        val dates = listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2024, 1, 31),
            LocalDate.of(2000, 2, 29),
            LocalDate.of(2024, 12, 31),
            LocalDate.of(1970, 1, 2)
        )
        dates.forEach { date ->
            val converted = converters.fromLocalDate(date)
            val restored = converters.toLocalDate(converted)
            assertEquals(date, restored)
        }
    }

    @Test
    fun `fromLocalDate handles leap year date`() {
        val leapYearDate = LocalDate.of(2024, 2, 29)
        val result = converters.fromLocalDate(leapYearDate)
        assertEquals(leapYearDate.toEpochDay(), result)
    }

    @Test
    fun `toLocalDate handles leap year epoch day`() {
        val leapYearDate = LocalDate.of(2024, 2, 29)
        val result = converters.toLocalDate(leapYearDate.toEpochDay())
        assertEquals(leapYearDate, result)
    }

    @Test
    fun `fromLocalDate handles year end`() {
        val date = LocalDate.of(2024, 12, 31)
        val result = converters.fromLocalDate(date)
        assertEquals(date.toEpochDay(), result)
    }

    @Test
    fun `toLocalDate handles year start`() {
        val date = LocalDate.of(2024, 1, 1)
        val result = converters.toLocalDate(date.toEpochDay())
        assertEquals(date, result)
    }

    // ==================== LocalTime Converters ====================

    @Test
    fun `fromLocalTime returns seconds of day for specific time`() {
        val time = LocalTime.of(12, 30, 45)
        val result = converters.fromLocalTime(time)
        assertEquals(time.toSecondOfDay(), result)
    }

    @Test
    fun `fromLocalTime returns zero for midnight`() {
        val time = LocalTime.MIDNIGHT
        val result = converters.fromLocalTime(time)
        assertEquals(0, result)
    }

    @Test
    fun `fromLocalTime returns max for last second of day`() {
        val time = LocalTime.of(23, 59, 59)
        val result = converters.fromLocalTime(time)
        assertEquals(86399, result)
    }

    @Test
    fun `toLocalTime returns correct time for seconds of day`() {
        val time = LocalTime.of(12, 30, 45)
        val result = converters.toLocalTime(time.toSecondOfDay())
        assertEquals(time, result)
    }

    @Test
    fun `toLocalTime returns midnight for zero`() {
        val result = converters.toLocalTime(0)
        assertEquals(LocalTime.MIDNIGHT, result)
    }

    @Test
    fun `toLocalTime returns last second for max value`() {
        val result = converters.toLocalTime(86399)
        assertEquals(LocalTime.of(23, 59, 59), result)
    }

    @Test
    fun `LocalTime round trip preserves value`() {
        val times = listOf(
            LocalTime.MIDNIGHT,
            LocalTime.NOON,
            LocalTime.of(23, 59, 59),
            LocalTime.of(0, 0, 1),
            LocalTime.of(12, 30, 45),
            LocalTime.of(6, 0, 0),
            LocalTime.of(18, 30, 30)
        )
        times.forEach { time ->
            val converted = converters.fromLocalTime(time)
            val restored = converters.toLocalTime(converted)
            assertEquals(time, restored)
        }
    }

    @Test
    fun `fromLocalTime handles noon`() {
        val time = LocalTime.NOON
        val result = converters.fromLocalTime(time)
        assertEquals(43200, result)
    }

    @Test
    fun `toLocalTime returns noon for 43200 seconds`() {
        val result = converters.toLocalTime(43200)
        assertEquals(LocalTime.NOON, result)
    }

    @Test
    fun `fromLocalTime with nanoseconds ignores nanos`() {
        val time = LocalTime.of(12, 30, 45, 500000000)
        val result = converters.fromLocalTime(time)
        assertEquals(45045, result)
    }

    @Test
    fun `toLocalTime handles first second of day`() {
        val result = converters.toLocalTime(1)
        assertEquals(LocalTime.of(0, 0, 1), result)
    }

    @Test
    fun `fromLocalTime handles first second of day`() {
        val time = LocalTime.of(0, 0, 1)
        val result = converters.fromLocalTime(time)
        assertEquals(1, result)
    }
}
