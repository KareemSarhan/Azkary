package com.app.azkary.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class LayoutDirection {
    SYSTEM, RTL, LTR
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LAYOUT_DIRECTION = stringPreferencesKey("layout_direction")
    private val ARABIC_FONT_SIZE = floatPreferencesKey("arabic_font_size")
    private val TRANSLATION_FONT_SIZE = floatPreferencesKey("translation_font_size")
    private val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")

    val layoutDirection: Flow<LayoutDirection> = context.dataStore.data.map { preferences ->
        LayoutDirection.valueOf(preferences[LAYOUT_DIRECTION] ?: LayoutDirection.SYSTEM.name)
    }

    val arabicFontSize: Flow<Float> = context.dataStore.data.map { it[ARABIC_FONT_SIZE] ?: 28f }

    suspend fun setLayoutDirection(direction: LayoutDirection) {
        context.dataStore.edit { it[LAYOUT_DIRECTION] = direction.name }
    }
}
