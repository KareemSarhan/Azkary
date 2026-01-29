package com.app.azkary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.domain.model.DayPrayerTimes
import com.app.azkary.domain.model.WindowCalculationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val geocodingRepository: com.app.azkary.data.repository.GeocodingRepository,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {
    val appLanguage = userPreferencesRepository.appLanguage

    val locationPreferences = userPreferencesRepository.locationPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.app.azkary.data.prefs.LocationPreferences()
        )

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation: StateFlow<Boolean> = _isRefreshingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    // Prayer times state
    private val _todayPrayerTimes = MutableStateFlow<DayPrayerTimes?>(null)
    val todayPrayerTimes: StateFlow<DayPrayerTimes?> = _todayPrayerTimes.asStateFlow()

    private val _currentWindows = MutableStateFlow<WindowCalculationResult?>(null)
    val currentWindows: StateFlow<WindowCalculationResult?> = _currentWindows.asStateFlow()

    private val _prayerTimesError = MutableStateFlow<String?>(null)
    val prayerTimesError: StateFlow<String?> = _prayerTimesError.asStateFlow()

    private val _isRefreshingPrayerTimes = MutableStateFlow(false)
    val isRefreshingPrayerTimes: StateFlow<Boolean> = _isRefreshingPrayerTimes.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(language)
        }
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

                    // Reverse geocode to get city name
                    val cityName = geocodingRepository.getCityName(
                        latLng.latitude,
                        latLng.longitude
                    )
                    userPreferencesRepository.setLocationName(cityName)
                } else {
                    _locationError.value = "Unable to get location. Check permissions."
                }
            } catch (e: Exception) {
                _locationError.value = "Location error: ${e.message}"
            } finally {
                _isRefreshingLocation.value = false
            }
        }
    }

    fun clearLocationError() {
        _locationError.value = null
    }

    // Prayer times methods
    fun refreshPrayerTimes() {
        viewModelScope.launch {
            val locationPrefs = locationPreferences.value
            if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
                _prayerTimesError.value = "Please enable location for prayer times"
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

                // Calculate current windows
                if (todayTimes != null) {
                    val windows = prayerTimesRepository.getCurrentWindows(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _currentWindows.value = windows
                }

            } catch (e: Exception) {
                _prayerTimesError.value = "Failed to load prayer times: ${e.message}"
            } finally {
                _isRefreshingPrayerTimes.value = false
            }
        }
    }

    fun clearPrayerTimesError() {
        _prayerTimesError.value = null
    }

    fun initializePrayerTimes() {
        // Auto-refresh prayer times when location becomes available
        viewModelScope.launch {
            locationPreferences.collect { prefs ->
                if (prefs.useLocation && prefs.lastResolvedLocation != null) {
                    refreshPrayerTimes()
                }
            }
        }
    }
}
