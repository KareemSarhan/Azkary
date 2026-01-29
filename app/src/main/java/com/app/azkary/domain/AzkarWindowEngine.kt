package com.app.azkary.domain

import com.app.azkary.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for calculating Azkar windows based on prayer times
 * 
 * Window Rules:
 * - MORNING: Fajr → Asr
 * - NIGHT: Asr → Isha  
 * - SLEEP: Isha → next day Fajr
 */
@Singleton
class AzkarWindowEngine @Inject constructor() {

    /**
     * Calculates current and next Azkar windows based on prayer times
     * 
     * @param currentTime Current time to evaluate against
     * @param todayTimes Today's prayer times
     * @param tomorrowTimes Tomorrow's prayer times (for sleep window calculation)
     * @return Window calculation result with current and next windows
     */
    fun calculateWindows(
        currentTime: Instant,
        todayTimes: DayPrayerTimes,
        tomorrowTimes: DayPrayerTimes? = null
    ): WindowCalculationResult {
        
        val todayInstants = todayTimes.toInstants()
        val tomorrowInstants = tomorrowTimes?.toInstants()
        
        val todayWindows = generateWindowsForDay(todayInstants, tomorrowInstants)
        
        val currentWindow = todayWindows.find { window ->
            currentTime >= window.start && currentTime < window.end
        }
        
        val nextWindow = findNextWindow(currentTime, todayWindows, tomorrowInstants)
        
        return WindowCalculationResult(
            currentWindow = currentWindow,
            nextWindow = nextWindow,
            todayTimes = todayTimes,
            tomorrowTimes = tomorrowTimes
        )
    }

    /**
     * Generates all Azkar windows for a given day
     */
    private fun generateWindowsForDay(
        today: DayPrayerInstants,
        tomorrow: DayPrayerInstants?
    ): List<AzkarSchedule> {
        
        val windows = mutableListOf<AzkarSchedule>()
        
        // MORNING window: Fajr to Asr
        windows.add(
            AzkarSchedule(
                window = AzkarWindow.MORNING,
                start = today.fajr,
                end = today.asr,
                date = today.date
            )
        )
        
        // NIGHT window: Asr to Isha
        windows.add(
            AzkarSchedule(
                window = AzkarWindow.NIGHT,
                start = today.asr,
                end = today.isha,
                date = today.date
            )
        )
        
        // SLEEP window: Isha to next day Fajr
        val nextDayFajr = tomorrow?.fajr ?: today.fajr.plus(java.time.Duration.ofDays(1))
        windows.add(
            AzkarSchedule(
                window = AzkarWindow.SLEEP,
                start = today.isha,
                end = nextDayFajr,
                date = today.date
            )
        )
        
        return windows
    }

    /**
     * Finds the next window after the current time
     */
    private fun findNextWindow(
        currentTime: Instant,
        todayWindows: List<AzkarSchedule>,
        tomorrowInstants: DayPrayerInstants?
    ): AzkarSchedule? {
        
        // Find next window today
        val nextTodayWindow = todayWindows.find { window ->
            currentTime < window.start
        }
        
        if (nextTodayWindow != null) {
            return nextTodayWindow
        }
        
        // If no more windows today, return first window of tomorrow
        tomorrowInstants?.let { tomorrow ->
            val tomorrowWindows = generateWindowsForDay(tomorrow, null)
            return tomorrowWindows.firstOrNull()
        }
        
        // Fallback: return first window of today (next day's cycle)
        return todayWindows.firstOrNull()
    }

    /**
     * Gets the window schedule for a specific date
     */
    fun getScheduleForDate(
        date: LocalDate,
        prayerTimes: DayPrayerTimes,
        nextDayPrayerTimes: DayPrayerTimes? = null
    ): List<AzkarSchedule> {
        
        val todayInstants = prayerTimes.toInstants()
        val tomorrowInstants = nextDayPrayerTimes?.toInstants()
        
        return generateWindowsForDay(todayInstants, tomorrowInstants)
    }

    /**
     * Determines if a given time falls within a specific window
     */
    fun isInWindow(
        time: Instant,
        window: AzkarWindow,
        prayerTimes: DayPrayerTimes,
        nextDayPrayerTimes: DayPrayerTimes? = null
    ): Boolean {
        
        val schedules = getScheduleForDate(prayerTimes.date, prayerTimes, nextDayPrayerTimes)
        
        return schedules.any { schedule ->
            schedule.window == window && 
            time >= schedule.start && 
            time < schedule.end
        }
    }
}