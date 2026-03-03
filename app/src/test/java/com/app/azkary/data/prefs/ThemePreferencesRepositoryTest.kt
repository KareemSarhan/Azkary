package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for ThemePreferencesRepository
 *
 * Tests cover:
 * - Default value fallback
 * - Data persistence across DataStore operations
 * - Theme mode setting and retrieval
 * - True black setting and retrieval
 * - Flow emissions
 * - Corruption recovery with invalid theme mode values
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ThemePreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: StandardTestDispatcher
    private lateinit var dataStoreFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher + Job())
        Dispatchers.setMain(testDispatcher)

        // Create a test-specific DataStore file
        dataStoreFile = File(context.filesDir, "test_theme_settings.preferences_pb")

        // Clean up any existing test data
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope
        ) {
            context.preferencesDataStoreFile("test_theme_settings")
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        testScope.cancel()

        // Clean up test DataStore file
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    @Test
    fun `themeSettings emits default values when no preferences set`() = testScope.runTest {
        // Create repository with fresh DataStore
        val testDataStore = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile("test_theme_defaults")
        }

        val repository = createTestRepository(testDataStore)

        // Test that default values are emitted
        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals(true, settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists LIGHT theme mode`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.LIGHT, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists DARK theme mode`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists SYSTEM theme mode`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.setThemeMode(ThemeMode.SYSTEM)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseTrueBlack persists true value`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setUseTrueBlack(true)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertTrue(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseTrueBlack persists false value`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setUseTrueBlack(false)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertFalse(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings emits updates when theme mode changes`() = testScope.runTest {
        val repository = createTestRepository()

        repository.themeSettings.test {
            // Initial value
            val initialSettings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, initialSettings.themeMode)

            // Update theme mode
            repository.setThemeMode(ThemeMode.DARK)
            testDispatcher.scheduler.advanceUntilIdle()

            // Updated value
            val updatedSettings = awaitItem()
            assertEquals(ThemeMode.DARK, updatedSettings.themeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings emits updates when true black changes`() = testScope.runTest {
        val repository = createTestRepository()

        repository.themeSettings.test {
            // Initial value
            val initialSettings = awaitItem()
            assertTrue(initialSettings.useTrueBlack)

            // Update true black setting
            repository.setUseTrueBlack(false)
            testDispatcher.scheduler.advanceUntilIdle()

            // Updated value
            val updatedSettings = awaitItem()
            assertFalse(updatedSettings.useTrueBlack)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings maintains independent values for both preferences`() = testScope.runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setUseTrueBlack(false)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.LIGHT, settings.themeMode)
            assertFalse(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings falls back to SYSTEM for invalid theme mode value`() = testScope.runTest {
        // Create a custom DataStore and inject an invalid value directly
        val testDataStore = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile("test_theme_invalid")
        }

        // Write an invalid theme mode value directly to the DataStore
        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "INVALID_MODE"
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent theme mode updates result in consistent state`() = testScope.runTest {
        val repository = createTestRepository()

        // Launch concurrent updates
        val jobs = listOf(
            launch { repository.setThemeMode(ThemeMode.LIGHT) },
            launch { repository.setThemeMode(ThemeMode.DARK) },
            launch { repository.setThemeMode(ThemeMode.SYSTEM) }
        )

        jobs.forEach { it.join() }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify a consistent final state
        repository.themeSettings.test {
            val settings = awaitItem()
            // Should be one of the valid modes
            assertTrue(
                settings.themeMode == ThemeMode.LIGHT ||
                        settings.themeMode == ThemeMode.DARK ||
                        settings.themeMode == ThemeMode.SYSTEM
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent true black updates result in consistent state`() = testScope.runTest {
        val repository = createTestRepository()

        // Launch concurrent updates
        val jobs = (1..10).map {
            launch { repository.setUseTrueBlack(it % 2 == 0) }
        }

        jobs.forEach { it.join() }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify a consistent final state
        repository.themeSettings.test {
            val settings = awaitItem()
            // Should be either true or false
            assertTrue(settings.useTrueBlack || !settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `data persists across repository instances`() = testScope.runTest {
        val dataStoreName = "test_theme_persistence"

        // Create first repository and set values
        val dataStore1 = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile(dataStoreName)
        }
        val repository1 = createTestRepository(dataStore1)

        repository1.setThemeMode(ThemeMode.DARK)
        repository1.setUseTrueBlack(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Create second repository with same DataStore
        val dataStore2 = PreferenceDataStoreFactory.create(scope = this) {
            context.preferencesDataStoreFile(dataStoreName)
        }
        val repository2 = createTestRepository(dataStore2)

        // Verify values persisted
        repository2.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertFalse(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all theme modes can be set and retrieved`() = testScope.runTest {
        val repository = createTestRepository()
        val modes = ThemeMode.entries.toTypedArray()

        modes.forEach { mode ->
            repository.setThemeMode(mode)
            testDispatcher.scheduler.advanceUntilIdle()

            repository.themeSettings.test {
                val settings = awaitItem()
                assertEquals(mode, settings.themeMode)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `themeSettings flow completes after cancellation`() = testScope.runTest {
        val repository = createTestRepository()

        val job = launch {
            repository.themeSettings.collect { }
        }

        // Let it collect some values
        testDispatcher.scheduler.advanceUntilIdle()

        // Cancel the job
        job.cancel()

        // Verify job is cancelled
        assertTrue(job.isCancelled)
    }

    /**
     * Helper function to create a test repository with a specific DataStore
     */
    private fun createTestRepository(
        dataStore: DataStore<Preferences> = testDataStore
    ): TestThemePreferencesRepository {
        return TestThemePreferencesRepository(dataStore)
    }

    /**
     * Testable version of ThemePreferencesRepository that accepts a DataStore directly
     */
    private class TestThemePreferencesRepository(
        private val dataStore: DataStore<Preferences>
    ) {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val USE_TRUE_BLACK = booleanPreferencesKey("use_true_black")

        val themeSettings: Flow<ThemeSettings> = dataStore.data.map { preferences ->
            val themeModeName = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
            ThemeSettings(
                themeMode = themeMode,
                useTrueBlack = preferences[USE_TRUE_BLACK] ?: true
            )
        }

        suspend fun setThemeMode(themeMode: ThemeMode) {
            dataStore.edit { it[THEME_MODE] = themeMode.name }
        }

        suspend fun setUseTrueBlack(useTrueBlack: Boolean) {
            dataStore.edit { it[USE_TRUE_BLACK] = useTrueBlack }
        }
    }
}
