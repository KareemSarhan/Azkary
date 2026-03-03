package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.azkary.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
    private val SHOW_WEEKLY_PROGRESS = booleanPreferencesKey("show_weekly_progress")
    private val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")

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

    val showWeeklyProgress: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_WEEKLY_PROGRESS] ?: true
    }

    val enableAnimations: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_ANIMATIONS] ?: true
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

    suspend fun setShowWeeklyProgress(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_WEEKLY_PROGRESS] = enabled }
    }

    suspend fun setEnableAnimations(enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_ANIMATIONS] = enabled }
    }
}
