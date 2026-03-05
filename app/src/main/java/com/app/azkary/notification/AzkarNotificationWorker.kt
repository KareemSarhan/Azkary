package com.app.azkary.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.prefs.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AzkarNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationManager: AzkarNotificationManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG = "azkar_notification_worker"
        const val KEY_CATEGORY_KEY = "category_key"
    }

    override suspend fun doWork(): Result {
        val categoryKeyName = inputData.getString(KEY_CATEGORY_KEY) ?: return Result.failure()
        val categoryKey = try {
            SystemCategoryKey.valueOf(categoryKeyName)
        } catch (e: IllegalArgumentException) {
            return Result.failure()
        }

        val notificationPrefs = userPreferencesRepository.notificationPreferences.first()

        when (categoryKey) {
            SystemCategoryKey.MORNING -> {
                if (notificationPrefs.morningAzkarEnabled) {
                    notificationManager.showMorningNotification()
                }
            }
            SystemCategoryKey.NIGHT -> {
                if (notificationPrefs.nightAzkarEnabled) {
                    notificationManager.showNightNotification()
                }
            }
            SystemCategoryKey.SLEEP -> {
                if (notificationPrefs.sleepAzkarEnabled) {
                    notificationManager.showSleepNotification()
                }
            }
        }

        return Result.success()
    }
}
