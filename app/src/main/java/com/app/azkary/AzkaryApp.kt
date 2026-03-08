package com.app.azkary

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import com.app.azkary.notification.AzkarNotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidApp
class AzkaryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: AzkarNotificationScheduler

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    @Inject
    lateinit var azkarRepository: AzkarRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            userPreferencesRepository.initializeFirstInstallDate()
            userPreferencesRepository.incrementAppOpenCount()
            scheduleNotificationsIfNeeded()
        }
    }

    private suspend fun scheduleNotificationsIfNeeded() {
        val locationPrefs = userPreferencesRepository.locationPreferences.first()
        if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) return

        val categories = azkarRepository.getCategoriesWithNotifications()
        if (categories.isEmpty()) return

        val location = locationPrefs.lastResolvedLocation
        val todayTimes = prayerTimesRepository.getDayPrayerTimes(
            date = LocalDate.now(),
            latitude = location.latitude,
            longitude = location.longitude
        ) ?: return

        notificationScheduler.scheduleNotifications(
            todayTimes = todayTimes,
            categories = categories
        )
    }
}
