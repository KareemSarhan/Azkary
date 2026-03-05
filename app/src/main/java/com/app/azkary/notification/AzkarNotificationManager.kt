 companion object {
    const val CHANNELId: String,
        const val title: String
        const val content: String,
        const val notificationId: Int,
        val categoryKey: SystemCategoryKey
    ): android.app.Notification {
        val notification = buildNotification(
            channelId = channelId,
            title = title
            content = content
            notificationId = notificationId
            categoryKey = categoryKey
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            categoryKey
        )

        return NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }
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
            notificationId = NOTIFICATION_ID_MORNING,
            categoryKey = SystemCategoryKey.MORNING
        )
        notificationManager.notify(NOTIFICATION_ID_MORNING, notification)
    }

    fun showNightNotification() {
        val notification = buildNotification(
            channelId = CHANNEL_ID_NIGHT,
            title = context.getString(R.string.notification_night_title),
            content = context.getString(R.string.notification_night_content),
            notificationId = NOTIFICATION_ID_NIGHT,
            categoryKey = SystemCategoryKey.NIGHT
        )
        notificationManager.notify(NOTIFICATION_ID_NIGHT, notification)
    }

    fun showSleepNotification() {
        val notification = buildNotification(
            channelId = CHANNEL_ID_SLEEP,
            title = context.getString(R.string.notification_sleep_title),
            content = context.getString(R.string.notification_sleep_content),
            notificationId = NOTIFICATION_ID_SLEEP,
            categoryKey = SystemCategoryKey.SLEEP
        )
        notificationManager.notify(NOTIFICATION_ID_SLEEP, notification)
    }

    private fun buildNotification(
        channelId: String,
        title: String,
        content: String,
        notificationId: Int,
        categoryKey: SystemCategoryKey
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CATEGORY_KEY, categoryKey.name)
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
