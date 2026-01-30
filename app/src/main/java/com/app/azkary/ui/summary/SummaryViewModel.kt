package com.app.azkary.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.model.WindowCalculationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

    init {
        println("DEBUG: SummaryViewModel - ViewModel initialized")
        // Auto-refresh prayer times when ViewModel is created and location is enabled
        viewModelScope.launch {
            userPreferencesRepository.locationPreferences.collect { prefs ->
                println("DEBUG: SummaryViewModel - Location prefs changed: useLocation=${prefs.useLocation}, location=${prefs.lastResolvedLocation}")
                if (prefs.useLocation && prefs.lastResolvedLocation != null) {
                    println("DEBUG: SummaryViewModel - Location enabled, calling refreshPrayerTimes")
                    refreshPrayerTimes()
                } else {
                    println("DEBUG: SummaryViewModel - Location not enabled or no location, keeping sessionEndTime null")
                    _sessionEndTime.value = null
                }
            }
        }
    }

    private val effectiveLang = userPreferencesRepository.appLanguage.map {
        if (it == AppLanguage.SYSTEM) Locale.getDefault().language else it.tag
    }
    private val fallbackTags = listOf("ar", "en")

    val categories: Flow<List<CategoryUi>> = effectiveLang.flatMapLatest { lang ->
        repository.observeCategoriesWithDisplayName(
            langTag = lang,
            fallbackTags = fallbackTags
        )
    }

    // Prayer times state - must be declared before currentSession uses it
    private val _sessionEndTime = MutableStateFlow<String?>(null)
    val sessionEndTime: StateFlow<String?> = _sessionEndTime.asStateFlow()

    private val _currentWindows = MutableStateFlow<WindowCalculationResult?>(null)
    val currentWindows: StateFlow<WindowCalculationResult?> = _currentWindows.asStateFlow()

    /**
     * Maps AzkarWindow to SystemCategoryKey for category selection
     */
    private fun azkarWindowToCategoryKey(window: com.app.azkary.domain.model.AzkarWindow): SystemCategoryKey {
        return when (window) {
            com.app.azkary.domain.model.AzkarWindow.MORNING -> SystemCategoryKey.MORNING
            com.app.azkary.domain.model.AzkarWindow.NIGHT -> SystemCategoryKey.NIGHT
            com.app.azkary.domain.model.AzkarWindow.SLEEP -> SystemCategoryKey.SLEEP
        }
    }

    val currentSession: Flow<CategoryUi?> = categories.combine(currentWindows) { categoryList, windows ->
        val targetKey = windows?.currentWindow?.window?.let { azkarWindowToCategoryKey(it) }
        categoryList.find { it.systemKey == targetKey }
            ?: categoryList.find { it.systemKey == SystemCategoryKey.MORNING }
            ?: categoryList.find { it.systemKey == SystemCategoryKey.NIGHT }
            ?: categoryList.firstOrNull()
    }

    private fun refreshPrayerTimes() {
        viewModelScope.launch {
            try {
                println("DEBUG: SummaryViewModel - refreshPrayerTimes called")
                // Get current location preferences without blocking
                val locationPrefs = userPreferencesRepository.locationPreferences.first()
                println("DEBUG: SummaryViewModel - Location prefs: useLocation=${locationPrefs.useLocation}, location=${locationPrefs.lastResolvedLocation}")
                
                if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
                    println("DEBUG: SummaryViewModel - Location not enabled or null, keeping sessionEndTime null")
                    // Graceful fallback - keep default behavior if location unavailable
                    _sessionEndTime.value = null
                    return@launch
                }
                
                val location = locationPrefs.lastResolvedLocation
                println("DEBUG: SummaryViewModel - Getting windows for location: $location")
                
                val windows = prayerTimesRepository.getCurrentWindows(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                println("DEBUG: SummaryViewModel - Received windows: currentWindow=${windows.currentWindow}, nextWindow=${windows.nextWindow}")
                _currentWindows.value = windows
                updateSessionEndTime(windows)
            } catch (e: Exception) {
                println("DEBUG: SummaryViewModel - Error in refreshPrayerTimes: ${e.message}")
                e.printStackTrace()
                // Graceful fallback - keep default behavior if prayer times unavailable
                _sessionEndTime.value = null
            }
        }
    }

    private fun updateSessionEndTime(windows: WindowCalculationResult) {
        println("DEBUG: SummaryViewModel - updateSessionEndTime called with windows: $windows")
        windows.currentWindow?.let { currentWindow ->
            // Get the current locale for formatting
            val currentLocale = Locale.getDefault()
            val endTime = currentWindow.end.atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale))
            println("DEBUG: SummaryViewModel - Setting sessionEndTime to: $endTime for window: ${currentWindow.window}")
            _sessionEndTime.value = endTime
        } ?: run {
            println("DEBUG: SummaryViewModel - No current window found, setting sessionEndTime to null")
            _sessionEndTime.value = null
        }
    }
}
