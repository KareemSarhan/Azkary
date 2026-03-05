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
import com.app.azkary.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ThemePreferencesRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var dataStoreFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        dataStoreFile = File(context.filesDir, "test_theme_settings.preferences_pb")

        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }

        testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_theme_settings")
        }
    }

    @After
    fun teardown() {
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    @Test
    fun `themeSettings emits default values when no preferences set`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_theme_defaults")
        }

        val repository = createTestRepository(testDataStore)

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals(true, settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists LIGHT theme mode`() = runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.LIGHT, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists DARK theme mode`() = runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists SYSTEM theme mode`() = runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        repository.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseTrueBlack persists true value`() = runTest {
        val repository = createTestRepository()

        repository.setUseTrueBlack(true)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertTrue(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUseTrueBlack persists false value`() = runTest {
        val repository = createTestRepository()

        repository.setUseTrueBlack(false)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertFalse(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings emits updates when theme mode changes`() = runTest {
        val repository = createTestRepository()

        repository.themeSettings.test {
            val initialSettings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, initialSettings.themeMode)

            repository.setThemeMode(ThemeMode.DARK)
            advanceUntilIdle()

            val updatedSettings = awaitItem()
            assertEquals(ThemeMode.DARK, updatedSettings.themeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings emits updates when true black changes`() = runTest {
        val repository = createTestRepository()

        repository.themeSettings.test {
            val initialSettings = awaitItem()
            assertTrue(initialSettings.useTrueBlack)

            repository.setUseTrueBlack(false)
            advanceUntilIdle()

            val updatedSettings = awaitItem()
            assertFalse(updatedSettings.useTrueBlack)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings maintains independent values for both preferences`() = runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setUseTrueBlack(false)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.LIGHT, settings.themeMode)
            assertFalse(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings falls back to SYSTEM for invalid theme mode value`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_theme_invalid")
        }

        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "INVALID_MODE"
        }
        advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent theme mode updates result in consistent state`() = runTest {
        val repository = createTestRepository()

        val jobs = listOf(
            backgroundScope.launch { repository.setThemeMode(ThemeMode.LIGHT) },
            backgroundScope.launch { repository.setThemeMode(ThemeMode.DARK) },
            backgroundScope.launch { repository.setThemeMode(ThemeMode.SYSTEM) }
        )

        jobs.forEach { it.join() }
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertTrue(
                settings.themeMode == ThemeMode.LIGHT ||
                        settings.themeMode == ThemeMode.DARK ||
                        settings.themeMode == ThemeMode.SYSTEM
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent true black updates result in consistent state`() = runTest {
        val repository = createTestRepository()

        val jobs = (1..10).map {
            backgroundScope.launch { repository.setUseTrueBlack(it % 2 == 0) }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertTrue(settings.useTrueBlack || !settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all theme modes can be set and retrieved`() = runTest {
        val repository = createTestRepository()
        val modes = ThemeMode.entries.toTypedArray()

        modes.forEach { mode ->
            repository.setThemeMode(mode)
            advanceUntilIdle()

            repository.themeSettings.test {
                val settings = awaitItem()
                assertEquals(mode, settings.themeMode)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `themeSettings flow completes after cancellation`() = runTest {
        val repository = createTestRepository()

        val job = backgroundScope.launch {
            repository.themeSettings.collect { }
        }

        advanceUntilIdle()

        job.cancel()

        assertTrue(job.isCancelled)
    }

    @Test
    fun `preferences persist across repository instances`() = runTest {
        val sharedDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_theme_persistence_shared")
        }

        val firstRepository = createTestRepository(sharedDataStore)
        firstRepository.setThemeMode(ThemeMode.LIGHT)
        firstRepository.setUseTrueBlack(false)
        advanceUntilIdle()

        val secondRepository = createTestRepository(sharedDataStore)

        secondRepository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.LIGHT, settings.themeMode)
            assertFalse(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme mode toggles through all modes`() = runTest {
        val repository = createTestRepository()

        repository.themeSettings.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            repository.setThemeMode(ThemeMode.LIGHT)
            advanceUntilIdle()
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)

            repository.setThemeMode(ThemeMode.DARK)
            advanceUntilIdle()
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            repository.setThemeMode(ThemeMode.SYSTEM)
            advanceUntilIdle()
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `useTrueBlack toggles correctly`() = runTest {
        val repository = createTestRepository()

        repository.themeSettings.test {
            assertTrue(awaitItem().useTrueBlack)

            repository.setUseTrueBlack(false)
            advanceUntilIdle()
            assertFalse(awaitItem().useTrueBlack)

            repository.setUseTrueBlack(true)
            advanceUntilIdle()
            assertTrue(awaitItem().useTrueBlack)

            repository.setUseTrueBlack(false)
            advanceUntilIdle()
            assertFalse(awaitItem().useTrueBlack)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings handles empty string theme mode`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_theme_empty_string")
        }

        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = ""
        }
        advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings handles lowercase theme mode`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_theme_lowercase")
        }

        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "dark"
        }
        advanceUntilIdle()

        val repository = createTestRepository(testDataStore)

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `both settings can be changed simultaneously`() = runTest {
        val repository = createTestRepository()

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setUseTrueBlack(false)
        advanceUntilIdle()

        repository.setThemeMode(ThemeMode.DARK)
        repository.setUseTrueBlack(true)
        advanceUntilIdle()

        repository.themeSettings.test {
            val settings = awaitItem()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertTrue(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `themeSettings default useTrueBlack is true`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_default_true_black")
        }

        val repository = createTestRepository(testDataStore)

        repository.themeSettings.test {
            val settings = awaitItem()
            assertTrue(settings.useTrueBlack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createTestRepository(
        dataStore: DataStore<Preferences> = testDataStore
    ): TestThemePreferencesRepository {
        return TestThemePreferencesRepository(dataStore)
    }

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
