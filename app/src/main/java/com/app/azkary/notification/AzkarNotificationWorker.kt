package com.app.azkary.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.domain.model.AzkarWindow
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
        const val KEY_WINDOW_TYPE = "window_type"
    }

    override suspend fun doWork(): Result {
        val windowType = inputData.getString(KEY_WINDOW_TYPE) ?: return Result.failure()

        val notificationPrefs = userPreferencesRepository.notificationPreferences.first()

        when (windowType) {
            AzkarWindow.MORNING.name -> {
                if (notificationPrefs.morningAzkarEnabled) {
                    notificationManager.showMorningNotification()
                }
            }
            AzkarWindow.NIGHT.name -> {
                if (notificationPrefs.eveningAzkarEnabled) {
                    notificationManager.showEveningNotification()
                }
            }
            "NIGHT_AZKAR" -> {
                if (notificationPrefs.nightAzkarEnabled) {
                    notificationManager.showNightNotification()
                }
            }
            AzkarWindow.SLEEP.name -> {
                if (notificationPrefs.sleepAzkarEnabled) {
                    notificationManager.showSleepNotification()
                }
            }
        }

        return Result.success()
    }
}
