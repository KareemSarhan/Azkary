package com.app.azkary

import com.app.azkary.data.network.AladhanApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit

/**
 * Contract tests for Aladhan API
 * These tests verify that the API contract remains stable and returns expected data formats.
 * Tests run against the live API to detect breaking changes.
 */
class AladhanLiveContractTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun createApi(): AladhanApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(AladhanApiService::class.java)
    }

    @Test
    fun `aladhan calendar endpoint contract - basic response structure`() = runTest {
        val api = createApi()

        // Pick a stable, past month to avoid "today" boundary weirdness
        val year = 2026
        val month = 1
        val lat = 24.7136
        val lng = 46.6753
        val method = 4
        val school = 0

        val resp = api.getMonthlyCalendar(
            year = year,
            month = month,
            latitude = lat,
            longitude = lng,
            method = method,
            school = school
        )

        // --- Contract assertions ---
        assertEquals("Expected Aladhan wrapper code=200", 200, resp.code())
        assertTrue("Expected non-empty data array", resp.body()?.data?.isNotEmpty() == true)

        val first = resp.body()!!.data.first()
        // Required fields exist
        assertNotNull("Timings should not be null", first.timings)
        assertNotNull("Meta should not be null", first.meta)
        assertNotNull("Date should not be null", first.date)
    }

    @Test
    fun `aladhan calendar endpoint - timezone format validation`() = runTest {
        val api = createApi()

        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        val first = resp.body()!!.data.first()
        val tz = first.meta.timezone
        assertTrue("Expected non-empty timezone", tz.isNotBlank())
        assertTrue("Timezone should look like Area/City format", tz.contains("/"))
        // Common timezone examples: Asia/Riyadh, Europe/London, America/New_York
        assertTrue("Timezone should contain valid area", 
            tz.contains("Asia/") || tz.contains("Europe/") || 
            tz.contains("America/") || tz.contains("Africa/") || 
            tz.contains("Australia/") || tz.contains("Pacific/"))
    }

    @Test
    fun `aladhan calendar endpoint - prayer times format validation`() = runTest {
        val api = createApi()

        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        val first = resp.body()!!.data.first()

        // All required prayer times should be present and formatted correctly
        assertTrue("Fajr should look like a time", looksLikeTime(first.timings.Fajr))
        assertTrue("Sunrise should look like a time", looksLikeTime(first.timings.Sunrise))
        assertTrue("Dhuhr should look like a time", looksLikeTime(first.timings.Dhuhr))
        assertTrue("Asr should look like a time", looksLikeTime(first.timings.Asr))
        assertTrue("Maghrib should look like a time", looksLikeTime(first.timings.Maghrib))
        assertTrue("Isha should look like a time", looksLikeTime(first.timings.Isha))
    }

    @Test
    fun `aladhan calendar endpoint - extended prayer times validation`() = runTest {
        val api = createApi()

        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        val first = resp.body()!!.data.first()

        // Extended times used by the app
        assertTrue("Firstthird should look like a time", looksLikeTime(first.timings.Firstthird))
        assertTrue("Midnight should look like a time", looksLikeTime(first.timings.Midnight))
        assertTrue("Lastthird should look like a time", looksLikeTime(first.timings.Lastthird))
        assertTrue("Sunset should look like a time", looksLikeTime(first.timings.Sunset))
    }

    @Test
    fun `aladhan calendar endpoint - date format validation`() = runTest {
        val api = createApi()

        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 1,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        val first = resp.body()!!.data.first()
        val dateStr = first.date.gregorian.date
        
        assertTrue("Gregorian date should not be blank", dateStr.isNotBlank())
        assertTrue("Date should follow DD-MM-YYYY format", 
            Regex("""^\d{2}-\d{2}-\d{4}$""").matches(dateStr))
    }

    @Test
    fun `aladhan calendar endpoint - returns full month data`() = runTest {
        val api = createApi()

        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 1, // January
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        // January should have 31 days of data
        assertEquals("January should have 31 days of prayer times", 31, resp.body()!!.data.size)
    }

    @Test
    fun `aladhan calendar endpoint - different locations return appropriate timezones`() = runTest {
        val api = createApi()

        // Test multiple locations
        val locations = listOf(
            Triple(24.7136, 46.6753, "Asia/Riyadh"),      // Riyadh
            Triple(51.5074, -0.1278, "Europe/London"),    // London
            Triple(40.7128, -74.0060, "America/New_York"), // New York
            Triple(25.2048, 55.2708, "Asia/Dubai")        // Dubai
        )

        for ((lat, lng, expectedTzPrefix) in locations) {
            val resp = api.getMonthlyCalendar(
                year = 2026,
                month = 1,
                latitude = lat,
                longitude = lng,
                method = 4,
                school = 0
            )

            val tz = resp.body()!!.data.first().meta.timezone
            assertTrue("Location ($lat, $lng) should return appropriate timezone", 
                tz.startsWith(expectedTzPrefix) || tz.contains(expectedTzPrefix.substringAfter("/")))
        }
    }

    @Test
    fun `aladhan calendar endpoint - different calculation methods`() = runTest {
        val api = createApi()

        // Test different calculation methods
        // 2 = Islamic Society of North America (ISNA)
        // 3 = Muslim World League
        // 4 = Umm Al-Qura University, Makkah (default)
        // 5 = Egyptian General Authority of Survey
        val methods = listOf(2, 3, 4, 5)

        for (method in methods) {
            val resp = api.getMonthlyCalendar(
                year = 2026,
                month = 1,
                latitude = 24.7136,
                longitude = 46.6753,
                method = method,
                school = 0
            )

            assertEquals("Method $method should return 200", 200, resp.code())
            assertTrue("Method $method should return data", resp.body()?.data?.isNotEmpty() == true)
        }
    }

    @Test
    fun `aladhan calendar endpoint - different juristic schools`() = runTest {
        val api = createApi()

        // 0 = Shafi (default) - earlier Asr time
        // 1 = Hanafi - later Asr time
        val respShafi = api.getMonthlyCalendar(
            year = 2026,
            month = 6,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        val respHanafi = api.getMonthlyCalendar(
            year = 2026,
            month = 6,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 1
        )

        // Both should return valid responses
        assertEquals("Shafi should return 200", 200, respShafi.code())
        assertEquals("Hanafi should return 200", 200, respHanafi.code())

        // Hanafi Asr should be later than Shafi Asr (same day comparison)
        val shafiAsr = parseTimeToMinutes(respShafi.body()!!.data[14].timings.Asr)
        val hanafiAsr = parseTimeToMinutes(respHanafi.body()!!.data[14].timings.Asr)
        
        assertTrue("Hanafi Asr ($hanafiAsr min) should be later than Shafi Asr ($shafiAsr min)", 
            hanafiAsr > shafiAsr)
    }

    @Test
    fun `aladhan calendar endpoint - prayer times chronological order`() = runTest {
        val api = createApi()

        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 6,
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        val day = resp.body()!!.data[14] // Middle of month
        
        val fajr = parseTimeToMinutes(day.timings.Fajr)
        val sunrise = parseTimeToMinutes(day.timings.Sunrise)
        val dhuhr = parseTimeToMinutes(day.timings.Dhuhr)
        val asr = parseTimeToMinutes(day.timings.Asr)
        val maghrib = parseTimeToMinutes(day.timings.Maghrib)
        val isha = parseTimeToMinutes(day.timings.Isha)

        // Prayer times should be in chronological order
        assertTrue("Fajr should be before Sunrise", fajr < sunrise)
        assertTrue("Sunrise should be before Dhuhr", sunrise < dhuhr)
        assertTrue("Dhuhr should be before Asr", dhuhr < asr)
        assertTrue("Asr should be before Maghrib", asr < maghrib)
        assertTrue("Maghrib should be before Isha", maghrib < isha)
    }

    @Test
    fun `aladhan calendar endpoint - error response structure`() = runTest {
        val api = createApi()

        // Test with invalid parameters (should still return a response)
        val resp = api.getMonthlyCalendar(
            year = 2026,
            month = 13, // Invalid month
            latitude = 24.7136,
            longitude = 46.6753,
            method = 4,
            school = 0
        )

        // API should return a non-200 code or empty data for invalid month
        assertNotNull("Response should not be null", resp)
    }

    @Test
    fun `aladhan calendar endpoint - extreme latitudes handling`() = runTest {
        val api = createApi()

        // Test extreme latitudes (near poles)
        val extremeLocations = listOf(
            Triple(70.0, 25.0, "High North"),
            Triple(-70.0, 25.0, "High South")
        )

        for ((lat, lng, description) in extremeLocations) {
            try {
                val resp = api.getMonthlyCalendar(
                    year = 2026,
                    month = 6,
                    latitude = lat,
                    longitude = lng,
                    method = 4,
                    school = 0
                )

                // API should either return valid data or a proper error
                assertNotNull("$description: Response should not be null", resp)
            } catch (e: Exception) {
                // Extreme latitudes may cause errors due to polar day/night
                // This is expected behavior
                assertTrue("$description: Expected exception type", 
                    e is retrofit2.HttpException || e is java.io.IOException)
            }
        }
    }

    // Helper functions
    private fun looksLikeTime(value: String): Boolean {
        val trimmed = value.trim()
        // Accept "05:12" or "05:12 (+03)" etc.
        val hhmm = trimmed.take(5)
        return Regex("""^\d{2}:\d{2}$""").matches(hhmm)
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val trimmed = timeStr.trim().take(5)
        val parts = trimmed.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
