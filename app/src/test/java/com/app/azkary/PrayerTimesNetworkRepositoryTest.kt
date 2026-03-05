package com.app.azkary

import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.data.network.exception.RateLimitException
import com.app.azkary.data.repository.PrayerTimesNetworkRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Unit tests for PrayerTimesNetworkRepository
 * Tests API interactions, error handling, and response parsing.
 */
class PrayerTimesNetworkRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: PrayerTimesNetworkRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(AladhanApiService::class.java)
        repo = PrayerTimesNetworkRepositoryImpl(api)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `fetchMonthlyPrayerTimes parses success response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validSingleDayResponse())
        )

        val resp = repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        assertEquals(200, resp.code)
        assertEquals(1, resp.data.size)
        assertEquals("05:00", resp.data.first().timings.Fajr)
        assertEquals("Asia/Riyadh", resp.data.first().meta.timezone)
    }

    @Test
    fun `fetchMonthlyPrayerTimes parses full month response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(valid31DayResponse())
        )

        val resp = repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        assertEquals(200, resp.code)
        assertEquals(31, resp.data.size)
    }

    @Test
    fun `fetchMonthlyPrayerTimes passes correct parameters`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validSingleDayResponse())
        )

        repo.fetchMonthlyPrayerTimes(2026, 6, 24.7136, 46.6753, method = 2, school = 1)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("/calendar/2026/6"))
        assertTrue(request.path!!.contains("latitude=24.7136"))
        assertTrue(request.path!!.contains("longitude=46.6753"))
        assertTrue(request.path!!.contains("method=2"))
        assertTrue(request.path!!.contains("school=1"))
    }

    @Test
    fun `fetchMonthlyPrayerTimes uses default method and school`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validSingleDayResponse())
        )

        repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("method=4"))
        assertTrue(request.path!!.contains("school=0"))
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 500 to ApiException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 429 to RateLimitException with retry-after`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "120")
        )

        try {
            repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            fail("Expected RateLimitException")
        } catch (e: RateLimitException) {
            assertEquals(120, e.retryAfterSeconds)
            assertEquals(429, e.httpCode)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 400 to ApiException with httpCode`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"code": 400, "status": "Bad Request"}""")
        )

        try {
            repo.fetchMonthlyPrayerTimes(2026, 13, 24.7136, 46.6753)
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(400, e.httpCode)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 401 to ApiException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        
        try {
            repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(401, e.httpCode)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 403 to ApiException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        
        try {
            repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.httpCode)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 503 to ApiException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        
        try {
            repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(503, e.httpCode)
        }
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes handles malformed JSON`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"code": 200, "status": "OK", "data": [invalid}""")
        )
        repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes handles empty response body`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes handles network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))
        repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test
    fun `fetchMonthlyPrayerTimes parses all prayer times correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validSingleDayResponse())
        )

        val resp = repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        val timings = resp.data.first().timings

        assertEquals("05:00", timings.Fajr)
        assertEquals("06:10", timings.Sunrise)
        assertEquals("12:05", timings.Dhuhr)
        assertEquals("15:25", timings.Asr)
        assertEquals("18:00", timings.Maghrib)
        assertEquals("19:15", timings.Isha)
        assertEquals("18:02", timings.Sunset)
        assertEquals("00:45", timings.Firstthird)
        assertEquals("01:30", timings.Midnight)
        assertEquals("02:15", timings.Lastthird)
    }

    @Test
    fun `fetchMonthlyPrayerTimes handles response with timezone offset in times`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseWithTimezoneOffset())
        )

        val resp = repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        assertEquals("05:00 (+03)", resp.data.first().timings.Fajr)
    }

    // Helper methods
    private fun validSingleDayResponse(): String {
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
                "Lastthird": "02:15"
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

    private fun valid31DayResponse(): String {
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
}
