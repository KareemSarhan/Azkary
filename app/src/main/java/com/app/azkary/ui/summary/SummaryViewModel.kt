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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

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
    
    val currentSession: Flow<CategoryUi?> = categories.map { list ->
        list.find { it.systemKey == SystemCategoryKey.MORNING }
            ?: list.find { it.systemKey == SystemCategoryKey.NIGHT }
            ?: list.firstOrNull()
    }

    // Prayer times state - simplified for now
    private val _sessionEndTime = MutableStateFlow<String?>(null)
    val sessionEndTime: StateFlow<String?> = _sessionEndTime.asStateFlow()

    private val _currentWindows = MutableStateFlow<WindowCalculationResult?>(null)
    val currentWindows: StateFlow<WindowCalculationResult?> = _currentWindows.asStateFlow()

    private fun refreshPrayerTimes() {
        viewModelScope.launch {
            try {
                // Get current location preferences without blocking
                val locationPrefs = userPreferencesRepository.locationPreferences.first()
                if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
                    // Graceful fallback - keep default behavior if location unavailable
                    _sessionEndTime.value = null
                    return@launch
                }
                
                val location = locationPrefs.lastResolvedLocation
                val windows = prayerTimesRepository.getCurrentWindows(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                _currentWindows.value = windows
                updateSessionEndTime(windows)
            } catch (e: Exception) {
                // Graceful fallback - keep default behavior if prayer times unavailable
                _sessionEndTime.value = null
            }
        }
    }

    private fun updateSessionEndTime(windows: WindowCalculationResult) {
        windows.currentWindow?.let { currentWindow ->
            val endTime = currentWindow.end.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            _sessionEndTime.value = endTime
        } ?: run {
            _sessionEndTime.value = null
        }
    }
}
