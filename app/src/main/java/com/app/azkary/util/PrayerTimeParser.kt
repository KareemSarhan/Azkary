package com.app.azkary.util

import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Utility class for parsing prayer time strings from API responses
 * 
 * API returns times in formats like:
 * - "06:06 (UTC)"
 * - "05:12 (+03)"
 * - "18:30"
 */
object PrayerTimeParser {
    
    private val TIME_PATTERN = Regex("""^(\d{2}):(\d{2})""")
    
    /**
     * Extracts HH:mm from prayer time string and converts to LocalTime
     * 
     * @param timeString Time string from API (e.g., "06:06 (UTC)")
     * @return Parsed LocalTime
     * @throws ParsingException if time format is invalid
     */
    fun parseTimeString(timeString: String): LocalTime {
        val trimmed = timeString.trim()
        
        val matchResult = TIME_PATTERN.find(trimmed)
            ?: throw ParsingException("Invalid time format: $timeString")
        
        val (hourStr, minuteStr) = matchResult.destructured
        
        return try {
            val hour = hourStr.toInt()
            val minute = minuteStr.toInt()
            
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw ParsingException("Invalid time values: $hour:$minute")
            }
            
            LocalTime.of(hour, minute)
        } catch (e: NumberFormatException) {
            throw ParsingException("Failed to parse time numbers: $timeString", e)
        } catch (e: DateTimeParseException) {
            throw ParsingException("Failed to create LocalTime: $timeString", e)
        }
    }
    
    /**
     * Checks if a string looks like a valid prayer time format
     */
    fun isValidTimeFormat(timeString: String): Boolean {
        return TIME_PATTERN.matches(timeString.trim().take(5))
    }
}

/**
 * Exception thrown when prayer time parsing fails
 */
class ParsingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)