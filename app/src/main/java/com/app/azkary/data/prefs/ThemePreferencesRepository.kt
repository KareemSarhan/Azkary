package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

@Singleton
class ThemePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val USE_TRUE_BLACK = booleanPreferencesKey("use_true_black")

    val themeSettings: Flow<ThemeSettings> = context.themeDataStore.data.map { preferences ->
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
        context.themeDataStore.edit { it[THEME_MODE] = themeMode.name }
    }

    suspend fun setUseTrueBlack(useTrueBlack: Boolean) {
        context.themeDataStore.edit { it[USE_TRUE_BLACK] = useTrueBlack }
    }
}