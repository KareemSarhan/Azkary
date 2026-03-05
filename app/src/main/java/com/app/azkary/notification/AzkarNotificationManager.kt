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
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID_MORNING = "azkar_morning_channel"
        const val CHANNEL_ID_NIGHT = "azkar_night_channel"
        const val CHANNEL_ID_SLEEP = "azkar_sleep_channel"

        const val NOTIFICATION_ID_MORNING = 1001
        const val NOTIFICATION_ID_NIGHT = 1002
        const val NOTIFICATION_ID_SLEEP = 1003
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_MORNING,
                    context.getString(R.string.notification_channel_morning),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_morning_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_NIGHT,
                    context.getString(R.string.notification_channel_night),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_night_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_SLEEP,
                    context.getString(R.string.notification_channel_sleep),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_sleep_desc)
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    fun showMorningNotification() {
        val notification = buildNotification(
            channelId = CHANNEL_ID_MORNING,
            title = context.getString(R.string.notification_morning_title),
            content = context.getString(R.string.notification_morning_content),
            notificationId = NOTIFICATION_ID_MORNING
        )
        notificationManager.notify(NOTIFICATION_ID_MORNING, notification)
    }

    fun showNightNotification() {
        val notification = buildNotification(
            channelId = CHANNEL_ID_NIGHT,
            title = context.getString(R.string.notification_night_title),
            content = context.getString(R.string.notification_night_content),
            notificationId = NOTIFICATION_ID_NIGHT
        )
        notificationManager.notify(NOTIFICATION_ID_NIGHT, notification)
    }

    fun showSleepNotification() {
        val notification = buildNotification(
            channelId = CHANNEL_ID_SLEEP,
            title = context.getString(R.string.notification_sleep_title),
            content = context.getString(R.string.notification_sleep_content),
            notificationId = NOTIFICATION_ID_SLEEP
        )
        notificationManager.notify(NOTIFICATION_ID_SLEEP, notification)
    }

    private fun buildNotification(
        channelId: String,
        title: String,
        content: String,
        notificationId: Int
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notificationId)
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
