package com.app.azkary.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPermissionHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Check if notification permission is granted
     * On Android 13+ (API 33+), requires POST_NOTIFICATIONS permission
     * On older versions, notifications are always allowed
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Check if notification permission should be requested
     * Returns true only on Android 13+ when permission is not granted
     */
    fun shouldRequestPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasNotificationPermission()
    }

    companion object {
        /**
         * Permission to request for notifications on Android 13+
         */
        val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

        /**
         * Check if notification permission is required for this Android version
         */
        fun isPermissionRequired(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
    }
}

/**
 * Extension function to request notification permission
 */
fun ManagedActivityResultLauncher<String, Boolean>.requestNotificationPermission() {
    if (NotificationPermissionHelper.isPermissionRequired()) {
        launch(NotificationPermissionHelper.NOTIFICATION_PERMISSION)
    }
}
