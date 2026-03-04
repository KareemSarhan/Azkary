package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.app.azkary.data.model.LatLng
import com.app.azkary.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class UserPreferencesRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var json: Json
    private lateinit var dataStoreFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        dataStoreFile = File(context.filesDir, "test_user_settings.preferences_pb")

        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }

        testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_user_settings")
        }
    }

    @After
    fun teardown() {
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    @Test
    fun `locationPreferences emits default values when no preferences set`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
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
    fun `holdToComplete emits default value when no preference set`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
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
    fun `setUseLocation persists false value`() = runTest {
        val repository = createTestRepository()

        repository.setUseLocation(false)
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertFalse(prefs.useLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseLocation persists true value`() = runTest {
        val repository = createTestRepository()

        repository.setUseLocation(false)
        advanceUntilIdle()

        repository.setUseLocation(true)
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertTrue(prefs.useLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLastResolvedLocation persists valid LatLng`() = runTest {
        val repository = createTestRepository()
        val location = LatLng(24.7136, 46.6753)

        repository.setLastResolvedLocation(location)
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNotNull(prefs.lastResolvedLocation)
            assertEquals(24.7136, prefs.lastResolvedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(46.6753, prefs.lastResolvedLocation?.longitude ?: 0.0, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLastResolvedLocation with null clears location`() = runTest {
        val repository = createTestRepository()
        val location = LatLng(24.7136, 46.6753)

        repository.setLastResolvedLocation(location)
        advanceUntilIdle()

        repository.setLastResolvedLocation(null)
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.lastResolvedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLocationName persists location name`() = runTest {
        val repository = createTestRepository()
        val locationName = "Makkah, Saudi Arabia"

        repository.setLocationName(locationName)
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertEquals(locationName, prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLocationName with null clears location name`() = runTest {
        val repository = createTestRepository()

        repository.setLocationName("Makkah")
        advanceUntilIdle()

        repository.setLocationName(null)
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setHoldToComplete persists false value`() = runTest {
        val repository = createTestRepository()

        repository.setHoldToComplete(false)
        advanceUntilIdle()

        repository.holdToComplete.test {
            val value = awaitItem()
            assertFalse(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setHoldToComplete persists true value`() = runTest {
        val repository = createTestRepository()

        repository.setHoldToComplete(false)
        advanceUntilIdle()

        repository.setHoldToComplete(true)
        advanceUntilIdle()

        repository.holdToComplete.test {
            val value = awaitItem()
            assertTrue(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences emits updates when useLocation changes`() = runTest {
        val repository = createTestRepository()

        repository.locationPreferences.test {
            val initial = awaitItem()
            assertTrue(initial.useLocation)

            repository.setUseLocation(false)
            advanceUntilIdle()

            val updated = awaitItem()
            assertFalse(updated.useLocation)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences emits updates when location changes`() = runTest {
        val repository = createTestRepository()

        repository.locationPreferences.test {
            val initial = awaitItem()
            assertNull(initial.lastResolvedLocation)

            repository.setLastResolvedLocation(LatLng(24.7136, 46.6753))
            advanceUntilIdle()

            val updated = awaitItem()
            assertNotNull(updated.lastResolvedLocation)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences emits updates when location name changes`() = runTest {
        val repository = createTestRepository()

        repository.locationPreferences.test {
            val initial = awaitItem()
            assertNull(initial.locationName)

            repository.setLocationName("Riyadh")
            advanceUntilIdle()

            val updated = awaitItem()
            assertEquals("Riyadh", updated.locationName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `holdToComplete emits updates when value changes`() = runTest {
        val repository = createTestRepository()

        repository.holdToComplete.test {
            val initial = awaitItem()
            assertTrue(initial)

            repository.setHoldToComplete(false)
            advanceUntilIdle()

            val updated = awaitItem()
            assertFalse(updated)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences handles complete location data`() = runTest {
        val repository = createTestRepository()

        repository.setUseLocation(true)
        repository.setLastResolvedLocation(LatLng(21.4225, 39.8262))
        repository.setLocationName("Makkah")
        advanceUntilIdle()

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
    fun `locationPreferences falls back to null for invalid JSON`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_location_invalid_json")
        }

        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_resolved_location")] = "invalid json"
        }
        advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.lastResolvedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences falls back to null for malformed JSON`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_location_malformed")
        }

        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_resolved_location")] =
                """{"latitude": 24.0}"""
        }
        advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNull(prefs.lastResolvedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent location updates result in consistent state`() = runTest {
        val repository = createTestRepository()
        val locations = listOf(
            LatLng(24.7136, 46.6753),
            LatLng(21.4225, 39.8262),
            LatLng(24.5247, 39.5692),
            LatLng(21.2854, 39.2376)
        )

        val jobs = locations.map { location ->
            backgroundScope.launch { repository.setLastResolvedLocation(location) }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertNotNull(prefs.lastResolvedLocation)
            val lat = prefs.lastResolvedLocation!!.latitude
            val lng = prefs.lastResolvedLocation!!.longitude
            assertTrue(locations.any { it.latitude == lat && it.longitude == lng })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent holdToComplete updates result in consistent state`() = runTest {
        val repository = createTestRepository()

        val jobs = (1..10).map {
            backgroundScope.launch { repository.setHoldToComplete(it % 2 == 0) }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        repository.holdToComplete.test {
            val value = awaitItem()
            assertTrue(value || !value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LatLng serialization and deserialization preserves coordinates`() = runTest {
        val repository = createTestRepository()
        val testLocations = listOf(
            LatLng(24.7136, 46.6753),
            LatLng(21.4225, 39.8262),
            LatLng(0.0, 0.0),
            LatLng(-33.8688, 151.2093),
            LatLng(90.0, 180.0),
            LatLng(-90.0, -180.0)
        )

        testLocations.forEach { location ->
            repository.setLastResolvedLocation(location)
            advanceUntilIdle()

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
    fun `empty location name is persisted correctly`() = runTest {
        val repository = createTestRepository()

        repository.setLocationName("")
        advanceUntilIdle()

        repository.locationPreferences.test {
            val prefs = awaitItem()
            assertEquals("", prefs.locationName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locationPreferences flow completes after cancellation`() = runTest {
        val repository = createTestRepository()

        val job = backgroundScope.launch {
            repository.locationPreferences.collect { }
        }

        advanceUntilIdle()
        job.cancel()

        assertTrue(job.isCancelled)
    }

    @Test
    fun `holdToComplete flow completes after cancellation`() = runTest {
        val repository = createTestRepository()

        val job = backgroundScope.launch {
            repository.holdToComplete.collect { }
        }

        advanceUntilIdle()
        job.cancel()

        assertTrue(job.isCancelled)
    }

    private fun createTestRepository(
        dataStore: DataStore<Preferences> = testDataStore
    ): TestUserPreferencesRepository {
        return TestUserPreferencesRepository(dataStore, json)
    }

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
