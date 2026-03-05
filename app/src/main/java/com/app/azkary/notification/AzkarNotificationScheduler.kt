package com.app.azkary.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.prefs.NotificationPreferences
import com.app.azkary.domain.model.DayPrayerTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzkarNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val REMINDER_MINUTES = 30L

        private const val WORK_NAME_MORNING = "morning_azkar_notification"
        private const val WORK_NAME_NIGHT = "night_azkar_notification"
        private const val WORK_NAME_SLEEP = "sleep_azkar_notification"
    }

    fun scheduleNotifications(
        todayTimes: DayPrayerTimes,
        tomorrowTimes: DayPrayerTimes? = null,
        preferences: NotificationPreferences
    ) {
        cancelAllNotifications()

        val now = Instant.now()
        val zoneId = todayTimes.timezone

        if (preferences.morningAzkarEnabled) {
            val morningTime = LocalDateTime.of(todayTimes.date, todayTimes.fajr)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofMinutes(REMINDER_MINUTES))

            if (morningTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_MORNING,
                    categoryKey = SystemCategoryKey.MORNING,
                    triggerTime = morningTime
                )
            }
        }

        if (preferences.nightAzkarEnabled) {
            val nightTime = LocalDateTime.of(todayTimes.date, todayTimes.asr)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofMinutes(REMINDER_MINUTES))

            if (nightTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_NIGHT,
                    categoryKey = SystemCategoryKey.NIGHT,
                    triggerTime = nightTime
                )
            }
        }

        if (preferences.sleepAzkarEnabled) {
            val sleepTime = LocalDateTime.of(todayTimes.date, todayTimes.isha)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofMinutes(REMINDER_MINUTES))

            if (sleepTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_SLEEP,
                    categoryKey = SystemCategoryKey.SLEEP,
                    triggerTime = sleepTime
                )
            }
        }
    }

    private fun scheduleNotification(
        workName: String,
        categoryKey: SystemCategoryKey,
        triggerTime: Instant
    ) {
        val delay = Duration.between(Instant.now(), triggerTime)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AzkarNotificationWorker>()
            .setInitialDelay(delay)
            .setConstraints(constraints)
            .setInputData(workDataOf(AzkarNotificationWorker.KEY_CATEGORY_KEY to categoryKey.name))
            .addTag(AzkarNotificationWorker.WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAllNotifications() {
        workManager.cancelUniqueWork(WORK_NAME_MORNING)
        workManager.cancelUniqueWork(WORK_NAME_NIGHT)
        workManager.cancelUniqueWork(WORK_NAME_SLEEP)
    }

    fun rescheduleNotifications(
        todayTimes: DayPrayerTimes,
        tomorrowTimes: DayPrayerTimes? = null,
        preferences: NotificationPreferences
    ) {
        scheduleNotifications(todayTimes, tomorrowTimes, preferences)
    }
}
