package com.app.azkary.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.app.azkary.data.prefs.MasjidPreferences
import com.app.azkary.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasjidDndManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun openNotificationPolicyAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun enableForMasjid(preferences: MasjidPreferences): Boolean {
        if (!hasNotificationPolicyAccess()) return false
        if (preferences.dndActive) return true

        val previousFilter = notificationManager.currentInterruptionFilter
        return try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            userPreferencesRepository.setMasjidDndSession(
                active = true,
                previousFilter = previousFilter
            )
            true
        } catch (e: SecurityException) {
            false
        } catch (e: RuntimeException) {
            false
        }
    }

    suspend fun restoreFromMasjid(preferences: MasjidPreferences): Boolean {
        if (!preferences.dndActive) return true
        if (!hasNotificationPolicyAccess()) return false

        val previousFilter =
            preferences.previousDndFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        return try {
            notificationManager.setInterruptionFilter(previousFilter)
            userPreferencesRepository.setMasjidDndSession(
                active = false,
                previousFilter = null
            )
            true
        } catch (e: SecurityException) {
            false
        } catch (e: RuntimeException) {
            false
        }
    }
}
