package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.azkary.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class LocationPreferences(
    val useLocation: Boolean = true,
    val lastResolvedLocation: LatLng? = null,
    val locationName: String? = null
)

data class ReadingPreferences(
    val holdToComplete: Boolean = true
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val USE_LOCATION = booleanPreferencesKey("use_location")
    private val LAST_RESOLVED_LOCATION = stringPreferencesKey("last_resolved_location")
    private val LOCATION_NAME = stringPreferencesKey("location_name")
    private val HOLD_TO_COMPLETE = booleanPreferencesKey("hold_to_complete")
    private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val SHOW_WEEKLY_PROGRESS = booleanPreferencesKey("show_weekly_progress")
    private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
    private val FIRST_INSTALL_DATE = longPreferencesKey("first_install_date")
    private val LAST_PROMPT_VERSION = stringPreferencesKey("last_prompt_version")

    val locationPreferences: Flow<LocationPreferences> = context.dataStore.data.map { preferences ->
        LocationPreferences(
            useLocation = preferences[USE_LOCATION] ?: true,
            lastResolvedLocation = preferences[LAST_RESOLVED_LOCATION]?.let {
                try { json.decodeFromString<LatLng>(it) } catch (e: Exception) { null }
            },
            locationName = preferences[LOCATION_NAME]
        )
    }

    val holdToComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HOLD_TO_COMPLETE] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED] ?: true
    }

    val showWeeklyProgress: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_WEEKLY_PROGRESS] ?: true
    }

    suspend fun setUseLocation(enabled: Boolean) {
        context.dataStore.edit { it[USE_LOCATION] = enabled }
    }

    suspend fun setLastResolvedLocation(location: LatLng?) {
        context.dataStore.edit {
            if (location != null) {
                it[LAST_RESOLVED_LOCATION] = json.encodeToString(location)
            } else {
                it.remove(LAST_RESOLVED_LOCATION)
            }
        }
    }

    suspend fun setLocationName(name: String?) {
        context.dataStore.edit {
            if (name != null) {
                it[LOCATION_NAME] = name
            } else {
                it.remove(LOCATION_NAME)
            }
        }
    }

    suspend fun setHoldToComplete(enabled: Boolean) {
        context.dataStore.edit { it[HOLD_TO_COMPLETE] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setShowWeeklyProgress(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_WEEKLY_PROGRESS] = enabled }
    }

    val appOpenCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[APP_OPEN_COUNT] ?: 0
    }

    val firstInstallDate: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[FIRST_INSTALL_DATE] ?: System.currentTimeMillis()
    }

    val lastPromptVersion: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_PROMPT_VERSION]
    }

    suspend fun incrementAppOpenCount() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[APP_OPEN_COUNT] ?: 0
            preferences[APP_OPEN_COUNT] = currentCount + 1
        }
    }

    suspend fun initializeFirstInstallDate() {
        context.dataStore.edit { preferences ->
            if (!preferences.contains(FIRST_INSTALL_DATE)) {
                preferences[FIRST_INSTALL_DATE] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setLastPromptVersion(version: String) {
        context.dataStore.edit { it[LAST_PROMPT_VERSION] = version }
    }

    suspend fun shouldShowRatingPrompt(currentVersion: String): Boolean {
        val preferences = context.dataStore.data.map { it }.first()
        val openCount = preferences[APP_OPEN_COUNT] ?: 0
        val firstInstall = preferences[FIRST_INSTALL_DATE] ?: System.currentTimeMillis()
        val lastPrompt = preferences[LAST_PROMPT_VERSION]

        val daysSinceInstall = (System.currentTimeMillis() - firstInstall) / (1000 * 60 * 60 * 24)

        return openCount >= 5 && 
               daysSinceInstall >= 3 && 
               lastPrompt != currentVersion
    }
}
