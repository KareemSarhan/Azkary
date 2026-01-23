package com.app.azkary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val geocodingRepository: com.app.azkary.data.repository.GeocodingRepository
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
}
