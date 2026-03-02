package com.app.azkary.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.layoutDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocaleManager - Read-only locale management using AndroidX AppCompatDelegate
 *
 * This class provides read-only access to the current locale.
 * Language changes are now handled at the system level via Android's app-specific language settings.
 */
@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Observable language tag that updates on configuration changes
    private val _currentLangTag = MutableStateFlow(getCurrentLanguageTag(context))
    val currentLangTagFlow: StateFlow<String> = _currentLangTag.asStateFlow()

    /**
     * Get the current locale of the application
     *
     * @return The current Locale
     */
    fun getCurrentLocale(context: Context): Locale {
        // Try to get from AppCompatDelegate first (Android 13+)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            return currentLocales[0] ?: Locale.getDefault()
        }

        // Fallback to configuration
        return context.resources.configuration.locales[0] ?: Locale.getDefault()
    }

    /**
     * Check if the current locale is RTL (Right-to-Left)
     *
     * @return true if RTL, false otherwise
     */
    fun isCurrentLocaleRtl(context: Context): Boolean {
        val locale = getCurrentLocale(context)
        return locale.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
    }

    /**
     * Get the language tag for the current locale
     *
     * @return Language tag (e.g., "en", "ar")
     */
    fun getCurrentLanguageTag(context: Context): String {
        return getCurrentLocale(context).language
    }

    /**
     * Get the display name of the current language
     *
     * @return Display name (e.g., "English", "العربية")
     */
    fun getCurrentLanguageDisplayName(context: Context): String {
        val locale = getCurrentLocale(context)
        return locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
    }

    /**
     * Open the Android app-specific language settings
     *
     * This opens the system language settings where users can change
     * the app's language preference.
     */
    fun openAppLanguageSettings(context: Context) {
        val pm = context.packageManager

        // Android 13+ (API 33): per-app language page (if Settings app supports it)
        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null) // IMPORTANT
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Avoid ActivityNotFoundException on devices where Settings doesn't expose this screen
        if (intent.resolveActivity(pm) != null) {
            runCatching { context.startActivity(intent) }
                .onSuccess { return }
        }

        // Fallback: app details settings
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (fallback.resolveActivity(pm) != null) {
            runCatching { context.startActivity(fallback) }
                .onFailure { android.util.Log.e("TAG", "Failed to open app settings", it) }
        } else {
            android.util.Log.e("TAG", "No Activity found to handle app settings intents")
        }
    }

    /**
     * Call this when configuration changes (e.g., from MainActivity.onConfigurationChanged)
     * This notifies all subscribers that the language may have changed
     */
    fun notifyLocaleChanged() {
        val newLang = getCurrentLanguageTag(context)
        val oldLang = _currentLangTag.value
        println("DEBUG: LocaleManager - Language changed check: old=$oldLang, new=$newLang")
        if (oldLang != newLang) {
            println("DEBUG: LocaleManager - Updating language from $oldLang to $newLang")
            _currentLangTag.value = newLang
        }
    }

}
