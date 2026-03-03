package com.app.azkary.ui.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.DayProgress
import com.app.azkary.data.model.SystemCategoryKey
import java.time.LocalDate
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.IslamicDateProvider
import com.app.azkary.domain.model.WindowCalculationResult
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val islamicDateProvider: IslamicDateProvider,
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

    // Prayer times state - must be declared before categories uses it
    private val _sessionEndTime = MutableStateFlow<String?>(null)
    val sessionEndTime: StateFlow<String?> = _sessionEndTime.asStateFlow()

    private val _currentWindows = MutableStateFlow<WindowCalculationResult?>(null)
    val currentWindows: StateFlow<WindowCalculationResult?> = _currentWindows.asStateFlow()

    // Edit mode state - must be declared before categories uses it
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    val categories: Flow<List<CategoryUi>> = combine(
        localeManager.currentLangTagFlow.flatMapLatest { lang ->
            flow { emit(islamicDateProvider.getCurrentDate().toString()) }.flatMapLatest { date ->
                repository.observeCategoriesWithDisplayName(
                    langTag = lang,
                    date = date
                )
            }
        },
        currentWindows,
        isEditMode
    ) { categoryList, windows, editMode ->
        if (editMode) {
            // In edit mode, use default order for manual reordering
            categoryList
        } else {
            // Sort by time relevance: on-time categories first
            sortCategoriesByTimeRelevance(categoryList, windows)
        }
    }

    val holdToComplete: StateFlow<Boolean> = userPreferencesRepository.holdToComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val showWeeklyProgress: StateFlow<Boolean> = userPreferencesRepository.showWeeklyProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

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

    /**
     * Gets the list of SystemCategoryKey values that match the current time window.
     * Note: NIGHT window includes both NIGHT and EVENING categories since they share the same time period (Asr to Isha).
     */
    private fun getCurrentWindowKeys(windows: WindowCalculationResult?): Set<SystemCategoryKey> {
        val currentWindow = windows?.currentWindow?.window ?: return emptySet()
        return when (currentWindow) {
            com.app.azkary.domain.model.AzkarWindow.MORNING -> setOf(SystemCategoryKey.MORNING)
            com.app.azkary.domain.model.AzkarWindow.NIGHT -> setOf(SystemCategoryKey.NIGHT, SystemCategoryKey.EVENING)
            com.app.azkary.domain.model.AzkarWindow.SLEEP -> setOf(SystemCategoryKey.SLEEP)
        }
    }

    /**
     * Sorts categories by time relevance:
     * 1. On-time categories (matching current window) come first
     * 2. Then other categories in natural order: MORNING, EVENING, NIGHT, SLEEP
     * 3. Custom (USER) categories are sorted by their system key or placed at the end
     */
    private fun sortCategoriesByTimeRelevance(
        categories: List<CategoryUi>,
        windows: WindowCalculationResult?
    ): List<CategoryUi> {
        val currentWindowKeys = getCurrentWindowKeys(windows)

        // Define the natural order for system categories
        val naturalOrder = listOf(
            SystemCategoryKey.MORNING,
            SystemCategoryKey.EVENING,
            SystemCategoryKey.NIGHT,
            SystemCategoryKey.SLEEP
        )

        return categories.sortedWith(compareBy(
            // First: on-time categories (0) come before others (1)
            { category ->
                if (category.systemKey in currentWindowKeys) 0 else 1
            },
            // Second: sort by natural order within each group
            { category ->
                category.systemKey?.let { naturalOrder.indexOf(it) } ?: Int.MAX_VALUE
            }
        ))
    }

    val currentSession: Flow<CategoryUi?> = categories.combine(currentWindows) { categoryList, windows ->
        val targetKey = windows?.currentWindow?.window?.let { azkarWindowToCategoryKey(it) }
        categoryList.find { it.systemKey == targetKey }
            ?: categoryList.find { it.systemKey == SystemCategoryKey.MORNING }
            ?: categoryList.find { it.systemKey == SystemCategoryKey.NIGHT }
            ?: categoryList.firstOrNull()
    }

    /**
     * Weekly progress for the last 7 days.
     * Combines repository data with current date to create DayProgress list.
     */
    val weeklyProgress: Flow<List<DayProgress>> = localeManager.currentLangTagFlow.flatMapLatest { langTag ->
        repository.getWeeklyProgress(langTag).map { dateProgressMap ->
            val today = LocalDate.now()
            val todayDayOfWeek = today.dayOfWeek.value % 7 + 1 // Convert to 1-7 (Sun-Sat)

            // Build list for last 7 days (oldest to newest)
            (0..6).map { daysAgo ->
                val date = today.minusDays(6 - daysAgo.toLong()) // Start from 6 days ago
                val dateString = date.toString()
                val progress = dateProgressMap[dateString] ?: 0f
                val dayOfWeek = date.dayOfWeek.value % 7 + 1 // Convert ISO (Mon=1) to Sun=1

                DayProgress(
                    date = dateString,
                    dayOfWeek = dayOfWeek,
                    dayOfMonth = date.dayOfMonth,
                    progress = progress,
                    isToday = date == today
                )
            }
        }
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
            val today = islamicDateProvider.getCurrentDate().toString()

            if (category.progress >= 1f) {
                repository.markCategoryIncomplete(categoryId, today)
            } else {
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
