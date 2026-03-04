package com.app.azkary.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.time.LocalTime

/**
 * Unit tests for PrayerTimeParser
 *
 * Tests cover:
 * - Parsing standard time formats (HH:mm)
 * - Parsing API time formats with timezone info (HH:mm (UTC), HH:mm (+03))
 * - Parsing edge cases (midnight, noon)
 * - Invalid format handling
 * - Invalid time value handling
 * - Validation function
 */
@RunWith(Parameterized::class)
class PrayerTimeParserParameterizedTest(
    private val testName: String,
    private val input: String,
    private val expectedHour: Int,
    private val expectedMinute: Int
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            // Standard formats
            arrayOf("Simple time - 06:06", "06:06", 6, 6),
            arrayOf("Simple time - 18:30", "18:30", 18, 30),
            arrayOf("Simple time - 00:00", "00:00", 0, 0),
            arrayOf("Simple time - 23:59", "23:59", 23, 59),
            arrayOf("Simple time - 12:00", "12:00", 12, 0),
            arrayOf("Simple time - 05:12", "05:12", 5, 12),

            // API formats with timezone info
            arrayOf("With UTC - 06:06 (UTC)", "06:06 (UTC)", 6, 6),
            arrayOf("With offset - 05:12 (+03)", "05:12 (+03)", 5, 12),
            arrayOf("With offset - 18:30 (-05)", "18:30 (-05)", 18, 30),
            arrayOf("With offset - 12:00 (+00)", "12:00 (+00)", 12, 0),

            // With leading/trailing whitespace
            arrayOf("Leading space -  06:06", " 06:06", 6, 6),
            arrayOf("Trailing space - 06:06 ", "06:06 ", 6, 6),
            arrayOf("Both spaces -  06:06 ", " 06:06 ", 6, 6),

            // Edge hours
            arrayOf("Hour 00 - 00:30", "00:30", 0, 30),
            arrayOf("Hour 01 - 01:15", "01:15", 1, 15),
            arrayOf("Hour 11 - 11:45", "11:45", 11, 45),
            arrayOf("Hour 12 - 12:30", "12:30", 12, 30),
            arrayOf("Hour 13 - 13:00", "13:00", 13, 0),
            arrayOf("Hour 22 - 22:45", "22:45", 22, 45),
            arrayOf("Hour 23 - 23:59", "23:59", 23, 59),

            // Edge minutes
            arrayOf("Minute 00 - 10:00", "10:00", 10, 0),
            arrayOf("Minute 01 - 10:01", "10:01", 10, 1),
            arrayOf("Minute 30 - 10:30", "10:30", 10, 30),
            arrayOf("Minute 59 - 10:59", "10:59", 10, 59),
        )
    }

    @Test
    fun testParseTimeString() {
        // Act
        val result = PrayerTimeParser.parseTimeString(input)

        // Assert
        assertEquals("Hour mismatch for test: $testName", expectedHour, result.hour)
        assertEquals("Minute mismatch for test: $testName", expectedMinute, result.minute)
    }
}

class PrayerTimeParserTest {

    // ==================== Valid Parsing Tests ====================

    @Test
    fun `parseTimeString returns LocalTime with correct values`() {
        // Act
        val result = PrayerTimeParser.parseTimeString("14:30")

        // Assert
        assertEquals(LocalTime.of(14, 30), result)
    }

    @Test
    fun `parseTimeString handles midnight`() {
        // Act
        val result = PrayerTimeParser.parseTimeString("00:00")

        // Assert
        assertEquals(LocalTime.MIDNIGHT, result)
    }

    @Test
    fun `parseTimeString handles noon`() {
        // Act
        val result = PrayerTimeParser.parseTimeString("12:00")

        // Assert
        assertEquals(LocalTime.NOON, result)
    }

    @Test
    fun `parseTimeString handles last minute of day`() {
        // Act
        val result = PrayerTimeParser.parseTimeString("23:59")

        // Assert
        assertEquals(LocalTime.of(23, 59), result)
    }

    @Test
    fun `parseTimeString extracts only hour and minute from longer strings`() {
        // Act
        val result = PrayerTimeParser.parseTimeString("06:06 (UTC) with extra text")

        // Assert - should extract just the time part
        assertEquals(LocalTime.of(6, 6), result)
    }

    // ==================== Invalid Format Tests ====================

    @Test
    fun `parseTimeString throws ParsingException for empty string`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    @Test
    fun `parseTimeString throws ParsingException for blank string`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("   ")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    @Test
    fun `parseTimeString throws ParsingException for non-time string`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("not a time")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    @Test
    fun `parseTimeString throws ParsingException for missing minutes`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("14")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    @Test
    fun `parseTimeString throws ParsingException for wrong separator`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("14.30")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    @Test
    fun `parseTimeString throws ParsingException for single digit hour`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("6:30")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    @Test
    fun `parseTimeString throws ParsingException for single digit minute`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("06:3")
        }
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    // ==================== Invalid Value Tests ====================

    @Test
    fun `parseTimeString throws ParsingException for hour 24`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("24:00")
        }
        assertTrue(exception.message!!.contains("Invalid time values"))
    }

    @Test
    fun `parseTimeString throws ParsingException for hour 99`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("99:00")
        }
        assertTrue(exception.message!!.contains("Invalid time values"))
    }

    @Test
    fun `parseTimeString throws ParsingException for minute 60`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("12:60")
        }
        assertTrue(exception.message!!.contains("Invalid time values"))
    }

    @Test
    fun `parseTimeString throws ParsingException for minute 99`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("12:99")
        }
        assertTrue(exception.message!!.contains("Invalid time values"))
    }

    @Test
    fun `parseTimeString throws ParsingException for negative hour`() {
        // Act & Assert
        val exception = assertThrows(ParsingException::class.java) {
            PrayerTimeParser.parseTimeString("-1:30")
        }
        // Regex won't match negative numbers, so it should be format error
        assertTrue(exception.message!!.contains("Invalid time format"))
    }

    // ==================== isValidTimeFormat Tests ====================

    @Test
    fun `isValidTimeFormat returns true for valid time format`() {
        // Act & Assert
        assertTrue(PrayerTimeParser.isValidTimeFormat("14:30"))
    }

    @Test
    fun `isValidTimeFormat returns true for valid time with timezone`() {
        // Act & Assert
        assertTrue(PrayerTimeParser.isValidTimeFormat("06:06 (UTC)"))
    }

    @Test
    fun `isValidTimeFormat returns false for empty string`() {
        // Act & Assert
        assertFalse(PrayerTimeParser.isValidTimeFormat(""))
    }

    @Test
    fun `isValidTimeFormat returns false for invalid format`() {
        // Act & Assert
        assertFalse(PrayerTimeParser.isValidTimeFormat("not a time"))
    }

    @Test
    fun `isValidTimeFormat returns false for single digit hour`() {
        // Act & Assert
        assertFalse(PrayerTimeParser.isValidTimeFormat("6:30"))
    }

    @Test
    fun `isValidTimeFormat returns false for wrong separator`() {
        // Act & Assert
        assertFalse(PrayerTimeParser.isValidTimeFormat("14.30"))
    }

    @Test
    fun `isValidTimeFormat handles string longer than 5 chars`() {
        // Act & Assert - should only check first 5 chars
        assertTrue(PrayerTimeParser.isValidTimeFormat("14:30 with extra text"))
    }

    @Test
    fun `isValidTimeFormat handles whitespace`() {
        // Act & Assert - trimmed should work
        assertTrue(PrayerTimeParser.isValidTimeFormat(" 14:30 "))
    }

    // ==================== ParsingException Tests ====================

    @Test
    fun `ParsingException has correct message`() {
        // Act
        val exception = ParsingException("Test message")

        // Assert
        assertEquals("Test message", exception.message)
        assertEquals(null, exception.cause)
    }

    @Test
    fun `ParsingException preserves cause`() {
        // Arrange
        val cause = NumberFormatException("Original error")

        // Act
        val exception = ParsingException("Test message", cause)

        // Assert
        assertEquals("Test message", exception.message)
        assertEquals(cause, exception.cause)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `parseTimeString handles various timezone formats`() {
        // Test different timezone format variations
        val testCases = listOf(
            "06:06 (UTC)" to LocalTime.of(6, 6),
            "05:12 (+03)" to LocalTime.of(5, 12),
            "18:30 (-05)" to LocalTime.of(18, 30),
            "12:00 (GMT+1)" to LocalTime.of(12, 0),
            "06:06 (+0530)" to LocalTime.of(6, 6),
        )

        testCases.forEach { (input, expected) ->
            assertEquals("Failed for input: $input", expected, PrayerTimeParser.parseTimeString(input))
        }
    }

    @Test
    fun `parseTimeString only extracts first 5 chars for time`() {
        // Arrange - string with multiple time-like patterns
        val input = "12:34 and 56:78"

        // Act
        val result = PrayerTimeParser.parseTimeString(input)

        // Assert - should parse 12:34
        assertEquals(LocalTime.of(12, 34), result)
    }
}
