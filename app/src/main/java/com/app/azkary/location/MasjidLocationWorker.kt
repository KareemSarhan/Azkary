package com.app.azkary.location

import android.content.Context
import android.location.Location
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.azkary.data.prefs.MasjidPreferences
import com.app.azkary.data.prefs.SavedMasjid
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

        if (!preferences.enabled || preferences.savedMasjids.isEmpty()) {
            restoreIfNeeded(preferences)
            if (preferences.activeMasjidId != null) {
                userPreferencesRepository.setActiveMasjid(null)
            }
            return Result.success()
        }

        val currentLocation = locationRepository.getCurrentLocation() ?: return Result.success()
        val matchingMasjid = nearestMatchingMasjid(currentLocation, preferences.savedMasjids)

        when {
            preferences.dndActive && !preferences.dndEnabled -> restoreIfNeeded(preferences)
            matchingMasjid != null && preferences.activeMasjidId != matchingMasjid.id -> {
                handleEnter(preferences, matchingMasjid)
            }
            matchingMasjid != null && preferences.dndEnabled && !preferences.dndActive -> {
                dndManager.enableForMasjid(preferences)
            }
            matchingMasjid == null && preferences.activeMasjidId != null -> handleExit(preferences)
            matchingMasjid == null && preferences.dndActive -> restoreIfNeeded(preferences)
        }

        return Result.success()
    }

    private suspend fun handleEnter(preferences: MasjidPreferences, masjid: SavedMasjid) {
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
        userPreferencesRepository.setActiveMasjid(masjid.id)
    }

    private suspend fun handleExit(preferences: MasjidPreferences) {
        restoreIfNeeded(preferences)
        userPreferencesRepository.setActiveMasjid(null)
    }

    private suspend fun restoreIfNeeded(preferences: MasjidPreferences) {
        if (preferences.dndActive) {
            dndManager.restoreFromMasjid(preferences)
        }
    }

    private fun nearestMatchingMasjid(
        current: Location,
        masjids: List<SavedMasjid>
    ): SavedMasjid? {
        return masjids
            .map { masjid -> masjid to distanceMeters(current, masjid) }
            .filter { (masjid, distance) -> distance <= masjid.radiusMeters }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun distanceMeters(current: Location, masjid: SavedMasjid): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            masjid.location.latitude,
            masjid.location.longitude,
            results
        )
        return results[0]
    }
}
