package com.app.azkary

import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.data.network.exception.RateLimitException
import com.app.azkary.data.repository.PrayerTimesNetworkRepository
import com.app.azkary.data.repository.PrayerTimesNetworkRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Comprehensive MockWebServer tests for Aladhan API
 * Tests various scenarios including success, error responses, network failures,
 * timeout handling, and malformed responses.
 */
class AladhanMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AladhanApiService
    private lateinit var repository: PrayerTimesNetworkRepository

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        api = retrofit.create(AladhanApiService::class.java)
        repository = PrayerTimesNetworkRepositoryImpl(api)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    //region Success Response Tests

    @Test
    fun `fetchMonthlyPrayerTimes parses success response correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validCalendarResponse())
        )

        val response = repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)

        assertEquals(200, response.code)
        assertEquals(31, response.data.size)
        assertEquals("05:00", response.data.first().timings.Fajr)
        assertEquals("Asia/Riyadh", response.data.first().meta.timezone)
    }

    @Test
    fun `API request includes correct query parameters`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validCalendarResponse())
        )

        repository.fetchMonthlyPrayerTimes(2026, 6, 24.7136, 46.6753, method = 4, school = 0)

        val request: RecordedRequest = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue("Request should contain year/month path", 
            request.path!!.contains("/calendar/2026/6"))
        assertTrue("Request should contain latitude", 
            request.path!!.contains("latitude=24.7136"))
        assertTrue("Request should contain longitude", 
            request.path!!.contains("longitude=46.6753"))
        assertTrue("Request should contain method", 
            request.path!!.contains("method=4"))
        assertTrue("Request should contain school", 
            request.path!!.contains("school=0"))
    }

    @Test
    fun `API handles 28-day month correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validFebruaryResponse())
        )

        val response = repository.fetchMonthlyPrayerTimes(2026, 2, 24.7136, 46.6753)

        assertEquals(28, response.data.size)
    }

    @Test
    fun `API handles 30-day month correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(valid30DayMonthResponse())
        )

        val response = repository.fetchMonthlyPrayerTimes(2026, 4, 24.7136, 46.6753)

        assertEquals(30, response.data.size)
    }

    //endregion

    //region Error Response Tests

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 500 to ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"code": 500, "status": "Internal Server Error"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 429 to RateLimitException with retry-after`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "120")
                .setBody("""{"code": 429, "status": "Too Many Requests"}""")
        )

        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            fail("Expected RateLimitException")
        } catch (e: RateLimitException) {
            assertEquals(120, e.retryAfterSeconds)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes handles 429 without retry-after header`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"code": 429, "status": "Too Many Requests"}""")
        )

        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            fail("Expected RateLimitException")
        } catch (e: RateLimitException) {
            assertNull(e.retryAfterSeconds)
        }
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 400 to ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"code": 400, "status": "Bad Request", "data": "Invalid month"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 13, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 401 to ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code": 401, "status": "Unauthorized"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 403 to ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"code": 403, "status": "Forbidden"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 404 to ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"code": 404, "status": "Not Found"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 503 to ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"code": 503, "status": "Service Unavailable"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    //endregion

    //region Network Failure Tests

    @Test(expected = ApiException::class)
    fun `handles connection timeout`() = runTest {
        // Simulate timeout by using a very short timeout client
        val shortTimeoutClient = OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofMillis(1))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(shortTimeoutClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val timeoutApi = retrofit.create(AladhanApiService::class.java)
        val timeoutRepo = PrayerTimesNetworkRepositoryImpl(timeoutApi)

        // Don't enqueue response - let it timeout
        timeoutRepo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    //endregion

    //region Malformed Response Tests

    @Test(expected = ApiException::class)
    fun `handles empty response body`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `handles malformed JSON response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"code": 200, "status": "OK", "data": [invalid}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `handles missing data field`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"code": 200, "status": "OK"}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `handles empty data array`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"code": 200, "status": "OK", "data": []}""")
        )

        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    //endregion

    //region Response Parsing Tests

    @Test
    fun `handles response with extra fields gracefully`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseWithExtraFields())
        )

        val response = repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)

        assertEquals(200, response.code)
        assertEquals(1, response.data.size)
        assertEquals("05:00", response.data.first().timings.Fajr)
    }

    @Test
    fun `handles response with timezone offset in time`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseWithTimezoneOffset())
        )

        val response = repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)

        assertEquals("05:00 (+03)", response.data.first().timings.Fajr)
    }

    //endregion

    //region Helper Methods

    private fun validCalendarResponse(): String {
        val days = (1..31).joinToString(",") { day ->
            val dayStr = day.toString().padStart(2, '0')
            """
            {
              "timings": {
                "Fajr": "05:00",
                "Sunrise": "06:10",
                "Dhuhr": "12:05",
                "Asr": "15:25",
                "Maghrib": "18:00",
                "Isha": "19:15",
                "Sunset": "18:02",
                "Firstthird": "00:45",
                "Midnight": "01:30",
                "Lastthird": "02:15"
              },
              "date": { 
                "gregorian": { 
                  "date":"$dayStr-01-2026",
                  "day":"$dayStr",
                  "month":{"number":1,"en":"January"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
            """.trimIndent()
        }

        return """
        {
          "code": 200,
          "status": "OK",
          "data": [$days]
        }
        """.trimIndent()
    }

    private fun validFebruaryResponse(): String {
        val days = (1..28).joinToString(",") { day ->
            val dayStr = day.toString().padStart(2, '0')
            """
            {
              "timings": {
                "Fajr": "05:30",
                "Sunrise": "06:35",
                "Dhuhr": "12:15",
                "Asr": "15:35",
                "Maghrib": "17:55",
                "Isha": "19:05",
                "Sunset": "17:57",
                "Firstthird": "00:45",
                "Midnight": "01:30",
                "Lastthird": "02:15"
              },
              "date": { 
                "gregorian": { 
                  "date":"$dayStr-02-2026",
                  "day":"$dayStr",
                  "month":{"number":2,"en":"February"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
            """.trimIndent()
        }

        return """
        {
          "code": 200,
          "status": "OK",
          "data": [$days]
        }
        """.trimIndent()
    }

    private fun valid30DayMonthResponse(): String {
        val days = (1..30).joinToString(",") { day ->
            val dayStr = day.toString().padStart(2, '0')
            """
            {
              "timings": {
                "Fajr": "04:45",
                "Sunrise": "05:50",
                "Dhuhr": "12:10",
                "Asr": "15:40",
                "Maghrib": "18:15",
                "Isha": "19:25",
                "Sunset": "18:17",
                "Firstthird": "00:45",
                "Midnight": "01:30",
                "Lastthird": "02:15"
              },
              "date": { 
                "gregorian": { 
                  "date":"$dayStr-04-2026",
                  "day":"$dayStr",
                  "month":{"number":4,"en":"April"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
            """.trimIndent()
        }

        return """
        {
          "code": 200,
          "status": "OK",
          "data": [$days]
        }
        """.trimIndent()
    }

    private fun responseWithExtraFields(): String {
        return """
        {
          "code": 200,
          "status": "OK",
          "data": [
            {
              "timings": {
                "Fajr": "05:00",
                "Sunrise": "06:10",
                "Dhuhr": "12:05",
                "Asr": "15:25",
                "Maghrib": "18:00",
                "Isha": "19:15",
                "Sunset": "18:02",
                "Firstthird": "00:45",
                "Midnight": "01:30",
                "Lastthird": "02:15",
                "Imsak": "04:50",
                "ExtraField": "ignored"
              },
              "date": { 
                "gregorian": { 
                  "date":"01-01-2026",
                  "day":"01",
                  "month":{"number":1,"en":"January"},
                  "year":"2026" 
                },
                "hijri": {
                  "date": "12-06-1447",
                  "format": "DD-MM-YYYY"
                }
              },
              "meta": { 
                "timezone":"Asia/Riyadh",
                "method": {"id": 4, "name": "Umm Al-Qura"}
              },
              "extraSection": {"key": "value"}
            }
          ],
          "extraRootField": "ignored"
        }
        """.trimIndent()
    }

    private fun responseWithTimezoneOffset(): String {
        return """
        {
          "code": 200,
          "status": "OK",
          "data": [
            {
              "timings": {
                "Fajr": "05:00 (+03)",
                "Sunrise": "06:10 (+03)",
                "Dhuhr": "12:05 (+03)",
                "Asr": "15:25 (+03)",
                "Maghrib": "18:00 (+03)",
                "Isha": "19:15 (+03)",
                "Sunset": "18:02 (+03)",
                "Firstthird": "00:45 (+03)",
                "Midnight": "01:30 (+03)",
                "Lastthird": "02:15 (+03)"
              },
              "date": { 
                "gregorian": { 
                  "date":"01-01-2026",
                  "day":"01",
                  "month":{"number":1,"en":"January"},
                  "year":"2026" 
                } 
              },
              "meta": { "timezone":"Asia/Riyadh" }
            }
          ]
        }
        """.trimIndent()
    }

    //endregion
}
