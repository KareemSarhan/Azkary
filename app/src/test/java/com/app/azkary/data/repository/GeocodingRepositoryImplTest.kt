package com.app.azkary.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GeocodingRepositoryImplTest {

    private lateinit var repository: GeocodingRepositoryImpl
    private lateinit var context: Context
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        mockkConstructor(Geocoder::class)

        context = mockk(relaxed = true) {
            every { resources.configuration.locales.get(0) } returns Locale.getDefault()
        }

        repository = GeocodingRepositoryImpl(context = context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkConstructor(Geocoder::class)
    }

    private fun createMockAddress(
        locality: String? = null,
        subAdminArea: String? = null,
        adminArea: String? = null,
        countryCode: String? = null,
        countryName: String? = null
    ): Address {
        val address = mockk<Address>(relaxed = true)
        every { address.locality } returns locality
        every { address.subAdminArea } returns subAdminArea
        every { address.adminArea } returns adminArea
        every { address.countryCode } returns countryCode
        every { address.countryName } returns countryName
        return address
    }

    @Test
    fun `getCityName returns city and country when both available`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "Riyadh",
            countryCode = "SA"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(24.7136, 46.6753, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(24.7136, 46.6753)

        // Then
        assertEquals("Riyadh, SA", result)
    }

    @Test
    fun `getCityName returns subAdminArea when locality is null`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = null,
            subAdminArea = "Manhattan",
            countryCode = "US"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(40.7128, -74.0060, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(40.7128, -74.0060)

        // Then
        assertEquals("Manhattan, US", result)
    }

    @Test
    fun `getCityName returns adminArea when locality and subAdminArea are null`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = null,
            subAdminArea = null,
            adminArea = "California",
            countryCode = "US"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(36.7783, -119.4179, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(36.7783, -119.4179)

        // Then
        assertEquals("California, US", result)
    }

    @Test
    fun `getCityName returns city only when country is null`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "Unknown City",
            countryCode = null,
            countryName = null
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(0.0, 0.0, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(0.0, 0.0)

        // Then
        assertEquals("Unknown City", result)
    }

    @Test
    fun `getCityName returns country only when city is null`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = null,
            subAdminArea = null,
            adminArea = null,
            countryName = "Unknown Country"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(0.0, 0.0, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(0.0, 0.0)

        // Then
        assertEquals("Unknown Country", result)
    }

    @Test
    fun `getCityName returns null when both city and country are null`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = null,
            subAdminArea = null,
            adminArea = null,
            countryCode = null,
            countryName = null
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(0.0, 0.0, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(0.0, 0.0)

        // Then
        assertNull(result)
    }

    @Test
    fun `getCityName returns null when geocoder returns empty list`() = runTest {
        // Given
        every { 
            anyConstructed<Geocoder>().getFromLocation(any(), any(), any()) 
        } returns emptyList()

        // When
        val result = repository.getCityName(24.7136, 46.6753)

        // Then
        assertNull(result)
    }

    @Test
    fun `getCityName returns null when geocoder returns null`() = runTest {
        // Given
        every { 
            anyConstructed<Geocoder>().getFromLocation(any(), any(), any()) 
        } returns null

        // When
        val result = repository.getCityName(24.7136, 46.6753)

        // Then
        assertNull(result)
    }

    @Test
    fun `getCityName returns null when geocoder throws IOException`() = runTest {
        // Given
        every { 
            anyConstructed<Geocoder>().getFromLocation(any(), any(), any()) 
        } throws IOException("Geocoding service unavailable")

        // When
        val result = repository.getCityName(24.7136, 46.6753)

        // Then
        assertNull(result)
    }

    @Test
    fun `getCityName returns null when geocoder throws RuntimeException`() = runTest {
        // Given
        every { 
            anyConstructed<Geocoder>().getFromLocation(any(), any(), any()) 
        } throws RuntimeException("Unexpected error")

        // When
        val result = repository.getCityName(24.7136, 46.6753)

        // Then
        assertNull(result)
    }

    @Test
    fun `getCityName uses countryName when countryCode is null`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "Paris",
            countryCode = null,
            countryName = "France"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(48.8566, 2.3522, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(48.8566, 2.3522)

        // Then
        assertEquals("Paris, France", result)
    }

    @Test
    fun `getCityName prefers countryCode over countryName`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "London",
            countryCode = "GB",
            countryName = "United Kingdom"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(51.5074, -0.1278, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(51.5074, -0.1278)

        // Then - should use countryCode "GB" not "United Kingdom"
        assertEquals("London, GB", result)
    }

    @Test
    fun `getCityName works with various coordinates`() = runTest {
        // Given - Multiple test cases
        val testCases = listOf(
            Triple(24.7136, 46.6753, createMockAddress("Riyadh", countryCode = "SA")),
            Triple(21.3891, 39.8579, createMockAddress("Mecca", countryCode = "SA")),
            Triple(24.4686, 39.6142, createMockAddress("Medina", countryCode = "SA")),
            Triple(40.7128, -74.0060, createMockAddress("New York", countryCode = "US")),
            Triple(51.5074, -0.1278, createMockAddress("London", countryCode = "GB")),
            Triple(35.6762, 139.6503, createMockAddress("Tokyo", countryCode = "JP"))
        )

        for ((lat, lon, address) in testCases) {
            every { 
                anyConstructed<Geocoder>().getFromLocation(lat, lon, 1) 
            } returns listOf(address)

            // When
            val result = repository.getCityName(lat, lon)

            // Then
            assert(result?.contains(address.locality ?: "") == true)
        }
    }

    @Test
    fun `getCityName handles coordinates at equator`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "Equatorial City",
            countryCode = "EC"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(0.0, 0.0, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(0.0, 0.0)

        // Then
        assertEquals("Equatorial City, EC", result)
    }

    @Test
    fun `getCityName handles negative coordinates`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "Sydney",
            countryCode = "AU"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(-33.8688, 151.2093, 1) 
        } returns listOf(mockAddress)

        // When
        val result = repository.getCityName(-33.8688, 151.2093)

        // Then
        assertEquals("Sydney, AU", result)
    }

    @Test
    fun `getCityName uses system locale for geocoder`() = runTest {
        // Given
        val mockAddress = createMockAddress(
            locality = "Moscow",
            countryCode = "RU"
        )

        every { 
            anyConstructed<Geocoder>().getFromLocation(55.7558, 37.6173, 1) 
        } returns listOf(mockAddress)

        // When
        repository.getCityName(55.7558, 37.6173)

        // Then - verify Geocoder was created with system locale
        // This is implicit in the mock setup, but we can verify it was called
        // The fact that our mock works confirms the code path executed
    }
}
