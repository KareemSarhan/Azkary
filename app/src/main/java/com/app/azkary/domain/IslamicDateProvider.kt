package com.app.azkary.domain

import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslamicDateProvider @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
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
}
