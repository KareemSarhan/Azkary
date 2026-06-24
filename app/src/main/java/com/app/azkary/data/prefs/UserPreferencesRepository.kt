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
import kotlinx.serialization.Serializable
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

@Serializable
data class SavedMasjid(
    val id: String,
    val name: String,
    val location: LatLng,
    val radiusMeters: Int = 120
)

data class MasjidPreferences(
    val enabled: Boolean = false,
    val dndEnabled: Boolean = false,
    val savedMasjids: List<SavedMasjid> = emptyList(),
    val activeMasjidId: String? = null,
    val dndActive: Boolean = false,
    val previousDndFilter: Int? = null,
    val lastEntryNotificationAtMillis: Long? = null
) {
    val savedLocation: LatLng? get() = savedMasjids.firstOrNull()?.location
    val radiusMeters: Int get() = savedMasjids.firstOrNull()?.radiusMeters ?: 120
    val isInsideMasjid: Boolean get() = activeMasjidId != null
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
    private val MASJID_ENABLED = booleanPreferencesKey("masjid_enabled")
    private val MASJID_DND_ENABLED = booleanPreferencesKey("masjid_dnd_enabled")
    private val MASJID_SAVED_LOCATION = stringPreferencesKey("masjid_saved_location")
    private val MASJID_SAVED_LOCATIONS = stringPreferencesKey("masjid_saved_locations")
    private val MASJID_RADIUS_METERS = intPreferencesKey("masjid_radius_meters")
    private val MASJID_IS_INSIDE = booleanPreferencesKey("masjid_is_inside")
    private val MASJID_ACTIVE_ID = stringPreferencesKey("masjid_active_id")
    private val MASJID_DND_ACTIVE = booleanPreferencesKey("masjid_dnd_active")
    private val MASJID_PREVIOUS_DND_FILTER = intPreferencesKey("masjid_previous_dnd_filter")
    private val MASJID_LAST_ENTRY_NOTIFICATION_AT = longPreferencesKey("masjid_last_entry_notification_at")

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

    val masjidPreferences: Flow<MasjidPreferences> = context.dataStore.data.map { preferences ->
        val savedMasjids = decodeSavedMasjids(preferences)
        MasjidPreferences(
            enabled = preferences[MASJID_ENABLED] ?: false,
            dndEnabled = preferences[MASJID_DND_ENABLED] ?: false,
            savedMasjids = savedMasjids,
            activeMasjidId = preferences[MASJID_ACTIVE_ID]
                ?: if (preferences[MASJID_IS_INSIDE] == true) savedMasjids.firstOrNull()?.id else null,
            dndActive = preferences[MASJID_DND_ACTIVE] ?: false,
            previousDndFilter = preferences[MASJID_PREVIOUS_DND_FILTER],
            lastEntryNotificationAtMillis = preferences[MASJID_LAST_ENTRY_NOTIFICATION_AT]
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

    suspend fun setHoldToComplete(enabled: Boolean) {
        context.dataStore.edit { it[HOLD_TO_COMPLETE] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setShowWeeklyProgress(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_WEEKLY_PROGRESS] = enabled }
    }

    suspend fun setMasjidEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MASJID_ENABLED] = enabled }
    }

    suspend fun setMasjidDndEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MASJID_DND_ENABLED] = enabled }
    }

    suspend fun setMasjidSavedLocation(location: LatLng?) {
        context.dataStore.edit {
            if (location != null) {
                it[MASJID_SAVED_LOCATION] = json.encodeToString(location)
                val masjid = SavedMasjid(
                    id = "legacy-current-masjid",
                    name = "Masjid",
                    location = location,
                    radiusMeters = it[MASJID_RADIUS_METERS] ?: 120
                )
                it[MASJID_SAVED_LOCATIONS] = json.encodeToString(listOf(masjid))
            } else {
                it.remove(MASJID_SAVED_LOCATION)
                it.remove(MASJID_SAVED_LOCATIONS)
                it.remove(MASJID_ACTIVE_ID)
                it[MASJID_IS_INSIDE] = false
            }
        }
    }

    suspend fun upsertMasjid(masjid: SavedMasjid) {
        context.dataStore.edit { preferences ->
            val current = decodeSavedMasjids(preferences)
            val normalizedMasjid = masjid.copy(
                name = masjid.name.ifBlank { "Masjid" },
                radiusMeters = masjid.radiusMeters.coerceIn(50, 500)
            )
            val updated = if (current.any { it.id == normalizedMasjid.id }) {
                current.map { if (it.id == normalizedMasjid.id) normalizedMasjid else it }
            } else {
                current + normalizedMasjid
            }
            preferences[MASJID_SAVED_LOCATIONS] = json.encodeToString(updated)
            preferences[MASJID_ENABLED] = true
        }
    }

    suspend fun removeMasjid(id: String) {
        context.dataStore.edit { preferences ->
            val updated = decodeSavedMasjids(preferences).filterNot { it.id == id }
            if (updated.isEmpty()) {
                preferences.remove(MASJID_SAVED_LOCATIONS)
                preferences.remove(MASJID_ACTIVE_ID)
                preferences[MASJID_IS_INSIDE] = false
            } else {
                preferences[MASJID_SAVED_LOCATIONS] = json.encodeToString(updated)
                if (preferences[MASJID_ACTIVE_ID] == id) {
                    preferences.remove(MASJID_ACTIVE_ID)
                    preferences[MASJID_IS_INSIDE] = false
                }
            }
        }
    }

    suspend fun setMasjidRadiusMeters(radiusMeters: Int) {
        context.dataStore.edit {
            it[MASJID_RADIUS_METERS] = radiusMeters.coerceIn(50, 500)
        }
    }

    suspend fun setMasjidInside(isInside: Boolean) {
        context.dataStore.edit { it[MASJID_IS_INSIDE] = isInside }
    }

    suspend fun setActiveMasjid(id: String?) {
        context.dataStore.edit {
            if (id != null) {
                it[MASJID_ACTIVE_ID] = id
                it[MASJID_IS_INSIDE] = true
            } else {
                it.remove(MASJID_ACTIVE_ID)
                it[MASJID_IS_INSIDE] = false
            }
        }
    }

    suspend fun setMasjidDndSession(active: Boolean, previousFilter: Int?) {
        context.dataStore.edit {
            it[MASJID_DND_ACTIVE] = active
            if (previousFilter != null) {
                it[MASJID_PREVIOUS_DND_FILTER] = previousFilter
            } else {
                it.remove(MASJID_PREVIOUS_DND_FILTER)
            }
        }
    }

    suspend fun setMasjidLastEntryNotificationAt(timestampMillis: Long) {
        context.dataStore.edit { it[MASJID_LAST_ENTRY_NOTIFICATION_AT] = timestampMillis }
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

    private fun decodeSavedMasjids(preferences: Preferences): List<SavedMasjid> {
        val storedList = preferences[MASJID_SAVED_LOCATIONS]?.let {
            try {
                json.decodeFromString<List<SavedMasjid>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        }.orEmpty()

        if (storedList.isNotEmpty()) {
            return storedList
        }

        val legacyLocation = preferences[MASJID_SAVED_LOCATION]?.let {
            try {
                json.decodeFromString<LatLng>(it)
            } catch (e: Exception) {
                null
            }
        } ?: return emptyList()

        return listOf(
            SavedMasjid(
                id = "legacy-current-masjid",
                name = "Masjid",
                location = legacyLocation,
                radiusMeters = preferences[MASJID_RADIUS_METERS] ?: 120
            )
        )
    }
}
