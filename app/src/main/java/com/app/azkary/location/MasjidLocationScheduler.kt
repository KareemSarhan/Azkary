package com.app.azkary.location

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasjidLocationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<MasjidLocationWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(MasjidLocationWorker.WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            MasjidLocationWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runCheckNow() {
        val request = OneTimeWorkRequestBuilder<MasjidLocationWorker>()
            .addTag(MasjidLocationWorker.WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            MasjidLocationWorker.UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelChecks() {
        workManager.cancelUniqueWork(MasjidLocationWorker.UNIQUE_PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(MasjidLocationWorker.UNIQUE_ONE_TIME_WORK_NAME)
    }
}
