package com.app.azkary.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.app.azkary.data.prefs.NotificationPreferences
import com.app.azkary.domain.model.AzkarWindow
import com.app.azkary.domain.model.DayPrayerTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzkarNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val WORK_NAME_MORNING = "morning_azkar_notification"
        const val WORK_NAME_EVENING = "evening_azkar_notification"
        const val WORK_NAME_NIGHT = "night_azkar_notification"
        const val WORK_NAME_SLEEP = "sleep_azkar_notification"
    }

    /**
     * Schedules all azkar notifications based on prayer times and user preferences
     */
    fun scheduleNotifications(
        todayTimes: DayPrayerTimes,
        tomorrowTimes: DayPrayerTimes? = null,
        preferences: NotificationPreferences
    ) {
        // Cancel existing notifications first
        cancelAllNotifications()

        val now = Instant.now()
        val zoneId = todayTimes.timezone

        // Schedule morning azkar (at Fajr time + reminder offset)
        if (preferences.morningAzkarEnabled) {
            val morningTime = LocalDateTime.of(todayTimes.date, todayTimes.fajr)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofMinutes(preferences.morningReminderMinutes.toLong()))

            if (morningTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_MORNING,
                    windowType = AzkarWindow.MORNING.name,
                    triggerTime = morningTime
                )
            }
        }

        // Schedule evening azkar (at Asr time + reminder offset)
        if (preferences.eveningAzkarEnabled) {
            val eveningTime = LocalDateTime.of(todayTimes.date, todayTimes.asr)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofMinutes(preferences.eveningReminderMinutes.toLong()))

            if (eveningTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_EVENING,
                    windowType = AzkarWindow.NIGHT.name,
                    triggerTime = eveningTime
                )
            }
        }

        // Schedule night azkar (at Isha time + reminder offset)
        if (preferences.nightAzkarEnabled) {
            val nightTime = LocalDateTime.of(todayTimes.date, todayTimes.isha)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofMinutes(preferences.nightReminderMinutes.toLong()))

            if (nightTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_NIGHT,
                    windowType = "NIGHT_AZKAR",
                    triggerTime = nightTime
                )
            }
        }

        // Schedule sleep azkar (at Isha time + 1 hour)
        if (preferences.sleepAzkarEnabled) {
            val sleepTime = LocalDateTime.of(todayTimes.date, todayTimes.isha)
                .atZone(zoneId)
                .toInstant()
                .plus(Duration.ofHours(1))

            if (sleepTime.isAfter(now)) {
                scheduleNotification(
                    workName = WORK_NAME_SLEEP,
                    windowType = AzkarWindow.SLEEP.name,
                    triggerTime = sleepTime
                )
            }
        }
    }

    /**
     * Schedules a single notification using WorkManager
     */
    private fun scheduleNotification(
        workName: String,
        windowType: String,
        triggerTime: Instant
    ) {
        val delay = Duration.between(Instant.now(), triggerTime)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AzkarNotificationWorker>()
            .setInitialDelay(delay)
            .setConstraints(constraints)
            .setInputData(workDataOf(AzkarNotificationWorker.KEY_WINDOW_TYPE to windowType))
            .addTag(AzkarNotificationWorker.WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels all scheduled azkar notifications
     */
    fun cancelAllNotifications() {
        workManager.cancelUniqueWork(WORK_NAME_MORNING)
        workManager.cancelUniqueWork(WORK_NAME_EVENING)
        workManager.cancelUniqueWork(WORK_NAME_NIGHT)
        workManager.cancelUniqueWork(WORK_NAME_SLEEP)
    }

    /**
     * Cancels and reschedules all notifications with new prayer times
     */
    fun rescheduleNotifications(
        todayTimes: DayPrayerTimes,
        tomorrowTimes: DayPrayerTimes? = null,
        preferences: NotificationPreferences
    ) {
        scheduleNotifications(todayTimes, tomorrowTimes, preferences)
    }
}
