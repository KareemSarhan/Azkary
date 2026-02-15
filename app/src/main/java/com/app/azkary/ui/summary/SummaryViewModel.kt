package com.app.azkary.ui.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.model.WindowCalculationResult
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val prayerTimesRepository: PrayerTimesRepository,
    private val localeManager: LocaleManager,
    @ApplicationContext private val context: Context
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

    val categories: Flow<List<CategoryUi>> = localeManager.currentLangTagFlow.flatMapLatest { lang ->
        repository.observeCategoriesWithDisplayName(
            langTag = lang,
        )
    }

    // Prayer times state - must be declared before currentSession uses it
    private val _sessionEndTime = MutableStateFlow<String?>(null)
    val sessionEndTime: StateFlow<String?> = _sessionEndTime.asStateFlow()

    private val _currentWindows = MutableStateFlow<WindowCalculationResult?>(null)
    val currentWindows: StateFlow<WindowCalculationResult?> = _currentWindows.asStateFlow()

    // Edit mode state
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

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

    /**
     * Toggle category completion status on long-press
     * If category is incomplete (progress < 100%): Mark all items as complete
     * If category is complete (progress >= 100%): Mark all items as incomplete
     */
    fun toggleCategoryCompletion(categoryId: String) {
        viewModelScope.launch {
            val category = categories.first().find { it.id == categoryId } ?: return@launch
            val today = java.time.LocalDate.now().toString()

            if (category.progress >= 1f) {
                // Category is complete, mark as incomplete
                repository.markCategoryIncomplete(categoryId, today)
            } else {
                // Category is incomplete, mark as complete
                repository.markCategoryComplete(categoryId, today)
            }
        }
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun reorderCategories(categoryIds: List<String>) {
        viewModelScope.launch {
            repository.reorderCategories(categoryIds)
        }
    }

    fun moveCategoryUp(index: Int) {
        viewModelScope.launch {
            val currentCategories = categories.first().toMutableList()
            if (index > 0 && index < currentCategories.size) {
                val currentCategory = currentCategories[index]
                val previousCategory = currentCategories[index - 1]
                
                currentCategories[index] = previousCategory
                currentCategories[index - 1] = currentCategory
                
                val categoryIds = currentCategories.map { it.id }
                repository.reorderCategories(categoryIds)
            }
        }
    }

    fun moveCategoryDown(index: Int) {
        viewModelScope.launch {
            val currentCategories = categories.first().toMutableList()
            if (index >= 0 && index < currentCategories.size - 1) {
                val currentCategory = currentCategories[index]
                val nextCategory = currentCategories[index + 1]
                
                currentCategories[index] = nextCategory
                currentCategories[index + 1] = currentCategory
                
                val categoryIds = currentCategories.map { it.id }
                repository.reorderCategories(categoryIds)
            }
        }
    }
}
