package com.app.azkary.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.app.azkary.MainActivity
import com.app.azkary.R
import com.app.azkary.data.model.CategoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzkarNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionHelper: NotificationPermissionHelper
) {

    companion object {
        const val CHANNEL_ID_BASE = "azkar_base_channel"
        const val CHANNEL_ID_USER = "azkar_user_channel"
        const val CHANNEL_ID_LOCATION = "azkar_location_channel"
        const val EXTRA_CATEGORY_ID = "category_id"
        private const val MASJID_ENTRY_NOTIFICATION_ID = 9001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val baseChannel = NotificationChannel(
                CHANNEL_ID_BASE,
                context.getString(R.string.notification_channel_base),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_base_desc)
            }

            val userChannel = NotificationChannel(
                CHANNEL_ID_USER,
                context.getString(R.string.notification_channel_user),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_user_desc)
            }

            val locationChannel = NotificationChannel(
                CHANNEL_ID_LOCATION,
                context.getString(R.string.notification_channel_location),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_location_desc)
            }

            notificationManager.createNotificationChannel(baseChannel)
            notificationManager.createNotificationChannel(userChannel)
            notificationManager.createNotificationChannel(locationChannel)
        }
    }

    fun showCategoryNotification(
        categoryId: String,
        categoryName: String,
        categoryType: CategoryType,
        notificationId: Int
    ) {
        val channelId = if (categoryType == CategoryType.DEFAULT) CHANNEL_ID_BASE else CHANNEL_ID_USER
        val notification = buildNotification(
            title = categoryName,
            content = context.getString(R.string.notification_reminder_content),
            notificationId = notificationId,
            categoryId = categoryId,
            channelId = channelId
        )
        if (permissionHelper.hasNotificationPermission()) {
            notificationManager.notify(notificationId, notification)
        }
    }

    fun showMasjidEntryNotification() {
        val notification = buildNotification(
            title = context.getString(R.string.notification_masjid_entry_title),
            content = context.getString(R.string.notification_masjid_entry_content),
            notificationId = MASJID_ENTRY_NOTIFICATION_ID,
            categoryId = "",
            channelId = CHANNEL_ID_LOCATION
        )
        if (permissionHelper.hasNotificationPermission()) {
            notificationManager.notify(MASJID_ENTRY_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        title: String,
        content: String,
        notificationId: Int,
        categoryId: String,
        channelId: String
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (categoryId.isNotBlank()) {
                putExtra(EXTRA_CATEGORY_ID, categoryId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
