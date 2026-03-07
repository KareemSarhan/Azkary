package com.app.azkary.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.PrayerTimesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class NotificationBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: AzkarNotificationScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                rescheduleNotifications()
            }
        }
    }

    private suspend fun rescheduleNotifications() {
        val locationPrefs = userPreferencesRepository.locationPreferences.first()
        val notificationPrefs = userPreferencesRepository.notificationPreferences.first()

        if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
            return
        }

        val location = locationPrefs.lastResolvedLocation

        val todayTimes = prayerTimesRepository.getDayPrayerTimes(
            date = LocalDate.now(),
            latitude = location.latitude,
            longitude = location.longitude
        )

        val tomorrowTimes = prayerTimesRepository.getDayPrayerTimes(
            date = LocalDate.now().plusDays(1),
            latitude = location.latitude,
            longitude = location.longitude
        )

        if (todayTimes != null) {
            notificationScheduler.scheduleNotifications(
                todayTimes = todayTimes,
                tomorrowTimes = tomorrowTimes,
                preferences = notificationPrefs
            )
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.first(): T {
        var result: T? = null
        collect { 
            result = it
            return@collect
        }
        return result!!
    }
}
