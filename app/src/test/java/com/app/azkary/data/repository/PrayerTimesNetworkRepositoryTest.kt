package com.app.azkary.data.repository

import com.app.azkary.data.network.AladhanApiService
import com.app.azkary.data.network.dto.DateDto
import com.app.azkary.data.network.dto.GregorianDateDto
import com.app.azkary.data.network.dto.MetaDto
import com.app.azkary.data.network.dto.MonthDto
import com.app.azkary.data.network.dto.PrayerCalendarResponse
import com.app.azkary.data.network.dto.PrayerDayDto
import com.app.azkary.data.network.dto.PrayerTimingsDto
import com.app.azkary.data.network.exception.ApiException
import com.app.azkary.data.network.exception.RateLimitException
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerTimesNetworkRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var apiService: AladhanApiService
    private lateinit var repository: PrayerTimesNetworkRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
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

        apiService = retrofit.create(AladhanApiService::class.java)
        repository = PrayerTimesNetworkRepositoryImpl(apiService)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    @Test
    fun `fetchMonthlyPrayerTimes parses success response with full data`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
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
                            "Sunset": "18:00",
                            "Firstthird": "23:00",
                            "Midnight": "00:00",
                            "Lastthird": "01:00"
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
                        },
                        {
                          "timings": {
                            "Fajr": "05:01",
                            "Sunrise": "06:11",
                            "Dhuhr": "12:06",
                            "Asr": "15:26",
                            "Maghrib": "18:01",
                            "Isha": "19:16",
                            "Sunset": "18:01",
                            "Firstthird": "23:01",
                            "Midnight": "00:01",
                            "Lastthird": "01:01"
                          },
                          "date": { 
                            "gregorian": { 
                              "date":"02-01-2026",
                              "day":"02",
                              "month":{"number":1,"en":"January"},
                              "year":"2026"
                            } 
                          },
                          "meta": { "timezone":"Asia/Riyadh" }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        // When
        val resp = repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)

        // Then
        assertEquals(200, resp.code)
        assertEquals("OK", resp.status)
        assertEquals(2, resp.data.size)
        assertEquals("05:00", resp.data[0].timings.Fajr)
        assertEquals("Asia/Riyadh", resp.data[0].meta.timezone)
        assertEquals("05:01", resp.data[1].timings.Fajr)
    }

    @Test
    fun `fetchMonthlyPrayerTimes uses default method and school values`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "code": 200,
                      "status": "OK",
                      "data": []
                    }
                """.trimIndent())
        )

        // When - call without specifying method and school
        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)

        // Then - verify the request URL includes default values
        val request = server.takeRequest()
        assertTrue(request.path?.contains("method=4") == true) // Default is 4 (Umm Al-Qura)
        assertTrue(request.path?.contains("school=0") == true) // Default is 0 (Shafi)
    }

    @Test
    fun `fetchMonthlyPrayerTimes uses custom method and school values`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "code": 200,
                      "status": "OK",
                      "data": []
                    }
                """.trimIndent())
        )

        // When - call with custom values
        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753, method = 2, school = 1)

        // Then
        val request = server.takeRequest()
        assertTrue(request.path?.contains("method=2") == true)
        assertTrue(request.path?.contains("school=1") == true)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 500 to ApiException`() = runTest {
        // Given
        server.enqueue(MockResponse().setResponseCode(500))

        // When - should throw
        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 400 to ApiException with client error`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error": "Bad Request"}""")
        )

        // When - should throw
        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        } catch (e: ApiException) {
            // Then
            assertTrue(e.message?.contains("Client error") == true)
            assertEquals(400, e.httpCode)
            throw e
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 429 to RateLimitException with retry-after`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "120")
        )

        // When/Then
        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            assertTrue("Expected RateLimitException", false)
        } catch (e: RateLimitException) {
            assertEquals(120, e.retryAfterSeconds)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes maps 429 to RateLimitException without retry-after`() = runTest {
        // Given
        server.enqueue(MockResponse().setResponseCode(429))

        // When/Then
        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            assertTrue("Expected RateLimitException", false)
        } catch (e: RateLimitException) {
            assertEquals(null, e.retryAfterSeconds)
        }
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes throws ApiException on empty body`() = runTest {
        // Given - successful response but empty body
        server.enqueue(MockResponse().setResponseCode(200))

        // When
        repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
    }

    @Test
    fun `fetchMonthlyPrayerTimes handles network error with message`() = runTest {
        // Given - Simulate by using mocked API service
        val mockApi = mockk<AladhanApiService>()
        val repoWithMock = PrayerTimesNetworkRepositoryImpl(mockApi)

        coEvery { 
            mockApi.getMonthlyCalendar(any(), any(), any(), any(), any(), any()) 
        } throws java.io.IOException("Network unavailable")

        // When/Then
        try {
            repoWithMock.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            assertTrue("Expected ApiException", false)
        } catch (e: ApiException) {
            assertTrue(e.message?.contains("Network error") == true)
            assertTrue(e.cause is java.io.IOException)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes preserves original ApiException`() = runTest {
        // Given - Simulate by using mocked API service
        val mockApi = mockk<AladhanApiService>()
        val repoWithMock = PrayerTimesNetworkRepositoryImpl(mockApi)
        val originalException = ApiException("Original error")

        coEvery { 
            mockApi.getMonthlyCalendar(any(), any(), any(), any(), any(), any()) 
        } throws originalException

        // When/Then
        try {
            repoWithMock.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            assertTrue("Expected ApiException", false)
        } catch (e: ApiException) {
            assertEquals("Original error", e.message)
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes preserves original RateLimitException`() = runTest {
        // Given - Simulate by using mocked API service
        val mockApi = mockk<AladhanApiService>()
        val repoWithMock = PrayerTimesNetworkRepositoryImpl(mockApi)
        val originalException = RateLimitException("Rate limited", 300)

        coEvery { 
            mockApi.getMonthlyCalendar(any(), any(), any(), any(), any(), any()) 
        } throws originalException

        // When/Then
        try {
            repoWithMock.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
            assertTrue("Expected RateLimitException", false)
        } catch (e: RateLimitException) {
            assertEquals(300, e.retryAfterSeconds)
        }
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps 502 to ApiException with server error`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setBody("Bad Gateway")
        )

        // When/Then
        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        } catch (e: ApiException) {
            assertTrue(e.message?.contains("Server error") == true)
            assertEquals(502, e.httpCode)
            throw e
        }
    }

    @Test(expected = ApiException::class)
    fun `fetchMonthlyPrayerTimes maps unexpected error code to ApiException`() = runTest {
        // Given - use a redirect status code (301) to test unexpected error path
        server.enqueue(
            MockResponse()
                .setResponseCode(301)
                .setBody("Moved Permanently")
        )

        // When/Then
        try {
            repository.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        } catch (e: ApiException) {
            assertTrue(e.message?.contains("Unexpected HTTP error") == true)
            assertEquals(301, e.httpCode)
            throw e
        }
    }

    @Test
    fun `fetchMonthlyPrayerTimes handles response with different timezone`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "code": 200,
                      "status": "OK",
                      "data": [
                        {
                          "timings": {
                            "Fajr": "04:30",
                            "Sunrise": "06:00",
                            "Dhuhr": "12:15",
                            "Asr": "15:45",
                            "Maghrib": "18:30",
                            "Isha": "19:45",
                            "Sunset": "18:30",
                            "Firstthird": "22:30",
                            "Midnight": "00:15",
                            "Lastthird": "02:00"
                          },
                          "date": { 
                            "gregorian": { 
                              "date":"15-06-2026",
                              "day":"15",
                              "month":{"number":6,"en":"June"},
                              "year":"2026"
                            } 
                          },
                          "meta": { "timezone":"Europe/London" }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        // When
        val resp = repository.fetchMonthlyPrayerTimes(2026, 6, 51.5074, -0.1278)

        // Then
        assertEquals("Europe/London", resp.data[0].meta.timezone)
        assertEquals("04:30", resp.data[0].timings.Fajr)
    }

    @Test
    fun `fetchMonthlyPrayerTimes correctly parses coordinates with negative values`() = runTest {
        // Given
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "code": 200,
                      "status": "OK",
                      "data": []
                    }
                """.trimIndent())
        )

        // When - New York coordinates (negative longitude)
        repository.fetchMonthlyPrayerTimes(2026, 1, 40.7128, -74.0060)

        // Then - verify the request URL includes correct coordinates
        val request = server.takeRequest()
        assertTrue(request.path?.contains("latitude=40.7128") == true)
        assertTrue(request.path?.contains("longitude=-74.006") == true)
    }
}
