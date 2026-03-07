package com.app.azkary.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.domain.model.DayPrayerTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzkarNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val REMINDER_MINUTES = 30L
        const val WORK_TAG = "azkar_notification_worker"
        
        fun getWorkNameForCategory(categoryId: String) = "category_notification_$categoryId"
    }

    fun scheduleNotifications(
        todayTimes: DayPrayerTimes,
        categories: List<CategoryEntity>
    ) {
        cancelAllNotifications()

        val now = Instant.now()
        val zoneId = todayTimes.timezone

        categories.forEach { category ->
            val triggerTime = getPrayerTimeByIndex(todayTimes, category.from, zoneId)
                .plus(Duration.ofMinutes(REMINDER_MINUTES))

            if (triggerTime.isAfter(now)) {
                scheduleNotification(
                    categoryId = category.categoryId,
                    triggerTime = triggerTime
                )
            }
        }
    }

    private fun getPrayerTimeByIndex(times: DayPrayerTimes, index: Int, zoneId: ZoneId): Instant {
        val time: LocalTime = when (index) {
            0 -> times.fajr
            1 -> times.sunrise
            2 -> times.dhuhr
            3 -> times.asr
            4 -> times.maghrib
            5 -> times.isha
            6 -> times.firstthird
            7 -> times.midnight
            8 -> times.lastthird
            else -> times.fajr
        }
        return LocalDateTime.of(times.date, time).atZone(zoneId).toInstant()
    }

    private fun scheduleNotification(
        categoryId: String,
        triggerTime: Instant
    ) {
        val delay = Duration.between(Instant.now(), triggerTime)
        
        if (delay.isNegative || delay.isZero) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AzkarNotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(workDataOf(
                AzkarNotificationWorker.KEY_CATEGORY_ID to categoryId
            ))
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            getWorkNameForCategory(categoryId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAllNotifications() {
        workManager.cancelAllWorkByTag(WORK_TAG)
    }

    fun cancelNotificationForCategory(categoryId: String) {
        workManager.cancelUniqueWork(getWorkNameForCategory(categoryId))
    }
}
