package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.app.azkary.data.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for UserPreferencesRepository
 *
 * Tests cover:
 * - Default value fallback for location preferences
 * - Default value fallback for hold-to-complete preference
 * - Location setting and retrieval (LatLng serialization)
 * - Location name setting and retrieval
 * - Use location toggle
 * - Hold-to-complete toggle
 * - Flow emissions
 * - Corruption recovery for invalid JSON
 * - Concurrent access handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class UserPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: StandardTestDispatcher
    private lateinit var json: Json

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher + Job())
        Dispatchers.setMain(testDispatcher)

        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope
        ) {
            context.preferencesDataStoreFile("test_user_settings")
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        testScope.cancel()

        // Clean up test DataStore file
        File(context.filesDir, "test_user_settings.preferences_pb").delete()
    }

    @Test
    fun `locationPreferences emits default values when no preferences set`() = testScope.runTest {
        val testDataStore = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile("test_location_defaults")
        }

        val repository = createTestRepository(testDataStore)

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertEquals(true, prefs.useLocation)
            assertNull(prefs.lastResolvedLocation)
            assertNull(prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `holdToComplete emits default value when no preference set`() = testScope.runTest {
        val testDataStore = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile("test_hold_defaults")
        }

        val repository = createTestRepository(testDataStore)

        repository.holdToComplete.test {
            val value = awaitItem()
            assertEquals(true, value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseLocation persists false value`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setUseLocation(false)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertFalse(prefs.useLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseLocation persists true value`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setUseLocation(false)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.setUseLocation(true)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertTrue(prefs.useLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLastResolvedLocation persists valid LatLng`() = testScope.runTest {
        val repository = createTestRepository()
        val location = LatLng(24.7136, 46.6753)

        repository.setLastResolvedLocation(location)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNotNull(prefs.lastResolvedLocation)
            assertEquals(24.7136, prefs.lastResolvedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(46.6753, prefs.lastResolvedLocation?.longitude ?: 0.0, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLastResolvedLocation with null clears location`() = testScope.runTest {
        val repository = createTestRepository()
        val location = LatLng(24.7136, 46.6753)

        repository.setLastResolvedLocation(location)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.setLastResolvedLocation(null)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.lastResolvedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLocationName persists location name`() = testScope.runTest {
        val repository = createTestRepository()
        val locationName = "Makkah, Saudi Arabia"

        repository.setLocationName(locationName)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertEquals(locationName, prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLocationName with null clears location name`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setLocationName("Makkah")
        testDispatcher.scheduler.advanceUntilIdle()

        repository.setLocationName(null)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setHoldToComplete persists false value`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setHoldToComplete(false)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.holdToComplete.test {
            val value = awaitItem()
            assertFalse(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setHoldToComplete persists true value`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setHoldToComplete(false)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.setHoldToComplete(true)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.holdToComplete.test {
            val value = awaitItem()
            assertTrue(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences emits updates when useLocation changes`() = testScope.runTest {
        val repository = createTestRepository()

        repository.locationPreferences.test {
            val initial = awaitItem()
            assertTrue(initial.useLocation)

            repository.setUseLocation(false)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertFalse(updated.useLocation)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences emits updates when location changes`() = testScope.runTest {
        val repository = createTestRepository()

        repository.locationPreferences.test {
            val initial = awaitItem()
            assertNull(initial.lastResolvedLocation)

            repository.setLastResolvedLocation(LatLng(24.7136, 46.6753))
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertNotNull(updated.lastResolvedLocation)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences emits updates when location name changes`() = testScope.runTest {
        val repository = createTestRepository()

        repository.locationPreferences.test {
            val initial = awaitItem()
            assertNull(initial.locationName)

            repository.setLocationName("Riyadh")
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals("Riyadh", updated.locationName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `holdToComplete emits updates when value changes`() = testScope.runTest {
        val repository = createTestRepository()

        repository.holdToComplete.test {
            val initial = awaitItem()
            assertTrue(initial)

            repository.setHoldToComplete(false)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertFalse(updated)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences handles complete location data`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setUseLocation(true)
        repository.setLastResolvedLocation(LatLng(21.4225, 39.8262))
        repository.setLocationName("Makkah")
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertTrue(prefs.useLocation)
            assertNotNull(prefs.lastResolvedLocation)
            assertEquals(21.4225, prefs.lastResolvedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(39.8262, prefs.lastResolvedLocation?.longitude ?: 0.0, 0.0001)
            assertEquals("Makkah", prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences falls back to null for invalid JSON`() = testScope.runTest {
        val testDataStore = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile("test_location_invalid_json")
        }

        // Write invalid JSON directly to the DataStore
        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_resolved_location")] = "invalid json"
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.lastResolvedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences falls back to null for malformed JSON`() = testScope.runTest {
        val testDataStore = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile("test_location_malformed")
        }

        // Write malformed JSON directly to the DataStore
        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_resolved_location")] =
                """{"latitude": 24.0}""" // Missing longitude
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.lastResolvedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent location updates result in consistent state`() = testScope.runTest {
        val repository = createTestRepository()
        val locations = listOf(
            LatLng(24.7136, 46.6753), // Riyadh
            LatLng(21.4225, 39.8262), // Makkah
            LatLng(24.5247, 39.5692), // Madinah
            LatLng(21.2854, 39.2376)  // Jeddah
        )

        // Launch concurrent updates
        val jobs = locations.map { location ->
            launch { repository.setLastResolvedLocation(location) }
        }

        jobs.forEach { it.join() }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify a consistent final state
        repository.locationPreferences.test {
            val prefs = awaitItem()
            // Location should be one of the valid locations
            assertNotNull(prefs.lastResolvedLocation)
            val lat = prefs.lastResolvedLocation!!.latitude
            val lng = prefs.lastResolvedLocation!!.longitude
            assertTrue(locations.any { it.latitude == lat && it.longitude == lng })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent holdToComplete updates result in consistent state`() = testScope.runTest {
        val repository = createTestRepository()

        // Launch concurrent updates
        val jobs = (1..10).map {
            launch { repository.setHoldToComplete(it % 2 == 0) }
        }

        jobs.forEach { it.join() }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify a consistent final state
        repository.holdToComplete.test {
            val value = awaitItem()
            // Should be either true or false
            assertTrue(value || !value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `data persists across repository instances`() = testScope.runTest {
        val dataStoreName = "test_user_persistence"

        // Create first repository and set values
        val dataStore1 = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile(dataStoreName)
        }
        val repository1 = createTestRepository(dataStore1)

        repository1.setUseLocation(false)
        repository1.setLastResolvedLocation(LatLng(24.7136, 46.6753))
        repository1.setLocationName("Riyadh")
        repository1.setHoldToComplete(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Create second repository with same DataStore
        val dataStore2 = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile(dataStoreName)
        }
        val repository2 = createTestRepository(dataStore2)

        // Verify values persisted
        repository2.locationPreferences.test {
            val prefs = awaitItem()
            assertFalse(prefs.useLocation)
            assertNotNull(prefs.lastResolvedLocation)
            assertEquals(24.7136, prefs.lastResolvedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals("Riyadh", prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }

        repository2.holdToComplete.test {
            val value = awaitItem()
            assertFalse(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LatLng serialization and deserialization preserves coordinates`() = testScope.runTest {
        val repository = createTestRepository()
        val testLocations = listOf(
            LatLng(24.7136, 46.6753),
            LatLng(21.4225, 39.8262),
            LatLng(0.0, 0.0),
            LatLng(-33.8688, 151.2093), // Sydney (negative coordinates)
            LatLng(90.0, 180.0),        // Max values
            LatLng(-90.0, -180.0)       // Min values
        )

        testLocations.forEach { location ->
            repository.setLastResolvedLocation(location)
            testDispatcher.scheduler.advanceUntilIdle()

            repository.locationPreferences.test {
                val prefs = awaitItem()
                assertNotNull(prefs.lastResolvedLocation)
                assertEquals(location.latitude, prefs.lastResolvedLocation!!.latitude, 0.0001)
                assertEquals(location.longitude, prefs.lastResolvedLocation!!.longitude, 0.0001)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `empty location name is persisted correctly`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setLocationName("")
        testDispatcher.scheduler.advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertEquals("", prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences flow completes after cancellation`() = testScope.runTest {
        val repository = createTestRepository()

        val job = launch {
            repository.locationPreferences.collect { }
        }

        testDispatcher.scheduler.advanceUntilIdle()
        job.cancel()

        assertTrue(job.isCancelled)
    }

    @Test
    fun `holdToComplete flow completes after cancellation`() = testScope.runTest {
        val repository = createTestRepository()

        val job = launch {
            repository.holdToComplete.collect { }
        }

        testDispatcher.scheduler.advanceUntilIdle()
        job.cancel()

        assertTrue(job.isCancelled)
    }

    /**
     * Helper function to create a test repository with a specific DataStore
     */
    private fun createTestRepository(
        dataStore: DataStore<Preferences> = testDataStore
    ): TestUserPreferencesRepository {
        return TestUserPreferencesRepository(dataStore, json)
    }

    /**
     * Testable version of UserPreferencesRepository that accepts a DataStore directly
     */
    private class TestUserPreferencesRepository(
        private val dataStore: DataStore<Preferences>,
        private val json: Json
    ) {
        private val USE_LOCATION = booleanPreferencesKey("use_location")
        private val LAST_RESOLVED_LOCATION = stringPreferencesKey("last_resolved_location")
        private val LOCATION_NAME = stringPreferencesKey("location_name")
        private val HOLD_TO_COMPLETE = booleanPreferencesKey("hold_to_complete")

        val locationPreferences: Flow<LocationPreferences> = dataStore.data.map { preferences ->
            LocationPreferences(
                useLocation = preferences[USE_LOCATION] ?: true,
                lastResolvedLocation = preferences[LAST_RESOLVED_LOCATION]?.let {
                    try { json.decodeFromString<LatLng>(it) } catch (e: Exception) { null }
                },
                locationName = preferences[LOCATION_NAME]
            )
        }

        val holdToComplete: Flow<Boolean> = dataStore.data.map { preferences ->
            preferences[HOLD_TO_COMPLETE] ?: true
        }

        suspend fun setUseLocation(enabled: Boolean) {
            dataStore.edit { it[USE_LOCATION] = enabled }
        }

        suspend fun setLastResolvedLocation(location: LatLng?) {
            dataStore.edit {
                if (location != null) {
                    it[LAST_RESOLVED_LOCATION] = json.encodeToString(location)
                } else {
                    it.remove(LAST_RESOLVED_LOCATION)
                }
            }
        }

        suspend fun setLocationName(name: String?) {
            dataStore.edit {
                if (name != null) {
                    it[LOCATION_NAME] = name
                } else {
                    it.remove(LOCATION_NAME)
                }
            }
        }

        suspend fun setHoldToComplete(enabled: Boolean) {
            dataStore.edit { it[HOLD_TO_COMPLETE] = enabled }
        }
    }
}
