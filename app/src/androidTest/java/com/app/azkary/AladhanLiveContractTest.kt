package com.app.azkary

import com.app.azkary.data.network.AladhanApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import retrofit2.Retrofit

class AladhanLiveContractTest {

    @Test
    fun `aladhan calendar endpoint contract - real network`() = runTest {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(AladhanApiService::class.java)

        // Pick a stable, past month to avoid "today" boundary weirdness
        val year = 2026
        val month = 1

        // Riyadh-ish coordinates (your earlier sample)
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
        Assert.assertEquals("Expected Aladhan wrapper code=200", 200, resp.code)
        Assert.assertTrue("Expected non-empty data array", resp.data.isNotEmpty())

        val first = resp.data.first()
        // Required fields exist
        Assert.assertNotNull(first.timings)
        Assert.assertNotNull(first.meta)
        Assert.assertNotNull(first.date)

        // Timezone should be a non-empty IANA-like string
        val tz = first.meta.timezone
        Assert.assertTrue("Expected non-empty timezone", tz.isNotBlank())
        Assert.assertTrue("Timezone should look like Area/City", tz.contains("/"))

        // Timings should be present and look time-like (HH:mm possibly with suffix)
        fun looksLikeTime(value: String): Boolean {
            val trimmed = value.trim()
            // Accept "05:12" or "05:12 (+03)" etc.
            val hhmm = trimmed.take(5)
            return Regex("""^\d{2}:\d{2}$""").matches(hhmm)
        }

        Assert.assertTrue("Fajr should look like a time", looksLikeTime(first.timings.Fajr))
        Assert.assertTrue("Dhuhr should look like a time", looksLikeTime(first.timings.Dhuhr))
        Assert.assertTrue("Asr should look like a time", looksLikeTime(first.timings.Asr))
        Assert.assertTrue("Maghrib should look like a time", looksLikeTime(first.timings.Maghrib))
        Assert.assertTrue("Isha should look like a time", looksLikeTime(first.timings.Isha))

        // Date should be parseable-ish; don't require exact format unless your DTO enforces it
        val dateStr = first.date.gregorian.date
        Assert.assertTrue("Gregorian date should not be blank", dateStr.isNotBlank())
    }
}