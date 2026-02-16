package com.app.azkary.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Represents the different Azkar time windows based on prayer times
 */
enum class AzkarWindow {
    MORNING,    // Fajr to Asr
    NIGHT,      // Asr to Isha  
    SLEEP       // Isha to next day Fajr
}

/**
 * Represents a scheduled Azkar window with start and end times
 */
data class AzkarSchedule(
    val window: AzkarWindow,
    val start: Instant,
    val end: Instant,
    val date: LocalDate
)

/**
 * Represents prayer times for a specific day
 */
data class DayPrayerTimes(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val timezone: ZoneId,
    val firstthird: LocalTime,
    val midnight: LocalTime,
    val lastthird: LocalTime
) {
    
    /**
     * Converts prayer times to Instant values for the given date
     */
    fun toInstants(): DayPrayerInstants {
        return DayPrayerInstants(
            date = date,
            fajr = ZonedDateTime.of(date, fajr, timezone).toInstant(),
            dhuhr = ZonedDateTime.of(date, dhuhr, timezone).toInstant(),
            asr = ZonedDateTime.of(date, asr, timezone).toInstant(),
            maghrib = ZonedDateTime.of(date, maghrib, timezone).toInstant(),
            isha = ZonedDateTime.of(date, isha, timezone).toInstant(),
            timezone = timezone
        )
    }
}

/**
 * Prayer times as Instant values for scheduling
 */
data class DayPrayerInstants(
    val date: LocalDate,
    val fajr: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
    val timezone: ZoneId
)

/**
 * Result of window calculation with current and next windows
 */
data class WindowCalculationResult(
    val currentWindow: AzkarSchedule?,
    val nextWindow: AzkarSchedule?,
    val todayTimes: DayPrayerTimes?,
    val tomorrowTimes: DayPrayerTimes?
)