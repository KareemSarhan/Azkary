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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

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
                            "Sunset": "18:00",
                            "Maghrib": "18:00",
                            "Isha": "19:15",
                            "Firstthird": "22:00",
                            "Midnight": "23:30",
                            "Lastthird": "01:00"
                          },
                          "date": { "gregorian": { "date":"01-01-2026","day":"01","month":{"number":1,"en":"January"},"year":"2026" } },
                          "meta": { "timezone":"Asia/Riyadh" }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val resp = repo.fetchMonthlyPrayerTimes(2026, 1, 24.7136, 46.6753)
        assertEquals(200, resp.code)
        assertEquals(1, resp.data.size)
        assertEquals("05:00", resp.data.first().timings.Fajr)
        assertEquals("Asia/Riyadh", resp.data.first().meta.timezone)
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
            throw AssertionError("Expected RateLimitException")
        } catch (e: RateLimitException) {
            assertEquals(120, e.retryAfterSeconds)
        }
    }
}