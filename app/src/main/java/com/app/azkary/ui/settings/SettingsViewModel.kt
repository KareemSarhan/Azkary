package com.app.azkary.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.ThemeMode
import com.app.azkary.data.prefs.ThemePreferencesRepository
import com.app.azkary.data.prefs.ThemeSettings
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.model.DayPrayerTimes
import android.util.Log
import com.app.azkary.R
import com.app.azkary.domain.model.WindowCalculationResult
import com.app.azkary.notification.AzkarNotificationScheduler
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val locationRepository: LocationRepository,
    private val geocodingRepository: com.app.azkary.data.repository.GeocodingRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val azkarRepository: AzkarRepository,
    private val notificationScheduler: AzkarNotificationScheduler,
    private val localeManager: LocaleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val themeSettings: StateFlow<ThemeSettings> = themePreferencesRepository.themeSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSettings()
        )

    val locationPreferences = userPreferencesRepository.locationPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.app.azkary.data.prefs.LocationPreferences()
        )

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

    val categories: StateFlow<List<CategoryUi>> = localeManager.currentLangTagFlow.flatMapLatest { langTag ->
        azkarRepository.observeCategoriesWithDisplayName(
            langTag = langTag,
            date = LocalDate.now().toString()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation: StateFlow<Boolean> = _isRefreshingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _todayPrayerTimes = MutableStateFlow<DayPrayerTimes?>(null)
    val todayPrayerTimes: StateFlow<DayPrayerTimes?> = _todayPrayerTimes.asStateFlow()

    private val _currentWindows = MutableStateFlow<WindowCalculationResult?>(null)
    val currentWindows: StateFlow<WindowCalculationResult?> = _currentWindows.asStateFlow()

    private val _prayerTimesError = MutableStateFlow<String?>(null)
    val prayerTimesError: StateFlow<String?> = _prayerTimesError.asStateFlow()

    private val _isRefreshingPrayerTimes = MutableStateFlow(false)
    val isRefreshingPrayerTimes: StateFlow<Boolean> = _isRefreshingPrayerTimes.asStateFlow()

    fun getCurrentLanguageDisplayName(): String {
        return localeManager.getCurrentLanguageDisplayName(context)
    }

    fun openLanguageSettings() {
        localeManager.openAppLanguageSettings(context)
    }

    fun toggleUseLocation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseLocation(enabled)
            if (enabled) {
                refreshLocation()
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isRefreshingLocation.value = true
            _locationError.value = null

            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    userPreferencesRepository.setLastResolvedLocation(latLng)

                    val cityName = geocodingRepository.getCityName(
                        latLng.latitude,
                        latLng.longitude
                    )
                    userPreferencesRepository.setLocationName(cityName)
                } else {
                    _locationError.value = context.getString(R.string.error_location_permission)
                }
            } catch (e: Exception) {
                _locationError.value = "${context.getString(R.string.error_location_generic)}: ${e.message}"
            } finally {
                _isRefreshingLocation.value = false
            }
        }
    }

    fun clearLocationError() {
        _locationError.value = null
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            themePreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setHoldToComplete(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHoldToComplete(enabled)
        }
    }

    fun setShowWeeklyProgress(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowWeeklyProgress(enabled)
        }
    }

    fun setCategoryNotificationEnabled(categoryId: String, enabled: Boolean) {
        viewModelScope.launch {
            azkarRepository.setCategoryNotificationEnabled(categoryId, enabled)
            updateNotificationSchedule()
        }
    }

    private fun updateNotificationSchedule() {
        viewModelScope.launch {
            val locationPrefs = locationPreferences.value

            if (locationPrefs.useLocation && locationPrefs.lastResolvedLocation != null) {
                try {
                    val location = locationPrefs.lastResolvedLocation
                    val todayTimes = prayerTimesRepository.getDayPrayerTimes(
                        date = LocalDate.now(),
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    if (todayTimes != null) {
                        val categoriesWithNotifications = azkarRepository.getCategoriesWithNotifications()
                        notificationScheduler.scheduleNotifications(
                            todayTimes = todayTimes,
                            categories = categoriesWithNotifications
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Failed to schedule notifications", e)
                }
            }
        }
    }

    fun refreshPrayerTimes() {
        viewModelScope.launch {
            val locationPrefs = locationPreferences.value
            if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
                _prayerTimesError.value = context.getString(R.string.error_location_disabled)
                return@launch
            }

            _isRefreshingPrayerTimes.value = true
            _prayerTimesError.value = null

            try {
                val location = locationPrefs.lastResolvedLocation
                val todayTimes = prayerTimesRepository.getDayPrayerTimes(
                    date = LocalDate.now(),
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                _todayPrayerTimes.value = todayTimes

                todayTimes?.let { times ->
                    Log.d("PrayerTimes", "=== Prayer Times (Settings) - All 9 Timings ===")
                    Log.d("PrayerTimes", "Date: ${times.date}, Timezone: ${times.timezone}")
                    Log.d("PrayerTimes", "Daytime Prayers:")
                    Log.d("PrayerTimes", "  Fajr: ${times.fajr}, Sunrise: ${times.sunrise}")
                    Log.d("PrayerTimes", "  Dhuhr: ${times.dhuhr}, Asr: ${times.asr}")
                    Log.d("PrayerTimes", "  Maghrib: ${times.maghrib}, Isha: ${times.isha}")
                    Log.d("PrayerTimes", "Night/Late Prayers:")
                    Log.d("PrayerTimes", "  Firstthird: ${times.firstthird} (Tahajjud start)")
                    Log.d("PrayerTimes", "  Midnight: ${times.midnight} (Islamic)")
                    Log.d("PrayerTimes", "  Lastthird: ${times.lastthird} (Before Fajr)")
                }

                if (todayTimes != null) {
                    val windows = prayerTimesRepository.getCurrentWindows(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _currentWindows.value = windows
                    Log.d("PrayerTimes", "Current window: ${windows.currentWindow?.window}, Next: ${windows.nextWindow?.window}")

                    val categoriesWithNotifications = azkarRepository.getCategoriesWithNotifications()
                    notificationScheduler.scheduleNotifications(
                        todayTimes = todayTimes,
                        categories = categoriesWithNotifications
                    )
                }

            } catch (e: Exception) {
                _prayerTimesError.value = "${context.getString(R.string.error_prayer_times)}: ${e.message}"
            } finally {
                _isRefreshingPrayerTimes.value = false
            }
        }
    }

    fun clearPrayerTimesError() {
        _prayerTimesError.value = null
    }

    fun initializePrayerTimes() {
        viewModelScope.launch {
            locationPreferences.collect { prefs ->
                if (prefs.useLocation && prefs.lastResolvedLocation != null) {
                    refreshPrayerTimes()
                }
            }
        }
    }
}
