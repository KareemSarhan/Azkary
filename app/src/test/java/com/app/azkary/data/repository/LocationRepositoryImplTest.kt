package com.app.azkary.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationRepositoryImplTest {

    private lateinit var repository: LocationRepositoryImpl
    private lateinit var context: Context
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        val application = RuntimeEnvironment.getApplication()
        context = application
        shadowApp = Shadows.shadowOf(application)
        fusedLocationClient = mockk(relaxed = true)

        repository = LocationRepositoryImpl(
            context = context,
            fusedLocationClient = fusedLocationClient
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun grantPermissions() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun grantFineOnly() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun grantCoarseOnly() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun denyAllPermissions() {
        // In Robolectric, permissions are denied by default unless explicitly granted
        // We need to revoke any previously granted permissions
    }

    private fun createMockLocation(latitude: Double, longitude: Double): Location {
        val location = mockk<Location>()
        every { location.latitude } returns latitude
        every { location.longitude } returns longitude
        return location
    }

    private fun <T> createMockTask(result: T?): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<T>>()) } answers {
            val listener = arg<OnSuccessListener<T>>(0)
            listener.onSuccess(result)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun <T> createMockTaskWithFailure(): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<T>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            val listener = arg<OnFailureListener>(0)
            listener.onFailure(Exception("Location failure"))
            task
        }
        return task
    }

    @Test
    fun `getCurrentLocation returns null when no permissions granted`() = runTest {
        // Given - no permissions
        denyAllPermissions()

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNull(result)
    }

    @Test
    fun `getCurrentLocation returns location with fine permission only`() = runTest {
        // Given - fine location permission
        grantFineOnly()

        val mockLocation = createMockLocation(24.7136, 46.6753)
        every { fusedLocationClient.lastLocation } returns createMockTask(mockLocation)

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNotNull(result)
        assertEquals(24.7136, result?.latitude ?: 0.0, 0.0001)
        assertEquals(46.6753, result?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun `getCurrentLocation returns location with coarse permission only`() = runTest {
        // Given - coarse location permission
        grantCoarseOnly()

        val mockLocation = createMockLocation(40.7128, -74.0060)
        every { fusedLocationClient.lastLocation } returns createMockTask(mockLocation)

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNotNull(result)
        assertEquals(40.7128, result?.latitude ?: 0.0, 0.0001)
        assertEquals(-74.0060, result?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun `getCurrentLocation returns location with both permissions`() = runTest {
        // Given - both permissions
        grantPermissions()

        val mockLocation = createMockLocation(51.5074, -0.1278)
        every { fusedLocationClient.lastLocation } returns createMockTask(mockLocation)

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNotNull(result)
        assertEquals(51.5074, result?.latitude ?: 0.0, 0.0001)
        assertEquals(-0.1278, result?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun `getCurrentLocation returns null when last location is null and current location fails`() = runTest {
        // Given
        grantFineOnly()

        // Last location is null
        every { fusedLocationClient.lastLocation } returns createMockTask<Location>(null)
        // Current location request fails
        val failureTask: Task<Location> = createMockTaskWithFailure()
        every { fusedLocationClient.getCurrentLocation(any<Int>(), any()) } answers { failureTask }

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNull(result)
    }

    @Test
    fun `getCurrentLocation tries current location when last location fails`() = runTest {
        // Given
        grantFineOnly()

        val mockLocation = createMockLocation(24.7136, 46.6753)

        // Last location fails
        val failureTask: Task<Location> = createMockTaskWithFailure()
        every { fusedLocationClient.lastLocation } returns failureTask
        // Current location succeeds
        val successTask: Task<Location> = createMockTask(mockLocation)
        every { fusedLocationClient.getCurrentLocation(any<Int>(), any()) } answers { successTask }

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNotNull(result)
        assertEquals(24.7136, result?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun `getCurrentLocation returns null when both location attempts fail`() = runTest {
        // Given
        grantFineOnly()

        // Both fail
        val failureTask: Task<Location> = createMockTaskWithFailure()
        every { fusedLocationClient.lastLocation } returns failureTask
        every { fusedLocationClient.getCurrentLocation(any<Int>(), any()) } answers { failureTask }

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNull(result)
    }

    @Test
    fun `getCurrentLocation handles exception gracefully`() = runTest {
        // Given
        grantFineOnly()
        every { fusedLocationClient.lastLocation } throws SecurityException("Test exception")

        // When
        val result = repository.getCurrentLocation()

        // Then - should catch exception and return null
        assertNull(result)
    }

    @Test
    fun `getCurrentLocation uses balanced power accuracy priority`() = runTest {
        // Given
        grantFineOnly()

        every { fusedLocationClient.lastLocation } returns createMockTask<Location>(null)

        val mockLocation = createMockLocation(24.7136, 46.6753)
        every { 
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ) 
        } returns createMockTask(mockLocation)

        // When
        val result = repository.getCurrentLocation()

        // Then
        assertNotNull(result)
    }
}
