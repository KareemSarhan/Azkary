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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzkarNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID_GENERAL = "azkar_general_channel"
        const val EXTRA_CATEGORY_ID = "category_id"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                context.getString(R.string.notification_channel_general),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_general_desc)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCategoryNotification(
        categoryId: String,
        categoryName: String,
        notificationId: Int
    ) {
        val notification = buildNotification(
            title = categoryName,
            content = context.getString(R.string.notification_reminder_content),
            notificationId = notificationId,
            categoryId = categoryId
        )
        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotification(
        title: String,
        content: String,
        notificationId: Int,
        categoryId: String
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CATEGORY_ID, categoryId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
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
