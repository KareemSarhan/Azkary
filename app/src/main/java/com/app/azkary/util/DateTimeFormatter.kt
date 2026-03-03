package com.app.azkary.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for locale-aware date and time formatting.
 * 
 * This class provides methods to format dates, times, and date-time values
 * according to the specified locale, with proper handling for RTL contexts
 * and appropriate numeral display for Arabic.
 */
@Singleton
class DateTimeFormatter @Inject constructor() {

    /**
     * Formats a LocalDate according to the specified locale.
     * 
     * Uses locale-specific date formatting (e.g., "January 30, 2026" for English,
     * "٣٠ يناير ٢٠٢٦" for Arabic with appropriate numerals).
     * 
     * @param date The LocalDate to format
     * @param locale The locale to use for formatting
     * @return Formatted date string
     */
    fun formatDate(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
        return date.format(formatter)
    }

    /**
     * Formats a LocalTime according to the specified locale.
     * 
     * Uses locale-specific time formatting (e.g., "3:45:30 PM" for English,
     * "٣:٤٥:٣٠ م" for Arabic with appropriate numerals and AM/PM indicator).
     * 
     * @param time The LocalTime to format
     * @param locale The locale to use for formatting
     * @return Formatted time string
     */
    fun formatTime(time: LocalTime, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
        return time.format(formatter)
    }

    /**
     * Formats a LocalDateTime according to the specified locale.
     * 
     * Uses locale-specific date-time formatting (e.g., "January 30, 2026, 3:45 PM" for English,
     * "٣٠ يناير ٢٠٢٦، ٣:٤٥ م" for Arabic with appropriate numerals).
     * 
     * @param dateTime The LocalDateTime to format
     * @param locale The locale to use for formatting
     * @return Formatted date-time string
     */
    fun formatDateTime(dateTime: LocalDateTime, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
        return dateTime.format(formatter)
    }

    /**
     * Formats a LocalDateTime as a relative time string (e.g., "2 hours ago", "in 3 days").
     * 
     * This method calculates the difference between the given date-time and the current time,
     * then returns a human-readable relative time string in the specified locale.
     * 
     * @param dateTime The LocalDateTime to format as relative time
     * @param locale The locale to use for formatting
     * @return Relative time string (e.g., "2 hours ago", "in 3 days", "just now")
     */
    fun formatRelativeTime(dateTime: LocalDateTime, locale: Locale, now: LocalDateTime = LocalDateTime.now()): String {
        val diff = ChronoUnit.SECONDS.between(dateTime, now)

        return when {
            diff < 0 -> {
                // Future time (must be checked first since negative diff is < all positive thresholds)
                val futureDiff = -diff
                when {
                    futureDiff < 60 -> {
                        // Less than a minute in the future
                        if (locale.language == "ar") {
                            "الآن"
                        } else {
                            "just now"
                        }
                    }
                    futureDiff < 3600 -> {
                        // Less than an hour in the future
                        val minutes = (futureDiff / 60).toInt()
                        if (locale.language == "ar") {
                            when (minutes) {
                                1 -> "خلال دقيقة"
                                2 -> "خلال دقيقتين"
                                in 3..10 -> "خلال $minutes دقائق"
                                else -> "خلال $minutes دقيقة"
                            }
                        } else {
                            when (minutes) {
                                1 -> "in 1 minute"
                                else -> "in $minutes minutes"
                            }
                        }
                    }
                    futureDiff < 86400 -> {
                        // Less than a day in the future
                        val hours = (futureDiff / 3600).toInt()
                        if (locale.language == "ar") {
                            when (hours) {
                                1 -> "خلال ساعة"
                                2 -> "خلال ساعتين"
                                in 3..10 -> "خلال $hours ساعات"
                                else -> "خلال $hours ساعة"
                            }
                        } else {
                            when (hours) {
                                1 -> "in 1 hour"
                                else -> "in $hours hours"
                            }
                        }
                    }
                    futureDiff < 2592000 -> {
                        // Less than 30 days in the future
                        val days = (futureDiff / 86400).toInt()
                        if (locale.language == "ar") {
                            when (days) {
                                1 -> "خلال يوم"
                                2 -> "خلال يومين"
                                in 3..10 -> "خلال $days أيام"
                                else -> "خلال $days يومًا"
                            }
                        } else {
                            when (days) {
                                1 -> "in 1 day"
                                else -> "in $days days"
                            }
                        }
                    }
                    else -> {
                        // More than 30 days in the future, return formatted date
                        formatDate(dateTime.toLocalDate(), locale)
                    }
                }
            }
            diff < 60 -> {
                // Less than a minute ago
                if (locale.language == "ar") {
                    "الآن"
                } else {
                    "just now"
                }
            }
            diff < 3600 -> {
                // Less than an hour ago
                val minutes = (diff / 60).toInt()
                if (locale.language == "ar") {
                    when (minutes) {
                        1 -> "منذ دقيقة"
                        2 -> "منذ دقيقتين"
                        in 3..10 -> "منذ $minutes دقائق"
                        else -> "منذ $minutes دقيقة"
                    }
                } else {
                    when (minutes) {
                        1 -> "1 minute ago"
                        else -> "$minutes minutes ago"
                    }
                }
            }
            diff < 86400 -> {
                // Less than a day ago
                val hours = (diff / 3600).toInt()
                if (locale.language == "ar") {
                    when (hours) {
                        1 -> "منذ ساعة"
                        2 -> "منذ ساعتين"
                        in 3..10 -> "منذ $hours ساعات"
                        else -> "منذ $hours ساعة"
                    }
                } else {
                    when (hours) {
                        1 -> "1 hour ago"
                        else -> "$hours hours ago"
                    }
                }
            }
            diff < 2592000 -> {
                // Less than 30 days ago
                val days = (diff / 86400).toInt()
                if (locale.language == "ar") {
                    when (days) {
                        1 -> "منذ يوم"
                        2 -> "منذ يومين"
                        in 3..10 -> "منذ $days أيام"
                        else -> "منذ $days يومًا"
                    }
                } else {
                    when (days) {
                        1 -> "1 day ago"
                        else -> "$days days ago"
                    }
                }
            }
            else -> {
                // More than 30 days ago, return formatted date
                formatDate(dateTime.toLocalDate(), locale)
            }
        }
    }

    /**
     * Formats a LocalDate with a short style (e.g., "1/30/26" for English).
     * 
     * @param date The LocalDate to format
     * @param locale The locale to use for formatting
     * @return Formatted short date string
     */
    fun formatShortDate(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(locale)
        return date.format(formatter)
    }

    /**
     * Formats a LocalTime with a medium style (e.g., "3:45:30 PM" for English).
     * 
     * @param time The LocalTime to format
     * @param locale The locale to use for formatting
     * @return Formatted medium time string
     */
    fun formatMediumTime(time: LocalTime, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            .withLocale(locale)
        return time.format(formatter)
    }

    /**
     * Formats a LocalDateTime with a long style (e.g., "Friday, January 30, 2026 at 3:45:30 PM EST" for English).
     * 
     * @param dateTime The LocalDateTime to format
     * @param locale The locale to use for formatting
     * @return Formatted long date-time string
     */
    fun formatLongDateTime(dateTime: LocalDateTime, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM)
            .withLocale(locale)
        return dateTime.format(formatter)
    }

    /**
     * Formats a date string from a timestamp (milliseconds since epoch).
     * 
     * @param timestamp The timestamp in milliseconds
     * @param locale The locale to use for formatting
     * @return Formatted date string
     */
    fun formatTimestamp(timestamp: Long, locale: Locale): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        return formatDateTime(dateTime, locale)
    }

    /**
     * Formats a timestamp as relative time.
     * 
     * @param timestamp The timestamp in milliseconds
     * @param locale The locale to use for formatting
     * @return Relative time string
     */
    fun formatTimestampAsRelative(timestamp: Long, locale: Locale): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        return formatRelativeTime(dateTime, locale)
    }

    /**
     * Formats a time string (HH:mm format) to LocalTime and then formats it according to locale.
     * 
     * @param timeString The time string in HH:mm format (e.g., "14:30")
     * @param locale The locale to use for formatting
     * @return Formatted time string
     */
    fun formatTimeString(timeString: String, locale: Locale): String {
        return try {
            val time = LocalTime.parse(timeString)
            formatTime(time, locale)
        } catch (e: Exception) {
            // Return original string if parsing fails
            timeString
        }
    }

    /**
     * Formats a date string (yyyy-MM-dd format) to LocalDate and then formats it according to locale.
     * 
     * @param dateString The date string in yyyy-MM-dd format (e.g., "2026-01-30")
     * @param locale The locale to use for formatting
     * @return Formatted date string
     */
    fun formatDateString(dateString: String, locale: Locale): String {
        return try {
            val date = LocalDate.parse(dateString)
            formatDate(date, locale)
        } catch (e: Exception) {
            // Return original string if parsing fails
            dateString
        }
    }
}
