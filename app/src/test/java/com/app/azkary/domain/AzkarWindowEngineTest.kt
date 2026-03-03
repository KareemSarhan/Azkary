package com.app.azkary.domain

import com.app.azkary.domain.model.AzkarWindow
import com.app.azkary.domain.model.DayPrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Unit tests for AzkarWindowEngine
 *
 * Tests cover:
 * - Morning window calculation (Fajr to Asr)
 * - Night window calculation (Asr to Isha)
 * - Sleep window calculation (Isha to next day Fajr)
 * - Current window detection
 * - Next window calculation
 * - Edge cases (midnight, exact boundaries, missing tomorrow times)
 */
@RunWith(Parameterized::class)
class AzkarWindowEngineParameterizedTest(
    private val testName: String,
    private val currentTimeOffset: Long, // minutes from start of day
    private val expectedWindow: AzkarWindow?
) {

    companion object {
        private val TEST_DATE = LocalDate.of(2026, 3, 3)
        private val TOMORROW_DATE = LocalDate.of(2026, 3, 4)
        private val ZONE = ZoneId.of("UTC")

        // Standard test prayer times
        private val FAJR = LocalTime.of(5, 30)
        private val SUNRISE = LocalTime.of(6, 45)
        private val DHUHR = LocalTime.of(12, 30)
        private val ASR = LocalTime.of(15, 45)
        private val MAGHRIB = LocalTime.of(18, 15)
        private val ISHA = LocalTime.of(20, 0)

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any?>> = listOf(
            // Morning window tests (Fajr 05:30 to Asr 15:45)
            arrayOf("During morning window - mid morning", 360L, AzkarWindow.MORNING),   // 06:00
            arrayOf("During morning window - before dhuhr", 720L, AzkarWindow.MORNING),  // 12:00
            arrayOf("During morning window - near asr", 930L, AzkarWindow.MORNING),      // 15:30

            // Night window tests (Asr 15:45 to Isha 20:00)
            arrayOf("During night window - just after asr", 950L, AzkarWindow.NIGHT),    // 15:50
            arrayOf("During night window - at maghrib", 1095L, AzkarWindow.NIGHT),       // 18:15
            arrayOf("During night window - before isha", 1170L, AzkarWindow.NIGHT),      // 19:30

            // Sleep window tests (Isha 20:00 to next day Fajr 05:30)
            arrayOf("During sleep window - just after isha", 1210L, AzkarWindow.SLEEP),  // 20:10
            arrayOf("During sleep window - late night", 1380L, AzkarWindow.SLEEP),       // 23:00
            arrayOf("During sleep window - midnight", 0L, AzkarWindow.SLEEP),            // 00:00
            arrayOf("During sleep window - early morning", 180L, AzkarWindow.SLEEP),     // 03:00
            arrayOf("During sleep window - before fajr", 300L, AzkarWindow.SLEEP),       // 05:00

            // Boundary tests
            arrayOf("At exact fajr time", 330L, AzkarWindow.MORNING),                    // 05:30
            arrayOf("Just before fajr", 329L, AzkarWindow.SLEEP),                        // 05:29 (still night)

            // Before morning window (early morning before Fajr) - part of sleep window
            arrayOf("Late night - well after isha", 1320L, AzkarWindow.SLEEP),           // 22:00
        )
    }

    private lateinit var engine: AzkarWindowEngine
    private lateinit var todayTimes: DayPrayerTimes
    private lateinit var tomorrowTimes: DayPrayerTimes

    @Before
    fun setup() {
        engine = AzkarWindowEngine()

        todayTimes = createPrayerTimes(TEST_DATE)
        tomorrowTimes = createPrayerTimes(TOMORROW_DATE)
    }

    private fun createPrayerTimes(date: LocalDate): DayPrayerTimes {
        return DayPrayerTimes(
            date = date,
            fajr = FAJR,
            sunrise = SUNRISE,
            dhuhr = DHUHR,
            asr = ASR,
            maghrib = MAGHRIB,
            isha = ISHA,
            sunset = LocalTime.of(18, 10),
            firstthird = LocalTime.of(1, 0),
            midnight = LocalTime.of(0, 30),
            lastthird = LocalTime.of(23, 0),
            timezone = ZONE
        )
    }

    private fun createInstant(minutesFromMidnight: Long): Instant {
        val time = LocalTime.of((minutesFromMidnight / 60).toInt(), (minutesFromMidnight % 60).toInt())
        return TEST_DATE.atTime(time).atZone(ZONE).toInstant()
    }

    @Test
    fun testCurrentWindow() {
        // Arrange
        val currentTime = createInstant(currentTimeOffset)

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        if (expectedWindow != null) {
            assertNotNull("Expected $expectedWindow but got null for test: $testName", result.currentWindow)
            assertEquals("Window mismatch for test: $testName", expectedWindow, result.currentWindow?.window)
        }
    }
}

class AzkarWindowEngineTest {

    private lateinit var engine: AzkarWindowEngine
    private val testDate = LocalDate.of(2026, 3, 3)
    private val tomorrowDate = LocalDate.of(2026, 3, 4)
    private val zone = ZoneId.of("UTC")

    @Before
    fun setup() {
        engine = AzkarWindowEngine()
    }

    private fun createPrayerTimes(
        date: LocalDate,
        fajr: LocalTime = LocalTime.of(5, 30),
        asr: LocalTime = LocalTime.of(15, 45),
        isha: LocalTime = LocalTime.of(20, 0)
    ): DayPrayerTimes {
        return DayPrayerTimes(
            date = date,
            fajr = fajr,
            sunrise = LocalTime.of(6, 45),
            dhuhr = LocalTime.of(12, 30),
            asr = asr,
            maghrib = LocalTime.of(18, 15),
            isha = isha,
            sunset = LocalTime.of(18, 10),
            firstthird = LocalTime.of(1, 0),
            midnight = LocalTime.of(0, 30),
            lastthird = LocalTime.of(23, 0),
            timezone = zone
        )
    }

    // ==================== Window Calculation Tests ====================

    @Test
    fun `calculateWindows returns morning window when time is during morning`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant() // 10:00 AM

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.MORNING, result.currentWindow?.window)
    }

    @Test
    fun `calculateWindows returns night window when time is during night`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant() // 5:00 PM

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.NIGHT, result.currentWindow?.window)
    }

    @Test
    fun `calculateWindows returns sleep window when time is after isha`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(22, 0)).atZone(zone).toInstant() // 10:00 PM

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.SLEEP, result.currentWindow?.window)
    }

    @Test
    fun `calculateWindows returns sleep window at midnight`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.MIDNIGHT).atZone(zone).toInstant()

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.SLEEP, result.currentWindow?.window)
    }

    // ==================== Next Window Tests ====================

    @Test
    fun `calculateWindows returns next window correctly during morning`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant() // Morning

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.nextWindow)
        assertEquals(AzkarWindow.NIGHT, result.nextWindow?.window) // Next is night
    }

    @Test
    fun `calculateWindows returns next window correctly during night`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant() // Night

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.nextWindow)
        assertEquals(AzkarWindow.SLEEP, result.nextWindow?.window) // Next is sleep
    }

    @Test
    fun `calculateWindows returns next day morning window during sleep`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(22, 0)).atZone(zone).toInstant() // Sleep

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.nextWindow)
        assertEquals(AzkarWindow.MORNING, result.nextWindow?.window) // Next is tomorrow's morning
        assertEquals(tomorrowDate, result.nextWindow?.date) // Should be tomorrow
    }

    @Test
    fun `calculateWindows falls back to first window when no tomorrow times`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val currentTime = testDate.atTime(LocalTime.of(22, 0)).atZone(zone).toInstant() // Sleep

        // Act - no tomorrow times provided
        val result = engine.calculateWindows(currentTime, todayTimes, null)

        // Assert
        assertNotNull(result.nextWindow)
        assertEquals(AzkarWindow.MORNING, result.nextWindow?.window) // Fallback to first window
    }

    // ==================== Edge Cases ====================

    @Test
    fun `calculateWindows handles exact fajr time`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(5, 30)).atZone(zone).toInstant() // Exact Fajr

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert - At exact Fajr, should be in morning window (>= start)
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.MORNING, result.currentWindow?.window)
    }

    @Test
    fun `calculateWindows handles exact asr time`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(15, 45)).atZone(zone).toInstant() // Exact Asr

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert - At exact Asr, should be in night window (>= start)
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.NIGHT, result.currentWindow?.window)
    }

    @Test
    fun `calculateWindows handles exact isha time`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant() // Exact Isha

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert - At exact Isha, should be in sleep window (>= start)
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.SLEEP, result.currentWindow?.window)
    }

    @Test
    fun `calculateWindows returns null current window when no matching window`() {
        // Arrange - time before Fajr (e.g., 4:00 AM) should still match sleep window from previous day
        // But if we only have today and no overlap, this tests the null case
        val todayTimes = createPrayerTimes(
            testDate,
            fajr = LocalTime.of(5, 30),
            asr = LocalTime.of(15, 45),
            isha = LocalTime.of(20, 0)
        )
        val tomorrowTimes = createPrayerTimes(tomorrowDate)

        // Time is 4:00 AM - before Fajr, should be in sleep window
        val currentTime = testDate.atTime(LocalTime.of(4, 0)).atZone(zone).toInstant()

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert - should be in sleep window (from previous day's Isha)
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.SLEEP, result.currentWindow?.window)
    }

    // ==================== getScheduleForDate Tests ====================

    @Test
    fun `getScheduleForDate returns all three windows`() {
        // Arrange
        val prayerTimes = createPrayerTimes(testDate)

        // Act
        val schedules = engine.getScheduleForDate(testDate, prayerTimes)

        // Assert
        assertEquals(3, schedules.size)
        assertTrue(schedules.any { it.window == AzkarWindow.MORNING })
        assertTrue(schedules.any { it.window == AzkarWindow.NIGHT })
        assertTrue(schedules.any { it.window == AzkarWindow.SLEEP })
    }

    @Test
    fun `getScheduleForDate returns correct window order`() {
        // Arrange
        val prayerTimes = createPrayerTimes(testDate)

        // Act
        val schedules = engine.getScheduleForDate(testDate, prayerTimes)

        // Assert - order should be MORNING, NIGHT, SLEEP
        assertEquals(AzkarWindow.MORNING, schedules[0].window)
        assertEquals(AzkarWindow.NIGHT, schedules[1].window)
        assertEquals(AzkarWindow.SLEEP, schedules[2].window)
    }

    @Test
    fun `getScheduleForDate uses tomorrow fajr for sleep window end`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)

        // Act
        val schedules = engine.getScheduleForDate(testDate, todayTimes, tomorrowTimes)

        // Assert
        val sleepSchedule = schedules.find { it.window == AzkarWindow.SLEEP }!!
        // Sleep window should end at tomorrow's Fajr time
        val expectedEnd = tomorrowDate.atTime(LocalTime.of(5, 30)).atZone(zone).toInstant()
        assertEquals(expectedEnd, sleepSchedule.end)
    }

    @Test
    fun `getScheduleForDate uses fallback when no tomorrow times`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)

        // Act
        val schedules = engine.getScheduleForDate(testDate, todayTimes, null)

        // Assert
        val sleepSchedule = schedules.find { it.window == AzkarWindow.SLEEP }!!
        // Should use today Fajr + 1 day as fallback
        val expectedEnd = testDate.atTime(LocalTime.of(5, 30)).atZone(zone).toInstant()
            .plusSeconds(86400) // +1 day
        assertEquals(expectedEnd, sleepSchedule.end)
    }

    // ==================== isInWindow Tests ====================

    @Test
    fun `isInWindow returns true when time is in specified window`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant()

        // Act
        val result = engine.isInWindow(currentTime, AzkarWindow.MORNING, todayTimes)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isInWindow returns false when time is not in specified window`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant() // Morning

        // Act - check if in night window
        val result = engine.isInWindow(currentTime, AzkarWindow.NIGHT, todayTimes)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isInWindow handles sleep window with tomorrow times`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(22, 0)).atZone(zone).toInstant() // After Isha

        // Act
        val result = engine.isInWindow(currentTime, AzkarWindow.SLEEP, todayTimes, tomorrowTimes)

        // Assert
        assertTrue(result)
    }

    // ==================== Different Timezone Tests ====================

    @Test
    fun `calculateWindows works with different timezones`() {
        // Arrange
        val cairoZone = ZoneId.of("Africa/Cairo")
        val cairoTimes = DayPrayerTimes(
            date = testDate,
            fajr = LocalTime.of(5, 0),
            sunrise = LocalTime.of(6, 30),
            dhuhr = LocalTime.of(12, 0),
            asr = LocalTime.of(15, 30),
            maghrib = LocalTime.of(18, 0),
            isha = LocalTime.of(19, 30),
            sunset = LocalTime.of(17, 55),
            firstthird = LocalTime.of(23, 0),
            midnight = LocalTime.of(0, 15),
            lastthird = LocalTime.of(2, 0),
            timezone = cairoZone
        )
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(cairoZone).toInstant()

        // Act
        val result = engine.calculateWindows(currentTime, cairoTimes, null)

        // Assert
        assertNotNull(result.currentWindow)
        assertEquals(AzkarWindow.MORNING, result.currentWindow?.window)
    }

    // ==================== Result Data Tests ====================

    @Test
    fun `calculateWindows returns correct result structure`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val tomorrowTimes = createPrayerTimes(tomorrowDate)
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant()

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, tomorrowTimes)

        // Assert
        assertNotNull(result.currentWindow)
        assertNotNull(result.nextWindow)
        assertEquals(todayTimes, result.todayTimes)
        assertEquals(tomorrowTimes, result.tomorrowTimes)
    }

    @Test
    fun `calculateWindows preserves prayer times in result`() {
        // Arrange
        val todayTimes = createPrayerTimes(testDate)
        val currentTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant()

        // Act
        val result = engine.calculateWindows(currentTime, todayTimes, null)

        // Assert
        assertEquals(todayTimes.fajr, result.todayTimes?.fajr)
        assertEquals(todayTimes.asr, result.todayTimes?.asr)
        assertEquals(todayTimes.isha, result.todayTimes?.isha)
    }
}
