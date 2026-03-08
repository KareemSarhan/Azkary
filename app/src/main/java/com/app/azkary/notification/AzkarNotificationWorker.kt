package com.app.azkary.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.util.LocaleManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class AzkarNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationManager: AzkarNotificationManager,
    private val repository: AzkarRepository,
    private val localeManager: LocaleManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG = "azkar_notification_worker"
        const val KEY_CATEGORY_ID = "category_id"
    }

    override suspend fun doWork(): Result {
        val categoryId = inputData.getString(KEY_CATEGORY_ID) ?: return Result.failure()

        val langTag = localeManager.getCurrentLanguageTag(applicationContext)
        val today = LocalDate.now().toString()

        val categories = repository.observeCategoriesWithDisplayName(langTag, today).first()
        val category = categories.find { it.id == categoryId } ?: return Result.failure()

        notificationManager.showCategoryNotification(
            categoryId = categoryId,
            categoryName = category.name,
            categoryType = category.type,
            notificationId = categoryId.hashCode()
        )

        return Result.success()
    }
}
