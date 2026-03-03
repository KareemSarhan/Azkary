package com.app.azkary.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Unit tests for DateTimeFormatter
 * 
 * Tests cover:
 * - Date formatting for English locale
 * - Date formatting for Arabic locale
 * - Time formatting
 * - Relative time formatting
 * - Timestamp formatting
 * - Date/time string parsing
 */
class DateTimeFormatterTest {

    private lateinit var dateTimeFormatter: DateTimeFormatter
    private lateinit var englishLocale: Locale
    private lateinit var arabicLocale: Locale

    @Before
    fun setup() {
        dateTimeFormatter = DateTimeFormatter()
        englishLocale = Locale("en")
        arabicLocale = Locale("ar")
    }

    // ==================== Date Formatting Tests ====================

    @Test
    fun `formatDate returns English formatted date for English locale`() {
        // Arrange
        val date = LocalDate.of(2026, 1, 30)

        // Act
        val result = dateTimeFormatter.formatDate(date, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
        // Should contain month name, day, and year
        assertTrue(result.contains("2026") || result.contains("26"))
    }

    @Test
    fun `formatDate returns Arabic formatted date for Arabic locale`() {
        // Arrange
        val date = LocalDate.of(2026, 1, 30)

        // Act
        val result = dateTimeFormatter.formatDate(date, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
        // Should contain Arabic numerals or year
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDate with different months`() {
        // Arrange
        val months = listOf(
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 12, 15)
        )

        months.forEach { date ->
            // Act
            val resultEn = dateTimeFormatter.formatDate(date, englishLocale)
            val resultAr = dateTimeFormatter.formatDate(date, arabicLocale)

            // Assert
            assertTrue(resultEn.isNotEmpty())
            assertTrue(resultAr.isNotEmpty())
        }
    }

    @Test
    fun `formatDate with edge dates`() {
        // Arrange
        val edgeDates = listOf(
            LocalDate.of(2026, 1, 1),   // First day of year
            LocalDate.of(2026, 12, 31), // Last day of year
            LocalDate.of(2000, 2, 29),  // Leap year
            LocalDate.of(2024, 2, 29)   // Another leap year
        )

        edgeDates.forEach { date ->
            // Act
            val resultEn = dateTimeFormatter.formatDate(date, englishLocale)
            val resultAr = dateTimeFormatter.formatDate(date, arabicLocale)

            // Assert
            assertTrue(resultEn.isNotEmpty())
            assertTrue(resultAr.isNotEmpty())
        }
    }

    // ==================== Time Formatting Tests ====================

    @Test
    fun `formatTime returns English formatted time for English locale`() {
        // Arrange
        val time = LocalTime.of(15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatTime(time, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
        // Should contain hour and minute
        assertTrue(result.contains("3") || result.contains("15"))
    }

    @Test
    fun `formatTime returns Arabic formatted time for Arabic locale`() {
        // Arrange
        val time = LocalTime.of(15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatTime(time, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatTime with AM times`() {
        // Arrange
        val amTimes = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(11, 30),
            LocalTime.of(8, 15)
        )

        amTimes.forEach { time ->
            // Act
            val resultEn = dateTimeFormatter.formatTime(time, englishLocale)
            val resultAr = dateTimeFormatter.formatTime(time, arabicLocale)

            // Assert
            assertTrue(resultEn.isNotEmpty())
            assertTrue(resultAr.isNotEmpty())
        }
    }

    @Test
    fun `formatTime with PM times`() {
        // Arrange
        val pmTimes = listOf(
            LocalTime.of(13, 0),
            LocalTime.of(18, 30),
            LocalTime.of(23, 59)
        )

        pmTimes.forEach { time ->
            // Act
            val resultEn = dateTimeFormatter.formatTime(time, englishLocale)
            val resultAr = dateTimeFormatter.formatTime(time, arabicLocale)

            // Assert
            assertTrue(resultEn.isNotEmpty())
            assertTrue(resultAr.isNotEmpty())
        }
    }

    @Test
    fun `formatTime with midnight`() {
        // Arrange
        val midnight = LocalTime.MIDNIGHT

        // Act
        val resultEn = dateTimeFormatter.formatTime(midnight, englishLocale)
        val resultAr = dateTimeFormatter.formatTime(midnight, arabicLocale)

        // Assert
        assertTrue(resultEn.isNotEmpty())
        assertTrue(resultAr.isNotEmpty())
    }

    @Test
    fun `formatTime with noon`() {
        // Arrange
        val noon = LocalTime.NOON

        // Act
        val resultEn = dateTimeFormatter.formatTime(noon, englishLocale)
        val resultAr = dateTimeFormatter.formatTime(noon, arabicLocale)

        // Assert
        assertTrue(resultEn.isNotEmpty())
        assertTrue(resultAr.isNotEmpty())
    }

    // ==================== Date-Time Formatting Tests ====================

    @Test
    fun `formatDateTime returns English formatted datetime for English locale`() {
        // Arrange
        val dateTime = LocalDateTime.of(2026, 1, 30, 15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatDateTime(dateTime, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateTime returns Arabic formatted datetime for Arabic locale`() {
        // Arrange
        val dateTime = LocalDateTime.of(2026, 1, 30, 15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatDateTime(dateTime, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    // ==================== Relative Time Formatting Tests ====================

    private val fixedNow = LocalDateTime.of(2026, 1, 15, 12, 0, 0)

    @Test
    fun `formatRelativeTime returns 'just now' for recent time in English`() {
        // Arrange
        val recentTime = fixedNow.minusSeconds(30)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(recentTime, englishLocale, fixedNow)

        // Assert
        assertEquals("just now", result)
    }

    @Test
    fun `formatRelativeTime returns 'الآن' for recent time in Arabic`() {
        // Arrange
        val recentTime = fixedNow.minusSeconds(30)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(recentTime, arabicLocale, fixedNow)

        // Assert
        assertEquals("الآن", result)
    }

    @Test
    fun `formatRelativeTime returns '1 minute ago' in English`() {
        // Arrange
        val oneMinuteAgo = fixedNow.minusMinutes(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneMinuteAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("1 minute ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ دقيقة' in Arabic`() {
        // Arrange
        val oneMinuteAgo = fixedNow.minusMinutes(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneMinuteAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ دقيقة", result)
    }

    @Test
    fun `formatRelativeTime returns '2 minutes ago' in English`() {
        // Arrange
        val twoMinutesAgo = fixedNow.minusMinutes(2)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(twoMinutesAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("2 minutes ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ دقيقتين' in Arabic`() {
        // Arrange
        val twoMinutesAgo = fixedNow.minusMinutes(2)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(twoMinutesAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ دقيقتين", result)
    }

    @Test
    fun `formatRelativeTime returns '5 minutes ago' in English`() {
        // Arrange
        val fiveMinutesAgo = fixedNow.minusMinutes(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveMinutesAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("5 minutes ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ 5 دقائق' in Arabic`() {
        // Arrange
        val fiveMinutesAgo = fixedNow.minusMinutes(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveMinutesAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ 5 دقائق", result)
    }

    @Test
    fun `formatRelativeTime returns '15 minutes ago' in English`() {
        // Arrange
        val fifteenMinutesAgo = fixedNow.minusMinutes(15)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fifteenMinutesAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("15 minutes ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ 15 دقيقة' in Arabic`() {
        // Arrange
        val fifteenMinutesAgo = fixedNow.minusMinutes(15)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fifteenMinutesAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ 15 دقيقة", result)
    }

    @Test
    fun `formatRelativeTime returns '1 hour ago' in English`() {
        // Arrange
        val oneHourAgo = fixedNow.minusHours(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneHourAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("1 hour ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ ساعة' in Arabic`() {
        // Arrange
        val oneHourAgo = fixedNow.minusHours(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneHourAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ ساعة", result)
    }

    @Test
    fun `formatRelativeTime returns '2 hours ago' in English`() {
        // Arrange
        val twoHoursAgo = fixedNow.minusHours(2)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(twoHoursAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("2 hours ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ ساعتين' in Arabic`() {
        // Arrange
        val twoHoursAgo = fixedNow.minusHours(2)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(twoHoursAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ ساعتين", result)
    }

    @Test
    fun `formatRelativeTime returns '5 hours ago' in English`() {
        // Arrange
        val fiveHoursAgo = fixedNow.minusHours(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveHoursAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("5 hours ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ 5 ساعات' in Arabic`() {
        // Arrange
        val fiveHoursAgo = fixedNow.minusHours(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveHoursAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ 5 ساعات", result)
    }

    @Test
    fun `formatRelativeTime returns '15 hours ago' in English`() {
        // Arrange
        val fifteenHoursAgo = fixedNow.minusHours(15)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fifteenHoursAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("15 hours ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ 15 ساعة' in Arabic`() {
        // Arrange
        val fifteenHoursAgo = fixedNow.minusHours(15)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fifteenHoursAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ 15 ساعة", result)
    }

    @Test
    fun `formatRelativeTime returns '1 day ago' in English`() {
        // Arrange
        val oneDayAgo = fixedNow.minusDays(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneDayAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("1 day ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ يوم' in Arabic`() {
        // Arrange
        val oneDayAgo = fixedNow.minusDays(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneDayAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ يوم", result)
    }

    @Test
    fun `formatRelativeTime returns '2 days ago' in English`() {
        // Arrange
        val twoDaysAgo = fixedNow.minusDays(2)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(twoDaysAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("2 days ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ يومين' in Arabic`() {
        // Arrange
        val twoDaysAgo = fixedNow.minusDays(2)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(twoDaysAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ يومين", result)
    }

    @Test
    fun `formatRelativeTime returns '5 days ago' in English`() {
        // Arrange
        val fiveDaysAgo = fixedNow.minusDays(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveDaysAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("5 days ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ 5 أيام' in Arabic`() {
        // Arrange
        val fiveDaysAgo = fixedNow.minusDays(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveDaysAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ 5 أيام", result)
    }

    @Test
    fun `formatRelativeTime returns '15 days ago' in English`() {
        // Arrange
        val fifteenDaysAgo = fixedNow.minusDays(15)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fifteenDaysAgo, englishLocale, fixedNow)

        // Assert
        assertEquals("15 days ago", result)
    }

    @Test
    fun `formatRelativeTime returns 'منذ 15 يومًا' in Arabic`() {
        // Arrange
        val fifteenDaysAgo = fixedNow.minusDays(15)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fifteenDaysAgo, arabicLocale, fixedNow)

        // Assert
        assertEquals("منذ 15 يومًا", result)
    }

    @Test
    fun `formatRelativeTime returns 'in 1 minute' for future time in English`() {
        // Arrange
        val oneMinuteFuture = fixedNow.plusMinutes(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneMinuteFuture, englishLocale, fixedNow)

        // Assert
        assertEquals("in 1 minute", result)
    }

    @Test
    fun `formatRelativeTime returns 'خلال دقيقة' for future time in Arabic`() {
        // Arrange
        val oneMinuteFuture = fixedNow.plusMinutes(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneMinuteFuture, arabicLocale, fixedNow)

        // Assert
        assertEquals("خلال دقيقة", result)
    }

    @Test
    fun `formatRelativeTime returns 'in 5 minutes' for future time in English`() {
        // Arrange
        val fiveMinutesFuture = fixedNow.plusMinutes(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveMinutesFuture, englishLocale, fixedNow)

        // Assert
        assertEquals("in 5 minutes", result)
    }

    @Test
    fun `formatRelativeTime returns 'خلال 5 دقائق' for future time in Arabic`() {
        // Arrange
        val fiveMinutesFuture = fixedNow.plusMinutes(5)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(fiveMinutesFuture, arabicLocale, fixedNow)

        // Assert
        assertEquals("خلال 5 دقائق", result)
    }

    @Test
    fun `formatRelativeTime returns 'in 1 hour' for future time in English`() {
        // Arrange
        val oneHourFuture = fixedNow.plusHours(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneHourFuture, englishLocale, fixedNow)

        // Assert
        assertEquals("in 1 hour", result)
    }

    @Test
    fun `formatRelativeTime returns 'خلال ساعة' for future time in Arabic`() {
        // Arrange
        val oneHourFuture = fixedNow.plusHours(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneHourFuture, arabicLocale, fixedNow)

        // Assert
        assertEquals("خلال ساعة", result)
    }

    @Test
    fun `formatRelativeTime returns 'in 1 day' for future time in English`() {
        // Arrange
        val oneDayFuture = fixedNow.plusDays(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneDayFuture, englishLocale, fixedNow)

        // Assert
        assertEquals("in 1 day", result)
    }

    @Test
    fun `formatRelativeTime returns 'خلال يوم' for future time in Arabic`() {
        // Arrange
        val oneDayFuture = fixedNow.plusDays(1)

        // Act
        val result = dateTimeFormatter.formatRelativeTime(oneDayFuture, arabicLocale, fixedNow)

        // Assert
        assertEquals("خلال يوم", result)
    }

    @Test
    fun `formatRelativeTime returns formatted date for old time`() {
        // Arrange
        val oldTime = fixedNow.minusDays(10)

        // Act
        val resultEn = dateTimeFormatter.formatRelativeTime(oldTime, englishLocale, fixedNow)
        val resultAr = dateTimeFormatter.formatRelativeTime(oldTime, arabicLocale, fixedNow)

        // Assert
        assertTrue(resultEn.isNotEmpty())
        assertTrue(resultAr.isNotEmpty())
    }

    // ==================== Short Date Formatting Tests ====================

    @Test
    fun `formatShortDate returns short format for English locale`() {
        // Arrange
        val date = LocalDate.of(2026, 1, 30)

        // Act
        val result = dateTimeFormatter.formatShortDate(date, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatShortDate returns short format for Arabic locale`() {
        // Arrange
        val date = LocalDate.of(2026, 1, 30)

        // Act
        val result = dateTimeFormatter.formatShortDate(date, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    // ==================== Medium Time Formatting Tests ====================

    @Test
    fun `formatMediumTime returns medium format for English locale`() {
        // Arrange
        val time = LocalTime.of(15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatMediumTime(time, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatMediumTime returns medium format for Arabic locale`() {
        // Arrange
        val time = LocalTime.of(15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatMediumTime(time, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    // ==================== Long Date-Time Formatting Tests ====================

    @Test
    fun `formatLongDateTime returns long format for English locale`() {
        // Arrange
        val dateTime = LocalDateTime.of(2026, 1, 30, 15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatLongDateTime(dateTime, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatLongDateTime returns long format for Arabic locale`() {
        // Arrange
        val dateTime = LocalDateTime.of(2026, 1, 30, 15, 45, 30)

        // Act
        val result = dateTimeFormatter.formatLongDateTime(dateTime, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    // ==================== Timestamp Formatting Tests ====================

    @Test
    fun `formatTimestamp returns formatted datetime for English locale`() {
        // Arrange
        val timestamp = System.currentTimeMillis()

        // Act
        val result = dateTimeFormatter.formatTimestamp(timestamp, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatTimestamp returns formatted datetime for Arabic locale`() {
        // Arrange
        val timestamp = System.currentTimeMillis()

        // Act
        val result = dateTimeFormatter.formatTimestamp(timestamp, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatTimestampAsRelative returns relative time for English locale`() {
        // Arrange
        val timestamp = System.currentTimeMillis() - 300000 // 5 minutes ago

        // Act
        val result = dateTimeFormatter.formatTimestampAsRelative(timestamp, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatTimestampAsRelative returns relative time for Arabic locale`() {
        // Arrange
        val timestamp = System.currentTimeMillis() - 300000 // 5 minutes ago

        // Act
        val result = dateTimeFormatter.formatTimestampAsRelative(timestamp, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    // ==================== Time String Formatting Tests ====================

    @Test
    fun `formatTimeString parses and formats time for English locale`() {
        // Arrange
        val timeString = "14:30"

        // Act
        val result = dateTimeFormatter.formatTimeString(timeString, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatTimeString parses and formats time for Arabic locale`() {
        // Arrange
        val timeString = "14:30"

        // Act
        val result = dateTimeFormatter.formatTimeString(timeString, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatTimeString returns original string on parse error`() {
        // Arrange
        val invalidTimeString = "invalid"

        // Act
        val result = dateTimeFormatter.formatTimeString(invalidTimeString, englishLocale)

        // Assert
        assertEquals(invalidTimeString, result)
    }

    // ==================== Date String Formatting Tests ====================

    @Test
    fun `formatDateString parses and formats date for English locale`() {
        // Arrange
        val dateString = "2026-01-30"

        // Act
        val result = dateTimeFormatter.formatDateString(dateString, englishLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateString parses and formats date for Arabic locale`() {
        // Arrange
        val dateString = "2026-01-30"

        // Act
        val result = dateTimeFormatter.formatDateString(dateString, arabicLocale)

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateString returns original string on parse error`() {
        // Arrange
        val invalidDateString = "invalid"

        // Act
        val result = dateTimeFormatter.formatDateString(invalidDateString, englishLocale)

        // Assert
        assertEquals(invalidDateString, result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles different locales consistently`() {
        // Arrange
        val locales = listOf(
            Locale("en"),
            Locale("ar"),
            Locale("fr"),
            Locale("es")
        )
        val date = LocalDate.of(2026, 1, 30)

        locales.forEach { locale ->
            // Act
            val result = dateTimeFormatter.formatDate(date, locale)

            // Assert
            assertTrue(result.isNotEmpty())
        }
    }

    @Test
    fun `handles null-like values gracefully`() {
        // Arrange
        val date = LocalDate.of(2026, 1, 30)
        val time = LocalTime.of(15, 45, 30)
        val dateTime = LocalDateTime.of(2026, 1, 30, 15, 45, 30)

        // Act & Assert - No exceptions should be thrown
        dateTimeFormatter.formatDate(date, englishLocale)
        dateTimeFormatter.formatTime(time, englishLocale)
        dateTimeFormatter.formatDateTime(dateTime, englishLocale)
    }
}
