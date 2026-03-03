package com.app.azkary.testutil

import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.dto.PrayerDayDto
import com.app.azkary.data.network.dto.PrayerTimingsDto
import com.app.azkary.data.network.dto.DateDto
import com.app.azkary.data.network.dto.GregorianDateDto
import com.app.azkary.data.network.dto.MonthDto
import com.app.azkary.data.network.dto.MetaDto

/**
 * Test utilities for creating mock prayer times data
 */
object PrayerTimesTestData {

    /**
     * Creates a valid calendar response with specified number of days
     */
    fun createCalendarResponse(
        year: Int,
        month: Int,
        days: Int,
        timezone: String = "Asia/Riyadh",
        baseFajrHour: Int = 5,
        baseFajrMinute: Int = 0
    ): PrayerCalendarResponse {
        val dayDtos = (1..days).map { day ->
            createPrayerDayDto(
                year = year,
                month = month,
                day = day,
                timezone = timezone,
                fajrHour = baseFajrHour,
                fajrMinute = baseFajrMinute + (day % 5) // Slight variation per day
            )
        }

        return PrayerCalendarResponse(
            code = 200,
            status = "OK",
            data = dayDtos
        )
    }

    /**
     * Creates a single prayer day DTO
     */
    fun createPrayerDayDto(
        year: Int,
        month: Int,
        day: Int,
        timezone: String = "Asia/Riyadh",
        fajrHour: Int = 5,
        fajrMinute: Int = 0
    ): PrayerDayDto {
        val dayStr = day.toString().padStart(2, '0')
        val monthStr = month.toString().padStart(2, '0')
        
        // Calculate other prayer times based on Fajr
        val sunriseMinute = fajrMinute + 10
        val sunriseHour = if (sunriseMinute >= 60) fajrHour + 1 else fajrHour
        val adjustedSunriseMinute = sunriseMinute % 60

        return PrayerDayDto(
            timings = PrayerTimingsDto(
                Fajr = formatTime(fajrHour, fajrMinute),
                Sunrise = formatTime(sunriseHour, adjustedSunriseMinute),
                Dhuhr = "12:05",
                Asr = "15:25",
                Sunset = "18:02",
                Maghrib = "18:00",
                Isha = "19:15",
                Firstthird = "00:45",
                Midnight = "01:30",
                Lastthird = "02:15"
            ),
            date = DateDto(
                gregorian = GregorianDateDto(
                    date = "$dayStr-$monthStr-$year",
                    day = dayStr,
                    month = MonthDto(number = month, en = getMonthName(month)),
                    year = year.toString()
                )
            ),
            meta = MetaDto(timezone = timezone)
        )
    }

    /**
     * Creates an error response JSON string
     */
    fun createErrorResponse(code: Int, status: String, message: String = ""): String {
        return """
        {
          "code": $code,
          "status": "$status"${if (message.isNotEmpty()) "," else ""}
          ${if (message.isNotEmpty()) """"data": "$message""" else ""}
        }
        """.trimIndent()
    }

    /**
     * Creates a rate limit response with Retry-After header simulation
     */
    fun createRateLimitResponse(retryAfter: Int? = null): Map<String, String> {
        return buildMap {
            put("code", "429")
            put("status", "Too Many Requests")
            retryAfter?.let { put("Retry-After", it.toString()) }
        }
    }

    /**
     * Sample locations for testing
     */
    object Locations {
        val RIYADH = Triple(24.7136, 46.6753, "Asia/Riyadh")
        val LONDON = Triple(51.5074, -0.1278, "Europe/London")
        val NEW_YORK = Triple(40.7128, -74.0060, "America/New_York")
        val DUBAI = Triple(25.2048, 55.2708, "Asia/Dubai")
        val CAIRO = Triple(30.0444, 31.2357, "Africa/Cairo")
        val JAKARTA = Triple(-6.2088, 106.8456, "Asia/Jakarta")
    }

    /**
     * Calculation methods supported by Aladhan API
     */
    object CalculationMethods {
        const val SHIA_ITHNA_ASHARI = 0
        const val KUWAIT = 1
        const val ISNA = 2
        const val MWL = 3
        const val MAKKAH = 4 // Umm Al-Qura
        const val EGYPT = 5
        const val TEHRAN = 7
        const val GULF = 8
        const val QATAR = 9
        const val SINGAPORE = 10
        const val FRANCE = 11
        const val TURKEY = 12
        const val RUSSIA = 13
        const val DUBAI = 14
    }

    /**
     * Juristic schools for Asr calculation
     */
    object JuristicSchools {
        const val SHAFI = 0 // Earlier Asr
        const val HANAFI = 1 // Later Asr
    }

    // Helper functions
    private fun formatTime(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }
    }
}

/**
 * Extension functions for test assertions
 */
fun PrayerCalendarResponse.assertSuccessful() {
    assert(code == 200) { "Expected successful response code 200, got $code" }
    assert(status == "OK") { "Expected status OK, got $status" }
    assert(data.isNotEmpty()) { "Expected non-empty data" }
}

fun PrayerDayDto.assertValidTimings() {
    assert(timings.Fajr.isNotBlank()) { "Fajr time should not be blank" }
    assert(timings.Dhuhr.isNotBlank()) { "Dhuhr time should not be blank" }
    assert(timings.Asr.isNotBlank()) { "Asr time should not be blank" }
    assert(timings.Maghrib.isNotBlank()) { "Maghrib time should not be blank" }
    assert(timings.Isha.isNotBlank()) { "Isha time should not be blank" }
    assert(meta.timezone.isNotBlank()) { "Timezone should not be blank" }
}
