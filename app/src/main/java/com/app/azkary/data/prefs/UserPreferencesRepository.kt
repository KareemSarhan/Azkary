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

enum class AppLanguage(val tag: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English")
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val APP_LANGUAGE = stringPreferencesKey("app_language")

    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        val tag = preferences[APP_LANGUAGE] ?: AppLanguage.SYSTEM.tag
        AppLanguage.entries.find { it.tag == tag } ?: AppLanguage.SYSTEM
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[APP_LANGUAGE] = language.tag }
    }
}
