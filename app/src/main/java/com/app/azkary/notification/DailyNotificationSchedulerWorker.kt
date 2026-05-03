package com.app.azkary.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class DailyNotificationSchedulerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationScheduler: AzkarNotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val azkarRepository: AzkarRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_notification_scheduler"
    }

    override suspend fun doWork(): Result {
        val locationPrefs = userPreferencesRepository.locationPreferences.first()
        if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
            return Result.success()
        }

        val location = locationPrefs.lastResolvedLocation
        val todayTimes = prayerTimesRepository.getDayPrayerTimes(
            date = LocalDate.now(),
            latitude = location.latitude,
            longitude = location.longitude
        ) ?: return Result.success()

        val categories = azkarRepository.getCategoriesWithNotifications()
        if (categories.isEmpty()) return Result.success()

        notificationScheduler.scheduleNotifications(
            todayTimes = todayTimes,
            categories = categories
        )

        return Result.success()
    }
}
