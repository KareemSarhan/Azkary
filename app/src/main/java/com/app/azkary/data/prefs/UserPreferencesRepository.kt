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

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val USE_LOCATION = booleanPreferencesKey("use_location")
    private val LAST_RESOLVED_LOCATION = stringPreferencesKey("last_resolved_location")
    private val LOCATION_NAME = stringPreferencesKey("location_name")

    val locationPreferences: Flow<LocationPreferences> = context.dataStore.data.map { preferences ->
        LocationPreferences(
            useLocation = preferences[USE_LOCATION] ?: true,
            lastResolvedLocation = preferences[LAST_RESOLVED_LOCATION]?.let {
                try { json.decodeFromString<LatLng>(it) } catch (e: Exception) { null }
            },
            locationName = preferences[LOCATION_NAME]
        )
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
}
