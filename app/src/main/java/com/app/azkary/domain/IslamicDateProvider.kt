package com.app.azkary.domain

import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslamicDateProvider @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val _currentDate = MutableStateFlow<LocalDate?>(null)
    val currentDateFlow: StateFlow<LocalDate?> = _currentDate.asStateFlow()

    suspend fun getCurrentDate(): LocalDate {
        val locationPrefs = userPreferencesRepository.locationPreferences.first()

        if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
            return LocalDate.now()
        }

        val location = locationPrefs.lastResolvedLocation

        return try {
            prayerTimesRepository.getIslamicCurrentDate(
                latitude = location.latitude,
                longitude = location.longitude
            )
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    suspend fun refreshDate() {
        _currentDate.value = getCurrentDate()
    }
}
