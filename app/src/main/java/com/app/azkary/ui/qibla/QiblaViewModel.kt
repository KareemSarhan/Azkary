package com.app.azkary.ui.qibla

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.R
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.domain.QiblaCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QiblaUiState(
    val location: LatLng? = null,
    val locationName: String? = null,
    val bearingDegrees: Double? = null,
    val isLoadingLocation: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.locationPreferences.collect { preferences ->
                val location = preferences.lastResolvedLocation
                if (location != null && _uiState.value.location == null) {
                    setLocation(
                        location = location,
                        locationName = preferences.locationName
                    )
                }
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingLocation = true,
                errorMessage = null
            )

            try {
                val currentLocation = locationRepository.getCurrentLocation()
                if (currentLocation == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        errorMessage = context.getString(R.string.error_location_permission)
                    )
                    return@launch
                }

                val latLng = LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude
                )
                userPreferencesRepository.setLastResolvedLocation(latLng)
                setLocation(
                    location = latLng,
                    locationName = null,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    errorMessage = "${context.getString(R.string.error_location_generic)}: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun setLocation(
        location: LatLng,
        locationName: String?,
        isLoading: Boolean = _uiState.value.isLoadingLocation
    ) {
        _uiState.value = _uiState.value.copy(
            location = location,
            locationName = locationName,
            bearingDegrees = QiblaCalculator.bearingToKaaba(location),
            isLoadingLocation = isLoading,
            errorMessage = null
        )
    }
}
