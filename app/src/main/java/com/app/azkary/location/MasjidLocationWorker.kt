package com.app.azkary.location

import android.content.Context
import android.location.Location
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.MasjidPreferences
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.LocationRepository
import com.app.azkary.notification.AzkarNotificationManager
import com.app.azkary.notification.MasjidDndManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class MasjidLocationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val notificationManager: AzkarNotificationManager,
    private val dndManager: MasjidDndManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG = "masjid_location_worker"
        const val UNIQUE_PERIODIC_WORK_NAME = "masjid_location_periodic_check"
        const val UNIQUE_ONE_TIME_WORK_NAME = "masjid_location_one_time_check"
        private val ENTRY_NOTIFICATION_COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(30)
    }

    override suspend fun doWork(): Result {
        val preferences = userPreferencesRepository.masjidPreferences.first()

        if (!preferences.enabled || preferences.savedLocation == null) {
            restoreIfNeeded(preferences)
            if (preferences.isInsideMasjid) {
                userPreferencesRepository.setMasjidInside(false)
            }
            return Result.success()
        }

        val currentLocation = locationRepository.getCurrentLocation() ?: return Result.success()
        val isInside = distanceMeters(currentLocation, preferences.savedLocation) <=
            preferences.radiusMeters

        when {
            preferences.dndActive && !preferences.dndEnabled -> restoreIfNeeded(preferences)
            isInside && !preferences.isInsideMasjid -> handleEnter(preferences)
            isInside && preferences.dndEnabled && !preferences.dndActive -> {
                dndManager.enableForMasjid(preferences)
            }
            !isInside && preferences.isInsideMasjid -> handleExit(preferences)
            !isInside && preferences.dndActive -> restoreIfNeeded(preferences)
        }

        return Result.success()
    }

    private suspend fun handleEnter(preferences: MasjidPreferences) {
        val now = System.currentTimeMillis()
        val lastShown = preferences.lastEntryNotificationAtMillis
        val shouldNotify = lastShown == null ||
            now - lastShown >= ENTRY_NOTIFICATION_COOLDOWN_MILLIS

        if (shouldNotify) {
            notificationManager.showMasjidEntryNotification()
            userPreferencesRepository.setMasjidLastEntryNotificationAt(now)
        }

        if (preferences.dndEnabled) {
            dndManager.enableForMasjid(preferences)
        }
        userPreferencesRepository.setMasjidInside(true)
    }

    private suspend fun handleExit(preferences: MasjidPreferences) {
        restoreIfNeeded(preferences)
        userPreferencesRepository.setMasjidInside(false)
    }

    private suspend fun restoreIfNeeded(preferences: MasjidPreferences) {
        if (preferences.dndActive) {
            dndManager.restoreFromMasjid(preferences)
        }
    }

    private fun distanceMeters(current: Location, target: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            target.latitude,
            target.longitude,
            results
        )
        return results[0]
    }
}
